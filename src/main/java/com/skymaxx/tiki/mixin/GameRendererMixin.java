package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.render.GameplayView;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={GameRenderer.class})
public class GameRendererMixin {
    @Inject(method={"render"}, require=0, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/render/GuiRenderer;render()V")})
    private void skymaxx$captureGameplay(DeltaTracker delta, boolean renderLevel, CallbackInfo ci) {
        GameplayView.capture();
    }
}

