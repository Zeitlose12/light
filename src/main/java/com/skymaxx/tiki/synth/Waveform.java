package com.skymaxx.tiki.synth;

public final class Waveform {
    private static final int BITS = 12;
    private static final int SIZE = 4096;
    private static final int MASK = 4095;
    private static final float[] SINE = new float[4097];

    private Waveform() {
    }

    public static float sine(float turns) {
        float wrapped = turns - (float)Math.floor(turns);
        float scaled = wrapped * 4096.0f;
        int index = (int)scaled;
        float frac = scaled - (float)index;
        return SINE[index &= 0xFFF] + (SINE[index + 1] - SINE[index]) * frac;
    }

    public static float decayPerSample(float seconds, float sampleRate) {
        return (float)Math.exp(-1.0 / (double)(Math.max(0.005f, seconds) * sampleRate));
    }

    static {
        for (int i = 0; i <= 4096; ++i) {
            Waveform.SINE[i] = (float)Math.sin(Math.PI * 2 * (double)i / 4096.0);
        }
    }
}

