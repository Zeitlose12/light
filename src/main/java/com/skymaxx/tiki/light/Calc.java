package com.skymaxx.tiki.light;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.detect.DropLines;
import com.skymaxx.tiki.light.dye.Dye;
import com.skymaxx.tiki.light.dye.DyeTracker;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/**
 * Light: drop chance calculator (Calculator tab and /light chance).
 * Base chances after Hypixel SkyBlock 0.27.3 (Magic Find Rework, Nov 17 2026). Chance per kill =
 * base x (1 + Magic Find / 100) (x Vincent for dyes). Over 100 % the rest is a chance at a double drop.
 * Magic Find against a mob = your base Magic Find + weapon (Looting) + that mob's Bestiary + Bobbin' Time
 * + Giant Waterbug Shard + Magma Lord (lava only) + Baby Yeti pet (winter only) + extra, capped at 500.
 */
public final class Calc {
    /** mob, drop, base chance (0..1), Journal key, dye (or null). */
    public record Drop(String mob, String item, double base, String mobKey, Dye dye) {
        public String label() {
            return this.item + "  ·  " + this.mob;
        }
    }

    public static final Drop[] DROPS = {
        new Drop("Wiki Tiki", "Tiki Mask", 0.0066, "tiki", null),
        new Drop("Wiki Tiki", "Bobbin' Scriptures", 0.025, "tiki", null),
        new Drop("Wiki Tiki", "Troubled Bubble", 0.02, "tiki", null),
        new Drop("Wiki Tiki", "Aquamarine Dye", 1.0 / 25_000, "tiki", Dye.AQUAMARINE),
        new Drop("Titanoboa", "Titanoboa Shed", 0.005, "boa", null),
        new Drop("Titanoboa", "Snake Eyes", 0.011, "boa", null),
        new Drop("Titanoboa", "Bobbin' Scriptures", 0.025, "boa", null),
        new Drop("Titanoboa", "Condensed Lily Pad", 0.05, "boa", null),
        new Drop("Titanoboa", "Aquamarine Dye", 1.0 / 25_000, "boa", Dye.AQUAMARINE),
        new Drop("Lord Jawbus", "Radioactive Vial", 0.0125, "jawbus", null),
        new Drop("Lord Jawbus", "Bobbin' Scriptures", 0.025, "jawbus", null),
        new Drop("Lord Jawbus", "Magma Lord Fragment", 0.5, "jawbus", null),
        new Drop("Lord Jawbus", "Carmine Dye", 1.0 / 25_000, "jawbus", Dye.CARMINE),
        new Drop("Thunder", "Flash I", 0.04, "thunder", null),
        new Drop("Thunder", "Carmine Dye", 1.0 / 50_000, "thunder", Dye.CARMINE),
        new Drop("Ragnarok", "Carmine Dye", 1.0 / 25_000, "ragnarok", Dye.CARMINE),
        new Drop("Plhlegblast", "Carmine Dye", 1.0 / 50_000, "plhlegblast", Dye.CARMINE),
        new Drop("Yeti", "True Ice", 0.05, "yeti", null),
        new Drop("Yeti", "Baby Yeti (Common)", 0.035, "yeti", null),
        new Drop("Yeti", "Iceberg Dye", 1.0 / 25_000, "yeti", Dye.ICEBERG),
        new Drop("Reindrake", "Iceberg Dye", 1.0 / 10_000, "reindrake", Dye.ICEBERG),
        new Drop("Grim Reaper", "Vampire Fang", 0.05, "grim", null),
        new Drop("Grim Reaper", "Midnight Dye", 1.0 / 10_000, "grim", Dye.MIDNIGHT)};

    public static final double MF_CAP = 500;
    public static final String[] WEAPON = {"None", "Looting VI +20", "Looting VII +25"};
    public static final double[] WEAPON_MF = {0, 20, 25};
    public static final String[] BOBBIN = {"Off", "III +12", "IV +16", "V +20"};
    public static final double[] BOBBIN_MF = {0, 12, 16, 20};
    public static final String[] MAGMA = {"Off", "2 pieces +2.5", "3 pieces +5", "4 pieces +7.5", "5 pieces +10"};
    public static final double[] MAGMA_MF = {0, 2.5, 5, 7.5, 10};
    public static final String[] YETI = {"Auto", "On", "Off"};

