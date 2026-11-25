package com.skymaxx.tiki.anim;

import com.skymaxx.tiki.anim.ReelProfile;

public final class SpinCurve {
    private static final float ROCK_DWELL = 0.35f;
    private static final float CORNER_FRACTION = 0.5f;
    private final float[] times;
    private final float[] values;
    private final float[] tangents;
    private final boolean[] rocking;
    private final float totalDegrees;
    private final float judderDegrees;
    private final float judderFrequency;
    private final float judderDecay;

    private SpinCurve(float[] times, float[] values, float totalDegrees, float judderDegrees, float judderFrequency, float judderDecay) {
        this.times = times;
        this.values = values;
        this.totalDegrees = totalDegrees;
        this.judderDegrees = judderDegrees;
        this.judderFrequency = judderFrequency;
        this.judderDecay = judderDecay;
        this.tangents = SpinCurve.computeTangents(times, values);
        this.rocking = new boolean[times.length];
        this.markTurningPoints();
    }

    private void markTurningPoints() {
        int i;
        int n = this.times.length;
        boolean[] turning = new boolean[n];
        for (i = 1; i < n - 1; ++i) {
            boolean bl = turning[i] = this.values[i] < 0.5f && (this.values[i] - this.values[i - 1]) * (this.values[i + 1] - this.values[i]) < 0.0f;
            if (!turning[i]) continue;
            this.tangents[i] = 0.0f;
        }
        for (i = 0; i < n - 1; ++i) {
            this.rocking[i] = turning[i] && turning[i + 1];
        }
    }

    private static float rock(float from, float to, float s) {
        float swing = 0.5f - 0.5f * (float)Math.cos(Math.PI * (double)s);
        float settle = s * s * s * (s * (6.0f * s - 15.0f) + 10.0f);
        return from + (to - from) * (swing + (settle - swing) * 0.35f);
    }

    public static SpinCurve build(float totalDegrees, ReelProfile profile, float resistanceScale, float judderDegrees) {
        float cruiseMid;
        float[][] tail = profile.tail();
        float tailStartTime = tail[0][0];
        float tailStartRemaining = tail[0][1];
        float tailStartProgress = (totalDegrees - tailStartRemaining) / totalDegrees;
        float spinUp = Math.min(profile.spinUpFraction(), tailStartTime * 0.5f);
        float slope = tailStartProgress / (tailStartTime - spinUp * 0.5f);
        float spinUpProgress = slope * spinUp * 0.5f;
        int n = 4 + tail.length;
        float[] times = new float[n];
        float[] values = new float[n];
        int i = 0;
        times[i] = 0.0f;
        values[i++] = 0.0f;
        times[i] = spinUp * 0.5f;
        values[i++] = spinUpProgress * 0.25f;
        times[i] = spinUp;
        values[i++] = spinUpProgress;
        times[i] = cruiseMid = (spinUp + tailStartTime) * 0.5f;
        values[i++] = spinUpProgress + slope * (cruiseMid - spinUp);
        for (float[] point : tail) {
            float remaining = point[1];
            if (point.length > 2 && point[2] != 0.0f) {
                remaining += point[2] * resistanceScale;
            }
            times[i] = point[0];
            values[i++] = (totalDegrees - remaining) / totalDegrees;
        }
        times[n - 1] = 1.0f;
        values[n - 1] = 1.0f;
        SpinCurve.separate(times);
        return new SpinCurve(times, values, totalDegrees, judderDegrees * profile.judderScale(), profile.judderFrequency(), 7.5f);
    }

    private static void separate(float[] times) {
        float minimum = 0.001f;
        for (int i = times.length - 2; i >= 0; --i) {
            if (!(times[i] > times[i + 1] - minimum)) continue;
            times[i] = times[i + 1] - minimum;
        }
        if (times[0] < 0.0f) {
            float shift = -times[0];
            float scale = 1.0f / (1.0f + shift);
            for (int i = 0; i < times.length; ++i) {
                times[i] = (times[i] + shift) * scale;
            }
        }
    }

    public float degrees(float t) {
        if (t <= 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return this.totalDegrees;
        }
        return this.totalDegrees * this.evaluate(t) + this.judder(t);
    }

    public float velocity(float t, float durationSeconds) {
        float h = 0.004f;
        float lo = Math.max(0.0f, t - h);
        float hi = Math.min(1.0f, t + h);
        float dt = (hi - lo) * durationSeconds;
        return dt <= 0.0f ? 0.0f : (this.degrees(hi) - this.degrees(lo)) / dt;
    }

    private float evaluate(float t) {
        float t0;
        int seg = this.segmentFor(t);
        float t1 = this.times[seg + 1];
        float h = t1 - (t0 = this.times[seg]);
        if (h <= 0.0f) {
            return this.values[seg];
        }
        float s = (t - t0) / h;
        if (this.rocking[seg]) {
            return SpinCurve.rock(this.values[seg], this.values[seg + 1], s);
        }
        float s2 = s * s;
        float s3 = s2 * s;
        float h00 = 2.0f * s3 - 3.0f * s2 + 1.0f;
        float h10 = s3 - 2.0f * s2 + s;
        float h01 = -2.0f * s3 + 3.0f * s2;
        float h11 = s3 - s2;
        return h00 * this.values[seg] + h10 * h * this.tangents[seg] + h01 * this.values[seg + 1] + h11 * h * this.tangents[seg + 1];
    }

    private float judder(float t) {
        if (this.judderDegrees <= 0.0f) {
            return 0.0f;
        }
        float remaining = 1.0f - t;
        float envelope = (float)Math.exp(-this.judderDecay * remaining) * remaining;
        float peak = (float)(Math.exp(-1.0) / (double)this.judderDecay);
        return this.judderDegrees * (envelope / peak) * (float)Math.sin(Math.PI * 2 * (double)this.judderFrequency * (double)t * (double)t);
    }

    private int segmentFor(float t) {
        int lo = 0;
        int hi = this.times.length - 1;
        while (lo < hi - 1) {
            int mid = lo + hi >>> 1;
            if (this.times[mid] <= t) {
                lo = mid;
                continue;
            }
            hi = mid;
        }
        return lo;
    }

    private static float[] computeTangents(float[] t, float[] v) {
        int n = t.length;
        float[] m = new float[n];
        for (int i = 0; i < n; ++i) {
            m[i] = i == 0 ? (v[1] - v[0]) / Math.max(1.0E-5f, t[1] - t[0]) : (i == n - 1 ? (v[n - 1] - v[n - 2]) / Math.max(1.0E-5f, t[n - 1] - t[n - 2]) : (v[i + 1] - v[i - 1]) / Math.max(1.0E-5f, t[i + 1] - t[i - 1]));
        }
        int n2 = n - 1;
        m[n2] = m[n2] * 0.15f;
        return m;
    }
}

