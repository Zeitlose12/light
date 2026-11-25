package com.skymaxx.tiki.audio;

import com.skymaxx.tiki.anim.AnimationDirector;
import com.skymaxx.tiki.anim.Prize;
import com.skymaxx.tiki.audio.GameMute;
import com.skymaxx.tiki.audio.Jackpot;
import com.skymaxx.tiki.audio.SoundTheme;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.synth.DeathWhistleVoice;
import com.skymaxx.tiki.synth.NoiseVoice;
import com.skymaxx.tiki.synth.ShepardVoice;
import com.skymaxx.tiki.synth.SynthEngine;
import com.skymaxx.tiki.synth.ToneVoice;

public class AudioDirector {
    private static final int STEPS = 16;
    private static final int[][] MOTIF = new int[][]{{-1, -1, 0, -1, -1, -1, -1, -1, -1, -1, 2, -1, -1, -1, -1, -1}, {-1, -1, 0, -1, -1, 3, -1, -1, -1, -1, 2, -1, -1, 1, -1, -1}, {-1, 0, 2, -1, -1, 3, -1, 1, -1, 0, 2, -1, -1, 3, -1, -1}, {-1, 0, 2, 3, -1, 3, 2, -1, -1, 0, 2, 3, -1, 4, 3, 1}};
    private static final float TEMPO_GLIDE = 2.2f;
    private static final float BUILD_FROM = 135.0f;
    private static final float[] SILENCE_FROM = new float[]{2.0f, 2.0f, 2.0f, 0.72f};
    private static final float[] LAND_DUCK = new float[]{0.38f, 0.46f, 0.58f, 0.7f};
    private static final float SCREAM_SHRIEK_HZ = 1850.0f;
    private static final float SCREAM_MOAN_HZ = 1150.0f;
    private static final float SCREAM_SECONDS = 3.5f;
    private static final float SCREAM_GAIN = 2.2f;
    private static final float SCREAM_GRIT = 0.7f;
    private static final float SCREAM_CROWD = 0.45f;
    private static final float SCARE_VOLUME_BOOST = 2.5f;
    private final TikiConfig cfg;
    private final SynthEngine synth = new SynthEngine();
    private volatile float ensemble = 1.0f;
    private final SoundTheme theme = SoundTheme.CLASSIC;
    private GameMute mute;
    private boolean drumsRunning;
    private int step;
    private float stepTimer;
    private float climb;
    private int tier;
    private float droneTimer;
    private float bpm;
    private float targetBpm;
    private float lastAngle;
    private float tickAccumulator;
    private int tickIndex;
    private float build;
    private float riserLeft;
    private boolean holding;
    private boolean broke;
    private float tickNoteGap;
    private static final float BREAK_AHEAD_LOW = 0.72f;
    private volatile long jackpotSince;
    private static final int[] CHAIN_NOTES = new int[]{5, 7, 8, 10};
    private volatile boolean scaring;

    public AudioDirector(TikiConfig cfg) {
        this.cfg = cfg;
    }

    public void init() {
        this.synth.start();
        this.applyVolume();
    }

    SynthEngine engine() {
        return this.synth;
    }

    public void shutdown() {
        this.synth.shutdown();
    }

    public void applyVolume() {
        this.synth.setMaster(this.cfg.volume * this.theme.trim() * this.ensemble);
    }

    public void setEnsemble(float value) {
        this.ensemble = Math.max(0.1f, Math.min(1.0f, value));
        this.applyVolume();
    }

    public boolean audioBroken() {
        return this.synth.broken();
    }

    public void tick() {
        this.synth.watchdog();
    }

    public SoundTheme theme() {
        return this.theme;
    }

    public void onAnimationStart() {
        this.scaring = false;
        this.applyVolume();
        this.drumsRunning = true;
        this.step = 0;
        this.stepTimer = 0.0f;
        this.droneTimer = 0.0f;
        this.climb = 0.0f;
        this.tier = 0;
        this.lastAngle = 0.0f;
        this.tickAccumulator = 0.0f;
        this.tickIndex = 0;
        this.build = 0.0f;
        this.riserLeft = 0.0f;
        this.holding = false;
        this.broke = true;
        this.targetBpm = this.bpm = this.tempoFor(0);
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float root = this.theme.root();
        this.synth.playBed(this.theme.drone(root * 0.5f, 0.3f, 3.4f));
        this.synth.play(this.theme.air(0.16f, -0.3f, 2.6f));
        this.synth.play(this.theme.land(root, 0.32f, 0.15f));
    }

