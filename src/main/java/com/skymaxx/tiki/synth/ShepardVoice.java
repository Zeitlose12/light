package com.skymaxx.tiki.synth;

import com.skymaxx.tiki.synth.Voice;
import com.skymaxx.tiki.synth.Waveform;

public final class ShepardVoice
implements Voice {
    private static final int PARTIALS = 6;
    private static final float WINDOW = 1.5f;
    private final float baseHz;
    private final float octavesPerSecond;
    private final float gain;
    private final float pan;
    private final float sendAmount;
    private final float attack;
    private final float sustain;
    private final float release;
    private final float cutoffFrom;
    private final float cutoffTo;
    private final float[] phase = new float[6];
    private final float[] hz = new float[6];
    private final float[] amp = new float[6];
    private final float[] ampAtEnd = new float[6];
    private float low;
    private float band;
    private float time;
    private final float lifetime;

    public ShepardVoice(float baseHz, float octavesPerSecond, float gain, float pan, float send, float attack, float sustain, float release, float cutoffFrom, float cutoffTo) {
        this.baseHz = Math.max(8.0f, baseHz);
        this.octavesPerSecond = octavesPerSecond;
        this.gain = gain;
        this.pan = pan;
        this.sendAmount = send;
        this.attack = Math.max(0.001f, attack);
        this.sustain = Math.max(0.0f, sustain);
        this.release = Math.max(0.01f, release);
        this.cutoffFrom = cutoffFrom;
        this.cutoffTo = cutoffTo;
        this.lifetime = this.attack + this.sustain + this.release;
        for (int p = 0; p < 6; ++p) {
            this.phase[p] = (float)p * 0.137f;
        }
    }

    @Override
    public boolean render(float[] left, float[] right, float[] send, int frames, float sampleRate) {
        if (this.time >= this.lifetime) {
            return false;
        }
        float climb = this.time * this.octavesPerSecond;
        float blockSeconds = (float)frames / sampleRate;
        for (int p = 0; p < 6; ++p) {
            float octave = ShepardVoice.wrap((float)p + climb);
            this.hz[p] = this.baseHz * ShepardVoice.exp2(octave);
            this.amp[p] = ShepardVoice.window(octave);
            this.ampAtEnd[p] = ShepardVoice.window(ShepardVoice.wrap((float)p + (this.time + blockSeconds) * this.octavesPerSecond));
        }
        float dt = 1.0f / sampleRate;
        float lGain = this.gain * (1.0f - Math.max(0.0f, this.pan));
        float rGain = this.gain * (1.0f + Math.min(0.0f, this.pan));
        float step = 1.0f / (float)frames;
        float fStart = ShepardVoice.coefficient(this.cutoffAt(this.time), sampleRate);
        float fEnd = ShepardVoice.coefficient(this.cutoffAt(this.time + blockSeconds), sampleRate);
        int i = 0;
        while (i < frames) {
            if (this.time >= this.lifetime) {
                return false;
            }
            float env = this.envelope();
            float k = (float)i * step;
            float sample = 0.0f;
            for (int p = 0; p < 6; ++p) {
                float a = this.amp[p] + (this.ampAtEnd[p] - this.amp[p]) * k;
                if (a < 1.0E-4f) continue;
                int n = p;
                this.phase[n] = this.phase[n] + this.hz[p] * dt;
                if (this.phase[p] > 1.0f) {
                    int n2 = p;
                    this.phase[n2] = this.phase[n2] - (float)Math.floor(this.phase[p]);
                }
                sample += Waveform.sine(this.phase[p]) * a;
            }
            float f = fStart + (fEnd - fStart) * k;
            float high = sample - this.low - 0.9f * this.band;
            this.band += f * high;
            this.low += f * this.band;
            float out = this.low * env;
            int n = i;
            left[n] = left[n] + out * lGain;
            int n3 = i;
            right[n3] = right[n3] + out * rGain;
            int n4 = i++;
            send[n4] = send[n4] + out * this.sendAmount;
            this.time += dt;
        }
        return true;
    }

    private float envelope() {
        if (this.time < this.attack) {
            float t = this.time / this.attack;
            return t * t;
        }
        if (this.time < this.attack + this.sustain) {
            return 1.0f;
        }
        float t = (this.time - this.attack - this.sustain) / this.release;
        return t >= 1.0f ? 0.0f : (1.0f - t) * (1.0f - t);
    }

    private float cutoffAt(float t) {
        float progress = Math.min(1.0f, Math.max(0.0f, t / this.lifetime));
        return this.cutoffFrom + (this.cutoffTo - this.cutoffFrom) * progress;
    }

    private static float coefficient(float cutoff, float sampleRate) {
        return 2.0f * Waveform.sine(Math.min(0.45f, cutoff / sampleRate) * 0.5f);
    }

    private static float window(float octave) {
        float d = (octave - 3.0f) / 1.5f;
        float g = 1.0f - d * d * 0.25f;
        return g <= 0.0f ? 0.0f : g * g;
    }

    private static float wrap(float octave) {
        float w = octave % 6.0f;
        return w < 0.0f ? w + 6.0f : w;
    }

    private static float exp2(float x) {
        int whole = (int)Math.floor(x);
        float frac = x - (float)whole;
        float f = 1.0f + frac * (0.6934f + frac * (0.2416f + frac * 0.065f));
        return f * (whole >= 0 ? (float)(1 << whole) : 1.0f / (float)(1 << -whole));
    }

    public static ShepardVoice riser(float baseHz, float gain, float seconds) {
        return new ShepardVoice(baseHz * 0.25f, 0.62f, gain, 0.0f, 0.42f, seconds * 0.45f, seconds * 0.4f, seconds * 0.5f, 420.0f, 5200.0f);
    }

    public static ShepardVoice faller(float baseHz, float gain, float seconds) {
        return new ShepardVoice(baseHz * 0.25f, -0.75f, gain, 0.0f, 0.5f, 0.02f, seconds * 0.5f, seconds * 0.6f, 3200.0f, 380.0f);
    }
}

