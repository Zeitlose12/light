package com.skymaxx.tiki.light;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.detect.DropLines;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.joml.Matrix3x2fStack;

/**
 * Light: the value of everything in your sacks, as a tiny HUD (like SkyHanni's sack display).
 * Amounts: read when you open a sack ("Stored: 1,234/60K") and kept up to date with the [Sacks] chat messages.
 * Prices: Hypixel's public bazaar API (one request for all items, every 5 minutes, no key) - sell offer or
 * insta-sell. Item names are matched to bazaar ids with Hypixel's public item list (cached for a week).
 */
public final class SackValue {
    public static final int W = 150;
    public static final int H = 30;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern STORED = Pattern.compile("(?i)Stored:\\s*([\\d,]+)\\s*/");
    private static final Pattern REMOVED = Pattern.compile("^\\s*-([\\d,]+)\\s+(.+?)\\s*\\((.+)\\)\\s*$");
    private static final Pattern ADDED = Pattern.compile("^\\s*\\+([\\d,]+)\\s+(.+?)\\s*\\((.+)\\)\\s*$");
    private static final long BAZAAR_MS = 5 * 60_000L;
    private static final long ITEMS_MS = 7L * 24 * 3_600_000L;

    /** sack name (without Small/Medium/Large) -> item name -> amount */
    private static final class Data {
        Map<String, Map<String, Long>> sacks = new HashMap<>();
        long readMs;
    }

    private static Data data;
    private static boolean dirty;
    private static int tick;
    /** bazaar id -> {insta-sell, sell offer} */
    private static final Map<String, double[]> PRICES = new ConcurrentHashMap<>();
    /** lower-case item name -> bazaar id */
    private static final Map<String, String> IDS = new ConcurrentHashMap<>();
    private static volatile long bazaarMs;
    private static volatile boolean busy;
    private static HttpClient http;
    // display
    private static double shown = -1;
    private static double from;
    private static long animStart;
    private static double target = -1;
    private static double delta;
    private static long deltaMs;

    private SackValue() {
    }

    private static TikiConfig cfg() {
        return TikiGambleClient.config;
    }

    private static Path folder() {
        return FabricLoader.getInstance().getConfigDir().resolve(LightFiles.FOLDER);
    }

