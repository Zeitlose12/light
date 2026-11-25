package com.skymaxx.tiki.synth;

import com.skymaxx.tiki.synth.Voice;
import com.skymaxx.tiki.synth.Waveform;
import java.util.concurrent.atomic.AtomicInteger;

public final class DeathWhistleVoice
implements Voice {
    private static final int COEFFICIENT_EVERY = 16;
    private static final float RESONANCE = 14.0f;
    private static final float LEAP = 0.035f;
    private static final float HANDOVER_FROM = 0.6f;
    private static final float HANDOVER_TO = 0.74f;
    private static final float SURGE_AT = 0.95f;
    private static final float RELEASE_SECONDS = 0.2f;
    private static final float LEAP_FROM = 0.58f;
    private static final float SHRIEK_DROOP = 0.035f;
    private static final float MOAN_SAG = 0.1f;
    private static final float RELEASE_FALL = 0.2f;
    private static final float RASP_HZ = 66.0f;
    private static final float RASP_DEPTH = 0.8f;
    private static final float AIR_DARKEN = 0.3f;
    private static final float SHRIEK_AIR_SCALE = 0.01f;
    private static final float MOAN_AIR_SCALE = 0.05f;
    private final float shriekHz;
    private final float moanHz;
    private final float seconds;
    private final float gain;
    private final float pan;
    private final float sendAmount;
    private final float drive;
    private final float driveNorm;
    private float time;
    private float shriekPhase;
    private float shriekOctave;
    private float moanPhase;
    private float moanOctave;
    private float raspPhase;
    private float pitchDrift;
    private float raspDrift;
    private float flicker;
    private float breathLow;
    private final float[] shriekBand = new float[2];
    private final float[] shriekOuter = new float[2];
    private final float[] moanBand = new float[2];
    private float shriekCoefficient;
    private float moanCoefficient;
    private int sinceCoefficient;
    private int noise;
    private static final AtomicInteger SEED = new AtomicInteger(625341585);

    public DeathWhistleVoice(float shriekHz, float moanHz, float seconds, float gain, float pan, float send) {
        this(shriekHz, moanHz, seconds, gain, pan, send, 0.0f);
    }

    public DeathWhistleVoice(float shriekHz, float moanHz, float seconds, float gain, float pan, float send, float grit) {
        this.shriekHz = shriekHz;
        this.moanHz = moanHz;
        this.seconds = Math.max(0.1f, seconds);
        this.gain = gain;
        this.pan = pan;
        this.sendAmount = send;
        this.drive = 1.0f + 3.0f * Math.max(0.0f, Math.min(1.0f, grit));
        this.driveNorm = (float)Math.tanh(this.drive);
        this.noise = SEED.addAndGet(1831565813) | 1;
    }

    @Override
    public boolean render(float[] left, float[] right, float[] send, int frames, float sampleRate) {
        float dt = 1.0f / sampleRate;
        float lifetime = this.seconds + 0.2f;
        float lGain = this.gain * (1.0f - Math.max(0.0f, this.pan));
        float rGain = this.gain * (1.0f + Math.min(0.0f, this.pan));
        float q = 0.071428575f;
        int i = 0;
        while (i < frames) {
            if (this.time >= lifetime) {
                return false;
            }
            float u = Math.min(1.0f, this.time / this.seconds);
            float out = this.time > this.seconds ? (this.time - this.seconds) / 0.2f : 0.0f;
            this.noise ^= this.noise << 13;
            this.noise ^= this.noise >>> 17;
            this.noise ^= this.noise << 5;
            float white = (float)this.noise * 4.656613E-10f;
            this.pitchDrift += (white - this.pitchDrift) * 0.0016f;
            this.raspDrift += (white - this.raspDrift) * 9.0E-4f;
            this.flicker += (white - this.flicker) * 0.0045f;
            float leap = u < 0.035f ? 1.0f - (1.0f - u / 0.035f) * (1.0f - u / 0.035f) : 1.0f;
            float droop = DeathWhistleVoice.smooth((u - 0.035f) / 0.705f);
            float fall = 1.0f - 0.2f * out;
            float drift = 1.0f + 0.7f * this.pitchDrift;
            float hiHz = this.shriekHz * (0.58f + 0.42000002f * leap) * (1.0f - 0.035f * droop) * drift * fall;
            float loHz = this.moanHz * (1.0f - 0.1f * DeathWhistleVoice.smooth((u - 0.6f) / 0.39999998f)) * (1.0f + 0.5f * this.pitchDrift) * fall;
            float handover = DeathWhistleVoice.smooth((u - 0.6f) / 0.13999999f);
            float shriekLevel = Math.min(1.0f, u / 0.035f) * (1.0f - 0.84f * handover);
            float moanLevel = u < 0.035f ? 0.85f - 0.69f * (u / 0.035f) : 0.16f + 0.84f * handover;
            float body = 1.0f - 0.38f * DeathWhistleVoice.smooth(u / 0.55f);
            float surge = 0.3f * DeathWhistleVoice.bump((u - 0.95f) / 0.07f);
            float attack = Math.min(1.0f, this.time / 0.045f);
            float release = out > 0.0f ? (1.0f - out) * (1.0f - out) : 1.0f;
            float turbulence = Math.max(0.35f, 1.0f + 9.0f * this.flicker);
            float env = attack * (body + surge) * release * turbulence;
            this.shriekPhase = DeathWhistleVoice.wrap(this.shriekPhase + hiHz * dt);
            this.shriekOctave = DeathWhistleVoice.wrap(this.shriekOctave + 2.0f * hiHz * dt);
            this.raspPhase = DeathWhistleVoice.wrap(this.raspPhase + 66.0f * (1.0f + 14.0f * this.raspDrift) * dt);
            float rasp = 1.0f - 0.8f * (0.5f + 0.5f * Waveform.sine(this.raspPhase));
            float shriekTone = (Waveform.sine(this.shriekPhase) + 0.08f * Waveform.sine(this.shriekOctave)) * rasp;
            this.moanPhase = DeathWhistleVoice.wrap(this.moanPhase + loHz * dt);
            this.moanOctave = DeathWhistleVoice.wrap(this.moanOctave + 2.0f * loHz * dt);
            float moanTone = Waveform.sine(this.moanPhase) + 0.22f * Waveform.sine(this.moanOctave);
            if (this.sinceCoefficient-- <= 0) {
                this.sinceCoefficient = 16;
                this.shriekCoefficient = 2.0f * (float)Math.sin(Math.PI * (double)Math.min(0.11f, hiHz / sampleRate));
                this.moanCoefficient = 2.0f * (float)Math.sin(Math.PI * (double)Math.min(0.11f, loHz / sampleRate));
            }
            this.breathLow += (white - this.breathLow) * 0.3f;
            float shriekAir = DeathWhistleVoice.band(this.shriekOuter, DeathWhistleVoice.band(this.shriekBand, this.breathLow, this.shriekCoefficient, q), this.shriekCoefficient, q) * 0.01f;
            float moanAir = DeathWhistleVoice.band(this.moanBand, this.breathLow, this.moanCoefficient, q) * 0.05f;
            float sample = env * (shriekLevel * (0.62f * shriekTone + shriekAir) + moanLevel * (0.55f * moanTone + moanAir));
            if (this.drive > 1.0f) {
                sample = (float)Math.tanh(sample * this.drive) / this.driveNorm;
            }
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

    private static float band(float[] state, float in, float coefficient, float q) {
        float high = in - state[0] - q * state[1];
        state[1] = state[1] + coefficient * high;
        state[0] = state[0] + coefficient * state[1];
        return state[1];
    }

    private static float smooth(float x) {
        x = x < 0.0f ? 0.0f : (x > 1.0f ? 1.0f : x);
        return x * x * (3.0f - 2.0f * x);
    }

    private static float bump(float x) {
        float a = Math.abs(x);
        return a >= 1.0f ? 0.0f : 1.0f - DeathWhistleVoice.smooth(a);
    }

    private static float wrap(float phase) {
        return phase >= 1.0f ? phase - (float)Math.floor(phase) : phase;
    }
}

