package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.light.BazaarLock;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Light, layer 3: the last stop before the click packet is built and sent.
 * Even if a screen or another mod bypasses the screen layers, a click on the
 * Sell Sacks Now slot of a Bazaar menu never reaches the server.
 */
@Mixin(MultiPlayerGameMode.class)
public class BazaarPacketGuardMixin {
    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true, require = 0)
    private void light$guardSellSacksPacket(int containerId, int slotId, int button, ContainerInput input, Player player, CallbackInfo ci) {
        if (BazaarLock.blocksPacket(slotId)) {
            BazaarLock.notice();
            ci.cancel();
        }
    }
}
