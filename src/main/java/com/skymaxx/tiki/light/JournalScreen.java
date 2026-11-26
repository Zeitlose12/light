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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.joml.Matrix3x2fStack;

/**
 * Light (Debug, test): "My Fishing Journey" - the drop history as a book. It lies closed, the cover swings open
 * on its spine, then you page through: contents + totals, one drop per page, a tally of the kills without a drop.
 * Everything on a page is laid out on a fixed 180 x 240 page and scaled up to the window, so text stays readable.
 * Arrow keys, mouse wheel or a click on the left/right half turn the pages. ESC closes.
 */
public final class JournalScreen extends Screen {
    private static final int PW = 180;
    private static final int PH = 240;
    private static final long HOLD_MS = 500L;
    private static final long OPEN_MS = 700L;
    private static final long FLIP_MS = 380L;
    private static final int PAPER = 0xFFF2E4C2;
    private static final int PAPER_EDGE = 0xFFE2CFA4;
    private static final int INK = 0xFF2E1C0C;
    private static final int INK_SOFT = 0xFF6A4E30;
    private static final int GOLD = 0xFFD8A84A;
    private static final int LEATHER = 0xFF6E3518;
    private static final int LEATHER_DARK = 0xFF4A2210;
    private final Screen parent;
    private final long opened = System.currentTimeMillis();
    private final List<Object> pages = new ArrayList<>();
    private int spread;
    private int flipTo = -1;
    private long flipStart;
    private boolean openSound;
    private boolean landSound;

    public JournalScreen(Screen parent) {
        super(Component.literal("My Fishing Journey"));
        this.parent = parent;
        Map<String, Integer> none = new LinkedHashMap<>();
        pages.add("toc");
        pages.add("sum");
        int drops = 0;
        for (GambleLog.Entry e : GambleLog.entries()) {
            if (e.drop != null) {
                pages.add(e);
                ++drops;
            } else {
                none.merge(e.mobName, 1, Integer::sum);
            }
        }
        if (drops == 0) pages.add("empty");
        pages.add(none);
        if (pages.size() % 2 == 1) pages.add("end");
    }

    private int spreads() {
        return pages.size() / 2;
    }

    /** Scale from page units to screen pixels: as big as fits, at least 1. */
    private float k() {
        return Math.max(1f, Math.min(1.8f, Math.min((this.width - 40) / (2f * PW + 16), (this.height - 56) / (PH + 16f))));
    }

