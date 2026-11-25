package com.skymaxx.tiki.render.boa;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public final class BoaArt {
    public static final int INK = -16315383;
    private static final Color INK_C = new Color(-16315383, true);
    public static final float CHAMBER_RING = 0.28515625f;
    public static final float CHAMBER_SIZE = 0.25f;
    public static final int TRI = 256;

    private BoaArt() {
    }

    private static BufferedImage image(int w, int h) {
        return new BufferedImage(w, h, 2);
    }

    private static Graphics2D g(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    private static Color c(int argb) {
        return new Color(argb, true);
    }

    private static Ellipse2D circle(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx - r, cy - r, r * 2.0, r * 2.0);
    }

    private static Area crescent(Shape shape, double dx, double dy) {
        Area a = new Area(shape);
        a.subtract(new Area(AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(shape)));
        return a;
    }

    private static void cel(Graphics2D g, Shape shape, int light, int mid, int dark, double lit, double ink) {
        g.setColor(BoaArt.c(mid));
        g.fill(shape);
        g.setColor(BoaArt.c(light));
        g.fill(BoaArt.crescent(shape, lit, lit));
        g.setColor(BoaArt.c(dark));
        g.fill(BoaArt.crescent(shape, -lit * 0.85, -lit * 0.85));
        if (ink > 0.0) {
            g.setColor(INK_C);
            g.setStroke(new BasicStroke((float)ink, 1, 1));
            g.draw(shape);
        }
    }

    public static BufferedImage cylinder() {
        int s = 256;
        double c = (double)s / 2.0;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        Ellipse2D outer = BoaArt.circle(c, c, 118.0);
        g.setColor(BoaArt.c(-9471097));
        g.fill(outer);
        g.setColor(BoaArt.c(1429881419));
        g.setStroke(new BasicStroke(3.0f));
        g.draw(BoaArt.circle(c, c, 100.0));
        g.setColor(BoaArt.c(-11642780));
        g.fill(BoaArt.circle(c, c, 23.0));
        g.setColor(INK_C);
        g.setStroke(new BasicStroke(3.0f));
        g.draw(BoaArt.circle(c, c, 23.0));
        Path2D.Double star = new Path2D.Double();
        for (int k = 0; k < 16; ++k) {
            double r;
            double a = Math.toRadians((double)k * 22.5);
            double d = r = k % 2 == 0 ? 19.0 : 12.0;
            if (k == 0) {
                ((Path2D)star).moveTo(c + Math.cos(a) * r, c + Math.sin(a) * r);
                continue;
            }
            ((Path2D)star).lineTo(c + Math.cos(a) * r, c + Math.sin(a) * r);
        }
        star.closePath();
        g.setColor(BoaArt.c(-9471097));
        g.fill(star);
        g.setColor(INK_C);
        g.setStroke(new BasicStroke(2.0f));
        g.draw(star);
        g.setColor(BoaArt.c(-5786948));
        g.fill(BoaArt.circle(c, c, 6.0));
        g.setColor(INK_C);
        g.draw(BoaArt.circle(c, c, 6.0));
        g.setStroke(new BasicStroke(7.0f));
        g.draw(outer);
        g.dispose();
        return img;
    }

    public static BufferedImage cylinderShade() {
        int s = 256;
        double c = (double)s / 2.0;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        Ellipse2D outer = BoaArt.circle(c, c, 118.0);
        g.setColor(BoaArt.c(0x70FFFFFF));
        g.fill(BoaArt.crescent(outer, 15.0, 15.0));
        g.setColor(BoaArt.c(Integer.MIN_VALUE));
        g.fill(BoaArt.crescent(outer, -12.75, -12.75));
        Ellipse2D hub = BoaArt.circle(c, c, 21.5);
        g.setColor(BoaArt.c(0x60FFFFFF));
        g.fill(BoaArt.crescent(hub, 4.0, 4.0));
        g.setColor(BoaArt.c(0x70000000));
        g.fill(BoaArt.crescent(hub, -3.4, -3.4));
        g.setComposite(AlphaComposite.Clear);
        g.setStroke(new BasicStroke(6.0f));
        g.draw(BoaArt.circle(c, c, 100.0));
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(INK_C);
        g.setStroke(new BasicStroke(7.0f));
        g.draw(outer);
        g.dispose();
        return img;
    }

    public static BufferedImage cylinderFlutes() {
        int s = 256;
        double c = (double)s / 2.0;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        Ellipse2D outer = BoaArt.circle(c, c, 118.0);
        Graphics2D fl = (Graphics2D)g.create();
        fl.clip(BoaArt.circle(c, c, 121.5));
        fl.setColor(BoaArt.c(-14275533));
        for (int k = 0; k < 8; ++k) {
            double a = Math.toRadians(22.5 + 45.0 * (double)k);
            Ellipse2D.Double flute = new Ellipse2D.Double(-15.0, -8.0, 30.0, 16.0);
            AffineTransform at = new AffineTransform();
            at.translate(c + Math.cos(a) * 113.0, c + Math.sin(a) * 113.0);
            at.rotate(a + 1.5707963267948966);
            fl.fill(at.createTransformedShape(flute));
        }
        fl.dispose();
        g.setColor(INK_C);
        g.setStroke(new BasicStroke(7.0f));
        g.draw(outer);
        g.dispose();
        return img;
    }

    public static BufferedImage chamber() {
        int s = 128;
        double c = (double)s / 2.0;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        BoaArt.cel(g, BoaArt.circle(c, c, 60.0), -3682088, -7892064, -11642780, 8.0, 0.0);
        Ellipse2D hole = BoaArt.circle(c, c, 50.0);
        g.setColor(BoaArt.c(-15131100));
        g.fill(hole);
        g.setColor(INK_C);
        g.setStroke(new BasicStroke(6.0f));
        g.draw(hole);
        g.dispose();
        return img;
    }

    public static BufferedImage cartridgeBase(boolean gold) {
        int s = 64;
        double c = (double)s / 2.0;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        int light = gold ? -2104 : -664948;
        int mid = gold ? -12475 : -3104186;
        int dark = gold ? -4619248 : -7707100;
        BoaArt.cel(g, BoaArt.circle(c, c, 28.0), light, mid, dark, 5.0, 3.5);
        g.setColor(BoaArt.c(dark));
        g.setStroke(new BasicStroke(2.0f));
        g.draw(BoaArt.circle(c, c, 21.0));
        BoaArt.cel(g, BoaArt.circle(c, c, 10.0), -986122, -4866618, -9735299, 2.5, 2.5);
        if (gold) {
            g.setColor(Color.WHITE);
            Path2D.Double glint = new Path2D.Double();
            double gx = c - 12.0;
            double gy = c - 12.0;
            ((Path2D)glint).moveTo(gx, gy - 9.0);
            ((Path2D)glint).lineTo(gx + 2.5, gy - 2.5);
            ((Path2D)glint).lineTo(gx + 9.0, gy);
            ((Path2D)glint).lineTo(gx + 2.5, gy + 2.5);
            ((Path2D)glint).lineTo(gx, gy + 9.0);
            ((Path2D)glint).lineTo(gx - 2.5, gy + 2.5);
            ((Path2D)glint).lineTo(gx - 9.0, gy);
            ((Path2D)glint).lineTo(gx - 2.5, gy - 2.5);
            glint.closePath();
            g.fill(glint);
        }
        g.dispose();
        return img;
    }

    public static BufferedImage bullet(boolean gold) {
        int w = 48;
        int h = 128;
        BufferedImage img = BoaArt.image(w, h);
        Graphics2D g = BoaArt.g(img);
        int caseLight = gold ? -2882 : -730998;
        int caseMid = gold ? -13508 : -3170493;
        int caseDark = gold ? -4817138 : -7707100;
        int tipLight = gold ? -1 : -741254;
        int tipMid = gold ? -6528 : -4167114;
        int tipDark = gold ? -2844132 : -8766181;
        Path2D.Double tip = new Path2D.Double();
        ((Path2D)tip).moveTo(24.0, 5.0);
        ((Path2D)tip).curveTo(33.0, 12.0, 35.0, 26.0, 35.0, 40.0);
        ((Path2D)tip).lineTo(13.0, 40.0);
        ((Path2D)tip).curveTo(13.0, 26.0, 15.0, 12.0, 24.0, 5.0);
        tip.closePath();
        BoaArt.banded(g, tip, 13.0, 35.0, tipLight, tipMid, tipDark);
        RoundRectangle2D.Double body = new RoundRectangle2D.Double(11.0, 38.0, 26.0, 78.0, 5.0, 5.0);
        BoaArt.banded(g, body, 11.0, 37.0, caseLight, caseMid, caseDark);
        RoundRectangle2D.Double rim = new RoundRectangle2D.Double(8.0, 112.0, 32.0, 12.0, 4.0, 4.0);
        BoaArt.banded(g, rim, 8.0, 40.0, caseLight, caseMid, caseDark);
        g.setColor(INK_C);
        g.setStroke(new BasicStroke(3.2f, 1, 1));
        g.draw(tip);
        g.draw(body);
        g.draw(rim);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Rectangle2D.Double(11.0, 44.0, 26.0, 0.1));
        g.dispose();
        return img;
    }

    public static BufferedImage bulletBack() {
        int s = 96;
        double c = (double)s / 2.0;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        g.setColor(BoaArt.c(587199442));
        g.fill(BoaArt.circle(c, c, 46.0));
        g.setColor(BoaArt.c(872412114));
        g.fill(BoaArt.circle(c, c, 38.0));
        BoaArt.cel(g, BoaArt.circle(c, c, 31.0), -734838, -3569074, -8499678, 6.0, 3.4);
        BoaArt.cel(g, BoaArt.circle(c, c, 20.0), -2051725, -5212609, -9748709, 4.0, 2.6);
        g.setColor(BoaArt.c(-11916782));
        g.fill(BoaArt.circle(c, c, 8.5));
        g.setColor(INK_C);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(BoaArt.circle(c, c, 8.5));
        g.setColor(BoaArt.c(-855640356));
        g.fill(new Ellipse2D.Double(c - 24.0, c - 24.0, 15.0, 9.0));
        g.dispose();
        return img;
    }

    private static void banded(Graphics2D g, Shape shape, double x0, double x1, int light, int mid, int dark) {
        g.setColor(BoaArt.c(mid));
        g.fill(shape);
        Graphics2D clip = (Graphics2D)g.create();
        clip.clip(shape);
        double w = x1 - x0;
        clip.setColor(BoaArt.c(light));
        clip.fill(new Rectangle2D.Double(x0 + w * 0.14, 0.0, w * 0.2, 200.0));
        clip.setColor(BoaArt.c(dark));
        clip.fill(new Rectangle2D.Double(x0 + w * 0.72, 0.0, w * 0.3, 200.0));
        clip.dispose();
    }

    public static BufferedImage triangle() {
        int s = 256;
        BufferedImage img = BoaArt.image(s, s);
        for (int y = 0; y < s; ++y) {
            for (int x = 0; x < s; ++x) {
                if (x + y > s + 1) continue;
                img.setRGB(x, y, -1);
            }
        }
        return img;
    }

    public static BufferedImage disc() {
        int s = 64;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        g.setColor(Color.WHITE);
        g.fill(BoaArt.circle((double)s / 2.0, (double)s / 2.0, (double)s / 2.0 - 1.0));
        g.dispose();
        return img;
    }

    public static BufferedImage scopeMask() {
        int s = 512;
        double c = (double)s / 2.0;
        BufferedImage img = BoaArt.image(s, s);
        for (int y = 0; y < s; ++y) {
            for (int x = 0; x < s; ++x) {
                double dx = ((double)x + 0.5 - c) / c;
                double dy = ((double)y + 0.5 - c) / c;
                double r = Math.sqrt(dx * dx + dy * dy);
                int a = r >= 1.0 ? 255 : (r >= 0.965 ? 235 : (r >= 0.9 ? 120 : (r >= 0.82 ? 46 : 0)));
                img.setRGB(x, y, a << 24 | 0x20403);
            }
        }
        Graphics2D g = BoaArt.g(img);
        g.setColor(BoaArt.c(1435878351));
        g.setStroke(new BasicStroke(3.0f));
        g.draw(BoaArt.circle(c, c, c * 0.965));
        g.dispose();
        return img;
    }

    public static BufferedImage reticle() {
        int s = 512;
        double c = (double)s / 2.0;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        g.setColor(Color.WHITE);
        double post = (double)s * 0.5;
        double fineStart = (double)s * 0.2;
        g.fill(new Rectangle2D.Double(c - 4.0, 0.0, 8.0, post - fineStart));
        g.fill(new Rectangle2D.Double(c - 4.0, c + fineStart, 8.0, post - fineStart));
        g.fill(new Rectangle2D.Double(0.0, c - 4.0, post - fineStart, 8.0));
        g.fill(new Rectangle2D.Double(c + fineStart, c - 4.0, post - fineStart, 8.0));
        double gap = 7.0;
        g.fill(new Rectangle2D.Double(c - 1.0, c - fineStart, 2.0, fineStart - gap));
        g.fill(new Rectangle2D.Double(c - 1.0, c + gap, 2.0, fineStart - gap));
        g.fill(new Rectangle2D.Double(c - fineStart, c - 1.0, fineStart - gap, 2.0));
        g.fill(new Rectangle2D.Double(c + gap, c - 1.0, fineStart - gap, 2.0));
        for (int i = 1; i <= 4; ++i) {
            double d = (double)i * fineStart / 5.0 + gap * 0.5;
            g.fill(BoaArt.circle(c + d, c, 2.6));
            g.fill(BoaArt.circle(c - d, c, 2.6));
            g.fill(BoaArt.circle(c, c + d, 2.6));
            g.fill(BoaArt.circle(c, c - d, 2.6));
        }
        g.fill(BoaArt.circle(c, c, 2.2));
        g.dispose();
        return img;
    }

    public static BufferedImage skyDusk() {
        int w = 1024;
        int h = 400;
        BufferedImage img = BoaArt.image(w, h);
        Graphics2D g = BoaArt.g(img);
        Random rng = new Random(2826L);
        BoaArt.bands(g, w, h, new int[]{-14935494, -13621939, -11260843, -7453620, -3378106, -1466278}, new double[]{0.0, 0.16, 0.3, 0.42, 0.53, 0.63, 1.0});
        BoaArt.sun(g, (double)w * 0.64, (double)h * 0.64, 46.0, new int[]{419405390, 721399900, 1157612660}, new int[]{-26052, -14754, -5720, -1828}, -1466278, -3378106);
        for (int i = 0; i < 6; ++i) {
            double cw = 150 + rng.nextInt(190);
            double ch = 9 + rng.nextInt(9);
            double cx = rng.nextInt(w);
            double cy = (double)h * (0.18 + 0.42 * rng.nextDouble());
            g.setColor(BoaArt.c(-10603176));
            g.fill(new Ellipse2D.Double(cx - cw / 2.0, cy - ch / 2.0, cw, ch));
            g.setColor(BoaArt.c(-3178910));
            g.fill(new Ellipse2D.Double(cx - cw * 0.4, cy + ch * 0.1, cw * 0.8, ch * 0.4));
        }
        BoaArt.treeline(g, w, h, (double)h * 0.9, 30.0, -14009531, rng, false);
        BoaArt.treeline(g, w, h, (double)h * 0.95, 42.0, -15721956, rng, true);
        g.dispose();
        return img;
    }

    public static BufferedImage skyNight() {
        double y;
        double x;
        int i;
        int w = 1024;
        int h = 400;
        BufferedImage img = BoaArt.image(w, h);
        Graphics2D g = BoaArt.g(img);
        Random rng = new Random(2846L);
        BoaArt.bands(g, w, h, new int[]{-16381412, -16117464, -15787464, -15391674, -14929838, -14336418}, new double[]{0.0, 0.18, 0.34, 0.48, 0.6, 0.7, 1.0});
        for (i = 0; i < 170; ++i) {
            x = rng.nextDouble() * (double)w;
            y = (double)h * Math.pow(rng.nextDouble(), 1.6) * 0.72;
            int a = 120 + rng.nextInt(135);
            g.setColor(BoaArt.c(a << 24 | 0xE8EEFF));
            double r = rng.nextDouble() < 0.12 ? 1.6 : 0.9;
            g.fill(BoaArt.circle(x, y, r));
        }
        g.setColor(BoaArt.c(-722945));
        for (i = 0; i < 7; ++i) {
            x = rng.nextDouble() * (double)w;
            y = (double)h * rng.nextDouble() * 0.5;
            double r = 5 + rng.nextInt(4);
            Path2D.Double p = new Path2D.Double();
            ((Path2D)p).moveTo(x, y - r);
            ((Path2D)p).lineTo(x + r * 0.22, y - r * 0.22);
            ((Path2D)p).lineTo(x + r, y);
            ((Path2D)p).lineTo(x + r * 0.22, y + r * 0.22);
            ((Path2D)p).lineTo(x, y + r);
            ((Path2D)p).lineTo(x - r * 0.22, y + r * 0.22);
            ((Path2D)p).lineTo(x - r, y);
            ((Path2D)p).lineTo(x - r * 0.22, y - r * 0.22);
            p.closePath();
            g.fill(p);
        }
        double mx = (double)w * 0.3;
        double my = (double)h * 0.4;
        g.setColor(BoaArt.c(583588095));
        g.fill(BoaArt.circle(mx, my, 96.0));
        g.setColor(BoaArt.c(868800767));
        g.fill(BoaArt.circle(mx, my, 66.0));
        Ellipse2D moon = BoaArt.circle(mx, my, 44.0);
        g.setColor(BoaArt.c(-1446154));
        g.fill(moon);
        g.setColor(BoaArt.c(-4208168));
        g.fill(BoaArt.crescent(moon, 11.0, -4.0));
        g.setColor(BoaArt.c(-3287326));
        for (double[] sea : new double[][]{{-12.0, -10.0, 11.0}, {10.0, 6.0, 8.0}, {-4.0, 16.0, 7.0}, {16.0, -14.0, 5.0}}) {
            g.fill(BoaArt.circle(mx + sea[0], my + sea[1], sea[2]));
        }
        for (int i2 = 0; i2 < 5; ++i2) {
            double cw = 180 + rng.nextInt(200);
            double ch = 7 + rng.nextInt(7);
            double cx = rng.nextInt(w);
            double cy = (double)h * (0.22 + 0.4 * rng.nextDouble());
            g.setColor(BoaArt.c(-15062456));
            g.fill(new Ellipse2D.Double(cx - cw / 2.0, cy - ch / 2.0, cw, ch));
            g.setColor(BoaArt.c(-10652778));
            g.fill(new Ellipse2D.Double(cx - cw * 0.38, cy + ch * 0.15, cw * 0.76, ch * 0.35));
        }
        BoaArt.treeline(g, w, h, (double)h * 0.9, 30.0, -15457740, rng, false);
        BoaArt.treeline(g, w, h, (double)h * 0.95, 44.0, -16578805, rng, true);
        g.dispose();
        return img;
    }

    public static BufferedImage skyMist() {
        int w = 1024;
        int h = 400;
        BufferedImage img = BoaArt.image(w, h);
        Graphics2D g = BoaArt.g(img);
        Random rng = new Random(12631L);
        BoaArt.bands(g, w, h, new int[]{-8415334, -7362906, -6244943, -5127237, -4009788, -2958131}, new double[]{0.0, 0.2, 0.36, 0.5, 0.62, 0.72, 1.0});
        double sx = (double)w * 0.62;
        double sy = (double)h * 0.52;
        g.setColor(BoaArt.c(0x44FFFFF2));
        g.fill(BoaArt.circle(sx, sy, 58.0));
        g.setColor(BoaArt.c(-461078));
        g.fill(BoaArt.circle(sx, sy, 30.0));
        BoaArt.treeline(g, w, h, (double)h * 0.84, 26.0, -6442840, rng, false);
        BoaArt.fogBand(g, w, (double)h * 0.78, 18.0, -1999841074);
        BoaArt.treeline(g, w, h, (double)h * 0.9, 34.0, -8679030, rng, false);
        BoaArt.fogBand(g, w, (double)h * 0.86, 14.0, 2009387720);
        BoaArt.treeline(g, w, h, (double)h * 0.96, 40.0, -11640229, rng, true);
        BoaArt.deadTrees(g, w, h, rng, -12956859);
        BoaArt.fogBand(g, w, (double)h * 0.95, 10.0, 1723648961);
        g.dispose();
        return img;
    }

    private static void sun(Graphics2D g, double sx, double sy, double r, int[] halo, int[] disc, int nearBand, int farBand) {
        g.setColor(BoaArt.c(halo[0]));
        g.fill(new Ellipse2D.Double(sx - r * 6.0, sy - r * 0.62, r * 12.0, r * 1.24));
        for (int i = 0; i < halo.length; ++i) {
            g.setColor(BoaArt.c(halo[i]));
            g.fill(BoaArt.circle(sx, sy, r * (2.25 - 0.45 * (double)i)));
        }
        Ellipse2D body = BoaArt.circle(sx, sy, r);
        for (int i = 0; i < disc.length; ++i) {
            g.setColor(BoaArt.c(disc[i]));
            g.fill(BoaArt.circle(sx, sy - r * 0.05 * (double)i, r * (1.0 - 0.19 * (double)i)));
        }
        Graphics2D clip = (Graphics2D)g.create();
        clip.clip(body);
        for (int i = 0; i < 4; ++i) {
            clip.setColor(BoaArt.c(i % 2 == 0 ? nearBand : farBand));
            clip.fill(new Rectangle2D.Double(sx - r * 1.2, sy + r * (0.1 + 0.26 * (double)i), r * 2.4, r * (0.07 + 0.028 * (double)i)));
        }
        clip.dispose();
    }

    private static void rays(Graphics2D g, double sx, double sy, double length, int argb, int count, double spread) {
        g.setColor(BoaArt.c(argb));
        for (int i = 0; i < count; ++i) {
            double a = -Math.PI + (double)i * (Math.PI * 2 / (double)count) + 0.3;
            double half = spread * (0.4 + 0.6 * (double)(i * 7 % 5) / 5.0);
            Path2D.Double wedge = new Path2D.Double();
            ((Path2D)wedge).moveTo(sx, sy);
            ((Path2D)wedge).lineTo(sx + Math.cos(a - half) * length, sy + Math.sin(a - half) * length);
            ((Path2D)wedge).lineTo(sx + Math.cos(a + half) * length, sy + Math.sin(a + half) * length);
            wedge.closePath();
            g.fill(wedge);
        }
    }

    public static BufferedImage skyGolden() {
        int w = 1024;
        int h = 400;
        BufferedImage img = BoaArt.image(w, h);
        Graphics2D g = BoaArt.g(img);
        Random rng = new Random(1549L);
        BoaArt.bands(g, w, h, new int[]{-13669220, -9532258, -4617638, -2058449, -673741, -11686, -5724}, new double[]{0.0, 0.09, 0.2, 0.32, 0.45, 0.58, 0.72, 1.0});
        double sx = (double)w * 0.57;
        double sy = (double)h * 0.55;
        BoaArt.rays(g, sx, sy, (double)h * 1.8, 486535364, 13, 0.045);
        BoaArt.sun(g, sx, sy, 58.0, new int[]{822067790, 1291834480, 1862263964}, new int[]{-19158, -11165, -4178, -528}, -11686, -673741);
        for (int i = 0; i < 8; ++i) {
            double cw = 170 + rng.nextInt(240);
            double ch = 9 + rng.nextInt(11);
            double cx = rng.nextInt(w);
            double cy = (double)h * (0.1 + 0.34 * rng.nextDouble());
            g.setColor(BoaArt.c(-6526406));
            g.fill(new Ellipse2D.Double(cx - cw / 2.0, cy - ch / 2.0, cw, ch));
            g.setColor(BoaArt.c(-6232));
            g.fill(new Ellipse2D.Double(cx - cw * 0.44, cy + ch * 0.08, cw * 0.88, ch * 0.5));
            g.setColor(BoaArt.c(-2346));
            g.fill(new Ellipse2D.Double(cx - cw * 0.3, cy + ch * 0.22, cw * 0.6, ch * 0.26));
        }
        BoaArt.treeline(g, w, h, (double)h * 0.84, 26.0, -2578338, rng, false);
        BoaArt.treeline(g, w, h, (double)h * 0.9, 32.0, -6523840, rng, false);
        BoaArt.treeline(g, w, h, (double)h * 0.96, 46.0, -12899817, rng, true);
        g.setColor(BoaArt.c(1291838632));
        g.fill(new Rectangle2D.Double(0.0, (double)h * 0.86, w, (double)h * 0.045));
        g.setColor(BoaArt.c(1728047540));
        g.fill(new Rectangle2D.Double(0.0, (double)h * 0.975, w, (double)h * 0.025));
        g.dispose();
        return img;
    }

    private static void bands(Graphics2D g, int w, int h, int[] bands, double[] edges) {
        for (int i = 0; i < bands.length; ++i) {
            g.setColor(BoaArt.c(bands[i]));
            Path2D.Double band = new Path2D.Double();
            double top = edges[i] * (double)h;
            ((Path2D)band).moveTo(0.0, h);
            ((Path2D)band).lineTo(0.0, top);
            for (int x = 0; x <= w; x += 32) {
                ((Path2D)band).lineTo(x, top + Math.sin((double)x * 0.011 + (double)i * 1.7) * 5.0);
            }
            ((Path2D)band).lineTo(w, h);
            band.closePath();
            g.fill(band);
        }
    }

    private static void fogBand(Graphics2D g, int w, double y, double thick, int argb) {
        int x;
        g.setColor(BoaArt.c(argb));
        Path2D.Double band = new Path2D.Double();
        ((Path2D)band).moveTo(0.0, y + thick);
        for (x = 0; x <= w; x += 16) {
            ((Path2D)band).lineTo(x, y - Math.abs(Math.sin((double)x * 0.009)) * thick * 0.8);
        }
        for (x = w; x >= 0; x -= 16) {
            ((Path2D)band).lineTo(x, y + thick + Math.sin((double)x * 0.013 + 1.1) * thick * 0.3);
        }
        band.closePath();
        g.fill(band);
    }

    private static void deadTrees(Graphics2D g, int w, int h, Random rng, int colour) {
        g.setColor(BoaArt.c(colour));
        for (int i = 0; i < 6; ++i) {
            double x = (double)w * (0.08 + 0.16 * (double)i) + (double)rng.nextInt(40) - 20.0;
            double top = (double)h * (0.52 + rng.nextDouble() * 0.16);
            double trunk = 4 + rng.nextInt(3);
            g.setStroke(new BasicStroke((float)trunk, 1, 1));
            Path2D.Double t = new Path2D.Double();
            ((Path2D)t).moveTo(x, h);
            ((Path2D)t).lineTo(x + (double)rng.nextInt(9) - 4.0, top);
            g.draw(t);
            g.setStroke(new BasicStroke(2.2f, 1, 1));
            for (int b = 0; b < 4; ++b) {
                double by = top + ((double)h - top) * (0.05 + 0.25 * rng.nextDouble());
                double dir = rng.nextBoolean() ? 1.0 : -1.0;
                double len = 14 + rng.nextInt(18);
                Path2D.Double br = new Path2D.Double();
                ((Path2D)br).moveTo(x, by);
                ((Path2D)br).lineTo(x + dir * len * 0.6, by - len * 0.5);
                ((Path2D)br).lineTo(x + dir * len, by - len * 0.45 - (double)rng.nextInt(8));
                g.draw(br);
            }
        }
    }

    private static void treeline(Graphics2D g, int w, int h, double base, double tall, int colour, Random rng, boolean cypress) {
        g.setColor(BoaArt.c(colour));
        Path2D.Double line = new Path2D.Double();
        ((Path2D)line).moveTo(0.0, h);
        for (int x = 0; x <= w; x += 8) {
            double y = base - tall * 0.35 - Math.abs(Math.sin((double)x * 0.02 + rng.nextDouble() * 0.2)) * tall * 0.35 - rng.nextDouble() * 6.0;
            ((Path2D)line).lineTo(x, y);
        }
        ((Path2D)line).lineTo(w, h);
        line.closePath();
        g.fill(line);
        if (!cypress) {
            return;
        }
        for (int i = 0; i < 11; ++i) {
            double x = (double)i * ((double)w / 10.0) + (double)rng.nextInt(50) - 25.0;
            double top = base - tall * (1.3 + rng.nextDouble() * 0.9);
            double half = 16 + rng.nextInt(12);
            Path2D.Double tree = new Path2D.Double();
            ((Path2D)tree).moveTo(x - half * 1.5, h);
            ((Path2D)tree).lineTo(x - half * 0.5, base);
            ((Path2D)tree).lineTo(x - half * 1.4, base - tall * 0.25);
            ((Path2D)tree).lineTo(x - half * 0.6, top + (base - top) * 0.45);
            ((Path2D)tree).lineTo(x - half * 1.1, top + (base - top) * 0.3);
            ((Path2D)tree).lineTo(x, top);
            ((Path2D)tree).lineTo(x + half * 1.1, top + (base - top) * 0.3);
            ((Path2D)tree).lineTo(x + half * 0.6, top + (base - top) * 0.45);
            ((Path2D)tree).lineTo(x + half * 1.4, base - tall * 0.25);
            ((Path2D)tree).lineTo(x + half * 0.5, base);
            ((Path2D)tree).lineTo(x + half * 1.5, h);
            tree.closePath();
            g.fill(tree);
            g.setStroke(new BasicStroke(2.2f));
            for (int m = 0; m < 5; ++m) {
                double mx = x - half + rng.nextDouble() * half * 2.0;
                double my = top + (base - top) * (0.3 + rng.nextDouble() * 0.3);
                g.draw(new Rectangle2D.Double(mx, my, 0.1, 10 + rng.nextInt(18)));
            }
        }
    }

    public static BufferedImage reeds() {
        int w = 512;
        int h = 256;
        BufferedImage img = BoaArt.image(w, h);
        Graphics2D g = BoaArt.g(img);
        Random rng = new Random(12013L);
        g.setColor(Color.WHITE);
        for (int i = 0; i < 46; ++i) {
            double x = rng.nextDouble() * (double)w;
            double top = (double)h * (0.15 + rng.nextDouble() * 0.6);
            double lean = (rng.nextDouble() - 0.5) * 40.0;
            Path2D.Double blade = new Path2D.Double();
            ((Path2D)blade).moveTo(x - 3.0, h);
            ((Path2D)blade).quadTo(x + lean * 0.3, ((double)h + top) / 2.0, x + lean, top);
            ((Path2D)blade).quadTo(x + lean * 0.3 + 3.0, ((double)h + top) / 2.0, x + 4.0, h);
            blade.closePath();
            g.fill(blade);
            if (!(rng.nextDouble() < 0.3)) continue;
            g.fill(new RoundRectangle2D.Double(x + lean - 5.0, top - 4.0, 10.0, 30.0, 8.0, 8.0));
        }
        g.dispose();
        return img;
    }

    public static BufferedImage splash() {
        int s = 128;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        Path2D.Double crown = new Path2D.Double();
        ((Path2D)crown).moveTo(18.0, 118.0);
        ((Path2D)crown).curveTo(22.0, 90.0, 30.0, 72.0, 26.0, 44.0);
        ((Path2D)crown).curveTo(38.0, 66.0, 44.0, 60.0, 50.0, 22.0);
        ((Path2D)crown).curveTo(58.0, 56.0, 62.0, 54.0, 66.0, 12.0);
        ((Path2D)crown).curveTo(72.0, 52.0, 78.0, 58.0, 86.0, 26.0);
        ((Path2D)crown).curveTo(90.0, 62.0, 98.0, 70.0, 104.0, 48.0);
        ((Path2D)crown).curveTo(102.0, 80.0, 104.0, 96.0, 110.0, 118.0);
        crown.closePath();
        BoaArt.cel(g, crown, -1, -3281938, -8407360, 6.0, 3.5);
        for (double[] d : new double[][]{{20.0, 30.0, 6.0}, {104.0, 22.0, 5.0}, {40.0, 8.0, 4.0}, {92.0, 6.0, 4.0}}) {
            BoaArt.cel(g, BoaArt.circle(d[0], d[1], d[2]), -1, -3281938, -8407360, 2.0, 2.5);
        }
        g.dispose();
        return img;
    }

    public static BufferedImage smoke(int variant) {
        int s = 128;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        Random rng = new Random(24076L + (long)variant * 7919L);
        Area cloud = new Area(BoaArt.circle(64.0, 66.0, 30.0));
        int lobes = 4 + variant;
        for (int i = 0; i < lobes; ++i) {
            double a = (double)i * Math.PI * 2.0 / (double)lobes + rng.nextDouble() * 0.6;
            double reach = 18.0 + rng.nextDouble() * 12.0;
            double r = 17.0 + rng.nextDouble() * 11.0;
            cloud.add(new Area(BoaArt.circle(64.0 + Math.cos(a) * reach, 64.0 + Math.sin(a) * reach * 0.85, r)));
        }
        g.setColor(BoaArt.c(-2498848));
        g.fill(cloud);
        g.setColor(BoaArt.c(-1));
        g.fill(BoaArt.crescent(cloud, 9.0, 10.0));
        g.setColor(BoaArt.c(-5327174));
        g.fill(BoaArt.crescent(cloud, -8.0, -9.0));
        g.setColor(BoaArt.c(-7366499));
        g.fill(BoaArt.crescent(cloud, -15.0, -17.0));
        g.setColor(BoaArt.c(1718647424));
        g.setStroke(new BasicStroke(2.5f, 1, 1));
        g.draw(cloud);
        g.dispose();
        return img;
    }

    public static BufferedImage ripple() {
        int s = 128;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        Area ring = new Area(BoaArt.wavyEllipse(64.0, 64.0, 60.0, 60.0, 11, 2.2, 0.0));
        ring.subtract(new Area(BoaArt.wavyEllipse(64.0, 64.0, 49.0, 49.0, 8, 2.0, 1.1)));
        g.setColor(BoaArt.c(-4202274));
        g.fill(ring);
        Graphics2D clip = (Graphics2D)g.create();
        clip.clip(ring);
        clip.setColor(BoaArt.c(-1));
        clip.fill(new Area(BoaArt.wavyEllipse(64.0, 64.0, 53.0, 53.0, 8, 2.0, 1.1)));
        clip.dispose();
        g.dispose();
        return img;
    }

    public static BufferedImage puff() {
        int s = 128;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        Area cloud = new Area(BoaArt.circle(46.0, 72.0, 30.0));
        cloud.add(new Area(BoaArt.circle(78.0, 66.0, 34.0)));
        cloud.add(new Area(BoaArt.circle(62.0, 44.0, 30.0)));
        cloud.add(new Area(BoaArt.circle(64.0, 84.0, 26.0)));
        g.setColor(BoaArt.c(-2630435));
        g.fill(cloud);
        g.setColor(BoaArt.c(-1));
        g.fill(BoaArt.crescent(cloud, 10.0, 10.0));
        g.setColor(BoaArt.c(-5590347));
        g.fill(BoaArt.crescent(cloud, -8.0, -8.0));
        g.dispose();
        return img;
    }

    public static BufferedImage fire() {
        int s = 128;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        Random rng = new Random(61742L);
        Area outer = new Area();
        for (int i = 0; i < 9; ++i) {
            double a = (double)i * Math.PI * 2.0 / 9.0;
            outer.add(new Area(BoaArt.circle(64.0 + Math.cos(a) * 30.0, 64.0 + Math.sin(a) * 30.0, 20 + rng.nextInt(10))));
        }
        outer.add(new Area(BoaArt.circle(64.0, 64.0, 34.0)));
        g.setColor(BoaArt.c(-1551590));
        g.fill(outer);
        g.setColor(BoaArt.c(-26070));
        g.fill(new Area(AffineTransform.getScaleInstance(0.72, 0.72).createTransformedShape(outer)).createTransformedArea(AffineTransform.getTranslateInstance(18.0, 18.0)));
        g.setColor(BoaArt.c(-5728));
        g.fill(BoaArt.circle(62.0, 60.0, 20.0));
        g.setColor(BoaArt.c(-10873338));
        g.setStroke(new BasicStroke(4.0f));
        g.draw(outer);
        g.dispose();
        return img;
    }

    public static BufferedImage ring() {
        int s = 256;
        double c = (double)s / 2.0;
        BufferedImage img = BoaArt.image(s, s);
        for (int y = 0; y < s; ++y) {
            for (int x = 0; x < s; ++x) {
                double dx = ((double)x + 0.5 - c) / c;
                double dy = ((double)y + 0.5 - c) / c;
                double r = Math.sqrt(dx * dx + dy * dy);
                if (r > 1.0 || r < 0.55) continue;
                double a = r > 0.88 ? 1.0 : (r > 0.76 ? 0.55 : 0.22);
                img.setRGB(x, y, (int)(a * 255.0) << 24 | 0xFFFFFF);
            }
        }
        return img;
    }

    public static BufferedImage softRing() {
        int s = 256;
        double c = (double)s / 2.0;
        BufferedImage img = BoaArt.image(s, s);
        for (int y = 0; y < s; ++y) {
            for (int x = 0; x < s; ++x) {
                double inner;
                double halo;
                double dx = ((double)x + 0.5 - c) / c;
                double dy = ((double)y + 0.5 - c) / c;
                double r = Math.sqrt(dx * dx + dy * dy);
                if (r >= 1.0) continue;
                double band = Math.exp(-Math.pow((r - 0.8) / 0.07, 2.0));
                double a = Math.min(1.0, band + (halo = 0.45 * Math.exp(-Math.pow((r - 0.8) / 0.17, 2.0))) + (inner = r < 0.8 ? 0.1 * (r / 0.8) * (r / 0.8) : 0.0));
                if (a <= 0.004) continue;
                img.setRGB(x, y, (int)(a * 255.0) << 24 | 0xFFFFFF);
            }
        }
        return img;
    }

    public static BufferedImage spark() {
        int s = 32;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        g.setColor(Color.WHITE);
        Path2D.Double p = new Path2D.Double();
        ((Path2D)p).moveTo(16.0, 0.0);
        ((Path2D)p).lineTo(19.0, 13.0);
        ((Path2D)p).lineTo(32.0, 16.0);
        ((Path2D)p).lineTo(19.0, 19.0);
        ((Path2D)p).lineTo(16.0, 32.0);
        ((Path2D)p).lineTo(13.0, 19.0);
        ((Path2D)p).lineTo(0.0, 16.0);
        ((Path2D)p).lineTo(13.0, 13.0);
        p.closePath();
        g.fill(p);
        g.dispose();
        return img;
    }

    private static Path2D wavyEllipse(double cx, double cy, double rx, double ry, int lobes, double amp, double phase) {
        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i <= 96; ++i) {
            double a = (double)i * Math.PI * 2.0 / 96.0;
            double k = 1.0 + amp / Math.max(rx, 1.0) * Math.sin(a * (double)lobes + phase);
            double x = cx + Math.cos(a) * rx * k;
            double y = cy + Math.sin(a) * ry * k;
            if (i == 0) {
                ((Path2D)path).moveTo(x, y);
                continue;
            }
            ((Path2D)path).lineTo(x, y);
        }
        path.closePath();
        return path;
    }

    public static BufferedImage cross() {
        int s = 32;
        BufferedImage img = BoaArt.image(s, s);
        Graphics2D g = BoaArt.g(img);
        g.setStroke(new BasicStroke(9.0f, 1, 1));
        g.setColor(INK_C);
        g.drawLine(6, 6, 26, 26);
        g.drawLine(26, 6, 6, 26);
        g.setStroke(new BasicStroke(5.0f, 1, 1));
        g.setColor(BoaArt.c(-1821909));
        g.drawLine(6, 6, 26, 26);
        g.drawLine(26, 6, 6, 26);
        g.dispose();
        return img;
    }

    public static BufferedImage white() {
        BufferedImage img = BoaArt.image(4, 4);
        for (int y = 0; y < 4; ++y) {
            for (int x = 0; x < 4; ++x) {
                img.setRGB(x, y, -1);
            }
        }
        return img;
    }

    public static BufferedImage stamp(BufferedImage skin, long seed) {
        int cell = 64;
        int size = 8 * cell;
        Random rng = new Random(seed);
        int[] face = new int[64];
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                int px = BoaArt.texel(skin, 8 + x, 8 + y);
                int hat = BoaArt.texel(skin, 40 + x, 8 + y);
                if (hat >>> 24 > 8) {
                    px = hat;
                }
                face[y * 8 + x] = px >>> 24 > 8 ? BoaArt.ink(px) : 0;
            }
        }
        double[][][] octaves = new double[][][]{BoaArt.lattice(rng, 6), BoaArt.lattice(rng, 14), BoaArt.lattice(rng, 34)};
        double[] weights = new double[]{0.55, 0.3, 0.15};
        int[] cells = new int[]{6, 14, 34};
        BufferedImage img = BoaArt.image(size, size);
        double edge = (double)size * 0.11;
        for (int y = 0; y < size; ++y) {
            for (int x = 0; x < size; ++x) {
                int px = face[y / cell * 8 + x / cell];
                if (px == 0) continue;
                double n = 0.0;
                for (int o = 0; o < 3; ++o) {
                    n += BoaArt.sample(octaves[o], cells[o], (double)x / (double)size, (double)y / (double)size) * weights[o];
                }
                double inset = (double)Math.min(Math.min(x, y), Math.min(size - 1 - x, size - 1 - y)) / edge;
                double keep = Math.min(1.0, inset) * 0.78 + 0.22;
                if (n > keep || n > keep - 0.045 && rng.nextDouble() < 0.42) continue;
                if (rng.nextDouble() < 0.3) {
                    double k = 1.0 + (n - 0.5) * 0.16;
                    px = BoaArt.scale(px, k);
                }
                img.setRGB(x, y, px);
            }
        }
        for (int i = 0; i < 150; ++i) {
            int cx = rng.nextInt(size);
            int cy = rng.nextInt(size);
            double inset = (double)Math.min(Math.min(cx, cy), Math.min(size - 1 - cx, size - 1 - cy)) / ((double)size * 0.5);
            if (rng.nextDouble() > Math.pow(1.0 - inset, 2.0) * 0.92 + 0.06) continue;
            int r = 2 + rng.nextInt(5);
            for (int yy = Math.max(0, cy - r); yy < Math.min(size, cy + r + 1); ++yy) {
                for (int xx = Math.max(0, cx - r); xx < Math.min(size, cx + r + 1); ++xx) {
                    if ((xx - cx) * (xx - cx) + (yy - cy) * (yy - cy) > r * r) continue;
                    img.setRGB(xx, yy, 0);
                }
            }
        }
        return img;
    }

    private static int texel(BufferedImage skin, int x, int y) {
        return x < skin.getWidth() && y < skin.getHeight() ? skin.getRGB(x, y) : 0;
    }

    private static int ink(int argb) {
        double r = (double)(argb >> 16 & 0xFF) / 255.0;
        double gr = (double)(argb >> 8 & 0xFF) / 255.0;
        double b = (double)(argb & 0xFF) / 255.0;
        double lum = 0.299 * r + 0.587 * gr + 0.114 * b;
        r = lum + (r - lum) * 0.5;
        gr = lum + (gr - lum) * 0.5;
        b = lum + (b - lum) * 0.5;
        r += (0.6 - r) * 0.34;
        gr += (0.6 - gr) * 0.34;
        b += (0.6 - b) * 0.34;
        return 0xFF000000 | BoaArt.clamp((int)Math.round(r * 255.0)) << 16 | BoaArt.clamp((int)Math.round(gr * 255.0)) << 8 | BoaArt.clamp((int)Math.round(b * 255.0));
    }

    private static int scale(int argb, double k) {
        return argb & 0xFF000000 | BoaArt.clamp((int)((double)(argb >> 16 & 0xFF) * k)) << 16 | BoaArt.clamp((int)((double)(argb >> 8 & 0xFF) * k)) << 8 | BoaArt.clamp((int)((double)(argb & 0xFF) * k));
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }

    private static double[][] lattice(Random rng, int cells) {
        double[][] grid;
        for (double[] row : grid = new double[cells + 1][cells + 1]) {
            for (int i = 0; i < row.length; ++i) {
                row[i] = rng.nextDouble();
            }
        }
        return grid;
    }

    private static double sample(double[][] grid, int cells, double u, double v) {
        double fx = u * (double)cells;
        double fy = v * (double)cells;
        int ix = Math.min(cells - 1, (int)fx);
        int iy = Math.min(cells - 1, (int)fy);
        double tx = fx - (double)ix;
        double ty = fy - (double)iy;
        tx = tx * tx * (3.0 - 2.0 * tx);
        ty = ty * ty * (3.0 - 2.0 * ty);
        double a = grid[iy][ix] + (grid[iy][ix + 1] - grid[iy][ix]) * tx;
        double b = grid[iy + 1][ix] + (grid[iy + 1][ix + 1] - grid[iy + 1][ix]) * tx;
        return a + (b - a) * ty;
    }
}

