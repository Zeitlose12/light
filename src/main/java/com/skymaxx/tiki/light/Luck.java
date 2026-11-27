package com.skymaxx.tiki.light;

import com.skymaxx.tiki.config.TikiStats;
import java.util.ArrayList;
import java.util.List;

/**
 * Light: compares the drops you got with what the base drop chances predict.
 * Base chances (no Magic Find) after Hypixel SkyBlock 0.27.3 (Magic Find Rework, Nov 17 2026):
 * Wiki Tiki  - Bobbin' 2.5%, Troubled Bubble 2%, Tiki Mask 0.66%, Aquamarine 1/25k
 * Titanoboa  - Lily Pad 5%, Bobbin' 2.5%, Shed 0.5%, Snake Eyes 1.1%, Aquamarine 1/25k
 */
public final class Luck {
    private Luck() {
    }

    public record Line(String name, int got, double expected) {
        /** Percent above (+) or below (-) the expectation, or NaN when nothing is expected yet. */
        public double percent() {
            if (this.expected <= 0.0 || this.got == 0 && this.expected < 1.0) {
                return Double.NaN;
            }
            return (this.got / this.expected - 1.0) * 100.0;
        }
    }

    public static List<Line> tiki(TikiStats s) {
        ArrayList<Line> out = new ArrayList<Line>();
        out.add(new Line("Bobbin' Scriptures", s.scripturesDropped, s.kills * 0.025));
        out.add(new Line("Troubled Bubble", s.bubblesDropped, s.kills * 0.02));
        out.add(new Line("Tiki Mask", s.masksDropped, s.kills * 0.0066));
        out.add(new Line("Aquamarine Dye", s.dyesDropped, s.kills * 4.0E-5));
        return out;
    }

    public static List<Line> boa(TikiStats s) {
        ArrayList<Line> out = new ArrayList<Line>();
        out.add(new Line("Condensed Lily Pad", s.lilyPadsDropped, s.boaKills * 0.05));
        out.add(new Line("Bobbin' Scriptures", s.boaScripturesDropped, s.boaKills * 0.025));
        out.add(new Line("Titanoboa Shed", s.shedsDropped, s.boaKills * 0.005));
        out.add(new Line("Snake Eyes", s.snakeEyesDropped, s.boaKills * 0.011));
        out.add(new Line("Aquamarine Dye", s.boaDyesDropped, s.boaKills * 4.0E-5));
        return out;
    }

    /** Overall luck over all drops except Aquamarine Dye (too rare to say anything). */
    public static double overall(TikiStats s) {
        double got = 0.0;
        double expected = 0.0;
        for (List<Line> lines : List.of(Luck.tiki(s), Luck.boa(s))) {
            for (Line line : lines) {
                if (line.name().startsWith("Aquamarine")) continue;
                got += line.got();
                expected += line.expected();
            }
        }
        return expected <= 0.0 ? Double.NaN : (got / expected - 1.0) * 100.0;
    }

    public static String format(double percent) {
        if (Double.isNaN(percent)) {
            return "-";
        }
        long rounded = Math.round(percent);
        return (rounded > 0 ? "§a+" : rounded < 0 ? "§c" : "§7") + rounded + "%";
    }

    public static String plain(double percent) {
        if (Double.isNaN(percent)) {
            return "-";
        }
        long rounded = Math.round(percent);
        return (rounded > 0 ? "+" : "") + rounded + "%";
    }
}
