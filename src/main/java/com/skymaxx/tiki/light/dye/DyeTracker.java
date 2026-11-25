package com.skymaxx.tiki.light.dye;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.detect.DropLines;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Light: fishing dye tracker. Takes your Sea Creature kills from the SkyBlock Profile Viewer (/pv)
 * menu and your Magic Find from the SkyBlock menu whenever you open them, and
 * notices your own dye drops in chat. Nothing is sent anywhere.
 */
public final class DyeTracker {
    private static final Pattern MAGIC_FIND = Pattern.compile("(?i)magic find\\D{0,6}([\\d,]+(?:\\.\\d+)?)");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data = new Data();
    private static File file;
    private static int tick;
    private static boolean dirty;

    /** Saved state. */
    public static final class Data {
        public Map<String, Long> kills = new LinkedHashMap<String, Long>();
        public double magicFind = -1.0;
        public long bestiaryReadMs = 0L;
        public Map<String, Integer> found = new LinkedHashMap<String, Integer>();
        public Map<String, Long> killsAtLastDrop = new LinkedHashMap<String, Long>();
        /** Catches counted live since the last Bestiary read, per creature. */
        public Map<String, Long> live = new LinkedHashMap<String, Long>();
        /** Loot share kills per creature, from the Profile Viewer (bestiary minus own kills). */
        public Map<String, Long> lootShare = new LinkedHashMap<String, Long>();
        public String pvProfile = "";
        public long pvReadMs = 0L;
        /** SkyBlock year (Light's own count) in which Vincent's rotation was read; 0 = never. */
        public long vincentYear = 0L;
    }

    /** Everything the HUD and the menu show for one dye. */
    public record Progress(Dye dye, long kills, double expected, double chance, int found, long sinceLast, String bestCreature, double bestChance, long lootShare) {
        /** Average chance per kill with your current mix of creatures. */
        public double mixChance() {
            double eff = this.kills + this.lootShare / 5.0;
            return eff <= 0 ? this.bestChance : this.expected / eff;
        }

        /** Total kills (at your mix) after which you reach the given chance. */
        public long killsFor(double target) {
            double p = this.mixChance();
            return p <= 0 ? -1L : (long)Math.ceil(Math.log(1.0 - target) / Math.log1p(-p));
        }

        /** Kills of the best creature alone for the given chance. */
        public long bestKillsFor(double target) {
            return this.bestChance <= 0 ? -1L : (long)Math.ceil(Math.log(1.0 - target) / Math.log1p(-this.bestChance));
        }
    }

    private DyeTracker() {
    }

