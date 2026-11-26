package com.skymaxx.tiki.gui;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class ConfigSlider
extends AbstractSliderButton {
    public static final float HEALTH_STEP = 250000.0f;
    private final String label;
    private final float min;
    private final float max;
    private final Format format;
    private final Consumer<Float> setter;
    private final Runnable onChange;

    public ConfigSlider(int x, int y, int width, int height, String label, float min, float max, Supplier<Float> getter, Consumer<Float> setter, Format format, Runnable onChange) {
        super(x, y, width, height, (Component)Component.empty(), (double)((getter.get().floatValue() - min) / Math.max(1.0E-5f, max - min)));
        this.label = label;
        this.min = min;
        this.max = max;
        this.format = format;
        this.setter = setter;
        this.onChange = onChange;
        this.updateMessage();
    }

    /** Light: a slim track with a round knob in the menu theme's colour; label left, value right. */
    @Override
    public void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partial) {
        com.skymaxx.tiki.light.Theme th = com.skymaxx.tiki.light.Theme.menu();
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        int x0 = this.getX(), y0 = this.getY(), x1 = x0 + this.getWidth(), y1 = y0 + this.getHeight();
        int acc = th.accent & 0xFFFFFF;
        boolean hover = this.isHoveredOrFocused();
        com.skymaxx.tiki.light.Theme.round(ctx, x0, y0, x1, y1, LightButton.alpha(hover ? 0x22 : 0x12, hover ? acc : 0xFFFFFF));
        String msg = LightButton.strip(this.getMessage().getString());
        int cut = msg.lastIndexOf(": ");
        String label = cut > 0 ? msg.substring(0, cut) : msg;
        String val = cut > 0 ? msg.substring(cut + 2) : "";
        int ty = y0 + (this.getHeight() - 8) / 2;
        int lw = font.width(label), vw = font.width(val);
        int tx0 = x0 + 7 + lw + 8, tx1 = x1 - 7 - vw - 8;
        double v = Math.max(0.0, Math.min(1.0, this.value));
        if (tx1 - tx0 >= 30) {
            ctx.text(font, Component.literal(label), x0 + 7, ty, th.dim);
            ctx.text(font, Component.literal(val), x1 - 7 - vw, ty, th.text);
            int my = y0 + this.getHeight() / 2;
            ctx.fill(tx0, my - 1, tx1, my + 1, LightButton.alpha(0x30, 0xFFFFFF));
            int kx = tx0 + (int) Math.round((tx1 - tx0) * v);
            ctx.fill(tx0, my - 1, kx, my + 1, LightButton.alpha(0xFF, acc));
            com.skymaxx.tiki.light.Theme.round(ctx, kx - 4, my - 4, kx + 4, my + 4, 0xFFFFFFFF);
        } else {
            // narrow: text centred, the track as a thin line along the bottom
            ctx.centeredText(font, Component.literal(label + ": " + val), (x0 + x1) / 2, ty - 1, th.text);
            int kx = x0 + 3 + (int) Math.round((this.getWidth() - 6) * v);
            ctx.fill(x0 + 3, y1 - 3, x1 - 3, y1 - 2, LightButton.alpha(0x30, 0xFFFFFF));
            ctx.fill(x0 + 3, y1 - 3, kx, y1 - 2, LightButton.alpha(0xFF, acc));
            ctx.fill(kx - 1, y1 - 5, kx + 2, y1, 0xFFFFFFFF);
        }
    }

    public float currentValue() {
        float raw = this.min + (float)this.value * (this.max - this.min);
        return this.format == Format.HEALTH ? (float)Math.round(raw / 250000.0f) * 250000.0f : raw;
    }

    protected void updateMessage() {
        float v = this.currentValue();
        Object text = switch (this.format.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> String.format("%.2fs", Float.valueOf(v));
            case 1 -> Math.round(v * 100.0f) + "%";
            case 2 -> Math.round(v) + "\u00b0";
            case 3 -> Math.round(v) + " BPM";
            case 4 -> String.format("%.2f", Float.valueOf(v));
            case 5 -> ConfigSlider.health(v);
            case 6 -> String.format(Locale.GERMANY, "%,d", Math.round(v) * 1_000_000L) + " Coins";
        };
        this.setMessage((Component)Component.literal((String)("\u00a77" + this.label + ": \u00a7f" + (String)text)));
    }

    private static String health(float v) {
        if (v >= 1000000.0f) {
            String text = String.format(Locale.ROOT, "%.2f", Float.valueOf(v / 1000000.0f));
            text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
            return text + "M";
        }
        if (v >= 1000.0f) {
            return Math.round(v / 1000.0f) + "k";
        }
        return v <= 0.0f ? "off" : String.valueOf(Math.round(v));
    }

    protected void applyValue() {
        this.setter.accept(Float.valueOf(this.currentValue()));
        if (this.onChange != null) {
            this.onChange.run();
        }
    }

    public static enum Format {
        SECONDS,
        PERCENT,
        DEGREES,
        BPM,
        PLAIN,
        HEALTH,
        MILLIONS;

    }
}

