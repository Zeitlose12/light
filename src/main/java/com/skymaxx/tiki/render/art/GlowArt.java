package com.skymaxx.tiki.render.art;

import java.awt.image.BufferedImage;

public final class GlowArt {
    public static final int SIZE = 256;
    private static final int RAYS = 12;

    private GlowArt() {
    }

    public static BufferedImage halo() {
        BufferedImage img = new BufferedImage(256, 256, 2);
        float centre = 128.0f;
        for (int y = 0; y < 256; ++y) {
            for (int x = 0; x < 256; ++x) {
                float dx = ((float)x + 0.5f - centre) / centre;
                float dy = ((float)y + 0.5f - centre) / centre;
                float r = (float)Math.sqrt(dx * dx + dy * dy);
                if (r >= 1.0f) continue;
                float core = GlowArt.falloff(r, 0.34f) * 0.85f;
                float skirt = GlowArt.falloff(r, 1.0f) * 0.42f;
                float angle = (float)Math.atan2(dy, dx);
                float spokes = (float)Math.cos(angle * 12.0f);
                float rayMask = GlowArt.clamp01((r - 0.22f) * 2.2f) * GlowArt.clamp01(1.0f - r * 1.05f);
                float rays = Math.max(0.0f, spokes) * rayMask * 0.38f;
                float alpha = GlowArt.clamp01(core + skirt + rays);
                if (alpha <= 0.002f) continue;
                int warm = (int)(255.0f - 26.0f * GlowArt.clamp01(r * 1.4f));
                img.setRGB(x, y, (int)(alpha * 255.0f) << 24 | 0xFF0000 | 0xFF00 | warm);
            }
        }
        return img;
    }

    public static BufferedImage bubble() {
        int s = 64;
        BufferedImage img = new BufferedImage(s, s, 2);
        float c = (float)s * 0.5f;
        for (int y = 0; y < s; ++y) {
            for (int x = 0; x < s; ++x) {
                float dx = ((float)x + 0.5f - c) / c;
                float dy = ((float)y + 0.5f - c) / c;
                float r = (float)Math.sqrt(dx * dx + dy * dy);
                if (r >= 1.0f) continue;
                float rim = (float)Math.exp(-Math.pow((r - 0.86f) / 0.07f, 2.0));
                float skin = 0.1f + 0.22f * r * r;
                float facing = (-dx - dy) / (float)Math.sqrt(2.0);
                rim *= 0.78f + 0.22f * facing;
                float crescentR = (float)Math.exp(-Math.pow((r - 0.62f) / 0.09f, 2.0));
                float crescentA = GlowArt.clamp01((float)Math.cos(Math.atan2(dy, dx) + (double)2.36f) * 1.6f - 0.6f);
                float crescent = crescentR * crescentA * 0.95f;
                float pdx = dx + 0.18f;
                float pdy = dy + 0.5f;
                float pin = GlowArt.clamp01(1.0f - (float)Math.sqrt(pdx * pdx + pdy * pdy) / 0.11f);
                float edge = GlowArt.clamp01((1.0f - r) / 0.04f);
                float alpha = GlowArt.clamp01(Math.max(skin, rim) + crescent + pin * pin) * edge;
                if (alpha <= 0.004f) continue;
                img.setRGB(x, y, (int)(alpha * 255.0f) << 24 | 0xFFFFFF);
            }
        }
        return img;
    }

    public static BufferedImage mote() {
        int s = 32;
        BufferedImage img = new BufferedImage(s, s, 2);
        float c = (float)s * 0.5f;
        for (int y = 0; y < s; ++y) {
            for (int x = 0; x < s; ++x) {
                float dx = ((float)x + 0.5f - c) / c;
                float dy = ((float)y + 0.5f - c) / c;
                float r = (float)Math.sqrt(dx * dx + dy * dy);
                float alpha = GlowArt.falloff(r, 1.0f);
                if ((alpha = GlowArt.clamp01(alpha * 1.1f + (r < 0.3f ? 0.25f * (1.0f - r / 0.3f) : 0.0f))) <= 0.004f) continue;
                img.setRGB(x, y, (int)(alpha * 255.0f) << 24 | 0xFFFFFF);
            }
        }
        return img;
    }

    private static float falloff(float r, float reach) {
        float t = GlowArt.clamp01(1.0f - r / reach);
        return t * t;
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }
}

