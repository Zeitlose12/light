package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.light.LightHookState;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = {FishingHookRenderState.class})
public class FishingHookRenderStateMixin implements LightHookState {
    @Unique
    private int light$lineStyle;
    @Unique
    private int light$bobberStyle;

    @Override
    public int light$line() {
        return this.light$lineStyle;
    }

    @Override
    public int light$bobber() {
        return this.light$bobberStyle;
    }

    @Override
    public void light$style(int line, int bobber) {
        this.light$lineStyle = line;
        this.light$bobberStyle = bobber;
    }
}
