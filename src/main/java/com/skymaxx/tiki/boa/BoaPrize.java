package com.skymaxx.tiki.boa;

import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.detect.DropLines;
import java.util.Locale;

public enum BoaPrize implements DropLines.Named
{
    NONE(0, null, null, 0.0f, 0),
    LILY_PAD(4, "condensedlilypad", "\u00a7aCondensed Lily Pad\u00a72!", 0.55f, 2),
    BOBBIN(4, "bobbinscriptures", "\u00a79Bobbin' Scriptures\u00a71!", 0.6f, 1),
    SHED(5, "titanoboashed", "\u00a75Titanoboa Shed\u00a78!", 0.9f, 4),
    SNAKE_EYES(5, "snakeeyes", "\u00a7dSnake Eyes\u00a75!", 0.95f, 3),
    AQUAMARINE(5, "aquamarinedye", "\u00a7bAquamarine Dye\u00a73!", 1.0f, 5);

    public static final int SEGMENTS = 5;
    private final int segments;
    private final String model;
    private final String announcement;
    private final float celebration;
    private final int rank;

    private BoaPrize(int segments, String model, String announcement, float celebration, int rank) {
        this.segments = segments;
        this.model = model;
        this.announcement = announcement;
        this.celebration = celebration;
        this.rank = rank;
    }

    public boolean dropped() {
        return this != NONE;
    }

    public double dropRate() {
        return switch (this.ordinal()) {
            case 1, 2 -> 0.01;
            case 3 -> 0.002;
            case 4 -> 0.0011;
            case 5 -> 2.0E-5;
            default -> 0.0;
        };
    }

    public int segments() {
        return this.segments;
    }

    public boolean inHead() {
        return this.segments == 5;
    }

    public String model() {
        return this.model;
    }

    public String announcement() {
        return this.announcement;
    }

    public float celebration() {
        return this.celebration;
    }

    public boolean stamps() {
        return this == SHED || this == SNAKE_EYES || this == AQUAMARINE;
    }

    public String title() {
        return switch (this.ordinal()) {
            case 1 -> "Condensed Lily Pad";
            case 2 -> "Bobbin' Scriptures";
            case 3 -> "Titanoboa Shed";
            case 4 -> "Snake Eyes";
            case 5 -> "Aquamarine Dye";
            default -> "";
        };
    }

    @Override
    public String configuredNames(TikiConfig cfg) {
        return switch (this.ordinal()) {
            case 1 -> cfg.lilyPadItemName;
            case 2 -> cfg.bobbinItemName;
            case 3 -> cfg.shedItemName;
            case 4 -> cfg.snakeEyesItemName;
            case 5 -> cfg.aquamarineItemName;
            default -> null;
        };
    }

    @Override
    public int rank() {
        return this.rank;
    }

    public static BoaPrize best(BoaPrize a, BoaPrize b) {
        if (a == null) {
            return b == null ? NONE : b;
        }
        if (b == null) {
            return a;
        }
        return b.rank() > a.rank() ? b : a;
    }

    public static BoaPrize byName(String name) {
        if (name == null) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT).replace("_", "");
        for (BoaPrize prize : BoaPrize.values()) {
            if (!prize.name().toLowerCase(Locale.ROOT).replace("_", "").equals(key) && (prize.model == null || !prize.model.equals(key))) continue;
            return prize;
        }
        return switch (key) {
            case "lilypad" -> LILY_PAD;
            case "bobbin" -> BOBBIN;
            case "shed" -> SHED;
            case "aquamarine", "dye" -> AQUAMARINE;
            default -> null;
        };
    }
}

