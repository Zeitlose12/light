package com.skymaxx.tiki.synth;

import java.util.Arrays;

public final class Reverb {
    private final float[][] combBuf = new float[4][];
    private final int[] combIdx = new int[4];
    private final float[] combFb = new float[4];
    private final float[] combStore = new float[4];
    private final float[][] apBuf = new float[2][];
    private final int[] apIdx = new int[2];
    private static final float DAMP = 0.28f;
    private static final float WET = 0.34f;

    public Reverb(float sampleRate) {
        int i;
        int[] combLen = new int[]{1687, 1801, 2053, 2251};
        int[] apLen = new int[]{389, 127};
        for (i = 0; i < 4; ++i) {
            this.combBuf[i] = new float[Math.max(16, (int)((float)combLen[i] * sampleRate / 44100.0f))];
            this.combFb[i] = 0.8f - (float)i * 0.015f;
        }
        for (i = 0; i < 2; ++i) {
            this.apBuf[i] = new float[Math.max(8, (int)((float)apLen[i] * sampleRate / 44100.0f))];
        }
    }

    public void reset() {
        for (float[] b : this.combBuf) {
            Arrays.fill(b, 0.0f);
        }
        for (float[] b : this.apBuf) {
            Arrays.fill(b, 0.0f);
        }
        Arrays.fill(this.combStore, 0.0f);
    }

    public void process(float[] send, float[] left, float[] right, int frames) {
        int i = 0;
        while (i < frames) {
            float y;
            int idx;
            float[] buf;
            float in = send[i] * 0.22f;
            float acc = 0.0f;
            for (int c = 0; c < 4; ++c) {
                buf = this.combBuf[c];
                idx = this.combIdx[c];
                y = buf[idx];
                this.combStore[c] = y * 0.72f + this.combStore[c] * 0.28f;
                buf[idx] = in + this.combStore[c] * this.combFb[c];
                this.combIdx[c] = (idx + 1) % buf.length;
                acc += y;
            }
            for (int a = 0; a < 2; ++a) {
                float v;
                buf = this.apBuf[a];
                idx = this.apIdx[a];
                y = buf[idx];
                buf[idx] = v = acc + y * 0.5f;
                this.apIdx[a] = (idx + 1) % buf.length;
                acc = y - v * 0.5f;
            }
            int n = i;
            left[n] = left[n] + acc * 0.34f;
            int n2 = i++;
            right[n2] = right[n2] + acc * 0.34f * 0.86f;
        }
    }
}

