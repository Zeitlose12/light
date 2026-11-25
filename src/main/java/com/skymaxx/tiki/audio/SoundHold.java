package com.skymaxx.tiki.audio;

import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public final class SoundHold {
    private static final int CAPACITY = 256;
    private static final ArrayDeque<Held> QUEUE = new ArrayDeque();
    private static boolean releasing;

    private SoundHold() {
    }

    public static boolean intercept(SoundInstance sound, long holdMs) {
        if (releasing || holdMs <= 0L || sound == null) {
            return false;
        }
        SoundSource source = sound.getSource();
        if (source == SoundSource.MUSIC || source == SoundSource.UI) {
            return false;
        }
        while (QUEUE.size() >= 256) {
            QUEUE.removeFirst();
        }
        QUEUE.addLast(new Held(sound, System.currentTimeMillis() + holdMs));
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void tick(boolean silent) {
        if (silent) {
            QUEUE.clear();
            return;
        }
        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        while (!QUEUE.isEmpty() && QUEUE.peekFirst().releaseAtMs() <= now) {
            Held held = QUEUE.removeFirst();
            releasing = true;
            try {
                mc.getSoundManager().play(held.sound());
            }
            catch (Throwable throwable) {
            }
            finally {
                releasing = false;
            }
        }
    }

    public static void clear() {
        QUEUE.clear();
    }

    private record Held(SoundInstance sound, long releaseAtMs) {
    }
}

