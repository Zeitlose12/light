package com.skymaxx.tiki.render.art;

import java.awt.image.BufferedImage;

public final class LeafArt {
    public static final int VARIANTS = 6;
    public static final int SIZE = 160;
    private static final int SAMPLES = 3;
    private static final int TONES = 4;
    private static final int[] PALETTE = new int[]{1455386, 2314536, 3439160, 5022282};
    private static final int VEIN = 6536280;
    private static final int INK = 529931;
    private static final int INK_RADIUS = 2;

    private LeafArt() {
    }

    public static BufferedImage leaf(int variant) {
        int v = Math.floorMod(variant, 6);
        BufferedImage img = new BufferedImage(160, 160, 2);
        for (int py = 0; py < 160; ++py) {
            for (int px = 0; px < 160; ++px) {
                float coverage = 0.0f;
                for (int sy = 0; sy < 3; ++sy) {
                    for (int sx = 0; sx < 3; ++sx) {
                        float fx = ((float)px + ((float)sx + 0.5f) / 3.0f) / 160.0f;
                        float fy = ((float)py + ((float)sy + 0.5f) / 3.0f) / 160.0f * 2.0f - 1.0f;
                        if (!LeafArt.inside(v, fx, fy)) continue;
                        coverage += 1.0f;
                    }
                }
                if ((coverage /= 9.0f) <= 0.0f) continue;
                float fx = ((float)px + 0.5f) / 160.0f;
                float fy = ((float)py + 0.5f) / 160.0f * 2.0f - 1.0f;
                img.setRGB(px, py, LeafArt.clamp255((int)(coverage * 255.0f)) << 24 | LeafArt.shade(v, fx, fy));
            }
        }
        return LeafArt.ink(img);
    }

    private static BufferedImage ink(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, 2);
        int r2 = 4;
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                int argb = src.getRGB(x, y);
                int a = argb >>> 24;
                if (a == 0) continue;
                boolean edge = false;
                block2: for (int dy = -2; dy <= 2 && !edge; ++dy) {
                    for (int dx = -2; dx <= 2; ++dx) {
                        if (dx * dx + dy * dy > r2) continue;
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < w && ny < h && src.getRGB(nx, ny) >>> 24 >= 48) continue;
                        edge = true;
                        continue block2;
                    }
                }
                out.setRGB(x, y, a << 24 | (edge ? 529931 : argb & 0xFFFFFF));
            }
        }
        return out;
    }

    private static boolean inside(int variant, float x, float y) {
        float stalk;
        if (x <= 0.0f || x >= 1.0f) {
            return false;
        }
        float ay = Math.abs(y);
        float f = stalk = variant == 2 ? 0.03f : 0.024f;
        if (x < LeafArt.stemLength(variant) && ay < stalk * (1.0f - x * 0.4f)) {
            return true;
        }
        return switch (variant) {
            case 0 -> {
                float w = LeafArt.width(x, 0.45f, 0.85f) * 0.62f;
                w *= 1.0f + 0.05f * (float)Math.sin(81.68140899333463 * (double)x);
                if (ay < w) {
                    yield true;
                }
                yield false;
            }
            case 1 -> {
                float w = LeafArt.width(x, 0.4f, 0.7f) * 0.7f;
                if (ay >= w) {
                    yield false;
                }
                float slit = x * 7.0f - ay * 1.35f;
                float phase = slit - (float)Math.floor(slit);
                if (!(phase > 0.72f) || !(ay > w * 0.3f)) {
                    yield true;
                }
                yield false;
            }
            case 2 -> {
                float w = LeafArt.width(x, 0.3f, 0.55f) * 0.78f;
                if (ay >= w) {
                    yield false;
                }
                if (ay < 0.035f) {
                    yield true;
                }
                float leaflet = x * 6.0f - ay * 1.6f;
                float phase = leaflet - (float)Math.floor(leaflet);
                if (phase < 0.74f) {
                    yield true;
                }
                yield false;
            }
            case 3 -> {
                float w = LeafArt.width(x, 0.7f, 1.25f) * 0.4f;
                if (ay < w) {
                    yield true;
                }
                yield false;
            }
            case 4 -> {
                float w = LeafArt.width(x, 0.28f, 0.62f) * 0.82f;
                if (ay >= w) {
                    yield false;
                }
                if (!(x < 0.2f) || !(ay < (0.2f - x) * 0.9f)) {
                    yield true;
                }
                yield false;
            }
            default -> {
                float w = LeafArt.width(x, 0.55f, 0.9f) * 0.58f;
                if (ay >= w) {
                    yield false;
                }
                if (ay < 0.03f) {
                    yield true;
                }
                float finger = x * 3.0f - ay * 0.55f;
                float phase = finger - (float)Math.floor(finger);
                if (phase < 0.8f) {
                    yield true;
                }
                yield false;
            }
        };
    }

    private static float width(float x, float a, float b) {
        return (float)(Math.pow(x, a) * Math.pow(1.0 - (double)x, b)) * 2.6f;
    }

    private static float stemLength(int variant) {
        return variant == 3 ? 0.16f : 0.22f;
    }

    private static int shade(int variant, float x, float y) {
        boolean side;
        float ay = Math.abs(y);
        boolean rib = ay < 0.028f * (1.0f - x * 0.5f);
        float vein = x * (variant == 2 ? 6.0f : 8.0f) - ay * 2.1f;
        boolean bl = side = !rib && ay > 0.04f && Math.abs(vein - (float)Math.round(vein)) < 0.055f;
        if (rib || side) {
            return 6536280;
        }
        float lift = (1.0f - ay * 0.9f) * (0.35f + x * 0.65f);
        int tone = (int)(LeafArt.clamp01(lift * 1.35f) * 4.0f);
        if (tone >= 4) {
            tone = 3;
        }
        return PALETTE[tone];
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}

