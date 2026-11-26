package com.skymaxx.tiki.light;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.joml.Matrix3x2fStack;

/**
 * Light (Debug, test): "My Fishing Journey" - the drop history as a book. Opens closed, the cover swings open,
 * then page by page: contents + totals, one drop per page, and a tally of the kills without a drop.
 * Arrow keys, mouse wheel or a click on the left/right half turn the pages. ESC closes.
 */
public final class JournalScreen extends Screen {
    private static final long OPEN_MS = 1300L;
    private static final long FLIP_MS = 420L;
    private final Screen parent;
    private final long opened = System.currentTimeMillis();
    private final List<Object> pages = new ArrayList<>();
    private int spread;
    private int flipTo = -1;
    private long flipStart;

    public JournalScreen(Screen parent) {
        super(Component.literal("My Fishing Journey"));
        this.parent = parent;
        List<GambleLog.Entry> all = GambleLog.entries();
        pages.add("toc");
        pages.add("sum");
        Map<String, Integer> none = new LinkedHashMap<>();
        for (GambleLog.Entry e : all) {
            if (e.drop != null) pages.add(e);
            else none.merge(e.mobName, 1, Integer::sum);
        }
        if (pages.size() == 2) pages.add("empty");
        pages.add(none);
        if (pages.size() % 2 == 1) pages.add(null);
    }

    private int spreads() {
        return pages.size() / 2;
    }

    // ------------------------------------------------------------------ layout

    private int bookW() {
        return Math.min(this.width - 40, 380);
    }

    private int bookH() {
        return Math.min(this.height - 50, 240);
    }

    private int bookX() {
        return (this.width - bookW()) / 2;
    }

    private int bookY() {
        return (this.height - bookH()) / 2 - 6;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partial) {
        ctx.fill(0, 0, this.width, this.height, 0xB0100810);
        long t = System.currentTimeMillis() - opened;
        int bx = bookX(), by = bookY(), bw = bookW(), bh = bookH();
        int half = bw / 2;
        if (t < OPEN_MS) {
            drawOpening(ctx, t, bx, by, bw, bh);
            return;
        }
        // leather back
        ctx.fill(bx - 6, by - 6, bx + bw + 6, by + bh + 6, 0xFF5A2E14);
        ctx.fill(bx - 3, by - 3, bx + bw + 3, by + bh + 3, 0xFF7A4220);
        int shown = spread;
        page(ctx, bx, by, half, bh, pages.get(shown * 2), -1);
        page(ctx, bx + half, by, half, bh, pages.get(shown * 2 + 1), 1);
        // the page turning
        if (flipTo >= 0) {
            float u = Math.min(1f, (System.currentTimeMillis() - flipStart) / (float) FLIP_MS);
            int w = (int) (half * Math.abs(Math.cos(u * Math.PI)));
            boolean fwd = flipTo > spread;
            if (fwd) {
                if (u < 0.5f) {
                    ctx.fill(bx + half, by, bx + half + w, by + bh, 0xFFF3E6C4);
                    ctx.fill(bx + half, by, bx + half + w, by + bh, argb(0.3f * u * 2, 0x000000));
                } else {
                    page(ctx, bx + half, by, half, bh, pages.get(flipTo * 2 + 1), 1);
                    ctx.enableScissor(bx + half - w, by, bx + half, by + bh);
                    page(ctx, bx, by, half, bh, pages.get(flipTo * 2), -1);
                    ctx.disableScissor();
                }
            } else {
                if (u < 0.5f) {
                    ctx.fill(bx + half - w, by, bx + half, by + bh, 0xFFF3E6C4);
                    ctx.fill(bx + half - w, by, bx + half, by + bh, argb(0.3f * u * 2, 0x000000));
                } else {
                    page(ctx, bx, by, half, bh, pages.get(flipTo * 2), -1);
                    ctx.enableScissor(bx + half, by, bx + half + w, by + bh);
                    page(ctx, bx + half, by, half, bh, pages.get(flipTo * 2 + 1), 1);
                    ctx.disableScissor();
                }
            }
            if (u >= 1f) {
                spread = flipTo;
                flipTo = -1;
            }
        }
        ctx.fill(bx + half - 2, by, bx + half + 2, by + bh, 0x803C1E0A);
        String nav = "Page " + (spread * 2 + 1) + "-" + (spread * 2 + 2) + " of " + pages.size();
        ctx.centeredText(this.font, Component.literal("§8◀  §7" + nav + "  §8▶"), this.width / 2, by + bh + 12, -1);
        ctx.centeredText(this.font, Component.literal("§e(Debug test) §8Arrow keys / wheel / click to turn · ESC to close"), this.width / 2, by + bh + 24, -1);
        super.extractRenderState(ctx, mouseX, mouseY, partial);
    }

