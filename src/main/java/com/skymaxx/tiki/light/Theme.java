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
 * Royal Gold an inner gold frame. The newer themes (Halloween and the six achievement themes) draw their effects in {@link Fx}.
 */
public enum Theme {
    PINK("Pink Glass", 0xD01A0F1C, 0xE02A1428, 0x50000000, 0xFFFF8CC8, 0x14FFFFFF, 0xC8140812, 0xFFF2EEF3, 0xFFA79AAE, 0),
    MIDNIGHT("Midnight", 0xE8101218, 0xF0181C24, 0x60000000, 0xFF5EC8FF, 0x10FFFFFF, 0xD00E1016, 0xFFE8EEF5, 0xFF8C98A8, 0),
    OCEAN("Ocean", 0xE0081E30, 0xE80C2A40, 0x50000814, 0xFF3FE0C8, 0x123FE0C8, 0xCC061C2C, 0xFFE6FBFF, 0xFF8FBFCC, 1),
    CLEAN("Clean", 0x59000000, 0x66000000, 0x00000000, 0xFFFFFFFF, 0x10FFFFFF, 0x00000000, 0xFFFFFFFF, 0xFFCFCFCF, 2),
    LAVA("Lava", 0xE01E0A06, 0xE82C0E06, 0x50140400, 0xFFFF8A2A, 0x14FF7828, 0xCC1C0804, 0xFFFFE9D6, 0xFFC8987A, 3),
    FROST("Frost", 0xE02C4458, 0xE8345068, 0x40C8E6FF, 0xFFBFE9FF, 0x18BFE9FF, 0xCC223648, 0xFFF0F8FF, 0xFFA8C4D8, 4),
    GOLD("Royal Gold", 0xEC0C0A06, 0xF0161208, 0x50241C0C, 0xFFE8C060, 0x12E8C060, 0xD80E0C06, 0xFFF6EEDC, 0xFFB0A080, 5),
    HALLOWEEN("Halloween", 0xEC0A050A, 0xF0140A12, 0x50100008, 0xFFFF7A1A, 0x14FF7A1A, 0xD80C060A, 0xFFF5E6DA, 0xFFA08C80, 6),
    RADIOACTIVE("Radioactive", 0xE8061208, 0xF00A1A0C, 0x50001000, 0xFF7DFF4A, 0x147DFF4A, 0xD0061008, 0xFFEFFFE6, 0xFF98B890, 7, "GLOWING"),
    THUNDER("Thunderstorm", 0xE80B1020, 0xF0101830, 0x50000010, 0xFF8FB8FF, 0x148FB8FF, 0xD00A0E1C, 0xFFEAF0FF, 0xFF98A4C0, 8, "STORM_CHASER"),
    END_TIMES("End Times", 0xE8140604, 0xF01E0806, 0x50140000, 0xFFFF5030, 0x14FF5030, 0xD0140604, 0xFFFFE6DE, 0xFFC09080, 9, "BOUNTY_HUNTER"),
    ABYSS("Abyss", 0xEC020A14, 0xF0041220, 0x50000408, 0xFF5EF0FF, 0x105EF0FF, 0xD8020A14, 0xFFE0FAFF, 0xFF7FA8B8, 10, "FULL_SWEEP"),
    TREASURE("Sunken Treasure", 0xE0062428, 0xE80A3034, 0x50001414, 0xFFFFCF5A, 0x12FFCF5A, 0xCC06201E, 0xFFFFF6E0, 0xFFA8C0B0, 11, "HIGH_ROLLER"),
    MOONLIT("Moonlit Lake", 0xE80A1030, 0xF00E163C, 0x50000418, 0xFFCFD8FF, 0x12CFD8FF, 0xD00A1030, 0xFFF0F4FF, 0xFF9CA6C8, 12, "DRY_SPELL");

    public final String title;
    public final int glass, header, side, accent, ground, hudBg, text, dim;
    /** 0 none, 1 waves, 2 clean (snow in December), 3 lava, 4 frost (snow), 5 gold frame, 6 halloween, 7 radioactive,
     *  8 thunderstorm, 9 end times, 10 abyss, 11 sunken treasure, 12 moonlit lake */
    public final int effect;
    /** Achievement that unlocks this theme (enum name), or null = free for everyone. */
    public final String unlock;

    Theme(String title, int glass, int header, int side, int accent, int ground, int hudBg, int text, int dim, int effect) {
        this(title, glass, header, side, accent, ground, hudBg, text, dim, effect, null);
    }

