package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/** Light: the "Achievement unlocked!" box (top right by default, movable in the HUDs tab). */
public final class AchievementPopup {
    public static final int W = 170;
    public static final int H = 36;
    private static final long SHOW_MS = 4500L;
    private static String title;
    private static String desc;
    private static long startMs = -1L;

    private AchievementPopup() {
    }

    public static void show(String t, String d) {
        title = t;
        desc = d;
        startMs = System.currentTimeMillis();
    }

    private static TikiConfig cfg() {
        return TikiGambleClient.config;
    }

    public static int boxX(TikiConfig c, int sw) {
        int w = Math.round(W * c.achPopupScale);
        return c.achPopupX < 0 ? sw - w - 6 : Math.round(c.achPopupX * sw);
    }

    public static int boxY(TikiConfig c, int sh) {
        return c.achPopupY < 0 ? 6 : Math.round(c.achPopupY * sh);
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        if (startMs < 0 || title == null) return;
        long t = System.currentTimeMillis() - startMs;
        if (t > SHOW_MS) {
            startMs = -1L;
            return;
        }
        TikiConfig c = cfg();
        if (c == null) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            draw(ctx, mc.font, boxX(c, ctx.guiWidth()), boxY(c, ctx.guiHeight()), c.achPopupScale, t, title, desc);
        } catch (Throwable e) {
            startMs = -1L;
            TikiGambleClient.LOGGER.warn("[Light] achievement pop-up failed", e);
        }
    }

    /** Slides in from above, stays, fades out. t in ms since it started. */
    public static void draw(GuiGraphicsExtractor ctx, Font font, int x, int y, float scale, long t, String title, String desc) {
        float in = Math.min(1f, t / 250f);
        float out = t > SHOW_MS - 400 ? Math.max(0f, (SHOW_MS - t) / 400f) : 1f;
        float a = Math.min(in, out);
        float slide = (1f - in) * -20f;
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(x, y + slide);
        ps.scale(scale, scale);
        Theme th = Theme.hud();
        if (!th.noBox()) {
            Theme.round(ctx, 0, 0, W, H, argb(a * ((th.hudBg >>> 24) / 255f), th.hudBg & 0xFFFFFF));
            Theme.roundOutline(ctx, 0, 0, W, H, argb(a, 0xFFD24A));
        }
        th.hudFx(ctx, 0, 0, W, H);
        float glow = 0.5f + 0.5f * (float) Math.sin(t / 120.0);
        if (!th.noBox()) ctx.fill(3, 1, W - 3, 2, argb(a * glow, 0xFFF3B0));
        ctx.text(font, Component.literal("Achievement unlocked!"), 8, 6, argb(a, 0xFFD24A));
        String line = "★ " + title;
        while (line.length() > 3 && font.width(line) > W - 14) line = line.substring(0, line.length() - 1);
        ctx.text(font, Component.literal(line), 8, 20, argb(a, 0xFFFFFF));
        ps.popMatrix();
    }

    private static int argb(float a, int rgb) {
        int al = Math.max(0, Math.min(255, Math.round(a * 255f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }
}
