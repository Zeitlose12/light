package com.skymaxx.tiki.light;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.detect.DropLines;
import com.skymaxx.tiki.config.TikiConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.joml.Matrix3x2fStack;

/**
 * Light: Bestiary rankings. Hypixel shows "Your Wiki Tiki Ranking - #3,012 out of 8,257,853 players" on an item in
 * the Bestiary page of a mob. Light only reads that item while YOU have the page open (no clicks, no requests)
 * and remembers it, for the small ranking HUD and the Journal.
 */
public final class Bestiary {
    private static final Pattern NAME = Pattern.compile("^Your (.+) Ranking$");
    private static final Pattern RANK = Pattern.compile("#([\\d,.]+) out of ([\\d,.]+) players");
    public static final long STALE_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, Rank> data;
    private static int tick;

    /** One mob: place, players, when read; prev = the place before the last change. */
    public static final class Rank {
        public String mob;
        public long rank;
        public long total;
        public long ms;
        public long prev = -1L;
        public long prevMs;

        public boolean stale() {
            return System.currentTimeMillis() - this.ms > STALE_MS;
        }

        /** Places gained since the reading before (positive = better). */
        public long change() {
            return this.prev > 0 ? this.prev - this.rank : 0;
        }

        public String top() {
            if (this.total <= 0) return "";
            double p = 100.0 * this.rank / this.total;
            return "Top " + (p < 0.1 ? String.format(Locale.ROOT, "%.2f", p) : p < 10 ? String.format(Locale.ROOT, "%.1f", p) : String.valueOf(Math.round(p))) + "%";
        }
    }

