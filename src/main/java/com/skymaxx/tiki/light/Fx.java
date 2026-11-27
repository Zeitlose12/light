package com.skymaxx.tiki.light;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Light: the animated effects of the newer themes. Every effect is a few dozen fills per frame and scales with the
 * area it is drawn in, so the same code draws the full /light window and the small preview tiles in Cosmetics.
 */
final class Fx {
    private Fx() {
    }

    private static float h(int i) {
        return Theme.hash(i);
    }

    private static int a(float alpha, int rgb) {
        return Math.max(0, Math.min(255, (int) (alpha * 255f))) << 24 | (rgb & 0xFFFFFF);
    }

    /** Filled circle. */
    static void disc(GuiGraphicsExtractor ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            ctx.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    static void menu(int effect, GuiGraphicsExtractor ctx, int l, int top, int r, int b, float t) {
        int w = Math.max(1, r - l), hh = Math.max(1, b - top);
        switch (effect) {
            case 6 -> halloween(ctx, l, top, r, b, w, hh, t, true);
            case 7 -> radioactive(ctx, l, top, r, b, w, hh, t, true);
            case 8 -> thunder(ctx, l, top, r, b, w, hh, t, true);
            case 9 -> endTimes(ctx, l, top, r, b, w, hh, t, true);
            case 10 -> abyss(ctx, l, top, r, b, w, hh, t, true);
            case 11 -> treasure(ctx, l, top, r, b, w, hh, t, true);
            case 12 -> moonlit(ctx, l, top, r, b, w, hh, t, true);
            default -> {
            }
        }
    }

    /** Small, calm versions for HUD boxes (always inside the box). */
    static void hud(int effect, GuiGraphicsExtractor ctx, int l, int top, int r, int b, float t) {
        int w = Math.max(1, r - l), hh = Math.max(1, b - top);
        switch (effect) {
            case 6 -> {
                for (int i = 0; i < 6; i++) {
                    float k = (t * (0.2f + (i % 3) * 0.06f) + i * 0.17f) % 1f;
                    int x = Math.min(l + (int) (h(i + 70) * w), r - 1);
                    int y = Math.max(top, b - 1 - (int) (k * hh));
                    ctx.fill(x, y, x + 1, y + 1, a(0.85f * (1 - k), i % 3 == 0 ? 0xB060FF : 0xFF8A2A));
                }
            }
            case 7 -> {
                for (int i = 0; i < 7; i++) {
                    float k = (t * (0.18f + (i % 3) * 0.05f) + i * 0.143f) % 1f;
                    int x = Math.min(l + (int) (h(i + 80) * w), r - 2);
                    int y = Math.max(top, b - 2 - (int) (k * hh));
                    ctx.fill(x, y, x + (i % 2 == 0 ? 2 : 1), y + (i % 2 == 0 ? 2 : 1), a(0.8f * (1 - k), 0x7DFF4A));
                }
            }
            case 8 -> {
                for (int i = 0; i < 9; i++) {
                    float k = (t * (0.9f + h(i + 90) * 0.4f) + h(i + 91)) % 1f;
                    int x = Math.min(l + (int) (((h(i + 92) * w - k * 6) % w + w) % w), r - 1);
                    int y = top + (int) (k * Math.max(1, hh - 3));
                    ctx.fill(x, y, x + 1, y + 3, 0x70A8C8FF);
                }
                if ((t % 7f) < 0.12f) Theme.roundOutline(ctx, l, top, r, b, 0x90FFFFFF);
            }
            case 9 -> {
                for (int i = 0; i < 7; i++) {
                    float k = (t * (0.12f + (i % 3) * 0.04f) + i * 0.143f) % 1f;
                    int x = Math.min(l + (int) (((h(i + 100) * w + Math.sin(t + i) * 3) % w + w) % w), r - 1);
                    int y = Math.min(top + (int) (k * hh), b - 1);
                    ctx.fill(x, y, x + 1, y + 1, a(0.8f * (1 - k * 0.5f), i % 3 == 0 ? 0xFF5030 : 0x9A8C88));
                }
            }
            case 10 -> {
                for (int i = 0; i < 8; i++) {
                    float p = 0.5f + 0.5f * (float) Math.sin(t * 1.7f + i * 1.3f);
                    int x = Math.min(l + 1 + (int) (h(i + 110) * (w - 2)), r - 2);
                    int y = Math.min(top + 1 + (int) (((h(i + 111) * hh - t * 2) % hh + hh) % hh), b - 2);
                    ctx.fill(x, y, x + 1, y + 1, a(0.9f * p, i % 3 == 0 ? 0x9A7CFF : 0x5EF0FF));
                }
            }
            case 11 -> {
                for (int i = 0; i < 5; i++) {
                    float k = (t * (0.2f + (i % 3) * 0.05f) + i * 0.2f) % 1f;
                    int x = Math.min(l + (int) (h(i + 120) * w), r - 2);
                    int y = Math.max(top, b - 2 - (int) (k * hh));
                    ctx.fill(x, y, x + 1, y + 1, a(0.7f * (1 - k), 0xDFF6FF));
                }
                for (int i = 0; i < 3; i++) {
                    if (h(i * 7 + (int) (t * 2f)) > 0.6f) {
                        int x = Math.min(l + 2 + (int) (h(i + 125) * (w - 4)), r - 2);
                        ctx.fill(x, b - 3, x + 1, b - 2, 0xF0FFE08A);
                    }
                }
            }
            case 12 -> {
                for (int i = 0; i < 8; i++) {
                    float p = 0.5f + 0.5f * (float) Math.sin(t * 2.3f + i * 2.1f);
                    int x = Math.min(l + 1 + (int) (h(i + 130) * (w - 2)), r - 2);
                    int y = Math.min(top + 1 + (int) (h(i + 131) * (hh - 2)), b - 2);
                    ctx.fill(x, y, x + 1, y + 1, a(0.25f + 0.7f * p, 0xEEF2FF));
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ Halloween: fog, bats with red eyes, evil pumpkins, embers

    private static void halloween(GuiGraphicsExtractor ctx, int l, int top, int r, int b, int w, int hh, float t, boolean big) {
        // fog drifting along the bottom
        for (int k = 0; k < 3; k++) {
            int base = b - 4 - k * Math.max(2, hh / 18);
            for (int x = l + 1; x < r - 1; x += 4) {
                int y = base - (int) ((Math.sin(x * 0.045 + t * (0.6 + k * 0.25) + k * 2) * 0.5 + 0.5) * Math.max(2, hh / 14));
                ctx.fill(x, y, Math.min(x + 4, r - 1), b - 1, 0x12B8A8C8);
            }
        }
        // embers
        int embers = Math.max(3, w / 40);
        for (int i = 0; i < embers; i++) {
            float k = (t * (0.15f + (i % 3) * 0.05f) + i * 0.113f) % 1f;
            int x = l + 4 + (int) (h(i + 200) * (w - 8));
            int y = b - 3 - (int) (k * hh * 0.7f);
            ctx.fill(x, y, x + 1, y + 1, a(0.8f * (1 - k), i % 4 == 0 ? 0xB060FF : 0xFF8A2A));
        }
        // evil pumpkins sitting on the bottom edge
        int pumpkins = Math.max(1, Math.min(4, w / 110));
        int pr = hh < 60 ? 3 : 5;
        for (int i = 0; i < pumpkins; i++) {
            int cx = l + (int) ((i + 0.5f) * w / pumpkins + (h(i + 210) - 0.5f) * 20);
            cx = Math.max(l + pr + 2, Math.min(r - pr - 3, cx));
            int cy = b - pr - 2;
            disc(ctx, cx, cy, pr, 0xFFB8500E);
            ctx.fill(cx - pr / 2, cy - pr, cx - pr / 2 + 1, cy + pr, 0x40000000);
            ctx.fill(cx + pr / 2, cy - pr, cx + pr / 2 + 1, cy + pr, 0x40000000);
            ctx.fill(cx, cy - pr - 2, cx + 1, cy - pr, 0xFF3A6A1E);
            float flick = 0.7f + 0.3f * (float) Math.sin(t * 9f + i * 3);
            int glow = a(flick, 0xFFE070);
            if (pr >= 5) {
                ctx.fill(cx - 3, cy - 2, cx - 1, cy - 1, glow);
                ctx.fill(cx - 2, cy - 1, cx - 1, cy, glow);
                ctx.fill(cx + 1, cy - 2, cx + 3, cy - 1, glow);
                ctx.fill(cx + 1, cy - 1, cx + 2, cy, glow);
                ctx.fill(cx - 3, cy + 2, cx + 4, cy + 3, glow);
                ctx.fill(cx - 2, cy + 1, cx - 1, cy + 2, glow);
                ctx.fill(cx + 2, cy + 1, cx + 3, cy + 2, glow);
            } else {
                ctx.fill(cx - 2, cy - 1, cx - 1, cy, glow);
                ctx.fill(cx + 1, cy - 1, cx + 2, cy, glow);
                ctx.fill(cx - 1, cy + 1, cx + 2, cy + 2, glow);
            }
        }
        // bats crossing the window, red eyes
        int bats = Math.max(2, Math.min(8, w / 55));
        for (int i = 0; i < bats; i++) {
            float sp = 14 + h(i + 220) * 18;
            int x = l + (int) (((h(i + 221) * w + t * sp * (i % 2 == 0 ? 1 : -1)) % w + w) % w);
            int y = top + 4 + (int) (h(i + 222) * hh * 0.55f + Math.sin(t * 2.2f + i) * 4);
            if (x < l + 4 || x > r - 6 || y > b - 12) continue;
            boolean up = Math.sin(t * 14f + i * 1.7f) > 0;
            int body = 0xF0140810;
            ctx.fill(x, y, x + 3, y + 2, body);
            ctx.fill(x - 3, up ? y - 1 : y + 1, x, up ? y : y + 2, body);
            ctx.fill(x + 3, up ? y - 1 : y + 1, x + 6, up ? y : y + 2, body);
            ctx.fill(x - 4, up ? y - 2 : y + 2, x - 3, up ? y - 1 : y + 3, body);
            ctx.fill(x + 6, up ? y - 2 : y + 2, x + 7, up ? y - 1 : y + 3, body);
            ctx.fill(x, y, x + 1, y + 1, 0xFFFF2424);
            ctx.fill(x + 2, y, x + 3, y + 1, 0xFFFF2424);
        }
    }

    // ------------------------------------------------------------------ Radioactive: toxic pool, rising bubbles, pulsing glow

    private static void radioactive(GuiGraphicsExtractor ctx, int l, int top, int r, int b, int w, int hh, float t, boolean big) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2.4f);
        Theme.roundOutline(ctx, l + 2, top + 2, r - 2, b - 2, a(0.12f + 0.25f * pulse, 0x7DFF4A));
        int pool = Math.max(3, hh / 26);
        for (int x = l + 1; x < r - 1; x += 3) {
            int wave = (int) (Math.sin(x * 0.08 + t * 2.0) * 1.2);
            ctx.fill(x, b - pool + wave, Math.min(x + 3, r - 1), b - 1, 0x9030B01A);
            ctx.fill(x, b - pool + wave, Math.min(x + 3, r - 1), b - pool + wave + 1, a(0.6f + 0.4f * pulse, 0xB6FF7A));
        }
        int n = Math.max(4, w / 30);
        for (int i = 0; i < n; i++) {
            float k = (t * (0.16f + (i % 4) * 0.05f) + i * 0.0913f) % 1f;
            int x = l + 4 + (int) (h(i + 300) * (w - 8) + Math.sin(t * 2 + i) * 2);
            int y = b - pool - (int) (k * (hh - pool) * 0.85f);
            int s = i % 3 == 0 ? 3 : 2;
            int c = a(0.75f * (1 - k), 0x9CFF6A);
            ctx.fill(x, y, x + s, y + 1, c);
            ctx.fill(x, y + s - 1, x + s, y + s, c);
            ctx.fill(x, y, x + 1, y + s, c);
            ctx.fill(x + s - 1, y, x + s, y + s, c);
        }
    }

    // ------------------------------------------------------------------ Thunderstorm: rain and lightning

    private static void thunder(GuiGraphicsExtractor ctx, int l, int top, int r, int b, int w, int hh, float t, boolean big) {
        int n = Math.max(10, Math.min(70, w * hh / 1400));
        for (int i = 0; i < n; i++) {
            float sp = 0.7f + h(i + 400) * 0.5f;
            float k = (t * sp + h(i + 401)) % 1f;
            int y = top + 2 + (int) (k * (hh - 6));
            int x = l + 2 + (int) (((h(i + 402) * w - k * 14) % (w - 4) + (w - 4)) % (w - 4));
            ctx.fill(x, y, x + 1, y + 3, 0x509CBCFF);
        }
        float cycle = t / 5.5f;
        int id = (int) Math.floor(cycle);
        float in = (cycle - id) * 5.5f;
        if (in < 0.35f) {
            float f = in < 0.08f ? 1f : (in > 0.16f && in < 0.22f ? 0.8f : 0.35f);
            ctx.fill(l + 1, top + 1, r - 1, b - 1, a(0.14f * f, 0xDDE8FF));
            int x = l + 10 + (int) (h(id + 410) * (w - 20));
            int y = top + 2;
            int segs = 7;
            int step = Math.max(2, (int) (hh * 0.7f) / segs);
            for (int s = 0; s < segs; s++) {
                int nx = x + (int) ((h(id * 13 + s + 420) - 0.5f) * 12);
                nx = Math.max(l + 3, Math.min(r - 4, nx));
                ctx.fill(x, y, x + 1, y + step, a(f, 0xF4F8FF));
                ctx.fill(Math.min(x, nx), y + step, Math.max(x, nx) + 1, y + step + 1, a(f, 0xF4F8FF));
                x = nx;
                y += step;
            }
        }
    }

    // ------------------------------------------------------------------ End Times: eclipse, crimson glow, falling ash

    private static void endTimes(GuiGraphicsExtractor ctx, int l, int top, int r, int b, int w, int hh, float t, boolean big) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 1.4f);
        int glow = Math.max(4, hh / 7);
        for (int k = 0; k < glow; k += 2) {
            float f = 1f - k / (float) glow;
            ctx.fill(l + 1, b - 2 - k, r - 1, b - k, a(0.18f * f * (0.6f + 0.4f * pulse), 0xC8200C));
        }
        int rr = Math.max(3, Math.min(10, hh / 7));
        int cx = r - rr - 8, cy = top + rr + 6;
        disc(ctx, cx, cy, rr + 2, a(0.25f + 0.2f * pulse, 0xFF5030));
        disc(ctx, cx, cy, rr + 1, 0xFFB02814);
        disc(ctx, cx - 1, cy, rr, 0xFF100404);
        int n = Math.max(6, w / 22);
        for (int i = 0; i < n; i++) {
            float k = (t * (0.08f + (i % 4) * 0.03f) + i * 0.071f) % 1f;
            int x = l + 3 + (int) (((h(i + 500) * (w - 6) + Math.sin(t * 0.8f + i) * 5) % (w - 6) + (w - 6)) % (w - 6));
            int y = top + 2 + (int) (k * (hh - 4));
            ctx.fill(x, y, x + 1, y + 1, a(0.75f * (1 - k * 0.6f), i % 4 == 0 ? 0xFF6A3A : 0x8C807C));
        }
    }

    // ------------------------------------------------------------------ Abyss: glowing plankton and a jellyfish

    private static void abyss(GuiGraphicsExtractor ctx, int l, int top, int r, int b, int w, int hh, float t, boolean big) {
        for (int k = 0; k < Math.max(3, hh / 10); k += 2) {
            ctx.fill(l + 1, b - 2 - k, r - 1, b - k, 0x10000000);
        }
        int n = Math.max(8, w / 18);
        for (int i = 0; i < n; i++) {
            float p = 0.5f + 0.5f * (float) Math.sin(t * (1.2f + h(i + 600)) + i * 1.9f);
            int x = l + 3 + (int) (((h(i + 601) * (w - 6) + Math.sin(t * 0.3f + i) * 6) % (w - 6) + (w - 6)) % (w - 6));
            int y = top + 3 + (int) (((h(i + 602) * (hh - 6) - t * (1.5f + h(i + 603) * 2)) % (hh - 6) + (hh - 6)) % (hh - 6));
            int s = i % 6 == 0 ? 2 : 1;
            ctx.fill(x, y, x + s, y + s, a(0.2f + 0.75f * p, i % 4 == 0 ? 0x9A7CFF : 0x5EF0FF));
        }
        int jelly = hh < 60 ? 1 : 2;
        for (int j = 0; j < jelly; j++) {
            float k = (t * 0.035f + j * 0.5f) % 1f;
            int jr = hh < 60 ? 3 : 5;
            int cx = l + (int) ((0.25f + 0.5f * j + 0.08f * (float) Math.sin(t * 0.5f + j)) * w);
            int cy = b - 6 - (int) (k * (hh - 12));
            float glow = 0.45f + 0.25f * (float) Math.sin(t * 2f + j);
            int col = a(glow, j == 0 ? 0xC88CFF : 0x7CF0FF);
            for (int dy = -jr; dy <= 0; dy++) {
                int dx = (int) Math.round(Math.sqrt(jr * jr - dy * dy));
                ctx.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, col);
            }
            for (int tn = -jr + 1; tn < jr; tn += 2) {
                int len = jr + 2 + (int) (Math.sin(t * 3f + tn) * 1.5f);
                for (int q = 1; q <= len; q++) {
                    int sx = cx + tn + (int) Math.round(Math.sin(t * 3f + q * 0.8f + tn) * 0.8f);
                    ctx.fill(sx, cy + q, sx + 1, cy + q + 1, a(glow * (1f - q / (float) (len + 1)), j == 0 ? 0xC88CFF : 0x7CF0FF));
                }
            }
        }
    }

