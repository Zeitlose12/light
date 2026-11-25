package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Light: drag the shark pop-up box, scroll to resize, right-click to reset, ESC to finish. Shows a looping preview. */
public final class SharkEditScreen extends Screen {
    private final Screen parent;
    private final long opened = System.currentTimeMillis();
    private boolean dragging;
    private double grabX;
    private double grabY;

    public SharkEditScreen(Screen parent) {
        super((Component)Component.literal("Move shark pop-up"));
        this.parent = parent;
    }

    private TikiConfig cfg() {
        return TikiGambleClient.config;
    }

    private int boxX() {
        return SharkPopup.boxX(this.cfg(), this.width);
    }

    private int boxY() {
        return SharkPopup.boxY(this.cfg(), this.height);
    }

    private int boxW() {
        return Math.round(SharkPopup.W * this.cfg().sharkPopupScale);
    }

    private int boxH() {
        return Math.round(SharkPopup.H * this.cfg().sharkPopupScale);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partial) {
        ctx.fill(0, 0, this.width, this.height, 0x60000000);
        ctx.centeredText(this.font, (Component)Component.literal("\u00a7dDrag the box \u00a77\u00b7 scroll to resize \u00a77\u00b7 right-click to reset \u00a77\u00b7 ESC when done"), this.width / 2, 12, -1);
        ctx.centeredText(this.font, (Component)Component.literal("\u00a78Size " + Math.round(this.cfg().sharkPopupScale * 100) + "%"), this.width / 2, 24, -1);
        long t = (System.currentTimeMillis() - this.opened) % 3600L;
        if (t < 3000L) {
            SharkPopup.draw(ctx, this.font, this.boxX(), this.boxY(), this.cfg().sharkPopupScale, t, "Megalodon");
        }
        ctx.outline(this.boxX() - 1, this.boxY() - 1, this.boxW() + 2, this.boxH() + 2, 0xFF4AA3FF);
        super.extractRenderState(ctx, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x();
        double my = event.y();
        boolean inside = mx >= this.boxX() && mx <= this.boxX() + this.boxW() && my >= this.boxY() && my <= this.boxY() + this.boxH();
        if (event.button() == 1) {
            this.cfg().sharkPopupX = -1f;
            this.cfg().sharkPopupY = -1f;
            this.cfg().sharkPopupScale = 1.0f;
            this.cfg().save();
            return true;
        }
        if (event.button() == 0 && inside) {
            this.dragging = true;
            this.grabX = mx - this.boxX();
            this.grabY = my - this.boxY();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.dragging) {
            double nx = Math.max(0, Math.min(this.width - this.boxW(), event.x() - this.grabX));
            double ny = Math.max(0, Math.min(this.height - this.boxH(), event.y() - this.grabY));
            this.cfg().sharkPopupX = (float)(nx / this.width);
            this.cfg().sharkPopupY = (float)(ny / this.height);
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
        float s = this.cfg().sharkPopupScale + (float)scrollY * 0.1f;
        this.cfg().sharkPopupScale = Math.max(0.5f, Math.min(3.0f, Math.round(s * 10f) / 10f));
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
