package com.skymaxx.tiki.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.GameplayCorner;
import com.skymaxx.tiki.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class GameplayView {
    private static final float WIDTH = 0.24f;
    private static final int MARGIN = 6;
    private static final int INK = -267973623;
    private static final int EDGE = -4603216;
    private static GpuTexture texture;
    private static GpuTextureView view;
    private static boolean captureWanted;
    private static boolean broken;

    private GameplayView() {
    }

    public static void draw(GuiGraphicsExtractor ctx, GameplayCorner corner, float amount) {
        if (broken) {
            GameplayView.release();
            return;
        }
        if (amount <= 0.01f) {
            return;
        }
        RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) {
            return;
        }
        try {
            if (texture == null || texture.isClosed() || texture.getWidth(0) != main.width || texture.getHeight(0) != main.height) {
                GameplayView.release();
                GpuDevice device = RenderSystem.getDevice();
                texture = device.createTexture(() -> "SkyMaxx gameplay view", 5, GpuFormat.RGBA8_UNORM, main.width, main.height, 1, 1);
                view = device.createTextureView(texture);
            }
        }
        catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not make the gameplay view; turning it off", t);
            broken = true;
            GameplayView.release();
            return;
        }
        captureWanted = true;
        int sw = ctx.guiWidth();
        int sh = ctx.guiHeight();
        float grow = RenderUtil.easeOutCubic(amount);
        float w = (float)sw * 0.24f * grow;
        float h = w * (float)sh / (float)Math.max(1, sw);
        boolean left = corner != GameplayCorner.TOP_RIGHT;
        boolean top = corner != GameplayCorner.BOTTOM_LEFT;
        int x0 = Math.round(left ? 6.0f : (float)(sw - 6) - w);
        int y0 = Math.round(top ? 6.0f : (float)(sh - 6) - h);
        int x1 = x0 + Math.max(1, Math.round(w));
        int y1 = y0 + Math.max(1, Math.round(h));
        ctx.nextStratum();
        ctx.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 3, -267973623);
        ctx.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, -4603216);
        ctx.fill(x0, y0, x1, y1, -16777216);
        ctx.blit(view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR), x0, y0, x1, y1, 0.0f, 1.0f, 1.0f, 0.0f);
    }

    public static void capture() {
        if (!captureWanted) {
            return;
        }
        captureWanted = false;
        if (broken || texture == null || texture.isClosed()) {
            return;
        }
        try {
            GpuTexture world = Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTexture();
            if (world == null) {
                return;
            }
            int w = Math.min(world.getWidth(0), texture.getWidth(0));
            int h = Math.min(world.getHeight(0), texture.getHeight(0));
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(world, texture, 0, 0, 0, 0, 0, w, h);
        }
        catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not copy the game into the gameplay view; turning it off", t);
            broken = true;
        }
    }

    public static void release() {
        captureWanted = false;
        if (view != null) {
            try {
                view.close();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            view = null;
        }
        if (texture != null) {
            try {
                texture.close();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            texture = null;
        }
    }
}

