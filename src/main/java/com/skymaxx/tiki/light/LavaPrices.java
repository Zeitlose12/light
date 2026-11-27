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
    private record Cached(long bin, long minBin, long instaSell, long sellOffer, long atMs) {
    }

    /** Where each drop is traded (checked by Grundsteuer in game): bazaar -> sell offer, auction house -> lowest BIN. */
    private static final java.util.Set<String> BAZAAR = java.util.Set.of("RADIOACTIVE_VIAL", "ENCHANTMENT_ULTIMATE_FLASH_1",
        "MAGMA_LORD_FRAGMENT", "BOBBIN_SCRIPTURES", "CHAIN_END_TIMES", "BRIMSTONE_HANDLE", "PET_ITEM_VAMPIRE_FANG", "DEEP_SEA_ORB",
        "TROUBLED_BUBBLE", "TITANOBOA_SHED", "THUNDER_SHARDS", "FLAMING_HEART", "ENCHANTMENT_PROSPERITY_1");
    private static final java.util.Set<String> AUCTION = java.util.Set.of("DYE_CARMINE", "DYE_MIDNIGHT", "DYE_ICEBERG",
        "DYE_AQUAMARINE", "BURNT_TEXTS", "BABY_YETI", "HILT_OF_TRUE_ICE", "SNAKE_EYES", "TIKI_MASK", "GREAT_WHITE_SHARK_TOOTH");

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
        TAGS.put("hilt of true ice", "HILT_OF_TRUE_ICE");
        TAGS.put("true ice", "HILT_OF_TRUE_ICE");
        TAGS.put("baby yeti", "BABY_YETI");
        TAGS.put("troubled bubble", "TROUBLED_BUBBLE");
        TAGS.put("tiki mask", "TIKI_MASK");
        TAGS.put("titanoboa shed", "TITANOBOA_SHED");
        TAGS.put("snake eyes", "SNAKE_EYES");
        TAGS.put("enchanted book (prosperity i)", "ENCHANTMENT_PROSPERITY_1");
        TAGS.put("enchanted book (flash i)", "ENCHANTMENT_ULTIMATE_FLASH_1"); // Flash is an ultimate enchantment
        TAGS.put("magma lord fragment", "MAGMA_LORD_FRAGMENT");
        TAGS.put("thunder fragment", "THUNDER_SHARDS");
    }

    private LavaPrices() {
    }

    /**
     * Journal price, always the same rule (whatever the Settings say): bazaar items -> current sell offer,
     * auction items -> current lowest BIN. Null while not known.
     */
    public static Long journalPrice(String itemName) {
        if (!enabled()) return null;
        String tag = tag(itemName);
        if (tag == null) return null;
        Cached c = CACHE.get(tag);
        if (c == null || System.currentTimeMillis() - c.atMs > CACHE_MS) request(tag);
        boolean bz = BAZAAR.contains(tag) || (!AUCTION.contains(tag) && c != null && c.minBin < 0);
        long v = c == null ? -1 : (bz ? c.sellOffer : c.minBin);
        if (v <= 0 && bz) {
            double hp = SackValue.bazaarPrice(tag, true);
            if (hp > 0) v = Math.round(hp);
        }
        return v > 0 ? v : null;
    }

    /** The SkyBlock id for a drop name, or null if Light does not know it yet. */
    public static String tag(String itemName) {
        if (itemName == null) return null;
        String k = norm(itemName);
        String t = TAGS.get(k);
        if (t == null) t = TAGS.get("enchanted book (" + k + ")");
        return t;
    }

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    /** "Enchanted Book (Flash 1)" / "Flash 1" / "Flash I" -> "flash i" (Hypixel writes book levels as digits). */
    public static String norm(String itemName) {
        String k = itemName.replaceAll("§.", "").trim();
        if (k.toLowerCase(Locale.ROOT).startsWith("enchanted book (") && k.endsWith(")")) k = k.substring(16, k.length() - 1).trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.*\\S)\\s+(\\d{1,2})$").matcher(k);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(2));
            if (n >= 1 && n <= 10) k = m.group(1) + " " + ROMAN[n];
        }
        return k.toLowerCase(Locale.ROOT);
    }

    /** Display name with a roman book level: "Flash 1" -> "Flash I". */
    /**
     * Light Crew banner: the board drop named in this chat text (any bazaar / auction drop Light prices), or null.
     * Thunder Fragment and Prosperity I are left out (almost worthless / always dropped).
     */
    private static final String[][] SHORT = {
        {"vial", "Radioactive Vial"}, {"flash ?(1|i)?( book)?", "Flash I"}, {"scriptures?|bobbin", "Bobbin' Scriptures"},
        {"magma lord frag(ment)?|mlf", "Magma Lord Fragment"}, {"burnt texts?", "Burnt Texts"}, {"chain", "Chain of the End Times"},
        {"brimstone", "Brimstone Handle"}, {"vampire fang|fang", "Vampire Fang"}, {"orb", "Deep Sea Orb"},
        {"bubble", "Troubled Bubble"}, {"shed", "Titanoboa Shed"}, {"mask", "Tiki Mask"}, {"snake ?eyes", "Snake Eyes"},
        {"baby yeti", "Baby Yeti"}, {"true ice|hilt", "True Ice"}, {"gws tooth|shark tooth", "Great White Shark Tooth"},
        {"flaming heart", "Flaming Heart"}, {"aqua(marine)?", "Aquamarine Dye"}, {"iceberg", "Iceberg Dye"},
        {"carmine", "Carmine Dye"}, {"midnight", "Midnight Dye"}};

    public static String boardDropIn(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        String best = null;
        for (Map.Entry<String, String> e : TAGS.entrySet()) {
            String tag = e.getValue();
            if (tag.equals("THUNDER_SHARDS") || tag.equals("ENCHANTMENT_PROSPERITY_1")) continue;
            if (!BAZAAR.contains(tag) && !AUCTION.contains(tag)) continue;
            String key = e.getKey();
            String inner = key.startsWith("enchanted book (") && key.endsWith(")") ? key.substring(16, key.length() - 1) : key;
            if ((t.contains(key) || t.contains(inner)) && (best == null || key.length() > best.length())) best = key;
        }
        if (best == null) {
            // custom party messages from other mods often use short names
            for (String[] al : SHORT) if (t.matches(".*\\b(?:" + al[0] + ")\\b.*")) return al[1];
            return null;
        }
        String d = display(best);
        StringBuilder out = new StringBuilder();
        for (String w : d.split(" ")) {
            if (out.length() > 0) out.append(' ');
            out.append(w.matches("[ivx]+") ? w.toUpperCase(Locale.ROOT) : w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1));
        }
        return out.toString().replace(" Of ", " of ").replace(" The ", " the ").replace("Hilt of True Ice", "True Ice");
    }

    public static String display(String itemName) {
        if (itemName == null) return null;
        String k = itemName.replaceAll("§.", "").trim();
        if (k.toLowerCase(Locale.ROOT).startsWith("enchanted book (") && k.endsWith(")")) k = k.substring(16, k.length() - 1).trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.*\\S)\\s+(\\d{1,2})$").matcher(k);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(2));
            if (n >= 1 && n <= 10) k = m.group(1) + " " + ROMAN[n];
        }
        return k;
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
        boolean bz = BAZAAR.contains(tag) || (c != null && c.bin < 0);
        long v = c == null ? -1 : (bz ? ("SELL_OFFER".equals(cfgBazaar()) ? c.sellOffer : c.instaSell) : c.bin);
        if (v < 0) {
            // fallback: Hypixel's own bazaar list (books are only sold there)
            double hp = SackValue.bazaarPrice(tag, "SELL_OFFER".equals(cfgBazaar()));
            if (hp > 0) v = Math.round(hp);
        }
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
                // bazaar items never ask the auction house and the other way round
                long[] p = BAZAAR.contains(tag) || tag.startsWith("ENCHANTMENT_") ? new long[]{-1, -1} : lowestBin(tag);
                long[] bz = p[0] < 0 && !AUCTION.contains(tag) ? bazaar(tag) : new long[]{-1, -1};
                boolean none = p[0] < 0 && bz[0] < 0 && bz[1] < 0;
                CACHE.put(tag, new Cached(p[0], p[1], bz[0], bz[1], System.currentTimeMillis() - (none ? CACHE_MS - 30_000L : 0L)));
            } catch (Throwable t) {
                // failed: try again in about 30 seconds instead of waiting for the full cache time
                CACHE.put(tag, new Cached(-1, -1, -1, -1, System.currentTimeMillis() - CACHE_MS + 30_000L));
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
    private static long[] lowestBin(String tag) throws Exception {
        String body = get("https://sky.coflnet.com/api/auctions/tag/" + tag + "/active/bin");
        if (body == null) return new long[]{-1, -1};
        com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(body);
        if (!root.isJsonArray()) return new long[]{-1, -1};
        java.util.List<Long> prices = new java.util.ArrayList<>();
        for (com.google.gson.JsonElement e : root.getAsJsonArray()) {
            if (!e.isJsonObject() || !e.getAsJsonObject().has("startingBid")) continue;
            long v = e.getAsJsonObject().get("startingBid").getAsLong();
            int count = e.getAsJsonObject().has("count") ? Math.max(1, e.getAsJsonObject().get("count").getAsInt()) : 1;
            if (v > 0) prices.add(v / count);
        }
        if (prices.isEmpty()) return new long[]{-1, -1};
        java.util.Collections.sort(prices);
        if ("MEDIAN_5".equals(cfgAuction())) {
            // median of the 5 cheapest: one troll listing can't fake the price
            int n = Math.min(5, prices.size());
            return new long[]{prices.get(n / 2), prices.get(0)};
        }
        return new long[]{prices.get(0), prices.get(0)};
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
