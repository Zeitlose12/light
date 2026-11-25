package com.skymaxx.tiki.render;

import com.skymaxx.tiki.render.RenderUtil;
import com.skymaxx.tiki.render.TikiAssets;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

public class JungleOverlay {
    private static final float MAX_INTRUSION = 0.23f;
    private static final float MIN_INTRUSION = 0.06f;
    private static final int BUNCH = 11;
    private static final float STEM_TUCK = 0.06f;
    private static final float MAX_ANGLE = 1.2f;
    private static final float SHADOW_OFFSET = 0.045f;
    private static final float SHADOW_ALPHA = 0.62f;
    private static final float MIRRORED_SHADE = 0.88f;
    private static final float SWAY_SPEEDUP = 1.1f;
    private static final float SWAY_GAIN = 1.6f;
    private static final float AGITATION_RATE = 2.2f;
    private static final float FINAL_SPEEDUP = 0.5f;
    private static final float[] DEPTHS = new float[]{0.48f, 0.72f, 1.0f};
    private static final float SOUND_LATE = 0.15f;
    private static final float[][] GUSTS = new float[][]{{0.0f, 1.0f, 0.0f}, {0.15f, 0.5f, 0.0f}, {0.3f, 0.35f, 0.0f}, {1.01f, 0.4f, 0.0f}, {1.3f, 0.24f, 0.0f}, {2.4f, 0.24f, 0.0f}, {3.15f, 0.85f, 0.0f}, {3.74f, 0.5f, -1.0f}, {4.19f, 0.5f, 1.0f}, {4.64f, 0.5f, -1.0f}, {5.04f, 0.5f, 1.0f}, {5.4900002f, 0.5f, -1.0f}, {5.84f, 0.5f, 1.0f}, {6.35f, 0.75f, 0.0f}};
    private static final float[][] IMPACT_GUSTS = new float[][]{{0.0f, 0.3f, 0.0f}};
    private static final float BELL_SECONDS = 2.8f;
    private static final float BEND_K = 70.0f;
    private static final float BEND_C = 4.2f;
    private static final float PUSH_K = 55.0f;
    private static final float PUSH_C = 5.5f;
    private static final float BEND_KICK = 2.7f;
    private static final float PUSH_KICK = 1.8f;
    private final Leaf[] leaves = new Leaf[11];
    private final Random rng = new Random();
    private float agitation;
    private boolean laidOut;
    private float jackpotTime = -1.0f;
    private float lastJackpotTime = -1.0f;
    private int jackpotLight = -9440544;
    private final float[] shiver = new float[2];
    private float impactTime = -1.0f;
    private float lastImpactTime = -1.0f;
    private int impactLight = -7270;
    private int flareLight = -9440544;
    private float hurry;

    public JungleOverlay() {
        for (int i = 0; i < this.leaves.length; ++i) {
            this.leaves[i] = new Leaf();
        }
    }

    private void layout() {
        for (int i = 0; i < this.leaves.length; ++i) {
            Leaf leaf = this.leaves[i];
            leaf.variant = this.rng.nextInt(6);
            leaf.anchor = -0.06f + ((float)i + 0.5f) / 11.0f * 1.12f + (this.rng.nextFloat() - 0.5f) * 0.08f;
            leaf.length = 0.28f + this.rng.nextFloat() * 0.3f;
            leaf.intrusion = 0.06f + this.rng.nextFloat() * 0.17f;
            leaf.tilt = (0.5f - leaf.anchor) * -1.25f + (this.rng.nextFloat() - 0.5f) * 0.35f;
            leaf.swayAmplitude = 0.035f + this.rng.nextFloat() * 0.055f;
            leaf.swayHz = 0.22f + this.rng.nextFloat() * 0.3f;
            leaf.phase = this.rng.nextFloat() * 6.2832f;
            leaf.delay = this.rng.nextFloat() * 0.45f;
            leaf.brightness = DEPTHS[this.rng.nextInt(DEPTHS.length)];
            leaf.alpha = 0.86f + this.rng.nextFloat() * 0.14f;
            leaf.response = 0.8f + this.rng.nextFloat() * 0.4f;
            leaf.shiverPhase = this.rng.nextFloat() * 6.2832f;
            for (int side = 0; side < 2; ++side) {
                leaf.bend[side] = 0.0f;
                leaf.bendVelocity[side] = 0.0f;
                leaf.push[side] = 0.0f;
                leaf.pushVelocity[side] = 0.0f;
                leaf.flare[side] = 0.0f;
            }
        }
        this.shiver[0] = 0.0f;
        this.shiver[1] = 0.0f;
        this.laidOut = true;
    }

    public void jackpot(float time, int light) {
        this.jackpotTime = time;
        if (light != 0) {
            this.jackpotLight = light;
        }
    }

    public void impact(float time, int light) {
        this.impactTime = time;
        if (light != 0) {
            this.impactLight = light;
        }
    }