    /** The closed book, then the cover swings open from the spine. */
    private void drawOpening(GuiGraphicsExtractor ctx, long t, int bx, int by, int bw, int bh) {
        int half = bw / 2;
        float open = t < 650 ? 0f : Math.min(1f, (t - 650) / 650f);
        open = 1f - (1f - open) * (1f - open);
        // pages underneath (right half) once it opens
        if (open > 0) {
            ctx.fill(bx + half - 3, by - 3, bx + bw + 6, by + bh + 6, 0xFF5A2E14);
            page(ctx, bx + half, by, half, bh, pages.get(1), 1);
            int lw = (int) (half * open);
            ctx.fill(bx + half - lw - 6, by - 6, bx + half, by + bh + 6, 0xFF5A2E14);
            if (open > 0.5f) {
                ctx.enableScissor(bx + half - lw, by, bx + half, by + bh);
                page(ctx, bx, by, half, bh, pages.get(0), -1);
                ctx.disableScissor();
            }
        }
        // the cover (right half, folding over to the left)
        float cw = half * (float) Math.abs(Math.cos(open * Math.PI));
        boolean front = open < 0.5f;
        int x0 = front ? bx + half : (int) (bx + half - cw);
        int x1 = front ? (int) (bx + half + cw) : bx + half;
        if (cw > 1) {
            ctx.fill(x0 - (front ? 3 : 0), by - 6, x1 + (front ? 6 : 0), by + bh + 6, 0xFF6A3418);
            ctx.fill(x0, by - 2, x1, by + bh + 2, front ? 0xFF8A4A22 : 0xFF5A2E14);
            if (front && cw > half * 0.6f) {
                int cx = (x0 + x1) / 2;
                ctx.outline(x0 + 8, by + 6, x1 - x0 - 16, bh - 12, 0xFFD8A84A);
                ctx.outline(x0 + 11, by + 9, x1 - x0 - 22, bh - 18, 0x80D8A84A);
                Matrix3x2fStack ps = ctx.pose();
                ps.pushMatrix();
                ps.translate(cx, by + bh * 0.30f);
                ps.scale(1.6f, 1.6f);
                ctx.centeredText(this.font, Component.literal("My Fishing").withStyle(ChatFormatting.BOLD), 0, -10, 0xFFF0C860);
                ctx.centeredText(this.font, Component.literal("Journey").withStyle(ChatFormatting.BOLD), 0, 2, 0xFFF0C860);
                ps.popMatrix();
                drawFish(ctx, cx, (int) (by + bh * 0.58f), 0xFFD8A84A);
                ctx.centeredText(this.font, Component.literal("§6Light"), cx, by + bh - 26, -1);
            }
        }
        if (t < 80) playPage(0.7f);
    }

    /** A small golden fish emblem for the cover. */
    private void drawFish(GuiGraphicsExtractor ctx, int cx, int cy, int c) {
        for (int i = -12; i <= 12; i++) {
            int hh = (int) Math.round(Math.sqrt(Math.max(0, 144 - i * i)) * 0.55);
            ctx.fill(cx + i, cy - hh, cx + i + 1, cy + hh + 1, c);
        }
        for (int i = 0; i < 8; i++) ctx.fill(cx + 12 + i, cy - i, cx + 13 + i, cy + i + 1, c);
        ctx.fill(cx - 7, cy - 2, cx - 5, cy, 0xFF6A3418);
    }

