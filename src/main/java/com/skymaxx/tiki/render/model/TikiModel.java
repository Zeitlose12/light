package com.skymaxx.tiki.render.model;

import java.awt.image.BufferedImage;
import java.util.List;

public final class TikiModel {
    private final List<Quad> quads;
    private final BufferedImage skin;
    private final float minY;
    private final float maxY;
    private final float radius;
    private final float coreHeight;
    private final float coreCentreY;
    private final float halfX;
    private final float halfZ;
    private final float coreHalfX;
    private final float coreHalfZ;
    private final int[] bottomRect;
    private BufferedImage bareBottomSkin;

    TikiModel(List<Quad> quads, BufferedImage skin, float minY, float maxY, float radius, float coreHeight, float coreCentreY, float halfX, float halfZ, float coreHalfX, float coreHalfZ, int[] bottomRect) {
        this.quads = quads;
        this.skin = skin;
        this.minY = minY;
        this.maxY = maxY;
        this.radius = radius;
        this.coreHeight = coreHeight > 0.0f ? coreHeight : maxY - minY;
        this.coreCentreY = coreHeight > 0.0f ? coreCentreY : (minY + maxY) * 0.5f;
        this.halfX = halfX;
        this.halfZ = halfZ;
        this.coreHalfX = coreHalfX > 0.0f ? coreHalfX : halfX;
        this.coreHalfZ = coreHalfZ > 0.0f ? coreHalfZ : halfZ;
        this.bottomRect = bottomRect;
    }

    public List<Quad> quads() {
        return this.quads;
    }

    public BufferedImage skin() {
        return this.skin;
    }

    public BufferedImage bareBottomSkin() {
        if (this.bareBottomSkin == null) {
            this.bareBottomSkin = this.plainFrontEdge();
        }
        return this.bareBottomSkin;
    }

    private BufferedImage plainFrontEdge() {
        if (this.bottomRect == null || this.bottomRect[3] < 2) {
            return this.skin;
        }
        int w = this.skin.getWidth();
        int h = this.skin.getHeight();
        BufferedImage out = new BufferedImage(w, h, 2);
        out.setRGB(0, 0, w, h, this.skin.getRGB(0, 0, w, h, null, 0, w), 0, w);
        int back = this.bottomRect[1];
        int front = this.bottomRect[1] + this.bottomRect[3] - 1;
        for (int x = this.bottomRect[0]; x < this.bottomRect[0] + this.bottomRect[2]; ++x) {
            out.setRGB(x, front, this.skin.getRGB(x, back));
        }
        return out;
    }

    public float height() {
        return this.maxY - this.minY;
    }

    public float centreY() {
        return this.coreCentreY;
    }

    public float coreHeight() {
        return this.coreHeight;
    }

    public float radius() {
        return this.radius;
    }

    public float bottomY() {
        return this.minY;
    }

    public float halfX() {
        return this.halfX;
    }

    public float halfZ() {
        return this.halfZ;
    }

    public float coreHalfX() {
        return this.coreHalfX;
    }

    public float coreHalfZ() {
        return this.coreHalfZ;
    }

    public int[] bottomRect() {
        return this.bottomRect;
    }

    public static final class Quad {
        public final float ox;
        public final float oy;
        public final float oz;
        public final float ux;
        public final float uy;
        public final float uz;
        public final float vx;
        public final float vy;
        public final float vz;
        public final float nx;
        public final float ny;
        public final float nz;
        public final int u0;
        public final int v0;
        public final int uw;
        public final int vh;
        public final int[] blank;
        private final boolean front;

        Quad(float ox, float oy, float oz, float ux, float uy, float uz, float vx, float vy, float vz, float nx, float ny, float nz, int u0, int v0, int uw, int vh, int[] blank, boolean front) {
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.ux = ux;
            this.uy = uy;
            this.uz = uz;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.u0 = u0;
            this.v0 = v0;
            this.uw = uw;
            this.vh = vh;
            this.blank = blank;
            this.front = front;
        }

        public boolean isFront() {
            return this.front;
        }

        public float centreX() {
            return this.ox + (this.ux + this.vx) * 0.5f;
        }

        public float centreY() {
            return this.oy + (this.uy + this.vy) * 0.5f;
        }

        public float centreZ() {
            return this.oz + (this.uz + this.vz) * 0.5f;
        }
    }
}

