package com.skymaxx.tiki.synth;

public interface Voice {
    public static final float HEADROOM = 0.28f;

    public boolean render(float[] var1, float[] var2, float[] var3, int var4, float var5);
}

