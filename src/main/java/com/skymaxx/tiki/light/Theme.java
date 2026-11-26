package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import java.time.LocalDate;
import java.time.Month;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Light: looks for the menu and for the HUDs (chosen separately in Settings). Every box has rounded corners.
 * Effects are a handful of fills per frame (a few dozen at most), so they never cost real performance:
 * Frost snows over the whole screen while /light is open and inside its HUDs, Clean snows in December,
 * Lava has a glowing lava pool at the bottom of the menu and embers in its HUDs, Ocean has waves,
 * Royal Gold an inner gold frame.
 */
public enum Theme {
    PINK("Pink Glass", 0xD01A0F1C, 0xE02A1428, 0x50000000, 0xFFFF8CC8, 0x14FFFFFF, 0xC8140812, 0xFFF2EEF3, 0xFFA79AAE, 0),
    MIDNIGHT("Midnight", 0xE8101218, 0xF0181C24, 0x60000000, 0xFF5EC8FF, 0x10FFFFFF, 0xD00E1016, 0xFFE8EEF5, 0xFF8C98A8, 0),
    OCEAN("Ocean", 0xE0081E30, 0xE80C2A40, 0x50000814, 0xFF3FE0C8, 0x123FE0C8, 0xCC061C2C, 0xFFE6FBFF, 0xFF8FBFCC, 1),
    CLEAN("Clean", 0x59000000, 0x66000000, 0x00000000, 0xFFFFFFFF, 0x10FFFFFF, 0x00000000, 0xFFFFFFFF, 0xFFCFCFCF, 2),
    LAVA("Lava", 0xE01E0A06, 0xE82C0E06, 0x50140400, 0xFFFF8A2A, 0x14FF7828, 0xCC1C0804, 0xFFFFE9D6, 0xFFC8987A, 3),
    FROST("Frost", 0xE02C4458, 0xE8345068, 0x40C8E6FF, 0xFFBFE9FF, 0x18BFE9FF, 0xCC223648, 0xFFF0F8FF, 0xFFA8C4D8, 4),
    GOLD("Royal Gold", 0xEC0C0A06, 0xF0161208, 0x50241C0C, 0xFFE8C060, 0x12E8C060, 0xD80E0C06, 0xFFF6EEDC, 0xFFB0A080, 5);

    public final String title;
    public final int glass, header, side, accent, ground, hudBg, text, dim;
    /** 0 none, 1 waves, 2 clean (snow in December), 3 lava, 4 frost (snow), 5 gold frame */
    public final int effect;

    Theme(String title, int glass, int header, int side, int accent, int ground, int hudBg, int text, int dim, int effect) {
        this.title = title;
        this.glass = glass;
        this.header = header;
        this.side = side;
        this.accent = accent;
        this.ground = ground;
        this.hudBg = hudBg;
        this.text = text;
        this.dim = dim;
        this.effect = effect;
    }

    public int soft() {
        return (0x99 << 24) | (this.accent & 0xFFFFFF);
    }

    public int line() {
        return (0x55 << 24) | (this.accent & 0xFFFFFF);
    }

    public int active() {
        return (0x45 << 24) | (this.accent & 0xFFFFFF);
    }

    public boolean noBox() {
        return this == CLEAN;
    }

    public static Theme byName(String n) {
        if (n != null) for (Theme t : values()) if (t.name().equalsIgnoreCase(n)) return t;
        return PINK;
    }

    public Theme next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public static Theme menu() {
        TikiConfig c = TikiGambleClient.config;
        return c == null ? PINK : byName(c.menuTheme);
    }

    public static Theme hud() {
        TikiConfig c = TikiGambleClient.config;
        return c == null ? PINK : byName(c.hudTheme);
    }

    public boolean snows() {
        return this.effect == 4 || (this.effect == 2 && LocalDate.now().getMonth() == Month.DECEMBER);
    }

    // ------------------------------------------------------------------ drawing helpers

