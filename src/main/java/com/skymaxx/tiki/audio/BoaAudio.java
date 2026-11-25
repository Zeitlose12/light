package com.skymaxx.tiki.audio;

import com.skymaxx.tiki.audio.AudioDirector;
import com.skymaxx.tiki.audio.Jackpot;
import com.skymaxx.tiki.audio.SoundTheme;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.synth.NoiseVoice;
import com.skymaxx.tiki.synth.ShepardVoice;
import com.skymaxx.tiki.synth.SynthEngine;
import com.skymaxx.tiki.synth.ToneVoice;
import java.util.Objects;

public class BoaAudio {
    private static final int STEPS = 16;
    private static final float[] BPM = new float[]{96.0f, 108.0f, 122.0f, 138.0f, 160.0f};
    private static final int[][] MOTIF = new int[][]{{0, -1, -1, -1, -1, -1, 2, -1, -1, -1, -1, -1, 1, -1, -1, -1}, {0, -1, -1, 2, -1, -1, 3, -1, -1, -1, 2, -1, 1, -1, -1, -1}, {0, -1, 2, -1, 3, -1, 2, -1, 0, -1, 2, -1, 4, -1, 3, -1}, {0, 2, -1, 3, -1, 4, 3, -1, 0, 2, -1, 3, 5, -1, 4, 3}, {0, 2, 3, 4, 3, 2, 3, 5, 0, 2, 3, 4, 5, 4, 6, 5}};
    private final TikiConfig cfg;
    private final AudioDirector host;
    private final SoundTheme theme = SoundTheme.CLASSIC;
    private boolean bed;
    private boolean scoped;
    private int stage;
    private int step;
    private float stepTimer;
    private float bpm = BPM[0];
    private float heartTimer;
    private float riserLeft;
    private boolean aiming;
    private boolean lastAim;
    private boolean lead = true;

    public BoaAudio(TikiConfig cfg, AudioDirector host) {
        this.cfg = cfg;
        this.host = host;
    }

    public BoaAudio twin() {
        final BoaAudio parent = this;
        BoaAudio twin = new BoaAudio(this.cfg, this.host){

            @Override
            public void setMuted(boolean wanted) {
                parent.setMuted(wanted);
            }
        };
        twin.lead = false;
        return twin;
    }

    public void setLead(boolean value) {
        this.lead = value;
    }

    public boolean lead() {
        return this.lead;
    }

    public void setEnsemble(float value) {
        this.host.setEnsemble(value);
    }

    private SynthEngine synth() {
        return this.host.engine();
    }

    private void applyMaster() {
        this.host.applyVolume();
    }

    public void setMuted(boolean wanted) {
        this.host.setMuted(wanted);
    }

    public void onRunStart() {
        this.applyMaster();
        this.bed = true;
        this.scoped = false;
        this.stage = 0;
        this.step = 0;
        this.stepTimer = 0.0f;
        this.bpm = 100.0f;
        this.heartTimer = 0.0f;
        this.riserLeft = 0.0f;
        this.aiming = false;
        this.lastAim = false;
        if (!this.cfg.soundsEnabled || !this.lead) {
            return;
        }
        float root = this.theme.root();
        this.synth().playBed(this.theme.drone(root * 0.5f, 0.24f, 3.2f));
        this.synth().play(this.theme.air(0.13f, -0.25f, 2.4f));
    }

    public void onSkipToScope() {
        if (this.lead) {
            this.synth().silence();
        }
        this.bed = true;
        this.riserLeft = 0.0f;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth().play(NoiseVoice.whoosh(0.2f, 0.35f));
    }

