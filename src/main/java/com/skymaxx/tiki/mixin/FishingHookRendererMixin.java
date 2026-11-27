package com.skymaxx.tiki.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.skymaxx.tiki.light.Crew;
import com.skymaxx.tiki.light.LightHookState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Light Crew: own fishing line colours and bobber sprites. Purely visual, only on this client.
 * Every injection is optional (require = 0): if Minecraft changes, the normal line and bobber are drawn.
 */
@Mixin(value = {FishingHookRenderer.class})
public class FishingHookRendererMixin {
    private static final String SUBMIT = "submit(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";
    private static final String GEOMETRY = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V";

    @Inject(method = {"extractRenderState(Lnet/minecraft/world/entity/projectile/FishingHook;Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;F)V"}, at = {@At(value = "TAIL")}, require = 0)
    private void light$crewStyle(FishingHook hook, FishingHookRenderState state, float partial, CallbackInfo ci) {
        int[] s = Crew.styleFor(hook.getPlayerOwner());
        ((LightHookState) state).light$style(s[0], s[1]);
    }

    @WrapOperation(method = {SUBMIT}, at = {@At(value = "INVOKE", target = GEOMETRY, ordinal = 0)}, require = 0)
    private void light$bobber(SubmitNodeCollector collector, PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer, Operation<Void> original, @Local(argsOnly = true) FishingHookRenderState state) {
        int b = ((LightHookState) state).light$bobber();
        RenderType own = b == 0 ? null : Crew.bobberType(b);
        if (own == null) {
            original.call(collector, pose, type, renderer);
            return;
        }
        original.call(collector, pose, own, (SubmitNodeCollector.CustomGeometryRenderer) (p, consumer) -> {
            Crew.bobberNow = b;
            try {
                renderer.render(p, consumer);
            } finally {
                Crew.bobberNow = 0;
            }
        });
    }

    @WrapOperation(method = {SUBMIT}, at = {@At(value = "INVOKE", target = GEOMETRY, ordinal = 1)}, require = 0)
    private void light$line(SubmitNodeCollector collector, PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer, Operation<Void> original, @Local(argsOnly = true) FishingHookRenderState state) {
        int l = ((LightHookState) state).light$line();
        if (l == 0) {
            original.call(collector, pose, type, renderer);
            return;
        }
        original.call(collector, pose, type, (SubmitNodeCollector.CustomGeometryRenderer) (p, consumer) -> {
            Crew.lineNow = l;
            try {
                renderer.render(p, consumer);
            } finally {
                Crew.lineNow = 0;
            }
        });
    }

    /** Line colour along the line (t: 0 at the bobber, 1 at the rod). */
    @WrapOperation(method = {"stringVertex"}, at = {@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;")}, require = 0)
    private static VertexConsumer light$lineColor(VertexConsumer consumer, int color, Operation<VertexConsumer> original, @Local(argsOnly = true, ordinal = 3) float t) {
        int l = Crew.lineNow;
        return original.call(consumer, l == 0 ? color : Crew.lineColor(l, t));
    }

    /** Crew lines are a little thicker so the colours show. */
    @WrapOperation(method = {"stringVertex"}, at = {@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setLineWidth(F)Lcom/mojang/blaze3d/vertex/VertexConsumer;")}, require = 0)
    private static VertexConsumer light$lineWidth(VertexConsumer consumer, float width, Operation<VertexConsumer> original) {
        return original.call(consumer, Crew.lineNow == 0 ? width : width * 1.6f);
    }

    /** Crew bobbers glow (full brightness, also at night). */
    @WrapOperation(method = {"vertex"}, at = {@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setLight(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;")}, require = 0)
    private static VertexConsumer light$bobberLight(VertexConsumer consumer, int light, Operation<VertexConsumer> original) {
        return original.call(consumer, Crew.bobberNow == 0 ? light : 0xF000F0);
    }
}
