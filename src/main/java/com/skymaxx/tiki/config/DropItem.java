package com.skymaxx.tiki.config;

public enum DropItem {
    TIKI_MASK("mask", "Tiki Mask", "Tiki Masks"),
    TROUBLED_BUBBLE("bubble", "Troubled Bubble", "Troubled Bubbles"),
    CONDENSED_LILY_PAD("lilypad", "Condensed Lily Pad", "Condensed Lily Pads"),
    TITANOBOA_SHED("shed", "Titanoboa Shed", "Titanoboa Sheds"),
    SNAKE_EYES("snakeeyes", "Snake Eyes", "Snake Eyes"),
    BOBBIN_SCRIPTURES("bobbin", "Bobbin' Scriptures", "Bobbin' Scriptures"),
    AQUAMARINE_DYE("aquamarine", "Aquamarine Dye", "Aquamarine Dyes");

    private final String word;
    private final String title;
    private final String plural;

    private DropItem(String word, String title, String plural) {
        this.word = word;
        this.title = title;
        this.plural = plural;
    }

    public String word() {
        return this.word;
    }

    public String title() {
        return this.title;
    }

    public String plural() {
        return this.plural;
    }
}

