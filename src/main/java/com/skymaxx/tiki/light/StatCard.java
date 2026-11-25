package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.config.TikiStats;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;

/** Light: draws your Wiki Tiki / Titanoboa stats as a PNG you can share. */
public final class StatCard {
    private static final int W = 1200;
    private static final int H = 675;
    private static final Color PINK = new Color(0xFF8CC8);
    private static final Color TEXT = new Color(0xF2EEF3);
    private static final Color DIM = new Color(0xA79AAE);

    private StatCard() {
    }

    public static File save() throws Exception {
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
        Minecraft mc = Minecraft.getInstance();
        String player = mc.getUser() != null ? mc.getUser().getName() : "Player";
        BufferedImage img = StatCard.render(TikiGambleClient.stats, TikiGambleClient.config, player);
        File dir = new File(mc.gameDirectory, "screenshots");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("cannot create " + dir);
        }
        File out = new File(dir, "light-statcard-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".png");
        ImageIO.write(img, "png", out);
        return out;
    }

    public static BufferedImage render(TikiStats s, TikiConfig cfg, String player) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(0x2A1428), W, H, new Color(0x120A14)));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 140, 200, 40));
        g.fillOval(W - 420, -220, 640, 640);
        g.setColor(PINK);
        g.setStroke(new BasicStroke(3f));
        g.draw(new RoundRectangle2D.Float(14, 14, W - 28, H - 28, 36, 36));

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
        g.setColor(PINK);
        g.drawString("Light", 60, 112);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 28));
        g.setColor(DIM);
        g.drawString("Fishing stats of " + player, 250, 108);

        double luck = Luck.overall(s);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        g.setColor(TEXT);
        String luckText = "Luck " + Luck.plain(luck);
        g.drawString(luckText, W - 60 - g.getFontMetrics().stringWidth(luckText), 108);

        StatCard.column(g, 60, 180, "Wiki Tiki", s.kills, Luck.tiki(s),
            "Reached mask 4: " + s.reachedFinalReel, "Worst dry streak: " + s.worstDryStreak);
        StatCard.column(g, W / 2 + 20, 180, "Titanoboa", s.boaKills, Luck.boa(s),
            "Reached the head: " + (s.segmentsDown.length > 3 ? s.segmentsDown[3] : 0), "Worst dry streak: " + s.boaWorstDryStreak);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        g.setColor(DIM);
        g.drawString("Achievements " + Achievements.count(cfg) + " / " + Achievements.values().length
            + "   ·   " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), 60, H - 50);
        String foot = "made with Light · based on SkyMaxx by Rachie";
        g.drawString(foot, W - 60 - g.getFontMetrics().stringWidth(foot), H - 50);
        g.dispose();
        return img;
    }

    private static void column(Graphics2D g, int x, int y, String title, int kills, List<Luck.Line> lines, String extra1, String extra2) {
        int w = W / 2 - 80;
        g.setColor(new Color(255, 255, 255, 18));
        g.fill(new RoundRectangle2D.Float(x - 20, y - 50, w, 410, 24, 24));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        g.setColor(PINK);
        g.drawString(title, x, y);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        g.setColor(TEXT);
        String k = kills + " kills";
        g.drawString(k, x + w - 40 - g.getFontMetrics().stringWidth(k), y);
        int row = y + 50;
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        for (Luck.Line line : lines) {
            g.setColor(DIM);
            g.drawString(line.name(), x, row);
            String v = line.got() + "   " + Luck.plain(line.percent());
            g.setColor(TEXT);
            g.drawString(v, x + w - 40 - g.getFontMetrics().stringWidth(v), row);
            row += 40;
        }
        row += 10;
        g.setColor(DIM);
        g.drawString(extra1, x, row);
        g.drawString(extra2, x, row + 36);
    }
}