    private static Data data() {
        if (data != null) return data;
        Data d = null;
        try {
            Path f = folder().resolve("sacks_v2.json");
            if (Files.exists(f)) d = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Data.class);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] could not read sacks.json", t);
        }
        if (d == null) d = new Data();
        if (d.sacks == null) d.sacks = new HashMap<>();
        data = d;
        return d;
    }

    private static void save() {
        try {
            Files.createDirectories(folder());
            Files.writeString(folder().resolve("sacks_v2.json"), GSON.toJson(data()), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] could not save sacks.json", t);
        }
    }

    private static String key(String name) {
        return DropLines.stripFormatting(name).trim().toLowerCase(Locale.ROOT);
    }

    /** "Large Enchanted Fishing Sack" -> "Enchanted Fishing Sack" (one entry per sack type). */
    public static String sackName(String raw) {
        String n = DropLines.stripFormatting(raw).trim().replaceAll("^(?i)(Small|Medium|Large)\\s+", "");
        return n.isEmpty() ? "Sack" : n;
    }

    private static Map<String, Long> sack(String name) {
        return data().sacks.computeIfAbsent(sackName(name), k -> new HashMap<>());
    }

    /** Known sacks, sorted by name (Bazaar tab: which ones count). */
    public static java.util.List<String> knownSacks() {
        java.util.List<String> out = new java.util.ArrayList<>(data().sacks.keySet());
        java.util.Collections.sort(out);
        return out;
    }

    public static boolean excluded(String sack) {
        TikiConfig c = cfg();
        if (c == null || c.sackExcluded == null || c.sackExcluded.isBlank()) return false;
        for (String s : c.sackExcluded.split(",")) if (s.equalsIgnoreCase(sack)) return true;
        return false;
    }

    public static void setExcluded(String sack, boolean ex) {
        TikiConfig c = cfg();
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        if (c.sackExcluded != null && !c.sackExcluded.isBlank()) for (String s : c.sackExcluded.split(",")) if (!s.equalsIgnoreCase(sack)) set.add(s);
        if (ex) set.add(sack);
        c.sackExcluded = String.join(",", set);
        c.save();
    }

    /** 902.971.234 -> "902,97M", 1.210.000.000 -> "1,21B". */
    public static String compact(double v) {
        double a = Math.abs(v);
        if (a >= 1e9) return String.format(Locale.GERMANY, "%.2fB", v / 1e9);
        if (a >= 1e6) return String.format(Locale.GERMANY, "%.2fM", v / 1e6);
        if (a >= 1e3) return String.format(Locale.GERMANY, "%.1fK", v / 1e3);
        return String.valueOf(Math.round(v));
    }

    private static String num(double v) {
        TikiConfig c = cfg();
        return c != null && c.sackCompact ? compact(v) : String.format(Locale.GERMANY, "%,d", Math.round(v));
    }

    // ------------------------------------------------------------------ reading

    /** Every client tick: reads an open sack a few times a second, saves now and then, refreshes prices. */
    public static void tick(Minecraft mc) {
        TikiConfig c = cfg();
        if (c == null || !c.sackHudEnabled) return;
        ++tick;
        if (tick % 10 == 0) {
            Screen screen = mc.gui == null ? null : mc.gui.screen();
            if (screen instanceof AbstractContainerScreen<?> container) scan(container);
        }
        if (dirty && tick % 100 == 0) {
            dirty = false;
            save();
        }
        if (tick % 200 == 0 && mc.level != null && System.currentTimeMillis() - bazaarMs > BAZAAR_MS) refresh();
    }

    private static void scan(AbstractContainerScreen<?> container) {
        String title = DropLines.stripFormatting(container.getTitle().getString());
        if (!title.contains("Sack") || title.contains("Sack of Sacks") || title.contains("Sacks")) return;
        Map<String, Long> into = null;
        int found = 0;
        for (Slot slot : container.getMenu().slots) {
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory) continue;
            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) continue;
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            for (Component line : lore.lines()) {
                Matcher m = STORED.matcher(DropLines.stripFormatting(line.getString()));
                if (!m.find()) continue;
                try {
                    long n = Long.parseLong(m.group(1).replace(",", ""));
                    String k = key(stack.getHoverName().getString());
                    if (into == null) into = sack(title);
                    Long old = into.put(k, n);
                    if (old == null || old != n) dirty = true;
                    ++found;
                } catch (NumberFormatException ignored) {
                }
                break;
            }
        }
        if (found > 0) data().readMs = System.currentTimeMillis();
    }

    /** [Sacks] chat message: its hover lists "+12 Item (Sack)" and "-5 Item (Sack)". */
    public static void onChat(Component message, boolean overlay) {
        TikiConfig c = cfg();
        if (c == null || !c.sackHudEnabled || overlay || message == null) return;
        String line = DropLines.stripFormatting(message.getString()).trim();
        if (!DropLines.isSackReport(line)) return;
        LinkedHashSet<String> tooltips = new LinkedHashSet<>();
        for (Component part : message.toFlatList()) {
            if (part.getStyle().getHoverEvent() instanceof HoverEvent.ShowText text) {
                tooltips.add(DropLines.stripFormatting(text.value().getString()));
            }
        }
        for (String tip : tooltips) {
            for (String l : tip.split("\\R")) {
                Matcher a = ADDED.matcher(l);
                Matcher r = REMOVED.matcher(l);
                try {
                    if (a.matches()) sack(a.group(3)).merge(key(a.group(2)), Long.parseLong(a.group(1).replace(",", "")), Long::sum);
                    else if (r.matches()) sack(r.group(3)).merge(key(r.group(2)), -Long.parseLong(r.group(1).replace(",", "")), (x, y) -> Math.max(0L, x + y));
                    else continue;
                    dirty = true;
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    // ------------------------------------------------------------------ prices

    public static void refresh() {
        if (busy) return;
        busy = true;
        bazaarMs = System.currentTimeMillis();
        Thread th = new Thread(() -> {
            try {
                if (IDS.isEmpty()) loadIds();
                String body = get("https://api.hypixel.net/v2/skyblock/bazaar");
                if (body != null) {
                    JsonObject products = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("products");
                    for (Map.Entry<String, JsonElement> e : products.entrySet()) {
                        JsonObject q = e.getValue().getAsJsonObject().getAsJsonObject("quick_status");
                        if (q == null) continue;
                        PRICES.put(e.getKey(), new double[]{q.get("sellPrice").getAsDouble(), q.get("buyPrice").getAsDouble()});
                    }
                }
            } catch (Throwable t) {
                TikiGambleClient.LOGGER.warn("[Light] sack prices failed", t);
            } finally {
                busy = false;
            }
        }, "Light-SackPrices");
        th.setDaemon(true);
        th.start();
    }

    /** Hypixel's item list (name -> id), cached in config/light/item_ids.json for a week. */
    private static void loadIds() {
        Path f = folder().resolve("item_ids.json");
        try {
            if (Files.exists(f) && System.currentTimeMillis() - Files.getLastModifiedTime(f).toMillis() < ITEMS_MS) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : o.entrySet()) IDS.put(e.getKey(), e.getValue().getAsString());
                if (!IDS.isEmpty()) return;
            }
        } catch (Throwable ignored) {
        }
        String body = get("https://api.hypixel.net/v2/resources/skyblock/items");
        if (body == null) return;
        JsonObject out = new JsonObject();
        for (JsonElement el : JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("items")) {
            JsonObject it = el.getAsJsonObject();
            if (!it.has("id") || !it.has("name")) continue;
            String name = key(it.get("name").getAsString());
            String id = it.get("id").getAsString();
            IDS.putIfAbsent(name, id);
            out.addProperty(name, id);
        }
        try {
            Files.createDirectories(folder());
            Files.writeString(f, out.toString(), StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
    }

    private static String get(String url) {
        try {
            if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Light-Mod (Fabric, Hypixel SkyBlock sack value)").GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200 ? res.body() : null;
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] request failed: " + url, t);
            return null;
        }
    }

    /** Total value of all sacks with the chosen price, or -1 when nothing is known yet. */
    public static double total() {
        Data d = data();
        if (d.sacks.isEmpty() || PRICES.isEmpty()) return -1;
        boolean offer = !"INSTA_SELL".equals(cfg().sackPriceMode);
        double sum = 0;
        for (Map.Entry<String, Map<String, Long>> s : d.sacks.entrySet()) {
            if (excluded(s.getKey())) continue;
            for (Map.Entry<String, Long> e : s.getValue().entrySet()) {
                if (e.getValue() <= 0) continue;
                String id = IDS.get(e.getKey());
                if (id == null) id = e.getKey().toUpperCase(Locale.ROOT).replace(' ', '_');
                double[] p = PRICES.get(id);
                if (p == null) continue;
                sum += e.getValue() * (offer ? p[1] : p[0]);
            }
        }
        return sum;
    }

    public static boolean hasCounts() {
        return !data().sacks.isEmpty();
    }

    // ------------------------------------------------------------------ HUD

    public static int boxX(TikiConfig c, int sw) {
        return c.sackHudX < 0 ? 6 : Math.round(c.sackHudX * sw);
    }

    public static int boxY(TikiConfig c, int sh) {
        return c.sackHudY < 0 ? 6 : Math.round(c.sackHudY * sh);
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker dt) {
        TikiConfig c = cfg();
        if (c == null || !c.sackHudEnabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        try {
            double now = total();
            long ms = System.currentTimeMillis();
            if (now >= 0) {
                if (target < 0) {
                    target = now;
                    shown = now;
                } else if (Math.abs(now - target) >= 1) {
                    if (now > target) {
                        delta = now - target;
                        deltaMs = ms;
                    }
                    from = shown;
                    target = now;
                    animStart = ms;
                }
                float u = Math.min(1f, (ms - animStart) / 800f);
                shown = from + (target - from) * (1 - Math.pow(1 - u, 3));
                if (u >= 1f) shown = target;
            }
            draw(ctx, mc.font, boxX(c, ctx.guiWidth()), boxY(c, ctx.guiHeight()), c.sackHudScale, now < 0 ? -1 : shown,
                ms - deltaMs < 2500 ? delta : 0, "TEXT".equals(c.sackHudStyle), !"INSTA_SELL".equals(c.sackPriceMode));
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] sack HUD failed", t);
        }
    }

    /** Style A (box) or B (text only). value < 0: not loaded yet. */
    public static void draw(GuiGraphicsExtractor ctx, Font font, int x, int y, float scale, double value, double plus, boolean textOnly, boolean offer) {
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(x, y);
        ps.scale(scale, scale);
        String label = "Sacks · " + (offer ? "Sell Offer" : "Insta-Sell");
        String num = value < 0 ? (hasCounts() ? "loading prices..." : "open your sacks once") : num(value) + " Coins";
        int w = Math.max(font.width(label), font.width(num)) + 12;
        if (textOnly) {
            ctx.text(font, Component.literal(textOnlyLabel(offer)), 0, 0, 0xFFFF7AC0);
            ctx.text(font, Component.literal(value < 0 ? num : num(value)), 0, 11, value < 0 ? 0xFF9E9890 : 0xFFFFD24A);
            if (plus > 0) ctx.text(font, Component.literal("+" + num(plus)), 0, 22, 0xFF55FF55);
        } else {
            ctx.fill(0, 0, w, H, 0xB8140812);
            ctx.outline(0, 0, w, H, 0xCCFF7AC0);
            ctx.text(font, Component.literal(label), 6, 5, 0xFFFF7AC0);
            ctx.text(font, Component.literal(num), 6, 17, value < 0 ? 0xFF9E9890 : 0xFFFFD24A);
            if (plus > 0) ctx.text(font, Component.literal("+" + num(plus)), w + 4, 17, 0xFF55FF55);
        }
        ps.popMatrix();
    }

    private static String textOnlyLabel(boolean offer) {
        return offer ? "Sacks" : "Sacks (insta)";
    }
}