    public void onReelStart(int index, float duration) {
        this.tier = index;
        this.targetBpm = this.tempoFor(index);
        this.build = 0.0f;
        this.riserLeft = 0.0f;
        this.holding = false;
        this.broke = false;
        this.lastAngle = 0.0f;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float hz = this.theme.tierHz(index);
        this.synth.play(NoiseVoice.whoosh(0.22f, Math.min(1.4f, duration * 0.4f)));
        this.synth.play(this.theme.lead(hz * 2.0f, 0.3f, 0.0f));
        this.synth.playBed(this.theme.bass(hz * 0.5f, 0.34f, 1.4f));
        for (int voice = 0; voice <= index; ++voice) {
            float degree = this.theme.scale()[Math.min(this.theme.scale().length - 1, voice * 2)];
            this.synth.playBed(this.theme.pad(hz * degree, 0.055f + 0.012f * (float)index, voice % 2 == 0 ? -0.45f : 0.45f, 2.2f + 0.5f * (float)index));
        }
    }

    private void onRotation(float angle) {
        if (!this.cfg.soundsEnabled || this.scaring) {
            return;
        }
        float delta = Math.abs(angle - this.lastAngle);
        this.lastAngle = angle;
        this.tickAccumulator += delta;
        while (this.tickAccumulator >= 9.0f) {
            this.tickAccumulator -= 9.0f;
            int degree = 5 + this.tickIndex++ % 5;
            float hz = this.theme.tierHz(this.tier) * this.theme.scale()[degree];
            this.synth.playBed(this.theme.tick(hz, 0.045f));
            if (!(this.tickNoteGap <= 0.0f)) continue;
            this.tickNoteGap = 0.055f;
            float pan = this.tickIndex % 2 == 0 ? -0.15f : 0.15f;
            this.synth.playBed(new ToneVoice(hz, new float[]{1.0f, 2.0f, 3.0f}, new float[]{1.0f, 0.35f, 0.12f}, new float[]{0.16f, 0.08f, 0.05f}, 0.001f, 0.045f, pan, 0.2f, 0.0f, 0.0f, 0.0f, 0.0f));
        }
    }

    private void onBreak() {
        this.onBreak(1.0f);
    }

