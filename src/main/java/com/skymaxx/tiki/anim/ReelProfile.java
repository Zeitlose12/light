package com.skymaxx.tiki.anim;

import java.util.ArrayList;

public final class ReelProfile {
    public static final float TOTAL_DEGREES = 180.0f;
    public static final float CORNER_DEGREES = 90.0f;
    private static final float GRIND_START = 133.0f;
    private static final float GRIND_END = 94.0f;
    private static final float GRIND_START_TIME = 0.22f;
    private static final float APPROACH_TIME = 0.035f;
    private static final float STALL_TIME_BASE = 1.9f;
    private static final float GRIND_SHARE = 0.56f;
    private static final float[][] BREAK = new float[][]{{0.0f, 90.0f}, {0.2f, 58.0f}, {0.42f, 30.0f}, {0.64f, 13.0f}, {0.84f, 4.0f}, {1.0f, 0.0f}};
    public static final ReelProfile[] BY_TIER = new ReelProfile[]{ReelProfile.build(2, 1.5f, 0.93f, 0.055f, 0.55f, 9.0f, 0.035f), ReelProfile.build(3, 2.2f, 0.945f, 0.05f, 0.8f, 10.0f, 0.035f), ReelProfile.build(4, 3.0f, 0.958f, 0.045f, 1.05f, 11.0f, 0.035f), ReelProfile.build(5, 3.8f, 0.968f, 0.04f, 1.35f, 12.0f, 0.035f)};
    private static final float[] BREAK_SECONDS = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, 0.12f, 0.095f};
    private static final float[] APPROACH_SECONDS = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, 0.065f, 0.055f};
    private static final float[] RECOIL = new float[]{1.6f, 2.1f, 3.3f, 4.2f};
    private final float spinUpFraction;
    private final float judderScale;
    private final float judderFrequency;
    private final float[][] tail;
    private final int stalls;
    private final float slip;
    private final float breakTime;

    public static float recoilFor(int tier) {
        return RECOIL[Math.max(0, Math.min(RECOIL.length - 1, tier))];
    }

    private ReelProfile(float spinUpFraction, float judderScale, float judderFrequency, float[][] tail, int stalls, float slip, float breakTime) {
        this.spinUpFraction = spinUpFraction;
        this.judderScale = judderScale;
        this.judderFrequency = judderFrequency;
        this.tail = tail;
        this.stalls = stalls;
        this.slip = slip;
        this.breakTime = breakTime;
    }

    public static ReelProfile forTier(int tier) {
        return BY_TIER[Math.max(0, Math.min(BY_TIER.length - 1, tier))];
    }

    public static ReelProfile forTier(int tier, float durationSeconds) {
        int t = Math.max(0, Math.min(BY_TIER.length - 1, tier));
        ReelProfile base = BY_TIER[t];
        float duration = Math.max(0.2f, durationSeconds);
        float breakAt = Math.max(base.breakTime, 1.0f - BREAK_SECONDS[t] / duration);
        float approach = Math.min(0.035f, APPROACH_SECONDS[t] / duration);
        if (breakAt == base.breakTime && approach == 0.035f) {
            return base;
        }
        return ReelProfile.build(base.stalls, base.slip, breakAt, base.spinUpFraction, base.judderScale, base.judderFrequency, approach);
    }

    private static ReelProfile build(int stalls, float slip, float breakTime, float spinUp, float judderScale, float judderFrequency, float approach) {
        ArrayList<float[]> rows = new ArrayList<float[]>();
        rows.add(new float[]{0.22f, 133.0f});
        float degreeWeight = 0.0f;
        float timeWeight = 0.0f;
        for (int k = 0; k < stalls; ++k) {
            degreeWeight += (float)(stalls - k) + 0.6f;
            timeWeight += (float)k + 1.9f;
        }
        float degreesSpan = 39.0f;
        float timeSpan = breakTime - approach - 0.22f;
        float degreesDone = 0.0f;
        float timeDone = 0.0f;
        for (int k = 0; k < stalls; ++k) {
            float slipEnd;
            float segmentTime = ((float)k + 1.9f) / timeWeight;
            float remaining = 133.0f - degreesSpan * (degreesDone += ((float)(stalls - k) + 0.6f) / degreeWeight);
            float grindEnd = timeDone + segmentTime * 0.56f;
            timeDone = slipEnd = timeDone + segmentTime;
            float depth = slip * (stalls == 1 ? 1.0f : 0.45f + 0.55f * (float)k / (float)(stalls - 1));
            rows.add(new float[]{0.22f + timeSpan * grindEnd, remaining});
            rows.add(new float[]{0.22f + timeSpan * slipEnd, remaining, depth});
        }
        rows.add(new float[]{breakTime, 90.0f});
        for (int i = 1; i < BREAK.length; ++i) {
            float t = breakTime + (1.0f - breakTime) * BREAK[i][0];
            rows.add(new float[]{Math.min(1.0f, t), BREAK[i][1]});
        }
        return new ReelProfile(spinUp, judderScale, judderFrequency, rows.toArray(new float[0][]), stalls, slip, breakTime);
    }

    public float spinUpFraction() {
        return this.spinUpFraction;
    }

    public float judderScale() {
        return this.judderScale;
    }

    public float judderFrequency() {
        return this.judderFrequency;
    }

    public float[][] tail() {
        return this.tail;
    }
}