    public void onCylinderIn(float intro) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float lands = intro + 0.06f;
        this.synth().play(NoiseVoice.whoosh(0.22f, Math.max(0.2f, intro * 1.5f)));
        this.schedule(lands, () -> this.synth().play(NoiseVoice.detent(0.34f, 0.0f)));
        this.schedule(lands, () -> this.synth().play(this.theme.hit(62.0f, 0.34f, 0.0f)));
    }

    public void onBulletLoad(int index, boolean bonus) {
        float pan;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float[] scale = this.theme.scale();
        float hz = this.theme.root() * scale[Math.min(scale.length - 1, 5 + index)];
        float f = pan = index % 2 == 0 ? -0.18f : 0.18f;
        if (bonus) {
            this.bonusRound(index, hz);
            return;
        }
        this.synth().play(NoiseVoice.detent(0.36f, pan));
        this.synth().play(ToneVoice.coin(hz * 2.0f, 0.15f, pan));
        this.synth().play(this.theme.tick(hz, 0.07f));
        this.synth().play(this.theme.hit(70.0f + (float)index * 6.0f, 0.2f, 0.0f));
    }

    private void bonusRound(int index, float hz) {
        int i;
        this.synth().duck(0.45f);
        float[] scale = this.theme.scale();
        int[] chord = this.theme.chordDegrees();
        this.synth().play(NoiseVoice.detent(0.48f, 0.0f));
        this.synth().play(NoiseVoice.slap(0.24f));
        this.synth().play(this.theme.hit(56.0f, 0.5f, 0.0f));
        for (i = 0; i < 3; ++i) {
            float degree = scale[Math.min(scale.length - 1, chord[i])];
            this.synth().play(this.theme.land(hz * degree * 0.5f, 0.22f - 0.05f * (float)i, i % 2 == 0 ? -0.3f : 0.3f));
        }
        for (i = 0; i < 4; ++i) {
            float pitch = hz * scale[Math.min(scale.length - 1, 5 + i)];
            this.schedule(0.04f + (float)i * 0.05f, () -> this.synth().play(this.theme.sparkle(pitch, 0.12f, 0.2f)));
        }
    }

    public void onChamberFull() {
        int i;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth().duck(0.7f);
        float root = this.theme.root();
        float[] scale = this.theme.scale();
        int[] chord = this.theme.chordDegrees();
        this.synth().play(ShepardVoice.riser(root * 2.0f, 0.12f, 0.5f));
        for (i = 0; i < chord.length; ++i) {
            float degree = scale[Math.min(scale.length - 1, chord[i])];
            this.synth().play(this.theme.pad(root * degree * 2.0f, 0.13f - 0.015f * (float)i, i % 2 == 0 ? -0.4f : 0.4f, 2.2f));
        }
        for (i = 0; i < 8; ++i) {
            float pitch = root * scale[Math.min(scale.length - 1, 5 + i)] * 2.0f;
            float pan = i % 2 == 0 ? -0.25f : 0.25f;
            this.schedule(0.06f * (float)i, () -> this.synth().play(this.theme.land(pitch, 0.18f, pan)));
        }
        this.schedule(0.5f, () -> this.synth().play(NoiseVoice.crash(0.18f, 1.4f)));
    }

    public void onCylinderSpin(float seconds) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth().play(NoiseVoice.whoosh(0.28f, seconds));
        float at = 0.0f;
        float gap = 0.028f;
        int guard = 0;
        while (at < seconds - 0.05f && guard++ < 40) {
            float gain = 0.16f + 0.12f * (at / seconds);
            float when = at;
            this.schedule(when, () -> this.synth().play(NoiseVoice.detent(gain, 0.0f)));
            at += gap;
            gap *= 1.16f;
        }
        this.schedule(seconds, () -> {
            this.synth().play(NoiseVoice.slap(0.4f));
            this.synth().play(NoiseVoice.detent(0.55f, 0.0f));
            this.synth().play(this.theme.hit(56.0f, 0.52f, 0.0f));
        });
    }

    public void onRifleRaise() {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth().play(NoiseVoice.whoosh(0.22f, 0.7f));
        this.schedule(0.66f, () -> {
            this.synth().play(NoiseVoice.detent(0.55f, 0.2f));
            this.synth().play(ToneVoice.woodBlock(820.0f, 0.26f));
        });
        this.schedule(0.8f, () -> {
            this.synth().play(NoiseVoice.detent(0.5f, 0.2f));
            this.synth().play(NoiseVoice.slap(0.18f));
        });
        this.schedule(1.05f, () -> this.synth().play(NoiseVoice.whoosh(0.16f, 0.6f)));
    }

    public void onScopeIn() {
        this.scoped = true;
        this.stage = 0;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float root = this.theme.root();
        this.synth().play(this.theme.air(0.16f, 0.0f, 2.8f));
        if (this.lead) {
            this.synth().playBed(this.theme.drone(root * 0.5f, 0.22f, 3.0f));
        }
        for (int i = 0; i < 3; ++i) {
            float hz = root * this.theme.scale()[5 + i] * 2.0f;
            this.schedule(0.2f + (float)i * 0.09f, () -> this.synth().play(this.theme.tick(hz, 0.05f)));
        }
    }

    public void onAim(int segment, float seconds, boolean lastShot, boolean cycleBolt) {
        this.stage = Math.max(0, Math.min(4, segment));
        this.aiming = true;
        this.riserLeft = 0.0f;
        this.lastAim = lastShot;
        this.bed = true;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        if (cycleBolt) {
            this.bolt();
        }
        if (lastShot) {
            this.synth().duck(0.7f);
            this.synth().play(this.theme.land(this.theme.root() * 0.5f, 0.34f, 0.0f));
            this.synth().play(this.theme.hit(40.0f, 0.5f, 0.0f));
            this.synth().play(this.theme.air(0.12f, 0.0f, seconds));
            this.schedule(0.25f, () -> this.synth().play(NoiseVoice.breath(0.16f, 0.0f, 0.9f)));
        }
    }

    private void bolt() {
        float pan = 0.3f;
        this.synth().play(new NoiseVoice(4200.0f, 2600.0f, 2.2f, true, 4.0E-4f, 0.018f, 0.3f, pan, 0.12f));
        this.schedule(0.07f, () -> this.synth().play(new NoiseVoice(2600.0f, 1500.0f, 1.4f, true, 0.004f, 0.045f, 0.16f, pan, 0.15f)));
        this.schedule(0.12f, () -> this.synth().play(NoiseVoice.detent(0.34f, pan)));
        this.schedule(0.24f, () -> this.synth().play(new NoiseVoice(2200.0f, 3000.0f, 1.4f, true, 0.004f, 0.04f, 0.16f, pan, 0.15f)));
        this.schedule(0.3f, () -> {
            this.synth().play(NoiseVoice.detent(0.42f, pan));
            this.synth().play(ToneVoice.woodBlock(1250.0f, 0.08f));
        });
        this.schedule(0.52f, () -> this.synth().play(BoaAudio.casing(0.085f, 0.45f)));
        this.schedule(0.64f, () -> this.synth().play(BoaAudio.casing(0.045f, 0.5f)));
    }

    private static ToneVoice casing(float gain, float pan) {
        return new ToneVoice(2950.0f, new float[]{1.0f, 1.63f, 2.41f, 3.2f}, new float[]{1.0f, 0.6f, 0.42f, 0.2f}, new float[]{0.16f, 0.1f, 0.07f, 0.05f}, 4.0E-4f, gain, pan, 0.3f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void onFire(int segment, boolean slowMotion, boolean lastShot) {
        this.aiming = false;
        this.lastAim = false;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth().duck(0.78f + 0.05f * (float)segment);
        float weight = lastShot ? 1.15f : 1.0f;
        this.report(weight, lastShot ? 7 : 5);
        if (slowMotion) {
            this.synth().play(new NoiseVoice(1400.0f, 160.0f, 2.2f, true, 0.05f, 0.55f, 0.2f, 0.0f, 0.6f));
            this.synth().play(ToneVoice.sweepDown(220.0f, 0.18f, 0.9f));
        }
    }

    private void report(float weight, int echoes) {
        this.synth().play(new NoiseVoice(7800.0f, 4200.0f, 1.2f, false, 1.0E-4f, 0.0035f, 0.78f * weight, 0.0f, 0.25f));
        this.synth().play(new NoiseVoice(4500.0f, 180.0f, 1.1f, false, 4.0E-4f, 0.085f, 0.95f * weight, 0.0f, 0.55f));
        this.synth().play(new ToneVoice(128.0f, new float[]{1.0f, 1.52f}, new float[]{1.0f, 0.32f}, new float[]{0.11f, 0.05f}, 5.0E-4f, 0.95f * weight, 0.0f, 0.12f, 40.0f, 0.09f, 0.0f, 0.0f));
        this.schedule(0.006f, () -> this.synth().play(ToneVoice.sweepDown(62.0f, 0.52f * weight, 0.42f)));
        this.schedule(0.012f, () -> this.synth().play(new NoiseVoice(4400.0f, 2500.0f, 1.6f, true, 3.0E-4f, 0.018f, 0.48f * weight, 0.08f, 0.3f)));
        this.synth().play(new NoiseVoice(900.0f, 90.0f, 0.7f, false, 0.02f, 0.75f, 0.18f * weight, 0.0f, 0.8f));
        float at = 0.17f;
        for (int i = 0; i < echoes; ++i) {
            float gain = 0.36f * (float)Math.pow(0.64, i) * weight;
            float cut = 1500.0f * (float)Math.pow(0.74, i);
            float pan = (i % 2 == 0 ? -0.5f : 0.5f) * (1.0f - 0.08f * (float)i);
            float decay = 0.13f + 0.055f * (float)i;
            this.schedule(at, () -> this.synth().play(new NoiseVoice(cut, cut * 0.35f, 0.8f, false, 0.004f, decay, gain, pan, 0.7f)));
            at += 0.16f + 0.09f * (float)i;
        }
        this.schedule(at + 0.1f, () -> this.synth().play(new NoiseVoice(420.0f, 130.0f, 0.7f, false, 0.02f, 0.5f, 0.1f * weight, 0.0f, 0.85f)));
    }

    public void onHit(int segment) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        int t = Math.max(0, Math.min(3, segment));
        this.synth().duck(0.45f + 0.1f * (float)t);
        this.synth().play(this.theme.hit(48.0f, 0.72f + 0.06f * (float)t, 0.0f));
        this.synth().play(NoiseVoice.slap(0.42f));
        this.synth().play(NoiseVoice.crash(0.18f + 0.05f * (float)t, 0.8f + 0.2f * (float)t));
        this.synth().play(ToneVoice.sweepDown(72.0f, 0.42f + 0.06f * (float)t, 0.5f));
        this.schedule(0.08f, () -> this.synth().play(NoiseVoice.sizzle(0.2f + 0.04f * (float)t, 1.0f)));
        float hz = this.theme.tierHz(t);
        float[] scale = this.theme.scale();
        int[] chord = this.theme.chordDegrees();
        this.synth().play(this.theme.land(hz, 0.42f + 0.05f * (float)t, 0.0f));
        for (int voice = 1; voice <= t; ++voice) {
            float degree = scale[Math.min(scale.length - 1, chord[Math.min(chord.length - 1, voice)])];
            this.synth().play(this.theme.land(hz * degree, 0.16f + 0.03f * (float)t, voice % 2 == 0 ? 0.28f : -0.28f));
        }
        int grains = 3 + t * 2;
        for (int i = 0; i < grains; ++i) {
            float pitch = hz * scale[Math.min(scale.length - 1, 5 + t + i)];
            float gain = 0.14f - 0.007f * (float)i;
            float pan = i % 2 == 0 ? -0.22f : 0.22f;
            this.schedule(0.08f + (float)i * (0.07f - 0.008f * (float)t), () -> this.synth().play(this.theme.sparkle(pitch, gain, pan)));
        }
    }

    public void onMiss(int segment, boolean near) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float pan = segment % 2 == 0 ? 0.25f : -0.25f;
        this.synth().duck(0.4f);
        this.waterImpact(pan, near ? 1.1f : 1.0f);
        this.synth().play(new NoiseVoice(3200.0f, 5400.0f, 2.2f, true, 0.01f, 0.14f, 0.08f, -pan, 0.25f));
        if (near) {
            this.synth().play(ToneVoice.sag(this.theme.tierHz(Math.min(3, segment)), 0.2f, 0.6f));
        } else {
            this.synth().play(this.theme.deadWood(this.theme.tierHz(Math.min(3, segment)) * 0.75f, 0.3f));
        }
    }

    private void waterImpact(float pan, float size) {
        this.synth().play(new NoiseVoice(5200.0f, 2600.0f, 1.4f, true, 4.0E-4f, 0.012f, 0.44f * size, pan, 0.2f));
        this.synth().play(new ToneVoice(150.0f, new float[]{1.0f}, new float[]{1.0f}, new float[]{0.07f}, 0.001f, 0.34f * size, pan, 0.15f, 62.0f, 0.07f, 0.0f, 0.0f));
        this.synth().play(new ToneVoice(560.0f, new float[]{1.0f, 2.02f}, new float[]{1.0f, 0.18f}, new float[]{0.06f, 0.03f}, 0.002f, 0.36f * size, pan, 0.3f, 1480.0f, 0.05f, 0.0f, 0.0f));
        this.synth().play(new NoiseVoice(6800.0f, 900.0f, 1.2f, false, 0.003f, 0.16f, 0.48f * size, pan, 0.4f));
        this.schedule(0.05f, () -> this.synth().play(new NoiseVoice(7000.0f, 4200.0f, 2.0f, true, 0.02f, 0.22f, 0.07f * size, -pan * 0.5f, 0.35f)));
        for (int i = 0; i < 6; ++i) {
            float when = 0.16f + (float)i * 0.075f + (float)(i % 2) * 0.03f;
            float hz = 1100.0f + (float)(i * 347 % 1300);
            float gain = (0.075f - 0.008f * (float)i) * size;
            float dropPan = pan + (float)(i * 5 % 7 - 3) * 0.12f;
            this.schedule(when, () -> this.synth().play(new ToneVoice(hz, new float[]{1.0f}, new float[]{1.0f}, new float[]{0.03f}, 0.001f, gain, dropPan, 0.3f, hz * 1.8f, 0.025f, 0.0f, 0.0f)));
        }
    }

    public void onOpenHold() {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth().duck(0.95f);
        this.synth().play(NoiseVoice.breath(0.2f, 0.0f, 0.7f));
        this.synth().playBed(ShepardVoice.riser(this.theme.tierHz(3), 0.07f, 0.6f));
    }

    public void onEmpty(boolean headNext, float delay) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        if (!headNext) {
            this.schedule(delay, () -> this.synth().play(NoiseVoice.breath(0.16f, 0.0f, 0.9f)));
            return;
        }
        this.schedule(delay + 0.1f, () -> this.synth().play(new NoiseVoice(2600.0f, 5200.0f, 2.6f, true, 0.08f, 0.35f, 0.2f, 0.0f, 0.3f)));
        this.schedule(delay + 0.18f, () -> this.synth().playBed(ShepardVoice.riser(this.theme.tierHz(3), 0.1f, 1.2f)));
    }

    public void onGlint() {
        this.bed = false;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        if (this.lead) {
            this.synth().silenceBed();
        }
        float hz = this.theme.root() * this.theme.scale()[12] * 2.0f;
        this.synth().play(this.theme.sparkle(hz, 0.22f, 0.0f));
        this.schedule(0.09f, () -> this.synth().play(this.theme.sparkle(hz * 1.5f, 0.16f, 0.1f)));
    }

    public void onPop(float celebration) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth().duck(0.8f);
        float root = this.theme.root();
        this.synth().play(NoiseVoice.slap(0.42f));
        this.synth().play(NoiseVoice.crash(0.26f, 1.4f));
        this.synth().play(this.theme.hit(46.0f, 0.72f, 0.0f));
        this.synth().play(ShepardVoice.riser(root * 2.0f, 0.12f, 0.45f));
    }

    public void onToss(float seconds) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth().play(new NoiseVoice(1800.0f, 5200.0f, 1.6f, true, 0.01f, Math.max(0.12f, seconds * 0.7f), 0.22f, -0.6f, 0.15f));
        this.synth().play(NoiseVoice.detent(0.18f, -0.4f));
        float hz = this.theme.root() * this.theme.scale()[9] * 2.0f;
        this.schedule(0.05f, () -> this.synth().play(this.theme.sparkle(hz, 0.1f, -0.7f)));
    }

    public void onExplode(float celebration, float burst) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.synth().duck(1.0f);
        this.synth().play(new NoiseVoice(6000.0f, 2000.0f, 1.2f, true, 2.0E-4f, 0.012f, 0.85f, 0.0f, 0.2f));
        this.synth().play(new NoiseVoice(1500.0f, 480.0f, 1.4f, true, 0.001f, 0.05f, 0.62f, 0.0f, 0.25f));
        this.synth().play(new ToneVoice(190.0f, new float[]{1.0f, 1.48f}, new float[]{1.0f, 0.4f}, new float[]{0.07f, 0.035f}, 5.0E-4f, 0.8f, 0.0f, 0.1f, 62.0f, 0.05f, 0.0f, 0.0f));
        this.synth().play(new ToneVoice(380.0f, new float[]{1.0f, 2.0f}, new float[]{1.0f, 0.3f}, new float[]{burst, burst}, burst * 0.8f, 0.07f, 0.0f, 0.3f, 1500.0f, burst, 0.0f, 0.0f));
        this.schedule(burst, () -> {
            this.synth().duck(1.0f);
            this.synth().play(new NoiseVoice(7500.0f, 3000.0f, 1.2f, false, 1.0E-4f, 0.006f, 0.8f, 0.0f, 0.3f));
            this.synth().play(new NoiseVoice(4500.0f, 90.0f, 1.1f, false, 6.0E-4f, 0.45f, 0.75f, 0.0f, 0.6f));
            this.synth().play(ToneVoice.sweepDown(80.0f, 0.95f, 1.5f));
            this.synth().play(this.theme.hit(34.0f, 1.0f, 0.0f));
            this.synth().play(NoiseVoice.crash(0.4f, 3.0f));
            this.synth().play(this.theme.accent(0.28f, 0.0f));
            for (int i = 0; i < 14; ++i) {
                float when = 0.22f + (float)i * 0.065f + (float)(i % 3) * 0.021f;
                float gain = 0.12f * (1.0f - (float)i / 16.0f);
                float pan = (float)(i * 7 % 11 - 5) / 6.0f;
                float hz = 1500.0f + (float)(i * 389 % 1400);
                this.schedule(when, () -> this.synth().play(new NoiseVoice(hz, hz * 0.6f, 1.3f, true, 0.001f, 0.045f, gain, pan, 0.3f)));
            }
            this.schedule(0.38f, () -> this.synth().play(new NoiseVoice(1300.0f, 300.0f, 0.8f, false, 0.004f, 0.3f, 0.26f, -0.5f, 0.75f)));
            this.schedule(0.78f, () -> this.synth().play(new NoiseVoice(900.0f, 220.0f, 0.8f, false, 0.004f, 0.4f, 0.14f, 0.5f, 0.8f)));
            this.synth().play(new ToneVoice(3950.0f, new float[]{1.0f, 1.004f}, new float[]{1.0f, 0.8f}, new float[]{1.2f, 1.2f}, 0.05f, 0.03f, 0.0f, 0.05f, 0.0f, 0.0f, 0.0f, 0.0f));
        });
    }

    public void onReveal(float celebration) {
        this.bed = false;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float size = Math.max(0.15f, Math.min(1.0f, celebration));
        this.synth().duck(0.55f + 0.35f * size);
        float root = this.theme.root();
        float[] scale = this.theme.scale();
        int[] chord = this.theme.chordDegrees();
        this.synth().play(ShepardVoice.riser(root * 2.0f, 0.15f * size, 0.5f));
        float chordGain = 0.75f + 0.25f * size;
        this.schedule(0.24f, () -> {
            this.synth().play(this.theme.land(root, 0.52f * chordGain, 0.0f));
            this.synth().play(this.theme.land(root * 2.0f, 0.28f * chordGain, 0.0f));
            for (int i = 0; i < chord.length; ++i) {
                float degree = scale[Math.min(scale.length - 1, chord[i])];
                this.synth().play(this.theme.pad(root * degree * 2.0f, (0.19f - 0.02f * (float)i) * chordGain, i % 2 == 0 ? -0.4f : 0.4f, 2.0f + 1.4f * size));
            }
            this.synth().play(this.theme.bass(root * 0.5f, 0.46f * chordGain, 1.8f + 0.8f * size));
            this.synth().play(this.theme.air(0.18f * size, 0.0f, 3.0f));
        });
        int strikes = Math.max(2, Math.round(6.0f * size));
        for (int i = 0; i < strikes; ++i) {
            float pitch = root * scale[10] * (i % 2 == 0 ? 1.0f : 1.5f);
            float gain = (i == strikes - 1 ? 0.32f : 0.24f) * size;
            this.schedule(0.3f + (float)i * 0.135f, () -> this.synth().play(this.theme.land(pitch, gain, 0.0f)));
        }
        int grains = Math.max(8, Math.round(32.0f * size));
        float span = 1.8f * (0.55f + 0.45f * size);
        for (int i = 0; i < grains; ++i) {
            float t = (float)i / (float)(grains - 1);
            float delay = 0.46f + t * span - 0.3f * t * t * size;
            float pitch = root * scale[Math.max(0, 13 - (int)(t * 7.0f))] * 2.0f;
            float gain = 0.16f * (1.0f - 0.5f * t) * (0.7f + 0.3f * size);
            float pan = (float)(i % 3 - 1) * 0.38f;
            this.schedule(delay, () -> this.synth().play(this.theme.sparkle(pitch, gain, pan)));
        }
    }

    public void onJackpot() {
        this.bed = false;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        this.host.markJackpot();
        Jackpot.play(this.synth(), this.theme);
    }

    public void onJackpotSkipped() {
        this.host.onJackpotSkipped();
    }

    public void onReload(int added) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        for (int i = 0; i < Math.min(4, added); ++i) {
            float hz = this.theme.root() * this.theme.scale()[8 + i] * 2.0f;
            this.schedule((float)i * 0.12f, () -> {
                this.synth().play(NoiseVoice.detent(0.45f, 0.0f));
                this.synth().play(ToneVoice.coin(hz, 0.16f, 0.0f));
            });
        }
    }

    public void onFail(float dive) {
        int i;
        this.bed = false;
        this.aiming = false;
        this.lastAim = false;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float root = this.theme.root();
        this.synth().play(NoiseVoice.detent(0.52f, 0.1f));
        this.schedule(0.42f, () -> this.synth().play(NoiseVoice.detent(0.46f, 0.1f)));
        this.schedule(dive + 0.1f, () -> {
            this.synth().play(ShepardVoice.faller(root, 0.18f, 1.6f));
            this.synth().play(ToneVoice.sag(root, 0.3f, 0.9f));
            this.synth().play(ToneVoice.sag(root * 1.5f, 0.14f, 0.75f));
        });
        this.schedule(dive + 0.3f, () -> this.synth().play(new NoiseVoice(1400.0f, 220.0f, 1.1f, false, 0.01f, 0.5f, 0.3f, 0.0f, 0.4f)));
        for (i = 1; i < 5; ++i) {
            float pan = i % 2 == 0 ? 0.2f : -0.2f;
            this.schedule(dive + 0.33f + (float)i * 0.17f, () -> this.synth().play(new NoiseVoice(900.0f, 260.0f, 1.0f, false, 0.03f, 0.18f, 0.12f, pan, 0.45f)));
        }
        for (i = 0; i < 5; ++i) {
            float when = dive + 1.2f + (float)i * 0.17f;
            float hz = 260.0f + (float)i * 55.0f;
            this.schedule(when, () -> this.synth().play(new ToneVoice(hz, new float[]{1.0f}, new float[]{1.0f}, new float[]{0.05f}, 0.004f, 0.07f, 0.0f, 0.3f, hz * 2.6f, 0.06f, 0.0f, 0.0f)));
        }
        this.schedule(dive + 0.45f, () -> this.synth().play(this.theme.drone(root * 0.5f, 0.14f, 1.2f)));
    }

    public void onEscape(float dive) {
        int i;
        this.bed = false;
        this.aiming = false;
        this.lastAim = false;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        if (this.lead) {
            this.synth().silenceBed();
        }
        this.synth().duck(1.0f);
        float root = this.theme.root();
        this.synth().play(new NoiseVoice(2400.0f, 320.0f, 1.2f, false, 0.01f, 0.45f, 0.44f, -0.2f, 0.4f));
        this.schedule(dive, () -> {
            this.synth().play(new NoiseVoice(1600.0f, 180.0f, 1.1f, false, 0.004f, 0.55f, 0.52f, 0.1f, 0.45f));
            this.synth().play(NoiseVoice.slap(0.26f));
            this.synth().play(this.theme.hit(42.0f, 0.48f, 0.0f));
        });
        this.schedule(dive + 0.12f, () -> {
            this.synth().play(ShepardVoice.faller(root, 0.16f, 1.3f));
            this.synth().play(ToneVoice.sag(root, 0.26f, 0.8f));
        });
        for (i = 1; i < 4; ++i) {
            float pan = i % 2 == 0 ? 0.22f : -0.22f;
            this.schedule(dive + 0.18f + (float)i * 0.11f, () -> this.synth().play(new NoiseVoice(1000.0f, 260.0f, 1.0f, false, 0.02f, 0.2f, 0.14f, pan, 0.45f)));
        }
        for (i = 0; i < 4; ++i) {
            float when = dive + 0.8f + (float)i * 0.15f;
            float hz = 250.0f + (float)i * 60.0f;
            this.schedule(when, () -> this.synth().play(new ToneVoice(hz, new float[]{1.0f}, new float[]{1.0f}, new float[]{0.05f}, 0.004f, 0.07f, 0.0f, 0.3f, hz * 2.6f, 0.06f, 0.0f, 0.0f)));
        }
        this.schedule(dive + 0.4f, () -> this.synth().play(this.theme.drone(root * 0.5f, 0.13f, 1.1f)));
    }

    public void onEnd() {
        this.bed = false;
        this.scoped = false;
        this.aiming = false;
        this.lastAim = false;
    }

    public void onCountTick(float progress) {
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float[] scale = this.theme.scale();
        int degree = Math.min(scale.length - 1, 8 + (int)(progress * 6.0f));
        float hz = this.theme.root() * scale[degree] * 2.0f;
        this.synth().play(this.theme.sparkle(hz, 0.1f, 0.0f));
    }

    public void onStamp() {
        int i;
        if (!this.cfg.soundsEnabled) {
            return;
        }
        float root = this.theme.root();
        float[] scale = this.theme.scale();
        int[] chord = this.theme.chordDegrees();
        this.synth().play(NoiseVoice.slap(0.6f));
        this.synth().play(NoiseVoice.crash(0.3f, 2.4f));
        this.synth().play(this.theme.hit(46.0f, 1.0f, 0.0f));
        this.synth().play(this.theme.accent(0.24f, 0.0f));
        this.synth().play(ToneVoice.sweepDown(76.0f, 0.5f, 0.75f));
        for (i = 0; i < chord.length; ++i) {
            float degree = scale[Math.min(scale.length - 1, chord[i])];
            this.synth().play(this.theme.land(root * degree * 2.0f, 0.3f - 0.05f * (float)i, i % 2 == 0 ? -0.3f : 0.3f));
        }
        this.synth().play(this.theme.pad(root * 2.0f, 0.16f, 0.0f, 2.4f));
        for (i = 0; i < 5; ++i) {
            float pitch = root * scale[Math.min(scale.length - 1, 9 + i)] * 2.0f;
            float pan = i % 2 == 0 ? 0.28f : -0.28f;
            this.schedule(0.05f + (float)i * 0.045f, () -> this.synth().play(this.theme.sparkle(pitch, 0.17f, pan)));
        }
    }

    public void update(float dt, int segment, boolean aimingNow, float aimProgress, boolean hold) {
        float gain;
        this.stage = Math.max(0, Math.min(4, segment));
        if (!(this.bed && this.lead && this.cfg.soundsEnabled)) {
            return;
        }
        float target = this.scoped ? BPM[this.stage] : 100.0f;
        this.bpm += (target - this.bpm) * Math.min(1.0f, dt * 2.2f);
        float stepSeconds = 60.0f / this.bpm / 4.0f;
        this.stepTimer += dt;
        int guard = 0;
        while (this.stepTimer >= stepSeconds && guard++ < 8) {
            this.stepTimer -= stepSeconds;
            if (!hold) {
                if (this.scoped) {
                    this.playStep(this.step);
                } else {
                    this.loaderStep(this.step);
                }
            }
            this.step = (this.step + 1) % 16;
        }
        if (hold) {
            float rate = 72.0f + 70.0f * aimProgress;
            this.heartTimer -= dt;
            if (this.heartTimer <= 0.0f) {
                this.heartTimer = 60.0f / rate;
                final float beat = 0.4f + 0.3f * aimProgress;
                this.synth().play(this.theme.hit(40.0f, beat, 0.0f));
                this.schedule(0.15f, () -> this.synth().play(this.theme.hit(36.0f, beat * 0.7f, 0.0f)));
            }
        } else {
            this.heartTimer = 0.0f;
        }
        if (aimingNow && (this.stage >= 3 || this.lastAim)) {
            this.riserLeft -= dt;
            if (this.riserLeft <= 0.0f) {
                float length = 1.5f;
                this.riserLeft = length * 0.62f;
                gain = (0.1f + 0.05f * (float)Math.max(0, this.stage - 3) + (this.lastAim ? 0.04f : 0.0f)) * (0.5f + 0.5f * aimProgress);
                this.synth().playBed(ShepardVoice.riser(this.theme.tierHz(3), gain, length));
                this.synth().playBed(NoiseVoice.riser(gain * 0.5f, length * 0.8f));
            }
        }
    }

    private void loaderStep(int s) {
        float vol = this.cfg.drumVolume * 0.55f;
        if (s == 0 || s == 8) {
            this.synth().playBed(this.theme.hit(54.0f, 0.55f * vol, 0.0f));
        }
        if (s == 4 || s == 12) {
            this.synth().playBed(this.theme.hat(0.08f * vol, s == 4 ? -0.3f : 0.3f));
        }
    }

    private void playStep(int s) {
        int degree;
        boolean kick;
        int row = this.stage;
        float vol = this.cfg.drumVolume * (0.62f + 0.08f * (float)row);
        float root = this.theme.tierHz(Math.min(3, row)) * 0.5f;
        float[] scale = this.theme.scale();
        kick = switch (row) {
            case 0 -> s == 0 || s == 8;
            case 1 -> s == 0 || s == 6 || s == 8;
            case 2 -> s == 0 || s == 3 || s == 8 || s == 11;
            case 3 -> s == 0 || s == 3 || s == 6 || s == 8 || s == 11 || s == 14;
            default -> s % 2 == 0;
        };
        if (kick) {
            boolean down = s == 0;
            this.synth().playBed(this.theme.hit(down ? 48.0f : 58.0f, (down ? 0.85f : 0.55f) * vol, 0.0f));
        }
        if (row >= 1 && (s == 4 || s == 12)) {
            this.synth().playBed(this.theme.tom(104.0f, 0.42f * vol));
            this.synth().playBed(this.theme.accent(0.11f * vol, s == 4 ? -0.25f : 0.25f));
        }
        if (row >= 2 && s % 2 == 1) {
            this.synth().playBed(this.theme.hat(0.09f * vol, s % 4 == 1 ? -0.35f : 0.35f));
        }
        if (s == 0 || s == 6 || s == 10) {
            float degree2 = s == 6 ? scale[3] : 1.0f;
            this.synth().playBed(this.theme.bass(root * degree2, (s == 0 ? 0.42f : 0.28f) * vol, 0.5f));
        }
        if ((degree = MOTIF[row][s]) >= 0) {
            this.synth().playBed(this.theme.lead(this.theme.tierHz(Math.min(3, row)) * scale[degree], (0.1f + 0.025f * (float)row) * vol, s % 8 < 4 ? -0.3f : 0.3f));
        }
    }

    private void schedule(float delay, Runnable action) {
        this.synth().after(delay, action);
    }
}