    private void gusts(float now, float before) {
        this.gusts(GUSTS, now, before);
    }

    private void gusts(float[][] table, float now, float before) {
        for (float[] gust : table) {
            for (Leaf leaf : this.leaves) {
                float arrive = gust[0] + 0.03f + Math.abs(leaf.anchor - 0.5f) * 0.22f;
                if (!(arrive > before) || !(arrive <= now) || !(now - arrive < 0.5f)) continue;
                this.kick(leaf, gust[1], (int)gust[2]);
            }
        }
    }

    private void kick(Leaf leaf, float strength, int sides) {
        float away = leaf.anchor < 0.5f ? -1.0f : 1.0f;
        float reach = 0.55f + 0.9f * Math.abs(leaf.anchor - 0.5f);
        for (int side = 0; side < 2; ++side) {
            if (sides < 0 && side != 0 || sides > 0 && side != 1) continue;
            int n = side;
            leaf.bendVelocity[n] = leaf.bendVelocity[n] + away * strength * 2.7f * reach * leaf.response;
            int n2 = side;
            leaf.pushVelocity[n2] = leaf.pushVelocity[n2] + strength * 1.8f * leaf.response;
            leaf.flare[side] = Math.min(1.0f, leaf.flare[side] + strength * 0.85f);
            this.shiver[side] = Math.max(this.shiver[side], strength * 0.07f);
        }
    }

    private void settle(float dt) {
        int steps = Math.max(1, (int)Math.ceil(dt / 0.008333334f));
        float h = dt / (float)steps;
        for (int s = 0; s < steps; ++s) {
            for (Leaf leaf : this.leaves) {
                for (int side = 0; side < 2; ++side) {
                    int n = side;
                    leaf.bendVelocity[n] = leaf.bendVelocity[n] + (-70.0f * leaf.bend[side] - 4.2f * leaf.bendVelocity[side]) * h;
                    leaf.bend[side] = Math.max(-0.7f, Math.min(0.7f, leaf.bend[side] + leaf.bendVelocity[side] * h));
                    int n2 = side;
                    leaf.pushVelocity[n2] = leaf.pushVelocity[n2] + (-55.0f * leaf.push[side] - 5.5f * leaf.pushVelocity[side]) * h;
                    leaf.push[side] = Math.max(-0.12f, Math.min(0.45f, leaf.push[side] + leaf.pushVelocity[side] * h));
                }
            }
        }
        float flareFade = (float)Math.exp(-3.2f * dt);
        float bell = this.jackpotTime >= 0.0f && this.jackpotTime < 2.8f ? 0.035f * (1.0f - this.jackpotTime / 2.8f) : 0.0f;
        float shiverFade = (float)Math.exp(-2.8f * dt);
        for (int side = 0; side < 2; ++side) {
            this.shiver[side] = Math.max(bell, this.shiver[side] * shiverFade);
        }
        for (Leaf leaf : this.leaves) {
            leaf.flare[0] = leaf.flare[0] * flareFade;
            leaf.flare[1] = leaf.flare[1] * flareFade;
            leaf.shiverPhase += 81.681595f * leaf.response * dt;
            if (!(leaf.shiverPhase > 6.2832f)) continue;
            leaf.shiverPhase -= 6.2832f;
        }
    }

    public void update(float dt, float takeover, float wanted) {
        this.update(dt, takeover, wanted, 0.0f);
    }

    public void update(float dt, float takeover, float wanted, float quicken) {
        if (takeover <= 0.001f) {
            this.laidOut = false;
            this.agitation = 0.0f;
            this.hurry = 0.0f;
            this.lastJackpotTime = this.jackpotTime;
            this.lastImpactTime = this.impactTime;
            return;
        }
        if (!this.laidOut) {
            this.layout();
            this.agitation = 0.0f;
            this.hurry = 0.0f;
        }
        if (this.jackpotTime >= 0.0f) {
            wanted = Math.max(wanted, 1.35f);
            this.flareLight = this.jackpotLight;
            this.gusts(this.jackpotTime, this.lastJackpotTime < 0.0f || this.lastJackpotTime > this.jackpotTime ? -1.0f : this.lastJackpotTime);
        } else if (this.impactTime >= 0.0f) {
            this.flareLight = this.impactLight;
            this.gusts(IMPACT_GUSTS, this.impactTime, this.lastImpactTime < 0.0f || this.lastImpactTime > this.impactTime ? -1.0f : this.lastImpactTime);
        }
        this.lastJackpotTime = this.jackpotTime;
        this.lastImpactTime = this.impactTime;
        this.settle(dt);
        this.agitation += (wanted - this.agitation) * Math.min(1.0f, dt * 2.2f);
        this.hurry += (Math.max(0.0f, Math.min(1.0f, quicken)) - this.hurry) * Math.min(1.0f, dt * 2.2f);
        float rate = 6.2832f * (1.0f + this.agitation * 1.1f) * (1.0f + this.hurry * 0.5f) * dt;
        for (Leaf leaf : this.leaves) {
            leaf.phase += leaf.swayHz * rate;
            if (!(leaf.phase > 6.2832f)) continue;
            leaf.phase -= 6.2832f;
        }
    }

