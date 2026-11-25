package com.skymaxx.tiki.render.boa;

import com.skymaxx.tiki.boa.BoaPrize;

public enum BoaTex {
    HEAD,
    BODY,
    LILY_PAD,
    BOBBIN,
    SHED,
    SNAKE_EYES,
    AQUAMARINE,
    CYLINDER,
    CYLINDER_SHADE,
    CYLINDER_FLUTES,
    CHAMBER,
    BASE,
    BASE_GOLD,
    BULLET,
    BULLET_GOLD,
    BULLET_BACK,
    SCOPE_MASK,
    RETICLE,
    SKY_DUSK,
    SKY_NIGHT,
    SKY_MIST,
    SKY_GOLDEN,
    REEDS,
    SPLASH,
    PUFF,
    FIRE,
    RING,
    SPARK,
    GLOW,
    CROSS,
    WHITE,
    SOFT_RING,
    RIPPLE,
    SMOKE_0,
    SMOKE_1,
    SMOKE_2,
    TRI,
    DISC,
    STAMP_SHED,
    STAMP_SNAKE_EYES,
    STAMP_AQUAMARINE,
    LEAF_0,
    LEAF_1,
    LEAF_2,
    LEAF_3,
    LEAF_4,
    LEAF_5,
    LEAF_M_0,
    LEAF_M_1,
    LEAF_M_2,
    LEAF_M_3,
    LEAF_M_4,
    LEAF_M_5;

    public static final int LEAF_VARIANTS = 6;

    public static BoaTex smoke(int variant) {
        return BoaTex.values()[SMOKE_0.ordinal() + Math.floorMod(variant, 3)];
    }

    public static BoaTex skinOf(BoaPrize prize) {
        return switch (prize) {
            case BoaPrize.LILY_PAD -> LILY_PAD;
            case BoaPrize.BOBBIN -> BOBBIN;
            case BoaPrize.SHED -> SHED;
            case BoaPrize.SNAKE_EYES -> SNAKE_EYES;
            case BoaPrize.AQUAMARINE -> AQUAMARINE;
            default -> BODY;
        };
    }

    public static BoaTex stampOf(BoaPrize prize) {
        return switch (prize) {
            case BoaPrize.SHED -> STAMP_SHED;
            case BoaPrize.SNAKE_EYES -> STAMP_SNAKE_EYES;
            case BoaPrize.AQUAMARINE -> STAMP_AQUAMARINE;
            default -> null;
        };
    }

    public static BoaTex leaf(int variant, boolean mirrored) {
        int v = Math.floorMod(variant, 6);
        return BoaTex.values()[(mirrored ? LEAF_M_0.ordinal() : LEAF_0.ordinal()) + v];
    }
}