    private void onBreak(float low) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth.duck(0.34f + 0.16f * (float)this.tier);
        float hz = this.theme.tierHz(this.tier);
        this.synth.play(NoiseVoice.crash(0.26f + 0.1f * (float)this.tier, 0.7f + 0.2f * (float)this.tier));
        this.synth.play(NoiseVoice.whoosh(0.3f + 0.1f * (float)this.tier, 0.38f));
        this.synth.play(this.theme.hit(44.0f, (0.55f + 0.15f * (float)this.tier) * low, 0.0f));
        this.synth.play(this.theme.accent(0.18f, 0.0f));
        this.synth.play(ToneVoice.sweepDown(64.0f + 8.0f * (float)this.tier, (0.42f + 0.12f * (float)this.tier) * low, 0.55f));
        this.synth.play(this.theme.bass(hz * 0.5f, 0.26f, 0.7f));
    }

    public void onBreakAhead() {
        if (this.broke || this.scaring) {
            return;
        }
        this.broke = true;
        this.onBreak(0.72f);
    }

    public void onReelLanded(int index, boolean won) {
        this.holding = false;
        this.onSecondLanded(index, won);
    }

    public void onSecondLanded(int index, boolean won) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        int t = Math.max(0, Math.min(3, index));
        this.synth.duck(LAND_DUCK[t]);
        this.synth.play(NoiseVoice.detent(0.26f, 0.0f));
        if (won) {
            this.landed(t);
        } else if (index >= 3) {
            this.nearMiss(index);
        } else {
            this.deadWood(index);
        }
    }

    public void onExtraLanded() {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float hz = this.theme.tierHz(2);
        float[] scale = this.theme.scale();
        this.synth.play(ToneVoice.bell(hz * 2.0f, 0.09f, -0.25f));
        this.schedule(0.07f, () -> this.synth.play(ToneVoice.bell(hz * 3.0f, 0.07f, 0.25f)));
        for (int i = 0; i < 4; ++i) {
            float pitch = hz * 2.0f * scale[Math.min(scale.length - 1, 2 + i)];
            float pan = i % 2 == 0 ? 0.3f : -0.3f;
            this.schedule(0.12f + (float)i * 0.06f, () -> this.synth.play(this.theme.sparkle(pitch, 0.08f, pan)));
        }
    }

    public void onSecondReelStart(int index, float duration) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float hz = this.theme.tierHz(Math.max(0, Math.min(3, index)));
        this.synth.play(NoiseVoice.whoosh(0.2f, Math.min(1.4f, duration * 0.4f)));
        this.synth.play(this.theme.lead(hz * 2.0f, 0.26f, 0.0f));
    }

    private void landed(int t) {
        float hz = this.theme.tierHz(t);
        float[] scale = this.theme.scale();
        int[] chord = this.theme.chordDegrees();
        this.synth.play(this.theme.land(hz, 0.55f + 0.06f * (float)t, 0.0f));
        for (int voice = 1; voice <= t; ++voice) {
            float degree = scale[Math.min(scale.length - 1, chord[Math.min(chord.length - 1, voice)])];
            this.synth.play(this.theme.land(hz * degree, 0.2f + 0.03f * (float)t, voice % 2 == 0 ? 0.28f : -0.28f));
        }
        float hold = 1.5f + 0.45f * (float)t;
        this.synth.play(this.theme.pad(hz * 2.0f, 0.12f + 0.02f * (float)t, -0.35f, hold));
        this.synth.play(this.theme.pad(hz * scale[chord[2]], 0.09f + 0.02f * (float)t, 0.35f, hold * 0.9f));
        this.synth.play(this.theme.bass(hz * 0.5f, 0.42f, 1.8f));
        this.synth.play(this.theme.air(0.13f + 0.02f * (float)t, 0.0f, 1.6f + 0.3f * (float)t));
        int grains = 3 + t * 2;
        for (int i = 0; i < grains; ++i) {
            float pitch = hz * scale[Math.min(scale.length - 1, 5 + t + i)];
            float gain = 0.16f - 0.008f * (float)i;
            this.schedule(0.06f + (float)i * (0.075f - 0.008f * (float)t), () -> this.synth.play(this.theme.sparkle(pitch, gain, grains % 2 == 0 ? -0.2f : 0.2f)));
        }
        if (t >= 2) {
            this.schedule(0.1f, () -> this.synth.play(ShepardVoice.riser(hz, 0.06f + 0.02f * (float)t, 1.1f)));
        }
        if (t >= 3) {
            this.synth.play(NoiseVoice.slap(0.06f));
            this.synth.play(this.theme.hit(32.0f, 0.3f, 0.0f));
            this.synth.play(ToneVoice.sweepDown(56.0f, 0.3f, 1.15f));
            this.synth.play(ToneVoice.sub(32.0f, 0.15f, 0.95f));
            this.synth.play(NoiseVoice.crash(0.05f, 1.8f));
        }
    }

    private void nearMiss(int index) {
        float hz = this.theme.tierHz(index);
        this.synth.play(ShepardVoice.faller(hz, 0.2f, 1.5f));
        this.synth.play(ToneVoice.sag(hz, 0.34f, 0.9f));
        this.synth.play(ToneVoice.sag(hz * 1.5f, 0.16f, 0.75f));
        this.synth.play(NoiseVoice.slap(0.14f));
    }

    private void deadWood(int index) {
        float hz = this.theme.tierHz(index);
        this.synth.play(this.theme.deadWood(hz * 0.72f, 0.75f));
        this.synth.play(this.theme.deadWood(hz * 1.05f, 0.35f));
        this.synth.play(NoiseVoice.slap(0.1f));
    }

    public void onFailOutro() {
        this.drumsRunning = false;
        this.holding = false;
        if (this.cfg.soundsEnabled) {
            this.synth.play(this.theme.drone(this.theme.root() * 0.5f, 0.16f, 1.1f));
        }
    }

    public void onWin() {
        this.onWin(1.0f);
    }

    public void onWin(float celebration) {
        this.drumsRunning = false;
        this.holding = false;
        this.payout(celebration);
    }

    public void onSecondWin(float celebration) {
        this.payout(celebration);
    }

    public void resumeBed(int index, float angle, float animClimb) {
        this.drumsRunning = true;
        this.holding = false;
        this.tier = Math.max(0, Math.min(3, index));
        this.targetBpm = Math.max(this.targetBpm, this.tempoFor(this.tier));
        this.climb = Math.max(this.climb, animClimb);
        this.lastAngle = angle;
        this.build = 0.0f;
        this.riserLeft = 0.0f;
        this.broke = 180.0f - angle <= 90.0f;
    }

    private void payout(float celebration) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float size = Math.max(0.15f, Math.min(1.0f, celebration));
        this.synth.duck(0.55f + 0.35f * size);
        float root = this.theme.root();
        float[] scale = this.theme.scale();
        int[] chord = this.theme.chordDegrees();
        this.synth.play(ShepardVoice.riser(root * 2.0f, 0.16f * size, 0.5f));
        float chordGain = 0.75f + 0.25f * size;
        this.schedule(0.24f, () -> {
            this.synth.play(this.theme.land(root, 0.55f * chordGain, 0.0f));
            this.synth.play(this.theme.land(root * 2.0f, 0.3f * chordGain, 0.0f));
            for (int i = 0; i < chord.length; ++i) {
                float degree = scale[Math.min(scale.length - 1, chord[i])];
                this.synth.play(this.theme.pad(root * degree * 2.0f, (0.2f - 0.02f * (float)i) * chordGain, i % 2 == 0 ? -0.4f : 0.4f, 2.0f + 1.2f * size));
            }
            this.synth.play(this.theme.bass(root * 0.5f, 0.5f * chordGain, 1.8f + 0.8f * size));
            this.synth.play(this.theme.air(0.2f * size, 0.0f, 3.0f));
            this.synth.play(NoiseVoice.crash(0.2f * size, 1.8f));
        });
        int strikes = Math.max(2, Math.round(6.0f * size));
        for (int i = 0; i < strikes; ++i) {
            float pitch = root * scale[10] * (i % 2 == 0 ? 1.0f : 1.5f);
            float gain = (i == strikes - 1 ? 0.34f : 0.26f) * size;
            this.schedule(0.3f + (float)i * 0.135f, () -> this.synth.play(this.theme.land(pitch, gain, 0.0f)));
        }
        int grains = Math.max(8, Math.round(30.0f * size));
        float span = 1.7f * (0.55f + 0.45f * size);
        for (int i = 0; i < grains; ++i) {
            float t = (float)i / (float)(grains - 1);
            float delay = 0.46f + t * span - 0.3f * t * t * size;
            float pitch = root * scale[Math.max(0, 13 - (int)(t * 7.0f))] * 2.0f;
            float gain = 0.17f * (1.0f - 0.5f * t) * (0.7f + 0.3f * size);
            float pan = (float)(i % 3 - 1) * 0.38f;
            this.schedule(delay, () -> this.synth.play(this.theme.sparkle(pitch, gain, pan)));
        }
        this.schedule(0.44f, () -> this.synth.play(this.theme.air(0.14f * size, 0.0f, 1.6f)));
    }

    public void onCountTick(float progress) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float[] scale = this.theme.scale();
        int degree = Math.min(scale.length - 1, 8 + (int)(progress * 6.0f));
        float hz = this.theme.root() * scale[degree] * 2.0f;
        this.synth.play(this.theme.sparkle(hz, 0.1f, 0.0f));
    }

    public void onStamp() {
        int i;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float root = this.theme.root();
        float[] scale = this.theme.scale();
        int[] chord = this.theme.chordDegrees();
        this.synth.play(NoiseVoice.slap(0.6f));
        this.synth.play(NoiseVoice.crash(0.3f, 2.4f));
        this.synth.play(this.theme.hit(46.0f, 1.0f, 0.0f));
        this.synth.play(this.theme.accent(0.24f, 0.0f));
        this.synth.play(ToneVoice.sweepDown(76.0f, 0.5f, 0.75f));
        for (i = 0; i < chord.length; ++i) {
            float degree = scale[Math.min(scale.length - 1, chord[i])];
            this.synth.play(this.theme.land(root * degree * 2.0f, 0.3f - 0.05f * (float)i, i % 2 == 0 ? -0.3f : 0.3f));
        }
        this.synth.play(this.theme.pad(root * 2.0f, 0.16f, 0.0f, 2.4f));
        for (i = 0; i < 5; ++i) {
            float pitch = root * scale[Math.min(scale.length - 1, 9 + i)] * 2.0f;
            float pan = i % 2 == 0 ? 0.28f : -0.28f;
            this.schedule(0.05f + (float)i * 0.045f, () -> this.synth.play(this.theme.sparkle(pitch, 0.17f, pan)));
        }
        this.schedule(0.06f, () -> this.synth.play(this.theme.air(0.16f, 0.0f, 2.2f)));
    }

    public void onJackpotLanded() {
        this.drumsRunning = false;
        this.holding = false;
        this.build = 0.0f;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.markJackpot();
        Jackpot.play(this.synth, this.theme);
    }

    void markJackpot() {
        this.jackpotSince = System.nanoTime();
    }

    public void onJackpotSkipped() {
        long since = this.jackpotSince;
        if (since == 0L) {
            return;
        }
        this.jackpotSince = 0L;
        if (System.nanoTime() - since < 11200000000L) {
            this.synth.silence();
        }
    }

    public void onChainTurn(Prize prize, int step, float lead, boolean last) {
        int turns = AnimationDirector.chainTurns(prize);
        if (!this.cfg.soundsEnabled || turns == 0) {
            return;
        }
        float at = Math.max(0.0f, lead - 0.14f);
        float seconds = AnimationDirector.chainSeconds(prize);
        float changeAt = at + (turns >= 2 ? 0.6f : 0.46f) * seconds;
        float lockAt = at + 0.82f * seconds;
        float[] scale = this.theme.scale();
        float root = this.theme.root();
        float note = root * scale[CHAIN_NOTES[Math.max(0, Math.min(CHAIN_NOTES.length - 1, step))]];
        float pan = (step % 2 == 0 ? -1.0f : 1.0f) * 0.28f;
        this.schedule(at, () -> this.synth.play(ToneVoice.pluck(note * 0.5f, 0.06f, -pan)));
        float sweepFrom = 0.12f;
        float sweepTo = turns >= 2 ? 0.66f : 0.46f;
        float sweep = (sweepTo - sweepFrom) / (float)turns;
        for (int k = 0; k < turns; ++k) {
            float start = at + (sweepFrom + (float)k * sweep) * seconds;
            float length = sweep * seconds;
            float lowHz = note * (k == 0 ? 0.5f : 1.0f);
            float highHz = lowHz * 2.0f;
            float sweepPan = (k % 2 == 0 ? pan : -pan) * 1.6f;
            this.schedule(start, () -> {
                this.synth.play(new NoiseVoice(600.0f, 2800.0f, 2.2f, true, length * 0.55f, length * 0.35f, 0.045f, sweepPan, 0.4f));
                this.synth.play(new ToneVoice(lowHz, new float[]{1.0f, 2.0f}, new float[]{1.0f, 0.22f}, new float[]{length * 0.9f, length * 0.5f}, length * 0.6f, 0.055f, sweepPan, 0.35f, highHz, length, 0.0f, 0.0f));
            });
        }
        this.schedule(changeAt, () -> {
            switch (prize) {
                case MASK: {
                    this.synth.play(ToneVoice.logDrum(note, 0.17f, pan));
                    this.synth.play(ToneVoice.bell(note * 2.0f, 0.08f, -pan));
                    break;
                }
                case BUBBLE: {
                    this.synth.play(new ToneVoice(note, new float[]{1.0f, 2.0f}, new float[]{1.0f, 0.2f}, new float[]{0.14f, 0.07f}, 0.002f, 0.14f, pan, 0.4f, note * 1.5f, 0.05f, 0.0f, 0.0f));
                    this.synth.play(ToneVoice.bell(note * 2.0f, 0.08f, -pan));
                    break;
                }
                default: {
                    this.synth.play(ToneVoice.bell(note * 2.0f, 0.13f, pan));
                    this.synth.play(new ToneVoice(note * 4.0f, new float[]{1.0f, 2.0f, 3.0f}, new float[]{1.0f, 0.3f, 0.12f}, new float[]{0.55f, 0.32f, 0.2f}, 0.003f, 0.035f, -pan, 0.6f, 0.0f, 0.0f, 6.0f, 0.003f));
                }
            }
        });
        this.schedule(lockAt, () -> {
            this.synth.play(NoiseVoice.detent(0.13f, -pan));
            this.synth.play(ToneVoice.pluck(note * 1.5f, 0.06f, -pan));
        });
        if (last) {
            float resolve = at + 0.92f * seconds;
            for (int i = 0; i < CHAIN_NOTES.length; ++i) {
                float hz = root * scale[CHAIN_NOTES[i]] * 2.0f;
                float spread = i % 2 == 0 ? -0.3f : 0.3f;
                this.schedule(resolve + (float)i * 0.06f, () -> this.synth.play(prize == Prize.MASK ? this.theme.sparkle(hz, 0.11f, spread) : ToneVoice.bell(hz, 0.08f, spread)));
            }
            this.schedule(resolve + 0.2f, () -> this.synth.play(this.theme.pad(root * 2.0f, 0.07f, 0.0f, 1.6f)));
        }
    }

    public void onScareRevealed(float lead) {
        this.scaring = true;
        this.drumsRunning = false;
        this.holding = false;
        this.build = 0.0f;
        this.riserLeft = 0.0f;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth.silence();
        this.synth.setMaster(Math.min(1.0f, this.cfg.volume * 2.5f) * this.theme.trim() * this.ensemble);
        float at = Math.max(0.0f, lead - 0.14f);
        this.schedule(at, () -> {
            this.synth.play(new DeathWhistleVoice(1850.0f, 1150.0f, 3.5f, 2.2f, -0.05f, 0.6f, 0.7f));
            this.synth.play(NoiseVoice.slap(0.6f));
            this.synth.play(NoiseVoice.crash(0.08f, 2.4f));
            this.synth.play(ToneVoice.sub(36.7f, 0.32f, 2.6f));
            this.synth.play(ToneVoice.sweepDown(92.0f, 0.62f, 1.4f));
            this.synth.play(ToneVoice.bass(55.0f, 0.32f, 3.0f));
            this.synth.play(ToneVoice.bass(58.2725f, 0.26f, 2.8f));
            this.synth.play(ToneVoice.bass(77.781f, 0.2f, 2.6f));
        });
        this.schedule(at + 0.07f, () -> this.synth.play(new DeathWhistleVoice(1936.9501f, 1184.5f, 3.2f, 0.99f, -0.75f, 0.75f, 0.7f)));
        this.schedule(at + 0.15f, () -> this.synth.play(new DeathWhistleVoice(1763.05f, 1115.5f, 3.7f, 0.99f, 0.75f, 0.75f, 0.7f)));
        this.scareFirstLine(at);
        this.scareSecondLine(at);
    }

    private void scareFirstLine(float at) {
        float lineOne = 1.8f;
        this.schedule(at + lineOne - 0.6f, () -> {
            this.synth.play(new ToneVoice(55.0f, new float[]{1.0f, 2.0f, 3.0f, 4.02f}, new float[]{1.0f, 0.3f, 0.15f, 0.06f}, new float[]{4.0f, 3.5f, 3.0f, 2.5f}, 1.6f, 0.3f, -0.15f, 0.5f, 0.0f, 0.0f, 0.27f, 0.012f));
            this.synth.play(new ToneVoice(58.2725f, new float[]{1.0f, 2.0f, 3.0f}, new float[]{1.0f, 0.26f, 0.1f}, new float[]{3.8f, 3.0f, 2.4f}, 2.0f, 0.2f, 0.15f, 0.5f, 0.0f, 0.0f, 0.19f, 0.015f));
        });
        this.schedule(at + lineOne, () -> {
            this.synth.play(ToneVoice.gong(73.4f, 0.42f, 0.0f));
            this.synth.play(ToneVoice.bell(155.6f, 0.12f, 0.0f));
            this.synth.play(ToneVoice.sub(41.2f, 0.3f, 1.6f));
        });
        this.schedule(at + 3.9f, () -> this.synth.play(AudioDirector.whisper(-0.85f, 0.09f)));
        this.schedule(at + 4.8f, () -> this.synth.play(AudioDirector.whisper(0.85f, 0.08f)));
        float[] box = new float[]{659.3f, 622.3f, 523.3f};
        float[] boxAt = new float[]{2.7f, 3.5f, 4.6f};
        for (int i = 0; i < box.length; ++i) {
            float hz = box[i] * (1.0f - 0.012f * (float)i);
            float pan = i % 2 == 0 ? -0.35f : 0.35f;
            this.schedule(at + boxAt[i], () -> {
                this.synth.play(ToneVoice.pluck(hz, 0.07f, pan));
                this.synth.play(ToneVoice.bell(hz * 2.0f, 0.025f, pan));
            });
        }
        this.heartbeat(at, lineOne + 0.5f, 5.2000003f, 0.95f, 1.02f, 1.0f);
    }

    private void scareSecondLine(float at) {
        float flash = 5.55f;
        float lineTwo = 5.6f;
        float end = 10.8f;
        float gone = end + 2.6f;
        this.schedule(at + flash - 0.16f, () -> this.synth.play(new NoiseVoice(1500.0f, 700.0f, 1.4f, true, 0.12f, 0.1f, 0.2f, 0.85f, 0.04f)));
        this.schedule(at + flash, () -> {
            this.synth.play(new DeathWhistleVoice(1998.0001f, 1242.0f, 0.42f, 2.2f, 0.0f, 0.35f, 0.7f));
            this.synth.play(NoiseVoice.slap(0.5f));
        });
        this.schedule(at + lineTwo, () -> {
            this.synth.play(ToneVoice.sweepDown(72.0f, 0.72f, 2.0f));
            this.synth.play(ToneVoice.gong(49.0f, 0.5f, 0.0f));
            this.synth.play(ToneVoice.sub(36.7f, 0.42f, 2.6f));
            this.synth.play(NoiseVoice.crash(0.1f, 2.4f));
            this.synth.play(ToneVoice.bell(155.6f, 0.1f, 0.0f));
        });
        this.schedule(at + lineTwo - 0.2f, () -> {
            this.synth.play(new ToneVoice(41.2f, new float[]{1.0f, 2.0f, 3.0f, 4.02f}, new float[]{1.0f, 0.32f, 0.16f, 0.07f}, new float[]{8.0f, 6.5f, 5.0f, 4.0f}, 2.0f, 0.34f, -0.2f, 0.5f, 0.0f, 0.0f, 0.22f, 0.014f));
            this.synth.play(new ToneVoice(43.65f, new float[]{1.0f, 2.0f, 3.0f}, new float[]{1.0f, 0.28f, 0.1f}, new float[]{7.5f, 6.0f, 4.5f}, 2.6f, 0.24f, 0.2f, 0.5f, 0.0f, 0.0f, 0.17f, 0.016f));
            this.synth.play(new ToneVoice(58.3f, new float[]{1.0f, 2.0f}, new float[]{1.0f, 0.2f}, new float[]{7.0f, 5.0f}, 3.2f, 0.14f, 0.0f, 0.55f, 0.0f, 0.0f, 0.11f, 0.02f));
        });
        this.schedule(at + lineTwo + 0.4f, () -> {
            this.synth.play(ToneVoice.choir(196.0f, 0.09f, -0.35f, 5.5f));
            this.synth.play(ToneVoice.choir(207.65f, 0.08f, 0.35f, 5.5f));
            this.synth.play(ToneVoice.choir(138.6f, 0.07f, 0.0f, 6.0f));
        });
        float[] box = new float[]{493.9f, 466.2f};
        float[] boxAt = new float[]{8.1f, 9.7f};
        for (int i = 0; i < box.length; ++i) {
            float hz = box[i] * (0.97f - 0.02f * (float)i);
            float pan = i % 2 == 0 ? 0.4f : -0.4f;
            this.schedule(at + boxAt[i], () -> {
                this.synth.play(ToneVoice.pluck(hz, 0.06f, pan));
                this.synth.play(ToneVoice.bell(hz * 2.0f, 0.02f, pan));
            });
        }
        this.heartbeat(at, lineTwo + 0.4f, gone - 0.4f, 0.64f, 1.05f, 0.0f);
        this.schedule(at + end, () -> {
            this.synth.play(ToneVoice.sag(55.0f, 0.28f, 3.4f));
            this.synth.play(NoiseVoice.breath(0.1f, 0.0f, 3.2f));
        });
        float lone = gone + this.cfg.stampDelaySeconds + 2.3f;
        this.schedule(at + lone, () -> this.synth.play(ToneVoice.drum(44.0f, 0.52f)));
        this.schedule(at + lone + 0.18f, () -> this.synth.play(ToneVoice.drum(39.0f, 0.34f)));
    }

    private void heartbeat(float at, float from, float to, float interval, float slowing, float floor) {
        float beat = from;
        float gap = interval;
        while (beat < to) {
            float k = (beat - from) / Math.max(0.01f, to - from);
            float level = 1.0f - (1.0f - floor) * k * k;
            float lub = 0.6f * level;
            float dub = 0.4f * level;
            this.schedule(at + beat, () -> this.synth.play(ToneVoice.drum(46.0f, lub)));
            this.schedule(at + beat + 0.17f, () -> this.synth.play(ToneVoice.drum(40.0f, dub)));
            beat += gap;
            gap *= slowing;
        }
    }

    private static NoiseVoice whisper(float pan, float gain) {
        return new NoiseVoice(2400.0f, 900.0f, 2.2f, true, 0.35f, 0.28f, gain, pan, 0.25f);
    }

    public void onScareSkipped() {
        this.synth.silence();
        this.applyVolume();
    }

    public void onAnimationEnd(boolean won) {
        this.drumsRunning = false;
        this.holding = false;
        this.build = 0.0f;
    }

    public void forceRestore() {
        this.drumsRunning = false;
        this.holding = false;
        if (this.mute != null) {
            this.mute.restore();
        }
        this.synth.silence();
        this.applyVolume();
    }

    public void update(float dt, boolean spinning, int index, float angle, float animClimb) {
        this.tickNoteGap -= dt;
        if (spinning) {
            this.tier = index;
            this.onRotation(angle);
            this.anticipation(dt, angle);
        } else {
            this.build = 0.0f;
            this.holding = false;
        }
        this.climb = Math.max(this.climb, animClimb);
        this.drums(dt);
    }

    private void anticipation(float dt, float angle) {
        if (this.scaring) {
            this.build = 0.0f;
            this.holding = false;
            return;
        }
        float remaining = 180.0f - angle;
        if (remaining <= 90.0f) {
            if (!this.broke) {
                this.broke = true;
                this.onBreak();
            }
            this.build = 1.0f;
            this.holding = false;
            return;
        }
        float span = 45.0f;
        this.build = AudioDirector.clamp01((135.0f - remaining) / Math.max(1.0f, span));
        if (this.build <= 0.0f) {
            return;
        }
        float weight = 0.55f + 0.15f * (float)this.tier;
        boolean bl = this.holding = this.build > SILENCE_FROM[Math.max(0, Math.min(3, this.tier))];
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.riserLeft -= dt;
        if (this.riserLeft <= 0.0f) {
            float length = 1.6f;
            this.riserLeft = length * 0.62f;
            float gain = 0.17f * weight * (0.5f + 0.5f * this.build);
            this.synth.playBed(ShepardVoice.riser(this.theme.tierHz(this.tier), gain, length));
            this.synth.playBed(NoiseVoice.riser(gain * 0.55f, length * 0.8f));
            this.synth.playBed(this.theme.air(gain * 0.45f, 0.0f, length * 0.7f));
        }
    }

    private void drums(float dt) {
        if (!(this.drumsRunning && this.cfg.drumsEnabled && this.cfg.soundsEnabled)) {
            return;
        }
        this.targetBpm = Math.max(this.targetBpm, this.tempoFor(this.tier));
        this.bpm = Math.max(this.bpm, 20.0f);
        this.bpm += (this.targetBpm - this.bpm) * Math.min(1.0f, dt * 2.2f);
        float stepSeconds = 60.0f / this.bpm / 4.0f;
        this.stepTimer += dt;
        int guard = 0;
        while (this.stepTimer >= stepSeconds && guard++ < 8) {
            this.stepTimer -= stepSeconds;
            if (!this.holding) {
                this.playStep(this.step);
            }
            ++this.step;
            if (this.step < 16) continue;
            this.step = 0;
        }
        this.droneTimer -= dt;
        if (this.droneTimer <= 0.0f) {
            this.droneTimer = 16.0f * stepSeconds;
            if (!this.holding) {
                this.synth.playBed(this.theme.drone(this.theme.root() * 0.5f, (0.12f + this.climb * 0.12f) * this.bedGain(), 3.0f));
            }
        }
    }

    private void playStep(int s) {
        int degree;
        boolean kick;
        int row = Math.max(0, Math.min(3, this.tier));
        float vol = this.cfg.drumVolume * this.bedGain();
        float root = this.theme.tierHz(this.tier) * 0.5f;
        float[] scale = this.theme.scale();
        kick = switch (row) {
            case 0 -> s == 0 || s == 8;
            case 1 -> s == 0 || s == 6 || s == 8 || s == 14;
            case 2 -> s == 0 || s == 3 || s == 6 || s == 8 || s == 11 || s == 14;
            default -> s % 2 == 0 || s == 3 || s == 11;
        };
        if (kick) {
            boolean downbeat = s == 0;
            this.synth.playBed(this.theme.hit(downbeat ? 48.0f : 58.0f, (downbeat ? 0.95f : 0.62f) * vol, 0.0f));
            if (downbeat) {
                this.synth.playBed(this.theme.accent(0.16f * vol, 0.0f));
            }
        }
        if (row >= 1 && (s == 4 || s == 12)) {
            this.synth.playBed(this.theme.tom(104.0f, 0.5f * vol));
            this.synth.playBed(this.theme.accent(0.13f * vol, s == 4 ? -0.25f : 0.25f));
        }
        if (row >= 2 && s % 2 == 1) {
            this.synth.playBed(this.theme.hat(0.1f * vol, s % 4 == 1 ? -0.35f : 0.35f));
        }
        if (row >= 3 && (s == 7 || s == 15)) {
            this.synth.playBed(this.theme.tom(88.0f, 0.34f * vol));
        }
        if (s == 0 || s == 6 || s == 10) {
            float degree2 = s == 6 ? scale[3] : 1.0f;
            this.synth.playBed(this.theme.bass(root * degree2, (s == 0 ? 0.46f : 0.3f) * vol, 0.55f));
        }
        if ((degree = MOTIF[row][s]) >= 0) {
            this.synth.playBed(this.theme.lead(this.theme.tierHz(this.tier) * scale[degree], (0.12f + 0.08f * this.climb) * vol, s % 8 < 4 ? -0.3f : 0.3f));
        }
    }

    private float bedGain() {
        return 0.7f + 0.1f * (float)Math.max(0, Math.min(3, this.tier));
    }

    private float tempoFor(int index) {
        int i = Math.max(0, Math.min(3, index));
        return AudioDirector.lerp(this.cfg.drumBpmMin, this.cfg.drumBpmMax, (float)i / 3.0f);
    }

    private void schedule(float delaySeconds, Runnable action) {
        this.synth.after(delaySeconds, action);
    }

    public void setMuted(boolean wanted) {
        if (wanted) {
            this.synth.keepAwake();
        }
        if (this.mute == null) {
            if (!wanted) {
                return;
            }
            this.mute = new GameMute();
        }
        this.mute.set(wanted);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }
}