    public void render(GuiGraphicsExtractor ctx, int w, int h, float takeover) {
        this.render(ctx, w, h, takeover, 1.0f);
    }

    public void render(GuiGraphicsExtractor ctx, int w, int h, float takeover, float reach) {
        if (takeover <= 0.01f || !this.laidOut || !TikiAssets.ready()) {
            return;
        }
        for (Leaf leaf : this.leaves) {
            float entry = RenderUtil.clamp01((takeover - leaf.delay) / Math.max(0.05f, 1.0f - leaf.delay));
            if (entry <= 0.002f) continue;
            entry = RenderUtil.easeOutCubic(entry);
            this.draw(ctx, leaf, w, h, takeover, entry, false, reach);
            this.draw(ctx, leaf, w, h, takeover, entry, true, reach);
        }
    }

    private void draw(GuiGraphicsExtractor ctx, Leaf leaf, int w, int h, float takeover, float entry, boolean mirrored, float reach) {
        Identifier texture = mirrored ? TikiAssets.leafMirrored(leaf.variant) : TikiAssets.leaf(leaf.variant);
        int tw = TikiAssets.widthOf(texture);
        int th = TikiAssets.heightOf(texture);
        float reachPx = leaf.intrusion * reach * (float)w;
        float length = Math.max(leaf.length * (float)h, reachPx / 0.94f);
        float scale = length / (float)tw;
        float rest = reachPx - length;
        float out = -length - 8.0f;
        int side = mirrored ? 1 : 0;
        float x = Math.min(RenderUtil.lerp(out, rest, entry) - leaf.push[side] * length * entry, -0.06f * length);
        float phase = leaf.phase + (mirrored ? 3.1416f : 0.0f);
        float sway = leaf.swayAmplitude * (1.0f + this.agitation * 1.6f) * (float)Math.sin(phase);
        float tremble = this.shiver[side] * leaf.response * (float)Math.sin(leaf.shiverPhase + (float)side * 1.7f);
        float angle = Math.max(-1.2f, Math.min(1.2f, leaf.tilt + sway + leaf.bend[side] + tremble));
        float pivotX = mirrored ? (float)w - x : x;
        float pivotY = leaf.anchor * (float)h;
        float bright = leaf.brightness * (mirrored ? 0.88f : 1.0f);
        int colour = RenderUtil.shade(-1, bright);
        if (this.jackpotTime >= 0.0f || leaf.flare[side] > 0.01f) {
            float breathe = this.jackpotTime >= 0.0f ? RenderUtil.clamp01(this.jackpotTime / 0.5f) * (0.72f + 0.28f * (float)Math.sin(this.jackpotTime * 3.2f)) : 0.0f;
            float lit = Math.min(0.75f, 0.16f * breathe + 0.65f * leaf.flare[side]);
            int light = this.jackpotTime >= 0.0f ? this.jackpotLight : this.flareLight;
            colour = RenderUtil.lerpColor(colour, RenderUtil.lerpColor(light, -1, 0.3f), lit);
            colour = RenderUtil.shade(colour, 1.0f + 0.45f * leaf.flare[side]);
        }
        int tint = RenderUtil.withAlpha(colour, leaf.alpha * takeover);
        int shadow = RenderUtil.argb((int)(158.1f * takeover), 4, 10, 5);
        float drop = length * 0.045f;
        this.drawOne(ctx, texture, tw, th, pivotX + drop, pivotY + drop, angle, scale, mirrored, shadow);
        this.drawOne(ctx, texture, tw, th, pivotX, pivotY, angle, scale, mirrored, tint);
    }

    private void drawOne(GuiGraphicsExtractor ctx, Identifier texture, int tw, int th, float px, float py, float angle, float scale, boolean mirrored, int tint) {
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(px, py);
        pose.rotate(mirrored ? -angle : angle);
        pose.scale(scale, scale);
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, mirrored ? -tw : 0, -th / 2, 0.0f, 0.0f, tw, th, tw, th, tint);
        pose.popMatrix();
    }

    public void abort() {
        this.laidOut = false;
    }

    private static final class Leaf {
        int variant;
        float anchor;
        float length;
        float intrusion;
        float tilt;
        float swayAmplitude;
        float swayHz;
        float phase;
        float delay;
        float brightness;
        float alpha;
        float response;
        final float[] bend = new float[2];
        final float[] bendVelocity = new float[2];
        final float[] push = new float[2];
        final float[] pushVelocity = new float[2];
        final float[] flare = new float[2];
        float shiverPhase;

        private Leaf() {
        }
    }
}