    private void play(SoundEvent ev, float pitch, float vol) {
        if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, vol));
    }

    // ------------------------------------------------------------------ frame

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partial) {
        ctx.fill(0, 0, this.width, this.height, 0xC0080408);
        long t = System.currentTimeMillis() - opened;
        float k = k();
        float cx = this.width / 2f;
        float cy = this.height / 2f - 8;
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(cx, cy);
        ps.scale(k, k);
        // book space: spine at x = 0, pages from -PW..PW, y -PH/2..PH/2
        if (t < HOLD_MS + OPEN_MS) {
            drawOpening(ctx, t);
        } else {
            if (!landSound) {
                landSound = true;
                play(SoundEvents.BOOK_PUT, 1.0f, 0.7f);
            }
            drawOpenBook(ctx);
        }
        ps.popMatrix();
        if (t >= HOLD_MS + OPEN_MS) {
            int bottom = (int) (cy + (PH / 2f + 10) * k);
            String nav = "Page " + (spread * 2 + 1) + " - " + (spread * 2 + 2) + " of " + pages.size();
            ctx.centeredText(this.font, Component.literal((spread > 0 ? "§f◀  " : "§8◀  ") + "§7" + nav + (spread < spreads() - 1 ? "  §f▶" : "  §8▶")), this.width / 2, bottom + 4, -1);
            ctx.centeredText(this.font, Component.literal("§e(Debug test) §8Arrow keys / wheel / click to turn · ESC to close"), this.width / 2, bottom + 16, -1);
        }
        super.extractRenderState(ctx, mouseX, mouseY, partial);
    }

    /** Closed book in the middle; the cover turns over the spine (scaled flat, like a real hinge). */
    private void drawOpening(GuiGraphicsExtractor ctx, long t) {
        float u = t < HOLD_MS ? 0f : Math.min(1f, (t - HOLD_MS) / (float) OPEN_MS);
        float e = u < 0.5f ? 2 * u * u : 1 - (float) Math.pow(-2 * u + 2, 2) / 2;
        if (u > 0 && !openSound) {
            openSound = true;
            play(SoundEvents.BOOK_PAGE_TURN, 0.8f, 0.9f);
        }
        // closed: the cover is centred (spine at -PW/2); open: the spine is in the middle
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(-PW / 2f * (1 - e), 0);
        shadow(ctx, e);
        ctx.fill(-3, -PH / 2 - 5, PW + 5, PH / 2 + 5, LEATHER_DARK);
        page(ctx, 0, pages.get(1), 1);
        if (e > 0.5f) ctx.fill(-PW - 5, -PH / 2 - 5, 3, PH / 2 + 5, LEATHER_DARK);
        // the cover on its hinge: cos goes 1 -> -1, a negative scale lays it over to the left
        float sx = (float) Math.cos(e * Math.PI);
        ps.pushMatrix();
        ps.scale(Math.abs(sx) < 0.001f ? 0.001f : sx, 1);
        if (sx >= 0) {
            cover(ctx);
        } else {
            ps.pushMatrix();
            ps.scale(-1, 1);
            ctx.fill(-PW - 5, -PH / 2 - 5, 3, PH / 2 + 5, LEATHER);
            page(ctx, -PW, pages.get(0), -1);
            ps.popMatrix();
        }
        int shade = (int) ((1 - Math.abs(sx)) * 90);
        ctx.fill(-3, -PH / 2 - 5, PW + 5, PH / 2 + 5, shade << 24);
        ps.popMatrix();
        ps.popMatrix();
    }

    private void drawOpenBook(GuiGraphicsExtractor ctx) {
        shadow(ctx, 1f);
        ctx.fill(-PW - 6, -PH / 2 - 6, PW + 6, PH / 2 + 6, LEATHER_DARK);
        ctx.fill(-PW - 4, -PH / 2 - 4, PW + 4, PH / 2 + 4, LEATHER);
        page(ctx, -PW, pages.get(spread * 2), -1);
        page(ctx, 0, pages.get(spread * 2 + 1), 1);
        if (flipTo >= 0) {
            float u = Math.min(1f, (System.currentTimeMillis() - flipStart) / (float) FLIP_MS);
            float sx = (float) Math.cos(u * Math.PI);
            boolean fwd = flipTo > spread;
            Matrix3x2fStack ps = ctx.pose();
            if (fwd) {
                // the right page lifts and lands on the left; underneath the next right page shows
                page(ctx, 0, pages.get(flipTo * 2 + 1), 1);
                ps.pushMatrix();
                ps.scale(Math.abs(sx) < 0.001f ? 0.001f : sx, 1);
                if (sx >= 0) page(ctx, 0, pages.get(spread * 2 + 1), 1);
                else {
                    ps.pushMatrix();
                    ps.scale(-1, 1);
                    page(ctx, -PW, pages.get(flipTo * 2), -1);
                    ps.popMatrix();
                }
                ctx.fill(0, -PH / 2, PW, PH / 2, ((int) ((1 - Math.abs(sx)) * 80) << 24));
                ps.popMatrix();
            } else {
                page(ctx, -PW, pages.get(flipTo * 2), -1);
                ps.pushMatrix();
                ps.scale(Math.abs(sx) < 0.001f ? 0.001f : sx, 1);
                if (sx >= 0) page(ctx, -PW, pages.get(spread * 2), -1);
                else {
                    ps.pushMatrix();
                    ps.scale(-1, 1);
                    page(ctx, 0, pages.get(flipTo * 2 + 1), 1);
                    ps.popMatrix();
                }
                ctx.fill(-PW, -PH / 2, 0, PH / 2, ((int) ((1 - Math.abs(sx)) * 80) << 24));
                ps.popMatrix();
            }
            if (u >= 1f) {
                spread = flipTo;
                flipTo = -1;
            }
        }
        // spine and ribbon
        ctx.fill(-1, -PH / 2, 1, PH / 2, 0x50402010);
        ctx.fill(-14, -PH / 2 - 4, -9, PH / 2 + 16, 0xFFA02020);
        ctx.fill(-14, PH / 2 + 12, -9, PH / 2 + 16, 0xFF701414);
    }

    private void shadow(GuiGraphicsExtractor ctx, float open) {
        int w = (int) (PW * (0.5f + 0.5f * open)) + 14;
        for (int i = 0; i < 6; i++) ctx.fill(-w - i * 2, PH / 2 + 4 + i, w + i * 2, PH / 2 + 6 + i, 0x14000000);
    }

    /** The closed front cover (drawn from the spine to the right). */
    private void cover(GuiGraphicsExtractor ctx) {
        ctx.fill(-3, -PH / 2 - 5, PW + 5, PH / 2 + 5, LEATHER);
        ctx.fill(-3, -PH / 2 - 5, 9, PH / 2 + 5, LEATHER_DARK);
        for (int yy : new int[]{-PH / 2 + 18, -PH / 2 + 24, PH / 2 - 26, PH / 2 - 20}) ctx.fill(-3, yy, 9, yy + 2, GOLD);
        ctx.outline(16, -PH / 2 + 8, PW - 26, PH - 16, GOLD);
        ctx.outline(20, -PH / 2 + 12, PW - 34, PH - 24, 0x80D8A84A);
        for (int[] c : new int[][]{{16, -PH / 2 + 8}, {PW - 16, -PH / 2 + 8}, {16, PH / 2 - 14}, {PW - 16, PH / 2 - 14}}) {
            ctx.fill(c[0] - 3, c[1] - 3, c[0] + 4, c[1] + 4, GOLD);
        }
        int mid = (PW + 6) / 2 + 3;
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(mid, -PH / 2 + 62);
        ps.scale(1.6f, 1.6f);
        ctx.centeredText(this.font, Component.literal("My Fishing").withStyle(ChatFormatting.BOLD), 0, -10, 0xFFF0C860);
        ctx.centeredText(this.font, Component.literal("Journey").withStyle(ChatFormatting.BOLD), 0, 2, 0xFFF0C860);
        ps.popMatrix();
        fish(ctx, mid, 14, GOLD);
        ctx.centeredText(this.font, Component.literal("Light"), mid, PH / 2 - 40, GOLD);
    }

    private void fish(GuiGraphicsExtractor ctx, int cx, int cy, int c) {
        for (int i = -16; i <= 16; i++) {
            int hh = (int) Math.round(Math.sqrt(Math.max(0, 256 - i * i)) * 0.55);
            ctx.fill(cx + i, cy - hh, cx + i + 1, cy + hh + 1, c);
        }
        for (int i = 0; i < 10; i++) ctx.fill(cx + 16 + i, cy - i, cx + 17 + i, cy + i + 1, c);
        ctx.fill(cx - 10, cy - 3, cx - 7, cy, LEATHER);
        for (int i = 0; i < 3; i++) ctx.fill(cx - 4 + i * 6, cy - 6 + i, cx - 3 + i * 6, cy + 6 - i, LEATHER);
    }

    // ------------------------------------------------------------------ one page at (x0 .. x0+PW, -PH/2 .. PH/2)

    private void page(GuiGraphicsExtractor ctx, int x0, Object what, int side) {
        int top = -PH / 2;
        ctx.fill(x0, top, x0 + PW, top + PH, PAPER);
        // page edges and a soft curve towards the spine
        ctx.fill(x0, top, x0 + PW, top + 1, PAPER_EDGE);
        ctx.fill(x0, top + PH - 1, x0 + PW, top + PH, PAPER_EDGE);
        int sp = side < 0 ? x0 + PW - 12 : x0;
        for (int i = 0; i < 12; i++) ctx.fill(sp + i, top, sp + i + 1, top + PH, ((side < 0 ? i : 11 - i) * 3) << 24 | 0x5A3A12);
        ctx.outline(x0 + 8, top + 8, PW - 16, PH - 16, 0x406A4E30);
        int cx = x0 + PW / 2;
        int index = pages.indexOf(what);
        if (index >= 0) ctx.centeredText(this.font, Component.literal(String.valueOf(index + 1)), cx, top + PH - 18, 0x906A4E30);
        if (what == null || "end".equals(what)) return;
        if ("toc".equals(what)) {
            heading(ctx, "Contents", cx, top + 22);
            String[] names = {"Lord Jawbus", "Thunder", "Ragnarok", "Plhlegblast", "Grim Reaper", "Yeti", "Reindrake"};
            boolean withTest = GambleLog.hasTest();
            for (int i = 0; i < names.length; i++) {
                int ly = top + 46 + i * 20;
                ctx.text(this.font, Component.literal(names[i]), x0 + 16, ly, INK);
                String n = String.valueOf(GambleLog.drops(GambleLog.MOBS[i], withTest));
                ctx.text(this.font, Component.literal(n), x0 + PW - 16 - this.font.width(n), ly, INK_SOFT);
                int dotsFrom = x0 + 20 + this.font.width(names[i]);
                for (int d = dotsFrom; d < x0 + PW - 22 - this.font.width(n); d += 4) ctx.fill(d, ly + 7, d + 1, ly + 8, 0x806A4E30);
            }
            ctx.centeredText(this.font, Component.literal("drops per mob"), cx, top + 196, INK_SOFT);
            return;
        }
        if ("sum".equals(what)) {
            boolean withTest = GambleLog.hasTest();
            heading(ctx, "Totals", cx, top + 22);
            GambleLog.Entry best = GambleLog.best(withTest);
            stat(ctx, x0, top + 46, "Total", LavaPrices.format(GambleLog.total(withTest)), 0xFF8A6400);
            stat(ctx, x0, top + 80, "Best hit", best == null ? "-" : best.drop, 0xFF9A3A00);
            stat(ctx, x0, top + 114, "Drops", GambleLog.drops(null, withTest) + " of " + GambleLog.count(withTest) + " kills", INK);
            stat(ctx, x0, top + 148, "RNG fanfares", GambleLog.fanfares(withTest) + " ★", 0xFF8A6400);
            return;
        }
        if ("empty".equals(what)) {
            heading(ctx, "No drops yet", cx, top + 22);
            ctx.centeredText(this.font, Component.literal("Your first gambling"), cx, top + 70, INK_SOFT);
            ctx.centeredText(this.font, Component.literal("drop gets its own"), cx, top + 82, INK_SOFT);
            ctx.centeredText(this.font, Component.literal("page right here."), cx, top + 94, INK_SOFT);
            return;
        }
        if (what instanceof Map<?, ?> none) {
            heading(ctx, "Without a drop", cx, top + 22);
            int i = 0;
            for (Map.Entry<?, ?> me : none.entrySet()) {
                int ly = top + 44 + i * 30;
                if (ly > top + PH - 50) break;
                int n = (Integer) me.getValue();
                ctx.text(this.font, Component.literal(String.valueOf(me.getKey())), x0 + 16, ly, INK);
                ctx.text(this.font, Component.literal(String.valueOf(n)), x0 + PW - 16 - this.font.width(String.valueOf(n)), ly, INK_SOFT);
                for (int q = 0; q < Math.min(n, 25); q++) {
                    int gx = x0 + 16 + (q / 5) * 24 + (q % 5) * 4;
                    if (q % 5 == 4) {
                        for (int s = 0; s < 20; s++) ctx.fill(gx - 18 + s, ly + 21 - s / 3, gx - 17 + s, ly + 22 - s / 3, 0xFF4A2E10);
                    } else {
                        ctx.fill(gx, ly + 11, gx + 1, ly + 23, 0xFF4A2E10);
                    }
                }
                i++;
            }
            if (none.isEmpty()) ctx.centeredText(this.font, Component.literal("Every kill dropped!"), cx, top + 70, INK_SOFT);
            return;
        }
        if (what instanceof GambleLog.Entry e) {
            int col = 0xFF000000 | darker(LavaScene.colorOf(e.mob));
            heading(ctx, e.mobName, cx, top + 22, col);
            ctx.fill(x0 + 30, top + 34, x0 + PW - 30, top + 35, col);
            ctx.fill(cx - 2, top + 33, cx + 2, top + 36, col);
            String date = new SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.GERMANY).format(new Date(e.ms));
            ctx.centeredText(this.font, Component.literal(date), cx, top + 42, INK_SOFT);
            if (e.test) ctx.centeredText(this.font, Component.literal("test entry"), cx, top + 52, 0xFFB08000);
            ctx.fill(cx - 30, top + 64, cx + 30, top + 124, 0x18402010);
            ctx.outline(cx - 30, top + 64, 60, 60, 0x506A4E30);
            GambleArt.item(ctx, this.font, LavaScene.iconForDrop(e.drop), cx, top + 94, 44, 1f);
            if (e.fanfare) {
                ctx.text(this.font, Component.literal("★ RNG").withStyle(ChatFormatting.BOLD), cx + 36, top + 66, 0xFFA07400);
            }
            ctx.centeredText(this.font, Component.literal(e.drop).withStyle(ChatFormatting.BOLD), cx, top + 134, 0xFF4A1468);
            if (e.price > 0) ctx.centeredText(this.font, Component.literal(LavaPrices.format(e.price)), cx, top + 148, 0xFF8A6400);
            if (e.dry > 0) ctx.centeredText(this.font, Component.literal("after " + String.format(Locale.GERMANY, "%,d", e.dry) + " " + e.mobName), cx, top + 162, INK);
            if (e.share) ctx.centeredText(this.font, Component.literal("(loot share)"), cx, top + 174, INK_SOFT);
            ctx.fill(x0 + 40, top + PH - 32, x0 + PW - 40, top + PH - 31, 0x406A4E30);
        }
    }

    private static int darker(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return ((int) (r * 0.6) << 16) | ((int) (g * 0.6) << 8) | (int) (b * 0.6);
    }

    private void heading(GuiGraphicsExtractor ctx, String text, int cx, int y) {
        heading(ctx, text, cx, y, INK);
    }

    private void heading(GuiGraphicsExtractor ctx, String text, int cx, int y, int color) {
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(cx, y);
        float s = Math.min(1.4f, (PW - 24f) / Math.max(1, this.font.width(text)));
        ps.scale(s, s);
        ctx.centeredText(this.font, Component.literal(text).withStyle(ChatFormatting.BOLD), 0, -4, color);
        ps.popMatrix();
    }

    private void stat(GuiGraphicsExtractor ctx, int x0, int y, String label, String value, int color) {
        ctx.text(this.font, Component.literal(label), x0 + 16, y, INK_SOFT);
        String v = value;
        while (v.length() > 3 && this.font.width(v) > PW - 34) v = v.substring(0, v.length() - 1);
        ctx.text(this.font, Component.literal(v).withStyle(ChatFormatting.BOLD), x0 + 16, y + 12, color);
    }

    // ------------------------------------------------------------------ input

    private void turn(int dir) {
        if (flipTo >= 0 || System.currentTimeMillis() - opened < HOLD_MS + OPEN_MS) return;
        int to = spread + dir;
        if (to < 0 || to >= spreads()) return;
        flipTo = to;
        flipStart = System.currentTimeMillis();
        play(SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.8f);
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
}