    Theme(String title, int glass, int header, int side, int accent, int ground, int hudBg, int text, int dim, int effect, String unlock) {
        this.unlock = unlock;
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

    /** Locked = needs an achievement the player does not have yet (the Light Crew has everything). */
    public boolean locked() {
        if (this.unlock == null) return false;
        TikiConfig c = TikiGambleClient.config;
        if (c == null) return true;
        if (Crew.isLocalCrew()) return false;
        String have = c.lightAchievements == null ? "" : c.lightAchievements;
        if (!have.equals(cachedFor)) {
            cachedSet = Achievements.unlocked(c);
            cachedFor = have;
        }
        return !cachedSet.contains(this.unlock);
    }

    /** The menu asks this many times per frame: the achievement list is parsed only when it changes. */
    private static String cachedFor;
    private static java.util.Set<String> cachedSet = java.util.Set.of();

    /** The achievement that unlocks it, e.g. "Storm Chaser". */
    public String requirement() {
        Achievements a = this.unlock == null ? null : Achievements.byName(this.unlock);
        return a == null ? "" : a.title;
    }

    public String requirementText() {
        Achievements a = this.unlock == null ? null : Achievements.byName(this.unlock);
        return a == null ? "" : a.description;
    }

    public static int unlockedCount() {
        int n = 0;
        for (Theme t : values()) if (!t.locked()) n++;
        return n;
    }

    /** Themes unlocked by this achievement (shown in the centre pop-up when it unlocks). */
    public static Theme unlockedBy(String achievement) {
        for (Theme t : values()) if (achievement.equals(t.unlock)) return t;
        return null;
    }

    public Theme next() {
        Theme t = this;
        for (int i = 0; i < values().length; i++) {
            t = values()[(t.ordinal() + 1) % values().length];
            if (!t.locked()) return t;
        }
        return this;
    }

    public static Theme menu() {
        TikiConfig c = TikiGambleClient.config;
        Theme t = c == null ? PINK : byName(c.menuTheme);
        return t.locked() ? PINK : t;
    }

    public static Theme hud() {
        TikiConfig c = TikiGambleClient.config;
        Theme t = c == null ? PINK : byName(c.hudTheme);
        return t.locked() ? PINK : t;
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
            for (int i = 0; i < 11; i++) {
                float l = (t * (0.18f + (i % 3) * 0.05f) + i * 0.091f) % 1f;
                int s = 1 + (i % 2);
                // stays inside the HUD's frame (the yellow box when you move it)
                int x = x0 + (int) Math.floor(((hash(i) * w + Math.sin(t + i) * 4) % w + w) % w);
                x = Math.min(x, x1 - s);
                int y = Math.min(y0 + (int) (l * h), y1 - s);
                ctx.fill(x, y, x + s, y + s, (int) (200 * (1 - l * 0.6f)) << 24 | 0xFFFFFF);
            }
        } else if (this.effect == 3) {
            for (int i = 0; i < 8; i++) {
                float l = (t * (0.25f + (i % 3) * 0.08f) + i * 0.125f) % 1f;
                int x = Math.min(x0 + (int) (hash(i + 50) * w), x1 - 1);
                int y = Math.max(y0, Math.min(y1 - 1 - (int) (l * h), y1 - 1));
                ctx.fill(x, y, x + 1, y + 1, (int) (220 * (1 - l)) << 24 | (i % 2 == 0 ? 0xFF9A2A : 0xFFD04A));
            }
        } else if (this.effect >= 6) {
            Fx.hud(this.effect, ctx, x0, y0, x1, y1, t);
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
                for (int i = 0; i < 7; i++) {
                    float l = (t * 0.7f + i * 0.143f) % 1f;
                    int x = left + 20 + (int) (hash(i + 7) * Math.max(1, right - left - 40));
                    int r = (int) (l * 3);
                    ctx.fill(x - r, bottom - 5 - r, x + r + 1, bottom - 4, (int) (180 * (1 - l)) << 24 | 0xFFD04A);
                }
                for (int i = 0; i < 14; i++) {
                    float l = (t * 0.22f + i * 0.0714f) % 1f;
                    int x = left + 10 + (int) (hash(i + 23) * Math.max(1, right - left - 20));
                    int y = bottom - 8 - (int) (l * (bottom - top) * 0.6f);
                    ctx.fill(x, y, x + 1, y + 1, (int) (200 * (1 - l)) << 24 | 0xFF8A2A);
                }
            }
            case 5 -> roundOutline(ctx, left + 3, top + 3, right - 3, bottom - 3, 0x50E8C060);
            default -> {
                if (this.effect >= 6) Fx.menu(this.effect, ctx, left, top, right, bottom, t);
            }
        }
    }

    /** A fixed pseudo-random number in [0, 1) for particle i. */
    static float hash(int i) {
        double v = Math.sin(i * 12.9898 + 78.233) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    /** Snow over the whole screen (Frost, Clean in December) while the menu is open. */
    public void screenSnow(GuiGraphicsExtractor ctx, int sw, int sh) {
        if (!snows()) return;
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        for (int i = 0; i < 140; i++) {
            // every flake gets its own random column and speed (spread over the whole screen)
            float sp = 14 + hash(i + 300) * 26;
            float y = ((hash(i + 600) * (sh + 10) + t * sp) % (sh + 10)) - 5;
            float x = (((hash(i) * (sw + 10) - t * 8 * (0.5f + hash(i + 900) * 0.8f) + (float) Math.sin(t * 1.1f + i) * 6) % (sw + 10)) + sw + 10) % (sw + 10) - 5;
            int s = i % 7 == 0 ? 2 : 1;
            ctx.fill((int) x, (int) y, (int) x + s, (int) y + s, (i % 3 == 0 ? 0xE0 : 0xA0) << 24 | 0xFFFFFF);
        }
    }
}