    public static void load() {
        file = FabricLoader.getInstance().getConfigDir().resolve("light").resolve("dyes.json").toFile();
        if (!file.isFile()) {
            return;
        }
        try (FileReader r = new FileReader(file)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) {
                data = d;
                if (data.kills == null) data.kills = new LinkedHashMap<String, Long>();
                if (data.found == null) data.found = new LinkedHashMap<String, Integer>();
                if (data.killsAtLastDrop == null) data.killsAtLastDrop = new LinkedHashMap<String, Long>();
                if (data.live == null) data.live = new LinkedHashMap<String, Long>();
                if (data.lootShare == null) data.lootShare = new LinkedHashMap<String, Long>();
                if (data.pvProfile == null) data.pvProfile = "";
                if (data.pvReadMs == 0L && !data.kills.isEmpty()) {
                    data.kills.clear(); // old Bestiary-menu numbers included loot share - /pv replaces them
                    data.bestiaryReadMs = 0L;
                    dirty = true;
                }
            }
        } catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[Light] could not read dyes.json", (Throwable)e);
        }
    }

    private static void save() {
        if (file == null) {
            return;
        }
        try {
            file.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(file)) {
                GSON.toJson(data, w);
            }
        } catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[Light] could not save dyes.json", (Throwable)e);
        }
    }

    public static Data data() {
        return data;
    }

    /** Called every client tick; reads open menus a few times per second. */
    public static void tick(Minecraft mc) {
        DyeTracker.watchCatches(mc);
        if (++tick % 5 == 0) {
            Screen screen = mc.gui == null ? null : mc.gui.screen();
            if (screen instanceof AbstractContainerScreen<?> container) {
                DyeTracker.scan(container);
            }
        }
        if (tick % 200 == 0) {
            DyeTracker.checkVincentYear();
        }
        if (dirty && tick % 40 == 0) {
            dirty = false;
            DyeTracker.save();
        }
    }

    private static final Pattern ROTATION = Pattern.compile("(?i)This dye is\\s*(\\d+)x as common during SkyBlock Year\\s*(\\d+)");
    private static long lastVincentNoticeMs;

    /** SkyBlock year count (124 real hours per year). Only compared with itself, so an offset does not matter. */
    static long skyblockYear() {
        return (System.currentTimeMillis() / 1000L - 1560275700L) / 446400L + 1L;
    }

    private static void checkVincentYear() {
        if (data.vincentYear > 0L && data.vincentYear != DyeTracker.skyblockYear()) {
            data.vincentYear = 0L;
            for (Dye d : Dye.values()) {
                DyeTracker.setVincent(d, 1);
            }
            if (TikiGambleClient.config != null) TikiGambleClient.config.save();
            dirty = true;
            TikiGambleClient.chat("\u00a7d[Light] \u00a7eNew SkyBlock year - Vincent's dye bonus was reset to 1x. Talk to Vincent or do /dyes to load it again.");
        }
    }

    public static boolean vincentLoaded() {
        return data.vincentYear > 0L;
    }

    /** Vincent's "Dyes" menu: heads with "This dye is 2x as common during SkyBlock Year N". */
    private static void readVincent(AbstractContainerScreen<?> container) {
        String title = DropLines.stripFormatting(container.getTitle().getString());
        if (!title.contains("Dyes")) {
            return;
        }
        Map<Dye, Integer> found = new java.util.EnumMap<Dye, Integer>(Dye.class);
        int rotation = 0;
        for (Slot slot : container.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) continue;
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            StringBuilder text = new StringBuilder();
            for (Component line : lore.lines()) {
                text.append(DropLines.stripFormatting(line.getString())).append(' ');
            }
            Matcher m = ROTATION.matcher(text.toString().replaceAll("\\s+", " "));
            if (!m.find()) continue;
            ++rotation;
            String name = DropLines.stripFormatting(stack.getHoverName().getString()).trim().toLowerCase(Locale.ROOT);
            for (Dye d : Dye.values()) {
                if (name.contains(d.shortName.toLowerCase(Locale.ROOT))) {
                    found.put(d, Integer.parseInt(m.group(1)));
                }
            }
        }
        if (rotation == 0) {
            return;
        }
        boolean changed = data.vincentYear != DyeTracker.skyblockYear();
        for (Dye d : Dye.values()) {
            int v = found.getOrDefault(d, 1);
            if (DyeTracker.vincent(d) != v) {
                DyeTracker.setVincent(d, v);
                changed = true;
            }
        }
        data.vincentYear = DyeTracker.skyblockYear();
        if (changed) {
            dirty = true;
            if (TikiGambleClient.config != null) TikiGambleClient.config.save();
            long now = System.currentTimeMillis();
            if (now - lastVincentNoticeMs > 4000L) {
                lastVincentNoticeMs = now;
                StringBuilder b = new StringBuilder();
                for (Dye d : Dye.values()) {
                    b.append(b.length() == 0 ? "" : ", ").append(d.shortName).append(' ').append(DyeTracker.vincent(d)).append('x');
                }
                TikiGambleClient.chat("\u00a7d[Light] \u00a77Vincent's dye bonus loaded: \u00a7f" + b);
            }
        }
    }

    /** Called on the client thread with own / loot share kills per creature from the Profile Viewer. */
    static void applyProfileViewer(String profileName, Map<String, long[]> split) {
        for (Map.Entry<String, long[]> e : split.entrySet()) {
            data.kills.put(e.getKey(), e.getValue()[0]);
            data.lootShare.put(e.getKey(), e.getValue()[1]);
            data.live.remove(e.getKey());
        }
        data.pvProfile = profileName == null ? "" : profileName;
        data.pvReadMs = System.currentTimeMillis();
        dirty = true;
        DyeTracker.save();
        long ls = 0L;
        for (long[] v : split.values()) ls += v[1];
        TikiGambleClient.chat("\u00a7d[Light] \u00a77Dye tracker: Profile Viewer data loaded (" + data.pvProfile + ") - own kills and " + String.format("%,d", ls) + " loot share kills split.");
    }

    private static void scan(AbstractContainerScreen<?> container) {
        DyeTracker.readVincent(container);
        for (Slot slot : container.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) continue;
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            StringBuilder text = new StringBuilder();
            for (Component line : lore.lines()) {
                text.append(DropLines.stripFormatting(line.getString())).append('\n');
            }
            String all = text.toString();
            String name = DropLines.stripFormatting(stack.getHoverName().getString()).trim();
            DyeTracker.readMagicFind(name, all);
        }
    }

    private static void readMagicFind(String name, String lore) {
        String n = name.toLowerCase(Locale.ROOT);
        if (!n.contains("profile") && !n.contains("stats")) {
            return;
        }
        Matcher m = MAGIC_FIND.matcher(lore);
        if (!m.find()) {
            return;
        }
        try {
            double mf = Double.parseDouble(m.group(1).replace(",", ""));
            if (mf != data.magicFind) {
                data.magicFind = mf;
                dirty = true;
            }
        } catch (NumberFormatException ignored) {
        }
    }

    /** Own dye drops in chat: "RARE DROP! Aquamarine Dye", "WOW! <you> found ..." etc. */
    public static void onChat(Component message, boolean overlay) {
        if (overlay || message == null) {
            return;
        }
        String line = DropLines.stripFormatting(message.getString()).trim();
        String lower = line.toLowerCase(Locale.ROOT);
        for (Dye dye : Dye.values()) {
            int at = lower.indexOf(dye.itemName.toLowerCase(Locale.ROOT));
            if (at < 0) continue;
            if (line.lastIndexOf(':', at) >= 0 && !lower.startsWith("rare drop") && !lower.contains("drop!")) {
                return; // someone typed it in chat
            }
            Minecraft mc = Minecraft.getInstance();
            String me = mc.getUser() == null ? "" : mc.getUser().getName().toLowerCase(Locale.ROOT);
            boolean mine = lower.contains("drop!") && !lower.contains(" found ") || !me.isEmpty() && lower.contains(me);
            if (!mine) {
                return;
            }
            data.found.merge(dye.name(), 1, Integer::sum);
            data.killsAtLastDrop.put(dye.name(), DyeTracker.eligibleKills(dye));
            dirty = true;
            DyeTracker.save();
            TikiGambleClient.chat("§d[Light] §f" + dye.itemName + " recorded - the dye tracker starts counting again.");
            return;
        }
    }

    public static long eligibleKills(Dye dye) {
        long sum = 0L;
        for (String c : DyeOdds.creatures(dye)) {
            sum += DyeTracker.killsOf(c);
        }
        return sum;
    }

    /** Kills from the Profile Viewer plus the catches counted live since then. */
    public static long killsOf(String creature) {
        return data.kills.getOrDefault(creature, 0L) + data.live.getOrDefault(creature, 0L);
    }

    // ---- live catch counting: a known sea creature that appears right at your bobber is your catch ----
    private static final double CATCH_RADIUS_SQ = 8.0 * 8.0;
    private static final long HOOK_MEMORY_MS = 3000L;
    private static final java.util.Set<String> MULTI_BODY = java.util.Set.of("Titanoboa");
    private static final Map<Integer, Long> SEEN = new java.util.HashMap<Integer, Long>();
    private static final Map<String, Long> LAST_COUNT = new java.util.HashMap<String, Long>();
    private static net.minecraft.world.phys.Vec3 lastHook;
    private static long lastHookMs;

    private static void watchCatches(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            SEEN.clear();
            lastHook = null;
            return;
        }
        long now = System.currentTimeMillis();
        if (mc.player.fishing != null) {
            lastHook = mc.player.fishing.position();
            lastHookMs = now;
        }
        SEEN.values().removeIf(t -> now - t > 120000L);
        boolean hookRecent = lastHook != null && now - lastHookMs <= HOOK_MEMORY_MS;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (!e.hasCustomName() || SEEN.containsKey(e.getId())) continue;
            Component custom = e.getCustomName();
            String text = custom == null ? "" : custom.getString();
            if (!text.contains("Lv")) continue;
            SEEN.put(e.getId(), now);
            if (!hookRecent) continue;
            String creature = DyeOdds.creatureIn(text);
            if (creature == null) continue;
            double dx = e.getX() - lastHook.x;
            double dz = e.getZ() - lastHook.z;
            if (dx * dx + dz * dz > CATCH_RADIUS_SQ) continue;
            if (MULTI_BODY.contains(creature) && now - LAST_COUNT.getOrDefault(creature, 0L) < 5000L) continue;
            LAST_COUNT.put(creature, now);
            data.live.merge(creature, 1L, Long::sum);
            dirty = true;
            if (TikiGambleClient.config != null && TikiGambleClient.config.debug) {
                TikiGambleClient.LOGGER.info("[Light] live catch: {}", creature);
            }
        }
    }

    /** Vincent's dye bonus from the config: 1, 2 or 3. */
    public static int vincent(Dye dye) {
        com.skymaxx.tiki.config.TikiConfig c = TikiGambleClient.config;
        if (c == null) {
            return 1;
        }
        int v = switch (dye) {
            case AQUAMARINE -> c.dyeVincentAquamarine;
            case ICEBERG -> c.dyeVincentIceberg;
            case CARMINE -> c.dyeVincentCarmine;
            case MIDNIGHT -> c.dyeVincentMidnight;
        };
        return Math.max(1, Math.min(3, v));
    }

    public static void setVincent(Dye dye, int v) {
        com.skymaxx.tiki.config.TikiConfig c = TikiGambleClient.config;
        if (c == null) {
            return;
        }
        v = Math.max(1, Math.min(3, v));
        switch (dye) {
            case AQUAMARINE -> c.dyeVincentAquamarine = v;
            case ICEBERG -> c.dyeVincentIceberg = v;
            case CARMINE -> c.dyeVincentCarmine = v;
            case MIDNIGHT -> c.dyeVincentMidnight = v;
        }
    }

    /** Magic Find and Vincent multiplier, the way DyeAddons does it: (1 + MF/100) x Vincent. */
    public static double multiplier(Dye dye) {
        return (1.0 + Math.max(0.0, data.magicFind) / 100.0) * DyeTracker.vincent(dye);
    }

    public static Progress progress(Dye dye) {
        List<String> creatures = DyeOdds.creatures(dye);
        double mult = DyeTracker.multiplier(dye);
        long kills = 0L;
        long lootShare = 0L;
        double expected = 0.0;
        double logMiss = 0.0;
        String best = null;
        double bestChance = 0.0;
        for (String c : creatures) {
            long k = DyeTracker.killsOf(c);
            double p = Math.min(1.0, DyeOdds.chance(c, dye) * mult);
            long ls = data.lootShare.getOrDefault(c, 0L);
            kills += k;
            lootShare += ls;
            expected += k * p + ls * p / 5.0;
            logMiss += k * Math.log1p(-p) + ls * Math.log1p(-p / 5.0);
            if (p > bestChance) {
                bestChance = p;
                best = c;
            }
        }
        double chance = 1.0 - Math.exp(logMiss);
        int found = data.found.getOrDefault(dye.name(), 0);
        long since = Math.max(0L, kills - data.killsAtLastDrop.getOrDefault(dye.name(), 0L));
        return new Progress(dye, kills, expected, chance, found, since, best, bestChance, lootShare);
    }

    public static boolean hasData() {
        return data.pvReadMs > 0L || !data.live.isEmpty();
    }

    public static void reset() {
        data = new Data();
        DyeTracker.save();
    }
}
