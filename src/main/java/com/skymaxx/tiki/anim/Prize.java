package com.skymaxx.tiki.anim;

import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.detect.DropLines;

public enum Prize implements DropLines.Named
{
    NONE(3, null, false, 0.0f, null),
    BOBBIN(2, "bobbin", false, 0.6f, "\u00a79Bobbin' Scriptures\u00a71!"),
    BUBBLE(3, "bubble", false, 1.0f, "\u00a7bTroubled Bubble\u00a73!"),
    MASK(3, null, false, 1.0f, null),
    AQUAMARINE(3, "aquamarine", true, 1.0f, "\u00a7bAquamarine Dye\u00a73!");

    private final int crownTier;
    private final String faceAsset;
    private final boolean tintedHalo;
    private final float celebration;
    private final String announcement;

    private Prize(int crownTier, String faceAsset, boolean tintedHalo, float celebration, String announcement) {
        this.crownTier = crownTier;
        this.faceAsset = faceAsset;
        this.tintedHalo = tintedHalo;
        this.celebration = celebration;
        this.announcement = announcement;
    }

    @Override
    public String configuredNames(TikiConfig cfg) {
        return switch (this.ordinal()) {
            case 3 -> cfg.dropItemName;
            case 2 -> cfg.bubbleItemName;
            case 4 -> cfg.aquamarineItemName;
            case 1 -> cfg.bobbinItemName;
            default -> null;
        };
    }

    public boolean dropped() {
        return this != NONE;
    }

    public double dropRate() {
        return switch (this.ordinal()) {
            case 1 -> 0.01;
            case 2 -> 0.002;
            case 3 -> 0.001;
            case 4 -> 2.0E-5;
            default -> 0.0;
        };
    }

    @Override
    public int rank() {
        return this.ordinal();
    }

    public static Prize best(Prize a, Prize b) {
        if (a == null) {
            return b == null ? NONE : b;
        }
        if (b == null) {
            return a;
        }
        return b.rank() > a.rank() ? b : a;
    }

    public boolean stamps() {
        return this == MASK || this == BUBBLE || this == AQUAMARINE;
    }

    public String title() {
        return switch (this.ordinal()) {
            case 3 -> "Tiki Mask";
            case 2 -> "Troubled Bubble";
            case 4 -> "Aquamarine Dye";
            case 1 -> "Bobbin' Scriptures";
            default -> "";
        };
    }

    public int crownTier() {
        return this.crownTier;
    }

    public int tiers() {
        return this.crownTier + 1;
    }

    public String faceAsset() {
        return this.faceAsset;
    }

    public boolean tintedHalo() {
        return this.tintedHalo;
    }

    public float celebration() {
        return this.celebration;
    }

    public String announcement() {
        return this.announcement;
    }
}