    // ------------------------------------------------------------------ pages

    private void page(GuiGraphicsExtractor ctx, int x, int y, int w, int h, Object what, int side) {
        ctx.fill(x, y, x + w, y + h, 0xFFF3E6C4);
        if (side < 0) ctx.fillGradient(x + w - 18, y, x + w, y + h, 0x00785020, 0x00785020);
        for (int i = 0; i < 18; i++) ctx.fill(x + (i * 53) % (w - 4), y + (i * 37) % (h - 4), x + (i * 53) % (w - 4) + 3, y + (i * 37) % (h - 4) + 2, 0x10966E3C);
        int shade = side < 0 ? x + w - 10 : x;
        for (int i = 0; i < 10; i++) ctx.fill(shade + i, y, shade + i + 1, y + h, argb(0.03f * (side < 0 ? i : 10 - i), 0x784C1E));
        int cx = x + w / 2;
        if (what == null) return;
        if ("toc".equals(what)) {
            title(ctx, "Contents", cx, y + 14);
            String[] mobs = GambleLog.MOBS;
            String[] names = {"Lord Jawbus", "Thunder", "Ragnarok", "Plhlegblast", "Grim Reaper", "Yeti", "Reindrake"};
            boolean withTest = GambleLog.hasTest();
            for (int i = 0; i < mobs.length; i++) {
                int ly = y + 38 + i * 15;
                ctx.text(this.font, Component.literal(names[i]), x + 14, ly, 0xFF3A2410);
                String n = GambleLog.drops(mobs[i], withTest) + " drops";
                ctx.text(this.font, Component.literal(n), x + w - 12 - this.font.width(n), ly, 0xFF7A5A3A);
            }
            return;
        }
        if ("sum".equals(what)) {
            boolean withTest = GambleLog.hasTest();
            title(ctx, "Totals", cx, y + 14);
            GambleLog.Entry best = GambleLog.best(withTest);
            line(ctx, x, y + 38, "Total", LavaPrices.format(GambleLog.total(withTest)), 0xFF8A6A00);
            line(ctx, x, y + 68, "Best hit", best == null ? "-" : best.drop, 0xFFA04000);
            line(ctx, x, y + 98, "Drops", GambleLog.drops(null, withTest) + " of " + GambleLog.count(withTest) + " kills", 0xFF3A2410);
            line(ctx, x, y + 128, "RNG fanfares", GambleLog.fanfares(withTest) + " ★", 0xFF8A6A00);
            return;
        }
        if ("empty".equals(what)) {
            title(ctx, "No drops yet", cx, y + 14);
            ctx.centeredText(this.font, Component.literal("Your first gambling drop"), cx, y + 50, 0xFF7A5A3A);
            ctx.centeredText(this.font, Component.literal("gets its own page here."), cx, y + 62, 0xFF7A5A3A);
            return;
        }
        if (what instanceof Map<?, ?> none) {
            title(ctx, "Without a drop", cx, y + 14);
            int i = 0;
            for (Map.Entry<?, ?> me : none.entrySet()) {
                int ly = y + 40 + i * 32;
                if (ly > y + h - 24) break;
                ctx.text(this.font, Component.literal(String.valueOf(me.getKey())).withStyle(ChatFormatting.BOLD), x + 14, ly, 0xFF3A2410);
                int n = (Integer) me.getValue();
                for (int k = 0; k < Math.min(n, 40); k++) {
                    int gx = x + 14 + (k / 5) * 22 + (k % 5) * 4;
                    if (k % 5 == 4) {
                        for (int s = 0; s < 18; s++) ctx.fill(gx - 16 + s, ly + 22 - s / 3, gx - 15 + s, ly + 23 - s / 3, 0xFF5A3A1A);
                    } else {
                        ctx.fill(gx, ly + 12, gx + 1, ly + 24, 0xFF5A3A1A);
                    }
                }
                if (n > 40) ctx.text(this.font, Component.literal("+" + (n - 40)), x + w - 30, ly + 14, 0xFF7A5A3A);
                i++;
            }
            if (none.isEmpty()) ctx.centeredText(this.font, Component.literal("Every kill dropped something!"), cx, y + 50, 0xFF7A5A3A);
            return;
        }
        if (what instanceof GambleLog.Entry e) {
            int col = 0xFF000000 | LavaScene.colorOf(e.mob);
            title(ctx, e.mobName, cx, y + 12, col);
            ctx.fill(x + 24, y + 26, x + w - 24, y + 27, col);
            String date = new SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.GERMANY).format(new Date(e.ms));
            ctx.centeredText(this.font, Component.literal(date + (e.test ? " · test" : "")), cx, y + 32, 0xFF7A5A3A);
            int iy = y + 48;
            ctx.fill(cx - 26, iy, cx + 26, iy + 52, 0x203A2410);
            GambleArt.item(ctx, this.font, LavaScene.iconForDrop(e.drop), cx, iy + 26, 40, 1f);
            if (e.fanfare) ctx.text(this.font, Component.literal("★ RNG").withStyle(ChatFormatting.BOLD), x + w - 12 - this.font.width("★ RNG") - 4, iy + 2, 0xFFA07A00);
            ctx.centeredText(this.font, Component.literal(e.drop).withStyle(ChatFormatting.BOLD), cx, iy + 62, 0xFF5A1A7A);
            if (e.price > 0) ctx.centeredText(this.font, Component.literal(LavaPrices.format(e.price)).withStyle(ChatFormatting.BOLD), cx, iy + 76, 0xFF8A6A00);
            if (e.dry > 0) ctx.centeredText(this.font, Component.literal("after " + String.format(Locale.GERMANY, "%,d", e.dry) + " " + e.mobName), cx, iy + 92, 0xFF5A3A1A);
            ctx.fill(x + 30, y + h - 26, x + w - 30, y + h - 25, 0x403A2410);
            ctx.centeredText(this.font, Component.literal(e.share ? "\"A shared catch.\"" : "\"The roll held.\""), cx, y + h - 18, 0xFF7A5A3A);
        }
    }

    private void title(GuiGraphicsExtractor ctx, String text, int cx, int y) {
        title(ctx, text, cx, y, 0xFF3A2410);
    }

    private void title(GuiGraphicsExtractor ctx, String text, int cx, int y, int color) {
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(cx, y);
        ps.scale(1.3f, 1.3f);
        ctx.centeredText(this.font, Component.literal(text).withStyle(ChatFormatting.BOLD), 0, -4, color);
        ps.popMatrix();
    }

    private void line(GuiGraphicsExtractor ctx, int x, int y, String label, String value, int color) {
        ctx.text(this.font, Component.literal(label), x + 14, y, 0xFF7A5A3A);
        ctx.text(this.font, Component.literal(value).withStyle(ChatFormatting.BOLD), x + 14, y + 11, color);
    }

    // ------------------------------------------------------------------ input

    private void turn(int dir) {
        if (flipTo >= 0 || System.currentTimeMillis() - opened < OPEN_MS) return;
        int to = spread + dir;
        if (to < 0 || to >= spreads()) return;
        flipTo = to;
        flipStart = System.currentTimeMillis();
        playPage(1.0f);
    }

    private void playPage(float pitch) {
        if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, pitch, 0.8f));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 262) {
            turn(1);
            return true;
        }
        if (event.key() == 263) {
            turn(-1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        turn(scrollY < 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0) {
            turn(event.x() < this.width / 2.0 ? -1 : 1);
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int argb(float a, int rgb) {
        int al = Math.max(0, Math.min(255, Math.round(a * 255f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }
}
