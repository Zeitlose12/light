package com.skymaxx.tiki.render.boa;

import com.skymaxx.tiki.audio.BoaAudio;
import com.skymaxx.tiki.boa.BoaPrize;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.render.boa.BoaArtSet;
import com.skymaxx.tiki.render.boa.BoaDrawList;
import com.skymaxx.tiki.render.boa.BoaTex;
import java.util.ArrayDeque;
import java.util.Random;

public final class BoaStamp {
    private static final float SLAM = 0.15f;
    private static final float ROLL = 0.95f;
    private static final float TARGET_HEIGHT = 0.42f;
    private final BoaAudio audio;
    private final TikiConfig cfg;
    private final Random rng = new Random();
    private final Print print = new Print();
    private final ArrayDeque<Object[]> waiting = new ArrayDeque();
    private static final float NEXT_DELAY = 0.12f;

    public BoaStamp(BoaAudio audio, TikiConfig cfg) {
        this.audio = audio;
        this.cfg = cfg;
    }

    public void trigger(BoaPrize prize, int number) {
        if (BoaTex.stampOf(prize) == null) {
            return;
        }
        if (this.print.active) {
            this.waiting.addLast(new Object[]{prize, number});
            return;
        }
        this.start(prize, number, this.cfg.stampDelaySeconds);
    }

    private void start(BoaPrize prize, int number, float delay) {
        this.print.prize = prize;
        this.print.number = Math.max(1, number);
        this.print.time = 0.0f;
        this.print.delayLeft = delay;
        this.print.fade = this.cfg.stampFadeMinSeconds + this.rng.nextFloat() * Math.max(0.0f, this.cfg.stampFadeMaxSeconds - this.cfg.stampFadeMinSeconds);
        this.print.tilt = -8.0f + this.rng.nextFloat() * 16.0f;
        this.print.active = true;
        this.print.impacted = false;
        this.print.ticksPlayed = 0;
    }

    public void update(float dt) {
        this.update(this.print, dt);
        if (!this.print.active && !this.waiting.isEmpty()) {
            Object[] next = this.waiting.removeFirst();
            this.start((BoaPrize)next[0], (Integer)next[1], 0.12f);
        }
    }

    private void update(Print print, float dt) {
        if (!print.active) {
            return;
        }
        if (print.delayLeft > 0.0f) {
            print.delayLeft -= dt;
            return;
        }
        if (!print.impacted) {
            print.impacted = true;
            this.audio.onStamp();
        }
        print.time += dt;
        int ticks = Math.max(1, Math.min(14, print.number));
        int wanted = Math.min(ticks, (int)(Math.min(1.0f, print.time / 0.95f) * (float)ticks));
        while (print.ticksPlayed < wanted) {
            ++print.ticksPlayed;
            this.audio.onCountTick((float)print.ticksPlayed / (float)ticks);
        }
        if (print.time >= 0.15f + print.fade) {
            print.active = false;
        }
    }

    public boolean visible() {
        return this.print.active && this.print.delayLeft <= 0.0f || !this.waiting.isEmpty();
    }

    public void abort() {
        this.print.active = false;
        this.waiting.clear();
    }

    public void build(BoaDrawList out, BoaArtSet art, float w, float h) {
        out.clear();
        if (art == null || !this.print.active || this.print.delayLeft > 0.0f) {
            return;
        }
        this.build(this.print, out, art, w, h);
    }

    private void build(Print print, BoaDrawList out, BoaArtSet art, float w, float h) {
        float alpha;
        float scale;
        float t;
        BoaTex tex = BoaTex.stampOf(print.prize);
        if (print.time < 0.15f) {
            t = print.time / 0.15f;
            float u = 1.0f - t;
            scale = 3.2f - 2.2f * (1.0f - u * u * u);
            alpha = Math.min(1.0f, t * 3.5f);
        } else {
            t = (print.time - 0.15f) / Math.max(0.05f, print.fade);
            scale = 1.0f + 0.04f * t;
            alpha = t < 0.33f ? 1.0f : 1.0f - (t - 0.33f) / 0.67f;
        }
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        if (alpha <= 0.004f) {
            return;
        }
        int tw = art.width(tex);
        int th = art.height(tex);
        float size = h * 0.42f * scale;
        float kick = print.time < 0.15f ? 0.0f : (float)Math.exp(-(print.time - 0.15f) * 14.0f);
        float cx = w * 0.5f + (float)Math.sin(print.time * 91.0f) * h * 0.012f * kick;
        float cy = h * 0.44f + (float)Math.cos(print.time * 77.0f) * h * 0.012f * kick;
        float rot = (float)Math.toRadians(print.tilt);
        float drop = size * 0.035f;
        out.sprite(tex, tw, th, cx + drop, cy + drop, size, size, rot, (int)(alpha * 170.0f) << 24 | 0xA0604);
        out.sprite(tex, tw, th, cx, cy, size, size, rot, (int)(alpha * 255.0f) << 24 | 0xFFFFFF);
        float roll = Math.min(1.0f, print.time / 0.95f);
        float eased = 1.0f - (1.0f - roll) * (1.0f - roll) * (1.0f - roll);
        int shown = Math.max(1, Math.round((float)print.number * eased));
        float textScale = 2.0f * (1.0f + 0.12f * (1.0f - roll));
        String caption = print.prize.title() + " #" + shown;
        float ty = cy + h * 0.42f * 0.62f;
        int ink = (int)(alpha * 255.0f) << 24 | 0x70C09;
        out.text(caption, w * 0.5f + textScale, ty + textScale, textScale, ink, true);
        out.text(caption, w * 0.5f, ty, textScale, (int)(alpha * 255.0f) << 24 | 0xF5C23A, true);
    }

    private static final class Print {
        BoaPrize prize = BoaPrize.NONE;
        int number;
        float time;
        float delayLeft;
        float fade;
        float tilt;
        boolean impacted;
        int ticksPlayed;
        boolean active;

        private Print() {
        }
    }
}

