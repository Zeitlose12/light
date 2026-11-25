package com.skymaxx.tiki.audio;

import com.skymaxx.tiki.audio.SoundTheme;
import com.skymaxx.tiki.synth.AlarmBellVoice;
import com.skymaxx.tiki.synth.NoiseVoice;
import com.skymaxx.tiki.synth.ShepardVoice;
import com.skymaxx.tiki.synth.SynthEngine;
import com.skymaxx.tiki.synth.ToneVoice;

final class Jackpot {
    static final float LENGTH = 6.2f;
    private static final float MAJOR_THIRD = 1.2599f;
    private static final float FIFTH = 1.4983f;
    private static final float WHOLE_TONE = 1.1225f;
    static final float BELL_HZ = 1050.0f;

    private Jackpot() {
    }

    static void play(SynthEngine synth, SoundTheme theme) {
        synth.duck(1.0f);
        float root = theme.root();
        float[] scale = theme.scale();
        synth.play(NoiseVoice.slap(0.55f));
        synth.play(NoiseVoice.crash(0.3f, 4.0f));
        synth.play(ToneVoice.sweepDown(78.0f, 0.55f, 1.8f));
        synth.play(ToneVoice.gong(55.0f, 0.35f, 0.0f));
        synth.play(theme.hit(40.0f, 0.5f, 0.0f));
        synth.after(0.02f, () -> synth.play(new AlarmBellVoice(1050.0f, 2.8f, 20.0f, 1.3f, 0.3f)));
        for (int i = 0; i < 16; ++i) {
            float pitch = root * 2.0f * scale[Math.min(scale.length - 1, i)];
            float pan = i % 2 == 0 ? -0.3f : 0.3f;
            synth.after(0.08f + (float)i * 0.028f, () -> synth.play(theme.sparkle(pitch, 0.12f, pan)));
        }
        float f = root * 2.0f;
        for (int i = 0; i < 3; ++i) {
            synth.after(0.36f + (float)i * 0.15f, () -> {
                synth.play(ToneVoice.horn(f * 1.4983f, 0.22f, -0.2f, 0.13f));
                synth.play(ToneVoice.horn(f, 0.2f, 0.2f, 0.13f));
            });
        }
        synth.after(0.86f, () -> Jackpot.chord(synth, theme, f, 1.0f, 2.4f));
        int coins = 72;
        for (int i = 0; i < coins; ++i) {
            float t = (float)i / (float)(coins - 1);
            float delay = 0.45f + t * 4.2f;
            float pitch = root * scale[Math.max(0, 13 - (int)((float)(i * 7919 % 97) / 97.0f * 8.0f))] * 2.0f;
            float gain = 0.2f * (1.0f - 0.45f * t);
            float pan = (float)(i * 5 % 9 - 4) / 5.0f;
            synth.after(delay, () -> synth.play(ToneVoice.coin(pitch * 2.0f, gain, pan)));
        }
        synth.after(0.5f, () -> synth.play(NoiseVoice.sizzle(0.08f, 4.0f)));
        synth.after(1.9f, () -> synth.play(ShepardVoice.riser(root * 2.0f, 0.13f, 1.1f)));
        synth.after(3.0f, () -> {
            synth.play(NoiseVoice.crash(0.24f, 3.0f));
            synth.play(ToneVoice.gong(61.7f, 0.4f, 0.0f));
            synth.play(theme.hit(44.0f, 0.6f, 0.0f));
            Jackpot.chord(synth, theme, f * 1.1225f, 1.0f, 2.6f);
        });
        float[] up = new float[]{3.25f, 3.7f, 4.15f, 4.55f, 5.0f, 5.35f};
        for (int k = 0; k < up.length; ++k) {
            float pan = k % 2 == 0 ? -0.6f : 0.6f;
            float at = up[k];
            synth.after(at, () -> synth.play(new NoiseVoice(600.0f, 5200.0f, 1.6f, true, 0.28f, 0.05f, 0.07f, pan, 0.3f)));
            synth.after(at + 0.34f, () -> {
                synth.play(NoiseVoice.crack(0.26f, pan));
                synth.play(ToneVoice.drum(58.0f, 0.34f));
            });
            for (int s = 0; s < 6; ++s) {
                float pitch = root * 4.0f * scale[(k + s * 3) % scale.length];
                float spark = pan + (s % 2 == 0 ? 0.15f : -0.15f);
                synth.after(at + 0.4f + (float)s * 0.045f, () -> synth.play(theme.sparkle(pitch, 0.07f, spark)));
            }
        }
        synth.after(5.6f, () -> synth.play(ShepardVoice.riser(root * 2.0f * 1.1225f, 0.1f, 0.6f)));
        synth.after(6.2f, () -> {
            synth.play(NoiseVoice.crash(0.2f, 3.6f));
            synth.play(ToneVoice.gong(49.0f, 0.42f, 0.0f));
            synth.play(ToneVoice.sweepDown(70.0f, 0.45f, 1.6f));
            Jackpot.chord(synth, theme, f * 1.1225f, 0.9f, 3.6f);
        });
    }

    private static void chord(SynthEngine synth, SoundTheme theme, float hz, float size, float seconds) {
        float[] ratios = new float[]{1.0f, 1.2599f, 1.4983f, 2.0f};
        for (int i = 0; i < ratios.length; ++i) {
            float pan = i % 2 == 0 ? -0.35f : 0.35f;
            synth.play(ToneVoice.horn(hz * ratios[i], (0.24f - 0.03f * (float)i) * size, pan, seconds));
            synth.play(theme.land(hz * ratios[i], (0.28f - 0.04f * (float)i) * size, -pan));
            synth.play(ToneVoice.choir(hz * ratios[i] * 0.5f, 0.09f * size, pan, seconds));
        }
        synth.play(ToneVoice.bass(hz * 0.25f, 0.4f * size, seconds));
        synth.play(theme.air(0.14f * size, 0.0f, seconds));
    }
}

