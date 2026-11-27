package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;

/**
 * Light: the moments behind the secret achievements. They are noticed here and remembered as flags in the
 * config; the achievement itself unlocks on the next check (once a second) like every other one.
 */
public final class Secrets {
    private static final long PATIENCE_MS = 2L * 60L * 60L * 1000L;
    private static final long FISH_GAP_MS = 5L * 60L * 1000L;
    private static long streakStart;
    private static long lastFishing;
    private static int creditClicks;
    private static long creditClickMs;

    private Secrets() {
    }

    private static Set<String> flags(TikiConfig c) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (c != null && c.lightSecrets != null && !c.lightSecrets.isBlank()) out.addAll(Arrays.asList(c.lightSecrets.split(",")));
        return out;
    }

    public static boolean has(String name) {
        return flags(TikiGambleClient.config).contains(name);
    }

    public static void flag(String name) {
        TikiConfig c = TikiGambleClient.config;
        if (c == null) return;
        Set<String> f = flags(c);
        if (!f.add(name)) return;
        c.lightSecrets = String.join(",", f);
        c.save();
    }

    /** A new (real) gambling, Wiki Tiki or Titanoboa result. prev = the one before it, or null. */
    static void onRecord(GambleLog.Entry e, GambleLog.Entry prev) {
        try {
            LocalDateTime at = LocalDateTime.ofInstant(Instant.ofEpochMilli(e.ms), ZoneId.systemDefault());
            if (at.getHour() == 3) flag("NIGHT_OWL");
            if (at.getHour() == 18 && at.getMinute() == 0) flag("GOLDEN_HOUR");
            if ("ragnarok".equals(e.mob) && e.drop == null) flag("PACIFIST");
            if (prev != null && gamble(e.mob) && gamble(prev.mob)) {
                if (e.drop != null && e.mob.equals(prev.mob) && e.drop.equalsIgnoreCase(prev.drop)) flag("DEJA_VU");
                if (Math.abs(e.ms - prev.ms) <= 60_000L) flag("SPEEDRUN");
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean gamble(String mob) {
        for (String m : GambleLog.MOBS) if (m.equals(mob)) return true;
        return false;
    }

    /** Patience is a Virtue: 2 hours of fishing (rod out at least every 5 minutes) without opening /light. */
    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            streakStart = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        if (mc.gui.screen() instanceof com.skymaxx.tiki.gui.SkyMaxxScreen) {
            streakStart = 0L;
            return;
        }
        if (mc.player.fishing != null) {
            if (streakStart == 0L || now - lastFishing > FISH_GAP_MS) streakStart = now;
            lastFishing = now;
            if (now - streakStart >= PATIENCE_MS) flag("PATIENCE");
        }
    }

    /** Behind the Scenes: the credits clicked 10 times (within a minute). */
    public static void creditClick() {
        long now = System.currentTimeMillis();
        if (now - creditClickMs > 60_000L) creditClicks = 0;
        creditClickMs = now;
        if (++creditClicks >= 10) flag("BEHIND_THE_SCENES");
    }
}
