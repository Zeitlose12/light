package com.skymaxx.tiki.detect;

public final class NameplateHealth {
    public static final float UNKNOWN = -1.0f;
    private static final char HEART = '\u2764';

    private NameplateHealth() {
    }

    public static float fraction(String text) {
        float current = NameplateHealth.current(text);
        float max = NameplateHealth.max(text);
        if (current < 0.0f || max <= 0.0f) {
            return -1.0f;
        }
        return Math.min(1.0f, current / max);
    }

    public static float current(String text) {
        if (text == null) {
            return -1.0f;
        }
        int heart = text.lastIndexOf(10084);
        int end = heart >= 0 ? heart : text.length();
        int slash = text.lastIndexOf(47, end - 1);
        if (slash > 0) {
            return NameplateHealth.before(text, slash);
        }
        return end > 0 ? NameplateHealth.before(text, end) : -1.0f;
    }

    public static float max(String text) {
        if (text == null) {
            return -1.0f;
        }
        int heart = text.lastIndexOf(10084);
        int end = heart >= 0 ? heart : text.length();
        int slash = text.lastIndexOf(47, end - 1);
        if (slash <= 0 || slash + 1 >= text.length()) {
            return -1.0f;
        }
        return NameplateHealth.after(text, slash);
    }

    private static float before(String text, int at) {
        int start;
        int end = at - 1;
        float multiplier = 1.0f;
        if (end >= 0 && NameplateHealth.suffix(text.charAt(end)) != 1.0f) {
            multiplier = NameplateHealth.suffix(text.charAt(end));
            --end;
        }
        for (start = end; start >= 0 && NameplateHealth.isAmountChar(text.charAt(start)); --start) {
        }
        return NameplateHealth.value(text, start + 1, end, multiplier);
    }

    private static float after(String text, int slash) {
        int start;
        int end;
        for (end = start = slash + 1; end < text.length() && NameplateHealth.isAmountChar(text.charAt(end)); ++end) {
        }
        float multiplier = end < text.length() ? NameplateHealth.suffix(text.charAt(end)) : 1.0f;
        return NameplateHealth.value(text, start, end - 1, multiplier);
    }

    private static float value(String text, int start, int end, float multiplier) {
        if (start > end || start < 0 || end >= text.length()) {
            return -1.0f;
        }
        StringBuilder digits = new StringBuilder(end - start + 1);
        for (int i = start; i <= end; ++i) {
            char c = text.charAt(i);
            if (c == ',') continue;
            digits.append(c);
        }
        try {
            return Float.parseFloat(digits.toString()) * multiplier;
        }
        catch (NumberFormatException e) {
            return -1.0f;
        }
    }

    private static boolean isAmountChar(char c) {
        return c >= '0' && c <= '9' || c == ',' || c == '.';
    }

    private static float suffix(char c) {
        return switch (c) {
            case 'K', 'k' -> 1000.0f;
            case 'M', 'm' -> 1000000.0f;
            case 'B', 'b' -> 1.0E9f;
            default -> 1.0f;
        };
    }
}

