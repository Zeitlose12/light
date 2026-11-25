package com.skymaxx.tiki.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={GuiTextRenderState.class})
public interface GuiTextRenderStateAccessor {
    @Accessor(value="font")
    public Font skymaxx$font();

    @Accessor(value="text")
    public FormattedCharSequence skymaxx$text();

    @Accessor(value="x")
    public int skymaxx$x();

    @Accessor(value="y")
    public int skymaxx$y();

    @Accessor(value="color")
    public int skymaxx$color();

    @Accessor(value="backgroundColor")
    public int skymaxx$backgroundColor();

    @Accessor(value="dropShadow")
    public boolean skymaxx$dropShadow();

    @Accessor(value="includeEmpty")
    public boolean skymaxx$includeEmpty();
}

