package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.TikiGambleClient;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={GuiRenderState.class})
public class GuiRenderStateMixin {
    @Inject(method={"reset"}, at={@At(value="HEAD")})
    private void skymaxx$newFrame(CallbackInfo ci) {
        TikiGambleClient.releaseBlurClaim();
    }

    @Inject(method={"blurBeforeThisStratum"}, at={@At(value="HEAD")}, cancellable=true)
    private void skymaxx$onlyBlurOnce(CallbackInfo ci) {
        if (TikiGambleClient.blurAlreadyClaimed()) {
            ci.cancel();
        }
    }
}

