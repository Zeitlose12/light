package com.skymaxx.tiki.render;

import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.TiledBlitRenderState;

public final class HudFilter {
    private static boolean active;
    private static float alpha;
    private static int untouched;
    private static boolean readding;

    private HudFilter() {
    }

    public static void begin(float hudAlpha) {
        active = true;
        alpha = Math.max(0.0f, Math.min(1.0f, hudAlpha));
        untouched = 0;
        readding = false;
    }

    public static void end() {
        active = false;
        alpha = 1.0f;
        untouched = 0;
        readding = false;
    }

    public static void untouched(Runnable draw) {
        ++untouched;
        try {
            draw.run();
        }
        finally {
            --untouched;
        }
    }

    public static boolean filtering() {
        return active && untouched == 0 && !readding;
    }

    public static boolean fading() {
        return active && alpha > 0.004f;
    }

    public static boolean dropsUncoloured() {
        return alpha < 0.5f;
    }

    public static GuiElementRenderState fade(GuiElementRenderState element) {
        if (alpha <= 0.004f) {
            return null;
        }
        if (element instanceof BlitRenderState) {
            BlitRenderState b = (BlitRenderState)element;
            return new BlitRenderState(b.pipeline(), b.textureSetup(), b.pose(), b.x0(), b.y0(), b.x1(), b.y1(), b.u0(), b.u1(), b.v0(), b.v1(), HudFilter.scale(b.color()), b.scissorArea(), b.bounds());
        }
        if (element instanceof ColoredRectangleRenderState) {
            ColoredRectangleRenderState r = (ColoredRectangleRenderState)element;
            return new ColoredRectangleRenderState(r.pipeline(), r.textureSetup(), r.pose(), r.x0(), r.y0(), r.x1(), r.y1(), HudFilter.scale(r.col1()), HudFilter.scale(r.col2()), r.scissorArea(), r.bounds());
        }
        if (element instanceof TiledBlitRenderState) {
            TiledBlitRenderState t = (TiledBlitRenderState)element;
            return new TiledBlitRenderState(t.pipeline(), t.textureSetup(), t.pose(), t.tileWidth(), t.tileHeight(), t.x0(), t.y0(), t.x1(), t.y1(), t.u0(), t.u1(), t.v0(), t.v1(), HudFilter.scale(t.color()), t.scissorArea(), t.bounds());
        }
        return HudFilter.dropsUncoloured() ? null : element;
    }

    public static int scale(int argb) {
        int a = Math.round((float)(argb >>> 24 & 0xFF) * alpha);
        return a << 24 | argb & 0xFFFFFF;
    }

    public static void readd(Runnable add) {
        readding = true;
        try {
            add.run();
        }
        finally {
            readding = false;
        }
    }

    static {
        alpha = 1.0f;
    }
}