    /** Saved in config/light/calc.json. */
    public static final class Data {
        public int weapon = 0;
        public int bobbin = 0;
        public boolean waterbug = false;
        public int magma = 0;
        public int yeti = 0;
        public String pet = "";
        public Map<String, Double> bestiaryAuto = new LinkedHashMap<>();
        public Map<String, Double> bestiaryTyped = new LinkedHashMap<>();
        public Map<String, Double> extra = new LinkedHashMap<>();
        public Map<String, Double> measured = new LinkedHashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern PET_TAB = Pattern.compile("^\\[Lvl \\d+\\] ([A-Za-z' ]+?)(?:\\s*[^A-Za-z' ].*)?$");
    private static final Pattern PET_CHAT = Pattern.compile("^(?:You summoned your|Autopet equipped your(?: \\[Lvl \\d+\\])?) ([A-Za-z' ]+?)(?:!| ✦|\\s*$).*");
    private static final Pattern PET_GONE = Pattern.compile("^You despawned your .+");
    private static final Pattern DROP_MF = Pattern.compile("DROP! (.+?) \\(\\+?([\\d,.]+)%? ?✯? ?Magic Find\\)");
    private static Data data;
    private static int tick;

    private Calc() {
    }

    // ------------------------------------------------------------------ saved data

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(LightFiles.FOLDER).resolve("calc.json");
    }

