package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.anim.AnimationDirector;
import com.skymaxx.tiki.boa.BoaDirector;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;

/**
 * Light: replaces TikiGambleClient.onScreenOpening. Only ESC (the pause menu)
 * skips a running animation; E (inventory), chat and options do nothing while
 * it runs. Returns true when the screen must not open.
 */
public final class LightScreens {
    private LightScreens() {
    }

    public static boolean onScreenOpening(Screen screen) {
        AnimationDirector director = TikiGambleClient.director;
        BoaDirector boa = TikiGambleClient.boa;
        boolean totemRunning = director != null && director.running();
        boolean boaRunning = boa != null && boa.running();
        if (!totemRunning && !boaRunning) {
            return screen instanceof ChatScreen && LightScreens.chatShut(director, boa);
        }
        boolean ownMenu = screen instanceof PauseScreen || screen instanceof InventoryScreen || screen instanceof ChatScreen || screen instanceof OptionsScreen;
        if (ownMenu && !(screen instanceof PauseScreen)) {
            return true;
        }
        if (totemRunning) {
            director.skip();
        }
        if (boaRunning) {
            if (ownMenu) {
                boa.skipStep();
            } else {
                boa.skip();
            }
        }
        return ownMenu;
    }

    private static boolean chatShut(AnimationDirector director, BoaDirector boa) {
        return director != null && (director.armed() || director.running()) || boa != null && (boa.armed() || boa.running());
    }
}
