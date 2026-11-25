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