    /** A filled rectangle with rounded corners (radius 2-3 px). */
    public static void round(GuiGraphicsExtractor ctx, int x0, int y0, int x1, int y1, int color) {
        if (x1 - x0 < 6 || y1 - y0 < 6) {
            ctx.fill(x0, y0, x1, y1, color);
            return;
        }
        ctx.fill(x0 + 3, y0, x1 - 3, y0 + 1, color);
        ctx.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 3, color);
        ctx.fill(x0, y0 + 3, x1, y1 - 3, color);
        ctx.fill(x0 + 1, y1 - 3, x1 - 1, y1 - 1, color);
        ctx.fill(x0 + 3, y1 - 1, x1 - 3, y1, color);
    }

    /** A 1 px outline with rounded corners. */
    public static void roundOutline(GuiGraphicsExtractor ctx, int x0, int y0, int x1, int y1, int color) {
        ctx.fill(x0 + 3, y0, x1 - 3, y0 + 1, color);
        ctx.fill(x0 + 3, y1 - 1, x1 - 3, y1, color);
        ctx.fill(x0, y0 + 3, x0 + 1, y1 - 3, color);
        ctx.fill(x1 - 1, y0 + 3, x1, y1 - 3, color);
        ctx.fill(x0 + 1, y0 + 1, x0 + 3, y0 + 2, color);
        ctx.fill(x0 + 1, y0 + 1, x0 + 2, y0 + 3, color);
        ctx.fill(x1 - 3, y0 + 1, x1 - 1, y0 + 2, color);
        ctx.fill(x1 - 2, y0 + 1, x1 - 1, y0 + 3, color);
        ctx.fill(x0 + 1, y1 - 2, x0 + 3, y1 - 1, color);
        ctx.fill(x0 + 1, y1 - 3, x0 + 2, y1 - 1, color);
        ctx.fill(x1 - 3, y1 - 2, x1 - 1, y1 - 1, color);
        ctx.fill(x1 - 2, y1 - 3, x1 - 1, y1 - 1, color);
    }

    /** HUD box in this theme (nothing for Clean), plus its little animation. */
    public void hudBox(GuiGraphicsExtractor ctx, int x0, int y0, int x1, int y1) {
        if (!noBox()) {
            round(ctx, x0, y0, x1, y1, this.hudBg);
            roundOutline(ctx, x0, y0, x1, y1, soft());
            if (this.effect == 5) roundOutline(ctx, x0 + 2, y0 + 2, x1 - 2, y1 - 2, (0x40 << 24) | (this.accent & 0xFFFFFF));
        }
        hudFx(ctx, x0, y0, x1, y1);
    }

    /** Snow drifting through the HUD (Frost, Clean in December) or embers rising (Lava). */
    public void hudFx(GuiGraphicsExtractor ctx, int x0, int y0, int x1, int y1) {
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        int w = Math.max(1, x1 - x0), h = Math.max(1, y1 - y0);
        if (snows()) {
            for (int i = 0; i < 7; i++) {
                float l = (t * (0.18f + (i % 3) * 0.05f) + i * 0.143f) % 1f;
                int x = x0 + (int) ((i * 37 + Math.sin(t + i) * 4) % w + w) % w;
                int y = y0 + (int) (l * h);
                ctx.fill(x, y, x + 1 + (i % 2), y + 1 + (i % 2), (int) (200 * (1 - l * 0.6f)) << 24 | 0xFFFFFF);
            }
        } else if (this.effect == 3) {
            for (int i = 0; i < 5; i++) {
                float l = (t * (0.25f + (i % 3) * 0.08f) + i * 0.2f) % 1f;
                int x = x0 + (i * 41) % w;
                int y = y1 - (int) (l * h);
                ctx.fill(x, y, x + 1, y + 1, (int) (220 * (1 - l)) << 24 | (i % 2 == 0 ? 0xFF9A2A : 0xFFD04A));
            }
        }
    }

    /** Menu effects inside the window (Lava pool, Ocean waves, Gold frame). */
    public void menuFx(GuiGraphicsExtractor ctx, int left, int top, int right, int bottom) {
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        switch (this.effect) {
            case 1 -> {
                for (int k = 0; k < 3; k++) {
                    int base = bottom - 10 - k * 9;
                    for (int x = left + 4; x < right - 4; x += 4) {
                        int y = base + (int) (Math.sin(x * 0.05 + k + t * 1.2) * 2);
                        ctx.fill(x, y, x + 4, y + 1, 0x1E3FE0C8);
                    }
                }
            }
            case 3 -> {
                // a glowing lava pool along the bottom edge, gently moving
                int pool = 7;
                for (int x = left + 1; x < right - 1; x += 3) {
                    int wave = (int) (Math.sin(x * 0.07 + t * 1.6) * 1.5 + Math.sin(x * 0.023 - t) * 1.0);
                    ctx.fill(x, bottom - pool + wave, x + 3, bottom - 1, 0xE0C83A10);
                    ctx.fill(x, bottom - pool + wave, x + 3, bottom - pool + wave + 1, 0xFFFFB347);
                }
                for (int i = 0; i < 4; i++) {
                    float l = (t * 0.7f + i * 0.25f) % 1f;
                    int x = left + 20 + (i * 97) % Math.max(1, right - left - 40);
                    int r = (int) (l * 3);
                    ctx.fill(x - r, bottom - 5 - r, x + r + 1, bottom - 4, (int) (180 * (1 - l)) << 24 | 0xFFD04A);
                }
                for (int i = 0; i < 8; i++) {
                    float l = (t * 0.22f + i * 0.125f) % 1f;
                    int x = left + 10 + (i * 61) % Math.max(1, right - left - 20);
                    int y = bottom - 8 - (int) (l * (bottom - top) * 0.6f);
                    ctx.fill(x, y, x + 1, y + 1, (int) (200 * (1 - l)) << 24 | 0xFF8A2A);
                }
            }
            case 5 -> roundOutline(ctx, left + 3, top + 3, right - 3, bottom - 3, 0x50E8C060);
            default -> {
            }
        }
    }

    /** Snow over the whole screen (Frost, Clean in December) while the menu is open. */
    public void screenSnow(GuiGraphicsExtractor ctx, int sw, int sh) {
        if (!snows()) return;
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        for (int i = 0; i < 80; i++) {
            float sp = 14 + (i * 37 % 30);
            float y = ((i * 53 + t * sp) % (sh + 10)) - 5;
            float x = (((i * 97 + t * -8 * (0.5f + (i % 5) * 0.2f) + (float) Math.sin(t * 1.1f + i) * 6) % (sw + 10)) + sw + 10) % (sw + 10) - 5;
            int s = i % 7 == 0 ? 2 : 1;
            ctx.fill((int) x, (int) y, (int) x + s, (int) y + s, (i % 3 == 0 ? 0xE0 : 0xA0) << 24 | 0xFFFFFF);
        }
    }
}
