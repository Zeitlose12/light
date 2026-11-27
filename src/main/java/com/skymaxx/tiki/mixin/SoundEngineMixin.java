package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.audio.SoundHold;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={SoundEngine.class})
public class SoundEngineMixin {
    @Inject(method={"play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;"}, at={@At(value="HEAD")}, cancellable=true)
    private void skymaxx$refuseSound(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        // Light: game sound off while a Lava / Spooky / Winter gambling scene runs (its own sounds still play)
        try {
            if (com.skymaxx.tiki.light.LavaScene.silencesGame() && !com.skymaxx.tiki.light.LavaScene.isOwnSound(instance)) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                return;
            }
        } catch (Throwable ignored) {
        }
        if (TikiGambleClient.gameShouldBeSilent()) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }
        if (TikiGambleClient.holdGameSounds() && SoundHold.intercept(instance, TikiGambleClient.config.soundHoldMs)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

    @Inject(method={"playDelayed(Lnet/minecraft/client/resources/sounds/SoundInstance;I)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void skymaxx$refuseDelayedSound(SoundInstance instance, int delay, CallbackInfo ci) {
        try {
            if (com.skymaxx.tiki.light.LavaScene.silencesGame() && !com.skymaxx.tiki.light.LavaScene.isOwnSound(instance)) {
                ci.cancel();
                return;
            }
        } catch (Throwable ignored) {
        }
        if (TikiGambleClient.gameShouldBeSilent()) {
            ci.cancel();
        }
    }
}