    private Bestiary() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(LightFiles.FOLDER).resolve("bestiary.json");
    }

    private static Map<String, Rank> data() {
        if (data == null) {
            data = new LinkedHashMap<>();
            try {
                Path f = file();
                if (Files.isRegularFile(f)) {
                    Map<String, Rank> read = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), new TypeToken<LinkedHashMap<String, Rank>>() {}.getType());
                    if (read != null) data.putAll(read);
                }
            } catch (Throwable t) {
                TikiGambleClient.LOGGER.warn("[Light] bestiary.json could not be read", t);
            }
        }
        return data;
    }

    private static void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(data()), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] bestiary.json could not be saved", t);
        }
    }

    /** "Wiki Tiki" -> its ranking, or null. */
    public static Rank get(String mobName) {
        return mobName == null ? null : data().get(mobName.toLowerCase(Locale.ROOT));
    }

    public static void tick(Minecraft mc) {
        if (++tick % 10 != 0) return;
        Screen screen = mc.gui == null ? null : mc.gui.screen();
        if (!(screen instanceof AbstractContainerScreen<?> container)) return;
        try {
            for (Slot slot : container.getMenu().slots) {
                if (slot.container instanceof net.minecraft.world.entity.player.Inventory) continue;
                ItemStack stack = slot.getItem();
                if (stack == null || stack.isEmpty()) continue;
                Matcher n = NAME.matcher(DropLines.stripFormatting(stack.getHoverName().getString()).trim());
                if (!n.matches()) continue;
                ItemLore lore = stack.get(DataComponents.LORE);
                if (lore == null) continue;
                for (Component line : lore.lines()) {
                    Matcher m = RANK.matcher(DropLines.stripFormatting(line.getString()));
                    if (!m.find()) continue;
                    store(n.group(1).trim(), num(m.group(1)), num(m.group(2)));
                    break;
                }
            }
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.debug("[Light] bestiary scan failed", t);
        }
    }

    private static long num(String s) {
        return Long.parseLong(s.replace(",", "").replace(".", ""));
    }

    private static void store(String mob, long rank, long total) {
        if (rank <= 0) return;
        String key = mob.toLowerCase(Locale.ROOT);
        Rank r = data().get(key);
        long now = System.currentTimeMillis();
        if (r == null) {
            r = new Rank();
            r.mob = mob;
            data().put(key, r);
        } else if (r.rank == rank && r.total == total) {
            if (now - r.ms > 60_000L) {
                r.ms = now;
                save();
            }
            return;
        } else if (r.rank != rank) {
            r.prev = r.rank;
            r.prevMs = r.ms;
        }
        r.rank = rank;
        r.total = total;
        r.ms = now;
        save();
    }

    // ------------------------------------------------------------------ HUD: one line

    /** The mob of your last Wiki Tiki / Titanoboa / gambling kill (or the newest ranking). */
    private static String shownMob() {
        TikiConfig c = TikiGambleClient.config;
        if (c != null && c.bestiaryHudMob != null && !c.bestiaryHudMob.isEmpty()) {
            for (int i = 0; i < GambleLog.JOURNAL.length; i++) if (GambleLog.JOURNAL[i].equals(c.bestiaryHudMob)) return GambleLog.JOURNAL_NAMES[i];
        }
        for (GambleLog.Entry e : GambleLog.entries()) {
            if (!e.test && e.mobName != null) return e.mobName;
        }
        Rank newest = null;
        for (String n : GambleLog.JOURNAL_NAMES) {
            Rank r = get(n); // only the mobs with an animation
            if (r != null && (newest == null || r.ms > newest.ms)) newest = r;
        }
        return newest == null ? GambleLog.JOURNAL_NAMES[0] : newest.mob; // nothing yet: Wiki Tiki
    }

    public static int boxX(TikiConfig c, int sw) {
        return c.bestiaryHudX < 0 ? 6 : Math.round(c.bestiaryHudX * sw);
    }

    public static int boxY(TikiConfig c, int sh) {
        return c.bestiaryHudY < 0 ? 60 : Math.round(c.bestiaryHudY * sh);
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker dt) {
        TikiConfig c = TikiGambleClient.config;
        if (c == null || !c.bestiaryHud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || LavaScene.ownsScreen()) return;
        if (c.bestiaryHudOnlyFishing && !FishStats.fishingNow(mc)) return;
        try {
            if (HudVis.hidden()) return;
            String mob = shownMob();
            if (mob == null) return;
            String text = line(mob, get(mob));
            int bx = boxX(c, ctx.guiWidth()), by = boxY(c, ctx.guiHeight());
            draw(ctx, mc.font, bx, by, c.bestiaryHudScale, text);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] bestiary HUD failed", t);
        }
    }

    public static String line(String mob, Rank r) {
        if (r == null) return mob + " §8· open /be once";
        StringBuilder b = new StringBuilder(mob).append(" §b#").append(String.format(Locale.ROOT, "%,d", r.rank));
        long ch = r.change();
        if (ch > 0) b.append(" §a↑").append(String.format(Locale.ROOT, "%,d", ch));
        else if (ch < 0) b.append(" §c↓").append(String.format(Locale.ROOT, "%,d", -ch));
        if (r.stale()) b.append(" §6●");
        return b.toString();
    }

    public static int[] size(Font font, String text) {
        return new int[]{font.width(text) + 12, 14};
    }

    public static void draw(GuiGraphicsExtractor ctx, Font font, int x, int y, float scale, String text) {
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(x, y);
        ps.scale(scale, scale);
        int[] s = size(font, text);
        Theme.hud().hudBox(ctx, 0, 0, s[0], s[1]);
        ctx.text(font, Component.literal(text), 6, 3, 0xFFD2D8E0); // unformatted parts (the mob name) in a soft light grey
        ps.popMatrix();
    }

    /** Sample line for the move screen. */
    public static String sample() {
        String mob = shownMob();
        Rank r = mob == null ? null : get(mob);
        return r != null ? line(mob, r) : "Wiki Tiki §b#3,012 §a↑37";
    }

    /** Label for the HUDs tab button. */
    public static String mobLabel(String key) {
        if (key == null || key.isEmpty()) return "Last killed";
        for (int i = 0; i < GambleLog.JOURNAL.length; i++) if (GambleLog.JOURNAL[i].equals(key)) return GambleLog.JOURNAL_NAMES[i];
        return "Last killed";
    }

    /** Next choice: last killed -> Wiki Tiki -> Titanoboa -> ... -> last killed. */
    public static String nextMob(String key) {
        if (key == null || key.isEmpty()) return GambleLog.JOURNAL[0];
        for (int i = 0; i < GambleLog.JOURNAL.length; i++) {
            if (GambleLog.JOURNAL[i].equals(key)) return i + 1 < GambleLog.JOURNAL.length ? GambleLog.JOURNAL[i + 1] : "";
        }
        return "";
    }

    /** All readings, best first (Journal page). */
    public static java.util.List<Rank> all() {
        java.util.List<Rank> out = new java.util.ArrayList<>(data().values());
        out.sort((a, b) -> Double.compare(a.total > 0 ? (double) a.rank / a.total : 1, b.total > 0 ? (double) b.rank / b.total : 1));
        return out;
    }
}
