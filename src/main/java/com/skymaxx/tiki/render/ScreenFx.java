package com.skymaxx.tiki.render;

import com.skymaxx.tiki.anim.AnimationDirector;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.render.RenderUtil;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ScreenFx {
    private final TikiConfig cfg;
    private final Random rng = new Random();
    private float shake;
    private float shakeX;
    private float shakeY;
    private float tension;

    public ScreenFx(TikiConfig cfg) {
        this.cfg = cfg;
    }

    public void impact(float magnitude) {
        this.shake = Math.max(this.shake, magnitude);
    }

    public void update(float dt, AnimationDirector director) {
        this.update(dt, director == null ? 0.0f : director.anticipation());
    }

    public void update(float dt, float anticipation) {
        if (this.shake > 0.0f) {
            this.shake = Math.max(0.0f, this.shake - dt * 26.0f);
            this.shakeX = (this.rng.nextFloat() - 0.5f) * 2.0f * this.shake;
            this.shakeY = (this.rng.nextFloat() - 0.5f) * 2.0f * this.shake;
        } else {
            this.shakeX = 0.0f;
            this.shakeY = 0.0f;
        }
        float target = anticipation;
        float rate = target > this.tension ? 5.5f : 2.4f;
        this.tension += (target - this.tension) * Math.min(1.0f, dt * rate);
    }

    public float shakeX() {
        return this.shakeX;
    }

    public float shakeY() {
        return this.shakeY;
    }

    public float tension() {
        return this.tension;
    }

    public void drawScrim(GuiGraphicsExtractor ctx, int w, int h, float strength, float anticipation) {
        if (strength <= 0.0f) {
            return;
        }
        float s = RenderUtil.clamp01(strength) * this.cfg.scrimStrength;
        float build = RenderUtil.clamp01(Math.max(anticipation, this.tension));
        ctx.fill(0, 0, w, h, RenderUtil.argb((int)(233.0f * s), 8, 12, 9));
        if (build > 0.01f) {
            ctx.fill(0, 0, w, h, RenderUtil.argb((int)(22.0f * build * s), 92, 44, 14));
        }
    }
}

