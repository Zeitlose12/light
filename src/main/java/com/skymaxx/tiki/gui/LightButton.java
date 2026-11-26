package com.skymaxx.tiki.gui;

import com.skymaxx.tiki.light.Theme;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Light: the menu's own buttons in the colours of the menu theme. The look follows the label, so every place in
 * the menu keeps working exactly as before:
 *  - "Name: ON/OFF"            -> a row with a sliding pill switch on the right
 *  - "▼/▶ Title  ON/OFF"       -> a category bar with an accent stripe (and a small pill)
 *  - "✔ Name" / "✘ Name"       -> a round chip (sack list)
 *  - labels with "Clear"/"Reset" or red text -> a red action button
 *  - everything else           -> a round action button that glows when hovered
 */
public class LightButton extends Button {
    private static final Pattern TOGGLE = Pattern.compile("^(.*?):\\s*§([a7])(ON|OFF)\\s*$");
    private static final Pattern HEADER = Pattern.compile("^§6([▼▶]) (.*?)(?:\\s{2}§([a7])(ON|OFF))?\\s*$");
    private float knob = -1f;

    protected LightButton(int x, int y, int w, int h, Component message, OnPress onPress) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
    }

    public static Builder create(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    public static final class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x, y, w = 150, h = 20;
        private Tooltip tooltip;

        Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder bounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int w, int h) {
            this.w = w;
            this.h = h;
            return this;
        }

        public Builder width(int w) {
            this.w = w;
            return this;
        }

        public Builder tooltip(Tooltip t) {
            this.tooltip = t;
            return this;
        }

        public LightButton build() {
            LightButton b = new LightButton(this.x, this.y, this.w, this.h, this.message, this.onPress);
            if (this.tooltip != null) b.setTooltip(this.tooltip);
            return b;
        }
    }

    static int alpha(int a, int rgb) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
    }

    static String strip(String s) {
        return s.replaceAll("§.", "");
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partial) {
        Theme th = Theme.menu();
        Font font = Minecraft.getInstance().font;
        int x0 = this.getX(), y0 = this.getY(), x1 = x0 + this.getWidth(), y1 = y0 + this.getHeight();
        boolean hover = this.isHoveredOrFocused() && this.active;
        int acc = th.accent & 0xFFFFFF;
        String raw = this.getMessage().getString();
        int ty = y0 + (this.getHeight() - 8) / 2;

        Matcher hm = HEADER.matcher(raw);
        if (hm.matches()) {
            Theme.round(ctx, x0, y0, x1, y1, hover ? alpha(0x30, acc) : alpha(0x16, 0xFFFFFF));
            ctx.fill(x0 + 1, y0 + 3, x0 + 3, y1 - 3, alpha(0xFF, acc));
            String title = hm.group(1) + " " + hm.group(2);
            ctx.text(font, Component.literal(hm.group(1)), x0 + 8, ty, alpha(0xFF, acc));
            ctx.text(font, Component.literal(strip(hm.group(2))), x0 + 8 + font.width(hm.group(1) + " "), ty, th.text);
            if (hm.group(4) != null) pill(ctx, x1 - 30, y0 + (this.getHeight() - 12) / 2, "ON".equals(hm.group(4)), acc);
            return;
        }
        Matcher tm = TOGGLE.matcher(raw);
        if (tm.matches()) {
            Theme.round(ctx, x0, y0, x1, y1, hover ? alpha(0x26, acc) : alpha(0x12, 0xFFFFFF));
            String label = strip(tm.group(1));
            while (label.length() > 3 && font.width(label) > this.getWidth() - 44) label = label.substring(0, label.length() - 2);
            ctx.text(font, Component.literal(label), x0 + 7, ty, this.active ? th.text : th.dim);
            pill(ctx, x1 - 30, y0 + (this.getHeight() - 12) / 2, "ON".equals(tm.group(3)), acc);
            return;
        }
        String plain = strip(raw).trim();
        if (plain.startsWith("✔") || plain.startsWith("✘")) {
            boolean on = plain.startsWith("✔");
            Theme.round(ctx, x0, y0, x1, y1, on ? alpha(hover ? 0x40 : 0x26, acc) : alpha(hover ? 0x18 : 0x0A, 0xFFFFFF));
            Theme.roundOutline(ctx, x0, y0, x1, y1, on ? alpha(0xA0, acc) : alpha(0x30, 0xFFFFFF));
            String chip = plain;
            if (font.width(chip) > this.getWidth() - 8) {
                while (chip.length() > 3 && font.width(chip + "\u2026") > this.getWidth() - 8) chip = chip.substring(0, chip.length() - 1);
                chip = chip.trim() + "\u2026";
            }
            ctx.centeredText(font, Component.literal(chip), (x0 + x1) / 2, ty, on ? th.text : th.dim);
            return;
        }
        boolean danger = raw.contains("§c") || plain.startsWith("Clear") || plain.startsWith("Reset");
        int col = danger ? 0xFF6A6A : acc;
        if (hover) Theme.roundOutline(ctx, x0 - 1, y0 - 1, x1 + 1, y1 + 1, alpha(0x50, col));
        Theme.round(ctx, x0, y0, x1, y1, hover ? alpha(0x38, col) : alpha(0x14, 0xFFFFFF));
        Theme.roundOutline(ctx, x0, y0, x1, y1, alpha(hover ? 0xE0 : 0x70, col));
        Component msg = this.getMessage();
        String shown = plain;
        if (font.width(msg) > this.getWidth() - 8) {
            while (shown.length() > 3 && font.width(shown) > this.getWidth() - 12) shown = shown.substring(0, shown.length() - 2);
            msg = Component.literal(shown + "…");
        }
        int textColor = !this.active ? th.dim : (hover ? 0xFFFFFFFF : 0xFFFFFFFF);
        ctx.centeredText(font, msg, (x0 + x1) / 2, ty, textColor);
    }

    /** A pill switch 24 x 12; the knob glides to its side. */
    private void pill(GuiGraphicsExtractor ctx, int x, int y, boolean on, int acc) {
        float target = on ? 1f : 0f;
        if (this.knob < 0) this.knob = target;
        this.knob += (target - this.knob) * 0.3f;
        Theme.round(ctx, x, y, x + 24, y + 12, on ? alpha(0xD8, acc) : alpha(0x40, 0xFFFFFF));
        int k = x + 2 + Math.round(this.knob * 12);
        Theme.round(ctx, k, y + 2, k + 8, y + 10, 0xFFFFFFFF);
    }
}
