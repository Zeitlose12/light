package com.skymaxx.tiki.light.dye;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.FishingRodItem;
import org.joml.Matrix3x2fStack;

/** Light: the small pink glass dye panel. Shows only while you fish (by default). */
public final class DyeHud {
    public static final int WIDTH = 150;
    private static final int PINK = 0xFFFF8CC8;
    private static final int TEXT = 0xFFF2EEF3;
    private static final int DIM = 0xFFA79AAE;
    private static final int GLASS = 0xD01A0F1C;
    private static final int ROW = 27;

    private DyeHud() {
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        TikiConfig cfg = TikiGambleClient.config;
        Minecraft mc = Minecraft.getInstance();
        if (cfg == null || !cfg.dyeHudEnabled || mc.player == null) {
            return;
        }
        if (mc.gui.screen() instanceof DyeHudEditScreen) {
            return;
        }
        if (TikiGambleClient.animationOwnsScreen()) {
            return;
        }
        if (cfg.dyeHudOnlyFishing && !DyeHud.isFishing(mc.player)) {
            return;
        }
        int sw = ctx.guiWidth();
        int sh = ctx.guiHeight();
        DyeHud.draw(ctx, mc.font, Math.round(cfg.dyeHudX * sw), Math.round(cfg.dyeHudY * sh), cfg.dyeHudScale, cfg);
    }

    public static boolean isFishing(LocalPlayer player) {
        return player.fishing != null
            || player.getMainHandItem().getItem() instanceof FishingRodItem
            || player.getOffhandItem().getItem() instanceof FishingRodItem;
    }

    public static List<Dye> shown(TikiConfig cfg) {
        ArrayList<Dye> out = new ArrayList<Dye>();
        if (cfg.dyeShowAquamarine) out.add(Dye.AQUAMARINE);
        if (cfg.dyeShowIceberg) out.add(Dye.ICEBERG);
        if (cfg.dyeShowCarmine) out.add(Dye.CARMINE);
        if (cfg.dyeShowMidnight) out.add(Dye.MIDNIGHT);
        return out;
    }

    public static int height(TikiConfig cfg) {
        int rows = Math.max(1, DyeHud.shown(cfg).size());
        return 18 + rows * ROW;
    }

    public static void draw(GuiGraphicsExtractor ctx, Font font, int x, int y, float scale, TikiConfig cfg) {
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate((float)x, (float)y);
        pose.scale(scale, scale);
        int h = DyeHud.height(cfg);
        ctx.fill(0, 0, WIDTH, h, GLASS);
        ctx.outline(0, 0, WIDTH, h, 0x99FF8CC8);
        ctx.text(font, (Component)Component.literal("§lLight §r· Dyes"), 6, 5, PINK);
        int ry = 18;
        List<Dye> dyes = DyeHud.shown(cfg);
        if (!DyeTracker.hasData()) {
            ctx.text(font, (Component)Component.literal("Open /pv once"), 6, ry + 2, DIM);
            ctx.text(font, (Component)Component.literal("to load your kills"), 6, ry + 12, DIM);
            pose.popMatrix();
            return;
        }
        for (Dye dye : dyes) {
            DyeTracker.Progress p = DyeTracker.progress(dye);
            double pc = p.chance() * 100.0;
            String pct = pc < 10.0 ? String.format("%.1f%%", pc) : Math.round(pc) + "%";
            ctx.text(font, (Component)Component.literal(dye.shortName), 6, ry, dye.color);
            ctx.text(font, (Component)Component.literal(pct), WIDTH - 6 - font.width(pct), ry, TEXT);
            int barW = WIDTH - 12;
            ctx.fill(6, ry + 10, 6 + barW, ry + 13, 0x30FFFFFF);
            int filled = (int)Math.round(barW * Math.max(0.0, Math.min(1.0, p.chance())));
            if (filled > 0) {
                ctx.fill(6, ry + 10, 6 + filled, ry + 13, dye.color);
            }
            String sub = String.format("%,d kills · %d found", p.kills(), p.found());
            ctx.text(font, (Component)Component.literal(sub), 6, ry + 15, DIM);
            ry += ROW;
        }
        pose.popMatrix();
    }
}
