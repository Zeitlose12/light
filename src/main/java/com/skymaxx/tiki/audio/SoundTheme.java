package com.skymaxx.tiki.audio;

import com.skymaxx.tiki.synth.NoiseVoice;
import com.skymaxx.tiki.synth.ToneVoice;
import com.skymaxx.tiki.synth.Voice;

public enum SoundTheme {
    CLASSIC{
        private static final float[] SCALE = new float[]{1.0f, 1.125f, 1.25f, 1.5f, 1.6667f, 2.0f, 2.25f, 2.5f, 3.0f, 3.3333f, 4.0f, 4.5f, 5.0f, 6.0f, 6.6667f};

        @Override
        public float root() {
            return 146.83f;
        }

        @Override
        public float[] scale() {
            return SCALE;
        }

        @Override
        public int[] tierDegrees() {
            return new int[]{0, 2, 3, 5};
        }

        @Override
        public Voice hit(float hz, float gain, float pan) {
            return ToneVoice.drum(hz * 0.82f, gain * 0.92f);
        }

        @Override
        public Voice tom(float hz, float gain) {
            return ToneVoice.drum(hz, gain * 0.85f);
        }

        @Override
        public Voice accent(float gain, float pan) {
            return NoiseVoice.detent(gain * 1.25f, pan);
        }

        @Override
        public Voice hat(float gain, float pan) {
            return NoiseVoice.detent(gain * 0.85f, pan);
        }

        @Override
        public Voice lead(float hz, float gain, float pan) {
            return ToneVoice.pluck(hz * 2.0f, gain * 1.15f, pan);
        }

        @Override
        public Voice bass(float hz, float gain, float seconds) {
            return ToneVoice.bass(hz, gain, seconds);
        }

        @Override
        public Voice land(float hz, float gain, float pan) {
            return ToneVoice.ring(hz * 2.0f, gain, pan);
        }

        @Override
        public Voice tick(float hz, float gain) {
            return NoiseVoice.detent(gain * 1.6f, 0.0f);
        }

        @Override
        public Voice deadWood(float hz, float gain) {
            return ToneVoice.sag(hz * 1.4f, gain * 0.85f, 0.5f);
        }

        @Override
        public Voice pad(float hz, float gain, float pan, float seconds) {
            return ToneVoice.horn(hz, gain * 0.9f, pan, seconds);
        }

        @Override
        public Voice sparkle(float hz, float gain, float pan) {
            return ToneVoice.coin(hz, gain * 1.2f, pan);
        }

        @Override
        public Voice drone(float hz, float gain, float seconds) {
            return ToneVoice.sub(hz, gain * 0.85f, seconds);
        }

        @Override
        public Voice air(float gain, float pan, float seconds) {
            return NoiseVoice.sizzle(gain * 0.55f, seconds);
        }
    };


    public float trim() {
        return 1.0f;
    }

    public abstract float root();

    public abstract float[] scale();

    public abstract int[] tierDegrees();

    public int[] chordDegrees() {
        return new int[]{0, 2, 3, 5};
    }

    public float tierHz(int tier) {
        int[] degrees = this.tierDegrees();
        return this.root() * this.scale()[degrees[Math.max(0, Math.min(degrees.length - 1, tier))]];
    }

    public abstract Voice hit(float var1, float var2, float var3);

    public abstract Voice tom(float var1, float var2);

    public abstract Voice accent(float var1, float var2);

    public abstract Voice hat(float var1, float var2);

    public abstract Voice lead(float var1, float var2, float var3);

    public abstract Voice bass(float var1, float var2, float var3);

    public abstract Voice land(float var1, float var2, float var3);

    public abstract Voice tick(float var1, float var2);

    public abstract Voice deadWood(float var1, float var2);

    public abstract Voice pad(float var1, float var2, float var3, float var4);

    public abstract Voice sparkle(float var1, float var2, float var3);

    public abstract Voice drone(float var1, float var2, float var3);

    public abstract Voice air(float var1, float var2, float var3);
}

