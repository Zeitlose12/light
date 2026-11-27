package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Light: the HUDs (Dye, Sack value, Fishing stats, Bestiary ranking) while a menu is open -
 * shown as always, dimmed, or hidden. The chat does not count as a menu; pop-ups are not affected.
 */
public final class HudVis {
    private HudVis() {
    }

    private static String mode() {
        TikiConfig c = TikiGambleClient.config;
        return c == null || c.hudInMenus == null ? "HIDDEN" : c.hudInMenus;
    }

    /** An inventory, chest, menu ... is open (not the chat). */
    public static boolean menuOpen() {
        Minecraft mc = Minecraft.getInstance();
        Screen s = mc.gui == null ? null : mc.gui.screen();
        return s != null && !(s instanceof ChatScreen);
    }

    /** The sack menu keeps the sack value HUD (it belongs there). */
    public static boolean sackMenu() {
        Minecraft mc = Minecraft.getInstance();
        Screen s = mc.gui == null ? null : mc.gui.screen();
        return s != null && s.getTitle().getString().contains("Sack");
    }

    /** Skip drawing this HUD right now. */
    public static boolean hidden() {
        return "HIDDEN".equals(mode()) && menuOpen();
    }

    /** "Dimmed": a soft dark veil over the HUD's box while a menu is open. */
    public static void veil(GuiGraphicsExtractor ctx, int x0, int y0, int x1, int y1) {
        if (!"DIMMED".equals(mode()) || !menuOpen()) return;
        Theme.round(ctx, x0, y0, x1, y1, 0x9A000000);
    }

    public static String next(String m) {
        return switch (m == null ? "HIDDEN" : m) {
            case "HIDDEN" -> "DIMMED";
            case "DIMMED" -> "SHOWN";
            default -> "HIDDEN";
        };
    }

    public static String label(String m) {
        return switch (m == null ? "HIDDEN" : m) {
            case "DIMMED" -> "Dimmed";
            case "SHOWN" -> "Shown";
            default -> "Hidden";
        };
    }
}
