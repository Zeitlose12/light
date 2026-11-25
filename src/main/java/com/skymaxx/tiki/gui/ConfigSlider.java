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
        HEALTH;

    }
}

