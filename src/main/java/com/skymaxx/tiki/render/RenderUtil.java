package com.skymaxx.tiki.render;

public final class RenderUtil {
    private RenderUtil() {
    }

    public static int argb(int a, int r, int g, int b) {
        return RenderUtil.clamp255(a) << 24 | RenderUtil.clamp255(r) << 16 | RenderUtil.clamp255(g) << 8 | RenderUtil.clamp255(b);
    }

    public static int withAlpha(int argb, float mul) {
        int a = (int)((float)(argb >>> 24 & 0xFF) * mul);
        return RenderUtil.clamp255(a) << 24 | argb & 0xFFFFFF;
    }

    public static int shade(int argb, float mul) {
        int a = argb >>> 24 & 0xFF;
        return RenderUtil.argb(a, (int)((float)(argb >> 16 & 0xFF) * mul), (int)((float)(argb >> 8 & 0xFF) * mul), (int)((float)(argb & 0xFF) * mul));
    }

    public static int lerpColor(int a, int b, float t) {
        t = RenderUtil.clamp01(t);
        return RenderUtil.argb((int)((float)(a >>> 24 & 0xFF) + (float)((b >>> 24 & 0xFF) - (a >>> 24 & 0xFF)) * t), (int)((float)(a >> 16 & 0xFF) + (float)((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t), (int)((float)(a >> 8 & 0xFF) + (float)((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t), (int)((float)(a & 0xFF) + (float)((b & 0xFF) - (a & 0xFF)) * t));
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    public static float easeOutCubic(float t) {
        float u = 1.0f - RenderUtil.clamp01(t);
        return 1.0f - u * u * u;
    }

    public static float easeInOutCubic(float t) {
        return (t = RenderUtil.clamp01(t)) < 0.5f ? 4.0f * t * t * t : 1.0f - (float)Math.pow(-2.0f * t + 2.0f, 3.0) / 2.0f;
    }

    public static float easeInCubic(float t) {
        t = RenderUtil.clamp01(t);
        return t * t * t;
    }

    public static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}

