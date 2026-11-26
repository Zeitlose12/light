package com.skymaxx.tiki.light;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Light: prices for the drops shown in the lava gambling scenes (data: SkyCofl, sky.coflnet.com/data).
 *
 * Plan (Coflnet / SkyCofl, sky.coflnet.com, public endpoints, no token):
 *   - auction items: GET https://sky.coflnet.com/api/auctions/tag/{TAG}/active/bin   (lowest BIN)
 *   - bazaar items:  GET https://sky.coflnet.com/api/bazaar/{TAG}/snapshot          (sell price)
 *   - one request per real drop when the result box opens, never in a loop; cached 10 minutes;
 *     limits are 30 requests / 10 s and 100 / min per IP, Light stays far below.
 *   - runs off the render thread; if no answer in time the box simply shows no price.
 *   - opt-in setting (config: lavaGamblePrices, default off), README + Modrinth text must say
 *     that Light then talks to sky.coflnet.com, and SkyCofl must be credited with a link.
 *
 * While the setting is off, {@link #price(String)} returns null and nothing touches the network.
 */
public final class LavaPrices {
    /** Item name as it appears in chat -> SkyBlock item id (checked against the NEU repo). */
    private static final Map<String, String> TAGS = new HashMap<>();

    /** bin: chosen auction price; instaSell / sellOffer: bazaar. -1 = unknown. */
    private record Cached(long bin, long instaSell, long sellOffer, long atMs) {
    }

    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();
    static final long CACHE_MS = 10L * 60L * 1000L;

    static {
        TAGS.put("radioactive vial", "RADIOACTIVE_VIAL");
        TAGS.put("carmine dye", "DYE_CARMINE");
        TAGS.put("burnt texts", "BURNT_TEXTS");
        TAGS.put("chain of the end times", "CHAIN_END_TIMES");
        TAGS.put("brimstone handle", "BRIMSTONE_HANDLE");
        TAGS.put("bobbin' scriptures", "BOBBIN_SCRIPTURES");
        TAGS.put("flaming heart", "FLAMING_HEART");
        TAGS.put("great white shark tooth", "GREAT_WHITE_SHARK_TOOTH");
        TAGS.put("vampire fang", "PET_ITEM_VAMPIRE_FANG");
        TAGS.put("deep sea orb", "DEEP_SEA_ORB");
        TAGS.put("midnight dye", "DYE_MIDNIGHT");
        TAGS.put("iceberg dye", "DYE_ICEBERG");
        TAGS.put("aquamarine dye", "DYE_AQUAMARINE");
        TAGS.put("yeti rod", "YETI_ROD");
        TAGS.put("hilt of true ice", "HILT_OF_TRUE_ICE");
        TAGS.put("true ice", "HILT_OF_TRUE_ICE");
        TAGS.put("enchanted book (prosperity i)", "ENCHANTMENT_PROSPERITY_1");
        TAGS.put("enchanted book (flash i)", "ENCHANTMENT_FLASH_1");
        TAGS.put("magma lord fragment", "MAGMA_LORD_FRAGMENT");
        TAGS.put("thunder fragment", "THUNDER_SHARDS");
    }

    private LavaPrices() {
    }

    /** The SkyBlock id for a drop name, or null if Light does not know it yet. */
    public static String tag(String itemName) {
        if (itemName == null) return null;
        return TAGS.get(itemName.replaceAll("§.", "").trim().toLowerCase(Locale.ROOT));
    }


    private static final java.util.Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final java.util.concurrent.ExecutorService WORKER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread th = new Thread(r, "Light-Prices");
        th.setDaemon(true);
        return th;
    });
    private static java.net.http.HttpClient http;
    private static volatile long pausedUntil;
    private static final java.util.ArrayDeque<Long> SENT = new java.util.ArrayDeque<>();

    /** Cached price in coins, or null (not known yet / prices switched off). Starts a fetch if needed. */
    public static Long price(String itemName) {
        if (!enabled()) return null;
        String tag = tag(itemName);
        if (tag == null) return null;
        Cached c = CACHE.get(tag);
        if (c == null || System.currentTimeMillis() - c.atMs > CACHE_MS) {
            request(tag);
        }
        if (c == null) return null;
        long v = c.bin >= 0 ? c.bin : ("SELL_OFFER".equals(cfgBazaar()) ? c.sellOffer : c.instaSell);
        return v < 0 ? null : v;
    }

    /** One background request per item (lowest BIN, else bazaar), never more than 20 a minute. */
    private static void request(String tag) {
        if (System.currentTimeMillis() < pausedUntil || !IN_FLIGHT.add(tag)) return;
        WORKER.execute(() -> {
            try {
                synchronized (SENT) {
                    long now = System.currentTimeMillis();
                    while (!SENT.isEmpty() && now - SENT.peekFirst() > 60_000L) SENT.pollFirst();
                    if (SENT.size() >= 20) return;
                    SENT.addLast(now);
                }
                long p = lowestBin(tag);
                long[] bz = p < 0 ? bazaar(tag) : new long[]{-1, -1};
                CACHE.put(tag, new Cached(p, bz[0], bz[1], System.currentTimeMillis()));
            } catch (Throwable t) {
                CACHE.put(tag, new Cached(-1, -1, -1, System.currentTimeMillis()));
            } finally {
                IN_FLIGHT.remove(tag);
            }
        });
    }

    private static String get(String url) throws Exception {
        if (http == null) http = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
            .timeout(java.time.Duration.ofSeconds(8))
            .header("User-Agent", "Light-Mod (Fabric, Hypixel SkyBlock fishing animations)")
            .GET().build();
        java.net.http.HttpResponse<String> res = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 429) {
            long wait = res.headers().firstValueAsLong("Retry-After").orElse(60L);
            pausedUntil = System.currentTimeMillis() + Math.max(10L, wait) * 1000L;
            return null;
        }
        return res.statusCode() == 200 ? res.body() : null;
    }

    /** GET /api/auctions/tag/{TAG}/active/bin -> cheapest active BIN. */
    private static long lowestBin(String tag) throws Exception {
        String body = get("https://sky.coflnet.com/api/auctions/tag/" + tag + "/active/bin");
        if (body == null) return -1;
        com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(body);
        if (!root.isJsonArray()) return -1;
        java.util.List<Long> prices = new java.util.ArrayList<>();
        for (com.google.gson.JsonElement e : root.getAsJsonArray()) {
            if (!e.isJsonObject() || !e.getAsJsonObject().has("startingBid")) continue;
            long v = e.getAsJsonObject().get("startingBid").getAsLong();
            int count = e.getAsJsonObject().has("count") ? Math.max(1, e.getAsJsonObject().get("count").getAsInt()) : 1;
            if (v > 0) prices.add(v / count);
        }
        if (prices.isEmpty()) return -1;
        java.util.Collections.sort(prices);
        if ("MEDIAN_5".equals(cfgAuction())) {
            // median of the 5 cheapest: one troll listing can't fake the price
            int n = Math.min(5, prices.size());
            return prices.get(n / 2);
        }
        return prices.get(0);
    }

    /**
     * GET /api/bazaar/{TAG}/snapshot -> {insta-sell, sell offer}. sellPrice is what buy orders pay right now
     * (insta-sell); buyPrice is what sellers ask (where your sell offer would sit).
     */
    private static long[] bazaar(String tag) throws Exception {
        String body = get("https://sky.coflnet.com/api/bazaar/" + tag + "/snapshot");
        if (body == null) return new long[]{-1, -1};
        com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(body);
        if (!root.isJsonObject()) return new long[]{-1, -1};
        com.google.gson.JsonObject o = root.getAsJsonObject();
        double sell = o.has("sellPrice") ? o.get("sellPrice").getAsDouble() : -1;
        double buy = o.has("buyPrice") ? o.get("buyPrice").getAsDouble() : -1;
        return new long[]{sell > 0 ? Math.round(sell) : -1, buy > 0 ? Math.round(buy) : -1};
    }

    private static String cfgBazaar() {
        com.skymaxx.tiki.config.TikiConfig c = com.skymaxx.tiki.TikiGambleClient.config;
        return c == null ? "INSTA_SELL" : c.priceBazaarMode;
    }

    private static String cfgAuction() {
        com.skymaxx.tiki.config.TikiConfig c = com.skymaxx.tiki.TikiGambleClient.config;
        return c == null ? "LOWEST_BIN" : c.priceAuctionMode;
    }

    /** Forget all prices (after changing the auction mode, or on request). */
    public static void clearCache() {
        CACHE.clear();
    }

    /** Only when the player switched it on (Gambling -> Lava Gambling -> Show prices). */
    private static boolean enabled() {
        com.skymaxx.tiki.config.TikiConfig c = com.skymaxx.tiki.TikiGambleClient.config;
        return c != null && c.lavaGamblePrices;
    }

    /** 10000000 -> "10.000.000 Coins", written out in full for the result box. */
    public static String format(long coins) {
        java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols(Locale.ROOT);
        sym.setGroupingSeparator('.');
        return new java.text.DecimalFormat("#,##0", sym).format(coins) + " Coins";
    }
}
