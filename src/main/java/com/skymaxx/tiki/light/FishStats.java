package com.skymaxx.tiki.light;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.detect.DropLines;
import com.skymaxx.tiki.config.TikiConfig;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.joml.Matrix3x2fStack;

/**
 * Light: a small one-line fishing stats HUD (Fishing Speed, Sea Creature Chance, Double Hook, Trophy, Treasure).
 * Values come from Hypixel's own displays only: the Stats widget in the tab list (live) or the "Fishing Stats" /
 * "Your SkyBlock Profile" item when you open the SkyBlock menu. Marina is detected from Hypixel's public election
 * API (no key, once per 30 minutes) and the tab list.
 */
public final class FishStats {
    /** Index: 0 Fishing Speed, 1 Sea Creature Chance, 2 Double Hook Chance, 3 Trophy Fish Chance, 4 Treasure Chance. */
    private static final Pattern[] STAT = {
        Pattern.compile("Fishing Speed:?\\s*\\D{0,3}?([\\d,]+(?:\\.\\d+)?)"),
        Pattern.compile("Sea Creature Chance:?\\s*\\D{0,3}?([\\d,]+(?:\\.\\d+)?)"),
        Pattern.compile("Double Hook Chance:?\\s*\\D{0,3}?([\\d,]+(?:\\.\\d+)?)"),
        Pattern.compile("Trophy Fish Chance:?\\s*\\D{0,3}?([\\d,]+(?:\\.\\d+)?)|Trophy Chance:?\\s*\\D{0,3}?([\\d,]+(?:\\.\\d+)?)"),
        Pattern.compile("Treasure Chance:?\\s*\\D{0,3}?([\\d,]+(?:\\.\\d+)?)")};
    private static final String[] ICON = {"§b☂", "§3α", "§9⚓", "§6♔", "§6⛃"};
    /** Each value in the colour of its icon (like Hypixel's stats). */
    private static final String[] COLOR = {"§b", "§3", "§9", "§6", "§6"};
    private static final boolean[] PERCENT = {false, true, true, true, true};
    private static final double[] VALUES = {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    private static long readMs;
    private static volatile boolean marinaApi;
    private static boolean marinaTab;
    private static volatile long electionMs;
    private static volatile boolean fetching;
    private static int tick;
    private static long lastRod;

    private FishStats() {
    }

    private static TikiConfig cfg() {
        return TikiGambleClient.config;
    }

    /** Fishing right now: hook out, or a rod in the hand in the last 15 seconds. */
    public static boolean fishingNow(Minecraft mc) {
        if (mc.player == null) return false;
        long now = System.currentTimeMillis();
        if (mc.player.fishing != null || mc.player.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem) lastRod = now;
        return now - lastRod < 15_000L;
    }

    public static boolean marina() {
        return marinaApi || marinaTab;
    }

    public static void tick(Minecraft mc) {
        TikiConfig c = cfg();
        if (c == null || !c.fishStatsHud || mc.level == null) return;
        if (!loaded) load(c);
        if (++tick % 20 != 0) return;
        try {
            readTab(mc);
            Screen screen = mc.gui == null ? null : mc.gui.screen();
            if (screen instanceof AbstractContainerScreen<?> container) readMenu(container);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.debug("[Light] fishing stats read failed", t);
        }
        if (c.fishStatsMarina && !fetching && System.currentTimeMillis() - electionMs > 30L * 60L * 1000L) fetchElection();
    }

    private static void readTab(Minecraft mc) {
        if (mc.getConnection() == null) return;
        boolean marina = false;
        double[] found = {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        boolean any = false;
        for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
            Component name = info.getTabListDisplayName();
            if (name == null) continue;
            String line = DropLines.stripFormatting(name.getString()).trim();
            if (line.isEmpty()) continue;
            if (line.contains("Marina") && (line.startsWith("Mayor") || line.startsWith("Minister") || line.contains("Perkpocalypse"))) marina = true;
            for (int i = 0; i < STAT.length; i++) {
                if (!Double.isNaN(found[i])) continue;
                double v = match(i, line);
                if (!Double.isNaN(v)) {
                    found[i] = v;
                    any = true;
                }
            }
        }
        marinaTab = marina;
        if (any) take(found);
    }

    /** The "Fishing Stats" item (stats menu) or "Your SkyBlock Profile" item in the SkyBlock menu. */
    private static void readMenu(AbstractContainerScreen<?> container) {
        for (Slot slot : container.getMenu().slots) {
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory) continue;
            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) continue;
            String title = DropLines.stripFormatting(stack.getHoverName().getString()).trim();
            if (!title.equals("Fishing Stats") && !title.equals("Your SkyBlock Profile")) continue;
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            double[] found = {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
            boolean any = false;
            for (Component l : lore.lines()) {
                String line = DropLines.stripFormatting(l.getString()).trim();
                for (int i = 0; i < STAT.length; i++) {
                    if (!Double.isNaN(found[i])) continue;
                    double v = match(i, line);
                    if (!Double.isNaN(v)) {
                        found[i] = v;
                        any = true;
                    }
                }
            }
            if (any) take(found);
        }
    }

