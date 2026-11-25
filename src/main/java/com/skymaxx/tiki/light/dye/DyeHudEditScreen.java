package com.skymaxx.tiki.light.dye;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Light: drag the dye HUD with the mouse, scroll to resize, ESC to finish. */
public final class DyeHudEditScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private double grabX;
    private double grabY;

    public DyeHudEditScreen(Screen parent) {
        super((Component)Component.literal("Move dye HUD"));
        this.parent = parent;
    }

    private TikiConfig cfg() {
        return TikiGambleClient.config;
    }

    private int hudX() {
        return Math.round(this.cfg().dyeHudX * this.width);
    }

    private int hudY() {
        return Math.round(this.cfg().dyeHudY * this.height);
    }

    private int hudW() {
        return Math.round(DyeHud.WIDTH * this.cfg().dyeHudScale);
    }

    private int hudH() {
        return Math.round(DyeHud.height(this.cfg()) * this.cfg().dyeHudScale);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partial) {
        ctx.fill(0, 0, this.width, this.height, 0x60000000);
        ctx.centeredText(this.font, (Component)Component.literal("§dDrag the panel §7· scroll to resize §7· ESC when done"), this.width / 2, 12, -1);
        ctx.centeredText(this.font, (Component)Component.literal("§8Size " + Math.round(this.cfg().dyeHudScale * 100) + "%"), this.width / 2, 24, -1);
        DyeHud.draw(ctx, this.font, this.hudX(), this.hudY(), this.cfg().dyeHudScale, this.cfg());
        ctx.outline(this.hudX() - 1, this.hudY() - 1, this.hudW() + 2, this.hudH() + 2, 0xFFFF8CC8);
        super.extractRenderState(ctx, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x();
        double my = event.y();
        if (event.button() == 0 && mx >= this.hudX() && mx <= this.hudX() + this.hudW() && my >= this.hudY() && my <= this.hudY() + this.hudH()) {
            this.dragging = true;
            this.grabX = mx - this.hudX();
            this.grabY = my - this.hudY();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.dragging) {
            double nx = Math.max(0, Math.min(this.width - this.hudW(), event.x() - this.grabX));
            double ny = Math.max(0, Math.min(this.height - this.hudH(), event.y() - this.grabY));
            this.cfg().dyeHudX = (float)(nx / this.width);
            this.cfg().dyeHudY = (float)(ny / this.height);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.dragging) {
            this.dragging = false;
            this.cfg().save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float s = this.cfg().dyeHudScale + (float)scrollY * 0.1f;
        this.cfg().dyeHudScale = Math.max(0.5f, Math.min(2.0f, Math.round(s * 10f) / 10f));
        this.cfg().save();
        return true;
    }

    @Override
    public void onClose() {
        this.cfg().save();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
