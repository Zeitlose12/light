package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.light.dye.DyeKills;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Light: reads (never changes or cancels) chat and sound packets for the dye tracker.
 * Runs before chat mods, so custom catch messages from other mods don't hide the original.
 */
@Mixin(value = ClientPacketListener.class, priority = 500)
public class LightPacketMixin {
    @Inject(method = "handleSystemChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), require = 0)
    private void light$chat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            DyeKills.onChatPacket(packet.content(), packet.overlay());
        }
    }

    @Inject(method = "handleSoundEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), require = 0)
    private void light$sound(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            DyeKills.onSound(packet);
        }
    }
}