    private static double match(int i, String line) {
        Matcher m = STAT[i].matcher(line);
        if (!m.find()) return Double.NaN;
        String g = m.group(1) != null ? m.group(1) : (m.groupCount() > 1 ? m.group(2) : null);
        if (g == null) return Double.NaN;
        try {
            return Double.parseDouble(g.replace(",", ""));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static void take(double[] found) {
        for (int i = 0; i < found.length; i++) if (!Double.isNaN(found[i])) VALUES[i] = found[i];
        readMs = System.currentTimeMillis();
        save();
    }

    private static boolean loaded;

    /** The last values from the config, so the HUD shows them right after a restart. */
    private static void load(TikiConfig c) {
        loaded = true;
        if (c.fishStatsSaved == null || c.fishStatsSaved.isBlank()) return;
        String[] p = c.fishStatsSaved.split(",", -1);
        for (int i = 0; i < VALUES.length && i < p.length; i++) {
            try {
                if (!p[i].isBlank() && Double.isNaN(VALUES[i])) VALUES[i] = Double.parseDouble(p[i]);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /** Writes the values to the config, only when they changed. */
    private static void save() {
        TikiConfig c = cfg();
        if (c == null) return;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < VALUES.length; i++) {
            if (i > 0) b.append(',');
            if (!Double.isNaN(VALUES[i])) b.append(VALUES[i]);
        }
        String s = b.toString();
        if (s.equals(c.fishStatsSaved)) return;
        c.fishStatsSaved = s;
        c.save();
    }

    /** Hypixel's public election resource (no key): is Marina mayor or minister? */
    private static void fetchElection() {
        fetching = true;
        electionMs = System.currentTimeMillis();
        Thread th = new Thread(() -> {
            try {
                java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://api.hypixel.net/v2/resources/skyblock/election"))
                    .timeout(java.time.Duration.ofSeconds(8)).header("User-Agent", "Light-Mod (Fabric, fishing stats)").GET().build();
                java.net.http.HttpResponse<String> res = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) return;
                JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
                boolean m = false;
                if (root.has("mayor") && root.get("mayor").isJsonObject()) {
                    JsonObject mayor = root.getAsJsonObject("mayor");
                    m = named(mayor, "Marina");
                    if (mayor.has("minister") && mayor.get("minister").isJsonObject()) m |= named(mayor.getAsJsonObject("minister"), "Marina");
                }
                marinaApi = m;
            } catch (Throwable t) {
                TikiGambleClient.LOGGER.debug("[Light] election not loaded", t);
                electionMs = System.currentTimeMillis() - 25L * 60L * 1000L; // try again in 5 minutes
            } finally {
                fetching = false;
            }
        }, "Light election");
        th.setDaemon(true);
        th.start();
    }

    private static boolean named(JsonObject o, String name) {
        JsonElement n = o.get("name");
        return n != null && n.isJsonPrimitive() && name.equalsIgnoreCase(n.getAsString());
    }

    // ------------------------------------------------------------------ HUD

    private static boolean shown(TikiConfig c, int i) {
        return switch (i) {
            case 0 -> c.fishStatSpeed;
            case 1 -> c.fishStatScc;
            case 2 -> c.fishStatDh;
            case 3 -> c.fishStatTrophy;
            default -> c.fishStatTreasure;
        };
    }

    private static String fmt(double v, boolean pct) {
        String s = v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        return pct ? s + "%" : s;
    }

    /** The line as shown, e.g. "☂200 α72.93% ⚓5.3%  [Marina]". sample = values for the move screen. */
    public static String line(TikiConfig c, boolean sample) {
        if (!loaded) load(c);
        double[] demo = {200, 72.93, 5.3, 26.5, 18.44};
        StringBuilder b = new StringBuilder();
        boolean have = false;
        for (int i = 0; i < VALUES.length; i++) {
            if (!shown(c, i)) continue;
            double v = sample && Double.isNaN(VALUES[i]) ? demo[i] : VALUES[i];
            if (b.length() > 0) b.append("  ");
            b.append(ICON[i]).append(' ').append(COLOR[i]).append(Double.isNaN(v) ? "?" : fmt(v, PERCENT[i]));
            if (!Double.isNaN(v)) have = true;
        }
        if (c.fishStatsMarina && (marina() || sample)) b.append("  §b[Marina]");
        if (!have && !sample) return "Fishing stats: §7show Stats in /tab or open your SkyBlock menu";
        return b.toString();
    }

    public static int boxX(TikiConfig c, int sw) {
        return c.fishStatsX < 0 ? 6 : Math.round(c.fishStatsX * sw);
    }

    public static int boxY(TikiConfig c, int sh) {
        return c.fishStatsY < 0 ? 42 : Math.round(c.fishStatsY * sh);
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker dt) {
        TikiConfig c = cfg();
        if (c == null || !c.fishStatsHud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || LavaScene.ownsScreen()) return;
        if (c.fishStatsOnlyFishing && !fishingNow(mc)) return;
        if (HudVis.hidden()) return;
        try {
            String text = line(c, false);
            int bx = boxX(c, ctx.guiWidth()), by = boxY(c, ctx.guiHeight());
            Bestiary.draw(ctx, mc.font, bx, by, c.fishStatsScale, text);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] fishing stats HUD failed", t);
        }
    }

    public static int[] size(Font font, TikiConfig c) {
        return Bestiary.size(font, line(c, true));
    }
}
