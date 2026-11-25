package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.render.HotspotText;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientPacketListener.class})
public class ParticlePacketMixin {
    @Inject(method={"handleParticleEvent"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift=At.Shift.AFTER)}, require=0)
    private void skymaxx$hotspotRing(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        HotspotText.onParticle(packet.getX(), packet.getY(), packet.getZ());
    }
}

