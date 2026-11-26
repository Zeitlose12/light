package com.skymaxx.tiki.light;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skymaxx.tiki.TikiGambleClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Light: every result of the gambling animations (Jawbus, Thunder, Ragnarok, Plhlegblast, Grim Reaper, Yeti,
 * Reindrake) - drop, price, dry streak. Saved to config/light/gamble_log.json. Test entries (previews and
 * /light test ...) live only in memory and never touch the real log, stats or achievements.
 */
public final class GambleLog {
    public static final String[] MOBS = {"jawbus", "thunder", "ragnarok", "plhlegblast", "grim", "yeti", "reindrake"};
    public static final String[] LAVA = {"jawbus", "thunder", "ragnarok", "plhlegblast"};
    private static final int MAX_ENTRIES = 1000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** One gambling result. drop == null: no rare drop. price < 0: not known. */
    public static final class Entry {
        public long ms;
        public String mob;
        public String mobName;
        public String drop;
        public long price = -1L;
        public boolean share;
        public long dry;
        public boolean fanfare;
        public transient boolean test;
    }

    private static final class Data {
        List<Entry> entries = new ArrayList<>();
        Map<String, Long> kills = new HashMap<>();
        Map<String, Long> killsAtDrop = new HashMap<>();
        Map<String, Long> killsAtItem = new HashMap<>();
        Map<String, Long> longestDry = new HashMap<>();
    }

    private static Data data;
    private static final List<Entry> TEST = new ArrayList<>();
    /** /light test dry: the next scene shows this dry streak. */
    public static long dryOverride = -1L;
    private static final long SESSION_START = System.currentTimeMillis();

