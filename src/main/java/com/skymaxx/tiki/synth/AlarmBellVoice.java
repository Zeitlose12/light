package com.skymaxx.tiki.synth;

import com.skymaxx.tiki.synth.Voice;
import com.skymaxx.tiki.synth.Waveform;

public final class AlarmBellVoice
implements Voice {
    private static final float[] RATIOS = new float[]{1.0f, 2.76f, 5.4f, 8.93f};
    private static final float[] AMPS = new float[]{1.0f, 0.62f, 0.34f, 0.18f};
    private static final float AMP_SUM = 2.14f;
    private final float hz;
    private final float seconds;
    private final float strikesPerSecond;
    private final float gain;
    private final float send;
    private final float[] phaseA = new float[RATIOS.length];
    private final float[] phaseB = new float[RATIOS.length];
    private final float[] levelA = new float[RATIOS.length];
    private final float[] levelB = new float[RATIOS.length];
    private float time;
    private float untilStrike;
    private boolean strikeA = true;
    private float click;
    private int noise = 1013904242;

    public AlarmBellVoice(float hz, float seconds, float strikesPerSecond, float gain, float send) {
        this.hz = hz;
        this.seconds = seconds;
        this.strikesPerSecond = strikesPerSecond;
        this.gain = gain;
        this.send = send;
    }

    @Override
    public boolean render(float[] left, float[] right, float[] sendBus, int frames, float sampleRate) {
        float dt = 1.0f / sampleRate;
        float hitDecay = Waveform.decayPerSample(0.085f, sampleRate);
        float tailDecay = Waveform.decayPerSample(0.9f, sampleRate);
        float clickDecay = Waveform.decayPerSample(0.004f, sampleRate);
        float lifetime = this.seconds + 3.0f;
        int i = 0;
        while (i < frames) {
            boolean ringing;
            if (this.time >= lifetime) {
                return false;
            }
            boolean bl = ringing = this.time < this.seconds;
            if (ringing) {
                this.untilStrike -= dt;
                if (this.untilStrike <= 0.0f) {
                    float wobble = 1.0f + 0.08f * Waveform.sine(1.7f * this.time);
                    this.untilStrike += 1.0f / (this.strikesPerSecond * wobble);
                    float[] level = this.strikeA ? this.levelA : this.levelB;
                    for (int p = 0; p < RATIOS.length; ++p) {
                        level[p] = AMPS[p];
                    }
                    this.strikeA = !this.strikeA;
                    this.click = 1.0f;
                }
            }
            float decay = ringing ? hitDecay : tailDecay;
            float a = 0.0f;
            float b = 0.0f;
            for (int p = 0; p < RATIOS.length; ++p) {
                int n = p;
                this.phaseA[n] = this.phaseA[n] + this.hz * RATIOS[p] * dt;
                if (this.phaseA[p] > 1.0f) {
                    int n2 = p;
                    this.phaseA[n2] = this.phaseA[n2] - (float)Math.floor(this.phaseA[p]);
                }
                int n3 = p;
                this.phaseB[n3] = this.phaseB[n3] + this.hz * 1.189f * RATIOS[p] * dt;
                if (this.phaseB[p] > 1.0f) {
                    int n4 = p;
                    this.phaseB[n4] = this.phaseB[n4] - (float)Math.floor(this.phaseB[p]);
                }
                a += Waveform.sine(this.phaseA[p]) * this.levelA[p];
                b += Waveform.sine(this.phaseB[p]) * this.levelB[p];
                this.levelA[p] = Math.max(this.levelA[p] * decay, ringing ? AMPS[p] * 0.22f : 0.0f);
                this.levelB[p] = Math.max(this.levelB[p] * decay, ringing ? AMPS[p] * 0.22f : 0.0f);
            }
            a /= 2.14f;
            b /= 2.14f;
            this.noise ^= this.noise << 13;
            this.noise ^= this.noise >>> 17;
            this.noise ^= this.noise << 5;
            float tick = (float)this.noise * 4.656613E-10f * this.click * 0.35f;
            this.click *= clickDecay;
            float fadeIn = Math.min(1.0f, this.time / 0.01f);
            float l = (a * 0.85f + b * 0.45f + tick) * fadeIn;
            float r = (b * 0.85f + a * 0.45f + tick) * fadeIn;
            int n = i;
            left[n] = left[n] + l * this.gain;
            int n5 = i;
            right[n5] = right[n5] + r * this.gain;
            int n6 = i++;
            sendBus[n6] = sendBus[n6] + (l + r) * 0.5f * this.send;
            this.time += dt;
        }
        return true;
    }
}

