package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.joml.Matrix3x2fStack;

/**
 * Light: the big pop-up in the middle of the screen.
 * "NEW THEME UNLOCKED" (drawn in the new theme's own look) and the Light Crew banner for big drops in party chat.
 */
public final class CosmeticPopup {
    private static final long SHOW_MS = 5000L;
    private static final int W = 220;
    private static final int H = 58;
    private static Theme theme;
    private static String crewName;
    private static String crewDrop;
    private static long startMs = -1L;
    private static boolean sounded;

    private CosmeticPopup() {
    }

    /** Shows the theme pop-up; delayMs lets the achievement box appear first. */
    public static void theme(Theme t, long delayMs) {
        theme = t;
        crewName = null;
        startMs = System.currentTimeMillis() + delayMs;
        sounded = false;
    }

    public static void crewDrop(String name, String drop) {
        Minecraft.getInstance().execute(() -> {
            theme = null;
            crewName = name;
            crewDrop = drop;
            startMs = System.currentTimeMillis();
            sounded = false;
        });
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        if (startMs < 0) return;
        long t = System.currentTimeMillis() - startMs;
        if (t < 0) return;
        if (t > SHOW_MS) {
            startMs = -1L;
            return;
        }
        if (LavaScene.ownsScreen()) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (!sounded) {
                sounded = true;
                TikiConfig c = TikiGambleClient.config;
                float vol = c == null ? 0.5f : Math.max(0f, Math.min(1f, c.lightAchievementVolume));
                if (vol > 0f) mc.getSoundManager().play(SimpleSoundInstance.forUI(theme != null ? SoundEvents.PLAYER_LEVELUP : SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.2f, vol));
            }
            int x = (ctx.guiWidth() - W) / 2;
            int y = Math.max(10, ctx.guiHeight() / 4 - H / 2);
            if (theme != null) drawTheme(ctx, mc.font, x, y, t, theme);
            else if (crewName != null) drawCrew(ctx, mc.font, x, y, t, crewName, crewDrop);
        } catch (Throwable e) {
            startMs = -1L;
            TikiGambleClient.LOGGER.warn("[Light] cosmetic pop-up failed", e);
        }
    }

    private static float alpha(long t) {
        float in = Math.min(1f, t / 300f);
        float out = t > SHOW_MS - 500 ? Math.max(0f, (SHOW_MS - t) / 500f) : 1f;
        return Math.min(in, out);
    }

    /** Drawn in the unlocked theme's own colours, with its animation running inside. */
    public static void drawTheme(GuiGraphicsExtractor ctx, Font font, int x, int y, long t, Theme th) {
        float a = alpha(t);
        if (a <= 0.02f) return;
        float pop = t < 300 ? 0.85f + 0.15f * (float) Math.sin(t / 300f * Math.PI / 2) : 1f;
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(x + W / 2f, y + H / 2f);
        ps.scale(pop, pop);
        ps.translate(-W / 2f, -H / 2f);
        Theme.round(ctx, 2, 2, W + 2, H + 2, argb(a * 0.35f, 0));
        Theme.round(ctx, 0, 0, W, H, argb(a * ((th.glass >>> 24) / 255f), th.glass & 0xFFFFFF));
        if (a > 0.6f) {
            ctx.enableScissor(1, 1, W - 1, H - 1); // the scissor follows the pose
            th.menuFx(ctx, 0, 0, W, H);
            ctx.disableScissor();
        }
        float glow = 0.6f + 0.4f * (float) Math.sin(t / 140.0);
        Theme.roundOutline(ctx, 0, 0, W, H, argb(a * glow, th.accent & 0xFFFFFF));
        center(ctx, font, "NEW THEME UNLOCKED", W / 2, 7, argb(a, th.accent & 0xFFFFFF), 1f);
        center(ctx, font, th.title, W / 2, 20, argb(a, 0xFFFFFF), 2f);
        center(ctx, font, "/light → Cosmetics", W / 2, 44, argb(a * 0.9f, th.dim & 0xFFFFFF), 1f);
        ps.popMatrix();
    }

    private static void drawCrew(GuiGraphicsExtractor ctx, Font font, int x, int y, long t, String name, String drop) {
        float a = alpha(t);
        if (a <= 0.02f) return;
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(x, y);
        Theme.round(ctx, 2, 2, W + 2, H + 2, argb(a * 0.35f, 0));
        Theme.round(ctx, 0, 0, W, H, argb(a * 0.88f, 0x1A0F1C));
        float time = t / 1000f;
        for (int i = 0; i < 18; i++) {
            float k = (time * (0.4f + (i % 3) * 0.15f) + i * 0.055f) % 1f;
            int px = 4 + (int) (Theme.hash(i + 900) * (W - 8));
            int py = H - 3 - (int) (k * (H - 6));
            ctx.fill(px, py, px + 1, py + 1, argb(a * (1 - k), Crew.hsb(i / 18f + time * 0.2f, 0.6f, 1f)));
        }
        int rainbow = Crew.hsb(time * 0.25f, 0.45f, 1f);
        Theme.roundOutline(ctx, 0, 0, W, H, argb(a, rainbow));
        center(ctx, font, "★ LIGHT CREW ★", W / 2, 7, argb(a, 0xFFB0F0), 1f);
        center(ctx, font, name, W / 2, 20, argb(a, 0xFFFFFF), 1.5f);
        center(ctx, font, "got " + drop + "!", W / 2, 40, argb(a, 0xFFD24A), 1f);
        ps.popMatrix();
    }

    private static void center(GuiGraphicsExtractor ctx, Font font, String s, int cx, int y, int color, float scale) {
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(cx, y);
        ps.scale(scale, scale);
        ctx.text(font, Component.literal(s), -font.width(s) / 2, 0, color);
        ps.popMatrix();
    }

    private static int argb(float a, int rgb) {
        int al = Math.max(0, Math.min(255, Math.round(a * 255f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }
}
