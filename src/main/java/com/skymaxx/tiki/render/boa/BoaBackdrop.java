package com.skymaxx.tiki.render.boa;

import com.skymaxx.tiki.render.boa.BoaTex;

public enum BoaBackdrop {
    DUSK(BoaTex.SKY_DUSK, -14935494, -15587030, -13743544, -11635600, -1465764, 0.64f, -16117232, -10590, -11637620, 0),
    MOONLIT(BoaTex.SKY_NIGHT, -16381412, -16313569, -14996155, -13479814, -2234113, 0.3f, -16644855, -3548430, -14009758, 0),
    MIST(BoaTex.SKY_MIST, -8415334, -11705247, -7692133, -5784651, -593182, 0.62f, -12692916, -725286, -10191752, 1507254228),
    GOLDEN(BoaTex.SKY_GOLDEN, -13669220, -14278641, -7706060, -2578336, -3384, 0.57f, -15331836, -7520, -10728874, 1392499876);

    private static final int[] WEIGHT;
    public final BoaTex sky;
    public final int top;
    public final int waterDeep;
    public final int waterFar;
    public final int waterLight;
    public final int glint;
    public final float glintAt;
    public final int reeds;
    public final int warm;
    public final int cool;
    public final int haze;

    private BoaBackdrop(BoaTex sky, int top, int waterDeep, int waterFar, int waterLight, int glint, float glintAt, int reeds, int warm, int cool, int haze) {
        this.sky = sky;
        this.top = top;
        this.waterDeep = waterDeep;
        this.waterFar = waterFar;
        this.waterLight = waterLight;
        this.glint = glint;
        this.glintAt = glintAt;
        this.reeds = reeds;
        this.warm = warm;
        this.cool = cool;
        this.haze = haze;
    }

    public static BoaBackdrop forRun(int seed) {
        int h = seed * 739982445;
        h ^= h >>> 15;
        int total = 0;
        for (int weight : WEIGHT) {
            total += weight;
        }
        int roll = Math.floorMod(h, total);
        for (int i = 0; i < WEIGHT.length && i < BoaBackdrop.values().length; ++i) {
            if ((roll -= WEIGHT[i]) >= 0) continue;
            return BoaBackdrop.values()[i];
        }
        return DUSK;
    }

    public static BoaBackdrop byName(String name) {
        for (BoaBackdrop b : BoaBackdrop.values()) {
            if (!b.name().equalsIgnoreCase(name)) continue;
            return b;
        }
        return null;
    }

    static {
        WEIGHT = new int[]{6, 6, 5, 2};
    }
}

