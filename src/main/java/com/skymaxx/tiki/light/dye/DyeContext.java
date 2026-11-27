package com.skymaxx.tiki.light.dye;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.FishingHook;

/**
 * Light: which dyes matter where you fish right now (Dye HUD "Auto").
 * Lava under your bobber -> Carmine. Water -> Aquamarine, plus Iceberg on Jerry's Workshop or after winter sea
 * creatures, plus Midnight after Spooky sea creatures (last 30 minutes). Only your own bobber, the tab list and
 * your catches are looked at.
 */
public final class DyeContext {
    private static final long RECENT_MS = 30L * 60L * 1000L;
    private static final Map<Dye, Long> LAST_SEEN = new ConcurrentHashMap<>();
    private static boolean lava;
    private static long lavaCheckedMs;
    private static String area = "";
    private static long areaMs;

    private DyeContext() {
    }

    /** A catch / kill of this creature (from DyeTracker). Remembers the event dyes it belongs to. */
    static void seen(String creature) {
        long now = System.currentTimeMillis();
        for (Dye d : new Dye[]{Dye.ICEBERG, Dye.MIDNIGHT, Dye.CARMINE}) {
            if (DyeOdds.creatures(d).contains(creature)) LAST_SEEN.put(d, now);
        }
    }

    private static boolean recent(Dye d) {
        Long t = LAST_SEEN.get(d);
        return t != null && System.currentTimeMillis() - t < RECENT_MS;
    }

    /** Liquid under your own bobber; remembered while the rod is reeled in. */
    private static void checkLiquid(Minecraft mc) {
        FishingHook hook = mc.player == null ? null : mc.player.fishing;
        if (hook == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lavaCheckedMs < 500L) return;
        lavaCheckedMs = now;
        if (hook.isInLava()) lava = true;
        else if (hook.isInWater()) lava = false;
    }

    private static void checkArea(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - areaMs < 2000L || mc.getConnection() == null) return;
        areaMs = now;
        for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
            Component n = info.getTabListDisplayName();
            if (n == null) continue;
            String line = n.getString().replaceAll("§.", "").trim();
            if (line.startsWith("Area:")) {
                area = line.substring(5).trim();
                return;
            }
        }
    }

    public static List<Dye> current() {
        Minecraft mc = Minecraft.getInstance();
        try {
            checkLiquid(mc);
            checkArea(mc);
        } catch (Throwable ignored) {
        }
        List<Dye> out = new ArrayList<>();
        if (lava) {
            out.add(Dye.CARMINE);
            return out;
        }
        out.add(Dye.AQUAMARINE);
        if (area.equalsIgnoreCase("Jerry's Workshop") || recent(Dye.ICEBERG)) out.add(Dye.ICEBERG);
        if (recent(Dye.MIDNIGHT)) out.add(Dye.MIDNIGHT);
        return out;
    }
}
