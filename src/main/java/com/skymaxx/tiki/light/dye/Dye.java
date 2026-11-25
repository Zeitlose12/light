package com.skymaxx.tiki.light.dye;

/** Light: the fishing dyes the tracker follows. */
public enum Dye {
    AQUAMARINE("Aquamarine", "Aquamarine Dye", 0xFF7FFFD4),
    ICEBERG("Iceberg", "Iceberg Dye", 0xFFB7E3F2),
    CARMINE("Carmine", "Carmine Dye", 0xFFE24B4A),
    MIDNIGHT("Midnight", "Midnight Dye", 0xFF8F87E6);

    public final String shortName;
    public final String itemName;
    public final int color;

    Dye(String shortName, String itemName, int color) {
        this.shortName = shortName;
        this.itemName = itemName;
        this.color = color;
    }
}
