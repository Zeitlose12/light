package com.skymaxx.tiki.render;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.anim.AnimationDirector;
import com.skymaxx.tiki.anim.Prize;
import com.skymaxx.tiki.anim.Reel;
import com.skymaxx.tiki.audio.AudioDirector;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.render.JungleOverlay;
import com.skymaxx.tiki.render.RenderUtil;
import com.skymaxx.tiki.render.ScreenFx;
import com.skymaxx.tiki.render.StampOverlay;
import com.skymaxx.tiki.render.TikiAssets;
import com.skymaxx.tiki.render.model.ModelProjection;
import com.skymaxx.tiki.render.model.TikiModel;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix3x2fc;

public class TotemHud {
    private static final float SEAM_BLEED = 1.01f;
    private static final int OUTLINE_STEPS = 8;
    private static final int OUTLINE_FINE = 16;
    private static final float SQUASHED = 1.25f;
    private static final float BAND_FROM = 0.6f;
    private static final float BAND_TO = 1.3f;
    private static final float OUTLINE_WEIGHT = 0.02f;
    private static final float OUTLINE_MIN = 2.5f;
    private static final int INK = -16381177;
    private static final float GLOW_SPREAD = 5.2f;
    private static final int GOLD = -13238;
    private static final int WARM = -7512;
    private static final int COOL = -10586200;
    private static final float CAMERA_RATE = 4.2f;
    private static final float PULLBACK_RATE = 1.5f;
    private static final float HOLD_BEFORE_PAN = 0.35f;
    private static final float THIRD_LEAN = 0.05f;
    private static final float FOURTH_LEAN = 0.09f;
    private static final float LEAN_IN_RATE = 10.0f;
    private static final float LEAN_OUT_RATE = 11.0f;
    private static final float LEANED_PUNCH = 0.35f;
    private static final float[] SLAM = new float[]{0.0f, 0.0f, 0.045f, 0.07f};
    private static final float SLAM_IN = 0.035f;
    private static final float SLAM_DECAY = 0.07f;
    private static final int BUBBLE = -4656897;
    private static final int BUBBLE_DEEP = -11355930;
    private static final int GILT = -7270;
    private static final int GILT_DEEP = -1861078;
    private static final float ORBIT_FLAT = 0.26f;
    private static final int WHIRL_TRAILS = 3;
    private static final int WHIRL_MOTES = 18;
    private static final int WHIRL_BUBBLES = 10;
    private static final float PARTICLES_AFTER_SECONDS = 1.1f;
    private static final int SCARE_RED = -1890788;
    private static final int TEXT_RED = -43691;
    private static final int TEXT_DARK_RED = -5636096;
    private static final String[][] SCARE_LINES = new String[][]{{"You're getting too ", "greedy."}, {"Gambling will destroy ", "you."}};
    private static final float SCARE_TEAR_SECONDS = 0.25f;
    private static final int[][] SCARE_COLOURS = new int[][]{{-43691, -5636096}, {-43691, -5636096}};
    private final TikiConfig cfg;
    private final AnimationDirector director;
    private final StampOverlay stamp;
    private final ScreenFx fx;
    private final JungleOverlay jungle;
    private final AudioDirector audio;
    private final List<ModelProjection.Face> faces = new ArrayList<ModelProjection.Face>();
    private final ModelProjection.FacePool pool = new ModelProjection.FacePool();
    private float[] texelPitch = new float[64];
    private final Map<BufferedImage, Map<Long, float[]>> painted = new IdentityHashMap<BufferedImage, Map<Long, float[]>>();
    private static final Comparator<ModelProjection.Face> BACK_TO_FRONT = Comparator.comparingDouble(f -> f.depth);
    private final View viewA = new View();
    private final View viewB = new View();
    private AnimationDirector run;
    private View view;
    private static final float PANEL_ASPECT = 0.7f;
    private static final float PAIR_SPACING = 0.76f;
    private static final float SPLIT_LEAF_REACH = 0.42f;
    private boolean drivenThisFrame;
    private boolean ownedAtHead;
    private boolean drawing;
    private static final float EDGE_FADE_FROM = 0.03f;
    private static final float EDGE_FADE_TO = 0.12f;
    private static final float BARE_BOTTOM_DEGREES = 30.0f;
    private final Matrix3x2f blitMatrix = new Matrix3x2f();

    private static float slam(float since) {
        if (since < 0.0f) {
            return 0.0f;
        }
        if (since < 0.035f) {
            float k = since / 0.035f;
            return k * k * (3.0f - 2.0f * k);
        }
        return (float)Math.exp(-(since - 0.035f) / 0.07f);
    }

