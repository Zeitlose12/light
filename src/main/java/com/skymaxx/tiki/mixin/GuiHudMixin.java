package com.skymaxx.tiki.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.render.HudFilter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={Hud.class}, priority=5000)
public class GuiHudMixin {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @WrapMethod(method={"extractRenderState"})
    private void skymaxx$drawTotemAndOwnTheScreen(GuiGraphicsExtractor ctx, DeltaTracker delta, Operation<Void> original) {
        // Light: a full-screen gambling scene hides the whole HUD (chat, titles, other mods) - no spoilers
        try {
            if (com.skymaxx.tiki.light.LavaScene.ownsScreen()) {
                com.skymaxx.tiki.light.LavaScene.render(ctx, delta);
                return;
            }
        } catch (Throwable ignored) {
        }
        if (!TikiGambleClient.renderTotemBeforeHud(ctx, delta)) {
            original.call(new Object[]{ctx, delta});
            return;
        }
        float hud = TikiGambleClient.hudAlphaWhileOwned();
        if (hud <= 0.004f) {
            return;
        }
        HudFilter.begin(hud);
        try {
            original.call(new Object[]{ctx, delta});
        }
        finally {
            HudFilter.end();
        }
    }
}

