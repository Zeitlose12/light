package com.skymaxx.tiki.gui;

/** Light: "5 min ago" style labels. */
final class DyeHudAgo {
    private DyeHudAgo() {
    }

    static String where(com.skymaxx.tiki.light.dye.Dye dye) {
        return switch (dye) {
            case AQUAMARINE -> "Water page";
            case CARMINE -> "Lava page";
            case ICEBERG -> "Winter page";
            case MIDNIGHT -> "Spooky page";
        };
    }

    static String vincentLabel(com.skymaxx.tiki.light.dye.Dye dye) {
        int v = com.skymaxx.tiki.light.dye.DyeTracker.vincent(dye);
        return dye.shortName + ": \u00a7" + (v == 1 ? "7" : v == 2 ? "e" : "6") + v + "x";
    }

    static String ago(long ms) {
        long s = Math.max(0L, (System.currentTimeMillis() - ms) / 1000L);
        if (s < 60) return "just now";
        if (s < 3600) return s / 60 + " min ago";
        if (s < 86400) return s / 3600 + " h ago";
        return s / 86400 + " days ago";
    }
}
