package com.skymaxx.tiki.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

final class GameMute {
    private static final SoundSource[] SOURCES = SoundSource.values();
    static final float FADE_SECONDS = 0.35f;
    private boolean muted;
    private boolean fading;
    private long fadeStartNanos;

    GameMute() {
    }

    void set(boolean wanted) {
        float k;
        if (!wanted) {
            if (this.muted || this.fading) {
                this.restore();
            }
            return;
        }
        if (this.muted) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        if (!this.fading) {
            this.fading = true;
            this.fadeStartNanos = now;
        }
        if ((k = Math.min(1.0f, (float)(now - this.fadeStartNanos) / 1.0E9f / 0.35f)) < 1.0f && mc.options != null) {
            float left = (1.0f - k) * (1.0f - k);
            // Light fix: updateCategoryVolume sets a gain multiplier (option volume is applied separately)
            mc.getSoundManager().updateCategoryVolume(SoundSource.MASTER, left);
            return;
        }
        for (SoundSource source : SOURCES) {
            mc.getSoundManager().updateCategoryVolume(source, 0.0f);
        }
        try {
            mc.getSoundManager().stop();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.fading = false;
        this.muted = true;
    }

    void restore() {
        this.muted = false;
        this.fading = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) {
            return;
        }
        for (SoundSource source : SOURCES) {
            // Light fix: restore the neutral gain 1.0 - writing the option value here squared the volume (e.g. 50% -> 25%)
            mc.getSoundManager().updateCategoryVolume(source, 1.0f);
        }
    }
}

