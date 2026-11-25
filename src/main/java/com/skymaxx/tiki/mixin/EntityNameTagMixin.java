package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.render.HotspotText;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EntityRenderer.class})
public class EntityNameTagMixin {
    @Inject(method={"extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V"}, at={@At(value="TAIL")}, require=0)
    private void skymaxx$hotspotTextOnWater(Entity entity, EntityRenderState state, float partialTick, double nameRange, double scoreRange, CallbackInfo ci) {
        if (HotspotText.hides(entity)) {
            state.nameTag = null;
        }
    }
}