    public TotemHud(TikiConfig cfg, AnimationDirector director, StampOverlay stamp, ScreenFx fx, JungleOverlay jungle, AudioDirector audio) {
        this.cfg = cfg;
        this.director = director;
        this.stamp = stamp;
        this.fx = fx;
        this.jungle = jungle;
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
        boolean anything;
        float dt = Math.min(delta.getRealtimeDeltaTicks() / 20.0f, 0.1f);
        this.drivenThisFrame = true;
        this.director.update(dt);
        AnimationDirector second = this.director.second();
        this.fx.update(dt, second == null ? this.director.anticipation() : Math.max(this.director.anticipation(), second.anticipation()));
        this.stamp.update(dt, this.fx);
        AnimationDirector dyed = TotemHud.jackpotTime(this.director) >= 0.0f || second == null ? this.director : second;
        this.jungle.jackpot(TotemHud.jackpotTime(dyed), TotemHud.dyeLight(dyed));
        AnimationDirector crowned = TotemHud.crownImpactTime(this.director) >= 0.0f || second == null ? this.director : second;
        this.jungle.impact(TotemHud.crownImpactTime(crowned), TotemHud.impactLight(crowned));
        this.jungle.update(dt, this.screenTakeover(second), second == null ? this.director.foliageAgitation() : Math.max(this.director.foliageAgitation(), second.foliageAgitation()), second == null ? this.director.foliageHurry() : Math.max(this.director.foliageHurry(), second.foliageHurry()));
        this.look(this.director, this.viewA);
        this.camera(dt);
        if (second != null) {
            this.look(second, this.viewB);
            this.camera(dt);
        } else {
            TotemHud.resetView(this.viewB);
        }
        this.audio.setMuted(TikiGambleClient.gameShouldBeSilent());
        if (!TikiAssets.ready()) {
            TikiAssets.init();
            if (!TikiAssets.ready()) {
                return false;
            }
        }
        if (!this.director.owningScreen()) {
            return false;
        }
        int w = ctx.guiWidth();
        int h = ctx.guiHeight();
        boolean bl = anything = this.director.takeover() > 0.004f || second != null && second.takeover() > 0.004f;
        if (anything) {
            ctx.nextStratum();
            TikiGambleClient.claimBlur(ctx);
            float anticipation = second == null ? this.director.anticipation() : Math.max(this.director.anticipation(), second.anticipation());
            this.fx.drawScrim(ctx, w, h, Math.max(this.director.takeover(), second == null ? 0.0f : second.takeover()), anticipation);
            boolean scare = this.director.scareShown();
            float scareTime = this.director.scareTime();
            if (scare) {
                this.scareDark(ctx, w, h, scareTime);
            }
            this.compose(ctx, second, w, h);
            float split = this.sharedScreen(second);
            float leaves = scare ? 1.0f - RenderUtil.clamp01(scareTime / 0.35f) : 1.0f;
            this.jungle.render(ctx, w, h, this.screenTakeover(second) * leaves, RenderUtil.lerp(1.0f, 0.42f, split));
            this.jackpotRoom(ctx, w, h, this.director);
            this.crownImpactRoom(ctx, w, h, this.director);
            if (second != null) {
                this.jackpotRoom(ctx, w, h, second);
                this.crownImpactRoom(ctx, w, h, second);
            }
            if (scare) {
                this.scareFailingLight(ctx, w, h, scareTime);
                this.scareFlash(ctx, w, h, scareTime);
                this.scareWords(ctx, w, h, scareTime);
                if (scareTime >= 5.55f && scareTime < 5.6200004f) {
                    ctx.fill(0, 0, w, h, RenderUtil.argb(230, 0, 0, 0));
                    this.scaryFace(ctx, w, h, 1.0f);
                }
            }
        }
        if (this.stamp.visible()) {
            ctx.nextStratum();
            this.stamp.render(ctx, w, h);
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
        if (!alreadyDrawn && this.stamp.visible() && TikiAssets.ready()) {
            ctx.nextStratum();
            this.stamp.render(ctx, ctx.guiWidth(), ctx.guiHeight());
        }
        if (!alreadyDrawn && this.director.inAftermath()) {
            float k = 1.0f - this.director.aftermathProgress();
            k *= k;
            int w = ctx.guiWidth();
            int h = ctx.guiHeight();
            ctx.nextStratum();
            ctx.fill(0, 0, w, h, RenderUtil.argb((int)(46.0f * k), 0, 0, 0));
            ctx.fillGradient(0, 0, w, h / 3, RenderUtil.argb((int)(110.0f * k), 70, 0, 0), RenderUtil.argb(0, 70, 0, 0));
            ctx.fillGradient(0, h - h / 3, w, h, RenderUtil.argb(0, 70, 0, 0), RenderUtil.argb((int)(110.0f * k), 70, 0, 0));
            float since = this.director.aftermathSeconds();
            if (since >= 3.2f && since < 3.27f && TikiAssets.ready()) {
                ctx.fill(0, 0, w, h, RenderUtil.argb(215, 0, 0, 0));
                this.scaryFace(ctx, w, h, 0.9f);
            }
        }
    }

    private static float hash(int n) {
        int x = n * -1640531535;
        x ^= x >>> 15;
        x *= -2048144789;
        x ^= x >>> 13;
        return (float)(x & 0xFFFF) / 65535.0f;
    }

    private float scarePulse(float time) {
        float in = RenderUtil.clamp01(time / 0.25f);
        float phase = time * 1.1f % 1.0f;
        float lub = (float)Math.exp(-phase * 9.0f);
        float dub = phase > 0.19f ? 0.7f * (float)Math.exp(-(phase - 0.19f) * 9.0f) : 0.0f;
        float beat = Math.min(1.0f, lub + dub);
        float shock = (float)Math.exp(-time * 4.0f);
        return RenderUtil.clamp01(in * (0.45f + 0.4f * beat) + 0.6f * shock);
    }

    private float scareTextShift(float time) {
        return RenderUtil.easeOutCubic(RenderUtil.clamp01((time - 1.8f + 0.6f) / 1.2f));
    }

    private void scareDark(GuiGraphicsExtractor ctx, int w, int h, float time) {
        float take = this.director.takeover();
        float in = RenderUtil.clamp01(time / 0.5f);
        ctx.fill(0, 0, w, h, RenderUtil.argb((int)(218.0f * in * take), 3, 0, 0));
        float beat = this.scarePulse(time);
        int edge = (int)((60.0f + 80.0f * beat) * in * take);
        ctx.fillGradient(0, 0, w, h / 3, RenderUtil.argb(edge, 130, 0, 0), RenderUtil.argb(0, 130, 0, 0));
        ctx.fillGradient(0, h - h / 3, w, h, RenderUtil.argb(0, 130, 0, 0), RenderUtil.argb(edge, 130, 0, 0));
    }

    private void scareFailingLight(GuiGraphicsExtractor ctx, int w, int h, float time) {
        float take = this.director.takeover();
        float dark = 0.0f;
        if (time > 1.0f && time < 10.8f && TotemHud.hash((int)(time * 20.0f) + 7919) < 0.045f) {
            dark = 0.8f;
        }
        if (dark > 0.01f) {
            ctx.fill(0, 0, w, h, RenderUtil.argb((int)(255.0f * dark * take), 0, 0, 0));
        }
    }

    private void scaryFace(GuiGraphicsExtractor ctx, int w, int h, float alpha) {
        Identifier skin = TikiAssets.scarySkin();
        if (skin == null) {
            return;
        }
        int tw = TikiAssets.widthOf(skin);
        int th = TikiAssets.heightOf(skin);
        float texel = (float)tw / 64.0f;
        int region = Math.max(1, Math.round(8.0f * texel));
        float size = (float)h * 0.95f;
        double seconds = (double)System.nanoTime() / 1.0E9;
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate((float)w * 0.5f + (float)Math.sin(seconds * 71.0) * (float)h * 0.01f, (float)h * 0.5f);
        pose.scale(size / (float)region, size / (float)region);
        int tint = RenderUtil.withAlpha(-25968, alpha);
        ctx.blit(RenderPipelines.GUI_TEXTURED, skin, -region / 2, -region / 2, 8.0f * texel, 8.0f * texel, region, region, tw, th, tint);
        ctx.blit(RenderPipelines.GUI_TEXTURED, skin, -region / 2, -region / 2, 40.0f * texel, 8.0f * texel, region, region, tw, th, tint);
        pose.popMatrix();
    }

    private void scareFlash(GuiGraphicsExtractor ctx, int w, int h, float time) {
        if (time < 0.06f) {
            ctx.fill(0, 0, w, h, RenderUtil.argb(235, 255, 215, 205));
            return;
        }
        float flash = 1.0f - RenderUtil.clamp01((time - 0.06f) / 0.45f);
        if (flash > 0.01f) {
            ctx.fill(0, 0, w, h, RenderUtil.argb((int)(215.0f * flash * flash), 255, 30, 20));
        }
    }

    private void scareWords(GuiGraphicsExtractor ctx, int w, int h, float time) {
        float since;
        int line;
        Font font = Minecraft.getInstance().font;
        float alpha = this.director.takeover();
        if (alpha <= 0.01f) {
            return;
        }
        float tear = 0.0f;
        float outAt = 5.3f;
        if (time >= 5.6f) {
            line = 1;
            since = time - 5.6f;
        } else if (time >= 1.8f && time < outAt + 0.25f) {
            line = 0;
            since = time - 1.8f;
            tear = RenderUtil.clamp01((time - outAt) / 0.25f);
        } else {
            return;
        }
        if (tear > 0.0f) {
            if (TotemHud.hash((int)(time * 40.0f)) < tear) {
                return;
            }
            alpha *= 1.0f - tear * 0.5f;
        }
        int widest = 0;
        for (String[] parts : SCARE_LINES) {
            widest = Math.max(widest, TotemHud.boldWidth(font, parts));
        }
        float scale = Math.min((float)w * 0.9f / (float)Math.max(1, widest), (float)h * 0.105f / 9.0f);
        float centreY = (float)h * 0.745f;
        String[] parts = SCARE_LINES[line];
        int total = 0;
        for (String part : parts) {
            total += part.length();
        }
        int shown = Math.min(total, 1 + (int)(since / 1.1f * (float)total));
        float lineWidth = (float)TotemHud.boldWidth(font, parts) * scale;
        float appear = RenderUtil.clamp01(since / 0.25f) * alpha;
        int band = Math.round(15.0f * scale);
        int mid = Math.round(centreY);
        ctx.fillGradient(0, mid - band, w, mid, RenderUtil.argb(0, 0, 0, 0), RenderUtil.argb((int)(190.0f * appear), 0, 0, 0));
        ctx.fillGradient(0, mid, w, mid + band, RenderUtil.argb((int)(190.0f * appear), 0, 0, 0), RenderUtil.argb(0, 0, 0, 0));
        Identifier glow = TikiAssets.glow();
        int gw = TikiAssets.widthOf(glow);
        int gh = TikiAssets.heightOf(glow);
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate((float)w * 0.5f, centreY);
        pose.scale(lineWidth * 1.5f / (float)gw, 24.0f * scale / (float)gh);
        ctx.blit(RenderPipelines.GUI_TEXTURED, glow, -gw / 2, -gh / 2, 0.0f, 0.0f, gw, gh, gw, gh, RenderUtil.withAlpha(-1890788, 0.32f * appear * (0.75f + 0.25f * this.scarePulse(time))));
        pose.popMatrix();
        double seconds = (double)System.nanoTime() / 1.0E9;
        float jx = (float)Math.sin(seconds * 13.0 + (double)line) * 0.1f * scale + (tear > 0.0f ? (TotemHud.hash((int)(time * 40.0f) + 31) - 0.5f) * 4.0f * scale * tear : 0.0f);
        float jy = (float)Math.cos(seconds * 11.0 + (double)line) * 0.1f * scale;
        pose.pushMatrix();
        pose.translate((float)w * 0.5f - lineWidth * 0.5f + jx, centreY - 4.0f * scale + jy);
        pose.scale(scale, scale);
        int ink = RenderUtil.withAlpha(-16777216, 0.92f * alpha);
        int x = 0;
        int left = shown;
        for (int p = 0; p < parts.length && left > 0; ++p) {
            String typed = parts[p].substring(0, Math.min(parts[p].length(), left));
            left -= typed.length();
            MutableComponent text = Component.literal((String)typed).withStyle(ChatFormatting.BOLD);
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dy = -1; dy <= 1; ++dy) {
                    if (dx == 0 && dy == 0) continue;
                    ctx.text(font, (Component)text, x + dx, dy, ink, false);
                }
            }
            ctx.text(font, (Component)text, x, 0, RenderUtil.withAlpha(SCARE_COLOURS[line][p], alpha), false);
            x += font.width((FormattedText)text);
        }
        pose.popMatrix();
    }

    private static int boldWidth(Font font, String[] parts) {
        int width = 0;
        for (String part : parts) {
            width += font.width((FormattedText)Component.literal((String)part).withStyle(ChatFormatting.BOLD));
        }
        return width;
    }

    private void look(AnimationDirector which, View camera) {
        this.run = which;
        this.view = camera;
    }

    private void compose(GuiGraphicsExtractor ctx, AnimationDirector second, int w, int h) {
        boolean secondLast;
        float panelW = (float)w * 0.5f;
        float panelH = Math.min((float)h, panelW / 0.7f);
        float top = ((float)h - panelH) * 0.5f;
        float offset = Math.min(panelW * 0.5f, panelH * 0.76f * 0.5f);
        boolean bl = secondLast = second == null || !second.leaving() || this.director.leaving();
        if (secondLast) {
            this.drawInBox(ctx, this.director, this.viewA, (float)w * 0.5f - offset - panelW * 0.5f, top, panelW, panelH, w, h);
        }
        if (second != null) {
            this.drawInBox(ctx, second, this.viewB, (float)w * 0.5f + offset - panelW * 0.5f, top, panelW, panelH, w, h);
        }
        if (!secondLast) {
            this.drawInBox(ctx, this.director, this.viewA, (float)w * 0.5f - offset - panelW * 0.5f, top, panelW, panelH, w, h);
        }
    }

    private void drawInBox(GuiGraphicsExtractor ctx, AnimationDirector which, View camera, float halfX, float halfY, float halfW, float halfH, int w, int h) {
        float slide = RenderUtil.easeInOutCubic(which.split());
        this.look(which, camera);
        this.drawTotem(ctx, RenderUtil.lerp(0.0f, halfX, slide), RenderUtil.lerp(0.0f, halfY, slide), RenderUtil.lerp(w, halfW, slide), RenderUtil.lerp(h, halfH, slide), 1.0f);
    }

    private float screenTakeover(AnimationDirector second) {
        return second == null ? this.director.takeover() : Math.max(this.director.takeover(), second.takeover());
    }

    private float sharedScreen(AnimationDirector second) {
        boolean secondGoing;
        if (second == null) {
            return this.director.split();
        }
        boolean firstGoing = !this.director.leaving();
        boolean bl = secondGoing = !second.leaving();
        if (firstGoing && secondGoing) {
            return Math.min(this.director.split(), second.split());
        }
        if (firstGoing) {
            return this.director.split();
        }
        if (secondGoing) {
            return second.split();
        }
        return Math.max(this.director.split(), second.split());
    }

    private static void resetView(View camera) {
        camera.focus = 0.0f;
        camera.pullback = 0.0f;
        camera.crownFade = 0.0f;
        camera.jackpotKicked = false;
        camera.spinZoom = 0.0f;
    }

    private void camera(float dt) {
        boolean wide;
        AnimationDirector.Phase phase = this.run.phase();
        boolean bl = wide = (phase == AnimationDirector.Phase.WIN_OUTRO || phase == AnimationDirector.Phase.RELEASING) && !this.run.scareShown();
        if (phase == AnimationDirector.Phase.IDLE) {
            TotemHud.resetView(this.view);
            return;
        }
        if (!this.view.jackpotKicked && TotemHud.jackpotTime(this.run) >= 0.0f && TotemHud.jackpotTime(this.run) < 1.0f) {
            this.view.jackpotKicked = true;
            this.fx.impact(18.0f);
        }
        int active = this.run.activeIndex();
        float lean = 0.0f;
        if (phase == AnimationDirector.Phase.SPINNING && active >= 2) {
            lean = TotemHud.spinLean(active, this.run.activeReel().progress());
        }
        if (phase != AnimationDirector.Phase.SCARE) {
            float rate = lean >= this.view.spinZoom ? 10.0f : 11.0f;
            this.view.spinZoom += (lean - this.view.spinZoom) * Math.min(1.0f, dt * rate);
        }
        int crown = this.run.prize().crownTier();
        float target = wide ? (float)crown * 0.5f : (phase == AnimationDirector.Phase.REVEAL && this.run.activeReel().won() && active < crown && this.run.phaseTime() > this.cfg.revealPauseSeconds * 0.35f ? (float)(active + 1) : (float)active);
        this.view.focus += (target - this.view.focus) * Math.min(1.0f, dt * 4.2f);
        float pullTarget = wide ? 1.0f : 0.0f;
        this.view.pullback += (pullTarget - this.view.pullback) * Math.min(1.0f, dt * 1.5f);
        float fadeTarget = wide && this.run.crowned() && crown < 3 ? 1.0f : 0.0f;
        this.view.crownFade += (fadeTarget - this.view.crownFade) * Math.min(1.0f, dt * 2.4f);
    }

    private static float spinLean(int active, float progress) {
        float e = TotemHud.smoothstep(0.08f, 0.96f, progress);
        return (active >= 3 ? 0.09f : 0.05f) * (0.25f * e + 0.75f * e * e);
    }

    private static float crownImpactTime(AnimationDirector which) {
        Prize prize = which.prize();
        if (prize != Prize.MASK && prize != Prize.BUBBLE || !which.crowned() || which.scare()) {
            return -1.0f;
        }
        float time = which.crownTime();
        return time > 50.0f ? -1.0f : time;
    }

    private float tierAlpha(int tier) {
        return tier <= this.run.prize().crownTier() ? 1.0f : 1.0f - this.view.crownFade;
    }

    private static float edgeFade(ModelProjection.Face face) {
        return TotemHud.smoothstep(0.03f, 0.12f, face.facing);
    }

    private int light() {
        if (this.run.scare()) {
            return -1890788;
        }
        int tint = TikiAssets.prizeTint(this.run.prize());
        return tint == 0 ? -13238 : tint;
    }

    private void drawTotem(GuiGraphicsExtractor ctx, float x0, float y0, float w, float h, float fade) {
        float shock;
        float takeover = this.run.takeover() * fade;
        if (takeover <= 0.004f) {
            return;
        }
        float pitch = 8.0f;
        float cp = (float)Math.cos(Math.toRadians(pitch));
        float sp = (float)Math.sin(Math.toRadians(pitch));
        float tallest = 0.0f;
        float widest = 0.0f;
        float core = 0.0f;
        for (int i = 0; i < 4; ++i) {
            TikiModel model = TikiAssets.model(i);
            tallest = Math.max(tallest, model.height());
            widest = Math.max(widest, model.radius());
            core = Math.max(core, model.coreHeight());
        }
        if (tallest <= 0.0f || core <= 0.0f) {
            return;
        }
        float step = core * 1.0f;
        int shown = this.run.prize().tiers();
        float oneMask = tallest * cp + 2.0f * widest * sp;
        float wholeStack = (step * (float)(shown - 1) + tallest) * cp + 2.0f * widest * sp;
        float near = h * 0.455f / Math.max(0.001f, oneMask);
        float far = h * 0.9f / Math.max(0.001f, wholeStack);
        float unit = RenderUtil.lerp(near, far, RenderUtil.easeOutCubic(this.view.pullback));
        unit *= 1.0f + this.view.spinZoom;
        Reel landing = this.run.reels()[this.run.activeIndex()];
        float flash = landing.landFlash();
        int landingTier = this.run.activeIndex();
        if (landingTier >= 2 && TotemHud.jackpotTime(this.run) < 0.0f && !this.run.scareShown()) {
            unit *= 1.0f + SLAM[landingTier] * TotemHud.slam(landing.sinceLanded());
        } else {
            float punch = this.run.activeIndex() >= 2 && TotemHud.jackpotTime(this.run) < 0.0f ? 0.133f : 0.38f;
            unit *= 1.0f + punch * flash * (1.0f - flash);
        }
        float scareTime = this.run.scareShown() ? this.run.scareTime() : -1.0f;
        float f = shock = scareTime < 0.0f ? 0.0f : (float)Math.exp(-scareTime * 5.5f);
        if (scareTime >= 0.0f) {
            float whole = 13.4f;
            unit *= 1.0f + 0.32f * shock + 0.13f * RenderUtil.clamp01(scareTime / whole);
        }
        float rise = 1.0f;
        if (this.run.phase() == AnimationDirector.Phase.INTRO) {
            rise = RenderUtil.easeOutCubic(Math.min(1.0f, this.run.phaseTime() / Math.max(0.05f, this.cfg.introSeconds)));
        }
        this.faces.clear();
        this.pool.reset();
        for (int i = 0; i < 4; ++i) {
            Reel reel = this.run.reels()[i];
            TikiModel model = TikiAssets.model(i);
            float lift = ((float)i - 1.5f) * step - model.centreY();
            float capY = model.centreY() + lift - model.coreHeight() * 0.5f;
            float yaw = reel.yaw() + this.run.chainSpin(i);
            ModelProjection.project(model, reel.blanked(), yaw, pitch, unit, lift, i, this.faces, this.pool);
            ModelProjection.projectUnderside(model, yaw, pitch, unit, capY, i, this.faces, this.pool);
        }
        this.faces.sort(BACK_TO_FRONT);
        float cx = x0 + w * 0.5f;
        float cy = y0 + h * 0.5f;
        float alpha = takeover * rise;
        float pan = (this.view.focus - 1.5f) * step * unit * cp;
        float scareX = 0.0f;
        float scareY = 0.0f;
        if (scareTime >= 0.0f) {
            double seconds = (double)System.nanoTime() / 1.0E9;
            float amount = h * (0.05f * shock + 0.0012f);
            scareX = (float)(Math.sin(seconds * 91.0) * 0.6 + Math.sin(seconds * 57.0) * 0.4) * amount;
            scareY = (float)(Math.cos(seconds * 83.0) * 0.6 + Math.sin(seconds * 47.0) * 0.4) * amount - h * 0.14f * this.scareTextShift(scareTime);
        }
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(this.fx.shakeX() + cx + scareX, this.fx.shakeY() + cy + pan + (1.0f - rise) * h * 0.4f + scareY);
        this.drawWinGlow(ctx, step, unit, cp, widest, alpha);
        this.jackpotBehind(ctx, step, unit, cp, widest, alpha);
        this.crownImpactBehind(ctx, step, unit, cp, widest, alpha);
        this.chainWhirl(ctx, step, unit, cp, widest, alpha, false);
        this.drawOutline(ctx, alpha, Math.max(2.5f, tallest * unit * 0.02f));
        for (ModelProjection.Face face : this.faces) {
            this.draw(ctx, face, alpha);
        }
        this.jackpotFront(ctx, step, unit, cp, widest, alpha);
        this.chainWhirl(ctx, step, unit, cp, widest, alpha, true);
        pose.popMatrix();
        this.jackpotShower(ctx, x0, y0, w, h, alpha);
    }

    private int chainLight() {
        return switch (this.run.prize()) {
            case Prize.MASK -> -7270;
            case Prize.AQUAMARINE -> RenderUtil.lerpColor(TotemHud.dyeLight(this.run), -1, 0.35f);
            default -> -4656897;
        };
    }

    private int chainDeep() {
        return switch (this.run.prize()) {
            case Prize.MASK -> -1861078;
            case Prize.AQUAMARINE -> TotemHud.dyeLight(this.run);
            default -> -11355930;
        };
    }

    private float chainFlash(int tier) {
        float turn = this.run.chainTurn(tier);
        if (turn <= 0.0f || turn >= 1.0f) {
            return 0.0f;
        }
        float degrees = AnimationDirector.turnDegrees(turn, AnimationDirector.chainTurns(this.run.prize()));
        float d = (degrees - this.run.chainChangeDegrees()) / 130.0f;
        return (float)Math.exp(-d * d);
    }

    private void chainWhirl(GuiGraphicsExtractor ctx, float step, float unit, float cp, float widest, float alpha, boolean near) {
        Prize prize = this.run.prize();
        int turns = AnimationDirector.chainTurns(prize);
        if (turns == 0 || this.run.crownTime() < 0.0f || this.run.crownTime() > 50.0f) {
            return;
        }
        Identifier mote = TikiAssets.mote();
        Identifier bubble = TikiAssets.bubbleFx();
        Identifier spark = TikiAssets.spark();
        Identifier glow = TikiAssets.glow();
        Identifier ring = TikiAssets.ring();
        if (mote == null || bubble == null || spark == null || glow == null || ring == null) {
            return;
        }
        int light = this.chainLight();
        int deep = this.chainDeep();
        float mask = widest * 2.0f * unit;
        float turnSeconds = AnimationDirector.chainSeconds(prize);
        for (int tier = 0; tier <= prize.crownTier(); ++tier) {
            float since = this.run.chainSince(tier);
            if (since < 0.0f || since > turnSeconds + 1.1f) continue;
            float turn = Math.min(1.0f, since / turnSeconds);
            float after = Math.max(0.0f, since - turnSeconds);
            float speed = AnimationDirector.turnSpeed(turn);
            float flash = this.chainFlash(tier);
            float degrees = AnimationDirector.turnDegrees(turn, turns);
            float y = -((float)tier - 1.5f) * step * unit * cp;
            float base = y + step * unit * cp * 0.5f;
            if (!near) {
                this.whirlLight(ctx, glow, mask, y, degrees, speed, flash, turn, after, alpha, light, deep);
                this.settleRipple(ctx, ring, mask, base, since, turnSeconds, alpha, light);
            } else if (flash > 0.02f) {
                float strength = AnimationDirector.chainChangesSkin(prize) ? 0.4f : 0.22f;
                this.sprite(ctx, glow, 0.0f, y, mask * (0.9f + 0.5f * flash), mask * (0.9f + 0.5f * flash), 0.0f, RenderUtil.withAlpha(RenderUtil.lerpColor(light, -1, 0.5f), strength * flash * alpha));
            }
            this.whirlTrails(ctx, mote, mask, y, degrees, speed, flash, tier, alpha, near, deep);
            this.whirlParticles(ctx, prize, bubble, spark, mask, y, since, turn, turnSeconds, after, tier, alpha, near, light);
        }
    }

    private void whirlLight(GuiGraphicsExtractor ctx, Identifier glow, float mask, float y, float degrees, float speed, float flash, float turn, float after, float alpha, int light, int deep) {
        float settled = turn >= 1.0f ? (float)Math.exp(-after * 3.0f) : 0.0f;
        float strength = 0.22f * RenderUtil.clamp01(turn * 5.0f) * (turn < 1.0f ? 1.0f : 0.0f) + 0.3f * speed + 0.55f * flash + 0.25f * settled;
        if (strength <= 0.01f) {
            return;
        }
        float size = mask * (1.5f + 1.0f * flash + 0.4f * speed);
        int colour = RenderUtil.lerpColor(deep, light, 0.4f + 0.6f * flash);
        this.sprite(ctx, glow, 0.0f, y, size, size, (float)Math.toRadians(degrees) * 0.5f, RenderUtil.withAlpha(colour, Math.min(0.9f, strength) * alpha));
    }

    private void whirlTrails(GuiGraphicsExtractor ctx, Identifier mote, float mask, float y, float degrees, float speed, float flash, int tier, float alpha, boolean near, int deep) {
        float strength = Math.min(1.0f, speed * 1.4f + 0.35f * flash);
        if (strength <= 0.02f) {
            return;
        }
        double head = Math.toRadians(degrees * 1.2f) + (double)tier * 0.7;
        double length = Math.toRadians(35.0f + 120.0f * speed);
        float radius = mask * (0.56f + 0.1f * speed);
        for (int trail = 0; trail < 3; ++trail) {
            double start = head + (double)trail * Math.PI * 2.0 / 3.0;
            float lift = mask * 0.16f * (float)(trail - 1);
            for (int m = 0; m < 18; ++m) {
                float u = (float)m / 17.0f;
                double angle = start - length * (double)u;
                float depth = (float)Math.cos(angle);
                if (depth > 0.0f != near) continue;
                float x = (float)Math.sin(angle) * radius;
                float my = y + lift - depth * radius * 0.26f;
                float size = mask * 0.12f * (1.0f - 0.65f * u) * (0.7f + 0.3f * speed) * (near ? 1.0f : 0.8f);
                float fade = (float)Math.pow(1.0f - u, 1.6f) * strength * (near ? 1.0f : 0.6f);
                int colour = RenderUtil.lerpColor(-1, deep, u);
                this.sprite(ctx, mote, x, my, size, size, 0.0f, RenderUtil.withAlpha(colour, fade * alpha));
            }
        }
    }

    private void whirlParticles(GuiGraphicsExtractor ctx, Prize prize, Identifier bubble, Identifier spark, float mask, float y, float since, float turn, float turnSeconds, float after, int tier, float alpha, boolean near, int light) {
        float carried = RenderUtil.clamp01(since / (turnSeconds * 1.6f));
        double orbit = Math.toRadians(520.0 * (double)AnimationDirector.chainTurns(prize) * (1.0 - Math.cos(Math.PI * (double)carried)) * 0.5) + (double)after * 1.3;
        for (int b = 0; b < 10; ++b) {
            double angle;
            float depth;
            float gone;
            float h1 = TotemHud.hash(b * 17 + tier * 5 + 3);
            float h2 = TotemHud.hash(b * 29 + tier * 11 + 7);
            float h3 = TotemHud.hash(b * 41 + tier * 13 + 1);
            float appear = RenderUtil.easeOutCubic(RenderUtil.clamp01((turn - 0.1f - 0.25f * h1) / 0.2f));
            if (appear <= 0.0f || (gone = RenderUtil.clamp01((after - 0.3f - 0.45f * h2) / 0.35f)) >= 1.0f || (depth = (float)Math.cos(angle = (double)h1 * Math.PI * 2.0 + orbit * (double)(0.85f + 0.3f * h3))) > 0.0f != near) continue;
            float radius = mask * (0.46f + 0.18f * h2 + 0.22f * RenderUtil.easeOutCubic(turn) + 0.3f * RenderUtil.easeOutCubic(RenderUtil.clamp01(after / 1.1f)));
            float x = (float)Math.sin(angle) * radius + (float)Math.sin(since * (5.0f + 3.0f * h3) + (float)b) * mask * 0.012f;
            float rise = mask * ((h3 - 0.5f) * 0.55f + 0.1f * turn + (0.45f + 0.5f * h1) * after * after);
            float by = y - rise - depth * radius * 0.26f;
            float wobble = 1.0f + 0.07f * (float)Math.sin(since * 17.0f + (float)b * 2.1f);
            boolean glint = prize == Prize.MASK || prize == Prize.AQUAMARINE && b % 3 == 0;
            float size = mask * (0.055f + 0.075f * h2) * appear * wobble * (1.0f + 0.35f * gone) * (near ? 1.0f : 0.85f);
            float fade = appear * (1.0f - gone) * (near ? 0.95f : 0.6f);
            float rotation = 0.0f;
            if (glint) {
                float twinkle = 0.55f + 0.45f * (float)Math.sin(since * (9.0f + 5.0f * h3) + (float)b * 1.7f);
                size *= 1.5f * twinkle;
                rotation = since * (2.0f + 2.0f * h2) + (float)b;
            }
            int colour = RenderUtil.lerpColor(-1, light, 0.35f + 0.3f * h1);
            this.sprite(ctx, glint ? spark : bubble, x, by, size, size, rotation, RenderUtil.withAlpha(colour, fade * alpha));
        }
    }

    private void settleRipple(GuiGraphicsExtractor ctx, Identifier ring, float mask, float base, float since, float turnSeconds, float alpha, int light) {
        float settleAt = 0.8f * turnSeconds;
        float k = (since - settleAt) / 0.65f;
        if (k <= 0.0f || k >= 1.0f) {
            return;
        }
        float e = RenderUtil.easeOutCubic(k);
        float width = mask * (0.7f + 1.5f * e);
        float fade = (1.0f - k) * (1.0f - k);
        this.sprite(ctx, ring, 0.0f, base, width, width * 0.26f * 1.3f, 0.0f, RenderUtil.withAlpha(light, 0.65f * fade * alpha));
    }

    private void sprite(GuiGraphicsExtractor ctx, Identifier texture, float x, float y, float width, float height, float rotation, int tint) {
        if (tint >>> 24 < 2 || width <= 0.01f || height <= 0.01f) {
            return;
        }
        int tw = TikiAssets.widthOf(texture);
        int th = TikiAssets.heightOf(texture);
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        if (rotation != 0.0f) {
            pose.rotate(rotation);
        }
        pose.scale(width / (float)tw, height / (float)th);
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, -tw / 2, -th / 2, 0.0f, 0.0f, tw, th, tw, th, tint);
        pose.popMatrix();
    }

    private static int impactLight(AnimationDirector which) {
        return which.prize() == Prize.BUBBLE ? -4656897 : -7270;
    }

    private void crownImpactRoom(GuiGraphicsExtractor ctx, int w, int h, AnimationDirector which) {
        float time = TotemHud.crownImpactTime(which);
        if (time < 0.0f || time > 1.2f) {
            return;
        }
        float wash = RenderUtil.clamp01(time / 0.09f) * (1.0f - TotemHud.smoothstep(0.12f, 0.95f, time));
        if (wash > 0.01f) {
            ctx.fill(0, 0, w, h, RenderUtil.withAlpha(TotemHud.impactLight(which), 0.1f * wash * which.takeover()));
        }
    }

    private void crownImpactBehind(GuiGraphicsExtractor ctx, float step, float unit, float cp, float widest, float alpha) {
        float life;
        float time = TotemHud.crownImpactTime(this.run);
        Identifier glow = TikiAssets.glow();
        Identifier ring = TikiAssets.ring();
        if (time < 0.0f || time > 0.9f || glow == null || ring == null) {
            return;
        }
        int light = TotemHud.impactLight(this.run);
        float mask = widest * 2.0f * unit;
        float crownY = -((float)this.run.prize().crownTier() - 1.5f) * step * unit * cp;
        float burst = 1.0f - RenderUtil.clamp01(time / 0.85f);
        if (burst > 0.01f) {
            float size = mask * (2.4f + 1.6f * RenderUtil.easeOutCubic(RenderUtil.clamp01(time / 0.7f)));
            this.sprite(ctx, glow, 0.0f, crownY, size, size, time * 0.5f, RenderUtil.withAlpha(light, 0.3f * burst * burst * alpha));
        }
        if (time < (life = 0.5f)) {
            float e = RenderUtil.easeOutCubic(time / life);
            float fade = 1.0f - time / life;
            float size = mask * (0.9f + 2.1f * e);
            this.sprite(ctx, ring, 0.0f, crownY, size, size * 0.82f, 0.0f, RenderUtil.withAlpha(light, 0.4f * fade * fade * alpha));
        }
    }

    private static float jackpotTime(AnimationDirector which) {
        if (which.prize() != Prize.AQUAMARINE || !which.crowned()) {
            return -1.0f;
        }
        float time = which.crownTime();
        return time > 50.0f ? -1.0f : time;
    }

    private static int dyeLight(AnimationDirector which) {
        int tint = TikiAssets.prizeTint(which.prize());
        return tint == 0 ? -9440544 : tint;
    }

    private void jackpotRoom(GuiGraphicsExtractor ctx, int w, int h, AnimationDirector which) {
        float time = TotemHud.jackpotTime(which);
        if (time < 0.0f) {
            return;
        }
        float take = which.takeover();
        int dye = TotemHud.dyeLight(which);
        float flash = 1.0f - RenderUtil.clamp01(time / 0.6f);
        if (flash > 0.01f) {
            int colour = RenderUtil.lerpColor(dye, -1, 0.6f * flash);
            ctx.fill(0, 0, w, h, RenderUtil.withAlpha(colour, 0.82f * flash * flash * take));
        }
        float in = RenderUtil.clamp01(time / 0.5f);
        float breathe = 0.72f + 0.28f * (float)Math.sin(time * 3.2f);
        int edge = RenderUtil.withAlpha(dye, 0.36f * in * breathe * take);
        int clear = RenderUtil.withAlpha(dye, 0.0f);
        ctx.fillGradient(0, 0, w, h / 4, edge, clear);
        ctx.fillGradient(0, h - h / 4, w, h, clear, edge);
    }

    private void jackpotBehind(GuiGraphicsExtractor ctx, float step, float unit, float cp, float widest, float alpha) {
        float time = TotemHud.jackpotTime(this.run);
        if (time < 0.0f) {
            return;
        }
        int dye = TotemHud.dyeLight(this.run);
        float mask = widest * 2.0f * unit;
        float crownY = -((float)this.run.prize().crownTier() - 1.5f) * step * unit * cp;
        float in = RenderUtil.easeOutCubic(RenderUtil.clamp01(time / 0.7f));
        Matrix3x2fStack pose = ctx.pose();
        Identifier glow = TikiAssets.glow();
        int gw = TikiAssets.widthOf(glow);
        int gh = TikiAssets.heightOf(glow);
        float rays = mask * (10.5f + 1.2f * (float)Math.sin(time * 1.6f)) * in;
        for (int layer = 0; layer < 2; ++layer) {
            float size = layer == 0 ? rays : rays * 0.7f;
            pose.pushMatrix();
            pose.translate(0.0f, crownY);
            pose.rotate(layer == 0 ? -time * 0.33f : time * 0.21f + 0.26f);
            pose.scale(size / (float)gw, size / (float)gh);
            int colour = layer == 0 ? dye : RenderUtil.lerpColor(dye, -1, 0.5f);
            ctx.blit(RenderPipelines.GUI_TEXTURED, glow, -gw / 2, -gh / 2, 0.0f, 0.0f, gw, gh, gw, gh, RenderUtil.withAlpha(colour, (layer == 0 ? 0.42f : 0.3f) * alpha));
            pose.popMatrix();
        }
        Identifier ring = TikiAssets.ring();
        if (ring != null) {
            int rw = TikiAssets.widthOf(ring);
            int rh = TikiAssets.heightOf(ring);
            int ringColour = RenderUtil.lerpColor(dye, -1, 0.35f);
            for (int k = 0; k < 4; ++k) {
                float strength;
                float reach;
                float life;
                float t;
                if (k < 3) {
                    t = time - (float)k * 0.15f;
                    life = 1.15f;
                    reach = 8.0f;
                    strength = 0.95f;
                } else {
                    if (time < 1.3f) continue;
                    t = (time - 1.3f) % 1.1f;
                    life = 1.1f;
                    reach = 4.8f;
                    strength = 0.45f;
                }
                if (t < 0.0f || t > life) continue;
                float e = RenderUtil.easeOutCubic(t / life);
                float fade = 1.0f - t / life;
                float size = mask * (0.8f + reach * e);
                pose.pushMatrix();
                pose.translate(0.0f, crownY);
                pose.scale(size / (float)rw, size * 0.82f / (float)rh);
                ctx.blit(RenderPipelines.GUI_TEXTURED, ring, -rw / 2, -rh / 2, 0.0f, 0.0f, rw, rh, rw, rh, RenderUtil.withAlpha(ringColour, strength * fade * fade * alpha));
                pose.popMatrix();
            }
        }
        this.orbit(ctx, mask, crownY, time, dye, alpha, false);
    }

    private void jackpotFront(GuiGraphicsExtractor ctx, float step, float unit, float cp, float widest, float alpha) {
        float time = TotemHud.jackpotTime(this.run);
        if (time < 0.0f) {
            return;
        }
        float mask = widest * 2.0f * unit;
        float crownY = -((float)this.run.prize().crownTier() - 1.5f) * step * unit * cp;
        this.orbit(ctx, mask, crownY, time, TotemHud.dyeLight(this.run), alpha, true);
    }

    private void orbit(GuiGraphicsExtractor ctx, float mask, float crownY, float time, int dye, float alpha, boolean near) {
        Identifier spark = TikiAssets.spark();
        if (spark == null) {
            return;
        }
        int sw = TikiAssets.widthOf(spark);
        int sh = TikiAssets.heightOf(spark);
        float in = RenderUtil.easeOutCubic(RenderUtil.clamp01((time - 0.1f) / 0.6f));
        if (in <= 0.0f) {
            return;
        }
        Matrix3x2fStack pose = ctx.pose();
        int count = 16;
        for (int i = 0; i < count; ++i) {
            double angle = (double)i * Math.PI * 2.0 / (double)count + (double)time * 1.05;
            float depth = (float)Math.sin(angle);
            if (depth > 0.0f != near) continue;
            float radius = mask * (0.62f + 0.5f * in) * (1.0f + 0.06f * (float)Math.sin(time * 2.3f + (float)i));
            float x = (float)Math.cos(angle) * radius;
            float y = crownY + depth * radius * 0.34f;
            float twinkle = 0.5f + 0.5f * (float)Math.sin(time * 8.0f + (float)i * 1.9f);
            float size = mask * 0.17f * (0.55f + 0.45f * twinkle) * in;
            pose.pushMatrix();
            pose.translate(x, y);
            pose.rotate(time * 2.0f + (float)i);
            pose.scale(size / (float)sw, size / (float)sh);
            int colour = i % 2 == 0 ? -1 : RenderUtil.lerpColor(dye, -1, 0.25f);
            ctx.blit(RenderPipelines.GUI_TEXTURED, spark, -sw / 2, -sh / 2, 0.0f, 0.0f, sw, sh, sw, sh, RenderUtil.withAlpha(colour, (0.45f + 0.55f * twinkle) * alpha));
            pose.popMatrix();
        }
    }

    private void jackpotShower(GuiGraphicsExtractor ctx, float x0, float y0, float w, float h, float alpha) {
        float time = TotemHud.jackpotTime(this.run);
        Identifier spark = TikiAssets.spark();
        if (time < 0.0f || spark == null) {
            return;
        }
        int sw = TikiAssets.widthOf(spark);
        int sh = TikiAssets.heightOf(spark);
        int dye = TotemHud.dyeLight(this.run);
        float in = RenderUtil.clamp01((time - 0.15f) / 0.5f);
        if (in <= 0.0f) {
            return;
        }
        Matrix3x2fStack pose = ctx.pose();
        for (int i = 0; i < 46; ++i) {
            float speed = h * (0.09f + 0.16f * TotemHud.hash(i * 3 + 101));
            float span = h * 1.15f;
            float travelled = (time * speed + TotemHud.hash(i * 7 + 211) * span) % span;
            float y = y0 + h * 1.05f - travelled;
            float x = x0 + w * (0.04f + 0.92f * TotemHud.hash(i * 11 + 307)) + (float)Math.sin(time * 1.4f + (float)i) * w * 0.018f;
            float twinkle = 0.5f + 0.5f * (float)Math.sin(time * (5.0f + 4.0f * TotemHud.hash(i + 401)) + (float)i);
            float size = h * (0.01f + 0.02f * TotemHud.hash(i * 13 + 503)) * (0.6f + 0.4f * twinkle);
            pose.pushMatrix();
            pose.translate(x, y);
            pose.rotate(time * (1.0f + TotemHud.hash(i + 601)) + (float)i);
            pose.scale(size / (float)sw, size / (float)sh);
            int colour = i % 3 == 0 ? -1 : RenderUtil.lerpColor(dye, -1, 0.2f);
            ctx.blit(RenderPipelines.GUI_TEXTURED, spark, -sw / 2, -sh / 2, 0.0f, 0.0f, sw, sh, sw, sh, RenderUtil.withAlpha(colour, in * (0.35f + 0.65f * twinkle) * alpha));
            pose.popMatrix();
        }
    }

    private void drawOutline(GuiGraphicsExtractor ctx, float alpha, float radius) {
        if (alpha <= 0.004f) {
            return;
        }
        float coarse = 2.0f * radius * (float)Math.sin(0.39269908169872414);
        float fine = 2.0f * radius * (float)Math.sin(0.19634954084936207);
        if (this.texelPitch.length < this.faces.size()) {
            this.texelPitch = new float[this.faces.size() * 2];
        }
        for (int k = 0; k < this.faces.size(); ++k) {
            this.texelPitch[k] = TotemHud.texelPitch(this.faces.get(k));
        }
        Matrix3x2fStack pose = ctx.pose();
        for (int i = 0; i < 16; ++i) {
            boolean between = i % 2 == 1;
            double angle = (double)i * 2.0 * Math.PI / 16.0;
            pose.pushMatrix();
            pose.translate((float)Math.cos(angle) * radius, (float)Math.sin(angle) * radius);
            for (int k = 0; k < this.faces.size(); ++k) {
                float faded;
                ModelProjection.Face face = this.faces.get(k);
                float pitch = this.texelPitch[k];
                if (between && pitch >= coarse * 1.25f || (faded = alpha * this.tierAlpha(face.tier) * TotemHud.edgeFade(face)) <= 0.004f) continue;
                this.blit(ctx, face, RenderUtil.withAlpha(-16381177, faded));
                float band = 1.0f - TotemHud.smoothstep(fine * 0.6f, fine * 1.3f, pitch);
                if (!(band > 0.004f)) continue;
                this.inkBand(ctx, face, fine * 1.1f, faded * band);
            }
            pose.popMatrix();
        }
    }

    private static float texelPitch(ModelProjection.Face face) {
        float det = Math.abs(face.ux * face.vy - face.uy * face.vx);
        float u = (float)Math.hypot(face.ux, face.uy);
        float v = (float)Math.hypot(face.vx, face.vy);
        float acrossU = det / Math.max(1.0E-4f, v) / (float)Math.max(1, face.uw);
        float acrossV = det / Math.max(1.0E-4f, u) / (float)Math.max(1, face.vh);
        return Math.min(acrossU, acrossV);
    }

    private static float smoothstep(float from, float to, float value) {
        float t = RenderUtil.clamp01((value - from) / Math.max(1.0E-4f, to - from));
        return t * t * (3.0f - 2.0f * t);
    }

    private void inkBand(GuiGraphicsExtractor ctx, ModelProjection.Face face, float thickness, float alpha) {
        if (!face.underside && face.ux * face.vy - face.uy * face.vx < 0.0f) {
            return;
        }
        float[] box = this.paintedBox(face);
        if (box == null) {
            return;
        }
        float ux = face.ux * 1.01f;
        float uy = face.uy * 1.01f;
        float vx = face.vx * 1.01f;
        float vy = face.vy * 1.01f;
        float ox = face.ox - (ux - face.ux) * 0.5f - (vx - face.vx) * 0.5f;
        float oy = face.oy - (uy - face.uy) * 0.5f - (vy - face.vy) * 0.5f;
        float bx = ox + ux * box[0] + vx * box[1];
        float by = oy + uy * box[0] + vy * box[1];
        float ax = ux * (box[2] - box[0]);
        float ay = uy * (box[2] - box[0]);
        float cx = vx * (box[3] - box[1]);
        float cy = vy * (box[3] - box[1]);
        float det = ax * cy - ay * cx;
        float lc = (float)Math.hypot(cx, cy);
        float across = Math.abs(det) / Math.max(1.0E-4f, lc);
        if (across < thickness && lc > 1.0E-4f) {
            float nx = -cy / lc;
            float ny = cx / lc;
            if (nx * ax + ny * ay < 0.0f) {
                nx = -nx;
                ny = -ny;
            }
            float grow = thickness - across;
            ax += nx * grow;
            ay += ny * grow;
            bx -= nx * grow * 0.5f;
            by -= ny * grow * 0.5f;
        }
        det = ax * cy - ay * cx;
        float la = (float)Math.hypot(ax, ay);
        float along = Math.abs(det) / Math.max(1.0E-4f, la);
        if (along < thickness && la > 1.0E-4f) {
            float nx = -ay / la;
            float ny = ax / la;
            if (nx * cx + ny * cy < 0.0f) {
                nx = -nx;
                ny = -ny;
            }
            float grow = thickness - along;
            cx += nx * grow;
            cy += ny * grow;
            bx -= nx * grow * 0.5f;
            by -= ny * grow * 0.5f;
        }
        if (Math.abs(det = ax * cy - ay * cx) < 1.0E-4f) {
            return;
        }
        if (det < 0.0f) {
            bx += ax;
            by += ay;
            ax = -ax;
            ay = -ay;
        }
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.mul((Matrix3x2fc)new Matrix3x2f().set(ax, ay, cx, cy, bx, by));
        ctx.fill(0, 0, 1, 1, RenderUtil.withAlpha(-16381177, alpha * box[4]));
        pose.popMatrix();
    }

    private float[] paintedBox(ModelProjection.Face face) {
        long key;
        Map boxes;
        float[] box;
        BufferedImage image = TikiAssets.imageOf(this.skinFor(face));
        if (image == null) {
            image = TikiAssets.model(face.tier).skin();
        }
        if (image == null) {
            return null;
        }
        if (this.painted.size() > 32) {
            this.painted.clear();
        }
        if ((box = (float[])(boxes = this.painted.computeIfAbsent(image, k -> new HashMap())).get(key = (long)(face.u0 & 0xFFFF) << 48 | (long)(face.v0 & 0xFFFF) << 32 | (long)(face.uw & 0xFFFF) << 16 | (long)(face.vh & 0xFFFF))) == null) {
            box = TotemHud.measurePainted(image, face.u0, face.v0, face.uw, face.vh);
            boxes.put(key, box);
        }
        return box.length == 0 ? null : box;
    }

    private static float[] measurePainted(BufferedImage image, int u0, int v0, int uw, int vh) {
        int minU = Integer.MAX_VALUE;
        int minV = Integer.MAX_VALUE;
        int maxU = -1;
        int maxV = -1;
        int most = 0;
        for (int y = Math.max(0, v0); y < Math.min(image.getHeight(), v0 + vh); ++y) {
            for (int x = Math.max(0, u0); x < Math.min(image.getWidth(), u0 + uw); ++x) {
                int a = image.getRGB(x, y) >>> 24 & 0xFF;
                if (a <= 12) continue;
                minU = Math.min(minU, x);
                maxU = Math.max(maxU, x);
                minV = Math.min(minV, y);
                maxV = Math.max(maxV, y);
                most = Math.max(most, a);
            }
        }
        if (maxU < 0 || uw <= 0 || vh <= 0) {
            return new float[0];
        }
        return new float[]{(float)(minU - u0) / (float)uw, (float)(minV - v0) / (float)vh, (float)(maxU + 1 - u0) / (float)uw, (float)(maxV + 1 - v0) / (float)vh, (float)most / 255.0f};
    }

    private void drawWinGlow(GuiGraphicsExtractor ctx, float step, float unit, float cp, float widest, float alpha) {
        int crown = this.run.prize().crownTier();
        if (this.run.scareShown()) {
            this.scareGlow(ctx, step, unit, cp, widest, alpha, crown);
            return;
        }
        Reel top = this.run.reels()[crown];
        if (!top.showsMask()) {
            return;
        }
        float celebration = this.run.prize().celebration();
        float strength = top.landFlash() * alpha * celebration;
        if (strength <= 0.01f) {
            return;
        }
        double seconds = (double)System.currentTimeMillis() / 1000.0;
        float breathe = 0.88f + 0.12f * (float)Math.sin(seconds * 2.1);
        Identifier texture = TikiAssets.glow();
        int tw = TikiAssets.widthOf(texture);
        int th = TikiAssets.heightOf(texture);
        float spread = 5.2f * (0.62f + 0.38f * celebration);
        float size = widest * 2.0f * unit * spread * breathe;
        float topY = -((float)crown - 1.5f) * step * unit * cp;
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(0.0f, topY);
        pose.rotate((float)(seconds * 0.15));
        pose.scale(size / (float)tw, size / (float)th);
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, -tw / 2, -th / 2, 0.0f, 0.0f, tw, th, tw, th, RenderUtil.withAlpha(this.light(), strength * 0.85f));
        pose.popMatrix();
    }

    private void scareGlow(GuiGraphicsExtractor ctx, float step, float unit, float cp, float widest, float alpha, int crown) {
        float strength = alpha * this.scarePulse(this.run.scareTime());
        if (strength <= 0.01f) {
            return;
        }
        Identifier texture = TikiAssets.glow();
        int tw = TikiAssets.widthOf(texture);
        int th = TikiAssets.heightOf(texture);
        float faceY = -((float)crown - 1.5f) * step * unit * cp;
        float faceSize = widest * 2.0f * unit * 5.2f * 1.25f;
        float stackSize = step * 4.0f * unit * cp * 2.2f;
        Matrix3x2fStack pose = ctx.pose();
        for (int i = 0; i < 2; ++i) {
            float size = i == 0 ? stackSize : faceSize;
            pose.pushMatrix();
            pose.translate(0.0f, i == 0 ? 0.0f : faceY);
            pose.scale(size / (float)tw, size / (float)th);
            ctx.blit(RenderPipelines.GUI_TEXTURED, texture, -tw / 2, -th / 2, 0.0f, 0.0f, tw, th, tw, th, RenderUtil.withAlpha(-1890788, strength * (i == 0 ? 0.55f : 0.9f)));
            pose.popMatrix();
        }
    }

    private void draw(GuiGraphicsExtractor ctx, ModelProjection.Face face, float alpha) {
        Identifier plain;
        Identifier changedSkin;
        float turn;
        float pulse;
        Reel reel = this.run.reels()[face.tier];
        float faded = alpha * this.tierAlpha(face.tier) * TotemHud.edgeFade(face);
        if (faded <= 0.004f) {
            return;
        }
        int lit = this.light();
        float bright = face.brightness;
        float warmth = face.tone;
        int colour = RenderUtil.lerpColor(-10586200, -7512, warmth * warmth);
        if (face.interior) {
            colour = RenderUtil.lerpColor(colour, -10586200, 0.5f);
        }
        if (face.front && reel.showsMask()) {
            pulse = 0.6f + 0.4f * (float)Math.sin((double)System.currentTimeMillis() / 300.0);
            bright *= 1.0f + 0.25f * reel.landFlash() * pulse;
            if (face.tier == this.run.prize().crownTier()) {
                colour = RenderUtil.lerpColor(colour, lit, 0.45f * reel.landFlash());
                bright *= 1.0f + 0.22f * reel.landFlash() * pulse;
            } else if (face.tier == this.run.extraTier()) {
                colour = RenderUtil.lerpColor(colour, lit, 0.3f * reel.landFlash());
                bright *= 1.0f + 0.15f * reel.landFlash() * pulse;
            }
        }
        if ((pulse = this.holyPulse(face.tier)) > 0.0f) {
            colour = RenderUtil.lerpColor(colour, lit, Math.min(0.75f, pulse));
            bright *= 1.0f + pulse;
        }
        if ((turn = this.run.chainTurn(face.tier)) > 0.0f && turn < 1.0f) {
            float changeFlash = this.chainFlash(face.tier);
            float sheen = AnimationDirector.turnSpeed(turn);
            float reach = AnimationDirector.chainChangesSkin(this.run.prize()) ? 1.0f : 0.6f;
            colour = RenderUtil.lerpColor(colour, this.chainLight(), Math.min(0.9f, (0.3f * sheen + 0.75f * changeFlash) * reach));
            bright *= 1.0f + (0.25f * sheen + 0.85f * changeFlash) * reach;
        }
        if (this.run.chainChanged(face.tier)) {
            float shimmer = 0.5f + 0.5f * (float)Math.sin(this.run.crownTime() * 3.0f + (float)face.tier * 1.3f);
            float settle = turn >= 1.0f ? 1.0f : RenderUtil.clamp01((turn - 0.8f) / 0.2f);
            colour = RenderUtil.lerpColor(colour, this.chainLight(), (0.1f + 0.1f * shimmer) * settle);
        }
        if (this.run.scareShown()) {
            float time = this.run.scareTime();
            float red = this.scarePulse(time);
            colour = RenderUtil.lerpColor(colour, -1890788, Math.min(0.85f, 0.35f + 0.5f * red));
            bright *= (0.8f + 0.75f * red) * (1.0f - 0.3f * this.scareTextShift(time));
            if (face.tier < this.run.prize().crownTier()) {
                bright *= 1.0f - 0.62f * RenderUtil.clamp01((time - 0.6f) / 1.6f);
            }
        }
        int tint = RenderUtil.withAlpha(RenderUtil.shade(colour, bright), faded);
        float morph = this.run.chainMorph(face.tier);
        Identifier identifier = changedSkin = morph > 0.0f && morph < 1.0f ? TikiAssets.prizeSkin(this.run.prize()) : null;
        if (changedSkin != null) {
            this.blit(ctx, face, this.carvedSkin(face), RenderUtil.withAlpha(tint, Math.min(1.0f, 2.0f * (1.0f - morph))));
            this.blit(ctx, face, changedSkin, RenderUtil.withAlpha(tint, Math.min(1.0f, 2.0f * morph)));
            return;
        }
        float bare = face.underside ? this.bareBottom(face.tier) : 0.0f;
        Identifier identifier2 = plain = bare > 0.0f ? TikiAssets.bareBottomSkin(face.tier) : null;
        if (plain != null) {
            if (bare < 1.0f) {
                this.blit(ctx, face, tint);
            }
            this.blit(ctx, face, plain, RenderUtil.withAlpha(tint, bare));
            return;
        }
        this.blit(ctx, face, tint);
    }

    private float bareBottom(int tier) {
        Reel reel = this.run.reels()[tier];
        if (!reel.blanked() || reel.state() == Reel.State.WAITING) {
            return 0.0f;
        }
        float corner = 90.0f;
        return TotemHud.smoothstep(corner, corner + 30.0f, reel.angle());
    }

    private float holyPulse(int tier) {
        int crown = this.run.prize().crownTier();
        if (tier > crown || !this.run.reels()[crown].showsMask()) {
            return 0.0f;
        }
        float strength = this.run.reels()[crown].landFlash() * this.run.prize().celebration();
        if (strength <= 0.01f) {
            return 0.0f;
        }
        float t = (float)(System.currentTimeMillis() % 1600L) / 1600.0f;
        float head = t * ((float)crown + 2.4f) - 0.7f;
        float distance = Math.abs((float)tier - head);
        if (distance > 0.9f) {
            return 0.0f;
        }
        float band = 1.0f - distance / 0.9f;
        return 0.85f * strength * band * band;
    }

    private Identifier skinFor(ModelProjection.Face face) {
        Identifier changed;
        Identifier scary;
        if (this.run.scare() && face.front && face.tier == this.run.prize().crownTier() && (scary = TikiAssets.scarySkin()) != null) {
            return scary;
        }
        if (this.run.chainChanged(face.tier) && (changed = TikiAssets.prizeSkin(this.run.prize())) != null) {
            return changed;
        }
        return this.carvedSkin(face);
    }

    private Identifier carvedSkin(ModelProjection.Face face) {
        Identifier carved;
        Identifier whole;
        Prize prize = this.run.prize();
        if (face.tier == prize.crownTier() && AnimationDirector.chainChangesSkin(prize) && this.run.reels()[face.tier].showsMask() && (whole = TikiAssets.prizeSkin(prize)) != null) {
            return whole;
        }
        if (face.front && face.tier == prize.crownTier() && (carved = TikiAssets.prizeSkin(prize)) != null) {
            return carved;
        }
        if (face.front && face.tier == this.run.extraTier() && (carved = TikiAssets.prizeSkin(this.run.extra())) != null) {
            return carved;
        }
        return TikiAssets.skin(face.tier);
    }

    private void blit(GuiGraphicsExtractor ctx, ModelProjection.Face face, int tint) {
        this.blit(ctx, face, this.skinFor(face), tint);
    }

    private void blit(GuiGraphicsExtractor ctx, ModelProjection.Face face, Identifier texture, int tint) {
        int texW = TikiAssets.widthOf(texture);
        int texH = TikiAssets.heightOf(texture);
        float ux = face.ux * 1.01f;
        float uy = face.uy * 1.01f;
        float vx = face.vx * 1.01f;
        float vy = face.vy * 1.01f;
        float ox = face.ox - (ux - face.ux) * 0.5f - (vx - face.vx) * 0.5f;
        float oy = face.oy - (uy - face.uy) * 0.5f - (vy - face.vy) * 0.5f;
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        if (face.underside && ux * vy - uy * vx < 0.0f) {
            pose.mul((Matrix3x2fc)this.blitMatrix.set(-ux / (float)face.uw, -uy / (float)face.uw, vx / (float)face.vh, vy / (float)face.vh, ox + ux, oy + uy));
            ctx.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, (float)(face.u0 + face.uw), (float)face.v0, face.uw, face.vh, -face.uw, face.vh, texW, texH, tint);
        } else {
            pose.mul((Matrix3x2fc)this.blitMatrix.set(ux / (float)face.uw, uy / (float)face.uw, vx / (float)face.vh, vy / (float)face.vh, ox, oy));
            ctx.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, (float)face.u0, (float)face.v0, face.uw, face.vh, texW, texH, tint);
        }
        pose.popMatrix();
    }

    private static final class View {
        float focus;
        float pullback;
        float crownFade;
        boolean jackpotKicked;
        float spinZoom;

        private View() {
        }
    }
}

