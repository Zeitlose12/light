package com.skymaxx.tiki.synth;

import com.skymaxx.tiki.synth.Voice;
import com.skymaxx.tiki.synth.Waveform;

public class ToneVoice
implements Voice {
    private final float[] ratios;
    private final float[] amps;
    private final float[] decays;
    private final float[] phase;
    private final float[] level;
    private final float[] falloff;
    private final float baseHz;
    private final float glideToHz;
    private final float glideSeconds;
    private final float attack;
    private final float gain;
    private final float pan;
    private final float sendAmount;
    private final float vibratoHz;
    private final float vibratoDepth;
    private float time;
    private final float lifetime;
    private boolean prepared;

    public ToneVoice(float baseHz, float[] ratios, float[] amps, float[] decays, float attack, float gain, float pan, float send, float glideToHz, float glideSeconds, float vibratoHz, float vibratoDepth) {
        this.baseHz = baseHz;
        this.ratios = ratios;
        this.amps = amps;
        this.decays = decays;
        this.phase = new float[ratios.length];
        this.level = new float[ratios.length];
        this.falloff = new float[ratios.length];
        this.attack = Math.max(5.0E-4f, attack);
        this.gain = gain;
        this.pan = pan;
        this.sendAmount = send;
        this.glideToHz = glideToHz;
        this.glideSeconds = glideSeconds;
        this.vibratoHz = vibratoHz;
        this.vibratoDepth = vibratoDepth;
        float longest = 0.0f;
        for (float d : decays) {
            longest = Math.max(longest, d);
        }
        this.lifetime = this.attack + longest * 3.2f;
    }

    @Override
    public boolean render(float[] left, float[] right, float[] send, int frames, float sampleRate) {
        if (!this.prepared) {
            this.prepared = true;
            for (int p = 0; p < this.ratios.length; ++p) {
                this.level[p] = this.amps[p];
                this.falloff[p] = Waveform.decayPerSample(this.decays[p], sampleRate);
            }
        }
        float dt = 1.0f / sampleRate;
        float lGain = this.gain * (1.0f - Math.max(0.0f, this.pan));
        float rGain = this.gain * (1.0f + Math.min(0.0f, this.pan));
        int i = 0;
        while (i < frames) {
            if (this.time >= this.lifetime) {
                return false;
            }
            float env = this.time < this.attack ? this.time / this.attack : 1.0f;
            float hz = this.baseHz;
            if (this.glideSeconds > 0.0f) {
                if (this.time < this.glideSeconds) {
                    float k = this.time / this.glideSeconds;
                    hz = this.baseHz + (this.glideToHz - this.baseHz) * (k * k);
                } else {
                    hz = this.glideToHz;
                }
            }
            if (this.vibratoDepth > 0.0f) {
                hz *= 1.0f + this.vibratoDepth * Waveform.sine(this.vibratoHz * this.time);
            }
            float sample = 0.0f;
            for (int p = 0; p < this.ratios.length; ++p) {
                float amplitude = this.level[p];
                this.level[p] = amplitude * this.falloff[p];
                if (amplitude < 1.0E-4f) continue;
                int n = p;
                this.phase[n] = this.phase[n] + hz * this.ratios[p] * dt;
                if (this.phase[p] > 1.0f) {
                    int n2 = p;
                    this.phase[n2] = this.phase[n2] - (float)Math.floor(this.phase[p]);
                }
                sample += Waveform.sine(this.phase[p]) * amplitude;
            }
            int n = i;
            left[n] = left[n] + (sample *= env) * lGain;
            int n3 = i;
            right[n3] = right[n3] + sample * rGain;
            int n4 = i++;
            send[n4] = send[n4] + sample * this.sendAmount;
            this.time += dt;
        }
        return true;
    }

    public static ToneVoice gong(float hz, float gain, float pan) {
        return new ToneVoice(hz, new float[]{1.0f, 2.76f, 5.4f, 8.93f, 13.34f}, new float[]{1.0f, 0.62f, 0.38f, 0.22f, 0.12f}, new float[]{2.6f, 1.7f, 1.1f, 0.7f, 0.45f}, 0.004f, gain, pan, 0.62f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static ToneVoice bell(float hz, float gain, float pan) {
        return new ToneVoice(hz, new float[]{1.0f, 2.0f, 3.01f, 4.17f, 5.43f}, new float[]{1.0f, 0.45f, 0.28f, 0.16f, 0.09f}, new float[]{1.5f, 1.0f, 0.7f, 0.5f, 0.35f}, 0.003f, gain, pan, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static ToneVoice choir(float hz, float gain, float pan, float seconds) {
        return new ToneVoice(hz, new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f}, new float[]{1.0f, 0.5f, 0.33f, 0.2f, 0.12f, 0.07f}, new float[]{seconds, seconds, seconds * 0.9f, seconds * 0.8f, seconds * 0.7f, seconds * 0.6f}, 0.35f, gain, pan, 0.55f, 0.0f, 0.0f, 5.2f, 0.004f);
    }

    public static ToneVoice drum(float hz, float gain) {
        return new ToneVoice(hz * 2.2f, new float[]{1.0f, 1.6f}, new float[]{1.0f, 0.18f}, new float[]{0.3f, 0.07f}, 0.001f, gain, 0.0f, 0.16f, hz, 0.045f, 0.0f, 0.0f);
    }

    public static ToneVoice taiko(float hz, float gain, float pan) {
        return new ToneVoice(hz * 2.6f, new float[]{1.0f, 1.5f, 2.9f}, new float[]{1.0f, 0.34f, 0.14f}, new float[]{0.46f, 0.12f, 0.045f}, 8.0E-4f, gain, pan, 0.22f, hz, 0.055f, 0.0f, 0.0f);
    }

    public static ToneVoice logDrum(float hz, float gain, float pan) {
        return new ToneVoice(hz, new float[]{1.0f, 2.01f, 3.02f, 4.94f}, new float[]{1.0f, 0.34f, 0.16f, 0.06f}, new float[]{0.62f, 0.3f, 0.18f, 0.09f}, 0.002f, gain, pan, 0.34f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static ToneVoice bass(float hz, float gain, float seconds) {
        return new ToneVoice(hz, new float[]{1.0f, 2.0f, 3.0f}, new float[]{1.0f, 0.22f, 0.07f}, new float[]{seconds, seconds * 0.5f, seconds * 0.3f}, 0.006f, gain, 0.0f, 0.12f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static ToneVoice woodBlock(float hz, float gain) {
        return new ToneVoice(hz, new float[]{1.0f, 1.58f, 2.31f, 3.05f}, new float[]{1.0f, 0.55f, 0.3f, 0.15f}, new float[]{0.11f, 0.08f, 0.055f, 0.04f}, 8.0E-4f, gain, 0.0f, 0.14f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static ToneVoice drone(float hz, float gain, float seconds) {
        return new ToneVoice(hz, new float[]{1.0f, 2.0f, 2.99f, 4.02f}, new float[]{1.0f, 0.34f, 0.16f, 0.07f}, new float[]{seconds, seconds * 0.8f, seconds * 0.6f, seconds * 0.5f}, 0.5f, gain, 0.0f, 0.45f, 0.0f, 0.0f, 3.1f, 0.003f);
    }

    public static ToneVoice coin(float hz, float gain, float pan) {
        return new ToneVoice(hz, new float[]{1.0f, 2.41f, 3.86f, 6.11f}, new float[]{1.0f, 0.72f, 0.44f, 0.22f}, new float[]{0.1f, 0.075f, 0.05f, 0.035f}, 6.0E-4f, gain, pan, 0.34f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static ToneVoice ring(float hz, float gain, float pan) {
        return new ToneVoice(hz, new float[]{1.0f, 2.05f, 3.42f, 4.81f, 6.63f}, new float[]{1.0f, 0.66f, 0.42f, 0.26f, 0.14f}, new float[]{0.85f, 0.55f, 0.38f, 0.26f, 0.16f}, 0.0015f, gain, pan, 0.55f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static ToneVoice horn(float hz, float gain, float pan, float seconds) {
        return new ToneVoice(hz, new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f}, new float[]{1.0f, 0.78f, 0.52f, 0.34f, 0.21f, 0.12f, 0.07f}, new float[]{seconds, seconds * 0.9f, seconds * 0.8f, seconds * 0.7f, seconds * 0.6f, seconds * 0.5f, seconds * 0.4f}, 0.07f, gain, pan, 0.38f, 0.0f, 0.0f, 5.6f, 0.0035f);
    }

    public static ToneVoice pluck(float hz, float gain, float pan) {
        return new ToneVoice(hz, new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f}, new float[]{1.0f, 0.42f, 0.24f, 0.11f, 0.05f}, new float[]{0.42f, 0.24f, 0.16f, 0.1f, 0.06f}, 0.0012f, gain, pan, 0.3f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static ToneVoice riser(float hz, float gain, float seconds) {
        return new ToneVoice(hz, new float[]{1.0f, 2.0f, 3.0f}, new float[]{1.0f, 0.34f, 0.14f}, new float[]{seconds * 1.6f, seconds * 1.3f, seconds}, seconds * 0.55f, gain, 0.0f, 0.45f, hz * 1.5f, seconds, 6.5f, 0.006f);
    }

    public static ToneVoice sub(float hz, float gain, float seconds) {
        return new ToneVoice(hz, new float[]{1.0f, 2.0f}, new float[]{1.0f, 0.12f}, new float[]{seconds, seconds * 0.4f}, seconds * 0.35f, gain, 0.0f, 0.1f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static ToneVoice sweepDown(float hz, float gain, float seconds) {
        return new ToneVoice(hz, new float[]{1.0f, 2.0f}, new float[]{1.0f, 0.1f}, new float[]{seconds, seconds * 0.35f}, 0.0015f, gain, 0.0f, 0.08f, hz * 0.25f, seconds * 0.85f, 0.0f, 0.0f);
    }

    public static ToneVoice sag(float hz, float gain, float seconds) {
        return new ToneVoice(hz, new float[]{1.0f, 2.0f, 3.01f}, new float[]{1.0f, 0.3f, 0.12f}, new float[]{seconds, seconds * 0.7f, seconds * 0.5f}, 0.02f, gain, 0.0f, 0.4f, hz * 0.75f, seconds * 0.8f, 0.0f, 0.0f);
    }
}

