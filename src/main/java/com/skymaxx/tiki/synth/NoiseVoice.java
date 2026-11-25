package com.skymaxx.tiki.synth;

import com.skymaxx.tiki.synth.Voice;
import com.skymaxx.tiki.synth.Waveform;
import java.util.concurrent.atomic.AtomicInteger;

public class NoiseVoice
implements Voice {
    private final float cutoffStart;
    private final float cutoffEnd;
    private final float resonance;
    private final boolean bandpass;
    private final float attack;
    private final float decay;
    private final float gain;
    private final float pan;
    private final float sendAmount;
    private float low;
    private float band;
    private float time;
    private int noise;
    private static final AtomicInteger SEED = new AtomicInteger(-1640531527);
    private boolean prepared;
    private float coefficientStart;
    private float coefficientEnd;
    private float envelopeFalloff;
    private float envelope = 1.0f;

    public NoiseVoice(float cutoffStart, float cutoffEnd, float resonance, boolean bandpass, float attack, float decay, float gain, float pan, float send) {
        this.cutoffStart = cutoffStart;
        this.cutoffEnd = cutoffEnd;
        this.resonance = resonance;
        this.bandpass = bandpass;
        this.attack = Math.max(5.0E-4f, attack);
        this.decay = decay;
        this.gain = gain;
        this.pan = pan;
        this.sendAmount = send;
        this.noise = SEED.addAndGet(1831565813) | 1;
    }

    @Override
    public boolean render(float[] left, float[] right, float[] send, int frames, float sampleRate) {
        if (!this.prepared) {
            this.prepared = true;
            this.coefficientStart = NoiseVoice.coefficient(this.cutoffStart, sampleRate);
            this.coefficientEnd = NoiseVoice.coefficient(this.cutoffEnd, sampleRate);
            this.envelopeFalloff = Waveform.decayPerSample(this.decay, sampleRate);
        }
        float dt = 1.0f / sampleRate;
        float lifetime = this.attack + this.decay * 3.0f;
        float lGain = this.gain * (1.0f - Math.max(0.0f, this.pan));
        float rGain = this.gain * (1.0f + Math.min(0.0f, this.pan));
        float q = 1.0f / Math.max(0.5f, this.resonance);
        int i = 0;
        while (i < frames) {
            float env;
            if (this.time >= lifetime) {
                return false;
            }
            if (this.time < this.attack) {
                env = this.time / this.attack;
            } else {
                this.envelope *= this.envelopeFalloff;
                env = this.envelope;
                if (env < 1.0E-4f) {
                    return false;
                }
            }
            float k = Math.min(1.0f, this.time / Math.max(0.01f, lifetime));
            float f = this.coefficientStart + (this.coefficientEnd - this.coefficientStart) * k;
            this.noise ^= this.noise << 13;
            this.noise ^= this.noise >>> 17;
            this.noise ^= this.noise << 5;
            float input = (float)this.noise * 4.656613E-10f;
            float high = input - this.low - q * this.band;
            this.band += f * high;
            this.low += f * this.band;
            float sample = (this.bandpass ? this.band : this.low) * env;
            int n = i;
            left[n] = left[n] + sample * lGain;
            int n2 = i;
            right[n2] = right[n2] + sample * rGain;
            int n3 = i++;
            send[n3] = send[n3] + sample * this.sendAmount;
            this.time += dt;
        }
        return true;
    }

    private static float coefficient(float cutoff, float sampleRate) {
        return 2.0f * (float)Math.sin(Math.PI * (double)Math.min(0.45f, cutoff / sampleRate));
    }

    public static NoiseVoice shaker(float gain, float pan) {
        return new NoiseVoice(5200.0f, 7400.0f, 1.1f, true, 0.001f, 0.035f, gain, pan, 0.2f);
    }

    public static NoiseVoice whoosh(float gain, float seconds) {
        return new NoiseVoice(380.0f, 1900.0f, 2.4f, true, seconds * 0.35f, seconds * 0.4f, gain, 0.0f, 0.5f);
    }

    public static NoiseVoice breath(float gain, float pan, float seconds) {
        return new NoiseVoice(900.0f, 320.0f, 1.6f, true, seconds * 0.4f, seconds * 0.6f, gain, pan, 0.7f);
    }

    public static NoiseVoice slap(float gain) {
        return new NoiseVoice(2600.0f, 900.0f, 1.2f, true, 6.0E-4f, 0.05f, gain, 0.0f, 0.3f);
    }

    public static NoiseVoice crack(float gain, float pan) {
        return new NoiseVoice(3400.0f, 1400.0f, 1.4f, true, 6.0E-4f, 0.075f, gain, pan, 0.24f);
    }

    public static NoiseVoice riser(float gain, float seconds) {
        return new NoiseVoice(260.0f, 6400.0f, 2.8f, true, seconds * 0.8f, seconds * 0.3f, gain, 0.0f, 0.45f);
    }

    public static NoiseVoice detent(float gain, float pan) {
        return new NoiseVoice(5600.0f, 2200.0f, 1.8f, true, 4.0E-4f, 0.022f, gain, pan, 0.16f);
    }

    public static NoiseVoice crash(float gain, float seconds) {
        return new NoiseVoice(8200.0f, 700.0f, 1.5f, true, 8.0E-4f, seconds * 0.42f, gain, 0.0f, 0.62f);
    }

    public static NoiseVoice sizzle(float gain, float seconds) {
        return new NoiseVoice(6800.0f, 4200.0f, 1.2f, true, seconds * 0.15f, seconds * 0.5f, gain, 0.0f, 0.4f);
    }
}

