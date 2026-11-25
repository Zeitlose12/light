package com.skymaxx.tiki.detect;

import com.skymaxx.tiki.anim.Prize;
import com.skymaxx.tiki.config.TikiConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DropLines {
    private static final String[] NO_NAMES = new String[0];
    private static final Pattern LOOT_SHARE = Pattern.compile("loot share!? you received (.+?) for assisting ");
    private static final Pattern CHARM = Pattern.compile("charm! you charmed the (.+?) and received ");
    private static final Pattern SACK_LINE = Pattern.compile("^\\s*\\+([\\d,]+)\\s+(.+?)\\s*\\((.+)\\)\\s*$");

    private DropLines() {
    }

    public static <P extends Named> P prizeFor(String itemName, String skyblockId, TikiConfig cfg, P[] values, P none) {
        String squashedName = DropLines.squash(itemName == null ? "" : itemName);
        String squashedId = DropLines.squash(skyblockId == null ? "" : skyblockId);
        P best = none;
        block0: for (P prize : values) {
            for (String name : DropLines.namesFor(prize, cfg)) {
                String needle = DropLines.squash(name);
                if (needle.isEmpty() || !squashedName.contains(needle) && !squashedId.equals(needle)) continue;
                if (best != none && prize.rank() <= best.rank()) continue block0;
                best = prize;
                continue block0;
            }
        }
        return best;
    }

    public static Prize prizeFor(String itemName, String skyblockId, TikiConfig cfg) {
        return (Prize)DropLines.prizeFor((String)itemName, (String)skyblockId, (TikiConfig)cfg, (Named[])Prize.values(), (Named)Prize.NONE);
    }

    public static String[] namesFor(Named prize, TikiConfig cfg) {
        String configured = prize.configuredNames(cfg);
        return configured == null || configured.isBlank() ? NO_NAMES : configured.split(",");
    }

    public static String squash(String in) {
        StringBuilder sb = new StringBuilder(in.length());
        for (int i = 0; i < in.length(); ++i) {
            char c = Character.toLowerCase(in.charAt(i));
            if (!Character.isLetterOrDigit(c)) continue;
            sb.append(c);
        }
        return sb.toString();
    }

    public static boolean isGameLine(String strippedLine, String phrase) {
        String want;
        if (strippedLine == null || phrase == null || phrase.isBlank()) {
            return false;
        }
        String line = DropLines.collapse(strippedLine);
        int at = line.indexOf(want = DropLines.collapse(phrase));
        if (at >= 0) {
            return at == 0 || line.lastIndexOf(58, at - 1) < 0;
        }
        String needle = DropLines.squash(phrase);
        return !needle.isEmpty() && strippedLine.indexOf(58) < 0 && DropLines.squash(strippedLine).contains(needle);
    }

    public static boolean isWholeGameLine(String strippedLine, String phrase) {
        if (strippedLine == null || phrase == null || phrase.isBlank() || strippedLine.indexOf(58) >= 0) {
            return false;
        }
        String want = DropLines.squash(phrase);
        return !want.isEmpty() && DropLines.squash(strippedLine).equals(want);
    }

    public static Share lootShare(String strippedLine, String plainPhrase, String creature) {
        if (strippedLine == null || strippedLine.isBlank()) {
            return Share.NONE;
        }
        if (DropLines.isGameLine(strippedLine, plainPhrase)) {
            return Share.UNNAMED;
        }
        String line = DropLines.collapse(strippedLine) + " ";
        Matcher m = LOOT_SHARE.matcher(line);
        if (!m.find() || line.lastIndexOf(58, m.start()) >= 0) {
            return Share.NONE;
        }
        String what = DropLines.squash(m.group(1));
        if (what.equals("loot") || what.endsWith("shards") || what.endsWith("shard")) {
            // Light: "LOOT SHARE! You received 2 Wiki Tiki Shards for assisting X!" (hunting shards) = a loot share too
            return Share.UNNAMED;
        }
        if (what.equals("loot")) {
            return Share.UNNAMED;
        }
        String name = DropLines.squash(creature == null ? "" : creature);
        return !name.isEmpty() && what.contains(name) ? Share.THIS_CREATURE : Share.OTHER_CREATURE;
    }

    public static Share charm(String strippedLine, String creature) {
        if (strippedLine == null || strippedLine.isBlank()) {
            return Share.NONE;
        }
        String line = DropLines.collapse(strippedLine) + " ";
        Matcher m = CHARM.matcher(line);
        if (!m.find() || line.lastIndexOf(58, m.start()) >= 0) {
            return Share.NONE;
        }
        String name = DropLines.squash(creature == null ? "" : creature);
        return !name.isEmpty() && DropLines.squash(m.group(1)).equals(name) ? Share.THIS_CREATURE : Share.OTHER_CREATURE;
    }

    private static String collapse(String in) {
        return in.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static boolean isSackReport(String strippedLine) {
        return strippedLine != null && strippedLine.startsWith("[Sacks]");
    }

    public static List<Added> sackAdditions(String tooltip) {
        ArrayList<Added> out = new ArrayList<Added>();
        if (tooltip == null || !tooltip.trim().toLowerCase(Locale.ROOT).startsWith("added")) {
            return out;
        }
        for (String line : tooltip.split("\\R")) {
            Matcher m = SACK_LINE.matcher(line);
            if (!m.matches()) continue;
            try {
                int count = Integer.parseInt(m.group(1).replace(",", ""));
                if (count <= 0) continue;
                out.add(new Added(count, m.group(2).trim()));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return out;
    }

    public static String stripFormatting(String in) {
        StringBuilder sb = new StringBuilder(in.length());
        for (int i = 0; i < in.length(); ++i) {
            char c = in.charAt(i);
            if (c == '\u00a7' && i + 1 < in.length()) {
                ++i;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static interface Named {
        public String configuredNames(TikiConfig var1);

        public int rank();
    }

    public static enum Share {
        NONE,
        UNNAMED,
        THIS_CREATURE,
        OTHER_CREATURE;

    }

    public record Added(int count, String name) {
    }
}

