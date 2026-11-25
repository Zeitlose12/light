package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.mixin.GuiTextRenderStateAccessor;
import com.skymaxx.tiki.render.HudFilter;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={GuiRenderState.class})
public class GuiRenderStateFadeMixin {
    private static final int FAINTEST_TEXT = 8;

    @Inject(method={"addGuiElement"}, at={@At(value="HEAD")}, cancellable=true)
    private void skymaxx$fadeElement(GuiElementRenderState element, CallbackInfo ci) {
        if (!HudFilter.filtering()) {
            return;
        }
        ci.cancel();
        GuiElementRenderState faded = HudFilter.fade(element);
        if (faded != null) {
            GuiRenderState self = (GuiRenderState)(Object)this;
            HudFilter.readd(() -> self.addGuiElement(faded));
        }
    }

    @Inject(method={"addBlitToCurrentLayer"}, at={@At(value="HEAD")}, cancellable=true)
    private void skymaxx$fadeBlit(BlitRenderState blit, CallbackInfo ci) {
        if (!HudFilter.filtering()) {
            return;
        }
        ci.cancel();
        GuiElementRenderState guiElementRenderState = HudFilter.fade((GuiElementRenderState)blit);
        if (guiElementRenderState instanceof BlitRenderState) {
            BlitRenderState faded = (BlitRenderState)guiElementRenderState;
            GuiRenderState self = (GuiRenderState)(Object)this;
            HudFilter.readd(() -> self.addBlitToCurrentLayer(faded));
        }
    }

    @Inject(method={"addText"}, at={@At(value="HEAD")}, cancellable=true)
    private void skymaxx$fadeText(GuiTextRenderState text, CallbackInfo ci) {
        if (!HudFilter.filtering()) {
            return;
        }
        ci.cancel();
        GuiTextRenderStateAccessor from = (GuiTextRenderStateAccessor)(Object)text;
        int colour = HudFilter.scale(from.skymaxx$color());
        if (colour >>> 24 < 8) {
            return;
        }
        GuiTextRenderState faded = new GuiTextRenderState(from.skymaxx$font(), from.skymaxx$text(), text.pose, from.skymaxx$x(), from.skymaxx$y(), colour, HudFilter.scale(from.skymaxx$backgroundColor()), from.skymaxx$dropShadow(), from.skymaxx$includeEmpty(), text.scissor);
        GuiRenderState self = (GuiRenderState)(Object)this;
        HudFilter.readd(() -> self.addText(faded));
    }

    @Inject(method={"addItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void skymaxx$dropItem(GuiItemRenderState item, CallbackInfo ci) {
        if (HudFilter.filtering() && HudFilter.dropsUncoloured()) {
            ci.cancel();
        }
    }

    @Inject(method={"addPicturesInPictureState"}, at={@At(value="HEAD")}, cancellable=true)
    private void skymaxx$dropPicture(PictureInPictureRenderState picture, CallbackInfo ci) {
        if (HudFilter.filtering() && HudFilter.dropsUncoloured()) {
            ci.cancel();
        }
    }
}

