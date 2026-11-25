package com.skymaxx.tiki.render.boa;

import com.skymaxx.tiki.TikiGambleClient;
import java.lang.reflect.Method;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Light: lends the Titanoboa rifle and its scope reticle (SkyMaxx art, MIT) to other scenes, e.g. the
 * Ragnarok shooting gallery. Builds a small draw list and replays it with the Titanoboa HUD's own replay.
 * Display only.
 */
public final class LightRifle {
    private static final BoaRifle RIFLE = new BoaRifle();
    private static final BoaRifle.Pose POSE = new BoaRifle.Pose();
    private static final BoaDrawList LIST = new BoaDrawList();
    private static Method replay;
    private static boolean broken;

    private LightRifle() {
    }

    private static void fail(String what, Throwable e) {
        broken = true;
        TikiGambleClient.LOGGER.warn("[Light] Titanoboa " + what + " failed", e);
        try {
            if (TikiGambleClient.config != null && TikiGambleClient.config.debug) {
                TikiGambleClient.chat("\u00a7c[Light] Titanoboa " + what + " off: " + e);
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean ready() {
        if (broken) return false;
        try {
            if (!BoaAssets.ready()) BoaAssets.init();
            return BoaAssets.ready() && TikiGambleClient.boaHud() != null;
        } catch (Throwable t) {
            fail("assets", t);
            return false;
        }
    }

    private static void replay(GuiGraphicsExtractor ctx) throws Exception {
        if (replay == null) {
            replay = BoaHud.class.getDeclaredMethod("replay", GuiGraphicsExtractor.class, BoaDrawList.class);
            replay.setAccessible(true);
        }
        replay.invoke(TikiGambleClient.boaHud(), ctx, LIST);
    }

    /** The rifle held at the bottom right of a W x H screen; recoil 0..1 kicks it up. */
    public static void rifle(GuiGraphicsExtractor ctx, float w, float h, float recoil, float t, float alpha) {
        if (!ready()) return;
        try {
            float breathe = (float) Math.sin(t * 1.9);
            BoaRifle.Pose p = POSE;
            p.focal = 0.8f * h;
            p.moveX = 0.0f;
            p.moveY = 0.015f * breathe + 0.05f * recoil;
            p.moveZ = 0.25f * recoil;
            p.pitch = 0.35f * breathe - 9.0f * recoil;
            p.roll = -5.0f;
            p.yaw = 2.5f;
            p.boltLift = 0;
            p.boltBack = 0.42f * recoil;
            float aside = 1.6f * Math.max(0.7f, Math.min(1.0f, w / h / 1.78f));
            p.camX = -aside;
            p.camY = 0.95f;
            p.camZ = -3.3f;
            p.camYaw = 0.0f;
            p.camPitch = -4.0f;
            p.camRoll = 0.0f;
            p.centreX = w / 2.0f;
            p.centreY = h / 2.0f;
            p.ink = Math.max(1.1f, 0.0042f * h);
            p.glassDark = 0.0f;
            LIST.clear();
            LIST.alpha(alpha);
            RIFLE.draw(LIST, p);
            replay(ctx);
        } catch (Throwable e) {
            fail("rifle", e);
        }
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Lens of the scope on a W x H screen, like Titanoboa: centre x, centre y, radius. */
    public static float lensX(float w) {
        return w / 2.0f;
    }

    public static float lensY(float h) {
        return 0.535f * h;
    }

    public static float lensR(float h) {
        return 0.425f * h;
    }

    /** Seconds from raising the rifle until you look through the scope (same timing as Titanoboa). */
    public static final float AIM_TIME = 0.7f + 0.22f + 0.62f;

    /**
     * Titanoboa's "raise the rifle and look through the scope" shot: pt = seconds since it started.
     * Returns how far the zoom is (0..1); at 1 you are in the scope.
     */
    public static float aim(GuiGraphicsExtractor ctx, float w, float h, float pt, float alpha) {
        if (!ready()) return clamp01(pt / AIM_TIME);
        float rise = 0.7f, hold = 0.22f, zoom = 0.62f;
        float e = 1.0f - (1.0f - clamp01(pt / rise)) * (1.0f - clamp01(pt / rise));
        float z = clamp01((pt - rise - hold) / zoom);
        float s = z * z * (3 - 2 * z);
        try {
            float breathe = (float) Math.sin(pt * 1.9);
            BoaRifle.Pose p = POSE;
            p.focal = 0.8f * h;
            p.moveX = 0.0f;
            p.moveY = -3.2f * (1.0f - e) + 0.015f * breathe * (1.0f - s);
            p.moveZ = 0.0f;
            p.pitch = -24.0f * (1.0f - e) + 0.35f * breathe * (1.0f - s);
            p.roll = -14.0f * (1.0f - e) + lerp(-5.0f, 0.0f, s);
            p.yaw = lerp(2.5f, 0.0f, s);
            p.boltLift = 62.0f * (ramp(pt, 0.56f, 0.62f) - ramp(pt, 0.8f, 0.86f));
            p.boltBack = 0.42f * (ramp(pt, 0.62f, 0.68f) - ramp(pt, 0.74f, 0.8f));
            float end = -0.95f - p.focal * 0.2f / lensR(h);
            float aside = 1.6f * Math.max(0.7f, Math.min(1.0f, w / h / 1.78f));
            p.camX = lerp(-aside, 0.0f, s);
            p.camY = lerp(0.95f, 0.66f, s);
            p.camZ = lerp(-3.3f, end, s);
            p.camYaw = 0.0f;
            p.camPitch = lerp(-4.0f, 0.0f, s);
            p.camRoll = 0.0f;
            p.centreX = w / 2.0f;
            p.centreY = lerp(h / 2.0f, lensY(h), s);
            p.ink = Math.max(1.1f, 0.0042f * h * (1.0f + 0.8f * s));
            p.glassDark = clamp01((z - 0.55f) / 0.45f);
            LIST.clear();
            LIST.alpha(alpha);
            RIFLE.draw(LIST, p);
            float dark = clamp01((z - 0.8f) / 0.2f);
            if (dark > 0) LIST.fill(0, 0, w, h, ((int) (dark * 255) << 24) | 0x020203);
            replay(ctx);
        } catch (Throwable ex) {
            fail("rifle", ex);
        }
        return z;
    }

    private static float ramp(float t, float a, float b) {
        return clamp01((t - a) / (b - a));
    }

    /** Looking through the scope: Titanoboa's reticle, its lens mask and black around it. */
    public static void scope(GuiGraphicsExtractor ctx, float w, float h, float shakeX, float shakeY, int reticleArgb) {
        if (!ready()) return;
        try {
            float x = lensX(w) + shakeX, y = lensY(h) + shakeY, r = lensR(h);
            LIST.clear();
            LIST.alpha(1.0f);
            LIST.sprite(BoaTex.RETICLE, 512, 512, x, y, r * 2.0f, r * 2.0f, 0.0f, reticleArgb);
            LIST.sprite(BoaTex.SCOPE_MASK, 512, 512, x, y, r * 2.0f, r * 2.0f, 0.0f, -1);
            int black = -16645117;
            LIST.fill(0, 0, w, y - r + 0.5f, black);
            LIST.fill(0, y + r - 0.5f, w, h, black);
            LIST.fill(0, y - r + 0.5f, x - r + 0.5f, y + r - 0.5f, black);
            LIST.fill(x + r - 0.5f, y - r + 0.5f, w, y + r - 0.5f, black);
            replay(ctx);
        } catch (Throwable ex) {
            fail("scope", ex);
        }
    }

    /** The Titanoboa scope reticle centred on (x, y), radius r. */
    public static void reticle(GuiGraphicsExtractor ctx, float x, float y, float r, int argb) {
        if (!ready()) return;
        try {
            LIST.clear();
            LIST.alpha(1.0f);
            LIST.sprite(BoaTex.RETICLE, 512, 512, x, y, r * 2.0f, r * 2.0f, 0.0f, argb);
            replay(ctx);
        } catch (Throwable e) {
            fail("reticle", e);
        }
    }
}
