package com.skymaxx.tiki.render.boa;

import com.skymaxx.tiki.boa.BoaDirector;
import com.skymaxx.tiki.boa.BoaPrize;
import com.skymaxx.tiki.boa.BoaRun;
import com.skymaxx.tiki.render.boa.BoaArtSet;
import com.skymaxx.tiki.render.boa.BoaBackdrop;
import com.skymaxx.tiki.render.boa.BoaDrawList;
import com.skymaxx.tiki.render.boa.BoaRifle;
import com.skymaxx.tiki.render.boa.BoaTex;
import com.skymaxx.tiki.render.model.ModelProjection;
import com.skymaxx.tiki.render.model.TikiModel;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BoaScene {
    private static final float PITCH = -7.0f;
    private static final float HORIZON = 8.5f;
    private static final float UNIT = 0.19f;
    private static final float[] ZOOM = new float[]{0.6f, 0.72f, 0.88f, 1.1f, 1.5f};
    private static final int FOURTH = 3;
    private static final int HEAD = 4;
    private static final float SPACING = 0.64f;
    private static final float SPEED = 0.4f;
    private static final float AMPLITUDE = 0.3f;
    private static final float WAVE = 2.0f;
    private static final float BOB = 0.06f;
    private static final float RIPPLE_RATE = 4.0f;
    private static final float RIDE = 0.13f;
    private static final float REAR_LIFT = 0.055f;
    private static final float WAKE_ALONG = 0.95f;
    private static final float WAKE_ACROSS = 1.04f;
    private static final float SURFACE_SQUASH = 0.32f;
    private static final int DODGES = 5;
    private static final int BOLT = 0;
    private static final int DUCK = 1;
    private static final int WHIP = 2;
    private static final int REAR_BACK = 3;
    private static final int COIL = 4;
    private static final float[] DODGE_LEAD = new float[]{0.11f, 0.14f, 0.12f, 0.15f, 0.13f};
    private static final float DODGE_NEAR = 0.52f;
    private static final float DODGE_FAR = 0.7f;
    private static final float DODGE_AT_IMPACT = 0.9f;
    private static final float DODGE_HOLD = 0.14f;
    private static final float DODGE_BACK = 0.95f;
    private static final float DIVE_TIME = 1.1f;
    private static final float DIVE_LAG = 0.17f;
    private static final float DIVE_DEPTH = 0.95f;
    private static final float HASTY_DIVE = 0.5f;
    private static final float HASTY_LAG = 0.55f;
    private static final int WARM = -10590;
    private static final int COOL = -11637620;
    private static final int INK = -16315383;
    private static final int GOLD = -13238;
    private static final int BLACK = -16645117;
    private static final int HIT_RED = -1957094;
    private static final float RED_TAIL = 0.16f;
    private static final float HEAD_ARRIVE = 1.45f;
    private static final float SEAM_BLEED = 1.01f;
    private static final int OUTLINE_STEPS = 8;
    private final BoaArtSet art;
    private final BoaRifle rifle = new BoaRifle();
    private final BoaRifle.Pose pose = new BoaRifle.Pose();
    private final List<ModelProjection.Face> faces = new ArrayList<ModelProjection.Face>();
    private final ModelProjection.FacePool pool = new ModelProjection.FacePool();
    private static final Comparator<ModelProjection.Face> BACK_TO_FRONT = Comparator.comparingDouble(f -> f.depth);
    private BoaBackdrop forcedBackdrop;
    private BoaDirector d;
    private BoaDrawList out;
    private BoaBackdrop bd;
    private float W;
    private float H;
    private float t;
    private float st;
    private float cx;
    private float cy;
    private float shakeX;
    private float shakeY;
    private float lensX;
    private float lensY;
    private float lensR;
    private float camX;
    private float camY;
    private float camZ;
    private float unit;
    private float aimX;
    private float aimY;
    private final float cp = (float)Math.cos(Math.toRadians(-7.0));
    private final float sp = (float)Math.sin(Math.toRadians(-7.0));
    private float px;
    private float py;
    private float pz;
    private float under;
    private float sinkHead;
    private boolean steady;
    private boolean level;
    private float bob;
    private float solo = 1.0f;
    private static final float FULL_TITLE_IN = 0.25f;
    private static final float FULL_TITLE_OUT = 0.5f;
    private static final int LEAVES = 11;
    private static final float[] LEAF_DEPTHS = new float[]{0.48f, 0.72f, 1.0f};
    private static final float LEAF_TUCK = 0.06f;
    private static final float RIFLE_FOCAL = 0.8f;
    private static final float SWING_TIME = 0.5f;
    private static final float SWING_OVERSHOOT = 5.0f;
    private static final float SWING_LIFT = 0.06f;
    private static final float SWING_BREATHE = 0.035f;
    private static final float SCOPE_SWAY = 0.05f;
    private static final float AIM_SWAY = 0.045f;
    private static final float FIRE_SWAY = 0.018f;
    private static final float OPEN_SWAY = 0.03f;
    private static final float STILL_SWAY = 0.008f;
    private static final float SWAY_BLEND = 0.35f;
    private static final float PUSH_RELEASE = 0.22f;
    private static final float NERVES_IN = 0.3f;
    private static final float NERVES_OUT = 0.3f;
    private float sunX;
    private final float[] dodgeX = new float[5];
    private final float[] dodgeZ = new float[5];
    private final float[] dodgeDown = new float[5];
    private static final float DODGE_SQUEEZE = 0.1f;
    private static final float DODGE_STRETCH = 0.55f;
    private final boolean[] segDrawn = new boolean[5];
    private final float[] segFlash = new float[5];
    private final float[] segSwell = new float[5];
    private final float[] segFade = new float[5];
    private final float[] segX = new float[5];
    private final float[] segY = new float[5];
    private final float[] segZ = new float[5];
    private final float[] segYaw = new float[5];
    private final float[] segSink = new float[5];
    private final float[] segRing = new float[5];
    private final float[] segShow = new float[5];
    private static final float SURFACE_FADE = 0.035f;
    private float outline;
    private static final int LINE_MAX = 4;
    private final float[][] lineX = new float[5][4];
    private final float[][] lineY = new float[5][4];
    private final int[] lineN = new int[5];
    private final boolean[] lineUnder = new boolean[5];
    private final float[][] clipA = new float[5][4];
    private final float[][] clipB = new float[5][4];
    private final float[][] clipC = new float[5][4];
    private final int[] clipCount = new int[5];
    private final float[] cornerX = new float[8];
    private final float[] cornerY = new float[8];
    private final int[] cornerOrder = new int[8];
    private static final int BEHIND = 0;
    private static final int IN_FRONT = 1;
    private int surface = 1;
    private static final int CUT = 24;
    private final float[] pieceX = new float[24];
    private final float[] pieceY = new float[24];
    private final float[] localX = new float[24];
    private final float[] localY = new float[24];
    private final float[][] cutX = new float[20][24];
    private final float[][] cutY = new float[20][24];
    private static final int RING_SIDES = 56;
    private static final int RIM = 0;
    private static final int FOAM = 1;
    private static final int LIP = 2;
    private static final float HOLE = 0.635f;
    private static final float LIP_EDGE = 0.705f;
    private static final float HOLE_REACH = 0.6f;
    private static final float HOLE_STRETCH = 0.45f;
    private static final double HOLE_SQUARE = 6.0;
    private static final double HOLE_JOINED = 3.0;
    private final float[] holeFront = new float[5];
    private final float[] holeBack = new float[5];
    private final double[] holeFrontPower = new double[5];
    private final double[] holeBackPower = new double[5];
    private final float[][] holeX = new float[5][56];
    private final float[][] holeY = new float[5][56];
    private final float[][] holeA = new float[5][56];
    private final float[][] holeB = new float[5][56];
    private final float[][] holeC = new float[5][56];
    private final boolean[] holeOpen = new boolean[5];
    private final float[] holeLeft = new float[5];
    private final float[] holeRight = new float[5];
    private final float[] holeTop = new float[5];
    private final float[] holeBottom = new float[5];
    private final float[][] clearX = new float[5][24];
    private final float[][] clearY = new float[5][24];
    private final float[][] keptX = new float[5][24];
    private final float[][] keptY = new float[5][24];
    private final float[] cutPX = new float[24];
    private final float[] cutPY = new float[24];
    private final float[] cutQX = new float[24];
    private final float[] cutQY = new float[24];
    private static final int OUTLINE_FINE = 16;
    private static final float SQUASHED = 1.25f;
    private static final float BAND_FROM = 0.6f;
    private static final float BAND_TO = 1.3f;
    private float[] texelPitch = new float[64];
    private final Map<BoaTex, Map<Long, float[]>> painted = new EnumMap<BoaTex, Map<Long, float[]>>(BoaTex.class);
    private static final float SPLASH_LIFE = 1.45f;
    private static final float SPLASH_GRAVITY = 9.0f;
    private static final float COLUMN_HEIGHT = 0.72f;
    private static final float COLUMN_WIDTH = 0.11f;
    private static final int CROWN_JETS = 18;
    private static final int SPRAY_DROPS = 22;
    private static final float POP_GROW = 0.3f;
    private static final float POP_RISE = 0.3f;
    private static final float POP_SIZE = 0.6f;
    private static final float POP_SPIN = 240.0f;
    private static final float SHOWN_SPIN = 54.0f;
    private static final float SPIN_SETTLE = 0.35f;
    private float itemX;
    private float itemY;
    private float itemSize;
    private float itemYaw;

    public BoaScene(BoaArtSet art) {
        this.art = art;
    }

    public void forceBackdrop(BoaBackdrop backdrop) {
        this.forcedBackdrop = backdrop;
    }

    public void build(BoaDirector director, float width, float height, float scrim, BoaDrawList list) {
        this.build(director, width, height, scrim, list, 0.0f);
    }

    public void build(BoaDirector director, float width, float height, float scrim, BoaDrawList list, float pair) {
        BoaDirector.Phase phase;
        this.solo = 1.0f - BoaScene.clamp01(pair);
        this.d = director;
        this.out = list;
        this.W = width;
        this.H = height;
        this.t = director.clock();
        this.st = director.sceneClock();
        this.cx = width * 0.5f;
        this.cy = height * 0.5f;
        this.sinkHead = 0.0f;
        this.steady = false;
        this.level = false;
        this.surface = 1;
        Arrays.fill(this.lineN, 0);
        this.bd = this.forcedBackdrop != null ? this.forcedBackdrop : BoaBackdrop.forRun(director.backdropSeed());
        list.clear();
        float take = director.takeover();
        if (take <= 0.004f) {
            return;
        }
        this.shake();
        list.alpha(take);
        if (scrim > 0.0f) {
            list.fill(0.0f, 0.0f, this.W, this.H, BoaScene.argb(0.93f * scrim, 6, 12, 12));
        }
        if ((phase = director.phase()) == BoaDirector.Phase.RELEASING) {
            this.released();
            return;
        }
        switch (phase) {
            case LOAD: {
                this.chamber();
                break;
            }
            case SPIN: {
                this.spin();
                break;
            }
            case RAISE: {
                this.rifle();
                break;
            }
            case SCOPE: 
            case AIM: 
            case FIRE: 
            case IMPACT: 
            case OPEN: 
            case EXPLODE: 
            case HOLD: 
            case FAIL: 
            case ESCAPE: {
                this.scope(1.0f);
                break;
            }
            case REVEAL: {
                this.reveal(director.phaseTime());
                break;
            }
        }
    }

    private void released() {
        BoaDirector.Phase from = this.d.releasedFrom();
        if (from == BoaDirector.Phase.HOLD) {
            this.scope(1.0f);
            return;
        }
        if (this.d.shown().dropped()) {
            float held = from == BoaDirector.Phase.REVEAL ? this.d.releasedTime() : 2.6f / Math.max(0.2f, this.d.endPace());
            this.reveal(held + this.d.phaseTime());
        } else if (from == BoaDirector.Phase.FAIL || from == BoaDirector.Phase.ESCAPE || from == BoaDirector.Phase.AIM || from == BoaDirector.Phase.FIRE || from == BoaDirector.Phase.IMPACT || from == BoaDirector.Phase.OPEN || from == BoaDirector.Phase.SCOPE) {
            this.scope(1.0f);
        }
    }

    private void shake() {
        float amount = 0.0f;
        for (BoaDirector.Event e : this.d.events()) {
            float from;
            float since = this.t - e.time();
            if (since < 0.0f || since > 1.2f) continue;
            float size = switch (e.kind()) {
                case BoaDirector.Kind.SHOT -> {
                    if (e.near()) {
                        yield 0.026f;
                    }
                    yield 0.013f;
                }
                case BoaDirector.Kind.HIT -> 0.008f + 0.004f * (float)e.segment();
                case BoaDirector.Kind.EXPLODE -> {
                    if (since < this.burst()) {
                        yield 0.004f;
                    }
                    yield 0.05f;
                }
                case BoaDirector.Kind.POP -> 0.012f;
                case BoaDirector.Kind.DIVE -> 0.006f;
                default -> 0.0f;
            };
            from = e.kind() == BoaDirector.Kind.EXPLODE ? since - this.burst() : since;
            if (from < 0.0f) {
                from = 0.0f;
            }
            amount = Math.max(amount, size * this.H * (float)Math.exp(-from * 9.0f));
        }
        if (this.d.phase() == BoaDirector.Phase.LOAD && this.d.loadStep() == BoaDirector.LoadStep.SEAT && this.d.loaded() > 5) {
            amount = Math.max(amount, 0.01f * this.H * (float)Math.exp(-this.d.loadTime() * 11.0f));
        }
        this.shakeX = (BoaScene.hash(Float.floatToIntBits(this.t), 1) - 0.5f) * 2.0f * amount;
        this.shakeY = (BoaScene.hash(Float.floatToIntBits(this.t), 2) - 0.5f) * 2.0f * amount;
    }

    private float chamberSize() {
        return 0.44f * this.H;
    }

    private float chamberX() {
        return this.cx;
    }

    private float chamberY() {
        return this.cy + 0.06f * this.H;
    }

    private void count(int shown, float size, float ccy, float pop, float alpha) {
        float centre = ccy + size * 0.5f + 0.085f * this.H;
        float px = 0.11f * this.H * pop;
        int colour = shown > 5 ? -13238 : -856350;
        this.label(String.valueOf(shown), this.cx, centre - px * 0.5f, px, BoaScene.withAlpha(colour, alpha), true);
        this.label(shown == 1 ? "SHOT" : "SHOTS", this.cx, centre + 0.07f * this.H, 0.032f * this.H, BoaScene.withAlpha(-4208712, 0.9f * alpha), true);
    }

    private float chamberAngle(int k, float theta) {
        return (float)Math.toRadians(-90.0 + 45.0 * (double)k) + theta;
    }

    private void chamber() {
        float size = this.chamberSize();
        float ccx = this.chamberX() + this.shakeX;
        float ccy = this.chamberY() + this.shakeY;
        BoaDirector.LoadStep step = this.d.loadStep();
        float lt = this.d.loadTime();
        float u = BoaScene.clamp01(lt / Math.max(0.001f, this.d.loadLength()));
        int loaded = this.d.loaded();
        float pace = this.d.chamberPace();
        float slide = 0.0f;
        float spinIn = 0.0f;
        if (step == BoaDirector.LoadStep.INTRO) {
            float e = BoaScene.easeOutBack(u);
            slide = this.W * 0.7f * (1.0f - e);
            spinIn = (float)Math.toRadians(150.0) * (1.0f - e);
        }
        float ratchet = step == BoaDirector.LoadStep.SEAT ? (float)(loaded - 1) + BoaScene.easeOutBack(BoaScene.clamp01(lt * pace / 0.12f)) : (float)loaded;
        float theta = (float)Math.toRadians(-45.0 * (double)ratchet) + spinIn;
        float x = ccx + slide;
        float full = step == BoaDirector.LoadStep.FULL ? BoaScene.clamp01(1.0f - lt * pace / 0.95f) : 0.0f;
        float glow = size * (1.9f + (full > 0.0f ? 1.4f * BoaScene.easeOut(1.0f - full) : 0.0f));
        this.out.sprite(BoaTex.GLOW, this.art.width(BoaTex.GLOW), this.art.height(BoaTex.GLOW), x, ccy, glow, glow, this.t * 0.12f, BoaScene.withAlpha(-14224, 0.26f + 0.5f * full));
        this.drawCylinder(x, ccy, size, theta, 0, loaded, 1.0f, true);
        float rc = size * 0.28515625f;
        float base = size * 0.203125f;
        float gateX = x;
        float gateY = ccy - rc;
        boolean bonus = loaded > 5;
        float seatA = this.chamberAngle((loaded - 1 + 8) % 8, theta);
        float seatX = x + (float)Math.cos(seatA) * rc;
        float seatY = ccy + (float)Math.sin(seatA) * rc;
        switch (step) {
            case FLY: {
                this.flyIn(gateX, gateY, base, u, loaded >= 5);
                break;
            }
            case SEAT: {
                this.seated(seatX, seatY, base, lt * pace, bonus, loaded);
                break;
            }
            case FULL: {
                this.fullChamber(x, ccy, size, theta, lt * pace);
                break;
            }
        }
        if (step == BoaDirector.LoadStep.FULL) {
            this.fullChamberTitle(lt * pace);
        } else if (step == BoaDirector.LoadStep.END && loaded >= 8) {
            this.fullChamberTitle(0.95f + lt * pace);
        }
        float since = step == BoaDirector.LoadStep.SEAT ? lt * pace : 9.0f;
        float pop = 1.0f + 0.4f * (1.0f - BoaScene.clamp01(since / (bonus ? 0.3f : 0.22f)));
        this.count(loaded, size, ccy, pop, 1.0f);
        this.leaves(BoaScene.clamp01(this.t / 0.7f), this.chamberAgitation());
    }

    private void drawCylinder(float x, float y, float size, float theta, int first, int last, float alpha, boolean shadow) {
        int cw = this.art.width(BoaTex.CYLINDER);
        int ch = this.art.height(BoaTex.CYLINDER);
        if (shadow) {
            this.out.sprite(BoaTex.CYLINDER, cw, ch, x + size * 0.035f, y + size * 0.045f, size, size, theta, BoaScene.argb(0.5f * alpha, 4, 8, 6));
        }
        int white = BoaScene.withAlpha(-1, alpha);
        this.out.sprite(BoaTex.CYLINDER, cw, ch, x, y, size, size, theta, white);
        this.out.sprite(BoaTex.CYLINDER_SHADE, cw, ch, x, y, size, size, 0.0f, white);
        this.out.sprite(BoaTex.CYLINDER_FLUTES, cw, ch, x, y, size, size, theta, white);
        float rc = size * 0.28515625f;
        float hole = size * 0.25f;
        for (int k = 0; k < 8; ++k) {
            float a = this.chamberAngle(k, theta);
            this.out.sprite(BoaTex.CHAMBER, 128, 128, x + (float)Math.cos(a) * rc, y + (float)Math.sin(a) * rc, hole, hole, 0.0f, white);
        }
        float base = size * 0.203125f;
        int end = Math.min(last, first + 8);
        for (int r = Math.max(0, first); r < end; ++r) {
            float a = this.chamberAngle(r % 8, theta);
            BoaTex tex = r >= 5 ? BoaTex.BASE_GOLD : BoaTex.BASE;
            this.out.sprite(tex, 64, 64, x + (float)Math.cos(a) * rc, y + (float)Math.sin(a) * rc, base, base, 0.0f, white);
        }
    }

    private void flyIn(float gx, float gy, float base, float u, boolean gold) {
        float e = 1.0f - (1.0f - u) * (1.0f - u);
        float sx = this.W * 0.86f;
        float sy = this.H * 1.12f;
        float kx = this.W * 0.8f;
        float ky = gy - 0.28f * this.H;
        float bx = (1.0f - e) * (1.0f - e) * sx + 2.0f * (1.0f - e) * e * kx + e * e * gx;
        float by = (1.0f - e) * (1.0f - e) * sy + 2.0f * (1.0f - e) * e * ky + e * e * gy;
        float tx = 2.0f * (1.0f - e) * (kx - sx) + 2.0f * e * (gx - kx);
        float ty = 2.0f * (1.0f - e) * (ky - sy) + 2.0f * e * (gy - ky);
        float rot = (float)Math.atan2(ty, tx) + 1.5707964f;
        float h = base * 2.3f;
        float w = h * 48.0f / 128.0f;
        float push = BoaScene.clamp01((u - 0.7f) / 0.3f);
        this.out.sprite(gold ? BoaTex.BULLET_GOLD : BoaTex.BULLET, 48, 128, bx, by, w, h * (1.0f - 0.7f * push), rot, BoaScene.withAlpha(-1, 1.0f - push * 0.8f));
        if (gold) {
            float glow = base * 2.6f * (1.0f - push * 0.5f);
            this.out.sprite(BoaTex.GLOW, 256, 256, bx, by, glow, glow, this.t * 0.5f, BoaScene.withAlpha(-11430, 0.45f * (1.0f - push)));
        }
    }

    private void seated(float gx, float gy, float base, float since, boolean gold, int loaded) {
        float life = gold ? 0.42f : 0.28f;
        float e = BoaScene.clamp01(since / life);
        if (e >= 1.0f) {
            return;
        }
        int tint = gold ? -11430 : -5712;
        float ring = base * ((gold ? 0.7f : 0.6f) + (gold ? 2.6f : 1.4f) * BoaScene.easeOut(e));
        this.out.sprite(BoaTex.RING, 256, 256, gx, gy, ring, ring, 0.0f, BoaScene.withAlpha(tint, 1.0f - e));
        int sparks = gold ? 10 : 5;
        for (int i = 0; i < sparks; ++i) {
            float a = (float)((double)i * Math.PI * 2.0 / (double)sparks + (double)loaded);
            float r = base * (0.45f + (gold ? 2.1f : 1.3f) * BoaScene.easeOut(e));
            float s = base * (gold ? 0.44f : 0.34f) * (1.0f - e);
            this.out.sprite(BoaTex.SPARK, 32, 32, gx + (float)Math.cos(a) * r, gy + (float)Math.sin(a) * r, s, s, a, BoaScene.withAlpha(tint, 1.0f - e));
        }
        if (gold) {
            float glow = base * 3.0f * (1.0f - e);
            this.out.sprite(BoaTex.GLOW, 256, 256, gx, gy, glow, glow, this.t * 0.4f, BoaScene.withAlpha(-11430, 0.5f * (1.0f - e)));
            this.label("+1", gx + base * 1.35f, gy - 0.05f * this.H - 0.09f * this.H * BoaScene.easeOut(e), 0.075f * this.H, BoaScene.withAlpha(-13238, 1.0f - e * e), true);
        }
    }

    private void fullChamber(float x, float y, float size, float theta, float lt) {
        float rc = size * 0.28515625f;
        float base = size * 0.203125f;
        for (int k = 0; k < 8; ++k) {
            float f = BoaScene.clamp01(1.0f - Math.abs(lt - 0.07f * (float)k - 0.06f) / 0.09f);
            if (f <= 0.0f) continue;
            float a = this.chamberAngle(k, theta);
            float s = base * 1.5f * f;
            this.out.sprite(BoaTex.SPARK, 32, 32, x + (float)Math.cos(a) * rc, y + (float)Math.sin(a) * rc, s, s, 0.0f, BoaScene.withAlpha(-1, f));
        }
    }

    private void fullChamberTitle(float since) {
        float end = 1.3199999f;
        float alpha = BoaScene.easeInOut(BoaScene.clamp01(since / 0.25f)) * (1.0f - BoaScene.easeInOut(BoaScene.clamp01((since - (end - 0.5f)) / 0.5f)));
        if (alpha <= 0.01f) {
            return;
        }
        float e = BoaScene.easeOut(BoaScene.clamp01(since / 0.3f));
        float px = 0.085f * this.H * (1.0f + 0.35f * (1.0f - e));
        this.label("FULL CHAMBER!", this.cx, 0.09f * this.H - px * 0.5f, px, BoaScene.withAlpha(-13238, alpha), true);
    }

    private float spinTotal(int rolled) {
        int turn = Math.floorMod(-45 * rolled, 360);
        return 720.0f + (float)turn;
    }

    private void spin() {
        BoaRun run = this.d.run();
        float pace = this.d.chamberPace();
        float pt = this.d.phaseTime() * pace;
        float spin = 0.8f;
        float fade = 0.38f;
        float su = BoaScene.clamp01(pt / spin);
        float total = this.spinTotal(run.rolled());
        float turned = total * (1.0f - (1.0f - su) * (1.0f - su) * (1.0f - su));
        float theta = (float)Math.toRadians(-45.0 * (double)run.rolled() - (double)turned);
        float k = BoaScene.easeInOut(BoaScene.clamp01((pt - spin) / fade));
        float size = this.chamberSize() * (1.0f + 0.16f * k);
        float x = this.chamberX() + this.shakeX;
        float y = this.chamberY() + this.shakeY;
        float punch = su >= 1.0f ? 1.0f + 0.07f * (1.0f - BoaScene.clamp01((pt - spin) / 0.12f)) : 1.0f;
        float alpha = 1.0f - k;
        this.leaves(1.0f, this.chamberAgitation());
        if (su < 0.85f) {
            float speed = (1.0f - su) * (1.0f - su);
            float rc = size * 0.28515625f;
            float hole = size * 0.25f;
            for (int g = 3; g >= 1; --g) {
                float ghost = theta + (float)g * 0.16f * speed;
                int tint = BoaScene.withAlpha(-1, 0.16f * speed / (float)g);
                this.out.sprite(BoaTex.CYLINDER, 256, 256, x, y, size, size, ghost, tint);
                this.out.sprite(BoaTex.CYLINDER_FLUTES, 256, 256, x, y, size, size, ghost, tint);
                for (int c = 0; c < 8; ++c) {
                    float a = this.chamberAngle(c, ghost);
                    this.out.sprite(BoaTex.CHAMBER, 128, 128, x + (float)Math.cos(a) * rc, y + (float)Math.sin(a) * rc, hole, hole, 0.0f, tint);
                }
            }
        }
        this.drawCylinder(x, y, size * punch, theta, 0, run.rolled(), alpha, true);
        if (su >= 1.0f) {
            float since = pt - spin;
            float e = BoaScene.clamp01(since / 0.35f);
            float ring = size * (1.0f + 0.9f * BoaScene.easeOut(e));
            this.out.sprite(BoaTex.RING, 256, 256, x, y, ring, ring, 0.0f, BoaScene.withAlpha(-5712, 0.8f * (1.0f - e)));
        }
        this.count(run.rolled(), this.chamberSize(), this.chamberY(), 1.0f, alpha);
        this.ammo(k);
        if (run.rolled() >= 8) {
            this.fullChamberTitle(1.17f + pt);
        }
    }

    private void leaves(float amount, float agitation) {
        if ((amount *= this.solo) <= 0.01f) {
            return;
        }
        int seed = this.d.runSeed();
        for (int i = 0; i < 11; ++i) {
            int k = i * 16;
            float delay = BoaScene.hash(seed, k + 1) * 0.45f;
            float entry = BoaScene.clamp01((amount - delay) / Math.max(0.05f, 1.0f - delay));
            if (entry <= 0.002f) continue;
            entry = BoaScene.easeOut(entry);
            int variant = (int)(BoaScene.hash(seed, k + 2) * 6.0f);
            float anchor = -0.06f + ((float)i + 0.5f) / 11.0f * 1.12f + (BoaScene.hash(seed, k + 3) - 0.5f) * 0.08f;
            float length = 0.24f + BoaScene.hash(seed, k + 4) * 0.24f;
            float reachIn = BoaScene.clamp01(this.W / (1.5f * this.H));
            float intrusion = (0.04f + BoaScene.hash(seed, k + 5) * 0.14f) * Math.max(0.45f, reachIn);
            float tilt = (0.5f - anchor) * -1.25f + (BoaScene.hash(seed, k + 6) - 0.5f) * 0.35f;
            float reach = (0.035f + BoaScene.hash(seed, k + 7) * 0.055f) * (1.0f + agitation * 1.6f);
            float hz = 0.22f + BoaScene.hash(seed, k + 8) * 0.3f;
            float phase = BoaScene.hash(seed, k + 9) * 6.2832f + 6.2832f * hz * this.t;
            float bright = LEAF_DEPTHS[Math.min(2, (int)(BoaScene.hash(seed, k + 10) * 3.0f))];
            float alpha = (0.86f + BoaScene.hash(seed, k + 11) * 0.14f) * amount;
            for (int side = 0; side < 2; ++side) {
                boolean mirrored = side == 1;
                BoaTex tex = BoaTex.leaf(variant, mirrored);
                int tw = this.art.width(tex);
                int th = this.art.height(tex);
                float len = Math.max(length * this.H, intrusion * this.W / 0.94f);
                float scale = len / (float)tw;
                float x = Math.min(BoaScene.lerp(-len - 8.0f, intrusion * this.W - len, entry), -0.06f * len);
                float angle = tilt + reach * (float)Math.sin((double)phase + (mirrored ? Math.PI : 0.0));
                float pivotX = mirrored ? this.W - x : x;
                float pivotY = anchor * this.H;
                float shadeBy = bright * (mirrored ? 0.88f : 1.0f);
                float drop = len * 0.045f;
                this.leafQuad(tex, tw, th, pivotX + drop, pivotY + drop, angle, scale, mirrored, BoaScene.argb(0.62f * amount, 4, 10, 5));
                this.leafQuad(tex, tw, th, pivotX, pivotY, angle, scale, mirrored, BoaScene.withAlpha(BoaScene.shade(-1, shadeBy), alpha));
            }
        }
    }

    private void leafQuad(BoaTex tex, int tw, int th, float px, float py, float angle, float scale, boolean mirrored, int argb) {
        float a = mirrored ? -angle : angle;
        float cos = (float)Math.cos(a) * scale;
        float sin = (float)Math.sin(a) * scale;
        float x0 = mirrored ? (float)(-tw) : 0.0f;
        float y0 = (float)(-th) * 0.5f;
        this.out.quad(tex, 0, 0, tw, th, cos, sin, -sin, cos, px + cos * x0 - sin * y0, py + sin * x0 + cos * y0, argb);
    }

    private float chamberAgitation() {
        float pace = this.d.chamberPace();
        if (this.d.phase() == BoaDirector.Phase.SPIN) {
            float su = BoaScene.clamp01(this.d.phaseTime() * pace / 0.8f);
            return 0.15f + 0.55f * (float)this.d.run().rolled() / 8.0f + 0.4f * (float)Math.sin(Math.PI * (double)su);
        }
        float a = 0.15f + 0.55f * (float)this.d.loaded() / 8.0f;
        if (this.d.phase() != BoaDirector.Phase.LOAD) {
            return a;
        }
        float lt = this.d.loadTime() * pace;
        float u = BoaScene.clamp01(this.d.loadTime() / Math.max(0.001f, this.d.loadLength()));
        return a + (switch (this.d.loadStep()) {
            case BoaDirector.LoadStep.SEAT -> {
                if (this.d.loaded() > 5) {
                    yield 0.45f * (1.0f - BoaScene.clamp01(lt / 0.45f));
                }
                yield 0.0f;
            }
            case BoaDirector.LoadStep.FULL -> 0.6f * (float)Math.sin(Math.PI * (double)u);
            default -> 0.0f;
        });
    }

    private float hudSize() {
        return 0.17f * this.H;
    }

    private float hudX() {
        return this.W - 0.13f * this.H;
    }

    private float hudY() {
        return this.H - 0.14f * this.H;
    }

    private void ammo(float alpha) {
        int left;
        if (alpha <= 0.01f) {
            return;
        }
        BoaRun run = this.d.run();
        int fired = this.d.roundsFired();
        BoaDirector.Event shot = this.last(BoaDirector.Kind.SHOT);
        float since = shot == null ? 9.0f : this.t - shot.time();
        float turn = fired > 0 && since < 0.2f ? (float)(fired - 1) + BoaScene.easeOutBack(since / 0.2f) : (float)fired;
        float theta = (float)Math.toRadians(-45.0 * (double)turn);
        float size = this.hudSize();
        float x = this.hudX();
        float y = this.hudY();
        float rc = size * 0.28515625f;
        if (this.d.lastShot() && this.d.phase() == BoaDirector.Phase.AIM) {
            float beat = this.heartbeat();
            float a = this.chamberAngle(fired % 8, theta);
            float g = size * (0.55f + 0.25f * beat);
            this.out.sprite(BoaTex.GLOW, 256, 256, x + (float)Math.cos(a) * rc, y + (float)Math.sin(a) * rc, g, g, 0.0f, BoaScene.withAlpha(-46550, alpha * (0.35f + 0.5f * beat)));
        }
        this.drawCylinder(x, y, size, theta, fired, run.bullets(), alpha, true);
        if (shot != null && since < 0.16f) {
            float a = this.chamberAngle((fired - 1 + 8) % 8, theta);
            float k = since / 0.16f;
            float s = size * 0.5f * (1.0f - k);
            this.out.sprite(BoaTex.SPARK, 32, 32, x + (float)Math.cos(a) * rc, y + (float)Math.sin(a) * rc, s, s, k * 2.0f, BoaScene.withAlpha(-3904, alpha * (1.0f - k)));
        }
        int colour = (left = run.bullets() - fired) <= 1 ? -42422 : -856350;
        this.label("x" + left, x - size * 0.95f, y - 0.035f * this.H, 0.075f * this.H, BoaScene.withAlpha(colour, alpha), true);
        for (BoaDirector.Event e : this.d.events()) {
            float s = this.t - e.time();
            if (e.kind() != BoaDirector.Kind.RELOAD || !(s >= 0.0f) || !(s < 1.6f)) continue;
            this.label("RELOAD +" + e.segment(), x - size * 0.4f, y - size * 0.75f - s * 0.05f * this.H, 0.05f * this.H, BoaScene.withAlpha(-13238, alpha * (1.0f - s / 1.6f)), true);
        }
    }

    private void tracker(float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        int hits = this.d.run().hits();
        float s = 0.066f * this.H;
        float gap = 0.028f * this.H;
        float total = 5.0f * s + 4.0f * gap;
        float y = 0.052f * this.H;
        for (int i = 0; i < 5; ++i) {
            float x = this.cx - total * 0.5f + s * 0.5f + (float)i * (s + gap);
            boolean head = i == 4;
            boolean down = i < hits;
            boolean current = i == hits && this.isShooting();
            float pad = Math.max(1.5f, s * 0.07f);
            this.out.fill(x - s * 0.5f - pad, y - s * 0.5f - pad, x + s * 0.5f + pad, y + s * 0.5f + pad, BoaScene.withAlpha(-16315383, alpha));
            BoaTex tex = head ? BoaTex.HEAD : BoaTex.BODY;
            int tint = down ? -12961222 : -1;
            this.out.region(tex, 8, 8, 8, 8, x, y, s, s, 0.0f, BoaScene.withAlpha(tint, alpha));
            if (head) {
                this.out.region(tex, 40, 8, 8, 8, x, y, s, s, 0.0f, BoaScene.withAlpha(tint, alpha));
            }
            if (down) {
                this.out.sprite(BoaTex.CROSS, 32, 32, x, y, s * 0.9f, s * 0.9f, 0.0f, BoaScene.withAlpha(-1, alpha));
            }
            if (current) {
                float pulse = 0.6f + 0.4f * (float)Math.sin((double)this.t * (head ? 11.0 : 7.0));
                int frame = BoaScene.withAlpha(head ? -42438 : -13238, alpha * pulse);
                float th = Math.max(1.5f, s * 0.09f);
                float e = s * 0.5f + pad + th;
                this.out.fill(x - e, y - e, x + e, y - e + th, frame);
                this.out.fill(x - e, y + e - th, x + e, y + e, frame);
                this.out.fill(x - e, y - e + th, x - e + th, y + e - th, frame);
                this.out.fill(x + e - th, y - e + th, x + e, y + e - th, frame);
            }
            if (!head || down) continue;
            float tw = 0.5f + 0.5f * (float)Math.sin((double)this.t * 4.0);
            float ss = s * 0.45f * (0.6f + 0.4f * tw);
            this.out.sprite(BoaTex.SPARK, 32, 32, x + s * 0.45f, y - s * 0.45f, ss, ss, this.t, BoaScene.withAlpha(-13238, alpha * tw));
        }
    }

    private boolean isShooting() {
        BoaDirector.Phase phase = this.d.phase() == BoaDirector.Phase.RELEASING ? this.d.releasedFrom() : this.d.phase();
        return phase == BoaDirector.Phase.AIM || phase == BoaDirector.Phase.FIRE || phase == BoaDirector.Phase.SCOPE;
    }

    private void rifle() {
        float pace = this.d.loadPace();
        float pt = this.d.phaseTime() * pace;
        float rise = 0.7f;
        float hold = 0.22f;
        float zoom = 0.62f;
        float e = BoaScene.easeOut(BoaScene.clamp01(pt / rise));
        float z = BoaScene.clamp01((pt - rise - hold) / zoom);
        float s = BoaScene.easeInOut(z);
        float breathe = (float)Math.sin((double)this.t * 1.9);
        BoaRifle.Pose p = this.pose;
        p.focal = 0.8f * this.H;
        p.moveX = 0.0f;
        p.moveY = -3.2f * (1.0f - e) + 0.015f * breathe * (1.0f - s);
        p.moveZ = 0.0f;
        p.pitch = -24.0f * (1.0f - e) + 0.35f * breathe * (1.0f - s);
        p.roll = -14.0f * (1.0f - e) + BoaScene.lerp(-5.0f, 0.0f, s);
        p.yaw = BoaScene.lerp(2.5f, 0.0f, s);
        p.boltLift = 62.0f * (BoaScene.ramp(pt, 0.56f, 0.62f) - BoaScene.ramp(pt, 0.8f, 0.86f));
        p.boltBack = 0.42f * (BoaScene.ramp(pt, 0.62f, 0.68f) - BoaScene.ramp(pt, 0.74f, 0.8f));
        float end = -0.95f - p.focal * 0.2f / this.lensRadius();
        float aside = 1.6f * Math.max(0.7f, Math.min(1.0f, this.W / this.H / 1.78f));
        p.camX = BoaScene.lerp(-aside, 0.0f, s);
        p.camY = BoaScene.lerp(0.95f, 0.66f, s);
        p.camZ = BoaScene.lerp(-3.3f, end, s);
        p.camYaw = 0.0f;
        p.camPitch = BoaScene.lerp(-4.0f, 0.0f, s);
        p.camRoll = 0.0f;
        p.centreX = this.cx + this.shakeX;
        p.centreY = BoaScene.lerp(this.cy, this.lensCentreY(), s) + this.shakeY;
        p.ink = Math.max(1.1f, 0.0042f * this.H * (1.0f + 0.8f * s));
        p.glassDark = BoaScene.clamp01((z - 0.55f) / 0.45f);
        float horizon = p.centreY + p.focal * (float)Math.tan(Math.toRadians(p.camPitch));
        this.wideBayou(horizon, BoaScene.easeOut(BoaScene.clamp01(pt / 0.5f)));
        this.leaves(1.0f - BoaScene.easeInOut(BoaScene.clamp01(z * 1.6f)), 0.15f + 0.55f * (float)this.d.run().rolled() / 8.0f);
        this.rifle.draw(this.out, p);
        float dark = BoaScene.clamp01((z - 0.8f) / 0.2f);
        if (dark > 0.0f) {
            this.out.fill(0.0f, 0.0f, this.W, this.H, BoaScene.withAlpha(-16645117, dark));
        }
        this.ammo(1.0f - BoaScene.easeInOut(BoaScene.clamp01(pt / 0.35f)));
    }

    private void wideBayou(float horizon, float alpha) {
        int i;
        float k;
        float y;
        if (alpha <= 0.01f) {
            return;
        }
        float before = this.out.alpha();
        this.out.alpha(before * alpha);
        float skyW = Math.max(this.W, this.H * 1.9f) * 1.15f;
        float skyH = skyW * 400.0f / 1024.0f;
        float skyX = this.cx;
        this.out.fill(0.0f, 0.0f, this.W, horizon - skyH + 2.0f, this.bd.top);
        this.out.sprite(this.bd.sky, 1024, 400, skyX, horizon - skyH * 0.5f, skyW, skyH, 0.0f, -1);
        this.out.fill(0.0f, horizon, this.W, this.H, this.bd.waterDeep);
        this.out.fill(0.0f, horizon, this.W, horizon + 0.008f * this.H, this.bd.waterFar);
        this.out.fill(0.0f, horizon + 0.008f * this.H, this.W, horizon + 0.02f * this.H, BoaScene.lerpColor(this.bd.waterFar, this.bd.waterDeep, 0.5f));
        float glintX = skyX - skyW * 0.5f + this.bd.glintAt * skyW;
        for (int row = 0; row < 36 && !((y = horizon + 0.02f * this.H + (this.H - horizon) * (k = ((float)row + 0.5f) / 36.0f) * k) > this.H); ++row) {
            float thick = Math.max(1.0f, this.H * (0.0015f + 0.0045f * k));
            for (int c = 0; c < 10; ++c) {
                float len;
                float h1 = BoaScene.hash(row * 131 + 7, c);
                if (h1 > 0.55f) continue;
                float x = ((float)c + h1) / 10.0f * this.W * 1.2f - 0.1f * this.W + (float)Math.sin((double)this.t * 0.7 + (double)row) * this.H * 0.004f;
                boolean lit = Math.abs(x + (len = (0.02f + 0.05f * BoaScene.hash(row, c + 50)) * this.W * (0.3f + 0.9f * k)) * 0.5f - glintX) < 0.05f * this.W * (0.4f + k);
                this.out.fill(x, y, x + len, y + thick, BoaScene.withAlpha(lit ? this.bd.glint : this.bd.waterLight, lit ? 0.8f : 0.22f + 0.35f * k));
            }
        }
        float[] aim = this.rifle.project(this.pose, 0.0f, 0.66f, 400.0f);
        float sx0 = (aim != null ? aim[0] : this.cx) + 0.07f * this.H;
        float sy0 = horizon + 0.045f * this.H;
        float seg = 0.017f * this.H;
        for (i = 0; i < 5; ++i) {
            float x = sx0 + (float)i * seg * 1.25f + (float)Math.sin((double)this.t * 1.8 + (double)i) * seg * 0.1f;
            float y2 = sy0 + (float)Math.sin((double)this.t * 3.0 - (double)i * 0.9) * seg * 0.12f;
            this.out.sprite(BoaTex.RIPPLE, 128, 128, x, y2 + seg * 0.35f, seg * 1.2f, seg * 0.42f, 0.0f, BoaScene.withAlpha(-1, 0.6f));
            this.out.fill(x - seg * 0.5f, y2 - seg * (i == 4 ? 0.75f : 0.5f), x + seg * 0.5f, y2 + seg * 0.35f, BoaScene.lerpColor(this.bd.cool, -16315383, 0.55f));
        }
        if (this.bd.haze != 0) {
            this.out.fill(0.0f, horizon - 0.03f * this.H, this.W, horizon + 0.05f * this.H, this.bd.haze);
        }
        if (this.bd == BoaBackdrop.MOONLIT) {
            for (i = 0; i < 12; ++i) {
                float blink = this.firefly(i);
                if (blink <= 0.02f) continue;
                float fx = BoaScene.hash(i, 71) * this.W;
                float fy = horizon + (0.05f + 0.35f * BoaScene.hash(i, 72)) * (this.H - horizon) + (float)Math.sin((double)this.t * 0.9 + (double)i) * 0.01f * this.H;
                float g = 0.03f * this.H;
                this.out.sprite(BoaTex.GLOW, 256, 256, fx, fy, g, g, 0.0f, BoaScene.withAlpha(-2949270, 0.7f * blink));
                this.out.sprite(BoaTex.DISC, 64, 64, fx, fy, g * 0.12f, g * 0.12f, 0.0f, BoaScene.withAlpha(-589870, blink));
            }
        }
        this.out.alpha(before);
    }

    private float firefly(int i) {
        float rate = 0.8f + 0.9f * BoaScene.hash(i, 73);
        float v = (float)Math.sin(this.t * rate * 2.4f + BoaScene.hash(i, 74) * 6.28f);
        return v <= 0.0f ? 0.0f : v * v * v;
    }

    private float lensRadius() {
        return 0.425f * this.H;
    }

    private float lensCentreY() {
        return 0.535f * this.H;
    }

    private void scope(float hudAlpha) {
        BoaDirector.Phase phase;
        this.camera(this.lensRadius());
        BoaDirector.Phase phase2 = phase = this.d.phase() == BoaDirector.Phase.RELEASING ? this.d.releasedFrom() : this.d.phase();
        if (phase == BoaDirector.Phase.SCOPE && this.d.phase() == BoaDirector.Phase.SCOPE) {
            float iris = BoaScene.clamp01(this.d.phaseTime() * this.d.loadPace() / 0.4f);
            this.lensR = this.lensRadius() * BoaScene.easeOut(iris);
            hudAlpha *= iris;
        }
        this.bayou();
        this.placeSnake();
        this.effectsBehind();
        this.snake();
        this.effectsInFront();
        this.effects();
        this.fireflies();
        this.tracerAndBullet();
        this.reeds();
        if (phase == BoaDirector.Phase.HOLD && this.d.shown().inHead()) {
            this.floatingItem();
        } else if (phase == BoaDirector.Phase.OPEN || phase == BoaDirector.Phase.HOLD) {
            this.poppedItem();
        } else {
            this.tossedItem();
        }
        this.lensDressing(phase, 1.0f);
        this.tracker(hudAlpha);
        this.ammo(hudAlpha);
        this.callouts(phase);
        if (phase == BoaDirector.Phase.FAIL) {
            float into = this.d.phase() == BoaDirector.Phase.FAIL ? this.d.phaseTime() * this.d.endPace() : 2.8f;
            float dark = BoaScene.clamp01((into - 2.0f) / 0.8f) * 0.6f;
            this.out.fill(0.0f, 0.0f, this.W, this.H, BoaScene.withAlpha(-16645117, dark));
        }
        if (phase == BoaDirector.Phase.ESCAPE) {
            this.gotAway();
        }
    }

    private void gotAway() {
        float into = this.d.phase() == BoaDirector.Phase.ESCAPE ? this.d.phaseTime() * this.d.endPace() : 2.35f;
        float in = BoaScene.clamp01((into - 0.2f - 0.35f) / 0.3f);
        if (in <= 0.0f) {
            return;
        }
        float dark = BoaScene.clamp01((into - 0.7f) / 1.0f) * 0.55f;
        this.out.fill(0.0f, 0.0f, this.W, this.H, BoaScene.withAlpha(-16645117, dark));
        float drop = 0.02f * this.H * (1.0f - BoaScene.easeOut(in));
        this.label("IT GOT AWAY", this.cx, this.lensY - 0.0375f * this.H + drop, 0.075f * this.H, BoaScene.withAlpha(-1511464, in), true);
    }

    private void camera(float radius) {
        boolean swinging;
        int target;
        BoaRun run = this.d.run();
        BoaDirector.Phase phase = this.d.phase() == BoaDirector.Phase.RELEASING ? this.d.releasedFrom() : this.d.phase();
        int from = target = Math.min(4, run.hits());
        float blend = 1.0f;
        float swing = -1.0f;
        BoaDirector.Event lastHit = this.last(BoaDirector.Kind.EVAPORATE);
        BoaDirector.Event empty = this.fork();
        BoaPrize shown = this.d.shown();
        if (shown.dropped() && !shown.inHead()) {
            target = 3;
            from = 3;
        } else if (run.hits() == 4 && (empty == null || empty.near()) && phase != BoaDirector.Phase.EXPLODE) {
            target = 3;
            from = 3;
        } else if (run.hits() == 4 && empty != null) {
            from = 3;
            swing = BoaScene.clamp01((this.t - empty.time()) / 0.5f);
            blend = BoaScene.swingCurve(swing);
        } else if (lastHit != null && lastHit.segment() == target - 1) {
            swing = BoaScene.clamp01((this.t - lastHit.time()) / 0.5f);
            blend = BoaScene.swingCurve(swing);
            from = lastHit.segment();
        }
        this.steady = true;
        this.level = true;
        this.segment(from, this.st);
        float fx = this.px;
        float fy = this.py;
        float fz = this.pz;
        this.segment(target, this.st);
        this.steady = false;
        this.level = false;
        this.camX = BoaScene.lerp(fx, this.px, blend);
        this.camY = BoaScene.lerp(fy, this.py, blend);
        this.camZ = BoaScene.lerp(fz, this.pz, blend);
        boolean bl = swinging = swing > 0.0f && swing < 1.0f;
        if (swinging) {
            float reach = (float)Math.hypot(this.px - fx, this.pz - fz);
            this.camY += 0.06f * reach * (float)Math.sin(Math.PI * (double)swing);
        }
        BoaDirector.Event burst = this.last(BoaDirector.Kind.EXPLODE);
        if (target == 4 && burst != null && (phase == BoaDirector.Phase.EXPLODE || (phase == BoaDirector.Phase.REVEAL || phase == BoaDirector.Phase.HOLD) && shown.inHead())) {
            this.steady = true;
            this.level = true;
            this.segmentAt(burst.time(), 4);
            this.steady = false;
            this.level = false;
            this.camX = this.px;
            this.camY = this.py;
            this.camZ = this.pz;
        }
        float zoom = BoaScene.lerp(ZOOM[from], ZOOM[target], swing < 0.0f ? blend : BoaScene.smoother(swing));
        if (swinging) {
            zoom *= 1.0f - 0.035f * (float)Math.sin(Math.PI * (double)swing);
        }
        zoom *= 1.0f + this.aimPush(phase, target);
        if (run.hits() > 3 && from == 3) {
            float lean = phase == BoaDirector.Phase.IMPACT || empty != null && !this.heldOver(empty) ? 0.0f : (target == 4 ? 1.0f - BoaScene.smoother(swing) : (phase == BoaDirector.Phase.OPEN && empty == null && !shown.dropped() ? BoaScene.clamp01(this.d.phaseTime() * this.d.endPace() / 0.48f) : 1.0f));
            zoom *= 1.0f + 0.16f * BoaScene.easeInOut(lean);
        }
        if (phase == BoaDirector.Phase.SCOPE) {
            float pace = this.d.loadPace();
            float search = BoaScene.clamp01((this.d.phaseTime() * pace - 0.4f) / 0.75f);
            float away = 1.0f - BoaScene.easeInOut(search);
            this.camX -= 2.4f * away;
            this.camZ += 0.6f * away;
            zoom *= BoaScene.lerp(0.84f, 1.0f, BoaScene.easeInOut(search));
        }
        this.unit = 0.19f * this.H * zoom;
        float amp = this.sway(phase, swing) * radius;
        this.aimX = amp * ((float)Math.sin((double)this.t * 1.7) + 0.5f * (float)Math.sin((double)this.t * 3.3 + 1.3));
        this.aimY = amp * 0.8f * ((float)Math.cos((double)this.t * 1.3) + 0.5f * (float)Math.sin((double)this.t * 2.9 + 0.4));
        float nerve = this.nerves(phase) * radius;
        if (nerve > 0.0f) {
            this.aimX += nerve * this.tremble(11);
            this.aimY += nerve * 0.85f * this.tremble(23);
        }
        float recoilX = 0.0f;
        float recoilY = 0.0f;
        BoaDirector.Event shot = this.last(BoaDirector.Kind.SHOT);
        if (shot != null) {
            float rise;
            float since = (this.t - shot.time()) * this.d.shotPace();
            float kick = since < (rise = 0.022f) ? BoaScene.easeOut(since / rise) : (float)(Math.exp((double)(-(since - rise)) * 6.5) * Math.cos((double)(since - rise) * 19.0));
            float size = shot.near() ? 0.125f : 0.095f;
            recoilY = -size * this.H * kick;
            recoilX = 0.022f * this.H * kick * (BoaScene.hash(shot.seed(), 3) - 0.5f) * 2.0f;
        }
        this.lensX = this.cx + recoilX + this.shakeX;
        this.lensY = this.lensCentreY() + recoilY + this.shakeY;
        this.lensR = radius;
    }

    private static float swingCurve(float u) {
        return BoaScene.swingCurve(u, 5.0f);
    }

    private static float swingCurve(float u, float overshoot) {
        u = BoaScene.clamp01(u);
        float back = 1.0f - u;
        return BoaScene.smoother(u) + overshoot * u * u * u * back * back;
    }

    private float sway(BoaDirector.Phase phase, float swing) {
        float own = this.beatSway(phase, swing);
        if (this.d.phase() == BoaDirector.Phase.RELEASING) {
            return own;
        }
        float into = BoaScene.clamp01(this.d.phaseTime() * this.paceOf(phase) / 0.35f);
        if (into >= 1.0f) {
            return own;
        }
        return BoaScene.lerp(this.entrySway(phase, swing), own, BoaScene.smoother(into));
    }

    private float paceOf(BoaDirector.Phase phase) {
        return switch (phase) {
            case BoaDirector.Phase.LOAD, BoaDirector.Phase.SPIN -> this.d.chamberPace();
            case BoaDirector.Phase.RAISE, BoaDirector.Phase.SCOPE -> this.d.loadPace();
            case BoaDirector.Phase.AIM, BoaDirector.Phase.FIRE, BoaDirector.Phase.IMPACT -> this.d.shotPace();
            default -> this.d.endPace();
        };
    }

    private float beatSway(BoaDirector.Phase phase, float swing) {
        return switch (phase) {
            case BoaDirector.Phase.SCOPE -> 0.05f;
            case BoaDirector.Phase.AIM -> BoaScene.lerp(0.045f, this.d.lastShot() ? 0.0025f : 0.005f, BoaScene.easeInOut(this.d.aimProgress()));
            case BoaDirector.Phase.FIRE -> 0.018f;
            case BoaDirector.Phase.IMPACT -> {
                BoaDirector.Event hit = this.last(BoaDirector.Kind.HIT);
                BoaDirector.Event miss = this.lastMiss();
                if (miss != null && (hit == null || miss.time() >= hit.time())) {
                    yield BoaScene.lerp(0.018f, 0.045f, BoaScene.smoother(this.d.phaseProgress()));
                }
                if (swing >= 0.0f) {
                    yield BoaScene.lerp(0.018f, 0.045f, BoaScene.smoother(swing));
                }
                yield 0.018f;
            }
            case BoaDirector.Phase.EXPLODE, BoaDirector.Phase.REVEAL -> 0.008f;
            case BoaDirector.Phase.HOLD -> {
                if (this.d.shown().inHead()) {
                    yield 0.008f;
                }
                yield 0.03f;
            }
            default -> 0.03f;
        };
    }

    private float entrySway(BoaDirector.Phase phase, float swing) {
        return switch (phase) {
            case BoaDirector.Phase.AIM -> {
                if (this.d.run().fired() == 0) {
                    yield 0.05f;
                }
                BoaDirector.Event landed = this.lastLanding();
                if (landed != null && BoaScene.isFork(landed) && this.heldOver(landed)) {
                    yield 0.03f;
                }
                if (swing >= 0.0f && swing < 1.0f) {
                    yield BoaScene.lerp(0.018f, 0.045f, BoaScene.smoother(swing));
                }
                yield 0.045f;
            }
            case BoaDirector.Phase.FIRE -> {
                if (this.d.lastShot()) {
                    yield 0.0025f;
                }
                yield 0.005f;
            }
            case BoaDirector.Phase.IMPACT, BoaDirector.Phase.EXPLODE -> {
                BoaDirector.Event landed;
                BoaDirector.Event shot = this.last(BoaDirector.Kind.SHOT);
                BoaDirector.Event v1 = landed = shot == null ? null : this.landing(shot);
                if (landed == null) {
                    yield 0.018f;
                }
                float flight = (landed.time() - shot.time()) * this.d.shotPace();
                yield BoaScene.lerp(shot.near() ? 0.0025f : 0.005f, 0.018f, BoaScene.smoother(flight / 0.35f));
            }
            case BoaDirector.Phase.OPEN -> 0.018f;
            case BoaDirector.Phase.REVEAL -> {
                if (this.d.shown().inHead()) {
                    yield 0.008f;
                }
                yield 0.03f;
            }
            case BoaDirector.Phase.FAIL, BoaDirector.Phase.ESCAPE -> {
                BoaDirector.Event landed = this.lastLanding();
                if (landed != null && BoaScene.isFork(landed)) {
                    if (this.heldOver(landed)) {
                        yield 0.03f;
                    }
                    yield 0.018f;
                }
                yield 0.045f;
            }
            default -> this.beatSway(phase, swing);
        };
    }

    private boolean heldOver(BoaDirector.Event empty) {
        BoaDirector.Event gone = this.evaporation(3);
        return gone != null && empty.time() > gone.time() + 1.0E-4f;
    }

    private BoaDirector.Event fork() {
        BoaDirector.Event empty = this.last(BoaDirector.Kind.EMPTY);
        BoaDirector.Event toss = this.last(BoaDirector.Kind.TOSS);
        if (toss == null) {
            return empty;
        }
        return empty == null || toss.time() >= empty.time() ? toss : empty;
    }

    private static boolean isFork(BoaDirector.Event e) {
        return e.kind() == BoaDirector.Kind.EMPTY || e.kind() == BoaDirector.Kind.TOSS;
    }

    private BoaDirector.Event lastLanding() {
        List<BoaDirector.Event> events = this.d.events();
        for (int i = events.size() - 1; i >= 0; --i) {
            BoaDirector.Kind kind = events.get(i).kind();
            if (kind != BoaDirector.Kind.HIT && kind != BoaDirector.Kind.MISS && kind != BoaDirector.Kind.EMPTY && kind != BoaDirector.Kind.TOSS && kind != BoaDirector.Kind.EXPLODE) continue;
            return events.get(i);
        }
        return null;
    }

    private float aimPush(BoaDirector.Phase phase, int target) {
        if (phase == BoaDirector.Phase.AIM) {
            return BoaScene.fullPush(target, this.d.lastShot()) * BoaScene.easeInOut(this.d.aimProgress());
        }
        if (phase != BoaDirector.Phase.FIRE && phase != BoaDirector.Phase.IMPACT && phase != BoaDirector.Phase.EXPLODE) {
            return 0.0f;
        }
        BoaDirector.Event shot = this.last(BoaDirector.Kind.SHOT);
        if (shot == null) {
            return 0.0f;
        }
        float since = (this.t - shot.time()) * this.d.shotPace();
        return BoaScene.fullPush(Math.min(4, shot.segment()), shot.near()) * (1.0f - BoaScene.smoother(since / 0.22f));
    }

    private static float fullPush(int target, boolean lastShot) {
        return 0.07f + (target == 4 ? 0.12f : 0.0f) + (lastShot ? 0.14f : 0.0f);
    }

    private float nerves(BoaDirector.Phase phase) {
        BoaDirector.Event landed;
        if (phase == BoaDirector.Phase.AIM) {
            int hits = this.d.run().hits();
            if (hits < 3) {
                return 0.0f;
            }
            float in = this.d.phase() == BoaDirector.Phase.RELEASING ? 1.0f : BoaScene.smoother(this.d.phaseTime() / 0.3f);
            return BoaScene.nerveBase(hits) * (0.3f + 0.7f * BoaScene.easeInOut(this.d.aimProgress())) * in;
        }
        if (phase != BoaDirector.Phase.FIRE && phase != BoaDirector.Phase.IMPACT && phase != BoaDirector.Phase.EXPLODE) {
            return 0.0f;
        }
        BoaDirector.Event shot = this.last(BoaDirector.Kind.SHOT);
        if (shot == null || shot.segment() < 3) {
            return 0.0f;
        }
        BoaDirector.Event event = landed = phase == BoaDirector.Phase.FIRE ? null : this.landing(shot);
        if (landed == null) {
            return BoaScene.nerveBase(shot.segment());
        }
        return BoaScene.nerveBase(shot.segment()) * (1.0f - BoaScene.smoother((this.t - landed.time()) / 0.3f));
    }

    private static float nerveBase(int segment) {
        return segment >= 4 ? 0.032f : 0.014f;
    }

    private float tremble(int offset) {
        return 0.55f * (float)Math.sin((double)this.t * 23.7 + (double)offset) + 0.3f * (float)Math.sin((double)this.t * 37.1 + (double)offset * 1.7) + 0.15f * (float)Math.sin((double)this.t * 13.3 + (double)offset * 0.6);
    }

    private float screenX(float x) {
        return this.lensX + (x - this.camX) * this.unit - this.aimX;
    }

    private float screenY(float y, float z) {
        return this.lensY - ((y - this.camY) * this.cp + (z - this.camZ) * this.sp) * this.unit - this.aimY;
    }

    private void bayou() {
        float left = this.lensX - this.lensR - 2.0f;
        float right = this.lensX + this.lensR + 2.0f;
        float top = this.lensY - this.lensR - 2.0f;
        float bottom = this.lensY + this.lensR + 2.0f;
        float horizon = this.screenY(0.0f, this.camZ - 8.5f);
        float skyH = 0.46f * this.H * (0.8f + 0.2f * this.unit / (0.19f * this.H));
        float skyW = skyH * 1024.0f / 400.0f;
        float drift = this.camX * this.unit * 0.08f;
        float first = this.lensX - skyW * 0.5f - (float)Math.floorMod((long)drift, (long)Math.max(1.0f, skyW));
        this.out.fill(left, top, right, horizon - skyH + 2.0f, this.bd.top);
        float sunX = 0.0f;
        float bestSun = Float.MAX_VALUE;
        for (int i = -2; i <= 2; ++i) {
            float x = first + (float)i * skyW;
            if (x + skyW < left || x > right) continue;
            this.out.sprite(this.bd.sky, 1024, 400, x + skyW * 0.5f, horizon - skyH * 0.5f, skyW + 1.0f, skyH, 0.0f, -1);
            float sx = x + skyW * this.bd.glintAt;
            if (!(Math.abs(sx - this.lensX) < bestSun)) continue;
            bestSun = Math.abs(sx - this.lensX);
            sunX = sx;
        }
        this.sunX = sunX;
        this.out.fill(left, horizon, right, bottom, this.bd.waterDeep);
        this.out.fill(left, horizon, right, horizon + 0.012f * this.H, this.bd.waterFar);
        this.out.fill(left, horizon + 0.012f * this.H, right, horizon + 0.03f * this.H, BoaScene.lerpColor(this.bd.waterFar, this.bd.waterDeep, 0.5f));
        this.streaks(left, horizon + 1.0f, right, bottom, horizon, bottom);
        if (this.bd.haze != 0) {
            this.out.fill(left, horizon - 0.022f * this.H, right, horizon + 0.045f * this.H, this.bd.haze);
        }
        this.mist();
    }

    private void streaks(float x0, float y0, float x1, float y1, float horizon, float bottom) {
        float step = 0.55f;
        float far = this.camZ - 8.5f;
        int firstRow = (int)Math.floor(far / step);
        int lastRow = firstRow + 90;
        for (int row = firstRow; row <= lastRow; ++row) {
            int firstCell;
            float z = (float)row * step;
            float y = this.screenY(0.0f, z);
            if (y < horizon + 1.0f) continue;
            if (y > bottom) break;
            float near = BoaScene.clamp01((y - horizon) / Math.max(1.0f, bottom - horizon));
            float thick = Math.max(1.0f, this.H * (0.002f + 0.005f * near));
            if (y + thick < y0 || y > y1) continue;
            float cell = 1.1f;
            for (int c = firstCell = (int)Math.floor((this.camX - 6.0f) / cell); c < firstCell + 12; ++c) {
                float h1 = BoaScene.hash(row * 7919, c);
                if (h1 > 0.5f) continue;
                float wx = (float)c * cell + h1 * 0.8f + (float)Math.sin((double)this.t * 0.8 + (double)row) * 0.05f;
                float len = (0.25f + 0.5f * BoaScene.hash(row, c * 31)) * this.unit * (0.5f + 0.7f * near);
                float sx = this.screenX(wx);
                if (sx + len < x0 || sx > x1) continue;
                boolean sunlit = Math.abs(sx + len * 0.5f - this.sunX) < 0.07f * this.W;
                int colour = sunlit ? this.bd.glint : this.bd.waterLight;
                float a = sunlit ? 0.75f : 0.3f + 0.35f * near;
                this.streak(Math.max(x0, sx), Math.max(y0, y), Math.min(x1, sx + len), Math.min(y1, y + thick), BoaScene.withAlpha(colour, a));
            }
        }
    }

    private void mist() {
        if (this.bd != BoaBackdrop.MIST) {
            return;
        }
        int seed = this.d.runSeed();
        for (int i = 0; i < 8; ++i) {
            float span = 12.0f;
            float base = BoaScene.hash(seed, 200 + i) * span + this.t * (0.1f + 0.06f * BoaScene.hash(seed, 210 + i));
            float x = this.camX - span * 0.5f + (float)Math.floorMod((long)((base - this.camX) * 100.0f), (long)(span * 100.0f)) / 100.0f;
            float z = this.camZ - 1.2f - BoaScene.hash(seed, 220 + i) * 3.5f;
            float y = 0.05f + 0.25f * BoaScene.hash(seed, 230 + i);
            float w = this.unit * (1.8f + 1.6f * BoaScene.hash(seed, 240 + i));
            this.out.sprite(BoaTex.PUFF, 128, 128, this.screenX(x), this.screenY(y, z), w, w * 0.3f, 0.0f, BoaScene.withAlpha(-1380120, 0.22f + 0.12f * BoaScene.hash(seed, 250 + i)));
        }
    }

    private void fireflies() {
        if (this.bd != BoaBackdrop.MOONLIT) {
            return;
        }
        int seed = this.d.runSeed();
        for (int i = 0; i < 14; ++i) {
            float blink = this.firefly(i + 20);
            if (blink <= 0.02f) continue;
            float span = 10.0f;
            float base = BoaScene.hash(seed, 300 + i) * span + (float)Math.sin((double)this.t * 0.4 + (double)i) * 0.3f;
            float x = this.camX - span * 0.5f + (float)Math.floorMod((long)((base - this.camX) * 100.0f), (long)(span * 100.0f)) / 100.0f;
            float y = 0.35f + 0.9f * BoaScene.hash(seed, 310 + i) + (float)Math.sin((double)this.t * 0.7 + (double)i * 1.7) * 0.08f;
            float z = this.camZ - 2.6f + BoaScene.hash(seed, 320 + i) * 3.4f;
            float g = this.unit * 0.32f;
            float sx = this.screenX(x);
            float sy = this.screenY(y, z);
            this.out.sprite(BoaTex.GLOW, 256, 256, sx, sy, g, g, 0.0f, BoaScene.withAlpha(-2949270, 0.65f * blink));
            this.out.sprite(BoaTex.DISC, 64, 64, sx, sy, g * 0.12f, g * 0.12f, 0.0f, BoaScene.withAlpha(-589870, blink));
        }
    }

    private void segment(int i, float clock) {
        int j = 4 - i;
        float head = -1.2f + 0.4f * clock;
        float x = head - (float)j * 0.64f;
        float thrash = this.thrash();
        float amp = 0.3f * (1.0f + 0.9f * thrash);
        float z = amp * (float)Math.sin(2.0f * x);
        this.bob = (float)Math.sin(2.8f * x - 4.0f * clock);
        float y = 0.13f + (this.level ? 0.0f : 0.06f * (1.0f + 0.8f * thrash) * this.bob);
        float down = 0.0f;
        if (i == 4) {
            float rear = this.rear();
            y += 0.055f * rear;
            x += 0.22f * rear * (float)Math.sin(1.4 * (double)clock);
            z += 0.15f * rear * (float)Math.cos(1.1 * (double)clock);
        }
        if (!this.steady) {
            if (i == 4) {
                down += 0.95f * this.sinkHead;
                float flee = this.fleeing();
                x += 4.6f * flee * flee;
                down += 0.55f * flee * flee;
            }
            down += 0.95f * this.dive(i);
            float k = this.dodge();
            if (k != 0.0f) {
                this.dodgeInto(k);
                x += this.dodgeX[i];
                z += this.dodgeZ[i];
                down += this.dodgeDown[i];
            }
        }
        this.px = x;
        this.py = y - down;
        this.pz = z;
        this.under = down;
    }

    private float yaw(int i, float clock) {
        float k;
        int j = 4 - i;
        float head = -1.2f + 0.4f * clock;
        float x = head - (float)j * 0.64f;
        float amp = 0.3f * (1.0f + 0.9f * this.thrash());
        float slope = amp * 2.0f * (float)Math.cos(2.0f * x);
        float yaw = 90.0f - (float)Math.toDegrees(Math.atan(slope));
        if (i == 4) {
            yaw = BoaScene.lerp(yaw, 16.0f * (float)Math.sin(1.6 * (double)clock), this.rear());
            yaw -= 34.0f * this.fleeing();
        }
        if ((k = this.dodge()) != 0.0f) {
            float w = 1.0f / (1.0f + 0.8f * (float)Math.abs(i - this.dodgeTarget()));
            float turn = switch (this.dodgeStyle()) {
                case 2 -> 40.0f;
                case 4 -> 26.0f;
                case 3 -> -20.0f;
                case 1 -> 12.0f;
                default -> 8.0f;
            };
            yaw += Math.signum(this.dodgeSide()) * turn * w * k;
        }
        return yaw;
    }

    private float thrash() {
        float sum = 0.0f;
        for (BoaDirector.Event e : this.d.events()) {
            float since;
            if (e.kind() != BoaDirector.Kind.EVAPORATE || !((since = this.t - e.time()) >= 0.0f)) continue;
            sum += (float)Math.exp(-since * 2.2f);
        }
        return Math.min(1.0f, sum);
    }

    private void dodgeInto(float k) {
        int i;
        int target = Math.max(0, this.dodgeTarget());
        int style = this.dodgeStyle();
        float side = Math.signum(this.dodgeSide());
        float reach = this.dodgeReach() * k;
        float follow = side >= 0.0f ? 0.18f : 2.4f;
        for (i = 0; i < 5; ++i) {
            float dist = Math.abs(i - target);
            float x = 0.0f;
            float z = 0.0f;
            float down = 0.0f;
            switch (style) {
                case 1: {
                    float w = 1.0f / (1.0f + 3.0f * dist);
                    down = 0.72f * reach * w;
                    x = 0.25f * reach * w;
                    break;
                }
                case 2: {
                    float w = 1.0f / (1.0f + follow * dist);
                    float bend = 1.0f / (1.0f + 0.5f * dist);
                    x = side * 1.0f * reach * w;
                    z = side * 1.2f * reach * bend;
                    break;
                }
                case 3: {
                    float w = 1.0f / (1.0f + 2.4f * dist);
                    float bend = 1.0f / (1.0f + 0.8f * dist);
                    x = -1.0f * reach * w;
                    down = 0.3f * reach * w;
                    z = side * 0.35f * reach * bend;
                    break;
                }
                case 4: {
                    float w = 1.0f / (1.0f + follow * dist);
                    float bend = 1.0f / (1.0f + 0.45f * dist);
                    float wave = (float)Math.cos((float)(i - target) * 1.7f);
                    x = side * 0.95f * reach * w;
                    z = side * 1.1f * reach * bend * wave;
                    down = 0.22f * reach * bend * (i == target ? 1.0f : 0.3f);
                    break;
                }
                default: {
                    float w = Math.max(0.45f, 1.0f - 0.11f * dist);
                    x = 1.05f * reach * w;
                    z = side * 0.12f * reach * w;
                    if (i != target) break;
                    down = 0.12f * reach / 0.7f;
                }
            }
            this.dodgeX[i] = x;
            this.dodgeZ[i] = z;
            this.dodgeDown[i] = down;
        }
        for (i = target + 1; i < 5; ++i) {
            this.dodgeX[i] = this.dodgeX[i - 1] + BoaScene.gap(this.dodgeX[i] - this.dodgeX[i - 1]);
        }
        for (i = target - 1; i >= 0; --i) {
            this.dodgeX[i] = this.dodgeX[i + 1] - BoaScene.gap(this.dodgeX[i + 1] - this.dodgeX[i]);
        }
    }

    private static float gap(float wanted) {
        float limit = wanted >= 0.0f ? 0.55f : 0.1f;
        return limit * (float)Math.tanh(wanted / limit);
    }

    private float dodge() {
        if (this.d.phase() == BoaDirector.Phase.FIRE) {
            if (this.d.plannedHit()) {
                return 0.0f;
            }
            int seed = this.d.plannedSeed();
            float lead = Math.min(DODGE_LEAD[this.dodgeStyle()] * (0.75f + 0.5f * BoaScene.hash(seed, 37)) / this.d.shotPace(), 0.85f * this.d.phaseLength());
            float until = this.d.phaseLength() - this.d.phaseTime();
            if (until >= lead || lead <= 0.0f) {
                return 0.0f;
            }
            return 0.9f * BoaScene.easeOut(1.0f - until / lead);
        }
        BoaDirector.Event miss = this.lastMiss();
        if (miss == null) {
            return 0.0f;
        }
        float since = (this.t - miss.time()) * this.d.shotPace();
        float hold = 0.14f * (0.7f + 0.6f * BoaScene.hash(miss.seed(), 41));
        float back = 0.95f * (0.8f + 0.4f * BoaScene.hash(miss.seed(), 43));
        if (since < 0.0f || since > hold + back) {
            return 0.0f;
        }
        return since < hold ? BoaScene.lerp(0.9f, 1.0f, BoaScene.easeOut(since / hold)) : 1.0f - BoaScene.easeInOut((since - hold) / back);
    }

    private int dodgeStyle() {
        int seed = this.d.phase() == BoaDirector.Phase.FIRE ? this.d.plannedSeed() : (this.lastMiss() == null ? 0 : this.lastMiss().seed());
        return Math.min(4, (int)(BoaScene.hash(seed, 91) * 5.0f));
    }

    private float dodgeSide() {
        BoaDirector.Event miss = this.lastMiss();
        return this.d.phase() == BoaDirector.Phase.FIRE ? this.d.plannedSide() : (miss == null ? 1.0f : miss.side());
    }

    private float dodgeReach() {
        BoaDirector.Event miss = this.lastMiss();
        boolean near = this.d.phase() == BoaDirector.Phase.FIRE ? this.d.plannedNear() : miss != null && miss.near();
        float side = this.dodgeSide();
        float k = (Math.abs(side) - 0.55f) / 0.45f;
        return near ? 0.52f + 0.04f * k : 0.7f + 0.08f * k;
    }

    private int dodgeTarget() {
        if (this.d.phase() == BoaDirector.Phase.FIRE) {
            return Math.min(4, this.d.run().hits());
        }
        BoaDirector.Event miss = this.lastMiss();
        return miss == null ? -1 : miss.segment();
    }

    private float fleeing() {
        BoaDirector.Event pop = this.last(BoaDirector.Kind.POP);
        BoaPrize shown = this.d.shown();
        if (pop == null || !shown.dropped() || shown.inHead()) {
            return 0.0f;
        }
        return BoaScene.clamp01((this.t - pop.time()) * this.d.endPace() / 0.9f);
    }

    private float rear() {
        if (this.d.run().hits() < 4) {
            return 0.0f;
        }
        BoaDirector.Event empty = this.fork();
        return empty == null || empty.near() ? 0.0f : BoaScene.easeInOut(BoaScene.clamp01((this.t - empty.time() - 0.08f) / 0.6f));
    }

    private float dive(int i) {
        BoaDirector.Event dive = this.last(BoaDirector.Kind.DIVE);
        if (dive == null) {
            return 0.0f;
        }
        return BoaScene.easeInOut(BoaScene.clamp01((this.t - dive.time() - this.diveLag(dive, i)) / this.diveTime(dive)));
    }

    private float diveTime(BoaDirector.Event dive) {
        return (dive.near() ? 0.55f : 1.1f) / this.d.endPace();
    }

    private float diveLag(BoaDirector.Event dive, int i) {
        float lag = dive.near() ? 0.0935f : 0.17f;
        return (float)(4 - i) * lag / this.d.endPace();
    }

    private float burst() {
        return 0.16f / this.d.endPace();
    }

    private void placeSnake() {
        int i;
        BoaRun run = this.d.run();
        this.outline = Math.max(1.1f, this.unit * 0.035f);
        for (int i2 = 0; i2 < 5; ++i2) {
            this.segDrawn[i2] = i2 >= run.hits();
            this.segFlash[i2] = 0.0f;
            this.segSwell[i2] = 1.0f;
            this.segFade[i2] = 1.0f;
            this.lineN[i2] = 0;
        }
        for (BoaDirector.Event e : this.d.events()) {
            int i3 = e.segment();
            if (i3 < 0 || i3 >= 5) continue;
            if (e.kind() == BoaDirector.Kind.HIT) {
                BoaDirector.Event gone = this.evaporation(i3);
                float since = this.t - e.time();
                if (gone == null || this.t < gone.time()) {
                    this.segDrawn[i3] = true;
                    float stop = gone == null ? 0.08f : Math.max(0.04f, gone.time() - e.time());
                    this.segFlash[i3] = 0.4f + 0.55f * BoaScene.easeOut(BoaScene.clamp01(since / stop));
                    continue;
                }
                if (!(this.t < gone.time() + 0.16f)) continue;
                this.segDrawn[i3] = true;
                float k = BoaScene.clamp01((this.t - gone.time()) / 0.16f);
                this.segFlash[i3] = 0.95f;
                this.segFade[i3] = 1.0f - k;
                this.segSwell[i3] = 1.0f + 0.1f * BoaScene.easeOut(k);
                continue;
            }
            if (e.kind() != BoaDirector.Kind.EXPLODE) continue;
            float since = this.t - e.time();
            if (since < this.burst()) {
                this.segDrawn[i3] = true;
                float k = BoaScene.clamp01(since / this.burst());
                this.segFlash[i3] = 0.35f + 0.6f * k;
                this.segSwell[i3] = 1.0f + 0.38f * k;
                continue;
            }
            this.segDrawn[i3] = false;
        }
        for (i = 0; i < 5; ++i) {
            if (!this.segDrawn[i]) continue;
            this.segment(i, this.st);
            if (this.under > 0.874f) {
                this.segDrawn[i] = false;
                continue;
            }
            this.segX[i] = this.px;
            this.segY[i] = this.py;
            this.segZ[i] = this.pz;
            TikiModel block = i == 4 ? this.art.head() : this.art.body();
            float top = this.py + (block.bottomY() - block.centreY() + block.height()) * this.segSwell[i];
            this.segShow[i] = BoaScene.clamp01(top / 0.035f);
            this.segYaw[i] = this.yaw(i, this.st);
            this.segSink[i] = 0.5f - 0.5f * this.bob;
            this.segRing[i] = (1.0f - BoaScene.clamp01(this.under / 0.6f)) * this.segFade[i];
        }
        for (i = 0; i < 5; ++i) {
            if (this.segDrawn[i]) {
                this.waterline(i);
            }
            this.hole(i);
        }
    }

    private void snake() {
        this.faces.clear();
        this.pool.reset();
        float dx = this.lensX - this.aimX;
        float dy = this.lensY - this.aimY;
        this.surface = 0;
        this.wakes();
        this.surface = 1;
        for (int i = 0; i < 5; ++i) {
            if (!this.segDrawn[i] || this.segShow[i] <= 0.004f) continue;
            TikiModel model = i == 4 ? this.art.head() : this.art.body();
            float s = this.segSwell[i];
            float lift = (this.segY[i] - this.camY) / s - model.centreY();
            ModelProjection.project(model, false, this.segYaw[i], -7.0f, this.unit * s, (this.segX[i] - this.camX) / s, lift, (this.segZ[i] - this.camZ) / s, i, this.faces, this.pool);
        }
        this.faces.sort(BACK_TO_FRONT);
        this.inkFaces(this.outline, dx, dy, null);
        for (ModelProjection.Face f : this.faces) {
            if (BoaScene.culled(f)) continue;
            int colour = BoaScene.lerpColor(this.bd.cool, this.bd.warm, f.tone * f.tone);
            if (f.interior) {
                colour = BoaScene.lerpColor(colour, this.bd.cool, 0.5f);
            }
            float faded = f.tier < 5 ? this.segFade[f.tier] * this.segShow[f.tier] : 1.0f;
            float flash = f.tier < 5 ? this.segFlash[f.tier] : 0.0f;
            float bright = f.brightness;
            if (flash > 0.0f) {
                colour = BoaScene.lerpColor(colour, -1957094, Math.min(1.0f, flash * 1.15f));
                bright *= 1.0f + 0.55f * flash;
            }
            this.face(f, this.texFor(f), BoaScene.withAlpha(BoaScene.shade(colour, bright), faded), dx, dy);
        }
        this.wakes();
    }

    private BoaTex texFor(ModelProjection.Face f) {
        if (f.tier >= 10) {
            return BoaTex.skinOf(this.d.shown());
        }
        return f.tier == 4 ? BoaTex.HEAD : BoaTex.BODY;
    }

    private BoaDirector.Event evaporation(int segment) {
        for (BoaDirector.Event e : this.d.events()) {
            if (e.kind() != BoaDirector.Kind.EVAPORATE || e.segment() != segment) continue;
            return e;
        }
        return null;
    }

    private void waterline(int i) {
        TikiModel model = i == 4 ? this.art.head() : this.art.body();
        float s = this.segSwell[i];
        float hx = model.halfX() * s;
        float hz = model.halfZ() * s;
        float bottom = this.segY[i] + (model.bottomY() - model.centreY()) * s;
        float top = bottom + model.height() * s;
        float level = Math.max(bottom, Math.min(0.0f, top));
        double yaw = Math.toRadians(this.segYaw[i]);
        float c = (float)Math.cos(yaw);
        float sn = (float)Math.sin(yaw);
        for (int k = 0; k < 4; ++k) {
            float ma = k == 0 || k == 3 ? -hx : hx;
            float mb = k < 2 ? -hz : hz;
            this.cornerX[k] = this.screenX(this.segX[i] + ma * c + mb * sn);
            this.cornerY[k] = this.screenY(level, this.segZ[i] - ma * sn + mb * c);
        }
        int n = this.lowerEdge(this.cornerX, this.cornerY, 4, this.lineX[i], this.lineY[i]);
        if (n < 2) {
            this.lineN[i] = 0;
            return;
        }
        float[] fArray = this.lineX[i];
        fArray[0] = fArray[0] - this.outline;
        float[] fArray2 = this.lineX[i];
        int n2 = n - 1;
        fArray2[n2] = fArray2[n2] + this.outline;
        if (bottom >= 0.0f) {
            int k = 0;
            while (k < n) {
                float[] fArray3 = this.lineY[i];
                int n3 = k++;
                fArray3[n3] = fArray3[n3] + this.outline;
            }
        }
        this.lineN[i] = n;
        this.lineUnder[i] = top < 0.0f;
        float drop = (level - bottom) * this.cp * this.unit;
        this.clipCount[i] = 0;
        if (drop > 0.0f) {
            int m = 0;
            int k = 0;
            while (k + 1 < n) {
                float x0 = this.lineX[i][k];
                float y0 = this.lineY[i][k];
                float x1 = this.lineX[i][k + 1];
                float y1 = this.lineY[i][k + 1];
                if (!(x1 - x0 < 0.001f)) {
                    this.clipA[i][m] = y1 - y0;
                    this.clipB[i][m] = x0 - x1;
                    this.clipC[i][m] = -(this.clipA[i][m] * x0 + this.clipB[i][m] * y0);
                    ++m;
                }
                ++k;
            }
            this.clipCount[i] = m;
        }
    }

    private int lowerEdge(float[] xs, float[] ys, int n, float[] outX, float[] outY) {
        this.sortCorners(xs, ys, n);
        int m = 0;
        for (int k = 0; k < n; ++k) {
            float x0;
            float y0;
            float onLine;
            int p = this.cornerOrder[k];
            if (m > 0 && xs[p] - outX[m - 1] < 0.001f) continue;
            while (m >= 2 && !(outY[m - 1] > (onLine = (y0 = outY[m - 2]) + (ys[p] - y0) * (outX[m - 1] - (x0 = outX[m - 2])) / (xs[p] - x0)) + 0.001f)) {
                --m;
            }
            outX[m] = xs[p];
            outY[m] = ys[p];
            ++m;
        }
        return m;
    }

    private void sortCorners(float[] xs, float[] ys, int n) {
        for (int k = 0; k < n; ++k) {
            int p = k;
            int j;
            for (j = k - 1; j >= 0; --j) {
                int q = this.cornerOrder[j];
                boolean after = xs[q] > xs[p] || xs[q] == xs[p] && ys[q] < ys[p];
                if (!after) break;
                this.cornerOrder[j + 1] = q;
            }
            this.cornerOrder[j + 1] = p;
        }
    }

    private void onSurface(float[] xs, float[] ys, int n, int argb, int from) {
        if (n < 3) {
            return;
        }
        float minX = Float.MAX_VALUE;
        float maxX = -3.4028235E38f;
        for (int k = 0; k < n; ++k) {
            minX = Math.min(minX, xs[k]);
            maxX = Math.max(maxX, xs[k]);
        }
        for (int i = from; i < 5; ++i) {
            int m = this.lineN[i];
            if (m < 2 || this.lineUnder[i] || maxX <= this.lineX[i][0] || minX >= this.lineX[i][m - 1]) continue;
            float[] lx = this.lineX[i];
            float[] ly = this.lineY[i];
            int base = i * 4;
            int k = BoaScene.cut(xs, ys, n, -1.0f, 0.0f, lx[0], this.cutX[base], this.cutY[base]);
            this.onSurface(this.cutX[base], this.cutY[base], k, argb, i + 1);
            k = BoaScene.cut(xs, ys, n, 1.0f, 0.0f, -lx[m - 1], this.cutX[base], this.cutY[base]);
            this.onSurface(this.cutX[base], this.cutY[base], k, argb, i + 1);
            int e = 0;
            while (e + 1 < m) {
                float x0 = lx[e];
                float y0 = ly[e];
                float x1 = lx[e + 1];
                float y1 = ly[e + 1];
                if (!(x1 - x0 < 0.001f)) {
                    k = BoaScene.cut(xs, ys, n, 1.0f, 0.0f, -x0, this.cutX[base + 1], this.cutY[base + 1]);
                    if ((k = BoaScene.cut(this.cutX[base + 1], this.cutY[base + 1], k, -1.0f, 0.0f, x1, this.cutX[base + 2], this.cutY[base + 2])) != 0) {
                        float a = y0 - y1;
                        float b = x1 - x0;
                        float c = -(a * x0 + b * y0);
                        int front = BoaScene.cut(this.cutX[base + 2], this.cutY[base + 2], k, a, b, c, this.cutX[base + 3], this.cutY[base + 3]);
                        this.onSurface(this.cutX[base + 3], this.cutY[base + 3], front, argb, i + 1);
                        if (this.surface == 0) {
                            int back = BoaScene.cut(this.cutX[base + 2], this.cutY[base + 2], k, -a, -b, -c, this.cutX[base + 3], this.cutY[base + 3]);
                            this.shape(this.cutX[base + 3], this.cutY[base + 3], back, argb);
                        }
                    }
                }
                ++e;
            }
            return;
        }
        if (this.surface == 1) {
            this.shape(xs, ys, n, argb);
        }
    }

    private static int cut(float[] inX, float[] inY, int n, float a, float b, float c, float[] outX, float[] outY) {
        int m = 0;
        for (int k = 0; k < n && m < 23; ++k) {
            int j = (k + 1) % n;
            float dk = a * inX[k] + b * inY[k] + c;
            float dj = a * inX[j] + b * inY[j] + c;
            if (dk >= 0.0f) {
                outX[m] = inX[k];
                outY[m] = inY[k];
                ++m;
            }
            if (dk >= 0.0f == dj >= 0.0f) continue;
            float f = dk / (dk - dj);
            outX[m] = inX[k] + (inX[j] - inX[k]) * f;
            outY[m] = inY[k] + (inY[j] - inY[k]) * f;
            ++m;
        }
        return m < 3 ? 0 : m;
    }

    private void shape(float[] xs, float[] ys, int n, int argb) {
        int k = 1;
        while (k + 1 < n) {
            int last = Math.min(k + 2, n - 1);
            this.out.poly(xs[0], ys[0], xs[k], ys[k], xs[k + 1], ys[k + 1], xs[last], ys[last], argb);
            k += 2;
        }
    }

    private void streak(float x0, float y0, float x1, float y1, int argb) {
        this.out.fill(x0, y0, x1, y1, argb);
    }

    private void hole(int i) {
        int k;
        this.holeOpen[i] = false;
        if (!this.segDrawn[i] || this.segRing[i] <= 0.01f) {
            return;
        }
        float grow = this.wakeScale(i) * (0.96f + 0.04f * this.segSink[i]);
        float across = 0.52f * grow;
        float along = 0.475f * grow;
        double yaw = Math.toRadians(this.segYaw[i]);
        float c = (float)Math.cos(yaw);
        float sn = (float)Math.sin(yaw);
        float front = along;
        float back = along;
        for (int j = i - 1; j <= i + 1; j += 2) {
            if (j < 0 || j >= 5 || !this.segDrawn[j] || this.segRing[j] <= 0.01f) continue;
            float ahead = (this.segX[j] - this.segX[i]) * sn + (this.segZ[j] - this.segZ[i]) * c;
            float reach = along + (BoaScene.holeReach(along, Math.abs(ahead)) - along) * this.segRing[j];
            if (ahead > 0.0f) {
                front = Math.max(front, reach);
                continue;
            }
            back = Math.max(back, reach);
        }
        this.holeFront[i] = front;
        this.holeBack[i] = back;
        this.holeFrontPower[i] = BoaScene.holePower(front, along);
        this.holeBackPower[i] = BoaScene.holePower(back, along);
        float shrink = 0.635f * this.segRing[i];
        float ox = this.screenX(this.segX[i]);
        float oy = this.screenY(0.0f, this.segZ[i]);
        float[] xs = this.holeX[i];
        float[] ys = this.holeY[i];
        float left = Float.MAX_VALUE;
        float right = -3.4028235E38f;
        float top = Float.MAX_VALUE;
        float bottom = -3.4028235E38f;
        float gx = 0.0f;
        float gy = 0.0f;
        for (k = 0; k < 56; ++k) {
            double a = (double)k * 2.0 * Math.PI / 56.0;
            float r = shrink * this.holeRadius(i, a, across);
            float lx = (float)Math.cos(a) * r;
            float ly = (float)Math.sin(a) * r;
            xs[k] = ox + this.unit * (lx * c + ly * sn);
            ys[k] = oy + 0.32f * this.unit * (-lx * sn + ly * c);
            left = Math.min(left, xs[k]);
            right = Math.max(right, xs[k]);
            top = Math.min(top, ys[k]);
            bottom = Math.max(bottom, ys[k]);
            gx += xs[k];
            gy += ys[k];
        }
        gx /= 56.0f;
        gy /= 56.0f;
        for (k = 0; k < 56; ++k) {
            int j = (k + 1) % 56;
            float a = ys[k] - ys[j];
            float b = xs[j] - xs[k];
            float cc = -(a * xs[k] + b * ys[k]);
            if (a * gx + b * gy + cc < 0.0f) {
                a = -a;
                b = -b;
                cc = -cc;
            }
            this.holeA[i][k] = a;
            this.holeB[i][k] = b;
            this.holeC[i][k] = cc;
        }
        this.holeLeft[i] = left;
        this.holeRight[i] = right;
        this.holeTop[i] = top;
        this.holeBottom[i] = bottom;
        this.holeOpen[i] = right - left > 0.5f;
    }

    private float holeRadius(int i, double angle, float across) {
        boolean ahead = Math.sin(angle) >= 0.0;
        return BoaScene.superRadius(angle, across, ahead ? this.holeFront[i] : this.holeBack[i], ahead ? this.holeFrontPower[i] : this.holeBackPower[i]);
    }

    private static double holePower(float reach, float along) {
        float joined = BoaScene.clamp01((reach / along - 1.0f) / 0.2f);
        return 6.0 + -3.0 * (double)joined;
    }

    private static float holeReach(float along, float ahead) {
        float wanted = 0.6f * ahead / 0.635f;
        float stretch = wanted / along - 1.0f;
        if (stretch <= 0.0f) {
            return along;
        }
        if (stretch <= 0.45f) {
            return wanted;
        }
        return along * (1.0f + 0.45f * Math.max(0.0f, 2.0f - stretch / 0.45f));
    }

    private void wakes() {
        for (int band = 0; band <= 2; ++band) {
            for (int i = 0; i < 5; ++i) {
                if (!this.segDrawn[i]) continue;
                this.wake(i, band);
            }
        }
    }

    private void wake(int i, int band) {
        float alpha = this.segRing[i];
        if (alpha <= 0.01f) {
            return;
        }
        float sink = this.segSink[i];
        float grow = this.wakeScale(i) * (0.96f + 0.04f * sink);
        float across = 0.52f * grow;
        float along = 0.475f * grow;
        double yaw = Math.toRadians(this.segYaw[i]);
        float c = (float)Math.cos(yaw);
        float sn = (float)Math.sin(yaw);
        float ox = this.screenX(this.segX[i]);
        float oy = this.screenY(0.0f, this.segZ[i]);
        int argb = switch (band) {
            case 2 -> BoaScene.lerpColor(this.bd.waterDeep, -1, 0.9f + 0.1f * sink);
            case 1 -> BoaScene.lerpColor(this.bd.waterDeep, BoaScene.lerpColor(-2757398, -1378571, sink), 0.8f + 0.15f * sink);
            default -> BoaScene.lerpColor(this.bd.waterDeep, -6304824, 0.8f);
        };
        argb = BoaScene.withAlpha(argb, alpha);
        float swirl = this.t * 0.9f + (float)i * 1.7f;
        block8: for (int k = 0; k < 56; ++k) {
            double a0 = (double)k * 2.0 * Math.PI / 56.0;
            double a1 = (double)(k + 1) * 2.0 * Math.PI / 56.0;
            float hole0 = this.holeRadius(i, a0, across);
            float hole1 = this.holeRadius(i, a1, across);
            float round0 = BoaScene.superRadius(a0, across, along, 2.8);
            float round1 = BoaScene.superRadius(a1, across, along, 2.8);
            float out0 = round0 * (0.955f + BoaScene.wave(a0, swirl));
            float out1 = round1 * (0.955f + BoaScene.wave(a1, swirl));
            float l0 = 0.705f * hole0;
            float l1 = 0.705f * hole1;
            float f0 = Math.max(l0, out0 - 0.07f * round0);
            float f1 = Math.max(l1, out1 - 0.07f * round1);
            switch (band) {
                case 2: {
                    this.foamPiece(a0, a1, 0.635f * hole0, 0.635f * hole1, l0, l1, i, c, sn, ox, oy, argb);
                    continue block8;
                }
                case 1: {
                    this.foamPiece(a0, a1, l0, l1, f0, f1, i, c, sn, ox, oy, argb);
                    continue block8;
                }
                default: {
                    this.foamPiece(a0, a1, f0, f1, out0, out1, i, c, sn, ox, oy, argb);
                }
            }
        }
    }

    private static float wave(double angle, float swirl) {
        return 0.018f * (float)Math.sin(angle * 7.0 + (double)swirl) + 0.01f * (float)Math.sin(angle * 4.0 - (double)swirl * 0.7);
    }

    private void foamPiece(double a0, double a1, float in0, float in1, float out0, float out1, int i, float c, float sn, float ox, float oy, int argb) {
        if (out0 <= in0 && out1 <= in1) {
            return;
        }
        float ca0 = (float)Math.cos(a0);
        float sa0 = (float)Math.sin(a0);
        float ca1 = (float)Math.cos(a1);
        float sa1 = (float)Math.sin(a1);
        this.localX[0] = ca0 * in0;
        this.localY[0] = sa0 * in0;
        this.localX[1] = ca0 * out0;
        this.localY[1] = sa0 * out0;
        this.localX[2] = ca1 * out1;
        this.localY[2] = sa1 * out1;
        this.localX[3] = ca1 * in1;
        this.localY[3] = sa1 * in1;
        int n = 4;
        for (int k = 0; k < n; ++k) {
            float across = this.localX[k];
            float along = this.localY[k];
            this.pieceX[k] = ox + this.unit * (across * c + along * sn);
            this.pieceY[k] = oy + 0.32f * this.unit * (-across * sn + along * c);
        }
        this.outsideHoles(this.pieceX, this.pieceY, n, argb, i, 0);
    }

    private void outsideHoles(float[] xs, float[] ys, int n, int argb, int owner, int from) {
        for (int h = from; h < 5; ++h) {
            if (h == owner || !this.holeOpen[h]) continue;
            float left = Float.MAX_VALUE;
            float right = -3.4028235E38f;
            float top = Float.MAX_VALUE;
            float bottom = -3.4028235E38f;
            for (int k = 0; k < n; ++k) {
                left = Math.min(left, xs[k]);
                right = Math.max(right, xs[k]);
                top = Math.min(top, ys[k]);
                bottom = Math.max(bottom, ys[k]);
            }
            if (right <= this.holeLeft[h] || left >= this.holeRight[h] || bottom <= this.holeTop[h] || top >= this.holeBottom[h]) continue;
            float[] restX = this.clearX[h];
            float[] restY = this.clearY[h];
            System.arraycopy(xs, 0, restX, 0, n);
            System.arraycopy(ys, 0, restY, 0, n);
            int rest = n;
            for (int e = 0; e < 56 && rest >= 3; ++e) {
                float a = this.holeA[h][e];
                float b = this.holeB[h][e];
                float c = this.holeC[h][e];
                boolean anyOut = false;
                boolean anyIn = false;
                for (int k = 0; k < rest; ++k) {
                    if (a * restX[k] + b * restY[k] + c < 0.0f) {
                        anyOut = true;
                        continue;
                    }
                    anyIn = true;
                }
                if (!anyOut) continue;
                if (!anyIn) {
                    this.outsideHoles(restX, restY, rest, argb, owner, h + 1);
                    return;
                }
                int kept = BoaScene.cut(restX, restY, rest, -a, -b, -c, this.keptX[h], this.keptY[h]);
                this.outsideHoles(this.keptX[h], this.keptY[h], kept, argb, owner, h + 1);
                rest = BoaScene.cut(restX, restY, rest, a, b, c, this.keptX[h], this.keptY[h]);
                System.arraycopy(this.keptX[h], 0, restX, 0, rest);
                System.arraycopy(this.keptY[h], 0, restY, 0, rest);
            }
            return;
        }
        this.onSurface(xs, ys, n, argb, 0);
    }

    private static float superRadius(double angle, float across, float along, double power) {
        double x = Math.abs(Math.cos(angle)) / (double)across;
        double y = Math.abs(Math.sin(angle)) / (double)along;
        return (float)(1.0 / Math.pow(Math.pow(x, power) + Math.pow(y, power), 1.0 / power));
    }

    private float wakeScale(int i) {
        return i == 4 ? this.art.head().radius() / Math.max(0.01f, this.art.body().radius()) : 1.0f;
    }

    private static boolean culled(ModelProjection.Face f) {
        return f.ux * f.vy - f.uy * f.vx < 0.0f;
    }

    private void face(ModelProjection.Face f, BoaTex tex, int argb, float dx, float dy) {
        float ux = f.ux * 1.01f;
        float uy = f.uy * 1.01f;
        float vx = f.vx * 1.01f;
        float vy = f.vy * 1.01f;
        float ox = f.ox - (ux - f.ux) * 0.5f - (vx - f.vx) * 0.5f + dx;
        float oy = f.oy - (uy - f.uy) * 0.5f - (vy - f.vy) * 0.5f + dy;
        this.quad(f.tier, tex, f.u0, f.v0, f.uw, f.vh, ux / (float)f.uw, uy / (float)f.uw, vx / (float)f.vh, vy / (float)f.vh, ox, oy, argb);
    }

    private void quad(int tier, BoaTex tex, int u0, int v0, int uw, int vh, float m00, float m01, float m10, float m11, float m20, float m21, int argb) {
        int k;
        int lines;
        int n = lines = tier >= 0 && tier < 5 ? this.clipCount[tier] : 0;
        if (lines == 0) {
            this.out.quad(tex, u0, v0, uw, vh, m00, m01, m10, m11, m20, m21, argb);
            return;
        }
        float ux = m00 * (float)uw;
        float uy = m01 * (float)uw;
        float vx = m10 * (float)vh;
        float vy = m11 * (float)vh;
        this.cutPX[0] = m20;
        this.cutPY[0] = m21;
        this.cutPX[1] = m20 + ux;
        this.cutPY[1] = m21 + uy;
        this.cutPX[2] = m20 + ux + vx;
        this.cutPY[2] = m21 + uy + vy;
        this.cutPX[3] = m20 + vx;
        this.cutPY[3] = m21 + vy;
        if (this.above(tier, lines, this.cutPX, this.cutPY, 4)) {
            this.out.quad(tex, u0, v0, uw, vh, m00, m01, m10, m11, m20, m21, argb);
            return;
        }
        if (ux * vy - uy * vx < 0.0f) {
            return;
        }
        int n2 = 4;
        float[] inX = this.cutPX;
        float[] inY = this.cutPY;
        float[] toX = this.cutQX;
        float[] toY = this.cutQY;
        for (k = 0; k < lines && n2 >= 3; ++k) {
            n2 = BoaScene.cut(inX, inY, n2, this.clipA[tier][k], this.clipB[tier][k], this.clipC[tier][k], toX, toY);
            float[] swapX = inX;
            float[] swapY = inY;
            inX = toX;
            inY = toY;
            toX = swapX;
            toY = swapY;
        }
        k = 1;
        while (k + 1 < n2) {
            int last = Math.min(k + 2, n2 - 1);
            this.out.texPoly(tex, u0, v0, uw, vh, m00, m01, m10, m11, m20, m21, inX[0], inY[0], inX[k], inY[k], inX[k + 1], inY[k + 1], inX[last], inY[last], argb);
            k += 2;
        }
    }

    private boolean above(int tier, int lines, float[] xs, float[] ys, int n) {
        for (int k = 0; k < lines; ++k) {
            float a = this.clipA[tier][k];
            float b = this.clipB[tier][k];
            float c = this.clipC[tier][k];
            for (int i = 0; i < n; ++i) {
                if (!(a * xs[i] + b * ys[i] + c < 0.0f)) continue;
                return false;
            }
        }
        return true;
    }

    private void poly(int tier, float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int argb) {
        int lines;
        int n = lines = tier >= 0 && tier < 5 ? this.clipCount[tier] : 0;
        if (lines == 0) {
            this.out.poly(ax, ay, bx, by, cx, cy, dx, dy, argb);
            return;
        }
        this.cutPX[0] = ax;
        this.cutPY[0] = ay;
        this.cutPX[1] = bx;
        this.cutPY[1] = by;
        this.cutPX[2] = cx;
        this.cutPY[2] = cy;
        this.cutPX[3] = dx;
        this.cutPY[3] = dy;
        int n2 = 4;
        float[] inX = this.cutPX;
        float[] inY = this.cutPY;
        float[] toX = this.cutQX;
        float[] toY = this.cutQY;
        for (int k = 0; k < lines && n2 >= 3; ++k) {
            n2 = BoaScene.cut(inX, inY, n2, this.clipA[tier][k], this.clipB[tier][k], this.clipC[tier][k], toX, toY);
            float[] swapX = inX;
            float[] swapY = inY;
            inX = toX;
            inY = toY;
            toX = swapX;
            toY = swapY;
        }
        this.shape(inX, inY, n2, argb);
    }

    private void inkFaces(float radius, float dx, float dy, BoaTex itemSkin) {
        float coarse = 2.0f * radius * (float)Math.sin(0.39269908169872414);
        float fine = 2.0f * radius * (float)Math.sin(0.19634954084936207);
        int n = this.faces.size();
        if (this.texelPitch.length < n) {
            this.texelPitch = new float[n * 2];
        }
        for (int k = 0; k < n; ++k) {
            this.texelPitch[k] = BoaScene.culled(this.faces.get(k)) ? -1.0f : BoaScene.texelPitch(this.faces.get(k));
        }
        for (int i = 0; i < 16; ++i) {
            boolean between = i % 2 == 1;
            double a = (double)i * 2.0 * Math.PI / 16.0;
            float ox = dx + (float)Math.cos(a) * radius;
            float oy = dy + (float)Math.sin(a) * radius;
            for (int k = 0; k < n; ++k) {
                ModelProjection.Face f = this.faces.get(k);
                float pitch = this.texelPitch[k];
                if (pitch < 0.0f || between && pitch >= coarse * 1.25f) continue;
                float faded = f.tier < 5 ? this.segFade[f.tier] * this.segShow[f.tier] : 1.0f;
                BoaTex tex = itemSkin != null ? itemSkin : this.texFor(f);
                this.face(f, tex, BoaScene.withAlpha(-16315383, faded), ox, oy);
                float band = 1.0f - BoaScene.smoothstep(fine * 0.6f, fine * 1.3f, pitch);
                if (!(band > 0.004f)) continue;
                this.inkBand(f, tex, fine * 1.1f, faded * band, ox, oy);
            }
        }
    }

    private static float texelPitch(ModelProjection.Face f) {
        float det = Math.abs(f.ux * f.vy - f.uy * f.vx);
        float u = (float)Math.hypot(f.ux, f.uy);
        float v = (float)Math.hypot(f.vx, f.vy);
        return Math.min(det / Math.max(1.0E-4f, v) / (float)Math.max(1, f.uw), det / Math.max(1.0E-4f, u) / (float)Math.max(1, f.vh));
    }

    private static float smoothstep(float from, float to, float value) {
        float k = BoaScene.clamp01((value - from) / Math.max(1.0E-4f, to - from));
        return k * k * (3.0f - 2.0f * k);
    }

    private void inkBand(ModelProjection.Face f, BoaTex tex, float thickness, float alpha, float dx, float dy) {
        if (f.ux * f.vy - f.uy * f.vx < 0.0f) {
            return;
        }
        float[] box = this.paintedBox(tex, f);
        if (box == null) {
            return;
        }
        float ux = f.ux * 1.01f;
        float uy = f.uy * 1.01f;
        float vx = f.vx * 1.01f;
        float vy = f.vy * 1.01f;
        float ox = f.ox - (ux - f.ux) * 0.5f - (vx - f.vx) * 0.5f + dx;
        float oy = f.oy - (uy - f.uy) * 0.5f - (vy - f.vy) * 0.5f + dy;
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
        this.poly(f.tier, bx, by, bx + ax, by + ay, bx + ax + cx, by + ay + cy, bx + cx, by + cy, BoaScene.withAlpha(-16315383, alpha * box[4]));
    }

    private float[] paintedBox(BoaTex tex, ModelProjection.Face f) {
        long key;
        BufferedImage image = this.art.image(tex);
        if (image == null) {
            return null;
        }
        Map boxes = this.painted.computeIfAbsent(tex, k -> new HashMap());
        float[] box = (float[])boxes.get(key = (long)(f.u0 & 0xFFFF) << 48 | (long)(f.v0 & 0xFFFF) << 32 | (long)(f.uw & 0xFFFF) << 16 | (long)(f.vh & 0xFFFF));
        if (box == null) {
            box = BoaScene.measurePainted(image, f.u0, f.v0, f.uw, f.vh);
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

    private void effectsBehind() {
        this.surface = 0;
        this.surfaceEffects();
        this.surface = 1;
    }

    private void effectsInFront() {
        this.surface = 1;
        this.surfaceEffects();
    }

    private void surfaceEffects() {
        for (BoaDirector.Event e : this.d.events()) {
            float since = this.t - e.time();
            if (since < 0.0f) continue;
            if (e.kind() == BoaDirector.Kind.MISS) {
                this.splash(e, since);
                continue;
            }
            if (e.kind() != BoaDirector.Kind.DIVE) continue;
            this.diveRipples(e, since);
        }
    }

    private void effects() {
        block9: for (BoaDirector.Event e : this.d.events()) {
            float since = this.t - e.time();
            if (since < 0.0f) continue;
            switch (e.kind()) {
                case SHOT: {
                    float f = e.near() ? 0.16f : 0.1f;
                    float flash = f;
                    if (!(since < flash)) continue block9;
                    this.out.fill(this.lensX - this.lensR, this.lensY - this.lensR, this.lensX + this.lensR, this.lensY + this.lensR, BoaScene.withAlpha(-2856, (e.near() ? 0.7f : 0.5f) * (1.0f - since / flash)));
                    break;
                }
                case HIT: {
                    this.impact(e, since);
                    break;
                }
                case EVAPORATE: {
                    this.evaporate(e, since);
                    break;
                }
                case EMPTY: {
                    if (!(since < 1.9f)) break;
                    this.segmentAt(e.time(), e.segment());
                    this.smoke(this.px, this.py, this.pz, e.seed(), since, 0, 6, 1.7f, -4603200, 0.0f);
                    break;
                }
                case GLINT: {
                    if (!(since < 0.45f)) break;
                    this.segmentAt(e.time(), e.segment());
                    float tw = (float)Math.sin((double)(since / 0.45f) * Math.PI);
                    float s = this.unit * 0.7f * tw;
                    this.out.sprite(BoaTex.SPARK, 32, 32, this.screenX(this.px), this.screenY(this.py + 0.1f, this.pz), s, s, since * 3.0f, BoaScene.withAlpha(-1, tw));
                    break;
                }
                case POP: {
                    if (!(since < 0.6f)) break;
                    this.segmentAt(e.time(), e.segment());
                    float k = since / 0.6f;
                    float ring = this.unit * (0.4f + 2.6f * BoaScene.easeOut(k));
                    int tint = this.art.tint(this.d.fourthItem());
                    this.out.sprite(BoaTex.RING, 256, 256, this.screenX(this.px), this.screenY(this.py, this.pz), ring, ring, 0.0f, BoaScene.withAlpha(tint, 1.0f - k));
                    this.sparks(this.screenX(this.px), this.screenY(this.py, this.pz), e.seed(), 10, this.unit * 2.2f, since, 0.6f, tint);
                    break;
                }
                case EXPLODE: {
                    this.explode(e, since);
                    break;
                }
            }
        }
    }

    private void diveRipples(BoaDirector.Event e, float since) {
        for (int i = this.d.run().hits(); i < 5; ++i) {
            float at = this.diveLag(e, i) + 0.3f * this.diveTime(e);
            float s = since - at;
            if (s < 0.0f || s > 1.8f) continue;
            this.segmentAt(e.time() + at, i);
            float x = this.px;
            float z = this.pz;
            if (i == 4) {
                this.waterSplash(x, z, e.seed() + i, s, e.near() ? 0.85f : 0.55f, 1);
            }
            for (int ring = 0; ring < 2; ++ring) {
                float k = BoaScene.clamp01((s - (float)ring * 0.3f) / 1.5f);
                if (k <= 0.0f || k >= 1.0f) continue;
                float rw = (0.5f + 1.3f * BoaScene.easeOut(k)) * (i == 4 ? 1.0f : 0.8f);
                float a = (1.0f - k) * (1.0f - k) * (ring == 0 ? 0.6f : 0.38f);
                this.ripple(x, z, rw, BoaScene.withAlpha(-1, a));
            }
        }
    }

    private void ripple(float x, float z, float width, int argb) {
        float r = 0.5f * width * this.unit;
        if (r < 0.5f || argb >>> 24 < 2) {
            return;
        }
        float ox = this.screenX(x);
        float oy = this.screenY(0.0f, z);
        int bright = argb;
        int soft = argb & 0xFF000000 | 0xBFE0DE;
        for (int k = 0; k < 56; ++k) {
            double a0 = (double)k * 2.0 * Math.PI / 56.0;
            double a1 = (double)(k + 1) * 2.0 * Math.PI / 56.0;
            float in0 = r * (0.817f + 0.03f * (float)Math.sin(a0 * 8.0 + 1.1));
            float in1 = r * (0.817f + 0.03f * (float)Math.sin(a1 * 8.0 + 1.1));
            float mid0 = in0 + r * 0.075f;
            float mid1 = in1 + r * 0.075f;
            float out0 = r * (0.965f + 0.035f * (float)Math.sin(a0 * 11.0));
            float out1 = r * (0.965f + 0.035f * (float)Math.sin(a1 * 11.0));
            this.ripplePiece(ox, oy, a0, a1, in0, in1, mid0, mid1, bright);
            this.ripplePiece(ox, oy, a0, a1, mid0, mid1, out0, out1, soft);
        }
    }

    private void ripplePiece(float ox, float oy, double a0, double a1, float in0, float in1, float out0, float out1, int argb) {
        float ca0 = (float)Math.cos(a0);
        float sa0 = (float)Math.sin(a0) * 0.32f;
        float ca1 = (float)Math.cos(a1);
        float sa1 = (float)Math.sin(a1) * 0.32f;
        this.pieceX[0] = ox + ca0 * in0;
        this.pieceY[0] = oy + sa0 * in0;
        this.pieceX[1] = ox + ca0 * out0;
        this.pieceY[1] = oy + sa0 * out0;
        this.pieceX[2] = ox + ca1 * out1;
        this.pieceY[2] = oy + sa1 * out1;
        this.pieceX[3] = ox + ca1 * in1;
        this.pieceY[3] = oy + sa1 * in1;
        this.onSurface(this.pieceX, this.pieceY, 4, argb, 0);
    }

    private void segmentAt(float when, int segment) {
        float sceneWhen = this.d.sceneClockAt(when);
        this.segment(Math.max(0, Math.min(4, segment)), sceneWhen);
    }

    private void impact(BoaDirector.Event e, float since) {
        float f;
        if (since >= 0.3f) {
            return;
        }
        this.segmentAt(e.time(), e.segment());
        float sx = this.screenX(this.px);
        float sy = this.screenY(this.py, this.pz);
        float k = since / 0.3f;
        float stage = (float)Math.min(3, e.segment()) / 3.0f;
        float ring = this.unit * (0.3f + 1.6f * BoaScene.easeOut(k)) * (1.0f + 0.25f * (float)e.segment());
        this.out.sprite(BoaTex.RING, 256, 256, sx, sy, ring, ring, 0.0f, BoaScene.withAlpha(-30102, 1.0f - k));
        if (since < 0.16f) {
            f = since / 0.16f;
            float glow = this.unit * (0.9f + 0.7f * f) * (1.0f + 0.3f * stage);
            this.out.sprite(BoaTex.GLOW, 256, 256, sx, sy, glow, glow, 0.0f, BoaScene.withAlpha(-7504, 0.9f * (1.0f - f) * (1.0f - f)));
        }
        if (since < 0.2f) {
            f = since / 0.2f;
            int streaks = 8 + 2 * Math.round(3.0f * stage);
            for (int i = 0; i < streaks; ++i) {
                double a = (double)((float)i + 0.5f * BoaScene.hash(e.seed(), i + 300)) * Math.PI * 2.0 / (double)streaks;
                float reach = this.unit * (0.55f + 0.35f * BoaScene.hash(e.seed(), i + 320)) * (1.0f + 0.4f * stage);
                float r0 = reach * (0.15f + 0.85f * BoaScene.easeOut(f));
                float r1 = r0 + this.unit * 0.3f * (1.0f - f);
                float ca = (float)Math.cos(a);
                float sa = (float)Math.sin(a);
                this.out.line(sx + ca * r0, sy + sa * r0, sx + ca * r1, sy + sa * r1, Math.max(1.0f, this.unit * 0.04f * (1.0f - f)), BoaScene.withAlpha(BoaScene.lerpColor(-2864, -34246, f), 1.0f - f * f));
            }
        }
    }

    private void evaporate(BoaDirector.Event e, float since) {
        if (since > 2.0f) {
            return;
        }
        this.segmentAt(e.time(), e.segment());
        float ox = this.px;
        float oy = this.py;
        float oz = this.pz;
        BoaTex tex = e.segment() == 4 ? BoaTex.HEAD : BoaTex.BODY;
        int stage = Math.min(3, e.segment());
        this.smoke(ox, oy, oz, e.seed(), since, 5 + stage, 3 + stage, 1.4f, -1774870, 0.75f);
        this.debris(tex, ox, oy, oz, e.seed(), 16 + 5 * stage, 1.5f + 0.25f * (float)stage, since, 1.0f);
    }

    private void debris(BoaTex tex, float ox, float oy, float oz, int seed, int count, float speed, float since, float life) {
        float gravity = 5.2f;
        for (int s = 0; s < count; ++s) {
            float h0 = BoaScene.hash(seed, s * 5);
            float a = BoaScene.hash(seed, s * 5 + 1) * (float)Math.PI * 2.0f;
            float vy = speed * (0.55f + 1.0f * BoaScene.hash(seed, s * 5 + 2));
            float vh = speed * (0.3f + 0.7f * BoaScene.hash(seed, s * 5 + 3));
            float spin = (BoaScene.hash(seed, s * 5 + 4) - 0.5f) * 16.0f;
            float height = Math.max(0.02f, oy + 0.1f);
            float landAt = (vy + (float)Math.sqrt(vy * vy + 10.4f * height)) / 5.2f;
            float lifeS = life * (0.85f + 0.3f * h0);
            float x = ox + (float)Math.cos(a) * vh * Math.min(since, landAt);
            float z = oz + (float)Math.sin(a) * vh * 0.55f * Math.min(since, landAt);
            if (since < landAt) {
                if (since > lifeS) continue;
                float y = oy + vy * since - 2.6f * since * since;
                float size = this.unit * (0.07f + 0.07f * h0);
                float flip = h0 * 6.0f + since * (7.0f + 8.0f * BoaScene.hash(seed, s * 5 + 6));
                float roll = BoaScene.hash(seed, s * 5 + 7) * 6.0f + since * (5.0f + 6.0f * h0);
                float faceOn = Math.abs((float)Math.cos(flip));
                float w = size * Math.max(0.22f, faceOn);
                float h = size * Math.max(0.35f, Math.abs((float)Math.cos(roll)));
                float fade = 1.0f - BoaScene.clamp01((since - lifeS * 0.8f) / (lifeS * 0.2f));
                float rot = spin * since + a;
                float sx = this.screenX(x);
                float sy = this.screenY(y, z);
                float ink = Math.max(1.0f, size * 0.22f);
                this.out.region(BoaTex.WHITE, 0, 0, 4, 4, sx, sy, w + ink, h + ink, rot, BoaScene.withAlpha(-16315383, fade));
                int lit = BoaScene.shade(BoaScene.lerpColor(this.bd.cool, this.bd.warm, 0.35f + 0.65f * faceOn), 0.75f + 0.45f * faceOn);
                float hot = 1.0f - BoaScene.clamp01(since / 0.2f);
                lit = BoaScene.lerpColor(lit, -38342, 0.75f * hot);
                int u0 = 8 + s * 3 % 6;
                int v0 = 8 + s * 5 % 6;
                this.out.region(tex, u0, v0, 2, 2, sx, sy, w, h, rot, BoaScene.withAlpha(lit, fade));
                continue;
            }
            float after = since - landAt;
            if (after > 0.55f || landAt > lifeS) continue;
            float k = after / 0.55f;
            this.ripple(x, z, 0.1f + 0.34f * BoaScene.easeOut(k), BoaScene.withAlpha(-1, 0.75f * (1.0f - k) * (1.0f - k)));
            if (!(after < 0.22f)) continue;
            float up = 0.16f * (after / 0.22f) * (1.0f - after / 0.22f) * 4.0f;
            float drop = this.unit * 0.035f;
            this.out.sprite(BoaTex.DISC, 64, 64, this.screenX(x), this.screenY(up, z), drop, drop, 0.0f, BoaScene.withAlpha(-1378571, 1.0f - after / 0.22f));
        }
    }

    private void smoke(float x, float y, float z, int seed, float since, int burst, int column, float life, int tint, float heat) {
        int colour;
        float alpha;
        float rot;
        float size;
        int i;
        for (i = 0; i < column; ++i) {
            float start = 0.05f + (float)i * 0.085f;
            float lifeI = life * (0.8f + 0.35f * BoaScene.hash(seed, i + 40));
            float k = (since - start) / lifeI;
            if (k <= 0.0f || k >= 1.0f) continue;
            float drift = (BoaScene.hash(seed, i + 41) - 0.5f) * 0.4f - 0.3f * k;
            float wx = x + drift;
            float wy = y + 0.12f + 1.05f * BoaScene.easeOut(k) + 0.1f * (float)i / (float)Math.max(1, column);
            float grow = 0.3f + 0.72f * BoaScene.easeOut(k) * (0.85f + 0.3f * BoaScene.hash(seed, i + 42));
            size = this.unit * grow * (1.0f - 0.3f * BoaScene.clamp01((k - 0.65f) / 0.35f));
            rot = BoaScene.hash(seed, i + 43) * 6.2832f + (BoaScene.hash(seed, i + 44) - 0.5f) * 1.4f * k;
            alpha = BoaScene.clamp01(k * 7.0f) * (1.0f - BoaScene.easeInOut(BoaScene.clamp01((k - 0.5f) / 0.5f))) * 0.9f;
            colour = BoaScene.puffColour(tint, heat, k);
            this.out.sprite(BoaTex.smoke(i + seed), 128, 128, this.screenX(wx), this.screenY(wy, z), size, size, rot, BoaScene.withAlpha(colour, alpha));
        }
        for (i = 0; i < burst; ++i) {
            float lifeI = 0.6f + 0.3f * BoaScene.hash(seed, i + 60);
            float k = since / lifeI;
            if (k >= 1.0f) continue;
            double a = Math.PI * (-0.12 + 1.24 * (double)((float)i + 0.6f * BoaScene.hash(seed, i + 61)) / (double)burst);
            float reach = (0.42f + 0.28f * BoaScene.hash(seed, i + 62)) * BoaScene.easeOut(k);
            float wx = x + (float)Math.cos(a) * reach;
            float wy = Math.max(0.08f, y + (float)Math.sin(a) * reach * 0.7f) + 0.25f * k;
            size = this.unit * (0.18f + 0.36f * BoaScene.easeOut(k)) * (1.0f - 0.35f * BoaScene.clamp01((k - 0.6f) / 0.4f));
            rot = (float)a + k * 0.8f;
            alpha = BoaScene.clamp01(k * 12.0f) * (1.0f - BoaScene.easeInOut(BoaScene.clamp01((k - 0.35f) / 0.65f)));
            colour = BoaScene.puffColour(tint, heat, k * 0.7f);
            this.out.sprite(BoaTex.smoke(i * 3 + seed + 1), 128, 128, this.screenX(wx), this.screenY(wy, z), size, size, rot, BoaScene.withAlpha(colour, alpha));
        }
    }

    private static int puffColour(int tint, float heat, float k) {
        int hot = BoaScene.lerpColor(tint, -19846, heat);
        int cooled = BoaScene.lerpColor(hot, tint, BoaScene.clamp01(k * 3.0f));
        return BoaScene.shade(cooled, 1.0f - 0.22f * BoaScene.clamp01((k - 0.4f) / 0.6f));
    }

    private void sparks(float sx, float sy, int seed, int count, float reach, float since, float life, int tint) {
        float k = BoaScene.clamp01(since / life);
        for (int i = 0; i < count; ++i) {
            float a = BoaScene.hash(seed, i + 70) * (float)Math.PI * 2.0f;
            float r = reach * (0.3f + 0.7f * BoaScene.hash(seed, i + 90)) * BoaScene.easeOut(k);
            float s = this.unit * 0.28f * (1.0f - k);
            this.out.sprite(BoaTex.SPARK, 32, 32, sx + (float)Math.cos(a) * r, sy + (float)Math.sin(a) * r, s, s, a, BoaScene.withAlpha(tint, 1.0f - k));
        }
    }

    private void splash(BoaDirector.Event e, float since) {
        if (since > 1.45f) {
            return;
        }
        this.steady = true;
        this.segmentAt(e.time(), e.segment());
        this.steady = false;
        float x = this.px + (BoaScene.hash(e.seed(), 5) - 0.5f) * 0.06f;
        float z = this.pz + this.py * this.cp / this.sp;
        this.waterSplash(x, z, e.seed(), since, 1.0f, 0);
    }

    private void waterSplash(float x, float z, int seed, float since, float size, int thrownIn) {
        boolean thrown = this.surface == thrownIn;
        int water = BoaScene.lerpColor(this.bd.waterLight, -1, 0.55f);
        int bright = BoaScene.lerpColor(this.bd.glint, -1, 0.6f);
        float patch = BoaScene.clamp01(since / 0.35f);
        float patchFade = 1.0f - BoaScene.clamp01((since - 0.15f) / 0.9f);
        if (patchFade > 0.0f) {
            this.foamPatch(x, z, size * (0.1f + 0.26f * BoaScene.easeOut(patch)), BoaScene.withAlpha(BoaScene.lerpColor(water, -1, 0.5f), 0.75f * patchFade * patchFade));
        }
        for (int ring = 0; ring < 3; ++ring) {
            float k = BoaScene.clamp01((since - (float)ring * 0.13f) / 1.1f);
            if (k <= 0.0f || k >= 1.0f) continue;
            float a = (1.0f - k) * (ring == 0 ? 1.0f : (ring == 1 ? 0.65f : 0.4f));
            this.ripple(x, z, size * (0.35f + (1.5f - 0.25f * (float)ring) * BoaScene.easeOut(k)), BoaScene.withAlpha(-1, a));
        }
        this.sprayRipples(x, z, seed, since, size);
        if (!thrown) {
            return;
        }
        if (since < 0.1f) {
            float k = since / 0.1f;
            float s = this.unit * size * (0.35f + 0.5f * k);
            this.out.sprite(BoaTex.GLOW, 256, 256, this.screenX(x), this.screenY(0.02f, z), s * 1.6f, s * 0.6f, 0.0f, BoaScene.withAlpha(bright, 0.85f * (1.0f - k)));
        }
        this.crown(x, z, seed, since, size, water, bright, false);
        this.column(x, z, seed, since, size, water, bright);
        this.crown(x, z, seed, since, size, water, bright, true);
        this.spray(x, z, seed, since, size, water, bright);
        this.splashMist(x, z, seed, since, size, water);
    }

    private void crown(float x, float z, int seed, float since, float size, int water, int bright, boolean near) {
        if (since > 0.75f) {
            return;
        }
        float spread = size * (0.06f + 0.26f * BoaScene.easeOut(BoaScene.clamp01(since / 0.32f)));
        float sheet = 1.0f - BoaScene.clamp01((since - 0.05f) / 0.16f);
        for (int j = 0; j < 18; ++j) {
            float fade;
            double a0 = (double)((float)j + 0.4f * BoaScene.hash(seed, 600 + j)) * Math.PI * 2.0 / 18.0;
            float depth = (float)Math.sin(a0);
            if (depth > 0.0f != near) continue;
            float tall = size * (0.2f + 0.2f * BoaScene.hash(seed, 620 + j));
            float launch = (float)Math.sqrt(18.0f * tall);
            float rise = launch * since - 4.5f * since * since;
            float lean = size * (0.1f + 0.1f * BoaScene.hash(seed, 640 + j)) * since;
            float bx = x + (float)Math.cos(a0) * spread;
            float bz = z + (float)Math.sin(a0) * spread;
            float tx = x + (float)Math.cos(a0) * (spread + lean);
            float tz = z + (float)Math.sin(a0) * (spread + lean);
            float peak = launch / 9.0f;
            float f = fade = near ? 1.0f : 0.8f;
            if (since < peak * 1.1f && rise > 0.0f) {
                double a1;
                float width = this.unit * size * 0.045f * (1.0f - 0.6f * since / (peak * 1.1f));
                this.taper(this.screenX(bx), this.screenY(0.0f, bz), this.screenX(tx), this.screenY(rise, tz), width, BoaScene.withAlpha(BoaScene.lerpColor(water, bright, 0.3f * (1.0f - depth)), 0.85f * fade));
                if (!(sheet > 0.0f) || (float)Math.sin(a1 = (double)((float)(j + 1) + 0.4f * BoaScene.hash(seed, 600 + (j + 1) % 18)) * Math.PI * 2.0 / 18.0) > 0.0f != near) continue;
                float tall1 = size * (0.2f + 0.2f * BoaScene.hash(seed, 620 + (j + 1) % 18));
                float launch1 = (float)Math.sqrt(18.0f * tall1);
                float rise1 = Math.max(0.0f, launch1 * since - 4.5f * since * since);
                float lean1 = size * (0.1f + 0.1f * BoaScene.hash(seed, 640 + (j + 1) % 18)) * since;
                float bx1 = x + (float)Math.cos(a1) * spread;
                float bz1 = z + (float)Math.sin(a1) * spread;
                float tx1 = x + (float)Math.cos(a1) * (spread + lean1);
                float tz1 = z + (float)Math.sin(a1) * (spread + lean1);
                this.out.poly(this.screenX(bx), this.screenY(0.0f, bz), this.screenX(bx1), this.screenY(0.0f, bz1), this.screenX(tx1), this.screenY(rise1 * 0.8f, tz1), this.screenX(tx), this.screenY(rise * 0.8f, tz), BoaScene.withAlpha(water, 0.32f * sheet * fade));
                continue;
            }
            float fall = Math.max(0.0f, rise);
            if (rise <= -0.02f) continue;
            float s = this.unit * size * (0.03f + 0.018f * BoaScene.hash(seed, 660 + j));
            float sx = this.screenX(tx);
            float sy = this.screenY(fall, tz);
            float prevRise = launch * (since - 0.03f) - 4.5f * (since - 0.03f) * (since - 0.03f);
            float lean0 = size * (0.1f + 0.1f * BoaScene.hash(seed, 640 + j)) * (since - 0.03f);
            this.out.line(this.screenX(x + (float)Math.cos(a0) * (spread + lean0)), this.screenY(Math.max(0.0f, prevRise), z + (float)Math.sin(a0) * (spread + lean0)), sx, sy, s * 0.7f, BoaScene.withAlpha(water, 0.55f * fade));
            this.out.sprite(BoaTex.DISC, 64, 64, sx, sy, s, s, 0.0f, BoaScene.withAlpha(bright, 0.9f * fade));
        }
    }

    private void column(float x, float z, int seed, float since, float size, int water, int bright) {
        float start = 0.05f;
        float s = since - start;
        if (s <= 0.0f) {
            return;
        }
        float tall = size * 0.72f * (0.85f + 0.3f * BoaScene.hash(seed, 700));
        float launch = (float)Math.sqrt(18.0f * tall);
        float top = launch * s - 4.5f * s * s;
        float peak = launch / 9.0f;
        if (top <= 0.0f && s > peak) {
            return;
        }
        top = Math.max(0.0f, top);
        float fallen = s > peak ? BoaScene.clamp01((s - peak) / peak) : 0.0f;
        float foot = top * 0.85f * BoaScene.easeIn(fallen);
        float lean = (BoaScene.hash(seed, 701) - 0.5f) * 0.2f * size * Math.min(1.0f, s / peak);
        float baseW = this.unit * size * 0.11f * (1.0f - 0.45f * BoaScene.clamp01(s / (peak * 1.6f)));
        float tipW = baseW * 0.35f;
        float alpha = 1.0f - BoaScene.clamp01((fallen - 0.55f) / 0.45f);
        float bx = this.screenX(x);
        float by = this.screenY(foot, z);
        float tx = this.screenX(x + lean);
        float ty = this.screenY(top, z);
        if (ty < by - 0.5f) {
            this.out.poly(bx - baseW, by, bx + baseW, by, tx + tipW, ty, tx - tipW, ty, BoaScene.withAlpha(water, 0.92f * alpha));
            this.out.poly(bx - baseW * 0.15f, by, bx + baseW * 0.55f, by, tx + tipW * 0.5f, ty, tx, ty, BoaScene.withAlpha(bright, 0.55f * alpha));
        }
        float drop = this.unit * size * 0.05f * (1.0f - 0.3f * fallen);
        this.out.sprite(BoaTex.DISC, 64, 64, tx, ty, drop, drop * 1.15f, 0.0f, BoaScene.withAlpha(bright, alpha));
        for (int k = 0; k < 3; ++k) {
            float shed = peak * (0.55f + 0.25f * (float)k);
            float after = s - shed;
            if (after <= 0.0f) continue;
            float t0 = shed;
            float from = Math.max(0.0f, launch * t0 - 4.5f * t0 * t0);
            float side = (BoaScene.hash(seed, 710 + k) - 0.5f) * 0.6f * size;
            float dy = from + (launch - 9.0f * t0) * 0.3f * after - 4.5f * after * after;
            if (dy <= 0.0f) continue;
            float dsz = this.unit * size * 0.028f;
            this.out.sprite(BoaTex.DISC, 64, 64, this.screenX(x + lean + side * after), this.screenY(dy, z), dsz, dsz, 0.0f, BoaScene.withAlpha(bright, 0.85f));
        }
    }

    private void spray(float x, float z, int seed, float since, float size, int water, int bright) {
        for (int i = 0; i < 22; ++i) {
            float at = 0.01f + 0.09f * BoaScene.hash(seed, 800 + i);
            float s = since - at;
            if (s <= 0.0f) continue;
            double a = (double)BoaScene.hash(seed, 810 + i) * Math.PI * 2.0;
            float reach = size * (0.35f + 0.95f * BoaScene.hash(seed, 820 + i));
            float up = size * (1.4f + 2.2f * BoaScene.hash(seed, 830 + i));
            float y = up * s - 4.5f * s * s;
            if (y <= 0.0f) continue;
            float wx = x + (float)Math.cos(a) * reach * s;
            float wz = z + (float)Math.sin(a) * reach * s * 0.6f;
            float p = s - 0.035f;
            float py0 = Math.max(0.0f, up * p - 4.5f * p * p);
            float px0 = x + (float)Math.cos(a) * reach * Math.max(0.0f, p);
            float pz0 = z + (float)Math.sin(a) * reach * Math.max(0.0f, p) * 0.6f;
            float sz = this.unit * size * (0.026f + 0.026f * BoaScene.hash(seed, 840 + i));
            float sx = this.screenX(wx);
            float sy = this.screenY(y, wz);
            float nearer = (float)Math.sin(a) > 0.0f ? 1.0f : 0.8f;
            this.out.line(this.screenX(px0), this.screenY(py0, pz0), sx, sy, sz * 0.75f, BoaScene.withAlpha(water, 0.5f * nearer));
            this.out.sprite(BoaTex.DISC, 64, 64, sx, sy, sz, sz, 0.0f, BoaScene.withAlpha(bright, 0.95f * nearer));
        }
    }

    private void sprayRipples(float x, float z, int seed, float since, float size) {
        for (int i = 0; i < 22; i += 2) {
            float up;
            float at = 0.01f + 0.09f * BoaScene.hash(seed, 800 + i);
            float land = at + 2.0f * (up = size * (1.4f + 2.2f * BoaScene.hash(seed, 830 + i))) / 9.0f;
            float after = since - land;
            if (after <= 0.0f || after > 0.5f) continue;
            double a = (double)BoaScene.hash(seed, 810 + i) * Math.PI * 2.0;
            float reach = size * (0.35f + 0.95f * BoaScene.hash(seed, 820 + i));
            float flight = land - at;
            float wx = x + (float)Math.cos(a) * reach * flight;
            float wz = z + (float)Math.sin(a) * reach * flight * 0.6f;
            float k = after / 0.5f;
            this.ripple(wx, wz, size * (0.06f + 0.22f * BoaScene.easeOut(k)), BoaScene.withAlpha(-1, 0.6f * (1.0f - k) * (1.0f - k)));
        }
    }

    private void splashMist(float x, float z, int seed, float since, float size, int water) {
        for (int i = 0; i < 4; ++i) {
            float s = since - 0.04f * (float)i;
            float life = 0.8f + 0.3f * BoaScene.hash(seed, 900 + i);
            if (s <= 0.0f || s >= life) continue;
            float k = s / life;
            float wy = size * (0.12f + 0.35f * BoaScene.easeOut(k) + 0.1f * (float)i);
            float wx = x + (BoaScene.hash(seed, 910 + i) - 0.5f) * 0.3f * size * (0.3f + k);
            float w = this.unit * size * (0.3f + 0.55f * BoaScene.easeOut(k));
            float alpha = BoaScene.clamp01(k * 8.0f) * (1.0f - k) * (1.0f - k) * 0.38f;
            this.out.sprite(BoaTex.PUFF, 128, 128, this.screenX(wx), this.screenY(wy, z), w, w * 0.7f, BoaScene.hash(seed, 920 + i) * 6.2832f + k * 0.6f, BoaScene.withAlpha(water, alpha));
        }
    }

    private void taper(float x0, float y0, float x1, float y1, float width, int argb) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) {
            return;
        }
        float nx = -dy / len * width * 0.5f;
        float ny = dx / len * width * 0.5f;
        this.out.poly(x0 - nx, y0 - ny, x0 + nx, y0 + ny, x1 + nx * 0.2f, y1 + ny * 0.2f, x1 - nx * 0.2f, y1 - ny * 0.2f, argb);
    }

    private void foamPatch(float x, float z, float radius, int argb) {
        float r = radius * this.unit;
        if (r < 0.5f || argb >>> 24 < 2) {
            return;
        }
        float ox = this.screenX(x);
        float oy = this.screenY(0.0f, z);
        int sides = 28;
        for (int k = 0; k < sides; ++k) {
            double a0 = (double)k * 2.0 * Math.PI / (double)sides;
            double a1 = (double)(k + 1) * 2.0 * Math.PI / (double)sides;
            float w0 = 1.0f + 0.08f * (float)Math.sin(a0 * 5.0 + 0.7);
            float w1 = 1.0f + 0.08f * (float)Math.sin(a1 * 5.0 + 0.7);
            this.pieceX[0] = ox;
            this.pieceY[0] = oy;
            this.pieceX[1] = ox + (float)Math.cos(a0) * r * w0;
            this.pieceY[1] = oy + (float)Math.sin(a0) * r * w0 * 0.32f;
            this.pieceX[2] = ox + (float)Math.cos(a1) * r * w1;
            this.pieceY[2] = oy + (float)Math.sin(a1) * r * w1 * 0.32f;
            this.onSurface(this.pieceX, this.pieceY, 3, argb, 0);
        }
    }

    private void explode(BoaDirector.Event e, float since) {
        float burst = this.burst();
        float after = since - burst;
        if (after < 0.0f || after > 2.0f) {
            return;
        }
        this.segmentAt(e.time(), e.segment());
        float ox = this.px;
        float oy = this.py;
        float oz = this.pz;
        float sx = this.screenX(ox);
        float sy = this.screenY(oy, oz);
        if (after < 0.3f) {
            float k = 1.0f - after / 0.3f;
            this.out.fill(0.0f, 0.0f, this.W, this.H, BoaScene.withAlpha(-1046, 0.8f * k * k));
        }
        BoaPrize prize = this.d.prize();
        float end = 1.0f / this.d.endPace() - this.burst();
        float inside = BoaScene.clamp01((after - 0.06f) / Math.max(0.12f, Math.min(0.62f, end - 0.1f)));
        if (this.d.phase() == BoaDirector.Phase.EXPLODE && prize.inHead() && inside > 0.0f && this.art.item(prize) != null) {
            float grow = BoaScene.easeOut(inside);
            float size = this.unit * 0.5f * (0.3f + 0.7f * grow);
            int tint = BoaScene.lerpColor(this.art.tint(prize), -1, 0.3f);
            this.itemLight(sx, sy, this.unit * 0.5f, BoaScene.easeOut(BoaScene.clamp01(after / 0.35f)), tint, prize.celebration());
            float before = this.out.alpha();
            this.out.alpha(before * BoaScene.clamp01(inside * 2.2f));
            this.drawItem(prize, sx, sy, size, 20.0f + (after - end) * 70.0f, tint);
            this.out.alpha(before);
        }
        float wave = BoaScene.clamp01(after / 0.55f);
        float ring = this.unit * (0.4f + 5.0f * BoaScene.easeOut(wave));
        this.out.sprite(BoaTex.RING, 256, 256, sx, sy, ring, ring * 0.8f, 0.0f, BoaScene.withAlpha(-19366, 1.0f - wave));
        for (int i = 0; i < 6; ++i) {
            float k = BoaScene.clamp01((after - (float)i * 0.03f) / 0.75f);
            if (k <= 0.0f || k >= 1.0f) continue;
            float a = BoaScene.hash(e.seed(), i + 11) * (float)Math.PI * 2.0f;
            float r = this.unit * 0.55f * BoaScene.easeOut(k) * (0.4f + BoaScene.hash(e.seed(), i + 12));
            float s = this.unit * (0.5f + 1.2f * BoaScene.easeOut(k));
            this.out.sprite(BoaTex.FIRE, 128, 128, sx + (float)Math.cos(a) * r, sy + (float)Math.sin(a) * r * 0.7f - k * this.unit * 0.4f, s, s, a, BoaScene.withAlpha(-1, 1.0f - k * k));
        }
        this.smoke(ox, oy, oz, e.seed(), after, 8, 7, 1.9f, -11910082, 1.0f);
        this.debris(BoaTex.HEAD, ox, oy, oz, e.seed(), 36, 3.0f, after, 1.5f);
        this.sparks(sx, sy, e.seed(), 14, this.unit * 3.2f, after, 0.9f, -11654);
    }

    private void tracerAndBullet() {
        if (this.d.phase() != BoaDirector.Phase.FIRE) {
            return;
        }
        float p = this.d.phaseProgress();
        int stage = this.d.run().hits();
        this.steady = true;
        this.segment(Math.min(4, stage), this.st);
        this.steady = false;
        float ex = this.screenX(this.px);
        float ey = this.screenY(this.py, this.pz);
        float bx = this.lensX;
        float by = this.lensY + this.lensR * 0.14f;
        float head = p;
        float tail = Math.max(0.0f, p - 0.35f);
        this.out.line(BoaScene.lerp(bx, ex, tail), BoaScene.lerp(by, ey, tail), BoaScene.lerp(bx, ex, head), BoaScene.lerp(by, ey, head), Math.max(1.0f, this.H * 0.006f * (1.0f - p * 0.6f)), BoaScene.withAlpha(-3920, 0.9f));
        if (this.d.slowMotion()) {
            float bxNow = BoaScene.lerp(bx, ex, p);
            float byNow = BoaScene.lerp(by, ey, p);
            float size = this.H * 0.105f * (1.0f - 0.55f * p);
            this.out.sprite(BoaTex.BULLET_BACK, 96, 96, bxNow, byNow, size, size, p * 7.0f, -1);
            float ring = this.lensR * 2.1f;
            this.out.sprite(BoaTex.RING, 256, 256, this.lensX, this.lensY, ring, ring, 0.0f, BoaScene.withAlpha(-1, 0.14f));
        }
    }

    private void reeds() {
        float h = 0.24f * this.H;
        float w = h * 2.0f;
        float drift = this.camX * this.unit * 1.5f;
        float first = this.lensX - w * 1.5f - (float)Math.floorMod((long)drift, (long)Math.max(1.0f, w));
        float y = this.lensY + this.lensR - h * 0.42f;
        for (int i = 0; i < 5; ++i) {
            float x = first + (float)i * w;
            if (x > this.lensX + this.lensR || x + w < this.lensX - this.lensR) continue;
            this.out.sprite(BoaTex.REEDS, 512, 256, x + w * 0.5f, y, w + 1.0f, h, 0.0f, this.bd.reeds);
        }
    }

    private float lastTension(BoaDirector.Phase phase) {
        if (phase == BoaDirector.Phase.AIM && this.d.lastShot()) {
            return BoaScene.easeInOut(this.d.aimProgress());
        }
        if (phase == BoaDirector.Phase.FIRE && this.d.lastShot()) {
            return 1.0f;
        }
        BoaDirector.Event shot = this.last(BoaDirector.Kind.SHOT);
        if (shot == null || !shot.near()) {
            return 0.0f;
        }
        BoaDirector.Event landed = this.landing(shot);
        return landed == null ? 0.0f : 1.0f - BoaScene.clamp01((this.t - landed.time()) * this.d.shotPace() / 0.45f);
    }

    private BoaDirector.Event landing(BoaDirector.Event shot) {
        for (BoaDirector.Event e : this.d.events()) {
            if (!(e.time() >= shot.time()) || e.kind() != BoaDirector.Kind.HIT && e.kind() != BoaDirector.Kind.MISS && e.kind() != BoaDirector.Kind.EXPLODE) continue;
            return e;
        }
        return null;
    }

    private float heartbeat() {
        float tau = this.d.phaseTime() * this.d.shotPace();
        float len = Math.max(0.3f, this.d.phaseLength() * this.d.shotPace());
        float beats = 1.2f * tau + 0.583f * tau * tau / len;
        float frac = beats - (float)Math.floor(beats);
        return (float)Math.exp(-frac * 7.0f);
    }

    private void lensDressing(BoaDirector.Phase phase, float sights) {
        BoaDirector.Event boom;
        BoaDirector.Event hit;
        float sinceHit;
        int reticle;
        int hits = this.d.run().hits();
        boolean head = hits >= 4 && phase != BoaDirector.Phase.HOLD && (phase != BoaDirector.Phase.REVEAL || this.d.shown().inHead()) && this.rear() > 0.0f;
        boolean fourth = hits == 3 && phase != BoaDirector.Phase.REVEAL;
        float tension = phase == BoaDirector.Phase.REVEAL ? 0.0f : this.lastTension(phase);
        float beat = 0.5f + 0.5f * (float)Math.sin((double)this.t * (6.0 + 6.0 * (double)this.d.aimProgress()));
        if (tension > 0.0f) {
            this.out.fill(this.lensX - this.lensR, this.lensY - this.lensR, this.lensX + this.lensR, this.lensY + this.lensR, BoaScene.withAlpha(-16645117, 0.22f * tension));
            this.tunnel(tension);
        }
        int n = reticle = head ? BoaScene.lerpColor(-15071739, -2082262, 0.35f + 0.5f * beat) : -15986419;
        if (tension > 0.0f) {
            float pulse = this.d.phase() == BoaDirector.Phase.AIM ? this.heartbeat() : 1.0f;
            reticle = BoaScene.lerpColor(reticle, -1560034, tension * (0.45f + 0.55f * pulse));
        }
        this.out.sprite(BoaTex.RETICLE, 512, 512, this.lensX, this.lensY, this.lensR * 2.0f, this.lensR * 2.0f, 0.0f, BoaScene.withAlpha(reticle, sights));
        if (fourth) {
            float slow = 0.5f + 0.5f * (float)Math.sin((double)this.t * (3.0 + 4.0 * (double)this.d.aimProgress()));
            float ring = this.lensR * 2.04f;
            this.out.sprite(BoaTex.RING, 256, 256, this.lensX, this.lensY, ring, ring, 0.0f, BoaScene.withAlpha(-20422, sights * (0.08f + 0.14f * slow * (0.4f + 0.6f * this.d.aimProgress()))));
        }
        if ((sinceHit = Math.min((hit = this.last(BoaDirector.Kind.HIT)) == null ? 9.0f : this.t - hit.time(), (boom = this.last(BoaDirector.Kind.EXPLODE)) == null ? 9.0f : this.t - boom.time())) < 0.3f) {
            float s = 0.075f * this.H * (1.0f + 0.4f * (1.0f - sinceHit / 0.3f));
            this.out.sprite(BoaTex.CROSS, 32, 32, this.lensX, this.lensY, s, s, 0.0f, BoaScene.withAlpha(-1, 1.0f - sinceHit / 0.3f));
        }
        if (head) {
            float ring = this.lensR * 2.04f;
            this.out.sprite(BoaTex.RING, 256, 256, this.lensX, this.lensY, ring, ring, 0.0f, BoaScene.withAlpha(-2084834, sights * (0.12f + 0.18f * beat * (this.d.holdingBreath() ? 1.0f : 0.5f))));
        }
        if (tension > 0.0f) {
            float pulse = this.d.phase() == BoaDirector.Phase.AIM ? this.heartbeat() : 0.6f;
            float ring = this.lensR * 2.04f;
            this.out.sprite(BoaTex.RING, 256, 256, this.lensX, this.lensY, ring, ring, 0.0f, BoaScene.withAlpha(-2084834, tension * (0.18f + 0.4f * pulse)));
        }
        this.out.sprite(BoaTex.SCOPE_MASK, 512, 512, this.lensX, this.lensY, this.lensR * 2.0f, this.lensR * 2.0f, 0.0f, -1);
        float l = this.lensX - this.lensR + 0.5f;
        float r = this.lensX + this.lensR - 0.5f;
        float top = this.lensY - this.lensR + 0.5f;
        float bottom = this.lensY + this.lensR - 0.5f;
        this.out.fill(0.0f, 0.0f, this.W, top, -16645117);
        this.out.fill(0.0f, bottom, this.W, this.H, -16645117);
        this.out.fill(0.0f, top, l, bottom, -16645117);
        this.out.fill(r, top, this.W, bottom, -16645117);
    }

    private void tunnel(float tension) {
        float r = this.lensR * (1.0f - 0.22f * tension);
        float a = 0.8f * tension;
        this.out.sprite(BoaTex.SCOPE_MASK, 512, 512, this.lensX, this.lensY, r * 2.0f, r * 2.0f, 0.0f, BoaScene.withAlpha(-1, a));
        int black = BoaScene.withAlpha(-16645117, a);
        float l = this.lensX - this.lensR;
        float rr = this.lensX + this.lensR;
        float top = this.lensY - this.lensR;
        float bottom = this.lensY + this.lensR;
        this.out.fill(l, top, rr, this.lensY - r, black);
        this.out.fill(l, this.lensY + r, rr, bottom, black);
        this.out.fill(l, this.lensY - r, this.lensX - r, this.lensY + r, black);
        this.out.fill(this.lensX + r, this.lensY - r, rr, this.lensY + r, black);
    }

    private void callouts(BoaDirector.Phase phase) {
        float px0 = this.lensX + this.lensR * 0.3f;
        float py0 = this.lensY - this.lensR * 0.34f;
        BoaDirector.Event hit = this.last(BoaDirector.Kind.HIT);
        BoaDirector.Event miss = this.lastMiss();
        float k;
        if (hit != null && this.t - hit.time() < 0.7f && (miss == null || hit.time() > miss.time())) {
            k = (this.t - hit.time()) / 0.7f;
            float stage = (float)Math.min(3, hit.segment()) / 3.0f;
            this.label("HIT!", px0, py0 - k * 0.03f * this.H, 0.06f * this.H * (1.0f + 0.55f * stage) * (1.0f + 0.3f * (1.0f - k)), BoaScene.withAlpha(BoaScene.lerpColor(-8070, -34250, stage), 1.0f - k * k), false);
        } else if (miss != null && this.t - miss.time() < 0.7f) {
            k = (this.t - miss.time()) / 0.7f;
            this.label("MISS", px0, py0, 0.045f * this.H, BoaScene.withAlpha(-6642788, 1.0f - k), false);
        }
        if (phase == BoaDirector.Phase.AIM && this.d.lastShot()) {
            float in = this.d.phaseTime() * this.d.shotPace();
            float slam = BoaScene.clamp01(in / 0.22f);
            float size = 0.07f * this.H * (1.0f + 1.4f * (1.0f - BoaScene.easeOut(slam))) * (1.0f + 0.08f * this.heartbeat());
            float gone = 1.0f - BoaScene.clamp01((this.d.aimProgress() - 0.72f) / 0.16f);
            float jolt = (1.0f - slam) * 0.012f * this.H;
            float jx = (BoaScene.hash(Float.floatToIntBits(this.t), 7) - 0.5f) * 2.0f * jolt;
            float jy = (BoaScene.hash(Float.floatToIntBits(this.t), 8) - 0.5f) * 2.0f * jolt;
            this.label("LAST SHOT", this.lensX + jx, this.lensY - this.lensR * 0.8f - size * 0.5f + jy, size, BoaScene.withAlpha(-46534, BoaScene.clamp01(slam * 3.0f) * gone), true);
        }
    }

    private void reveal(float into) {
        float titleIn;
        float spinYaw;
        float y;
        float x;
        float size;
        BoaPrize prize = this.d.shown();
        if (!prize.dropped()) {
            return;
        }
        float pace = this.d.endPace();
        float pt = into * pace;
        boolean grand = prize.inHead();
        float open = grand ? BoaScene.smoother(BoaScene.clamp01(pt / 1.45f)) : BoaScene.easeInOut(BoaScene.clamp01(pt / 1.25f));
        this.camera(this.lensRadius());
        float startR = this.lensRadius();
        BoaDirector.Event burst = this.last(BoaDirector.Kind.EXPLODE);
        if (grand && burst != null) {
            this.segmentAt(burst.time(), 4);
        } else {
            this.segment(3, this.st);
        }
        float fromX = this.screenX(this.px);
        float fromY = this.screenY(this.py, this.pz);
        this.lensR = BoaScene.lerp(startR, (float)Math.hypot(this.W, this.H) * 0.8f, open);
        this.bayou();
        if (!grand) {
            this.sinkHead = BoaScene.easeInOut(BoaScene.clamp01(pt / 1.2f));
            this.placeSnake();
        }
        this.effectsBehind();
        if (!grand) {
            float before = this.out.alpha();
            this.out.alpha(before * (1.0f - open));
            this.snake();
            this.out.alpha(before);
        }
        this.effectsInFront();
        this.effects();
        this.reeds();
        this.out.fill(0.0f, 0.0f, this.W, this.H, BoaScene.argb((grand ? 0.52f : 0.45f) * open, 4, 10, 10));
        TikiModel model = this.art.item(prize);
        if (model == null) {
            this.lensDressing(BoaDirector.Phase.REVEAL, 1.0f - open);
            this.tracker(1.0f - open);
            this.ammo(1.0f - open);
            return;
        }
        int tint = BoaScene.lerpColor(this.art.tint(prize), -1, 0.3f);
        float celebration = prize.celebration();
        this.leaves(open, 0.3f + 0.9f * celebration);
        if (grand) {
            size = BoaScene.lerp(this.unit * 0.5f, 0.34f * this.H, open);
            x = BoaScene.lerp(fromX, this.cx, BoaScene.easeOut(BoaScene.clamp01(open * 3.0f)));
            float held = this.d.heldFor();
            float arc = -0.05f * this.H * (float)Math.sin(Math.PI * (double)open);
            float bob = Math.max(open, BoaScene.smoother(BoaScene.clamp01(held / 0.6f)));
            y = BoaScene.lerp(fromY, this.cy - 0.04f * this.H, open) + arc + (float)Math.sin((double)this.t * 2.0) * this.H * 0.008f * bob;
            spinYaw = 20.0f + (pt + held) * 54.0f + 14.0f * (float)Math.sin((double)pt * 2.2);
            this.godRays(x, y, size, open, tint, celebration);
        } else {
            this.poppedPose(this.popSeconds(pt), open);
            x = this.itemX;
            y = this.itemY;
            size = this.itemSize;
            spinYaw = this.itemYaw;
        }
        float lit = grand ? 1.0f : open;
        this.itemLight(x, y, size, lit, tint, celebration);
        int count = 6 + Math.round(10.0f * celebration);
        for (int i = 0; i < count; ++i) {
            float period = 1.1f;
            float phase = (pt + (float)i * period / (float)count) % period;
            float k = phase / period;
            float a = BoaScene.hash(i * 131, (int)((pt + (float)i * period / (float)count) / period)) * (float)Math.PI * 2.0f;
            float r = size * (0.8f + 2.4f * BoaScene.easeOut(k));
            float s = size * 0.22f * (1.0f - k);
            this.out.sprite(BoaTex.SPARK, 32, 32, x + (float)Math.cos(a) * r, y + (float)Math.sin(a) * r, s, s, a, BoaScene.withAlpha(i % 2 == 0 ? tint : -13238, (1.0f - k) * open));
        }
        this.drawItem(prize, x, y, size, spinYaw, tint, grand ? 1.0f : open);
        this.lensDressing(BoaDirector.Phase.REVEAL, 1.0f - open);
        this.tracker(1.0f - open);
        this.ammo(1.0f - open);
        float titleAt = 0.95f;
        if (grand) {
            this.arrival(x, y, size, pt - 1.45f, tint);
            this.sparkleShower(pt - 1.45f + 0.3f, tint, celebration);
            titleAt = 1.4000001f;
        }
        if ((titleIn = BoaScene.clamp01((pt - titleAt) / 0.35f)) > 0.0f) {
            float pop = 1.0f + (grand ? 0.5f : 0.35f) * (1.0f - BoaScene.easeOut(titleIn));
            String name = prize.title().toUpperCase(Locale.ROOT);
            float px = Math.min(0.085f * this.H, this.W * 0.88f / Math.max(1.0f, 0.86f * (float)name.length()));
            float ty = y + size * 0.95f;
            this.label(name, this.cx, ty, px * pop, BoaScene.withAlpha(BoaScene.lerpColor(tint, -1, 0.35f), titleIn), true);
        }
    }

    private float popSeconds(float revealSeconds) {
        BoaDirector.Event pop = this.last(BoaDirector.Kind.POP);
        if (pop != null) {
            return Math.max(0.0f, (this.t - pop.time()) * this.d.endPace());
        }
        return revealSeconds + 1.15f - 0.74f;
    }

    private void poppedPose(float since, float open) {
        float grow = Math.max(0.05f, BoaScene.easeOutBack(BoaScene.clamp01(since / 0.3f)));
        float lift = 0.3f * BoaScene.easeOut(BoaScene.clamp01(since / 0.6f));
        this.segment(3, this.st);
        float fromX = this.screenX(this.px);
        float fromY = this.screenY(this.py + lift, this.pz);
        this.itemSize = BoaScene.lerp(this.unit * 0.6f * grow, 0.34f * this.H, open);
        this.itemX = BoaScene.lerp(fromX, this.cx, open);
        this.itemY = BoaScene.lerp(fromY, this.cy - 0.04f * this.H, open) + (float)Math.sin((double)this.t * 2.0) * this.H * 0.008f * open;
        this.itemYaw = 20.0f + 54.0f * since + 65.1f * (1.0f - (float)Math.exp(-since / 0.35f)) + 14.0f * (float)Math.sin(since * 2.2f) * open;
    }

    private void floatingItem() {
        BoaDirector.Event burst = this.last(BoaDirector.Kind.EXPLODE);
        BoaPrize prize = this.d.shown();
        if (burst == null || !prize.inHead() || this.art.item(prize) == null) {
            return;
        }
        this.segmentAt(burst.time(), 4);
        float held = this.d.heldFor();
        float x = this.screenX(this.px);
        float y = this.screenY(this.py, this.pz) + this.floatBob(held);
        float size = this.unit * 0.5f;
        int tint = BoaScene.lerpColor(this.art.tint(prize), -1, 0.3f);
        this.itemLight(x, y, size, 1.0f, tint, prize.celebration());
        this.drawItem(prize, x, y, size, 20.0f + held * 54.0f, tint);
    }

    private float floatBob(float held) {
        return (float)Math.sin((double)this.t * 2.0) * this.H * 0.008f * BoaScene.smoother(BoaScene.clamp01(held / 0.6f));
    }

    private void poppedItem() {
        BoaDirector.Event pop = this.last(BoaDirector.Kind.POP);
        BoaPrize prize = this.d.fourthItem();
        if (pop == null || this.t < pop.time() || !prize.dropped() || this.art.item(prize) == null) {
            return;
        }
        this.poppedPose(this.popSeconds(0.0f), 0.0f);
        this.drawItem(prize, this.itemX, this.itemY, this.itemSize, this.itemYaw, BoaScene.lerpColor(this.art.tint(prize), -1, 0.3f), 0.0f);
    }

    private void tossedItem() {
        BoaDirector.Event toss = this.last(BoaDirector.Kind.TOSS);
        BoaPrize prize = this.d.extra();
        if (toss == null || !prize.dropped() || this.art.item(prize) == null || this.last(BoaDirector.Kind.POP) == null) {
            return;
        }
        float k = (this.t - toss.time()) * this.d.endPace() / 0.42f;
        if (k < 0.0f || k >= 1.0f) {
            return;
        }
        this.poppedPose(this.popSeconds(0.0f), 0.0f);
        float throwK = BoaScene.easeIn(k);
        float exit = this.lensX - this.lensR - this.itemSize * 2.5f;
        float x = BoaScene.lerp(this.itemX, exit, throwK);
        float y = this.itemY - this.lensR * 0.12f * (float)Math.sin(Math.PI * (double)Math.min(1.0f, k * 1.2f));
        float yaw = this.itemYaw + 540.0f * k * k;
        int tint = BoaScene.lerpColor(this.art.tint(prize), -1, 0.3f);
        for (int i = 1; i <= 4; ++i) {
            float back = Math.max(0.0f, throwK - (float)i * 0.07f);
            float tx = BoaScene.lerp(this.itemX, exit, back);
            float s = this.itemSize * (0.9f - 0.15f * (float)i);
            this.out.sprite(BoaTex.GLOW, 256, 256, tx, y, s * 2.2f, s * 1.2f, 0.0f, BoaScene.withAlpha(tint, 0.35f * (1.0f - k) * (1.0f - (float)i * 0.2f)));
        }
        this.drawItem(prize, x, y, this.itemSize, yaw, tint, 0.0f);
    }

    private void itemLight(float x, float y, float size, float lit, int tint, float celebration) {
        if (lit <= 0.01f) {
            return;
        }
        float breathe = 1.0f + 0.05f * (float)Math.sin((double)this.t * 2.1);
        float wash = size * 7.5f * breathe;
        this.out.sprite(BoaTex.GLOW, 256, 256, x, y, wash, wash, this.t * 0.06f, BoaScene.withAlpha(tint, 0.26f * lit));
        float rays = size * 4.2f * breathe * (0.75f + 0.25f * celebration);
        this.out.sprite(BoaTex.GLOW, 256, 256, x, y, rays, rays, this.t * 0.22f, BoaScene.withAlpha(tint, 0.78f * lit));
        this.out.sprite(BoaTex.GLOW, 256, 256, x, y, rays * 0.62f, rays * 0.62f, -this.t * 0.35f, BoaScene.withAlpha(-1, 0.36f * lit));
        float pulse = 0.5f + 0.5f * (float)Math.sin((double)this.t * 2.6);
        float circle = size * 2.7f * (1.0f + 0.035f * pulse);
        this.out.sprite(BoaTex.SOFT_RING, 256, 256, x, y, circle, circle, 0.0f, BoaScene.withAlpha(BoaScene.lerpColor(tint, -1, 0.45f), (0.55f + 0.25f * pulse) * lit));
        float outer = circle * 1.16f;
        this.out.sprite(BoaTex.SOFT_RING, 256, 256, x, y, outer, outer, 0.0f, BoaScene.withAlpha(tint, (0.22f + 0.18f * (1.0f - pulse)) * lit));
        float radius = circle * 0.5f * 0.8f;
        int motes = 12 + Math.round(10.0f * celebration);
        for (int i = 0; i < motes; ++i) {
            double a = (double)i * Math.PI * 2.0 / (double)motes + (double)this.t * (i % 2 == 0 ? 0.6 : -0.45);
            float wobble = 1.0f + 0.05f * (float)Math.sin((double)this.t * 3.0 + (double)i * 1.7);
            float mx = x + (float)Math.cos(a) * radius * wobble;
            float my = y + (float)Math.sin(a) * radius * wobble;
            float twinkle = 0.5f + 0.5f * (float)Math.sin((double)this.t * 6.0 + (double)i * 2.3);
            float s = size * (0.1f + 0.1f * twinkle);
            this.out.sprite(BoaTex.SPARK, 32, 32, mx, my, s, s, this.t * 1.5f + (float)i, BoaScene.withAlpha(i % 3 == 0 ? -1 : BoaScene.lerpColor(tint, -1, 0.3f), (0.35f + 0.65f * twinkle) * lit));
        }
    }

    private void godRays(float x, float y, float size, float open, int tint, float celebration) {
        float reach = Math.max(this.W, this.H) * (1.1f + 0.3f * celebration);
        float in = BoaScene.easeInOut(BoaScene.clamp01((open - 0.25f) / 0.75f));
        if (in <= 0.0f) {
            return;
        }
        this.out.sprite(BoaTex.GLOW, 256, 256, x, y, reach, reach, -this.t * 0.12f, BoaScene.withAlpha(tint, 0.3f * in));
        this.out.sprite(BoaTex.GLOW, 256, 256, x, y, reach * 0.75f, reach * 0.75f, this.t * 0.09f + 0.26f, BoaScene.withAlpha(BoaScene.lerpColor(tint, -1, 0.5f), 0.22f * in));
    }

    private void arrival(float x, float y, float size, float since, int tint) {
        if (since < 0.0f || since > 1.2f) {
            return;
        }
        if (since < 0.45f) {
            float k = 1.0f - since / 0.45f;
            this.out.fill(0.0f, 0.0f, this.W, this.H, BoaScene.withAlpha(BoaScene.lerpColor(tint, -1, 0.65f), 0.34f * k * k));
        }
        for (int ring = 0; ring < 2; ++ring) {
            float k = BoaScene.clamp01((since - (float)ring * 0.14f) / 0.9f);
            if (k <= 0.0f || k >= 1.0f) continue;
            float r = size * (2.2f + 9.0f * BoaScene.easeOut(k));
            this.out.sprite(BoaTex.SOFT_RING, 256, 256, x, y, r, r, 0.0f, BoaScene.withAlpha(BoaScene.lerpColor(tint, -1, 0.4f), 0.9f * (1.0f - k) * (1.0f - k)));
        }
        float k = BoaScene.clamp01(since / 0.8f);
        for (int i = 0; i < 18; ++i) {
            float a = (float)i * (float)Math.PI * 2.0f / 18.0f + 0.17f;
            float r = size * (1.2f + 4.2f * BoaScene.easeOut(k)) * (0.8f + 0.4f * BoaScene.hash(i, 911));
            float s = size * 0.3f * (1.0f - k);
            this.out.sprite(BoaTex.SPARK, 32, 32, x + (float)Math.cos(a) * r, y + (float)Math.sin(a) * r, s, s, a, BoaScene.withAlpha(i % 2 == 0 ? -1 : tint, 1.0f - k));
        }
    }

    private void sparkleShower(float since, int tint, float celebration) {
        float in = BoaScene.clamp01(since / 0.6f);
        if (in <= 0.0f) {
            return;
        }
        int count = 18 + Math.round(26.0f * celebration);
        for (int i = 0; i < count; ++i) {
            float speed = this.H * (0.08f + 0.14f * BoaScene.hash(i, 1201));
            float span = this.H * 1.15f;
            float travelled = (since * speed + BoaScene.hash(i, 1202) * span) % span;
            float y = this.H * 1.05f - travelled;
            float x = this.W * (0.03f + 0.94f * BoaScene.hash(i, 1203)) + (float)Math.sin((double)this.t * 1.3 + (double)i) * this.W * 0.012f;
            float twinkle = 0.5f + 0.5f * (float)Math.sin(this.t * (5.0f + 4.0f * BoaScene.hash(i, 1204)) + (float)i);
            float s = this.H * (0.01f + 0.018f * BoaScene.hash(i, 1205)) * (0.6f + 0.4f * twinkle);
            this.out.sprite(BoaTex.SPARK, 32, 32, x, y, s, s, this.t + (float)i, BoaScene.withAlpha(i % 3 == 0 ? -1 : tint, in * (0.3f + 0.7f * twinkle)));
        }
    }

    private void drawItem(BoaPrize prize, float x, float y, float size, float yaw, int tint) {
        this.drawItem(prize, x, y, size, yaw, tint, 1.0f);
    }

    private void drawItem(BoaPrize prize, float x, float y, float size, float yaw, int tint, float look) {
        TikiModel model = this.art.item(prize);
        if (model == null) {
            return;
        }
        this.faces.clear();
        this.pool.reset();
        ModelProjection.project(model, false, yaw, BoaScene.lerp(-7.0f, -9.0f, look), size / 0.5f, 0.0f, -model.centreY(), 0.0f, 10, this.faces, this.pool);
        this.faces.sort(BACK_TO_FRONT);
        BoaTex skin = BoaTex.skinOf(prize);
        float ink = BoaScene.lerp(this.outline, Math.max(1.5f, size * 0.03f), look);
        this.inkFaces(ink, x, y, skin);
        for (ModelProjection.Face f : this.faces) {
            int scene = BoaScene.lerpColor(this.bd.cool, this.bd.warm, f.tone * f.tone);
            if (f.interior) {
                scene = BoaScene.lerpColor(scene, this.bd.cool, 0.5f);
            }
            int shown = BoaScene.lerpColor(BoaScene.lerpColor(-11637620, -10590, f.tone * f.tone), tint, 0.25f);
            int colour = BoaScene.lerpColor(BoaScene.shade(scene, f.brightness), BoaScene.shade(shown, f.brightness * 1.08f), look);
            this.face(f, skin, colour, x, y);
        }
    }

    private BoaDirector.Event last(BoaDirector.Kind kind) {
        List<BoaDirector.Event> events = this.d.events();
        for (int i = events.size() - 1; i >= 0; --i) {
            if (events.get(i).kind() != kind) continue;
            return events.get(i);
        }
        return null;
    }

    private BoaDirector.Event lastMiss() {
        return this.last(BoaDirector.Kind.MISS);
    }

    private void label(String text, float x, float y, float px, int argb, boolean centred) {
        float scale = px / 7.0f;
        this.out.text(text, x + scale, y + scale, scale, BoaScene.withAlpha(-16315383, (float)(argb >>> 24 & 0xFF) / 255.0f), centred);
        this.out.text(text, x, y, scale, argb, centred);
    }

    private static int argb(float a, int r, int g, int b) {
        int alpha = (int)(Math.max(0.0f, Math.min(1.0f, a)) * 255.0f);
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    private static int withAlpha(int argb, float mul) {
        int a = (int)((float)(argb >>> 24 & 0xFF) * Math.max(0.0f, Math.min(1.0f, mul)));
        return a << 24 | argb & 0xFFFFFF;
    }

    private static int shade(int argb, float mul) {
        int r = Math.min(255, (int)((float)(argb >> 16 & 0xFF) * mul));
        int g = Math.min(255, (int)((float)(argb >> 8 & 0xFF) * mul));
        int b = Math.min(255, (int)((float)(argb & 0xFF) * mul));
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int lerpColor(int a, int b, float t) {
        t = BoaScene.clamp01(t);
        int aa = a >>> 24 & 0xFF;
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        return (int)((float)aa + (float)((b >>> 24 & 0xFF) - aa) * t) << 24 | (int)((float)ar + (float)((b >> 16 & 0xFF) - ar) * t) << 16 | (int)((float)ag + (float)((b >> 8 & 0xFF) - ag) * t) << 8 | (int)((float)ab + (float)((b & 0xFF) - ab) * t);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float ramp(float v, float a, float b) {
        return BoaScene.easeInOut((v - a) / (b - a));
    }

    private static float easeOut(float t) {
        float u = 1.0f - BoaScene.clamp01(t);
        return 1.0f - u * u * u;
    }

    private static float easeIn(float t) {
        t = BoaScene.clamp01(t);
        return t * t * t;
    }

    private static float easeInOut(float t) {
        t = BoaScene.clamp01(t);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float smoother(float t) {
        t = BoaScene.clamp01(t);
        return t * t * t * (t * (6.0f * t - 15.0f) + 10.0f);
    }

    private static float easeOutBack(float t) {
        t = BoaScene.clamp01(t);
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float u = t - 1.0f;
        return 1.0f + c3 * u * u * u + c1 * u * u;
    }

    private static float hash(int a, int b) {
        int h = a * 668265261 ^ b * 374761393;
        h ^= h >>> 15;
        h *= 739982445;
        h ^= h >>> 12;
        h *= 695872825;
        h ^= h >>> 15;
        return (float)(h & 0xFFFFFF) / 1.6777216E7f;
    }
}

