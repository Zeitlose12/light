package com.skymaxx.tiki.render.boa;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.audio.AudioDirector;
import com.skymaxx.tiki.boa.BoaDirector;
import com.skymaxx.tiki.config.GameplayCorner;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.render.GameplayView;
import com.skymaxx.tiki.render.boa.BoaArtSet;
import com.skymaxx.tiki.render.boa.BoaAssets;
import com.skymaxx.tiki.render.boa.BoaDrawList;
import com.skymaxx.tiki.render.boa.BoaScene;
import com.skymaxx.tiki.render.boa.BoaStamp;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix3x2fc;

public class BoaHud {
    private final TikiConfig cfg;
    private final BoaDirector director;
    private final BoaStamp stamp;
    private final AudioDirector audio;
    private BoaScene scene;
    private BoaScene secondScene;
    private final BoaDrawList list = new BoaDrawList();
    private static final float HALF_ASPECT = 1.25f;
    private final BoaDrawList stampList = new BoaDrawList();
    private final Matrix3x2f matrix = new Matrix3x2f();
    private boolean drivenThisFrame;
    private boolean ownedAtHead;
    private boolean drawing;

    public BoaHud(TikiConfig cfg, BoaDirector director, BoaStamp stamp, AudioDirector audio) {
        this.cfg = cfg;
        this.director = director;
        this.stamp = stamp;
        this.audio = audio;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean renderBeneathHud(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        if (this.drawing) {
            return this.director.owningScreen();
        }
        this.drawing = true;
        try {
            boolean owned;
            this.ownedAtHead = owned = this.draw(ctx, delta);
            boolean bl = owned;
            return bl;
        }
        finally {
            this.drawing = false;
        }
    }

    private boolean draw(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        float dt = Math.min(delta.getRealtimeDeltaTicks() / 20.0f, 0.1f);
        this.drivenThisFrame = true;
        this.director.update(dt);
        this.stamp.update(dt);
        this.audio.setMuted(TikiGambleClient.gameShouldBeSilent());
        if (!BoaAssets.ready()) {
            BoaAssets.init();
            if (!BoaAssets.ready()) {
                return false;
            }
        }
        if (!this.director.owningScreen()) {
            GameplayView.release();
            return false;
        }
        BoaDirector second = this.director.second();
        float shown = Math.max(this.director.takeover(), second != null ? second.takeover() : 0.0f);
        if (shown > 0.004f) {
            boolean pairOnScreen;
            if (this.scene == null) {
                this.scene = new BoaScene(BoaAssets.art());
            }
            ctx.nextStratum();
            TikiGambleClient.claimBlur(ctx);
            boolean bl = pairOnScreen = second != null && second.takeover() > 0.004f;
            if (!pairOnScreen && this.director.split() <= 0.001f) {
                this.scene.build(this.director, ctx.guiWidth(), ctx.guiHeight(), this.cfg.boaScrimStrength, this.list);
                this.replay(ctx, this.list);
            } else {
                this.drawPair(ctx, second);
            }
            if (this.cfg.boaGameplayView) {
                GameplayView.draw(ctx, GameplayCorner.byName(this.cfg.boaGameplayCorner), shown);
            } else {
                GameplayView.release();
            }
        }
        if (this.stamp.visible() && BoaAssets.ready()) {
            ctx.nextStratum();
            this.stamp.build(this.stampList, BoaAssets.art(), ctx.guiWidth(), ctx.guiHeight());
            this.replay(ctx, this.stampList);
        }
        return true;
    }

    public void renderAboveHud(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        boolean alreadyDrawn = this.ownedAtHead;
        this.ownedAtHead = false;
        if (!this.drivenThisFrame) {
            alreadyDrawn = this.renderBeneathHud(ctx, delta);
        }
        this.drivenThisFrame = false;
        if (!alreadyDrawn && this.stamp.visible() && BoaAssets.ready()) {
            ctx.nextStratum();
            this.stamp.build(this.stampList, BoaAssets.art(), ctx.guiWidth(), ctx.guiHeight());
            this.replay(ctx, this.stampList);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void drawPair(GuiGraphicsExtractor ctx, BoaDirector second) {
        BoaDirector[] order;
        BoaDirector[] boaDirectorArray;
        if (this.secondScene == null) {
            this.secondScene = new BoaScene(BoaAssets.art());
        }
        int w = ctx.guiWidth();
        int h = ctx.guiHeight();
        float firstSplit = BoaHud.smooth(this.director.split());
        float secondSplit = second != null ? BoaHud.smooth(second.split()) : 0.0f;
        float pair = Math.min(1.0f, 2.5f * Math.max(firstSplit, secondSplit));
        float take = Math.max(this.director.takeover(), second != null ? second.takeover() : 0.0f);
        float ground = 0.93f * this.cfg.boaScrimStrength + (1.0f - 0.93f * this.cfg.boaScrimStrength) * pair;
        int alpha = (int)(Math.max(0.0f, Math.min(1.0f, ground * take)) * 255.0f);
        int shade = (int)(12.0f * (1.0f - pair));
        ctx.fill(0, 0, w, h, alpha << 24 | shade / 2 << 16 | shade << 8 | shade);
        if (second != null && BoaHud.playing(this.director) && !BoaHud.playing(second)) {
            BoaDirector[] boaDirectorArray2 = new BoaDirector[2];
            boaDirectorArray2[0] = second;
            boaDirectorArray = boaDirectorArray2;
            boaDirectorArray2[1] = this.director;
        } else {
            BoaDirector[] boaDirectorArray3 = new BoaDirector[2];
            boaDirectorArray3[0] = this.director;
            boaDirectorArray = boaDirectorArray3;
            boaDirectorArray3[1] = second;
        }
        for (BoaDirector run : order = boaDirectorArray) {
            if (run == null || run.takeover() <= 0.004f) continue;
            float e = run == this.director ? firstSplit : secondSplit;
            float halfW = (float)w * 0.5f;
            float halfH = Math.min((float)h, halfW / 1.25f);
            float x = BoaHud.lerp(0.0f, run.isSecond() ? halfW : 0.0f, e);
            float y = BoaHud.lerp(0.0f, ((float)h - halfH) * 0.5f, e);
            float vw = BoaHud.lerp(w, halfW, e);
            float vh = BoaHud.lerp(h, halfH, e);
            float fade = run.isSecond() && BoaHud.playing(this.director) ? BoaHud.secondFade(firstSplit) : 1.0f;
            BoaScene composer = run == this.director ? this.scene : this.secondScene;
            composer.build(run, vw, vh, 0.0f, this.list, e);
            if (fade < 0.999f) {
                this.list.alpha(1.0f);
                BoaHud.fadeList(this.list, fade);
            }
            int x0 = Math.round(x);
            int y0 = Math.round(y);
            ctx.nextStratum();
            ctx.enableScissor(x0, y0, Math.round(x + vw), Math.round(y + vh));
            Matrix3x2fStack pose = ctx.pose();
            pose.pushMatrix();
            pose.translate(x, y);
            try {
                this.replay(ctx, this.list);
            }
            finally {
                pose.popMatrix();
                ctx.disableScissor();
            }
        }
    }

    private static void fadeList(BoaDrawList frame, float mul) {
        for (int i = 0; i < frame.size(); ++i) {
            BoaDrawList.Op op = frame.get(i);
            int a = (int)((float)(op.argb >>> 24 & 0xFF) * Math.max(0.0f, Math.min(1.0f, mul)));
            op.argb = a << 24 | op.argb & 0xFFFFFF;
        }
    }

    private static boolean playing(BoaDirector run) {
        return run.phase() != BoaDirector.Phase.IDLE && run.phase() != BoaDirector.Phase.RELEASING;
    }

    public static float secondFade(float firstSplit) {
        return BoaHud.smooth((firstSplit - 0.45f) / 0.55f);
    }

    private static float smooth(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return t * t * (3.0f - 2.0f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private void replay(GuiGraphicsExtractor ctx, BoaDrawList frame) {
        BoaArtSet art = BoaAssets.art();
        Font font = Minecraft.getInstance().font;
        Matrix3x2fStack pose = ctx.pose();
        Matrix3x2f shared = null;
        block7: for (int i = 0; i < frame.size(); ++i) {
            BoaDrawList.Op op = frame.get(i);
            switch (op.kind) {
                case QUAD: {
                    Identifier id = BoaAssets.id(op.tex);
                    if (id == null) continue block7;
                    this.matrix.set(op.m00, op.m01, op.m10, op.m11, op.m20, op.m21);
                    pose.pushMatrix();
                    pose.mul((Matrix3x2fc)this.matrix);
                    ctx.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, (float)op.u0, (float)op.v0, op.uw, op.vh, art.width(op.tex), art.height(op.tex), op.argb);
                    pose.popMatrix();
                    continue block7;
                }
                case FILL: {
                    ctx.fill(Math.round(op.x0), Math.round(op.y0), Math.round(op.x1), Math.round(op.y1), op.argb);
                    continue block7;
                }
                case TEX_POLY: {
                    Identifier id = BoaAssets.id(op.tex);
                    if (id == null) continue block7;
                    if (shared == null) {
                        shared = new Matrix3x2f((Matrix3x2fc)pose);
                    }
                    AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
                    ctx.guiRenderState.addGuiElement((GuiElementRenderState)TexturedShape.of((Matrix3x2fc)shared, op, art.width(op.tex), art.height(op.tex), texture, ctx.scissorStack.peek()));
                    continue block7;
                }
                case POLY: {
                    if (shared == null) {
                        shared = new Matrix3x2f((Matrix3x2fc)pose);
                    }
                    ctx.guiRenderState.addGuiElement((GuiElementRenderState)FlatShape.of((Matrix3x2fc)shared, op, ctx.scissorStack.peek()));
                    continue block7;
                }
                case TEXT: {
                    pose.pushMatrix();
                    pose.translate(op.x0, op.y0);
                    pose.scale(op.scale, op.scale);
                    MutableComponent text = Component.literal((String)op.text);
                    int x = op.centered ? -font.width((FormattedText)text) / 2 : 0;
                    ctx.text(font, (Component)text, x, 0, op.argb, false);
                    pose.popMatrix();
                }
            }
        }
    }

    private record TexturedShape(Matrix3x2fc pose, float[] xs, float[] ys, float[] us, float[] vs, int argb, GpuTextureView view, GpuSampler sampler, ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState
    {
        static TexturedShape of(Matrix3x2fc pose, BoaDrawList.Op op, int texW, int texH, AbstractTexture texture, ScreenRectangle scissor) {
            float[] xs = (float[])op.px.clone();
            float[] ys = (float[])op.py.clone();
            float[] us = new float[4];
            float[] vs = new float[4];
            float det = op.m00 * op.m11 - op.m01 * op.m10;
            float inv = Math.abs(det) < 1.0E-9f ? 0.0f : 1.0f / det;
            for (int i = 0; i < 4; ++i) {
                float px = xs[i] - op.m20;
                float py = ys[i] - op.m21;
                float a = (px * op.m11 - py * op.m10) * inv;
                float b = (op.m00 * py - op.m01 * px) * inv;
                a = Math.max(0.0f, Math.min((float)op.uw, a));
                b = Math.max(0.0f, Math.min((float)op.vh, b));
                us[i] = ((float)op.u0 + a) / (float)texW;
                vs[i] = ((float)op.v0 + b) / (float)texH;
            }
            float minX = Math.min(Math.min(xs[0], xs[1]), Math.min(xs[2], xs[3]));
            float maxX = Math.max(Math.max(xs[0], xs[1]), Math.max(xs[2], xs[3]));
            float minY = Math.min(Math.min(ys[0], ys[1]), Math.min(ys[2], ys[3]));
            float maxY = Math.max(Math.max(ys[0], ys[1]), Math.max(ys[2], ys[3]));
            int x0 = (int)Math.floor(minX);
            int y0 = (int)Math.floor(minY);
            ScreenRectangle box = new ScreenRectangle(x0, y0, Math.max(1, (int)Math.ceil(maxX) - x0), Math.max(1, (int)Math.ceil(maxY) - y0)).transformMaxBounds(pose);
            return new TexturedShape(pose, xs, ys, us, vs, op.argb, texture.getTextureView(), texture.getSampler(), scissor, scissor != null ? box.intersection(scissor) : box);
        }

        public void buildVertices(VertexConsumer consumer) {
            for (int i = 0; i < 4; ++i) {
                consumer.addVertexWith2DPose(this.pose, this.xs[i], this.ys[i]).setUv(this.us[i], this.vs[i]).setColor(this.argb);
            }
        }

        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }

        public TextureSetup textureSetup() {
            return TextureSetup.singleTexture((GpuTextureView)this.view, (GpuSampler)this.sampler);
        }
    }

    private record FlatShape(Matrix3x2fc pose, float[] xs, float[] ys, int argb, ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState
    {
        static FlatShape of(Matrix3x2fc pose, BoaDrawList.Op op, ScreenRectangle scissor) {
            float[] xs = (float[])op.px.clone();
            float[] ys = (float[])op.py.clone();
            float minX = Math.min(Math.min(xs[0], xs[1]), Math.min(xs[2], xs[3]));
            float maxX = Math.max(Math.max(xs[0], xs[1]), Math.max(xs[2], xs[3]));
            float minY = Math.min(Math.min(ys[0], ys[1]), Math.min(ys[2], ys[3]));
            float maxY = Math.max(Math.max(ys[0], ys[1]), Math.max(ys[2], ys[3]));
            int x0 = (int)Math.floor(minX);
            int y0 = (int)Math.floor(minY);
            ScreenRectangle box = new ScreenRectangle(x0, y0, Math.max(1, (int)Math.ceil(maxX) - x0), Math.max(1, (int)Math.ceil(maxY) - y0)).transformMaxBounds(pose);
            return new FlatShape(pose, xs, ys, op.argb, scissor, scissor != null ? box.intersection(scissor) : box);
        }

        public void buildVertices(VertexConsumer consumer) {
            for (int i = 0; i < 4; ++i) {
                consumer.addVertexWith2DPose(this.pose, this.xs[i], this.ys[i]).setColor(this.argb);
            }
        }

        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }
}

