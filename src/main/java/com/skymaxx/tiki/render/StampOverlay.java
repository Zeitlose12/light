package com.skymaxx.tiki.render;

import com.skymaxx.tiki.anim.Prize;
import com.skymaxx.tiki.audio.AudioDirector;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.render.RenderUtil;
import com.skymaxx.tiki.render.ScreenFx;
import com.skymaxx.tiki.render.TikiAssets;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

public class StampOverlay {
    private static final float SLAM = 0.15f;
    private static final float ROLL = 0.95f;
    private static final float TARGET_HEIGHT = 0.42f;
    private static final float SHADOW_OFFSET = 0.035f;
    private static final int SLOTS = 2;
    private static final float COMBO_LEAN = 0.085f;
    private static final float COMBO_BEAT = 0.2f;
    private final AudioDirector audio;
    private final Print[] prints = new Print[2];

    public StampOverlay(AudioDirector audio) {
        this.audio = audio;
        for (int i = 0; i < 2; ++i) {
            this.prints[i] = new Print();
        }
    }

    public void trigger(Prize prize, int number, TikiConfig cfg, Random rng) {
        if (prize == null || !prize.stamps() || !cfg.stampsEnabled) {
            return;
        }
        Print slot = this.free();
        if (slot == null) {
            return;
        }
        slot.prize = prize;
        slot.number = Math.max(1, number);
        slot.fadeDuration = cfg.stampFadeMinSeconds + rng.nextFloat() * Math.max(0.0f, cfg.stampFadeMaxSeconds - cfg.stampFadeMinSeconds);
        slot.tiltDegrees = -8.0f + rng.nextFloat() * 16.0f;
        slot.delayLeft = cfg.stampDelaySeconds;
        slot.time = 0.0f;
        slot.active = true;
        slot.impacted = false;
        slot.ticksPlayed = 0;
        slot.lean = 0.0f;
        Print other = this.otherThan(slot);
        if (other != null) {
            other.lean = -1.0f;
            slot.lean = 1.0f;
            slot.delayLeft += 0.2f;
            slot.tiltDegrees = -Math.signum(other.tiltDegrees) * (6.0f + rng.nextFloat() * 8.0f);
        }
    }

    private Print free() {
        for (Print print : this.prints) {
            if (print.active) continue;
            return print;
        }
        return null;
    }

    private Print otherThan(Print slot) {
        for (Print print : this.prints) {
            if (print == slot || !print.active) continue;
            return print;
        }
        return null;
    }

    public void update(float dt, ScreenFx fx) {
        for (Print print : this.prints) {
            this.update(print, dt, fx);
        }
    }

    private void update(Print print, float dt, ScreenFx fx) {
        if (!print.active) {
            return;
        }
        if (print.delayLeft > 0.0f) {
            print.delayLeft -= dt;
            return;
        }
        if (!print.impacted) {
            print.impacted = true;
            fx.impact(11.0f);
            this.audio.onStamp();
        }
        print.time += dt;
        int ticks = StampOverlay.countTicks(print);
        int wanted = Math.min(ticks, (int)(StampOverlay.rollProgress(print) * (float)ticks));
        while (print.ticksPlayed < wanted) {
            ++print.ticksPlayed;
            this.audio.onCountTick((float)print.ticksPlayed / (float)Math.max(1, ticks));
        }
        if (print.time >= 0.15f + print.fadeDuration) {
            print.active = false;
        }
    }

    private static int countTicks(Print print) {
        return Math.max(1, Math.min(14, print.number));
    }

    private static float rollProgress(Print print) {
        return RenderUtil.clamp01(print.time / 0.95f);
    }

    public boolean visible() {
        for (Print print : this.prints) {
            if (!print.active || !(print.delayLeft <= 0.0f)) continue;
            return true;
        }
        return false;
    }

    public void render(GuiGraphicsExtractor ctx, int screenW, int screenH) {
        if (!TikiAssets.ready()) {
            return;
        }
        int line = 0;
        for (Print print : this.prints) {
            if (!print.active || !(print.delayLeft <= 0.0f)) continue;
            this.render(print, ctx, screenW, screenH, line++);
        }
    }

    private void render(Print print, GuiGraphicsExtractor ctx, int screenW, int screenH, int line) {
        float alpha;
        float scale;
        float t;
        if (print.time < 0.15f) {
            t = print.time / 0.15f;
            scale = 3.2f - 2.2f * RenderUtil.easeOutCubic(t);
            alpha = Math.min(1.0f, t * 3.5f);
        } else {
            t = (print.time - 0.15f) / Math.max(0.05f, print.fadeDuration);
            scale = 1.0f + 0.04f * t;
            alpha = t < 0.33f ? 1.0f : 1.0f - (t - 0.33f) / 0.67f;
        }
        alpha = RenderUtil.clamp01(alpha);
        if (alpha <= 0.004f) {
            return;
        }
        Identifier texture = TikiAssets.stampFor(print.prize);
        if (texture == null) {
            return;
        }
        int texW = TikiAssets.widthOf(texture);
        int texH = TikiAssets.heightOf(texture);
        float target = (float)screenH * 0.42f;
        float base = target / (float)texH * scale;
        float kick = print.time < 0.15f ? 0.0f : (float)Math.exp(-(print.time - 0.15f) * 14.0f);
        float cx = (float)screenW * 0.5f + print.lean * 0.085f * (float)screenW + (float)Math.sin(print.time * 91.0f) * (float)screenH * 0.012f * kick;
        float cy = (float)screenH * 0.44f + (float)Math.cos(print.time * 77.0f) * (float)screenH * 0.012f * kick;
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.rotate((float)Math.toRadians(print.tiltDegrees));
        pose.scale(base, base);
        int drop = Math.max(2, (int)((float)texH * 0.035f));
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, -texW / 2 + drop, -texH / 2 + drop, 0.0f, 0.0f, texW, texH, texW, texH, RenderUtil.argb((int)(alpha * 170.0f), 10, 6, 4));
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, -texW / 2, -texH / 2, 0.0f, 0.0f, texW, texH, texW, texH, RenderUtil.withAlpha(-1, alpha));
        pose.popMatrix();
        Minecraft mc = Minecraft.getInstance();
        int shown = Math.max(1, Math.round((float)print.number * RenderUtil.easeOutCubic(StampOverlay.rollProgress(print))));
        MutableComponent label = Component.literal((String)(print.prize.title() + " #" + shown));
        pose.pushMatrix();
        pose.translate((float)screenW * 0.5f, cy + target * 0.62f + (float)(line * screenH) * 0.055f);
        float pop = 1.0f + 0.12f * (1.0f - StampOverlay.rollProgress(print));
        pose.scale(2.0f * pop, 2.0f * pop);
        ctx.centeredText(mc.font, (Component)label, 0, 0, RenderUtil.withAlpha(-671174, alpha));
        pose.popMatrix();
    }

    public void abort() {
        for (Print print : this.prints) {
            print.active = false;
        }
    }

    private static final class Print {
        Prize prize = Prize.NONE;
        int number;
        float time;
        float fadeDuration;
        float tiltDegrees;
        float delayLeft;
        boolean impacted;
        int ticksPlayed;
        float lean;
        boolean active;

        private Print() {
        }
    }
}

