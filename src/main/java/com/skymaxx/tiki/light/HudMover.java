package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Light: drag a one-line HUD, scroll to resize, right-click to reset, ESC to finish (Fishing stats, Bestiary ranking). */
public final class HudMover extends Screen {
    /** Where the HUD sits: x / y as a share of the screen (-1 = default), scale. */
    public interface Spot {
        float x();

        float y();

        float scale();

        void set(float x, float y, float scale);

        int defaultX(int sw);

        int defaultY(int sh);
    }

    private final Screen parent;
    private final Spot spot;
    private final Supplier<String> text;
    private boolean dragging;
    private double grabX;
    private double grabY;

    public HudMover(Screen parent, String title, Spot spot, Supplier<String> text) {
        super(Component.literal(title));
        this.parent = parent;
        this.spot = spot;
        this.text = text;
    }

    private int bx() {
        return this.spot.x() < 0 ? this.spot.defaultX(this.width) : Math.round(this.spot.x() * this.width);
    }

    private int by() {
        return this.spot.y() < 0 ? this.spot.defaultY(this.height) : Math.round(this.spot.y() * this.height);
    }

    private int bw() {
        return Math.round(Bestiary.size(this.font, this.text.get())[0] * this.spot.scale());
    }

    private int bh() {
        return Math.round(Bestiary.size(this.font, this.text.get())[1] * this.spot.scale());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partial) {
        ctx.fill(0, 0, this.width, this.height, 0x60000000);
        ctx.centeredText(this.font, Component.literal("§dDrag the box §7· scroll to resize §7· right-click to reset §7· ESC when done"), this.width / 2, 12, -1);
        ctx.centeredText(this.font, Component.literal("§8Size " + Math.round(this.spot.scale() * 100) + "%"), this.width / 2, 24, -1);
        Bestiary.draw(ctx, this.font, this.bx(), this.by(), this.spot.scale(), this.text.get());
        ctx.outline(this.bx() - 1, this.by() - 1, this.bw() + 2, this.bh() + 2, 0xFFFFD24A);
        super.extractRenderState(ctx, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x(), my = event.y();
        if (event.button() == 1) {
            this.spot.set(-1f, -1f, 1f);
            save();
            return true;
        }
        if (event.button() == 0 && mx >= this.bx() && mx <= this.bx() + this.bw() && my >= this.by() && my <= this.by() + this.bh()) {
            this.dragging = true;
            this.grabX = mx - this.bx();
            this.grabY = my - this.by();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.dragging) {
            double nx = Math.max(0, Math.min(this.width - this.bw(), event.x() - this.grabX));
            double ny = Math.max(0, Math.min(this.height - this.bh(), event.y() - this.grabY));
            this.spot.set((float) (nx / this.width), (float) (ny / this.height), this.spot.scale());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.dragging) {
            this.dragging = false;
            save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float s = Math.max(0.5f, Math.min(3.0f, Math.round((this.spot.scale() + (float) scrollY * 0.1f) * 10f) / 10f));
        this.spot.set(this.spot.x(), this.spot.y(), s);
        save();
        return true;
    }

    private static void save() {
        if (TikiGambleClient.config != null) TikiGambleClient.config.save();
    }

    @Override
    public void onClose() {
        save();
        if (this.minecraft != null) this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Fishing stats HUD position. */
    public static Spot fishStats() {
        return new Spot() {
            public float x() { return TikiGambleClient.config.fishStatsX; }
            public float y() { return TikiGambleClient.config.fishStatsY; }
            public float scale() { return TikiGambleClient.config.fishStatsScale; }
            public void set(float x, float y, float s) {
                TikiGambleClient.config.fishStatsX = x;
                TikiGambleClient.config.fishStatsY = y;
                TikiGambleClient.config.fishStatsScale = s;
            }
            public int defaultX(int sw) { return 6; }
            public int defaultY(int sh) { return 42; }
        };
    }

    /** Bestiary ranking HUD position. */
    public static Spot bestiary() {
        return new Spot() {
            public float x() { return TikiGambleClient.config.bestiaryHudX; }
            public float y() { return TikiGambleClient.config.bestiaryHudY; }
            public float scale() { return TikiGambleClient.config.bestiaryHudScale; }
            public void set(float x, float y, float s) {
                TikiGambleClient.config.bestiaryHudX = x;
                TikiGambleClient.config.bestiaryHudY = y;
                TikiGambleClient.config.bestiaryHudScale = s;
            }
            public int defaultX(int sw) { return 6; }
            public int defaultY(int sh) { return 60; }
        };
    }
}
