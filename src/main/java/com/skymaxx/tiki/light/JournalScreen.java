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
    private static final int PAPER = 0xFFE9D6AE;
    private static final int PAPER_EDGE = 0xFFC9AE7C;
    private static final int RANK_INK = 0xFF6A3A1A; // warm sepia ink
    private static final int INK = 0xFF1A0E04;
    private static final int GOLD_INK = 0xFF7A4E00;
    private static final int ITEM_INK = 0xFF3C0C5C;
    private static final int INK_SOFT = 0xFF3A2410;
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
        pages.add("ranks");
        pages.add("sum");
        int drops = 0;
        // one page per mob + drop (newest first); a new drop of the same kind only raises its count
        Map<String, Group> groups = new LinkedHashMap<>();
        for (GambleLog.Entry e : GambleLog.entries()) {
            if (GambleLog.worthy(e)) {
                Group g = groups.computeIfAbsent(e.mob + "|" + LavaPrices.norm(e.drop), k -> new Group(e));
                g.count++;
                if (e.price > g.best) g.best = e.price;
                if (e.fanfare) g.fanfare = true;
                if (e.share) g.shares++;
            } else {
                none.merge(e.mobName, 1, Integer::sum);
            }
        }
        for (Group g : groups.values()) {
            pages.add(g);
            ++drops;
        }
        if (drops == 0) pages.add("empty");
        pages.add(none);
        if (pages.size() % 2 == 1) pages.add("end");
    }

    /** All drops of one kind from one mob: the newest one plus how often it dropped. */
    private static final class Group {
        final GambleLog.Entry last;
        int count;
        int shares;
        long best = -1L;
        boolean fanfare;

        Group(GambleLog.Entry last) {
            this.last = last;
        }
    }

    private int spreads() {
        return pages.size() / 2;
    }

    /** Scale from page units to GUI pixels: whole numbers only (1x, 2x), so the pixel font stays sharp. */
    private float k() {
        float fit = Math.min((this.width - 40) / (2f * PW + 16), (this.height - 56) / (PH + 16f));
        return Math.max(1f, Math.min(1.8f, fit));
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
            int bottom = (int) (cy + (PH / 2f + 16) * k);
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
        ribbon(ctx);
    }

    /** A silk bookmark lying in the gutter along the spine (never over the text), hanging out below with a V cut. */
    private void ribbon(GuiGraphicsExtractor ctx) {
        int top = -PH / 2 - 4;
        int inside = PH / 2;
        ctx.fill(3, top, 5, inside, 0x28000000);
        ctx.fill(-2, top, -1, inside, 0xFF5A0E10);
        ctx.fill(-1, top, 1, inside, 0xFFA8202A);
        ctx.fill(1, top, 2, inside, 0xFFD24A54);
        ctx.fill(2, top, 3, inside, 0xFF7A1418);
        int len = 12;
        for (int i = 0; i < len; i += 2) {
            int dx = (int) (i * 0.35f);
            int y = inside + i;
            ctx.fill(-2 + dx, y, -1 + dx, y + 2, 0xFF5A0E10);
            ctx.fill(-1 + dx, y, 1 + dx, y + 2, 0xFFA8202A);
            ctx.fill(1 + dx, y, 2 + dx, y + 2, 0xFFD24A54);
            ctx.fill(2 + dx, y, 3 + dx, y + 2, 0xFF7A1418);
        }
        int ex = (int) (len * 0.35f);
        ctx.fill(ex, inside + len - 2, ex + 1, inside + len, 0xFF140A0C);
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

    private static float hsh(int i) {
        double v = Math.sin(i * 12.9898 + 78.233) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    /** Old paper: grain, foxing spots, darker worn edges, a ragged outer edge, sometimes a coffee ring. Fixed per page. */
    private void agedPaper(GuiGraphicsExtractor ctx, int x0, int top, int side, int seed) {
        ctx.fill(x0, top, x0 + PW, top + PH, PAPER);
        for (int i = 0; i < 260; i++) {
            int gx = x0 + (int) (hsh(seed * 977 + i) * PW), gy = top + (int) (hsh(seed * 613 + i * 3) * PH);
            int a = 10 + (int) (hsh(i * 7 + seed) * 22);
            ctx.fill(gx, gy, gx + 1, gy + 1, a << 24 | (hsh(i * 11 + seed) > 0.5f ? 0x6A4418 : 0x8A6A3A));
        }
        for (int i = 0; i < 7; i++) {
            int fx = x0 + 10 + (int) (hsh(seed * 31 + i * 17) * (PW - 20)), fy = top + 10 + (int) (hsh(seed * 37 + i * 19) * (PH - 20));
            int r = 1 + (int) (hsh(seed * 41 + i) * 3);
            for (int k = r; k >= 1; k--) ctx.fill(fx - k, fy - k, fx + k, fy + k, (int) (14 + 10 * (r - k + 1)) << 24 | 0x8C5A28);
        }
        // worn edges: top, bottom and the outer side get darker
        for (int k = 0; k < 9; k++) {
            int a = (9 - k) * 6;
            ctx.fill(x0, top + k, x0 + PW, top + k + 1, a << 24 | 0x5A3410);
            ctx.fill(x0, top + PH - 1 - k, x0 + PW, top + PH - k, a << 24 | 0x5A3410);
            int ex = side < 0 ? x0 + k : x0 + PW - 1 - k;
            ctx.fill(ex, top, ex + 1, top + PH, a << 24 | 0x5A3410);
        }
        // ragged outer edge (the leather shows through)
        int edge = side < 0 ? x0 : x0 + PW;
        for (int y = top; y < top + PH; y += 3) {
            int d = (int) (hsh(seed * 53 + y) * 2.5f);
            if (d > 0) ctx.fill(side < 0 ? edge : edge - d, y, side < 0 ? edge + d : edge, y + 3, LEATHER);
        }
        // a faint coffee ring on some pages
        if (hsh(seed * 71) > 0.72f) {
            int cx = x0 + 30 + (int) (hsh(seed * 73) * (PW - 60)), cy = top + PH - 50 - (int) (hsh(seed * 79) * 60);
            for (int d = 0; d < 360; d += 6) {
                if (d > 300) continue;
                double rad = Math.toRadians(d);
                int px = cx + (int) Math.round(Math.cos(rad) * 17), py = cy + (int) Math.round(Math.sin(rad) * 17);
                ctx.fill(px, py, px + 2, py + 1, 0x2A784B23);
            }
        }
    }

    private void page(GuiGraphicsExtractor ctx, int x0, Object what, int side) {
        int top = -PH / 2;
        agedPaper(ctx, x0, top, side, Math.max(0, pages.indexOf(what)) + 1);
        // page edges and a soft curve towards the spine
        ctx.fill(x0, top, x0 + PW, top + 1, PAPER_EDGE);
        ctx.fill(x0, top + PH - 1, x0 + PW, top + PH, PAPER_EDGE);
        int sp = side < 0 ? x0 + PW - 12 : x0;
        for (int i = 0; i < 12; i++) ctx.fill(sp + i, top, sp + i + 1, top + PH, ((side < 0 ? i : 11 - i) * 3) << 24 | 0x5A3A12);
        ctx.outline(x0 + 8, top + 8, PW - 16, PH - 16, 0x406A4E30);
        int cx = x0 + PW / 2;
        int index = pages.indexOf(what);
        if (index >= 0) ink(ctx, Component.literal(String.valueOf(index + 1)), cx, top + PH - 18, 0x906A4E30);
        if (what == null || "end".equals(what)) return;
        if ("toc".equals(what)) {
            heading(ctx, "Contents", cx, top + 22);
            String[] names = GambleLog.JOURNAL_NAMES;
            boolean withTest = GambleLog.hasTest();
            String[] special = {"Bestiary Rankings", "Total Drops"};
            for (int i = 0; i < special.length; i++) {
                int ly = top + 40 + i * 13;
                String n = String.valueOf(i + 2);
                ctx.text(this.font, Component.literal(special[i]).withStyle(ChatFormatting.ITALIC), x0 + 16, ly, RANK_INK, false);
                ctx.text(this.font, Component.literal(n), x0 + PW - 16 - this.font.width(n), ly, RANK_INK, false);
                int dotsFrom = x0 + 20 + this.font.width(Component.literal(special[i]).withStyle(ChatFormatting.ITALIC));
                for (int d = dotsFrom; d < x0 + PW - 22 - this.font.width(n); d += 4) ctx.fill(d, ly + 7, d + 1, ly + 8, 0x806A3A1A);
            }
            ctx.fill(x0 + 40, top + 68, x0 + PW - 40, top + 69, 0x406A4E30);
            for (int i = 0; i < names.length; i++) {
                int ly = top + 74 + i * 13;
                ctx.text(this.font, Component.literal(names[i]), x0 + 16, ly, INK, false);
                String n = String.valueOf(GambleLog.worthyDrops(GambleLog.JOURNAL[i], withTest));
                ctx.text(this.font, Component.literal(n), x0 + PW - 16 - this.font.width(n), ly, INK_SOFT, false);
                int dotsFrom = x0 + 20 + this.font.width(names[i]);
                for (int d = dotsFrom; d < x0 + PW - 22 - this.font.width(n); d += 4) ctx.fill(d, ly + 7, d + 1, ly + 8, 0x806A4E30);
            }
            ink(ctx, Component.literal("drops per mob"), cx, top + 196, INK_SOFT);
            return;
        }
        if ("ranks".equals(what)) {
            heading(ctx, "Bestiary Rankings", cx, top + 22);
            ink(ctx, Component.literal("your place among all players").withStyle(ChatFormatting.ITALIC), cx, top + 34, INK_SOFT);
            java.util.List<Bestiary.Rank> all = new java.util.ArrayList<>();
            for (String n : GambleLog.JOURNAL_NAMES) {
                Bestiary.Rank r = Bestiary.get(n);
                if (r != null) all.add(r);
            }
            if (all.isEmpty()) {
                ink(ctx, Component.literal("Open a sea creature"), cx, top + 80, INK_SOFT);
                ink(ctx, Component.literal("in your Bestiary (/be)"), cx, top + 92, INK_SOFT);
                ink(ctx, Component.literal("and its place shows"), cx, top + 104, INK_SOFT);
                ink(ctx, Component.literal("up right here."), cx, top + 116, INK_SOFT);
                return;
            }
            int i = 0;
            for (Bestiary.Rank r : all) {
                int ly = top + 48 + i * 21;
                if (ly > top + PH - 44) break;
                ctx.text(this.font, Component.literal(r.mob), x0 + 16, ly, INK, false);
                String place = "#" + String.format(Locale.ROOT, "%,d", r.rank);
                Component pc = Component.literal(place);
                ctx.text(this.font, pc, x0 + PW - 16 - this.font.width(pc), ly, RANK_INK, false);
                long ch = r.change();
                String sub = r.top() + (ch > 0 ? "  \u2191" + String.format(Locale.ROOT, "%,d", ch) : ch < 0 ? "  \u2193" + String.format(Locale.ROOT, "%,d", -ch) : "");
                ps2(ctx, sub, x0 + 16, ly + 10, INK_SOFT);
                if (r.stale()) {
                    long days = (System.currentTimeMillis() - r.ms) / 86_400_000L;
                    String old = "\u25cf " + days + " days old";
                    ps2(ctx, old, x0 + PW - 16 - Math.round(this.font.width(old) * 0.75f), ly + 10, 0xFFB8661A);
                }
                ctx.fill(x0 + 16, ly + 18, x0 + PW - 16, ly + 19, 0x306A4E30);
                i++;
            }
            ps2(ctx, "updated when you open /be", x0 + 16, top + PH - 30, INK_SOFT);
            return;
        }
        if ("sum".equals(what)) {
            boolean withTest = GambleLog.hasTest();
            heading(ctx, "Total Drops", cx, top + 22);
            GambleLog.Entry best = GambleLog.best(withTest);
            stat(ctx, x0, top + 40, "Total", LavaPrices.format(GambleLog.total(withTest)), 0xFF7A4E00);
            stat(ctx, x0, top + 66, "Best hit", best == null ? "-" : LavaPrices.display(best.drop), 0xFF9A3A00);
            stat(ctx, x0, top + 92, "Drops", GambleLog.drops(null, withTest) + " of " + GambleLog.count(withTest) + " kills", INK);
            stat(ctx, x0, top + 118, "RNG fanfares", GambleLog.fanfares(withTest) + " \u2605", 0xFF7A4E00);
            // per mob: drops and value
            ctx.fill(x0 + 40, top + 146, x0 + PW - 40, top + 147, 0x406A4E30);
            int i = 0;
            for (int m = 0; m < GambleLog.JOURNAL.length && i < 6; m++) {
                int n = 0;
                long sum = 0;
                for (GambleLog.Entry e : GambleLog.entries()) {
                    if ((!withTest && e.test) || !GambleLog.JOURNAL[m].equals(e.mob) || !GambleLog.worthy(e)) continue;
                    n++;
                    if (e.price > 0) sum += e.price;
                }
                if (n == 0) continue;
                int ly = top + 152 + i * 11;
                ps2(ctx, GambleLog.JOURNAL_NAMES[m], x0 + 16, ly, INK);
                String v = n + (sum > 0 ? " \u00b7 " + LavaPrices.format(sum) : "");
                ps2(ctx, v, x0 + PW - 16 - Math.round(this.font.width(v) * 0.75f), ly, INK_SOFT);
                i++;
            }
            return;
        }
        if ("empty".equals(what)) {
            heading(ctx, "No drops yet", cx, top + 22);
            ink(ctx, Component.literal("Your first gambling"), cx, top + 70, INK_SOFT);
            ink(ctx, Component.literal("drop gets its own"), cx, top + 82, INK_SOFT);
            ink(ctx, Component.literal("page right here."), cx, top + 94, INK_SOFT);
            return;
        }
        if (what instanceof Map<?, ?> none) {
            heading(ctx, "Without a drop", cx, top + 22);
            int i = 0;
            for (Map.Entry<?, ?> me : none.entrySet()) {
                int ly = top + 44 + i * 30;
                if (ly > top + PH - 50) break;
                int n = (Integer) me.getValue();
                ctx.text(this.font, Component.literal(String.valueOf(me.getKey())), x0 + 16, ly, INK, false);
                ctx.text(this.font, Component.literal(String.valueOf(n)), x0 + PW - 16 - this.font.width(String.valueOf(n)), ly, INK_SOFT, false);
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
            if (none.isEmpty()) ink(ctx, Component.literal("Every kill dropped!"), cx, top + 70, INK_SOFT);
            return;
        }
        if (what instanceof Group g) {
            GambleLog.Entry e = g.last;
            int col = 0xFF000000 | darker(LavaScene.colorOf(e.mob));
            heading(ctx, e.mobName, cx, top + 22, col);
            ctx.fill(x0 + 30, top + 34, x0 + PW - 30, top + 35, col);
            ctx.fill(cx - 2, top + 33, cx + 2, top + 36, col);
            String date = (g.count > 1 ? "last: " : "") + new SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.GERMANY).format(new Date(e.ms));
            ink(ctx, Component.literal(date), cx, top + 42, INK);
            if (e.test) ink(ctx, Component.literal("test entry"), cx, top + 53, 0xFF8A3A00);
            ctx.fill(cx - 30, top + 64, cx + 30, top + 124, 0x18402010);
            ctx.outline(cx - 30, top + 64, 60, 60, 0x506A4E30);
            GambleArt.item(ctx, this.font, LavaScene.iconForDrop(e.drop), cx, top + 94, 44, 1f);
            if (g.fanfare) {
                ctx.text(this.font, Component.literal("★ RNG").withStyle(ChatFormatting.BOLD), cx + 36, top + 66, 0xFF8A5A00, false);
            }
            String name = LavaPrices.display(e.drop) + (g.count > 1 ? "  \u00d7" + g.count : "");
            ink(ctx, Component.literal(name).withStyle(ChatFormatting.BOLD), cx, top + 134, ITEM_INK);
            Long now = LavaPrices.journalPrice(e.drop); // current price: sell offer (bazaar) or lowest BIN (auction)
            long price = now != null ? now : (e.price > 0 ? e.price : g.best);
            if (price > 0) ink(ctx, Component.literal(LavaPrices.format(price)).withStyle(ChatFormatting.BOLD), cx, top + 148, GOLD_INK);
            if (e.dry > 0) ink(ctx, Component.literal((g.count > 1 ? "last after " : "after ") + String.format(Locale.GERMANY, "%,d", e.dry) + " " + e.mobName), cx, top + 162, INK);
            if (g.count > 1) ink(ctx, Component.literal("dropped " + g.count + " times"), cx, top + 174, INK_SOFT);
            if (g.shares > 0) ink(ctx, Component.literal(g.shares == g.count ? "(loot share)" : "(" + g.shares + "x loot share)"), cx, top + 186, INK_SOFT);
            ctx.fill(x0 + 40, top + PH - 32, x0 + PW - 40, top + PH - 31, 0x406A4E30);
        }
    }

    /** Centered text WITHOUT the drop shadow (dark ink on paper reads badly with a shadow - books have none). */
    private void ink(GuiGraphicsExtractor ctx, Component text, int cx, int y, int color) {
        ctx.text(this.font, text, cx - this.font.width(text) / 2, y, color, false);
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
        Component c = Component.literal(text).withStyle(ChatFormatting.BOLD);
        float s = Math.min(2f, (PW - 28f) / Math.max(1, this.font.width(c)));
        ps.scale(s, s);
        ink(ctx, c, 0, -4, color);
        ps.popMatrix();
    }

    /** Small text (3/4 size), no shadow. */
    private void ps2(GuiGraphicsExtractor ctx, String text, int x, int y, int color) {
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(x, y);
        ps.scale(0.75f, 0.75f);
        ctx.text(this.font, Component.literal(text), 0, 0, color, false);
        ps.popMatrix();
    }

    private void stat(GuiGraphicsExtractor ctx, int x0, int y, String label, String value, int color) {
        ctx.text(this.font, Component.literal(label), x0 + 16, y, INK_SOFT, false);
        String v = value;
        while (v.length() > 3 && this.font.width(v) > PW - 34) v = v.substring(0, v.length() - 1);
        ctx.text(this.font, Component.literal(v).withStyle(ChatFormatting.BOLD), x0 + 16, y + 12, color, false);
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
