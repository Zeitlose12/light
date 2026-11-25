package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.TikiGambleClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Gui.class})
public class GuiScreenMixin {
    @Inject(method={"setScreen"}, at={@At(value="HEAD")}, cancellable=true)
    private void skymaxx$interceptMenus(Screen screen, CallbackInfo ci) {
        if (screen != null && com.skymaxx.tiki.light.LightScreens.onScreenOpening(screen)) {
            ci.cancel();
        }
    }
}