    // ------------------------------------------------------------------ Sunken Treasure: light rays, bubbles, sand with gold

    private static void treasure(GuiGraphicsExtractor ctx, int l, int top, int r, int b, int w, int hh, float t, boolean big) {
        int rays = Math.max(2, Math.min(4, w / 90));
        for (int k = 0; k < rays; k++) {
            int base = l + (int) ((k + 0.5f) * w / rays + Math.sin(t * 0.3f + k) * 8);
            float alpha = 0.05f + 0.03f * (float) Math.sin(t * 0.8f + k * 2);
            int len = (int) (hh * 0.75f);
            for (int y = 0; y < len; y += 3) {
                int x = base + y * 2 / 5;
                if (x + 7 >= r - 1) break;
                ctx.fill(Math.max(l + 1, x), top + 1 + y, x + 7, top + 4 + y, a(alpha * (1 - y / (float) len), 0xE8FFF0));
            }
        }
        int sand = Math.max(3, hh / 22);
        for (int x = l + 1; x < r - 1; x += 4) {
            int bump = (int) ((Math.sin(x * 0.06) * 0.5 + 0.5) * 2);
            ctx.fill(x, b - sand - bump, Math.min(x + 4, r - 1), b - 1, 0x70C8A860);
        }
        int coins = Math.max(3, w / 25);
        for (int i = 0; i < coins; i++) {
            int x = l + 4 + (int) (h(i + 700) * (w - 8));
            int y = b - 2 - (int) (h(i + 701) * sand);
            ctx.fill(x, y, x + 2, y + 1, 0xFFE8B030);
            if (h(i * 5 + (int) (t * 3f)) > 0.8f) ctx.fill(x, y - 1, x + 1, y, 0xFFFFFFFF);
        }
        if (hh >= 60) {
            int cx = r - 26, cy = b - sand - 7;
            ctx.fill(cx, cy, cx + 12, cy + 7, 0xFF6A3E1C);
            ctx.fill(cx, cy + 2, cx + 12, cy + 3, 0xFFE8B030);
            ctx.fill(cx + 5, cy + 2, cx + 7, cy + 5, 0xFFE8B030);
            ctx.fill(cx + 1, cy - 1, cx + 11, cy, a(0.5f + 0.5f * (float) Math.sin(t * 3f), 0xFFE08A));
        }
        int n = Math.max(4, w / 35);
        for (int i = 0; i < n; i++) {
            float k = (t * (0.14f + (i % 3) * 0.05f) + i * 0.137f) % 1f;
            int x = l + 4 + (int) (h(i + 720) * (w - 8) + Math.sin(t * 2.5f + i) * 2);
            int y = b - sand - 2 - (int) (k * (hh - sand) * 0.9f);
            int c = a(0.6f * (1 - k), 0xDFF6FF);
            ctx.fill(x, y, x + 2, y + 1, c);
            ctx.fill(x, y + 2, x + 2, y + 3, c);
            ctx.fill(x - 1, y + 1, x, y + 2, c);
            ctx.fill(x + 2, y + 1, x + 3, y + 2, c);
        }
    }

