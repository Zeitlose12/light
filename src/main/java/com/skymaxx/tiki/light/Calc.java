package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.light.dye.Dye;
import com.skymaxx.tiki.light.dye.DyeTracker;
import java.util.Locale;

/**
 * Light: drop chance calculator (Calculator tab and /light chance).
 * Base chances after Hypixel SkyBlock 0.27.3 (Magic Find Rework, Nov 17 2026). Chance per kill =
 * base x (1 + Magic Find / 100) (x Vincent for dyes). Over 100 % the rest is a chance at a double drop.
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

    private Calc() {
    }

    /** Magic Find Light last read from your SkyBlock menu, or -1. */
    public static double magicFind() {
        try {
            return DyeTracker.data().magicFind;
        } catch (Throwable t) {
            return -1;
        }
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

    public static Drop find(String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        String alias = LavaPrices.boardDropIn(query); // short names: mlf, vial, flash book ...
        if (alias != null && !alias.toLowerCase(Locale.ROOT).contains(q)) {
            Drop a = find(alias);
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
            for (String w : q.split("\\s+")) if (!(d.item() + " " + d.mob()).toLowerCase(Locale.ROOT).contains(w)) all = false;
            if (all) return d;
        }
        return null;
    }

    public static String pct(double p) {
        if (p >= 1) return "100% + " + Math.round((p - 1) * 100) + "% double";
        double v = p * 100;
        return (v < 0.01 ? String.format(Locale.ROOT, "%.4f", v) : v < 1 ? String.format(Locale.ROOT, "%.3f", v) : String.format(Locale.ROOT, "%.2f", v)) + "%";
    }

    public static String oneIn(double p) {
        return p >= 1 ? "guaranteed" : "1 in " + String.format(Locale.ROOT, "%,d", Math.round(1 / p));
    }

    /** /light chance <drop> */
    public static int chat(String query) {
        Drop d = find(query);
        if (d == null) {
            TikiGambleClient.chat("§c[Light] Unknown drop: " + query + " §7- try: tiki mask, vial, flash, shed, aquamarine ...");
            return 0;
        }
        double mf = magicFind();
        double p = chance(d, mf);
        long[] dry = dry(d);
        TikiGambleClient.chat("§8§m        §r §d§lDrop Chance §8§m        ");
        TikiGambleClient.chat("§6★ §l" + d.item() + "§r §7from " + d.mob());
        TikiGambleClient.chat("   §7Chance: §e" + pct(p) + " §7(" + oneIn(p) + ") §8· §7MF §b" + (mf < 0 ? "?" : String.valueOf(Math.round(mf)))
            + (vincent(d) > 1 ? " §8· §7Vincent §d" + Math.round(vincent(d)) + "x" : ""));
        if (p < 1) {
            TikiGambleClient.chat("   §7For 50%: §f" + String.format(Locale.ROOT, "%,d", killsFor(p, 0.5)) + " §8· §7for 90%: §f"
                + String.format(Locale.ROOT, "%,d", killsFor(p, 0.9)) + " §7kills");
            TikiGambleClient.chat("   §7Dry streak: §f" + String.format(Locale.ROOT, "%,d", dry[0]) + (dry[1] == 0 && dry[0] > 0 ? "+" : "")
                + " §7→ only §a" + String.format(Locale.ROOT, "%.1f", stillWaiting(p, dry[0]) * 100) + "% §7wait this long");
        }
        if (mf < 0) TikiGambleClient.chat("   §8Open your SkyBlock menu once so Light knows your Magic Find.");
        return 1;
    }
}