    private GambleLog() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(LightFiles.FOLDER).resolve("gamble_log.json");
    }

    private static Data data() {
        if (data != null) return data;
        Data d = null;
        try {
            Path f = file();
            if (Files.exists(f)) d = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Data.class);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] could not read gamble_log.json", t);
        }
        if (d == null) d = new Data();
        if (d.entries == null) d.entries = new ArrayList<>();
        if (d.kills == null) d.kills = new HashMap<>();
        if (d.killsAtDrop == null) d.killsAtDrop = new HashMap<>();
        if (d.killsAtItem == null) d.killsAtItem = new HashMap<>();
        if (d.longestDry == null) d.longestDry = new HashMap<>();
        data = d;
        return d;
    }

    public static void save() {
        if (data == null) return;
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] could not save gamble_log.json", t);
        }
    }

    private static String itemKey(String mob, String drop) {
        return mob + "|" + drop.replaceAll("§.", "").trim().toLowerCase(Locale.ROOT);
    }

    /** Records one result and returns its entry (with the dry streak). Test results only go to the test list. */
    public static Entry record(String mob, String mobName, String drop, boolean share, boolean test) {
        Data d = data();
        Entry e = new Entry();
        e.ms = System.currentTimeMillis();
        e.mob = mob;
        e.mobName = mobName;
        e.drop = drop;
        e.share = share;
        e.test = test;
        long kills = d.kills.getOrDefault(mob, 0L) + 1;
        if (drop == null) {
            e.dry = kills - d.killsAtDrop.getOrDefault(mob, 0L);
        } else {
            e.dry = kills - d.killsAtItem.getOrDefault(itemKey(mob, drop), 0L);
        }
        if (dryOverride >= 0) {
            e.dry = dryOverride;
            dryOverride = -1L;
        }
        if (test) {
            TEST.add(0, e);
            return e;
        }
        d.kills.put(mob, kills);
        if (drop == null) {
            d.longestDry.merge(mob, e.dry, Math::max);
        } else {
            d.killsAtDrop.put(mob, kills);
            d.killsAtItem.put(itemKey(mob, drop), kills);
        }
        d.entries.add(0, e);
        while (d.entries.size() > MAX_ENTRIES) d.entries.remove(d.entries.size() - 1);
        save();
        return e;
    }

    public static void setPrice(Entry e, long price) {
        if (e == null || price <= 0 || e.price == price) return;
        e.price = price;
        if (!e.test) save();
    }

    public static void setFanfare(Entry e) {
        if (e == null || e.fanfare) return;
        e.fanfare = true;
        if (!e.test) save();
    }

    /** Real entries, newest first (plus test entries first while there are any). */
    public static List<Entry> entries() {
        List<Entry> out = new ArrayList<>(TEST);
        out.addAll(data().entries);
        return out;
    }

    public static boolean hasTest() {
        return !TEST.isEmpty();
    }

    public static void clearTest() {
        TEST.clear();
        dryOverride = -1L;
    }

    /** Fills the test list with a few made-up results (for /light test history and the journal). */
    public static void fillTest() {
        Object[][] demo = {
            {"jawbus", "Lord Jawbus", "Radioactive Vial", 28_700_000L, 412L, true},
            {"thunder", "Thunder", null, -1L, 14L, false},
            {"ragnarok", "Ragnarok", "Burnt Texts", 2_150_000L, 23L, false},
            {"yeti", "Yeti", "Baby Yeti", 41_000_000L, 137L, true},
            {"plhlegblast", "Plhlegblast", null, -1L, 9L, false},
            {"grim", "Grim Reaper", "Deep Sea Orb", 6_900_000L, 9L, false},
            {"reindrake", "Reindrake", "Iceberg Dye", 35_500_000L, 61L, true},
        };
        long now = System.currentTimeMillis();
        for (int i = 0; i < demo.length; i++) {
            Entry e = new Entry();
            e.ms = now - i * 1_000_000L;
            e.mob = (String) demo[i][0];
            e.mobName = (String) demo[i][1];
            e.drop = (String) demo[i][2];
            e.price = (Long) demo[i][3];
            e.dry = (Long) demo[i][4];
            e.fanfare = (Boolean) demo[i][5];
            e.test = true;
            TEST.add(e);
        }
    }

    /** Clears the whole real log (History tab button). */
    public static void clearAll() {
        data = new Data();
        save();
    }

    // ------------------------------------------------------------------ numbers for History, Journal, Achievements

    public static long total(boolean withTest) {
        long sum = 0;
        for (Entry e : withTest ? entries() : data().entries) if (e.drop != null && e.price > 0) sum += e.price;
        return sum;
    }

    public static Entry best(boolean withTest) {
        Entry best = null;
        for (Entry e : withTest ? entries() : data().entries) if (e.drop != null && e.price > 0 && (best == null || e.price > best.price)) best = e;
        return best;
    }

    /** Coins per hour of this game session (since the game started, at least 15 minutes counted). */
    public static long perHour() {
        long sum = 0;
        for (Entry e : data().entries) if (e.ms >= SESSION_START && e.drop != null && e.price > 0) sum += e.price;
        double hours = Math.max(0.25, (System.currentTimeMillis() - SESSION_START) / 3_600_000.0);
        return Math.round(sum / hours);
    }

    public static int count(boolean withTest) {
        return (withTest ? entries() : data().entries).size();
    }

    public static int drops(String mob, boolean withTest) {
        int n = 0;
        for (Entry e : withTest ? entries() : data().entries) if (e.drop != null && (mob == null || mob.equals(e.mob))) n++;
        return n;
    }

    public static boolean dropped(String mob, String itemPart) {
        for (Entry e : data().entries) {
            if (e.drop == null || !mob.equals(e.mob)) continue;
            if (itemPart == null || e.drop.toLowerCase(Locale.ROOT).contains(itemPart)) return true;
        }
        return false;
    }

    public static long bestPrice() {
        Entry b = best(false);
        return b == null ? 0 : b.price;
    }

    public static long longestDry(String[] mobs) {
        long m = 0;
        for (String mob : mobs) m = Math.max(m, data().longestDry.getOrDefault(mob, 0L));
        return m;
    }

    public static int fanfares(boolean withTest) {
        int n = 0;
        for (Entry e : withTest ? entries() : data().entries) if (e.fanfare) n++;
        return n;
    }

    /** Kills of this mob without any drop, counting from the last drop (or the start of the log). */
    public static long sinceDrop(String mob) {
        Data d = data();
        return d.kills.getOrDefault(mob, 0L) - d.killsAtDrop.getOrDefault(mob, 0L);
    }
}