    // ------------------------------------------------------------------ Moonlit Lake: stars, moon, shimmering lake

    private static void moonlit(GuiGraphicsExtractor ctx, int l, int top, int r, int b, int w, int hh, float t, boolean big) {
        int lake = b - Math.max(4, hh / 4);
        int n = Math.max(8, w / 16);
        for (int i = 0; i < n; i++) {
            float p = 0.5f + 0.5f * (float) Math.sin(t * (1.5f + h(i + 800) * 2) + i * 2.3f);
            int x = l + 2 + (int) (h(i + 801) * (w - 4));
            int y = top + 2 + (int) (h(i + 802) * Math.max(1, lake - top - 4));
            ctx.fill(x, y, x + 1, y + 1, a(0.2f + 0.75f * p, 0xEEF2FF));
        }
        int mr = Math.max(3, Math.min(9, hh / 9));
        int mx = r - mr - 10, my = top + mr + 6;
        disc(ctx, mx, my, mr + 3, 0x14CFD8FF);
        disc(ctx, mx, my, mr + 1, 0x24CFD8FF);
        disc(ctx, mx, my, mr, 0xFFE8ECFF);
        if (mr >= 5) {
            ctx.fill(mx - 2, my - 2, mx, my, 0xFFC8D0EE);
            ctx.fill(mx + 2, my + 1, mx + 3, my + 2, 0xFFC8D0EE);
        }
        ctx.fill(l + 1, lake, r - 1, b - 1, 0x200A1A50);
        for (int y = lake + 1; y < b - 1; y += 2) {
            for (int x = l + 3; x < r - 3; x += 9) {
                float s = (float) Math.sin(x * 0.11 + y * 0.7 + t * 1.6);
                if (s > 0.55f) ctx.fill(x, y, x + 5, y + 1, a(0.15f + 0.2f * s, 0xBFCBFF));
            }
            float k = (float) Math.sin(t * 2.2 + y * 0.9);
            int half = (int) (mr * (0.6f + 0.4f * k));
            ctx.fill(mx - half, y, mx + half + 1, y + 1, a(0.35f + 0.2f * k, 0xE8ECFF));
        }
    }
}