    public static Data data() {
        if (data == null) {
            try {
                Path f = file();
                if (Files.isRegularFile(f)) data = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Data.class);
            } catch (Throwable t) {
                TikiGambleClient.LOGGER.warn("[Light] calc.json could not be read", t);
            }
            if (data == null) data = new Data();
            if (data.bestiaryAuto == null) data.bestiaryAuto = new LinkedHashMap<>();
            if (data.bestiaryTyped == null) data.bestiaryTyped = new LinkedHashMap<>();
            if (data.extra == null) data.extra = new LinkedHashMap<>();
            if (data.measured == null) data.measured = new LinkedHashMap<>();
            if (data.pet == null) data.pet = "";
        }
        return data;
    }

    public static void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(data()), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] calc.json could not be saved", t);
        }
    }

    // ------------------------------------------------------------------ mob groups

    public static boolean lava(String mobKey) {
        return mobKey.equals("jawbus") || mobKey.equals("thunder") || mobKey.equals("ragnarok") || mobKey.equals("plhlegblast");
    }

    public static boolean winter(String mobKey) {
        return mobKey.equals("yeti") || mobKey.equals("reindrake");
    }

    // ------------------------------------------------------------------ Magic Find parts

    /** Magic Find Light last read from your SkyBlock menu, or -1. */
    public static double magicFind() {
        try {
            return DyeTracker.data().magicFind;
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Bestiary Magic Find for that mob: typed value, else the one read from its Bestiary page, else 0. */
    public static double bestiary(String mobKey) {
        Double t = data().bestiaryTyped.get(mobKey);
        if (t != null) return t;
        Double a = data().bestiaryAuto.get(mobKey);
        return a == null ? 0 : a;
    }

    public static boolean bestiaryRead(String mobKey) {
        return data().bestiaryAuto.containsKey(mobKey);
    }

    public static double extra(String mobKey) {
        Double e = data().extra.get(mobKey);
        return e == null ? 0 : e;
    }

    public static boolean yetiPet() {
        return data().pet.toLowerCase(Locale.ROOT).contains("baby yeti");
    }

    public static boolean yetiOn() {
        Data d = data();
        return d.yeti == 1 || d.yeti == 0 && yetiPet();
    }

    /** All bonuses on top of the base Magic Find, against this mob. */
    public static double bonus(String mobKey) {
        Data d = data();
        double b = WEAPON_MF[Math.floorMod(d.weapon, WEAPON_MF.length)] + bestiary(mobKey)
            + BOBBIN_MF[Math.floorMod(d.bobbin, BOBBIN_MF.length)] + (d.waterbug ? 5 : 0) + extra(mobKey);
        if (lava(mobKey)) b += MAGMA_MF[Math.floorMod(d.magma, MAGMA_MF.length)];
        if (winter(mobKey) && yetiOn()) b += 10;
        return b;
    }

    /** Total Magic Find against this mob (capped at 500). base < 0 = unknown, counted as 0. */
    public static double total(double base, String mobKey) {
        return Math.min(MF_CAP, Math.max(0, base) + bonus(mobKey));
    }

    /** Magic Find Hypixel showed in your last drop message from that mob, or -1. */
    public static double measured(String mobKey) {
        Double m = data().measured.get(mobKey);
        return m == null ? -1 : m;
    }

    public static double vincent(Drop d) {
        try {
            return d.dye() == null ? 1.0 : DyeTracker.vincent(d.dye());
        } catch (Throwable t) {
            return 1.0;
        }
    }

    /** Chance per kill (can be above 1: the rest is a chance at a double drop). */
    public static double chance(Drop d, double mf) {
        return d.base() * (1.0 + Math.max(0, mf) / 100.0) * vincent(d);
    }

    /** Kills needed to get at least one drop with probability q. */
    public static long killsFor(double p, double q) {
        if (p >= 1) return 1;
        if (p <= 0) return -1;
        return (long) Math.ceil(Math.log(1 - q) / Math.log(1 - p));
    }

    /** Share of players who would still be waiting after this many kills. */
    public static double stillWaiting(double p, long kills) {
        if (p >= 1) return 0;
        return Math.pow(1 - p, Math.max(0, kills));
    }

    /** Colour + word for a dry streak: half of all players wait this long = normal. */
    public static String verdict(double wait) {
        return wait >= 0.5 ? "§anormal" : wait >= 0.1 ? "§eunlucky" : "§cvery unlucky";
    }

    /** Kills of that mob since you last got this drop (from History), and whether the drop was found at all. */
    public static long[] dry(Drop d) {
        long n = 0;
        String item = d.item().toLowerCase(Locale.ROOT).replace(" (common)", "");
        for (GambleLog.Entry e : GambleLog.entries()) {
            if (e.test || !d.mobKey().equals(e.mob)) continue;
            if (e.drop != null && e.drop.toLowerCase(Locale.ROOT).contains(item)) return new long[]{n, 1};
            n++;
        }
        return new long[]{n, 0};
    }

    // ------------------------------------------------------------------ reading (only what you see yourself)

    /** Every 2 s: your active pet from the tab list. */
    public static void tick(Minecraft mc) {
        if (++tick % 40 != 0 || mc.getConnection() == null) return;
        try {
            boolean petSection = false;
            for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
                Component name = info.getTabListDisplayName();
                if (name == null) continue;
                String line = DropLines.stripFormatting(name.getString()).trim();
                if (line.startsWith("Pet:")) {
                    petSection = true;
                    continue;
                }
                if (!petSection) continue;
                Matcher m = PET_TAB.matcher(line);
                if (m.matches()) setPet(m.group(1).trim());
                else if (line.equalsIgnoreCase("None") || line.contains("No pet")) setPet("");
                break;
            }
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.debug("[Light] pet read failed", t);
        }
    }

    private static void setPet(String pet) {
        if (pet.equals(data().pet)) return;
        data().pet = pet;
        save();
    }

    /** Pet messages and "RARE DROP! X (+123 ✯ Magic Find)". */
    public static void onChat(Component message, boolean overlay) {
        if (overlay) return;
        String t = DropLines.stripFormatting(message.getString()).trim();
        Matcher p = PET_CHAT.matcher(t);
        if (p.matches()) {
            setPet(p.group(1).trim());
            return;
        }
        if (PET_GONE.matcher(t).matches()) {
            setPet("");
            return;
        }
        Matcher m = DROP_MF.matcher(t);
        if (!m.find()) return;
        String item = m.group(1).toLowerCase(Locale.ROOT);
        double mf;
        try {
            mf = Double.parseDouble(m.group(2).replace(",", ""));
        } catch (NumberFormatException e) {
            return;
        }
        List<Drop> hits = new ArrayList<>();
        for (Drop d : DROPS) {
            String di = d.item().toLowerCase(Locale.ROOT).replace(" (common)", "");
            if (item.contains(di) && hits.stream().noneMatch(h -> h.mobKey().equals(d.mobKey()))) hits.add(d);
        }
        if (hits.isEmpty()) return;
        String mob = hits.get(0).mobKey();
        if (hits.size() > 1) { // same drop from several mobs: the one you killed last
            mob = null;
            for (GambleLog.Entry e : GambleLog.entries()) {
                if (e.test || e.mob == null) continue;
                for (Drop h : hits) if (h.mobKey().equals(e.mob)) mob = e.mob;
                break;
            }
            if (mob == null) return;
        }
        data().measured.put(mob, mf);
        save();
    }

    /** Bestiary Magic Find, read from the mob's item in an open Bestiary page (called by Bestiary.tick). */
    public static void readBestiaryItem(String itemName, List<String> lore) {
        String name = itemName.trim();
        for (Drop d : DROPS) {
            if (!name.toLowerCase(Locale.ROOT).startsWith(d.mob().toLowerCase(Locale.ROOT))) continue;
            String rest = name.substring(d.mob().length()).trim();
            if (!rest.isEmpty() && !rest.matches("[IVXLC\\d]+")) return; // "Wiki Tiki 25" / "Wiki Tiki XXV"
            Double found = null, later = null;
            boolean next = false;
            Pattern mfp = Pattern.compile("\\+([\\d.]+)\\s*[^\\sa-z\\d]?\\s*magic find");
            for (String line : lore) {
                String l = line.toLowerCase(Locale.ROOT);
                if (l.contains("next") || l.contains("upgrade") || l.contains("to unlock")) next = true;
                Matcher m = mfp.matcher(l);
                if (!m.find()) continue;
                try {
                    double v = Double.parseDouble(m.group(1));
                    if (!next && found == null) found = v;
                    else if (later == null) later = v;
                } catch (NumberFormatException ignored) {
                }
            }
            if (found == null) found = later; // only a "next tier" line: better than nothing, you can type it
            if (found == null) return;
            Double old = data().bestiaryAuto.put(d.mobKey(), found);
            if (old == null || !old.equals(found)) save();
            return;
        }
    }

    // ------------------------------------------------------------------ search and chat

    public static Drop find(String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        String alias = LavaPrices.boardDropIn(query); // short names: mlf, vial, flash book ...
        if (alias != null && !alias.toLowerCase(Locale.ROOT).contains(q)) {
            Drop a = find(alias + mobWords(q));
            if (a == null) a = find(alias);
            if (a != null) return a;
        }
        Drop best = null;
        for (Drop d : DROPS) {
            String full = (d.item() + " " + d.mob()).toLowerCase(Locale.ROOT);
            if (full.contains(q) || d.item().toLowerCase(Locale.ROOT).startsWith(q)) {
                if (best == null) best = d;
            }
        }
        if (best != null) return best;
        for (Drop d : DROPS) {
            boolean all = true;
            for (String w : q.split("\\s+")) if (!(d.item() + " " + d.mob() + " " + d.mobKey()).toLowerCase(Locale.ROOT).contains(w)) all = false;
            if (all) return d;
        }
        return null;
    }

    /** " boa" when the query names a mob, else "". */
    private static String mobWords(String q) {
        for (Drop d : DROPS) {
            if (q.contains(d.mobKey()) || q.contains(d.mob().toLowerCase(Locale.ROOT))) return " " + d.mobKey();
        }
        return "";
    }

    /** Does the query name a mob ("aquamarine boa", "carmine jawbus")? */
    private static boolean namesMob(String q) {
        q = q.toLowerCase(Locale.ROOT);
        for (Drop d : DROPS) {
            if (q.contains(d.mobKey()) || q.contains(d.mob().toLowerCase(Locale.ROOT))) return true;
            for (String w : d.mob().toLowerCase(Locale.ROOT).split(" ")) if (w.length() > 3 && q.contains(w)) return true;
        }
        return false;
    }

    /** All drops with that item (Aquamarine Dye -> Wiki Tiki + Titanoboa). */
    public static List<Drop> sameItem(Drop d) {
        List<Drop> out = new ArrayList<>();
        for (Drop o : DROPS) if (o.item().equals(d.item())) out.add(o);
        return out;
    }

    public static String pct(double p) {
        if (p >= 1) return "100% + " + Math.round((p - 1) * 100) + "% double";
        double v = p * 100;
        return (v < 0.01 ? String.format(Locale.ROOT, "%.4f", v) : v < 1 ? String.format(Locale.ROOT, "%.3f", v) : String.format(Locale.ROOT, "%.2f", v)) + "%";
    }

    public static String oneIn(double p) {
        return p >= 1 ? "guaranteed" : "1 in " + String.format(Locale.ROOT, "%,d", Math.round(1 / p));
    }

    private static String n(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    private static String mfText(double mf) {
        return mf == Math.rint(mf) ? String.valueOf(Math.round(mf)) : String.format(Locale.ROOT, "%.1f", mf);
    }

    /** /light chance <drop> */
    public static int chat(String query) {
        Drop d = find(query);
        if (d == null) {
            TikiGambleClient.chat("§c[Light] Unknown drop: " + query + " §7- try: tiki mask, vial, flash, shed, aquamarine ...");
            return 0;
        }
        double base = magicFind();
        List<Drop> group = sameItem(d);
        if (group.size() > 1 && !namesMob(query)) {
            overview(d, group, base);
            return 1;
        }
        double mf = total(base, d.mobKey());
        double p = chance(d, mf);
        long[] dry = dry(d);
        TikiGambleClient.chat("§8§m        §r §d§lDrop Chance §8§m        ");
        TikiGambleClient.chat("§6★ §l" + d.item() + "§r §7from " + d.mob());
        TikiGambleClient.chat("   §7Chance: §e" + pct(p) + " §7(" + oneIn(p) + ") §8· §7MF §b" + (base < 0 ? "?" : mfText(mf))
            + (vincent(d) > 1 ? " §8· §7Vincent §d" + Math.round(vincent(d)) + "x" : ""));
        double meas = measured(d.mobKey());
        if (meas >= 0) TikiGambleClient.chat("   §8Last drop message from " + d.mob() + ": " + mfText(meas) + " MF");
        if (p < 1) {
            TikiGambleClient.chat("   §7For 50%: §f" + n(killsFor(p, 0.5)) + " §8· §7for 90%: §f" + n(killsFor(p, 0.9)) + " §7kills");
            double wait = stillWaiting(p, dry[0]);
            TikiGambleClient.chat("   §7Since drop: §f" + n(dry[0]) + (dry[1] == 0 && dry[0] > 0 ? "+" : "") + " §7kills §8· §f"
                + Math.round(wait * 100) + "% §7of players wait this long §8→ " + verdict(wait));
        }
        if (group.size() > 1) TikiGambleClient.chat("   §8Also drops from: " + others(d, group) + " §8- /light chance " + d.item().toLowerCase(Locale.ROOT).split(" ")[0]);
        if (base < 0) TikiGambleClient.chat("   §8Open your SkyBlock menu once so Light knows your Magic Find.");
        return 1;
    }

    private static String others(Drop d, List<Drop> group) {
        StringBuilder b = new StringBuilder();
        for (Drop o : group) {
            if (o == d) continue;
            if (b.length() > 0) b.append(", ");
            b.append(o.mob());
        }
        return b.toString();
    }

    /** One drop from several mobs: a line per mob plus all kills together. */
    private static void overview(Drop d, List<Drop> group, double base) {
        TikiGambleClient.chat("§8§m        §r §d§lDrop Chance §8§m        ");
        TikiGambleClient.chat("§6★ §l" + d.item() + "§r §7from " + group.size() + " sea creatures");
        long kills = 0;
        boolean plus = false;
        double wait = 1;
        for (Drop o : group) {
            double p = chance(o, total(base, o.mobKey()));
            long[] dry = dry(o);
            kills += dry[0];
            if (dry[1] == 0 && dry[0] > 0) plus = true;
            wait *= stillWaiting(p, dry[0]);
            TikiGambleClient.chat("   §f" + o.mob() + " §8· §e" + oneIn(p) + " §8· §7since drop §f" + n(dry[0]) + (dry[1] == 0 && dry[0] > 0 ? "+" : ""));
        }
        TikiGambleClient.chat("   §7Together: §f" + n(kills) + (plus ? "+" : "") + " §7kills §8· §f" + Math.round(wait * 100)
            + "% §7of players wait this long §8→ " + verdict(wait));
        TikiGambleClient.chat("   §8One mob only: /light chance " + d.item().toLowerCase(Locale.ROOT).split(" ")[0] + " " + group.get(group.size() - 1).mobKey());
        if (base < 0) TikiGambleClient.chat("   §8Open your SkyBlock menu once so Light knows your Magic Find.");
    }
}
