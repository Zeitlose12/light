package com.skymaxx.tiki.config;

import java.util.Locale;

public enum GameplayCorner {
    TOP_LEFT("Top left"),
    BOTTOM_LEFT("Bottom left"),
    TOP_RIGHT("Top right");

    private final String title;

    private GameplayCorner(String title) {
        this.title = title;
    }

    public String title() {
        return this.title;
    }

    public GameplayCorner next() {
        GameplayCorner[] all = GameplayCorner.values();
        return all[(this.ordinal() + 1) % all.length];
    }

    public static GameplayCorner byName(String name) {
        if (name != null) {
            String key = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            for (GameplayCorner corner : GameplayCorner.values()) {
                if (!corner.name().equals(key)) continue;
                return corner;
            }
        }
        return TOP_LEFT;
    }
}

