package com.skymaxx.tiki.render.boa;

import com.skymaxx.tiki.render.boa.BoaTex;
import java.util.ArrayList;
import java.util.List;

public final class BoaDrawList {
    private final List<Op> ops = new ArrayList<Op>();
    private int size;
    private float alpha = 1.0f;

    public void clear() {
        this.size = 0;
        this.alpha = 1.0f;
    }

    public int size() {
        return this.size;
    }

    public Op get(int i) {
        return this.ops.get(i);
    }

    public void alpha(float value) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, value));
    }

    public float alpha() {
        return this.alpha;
    }

    private Op next(Kind kind) {
        Op op;
        if (this.size == this.ops.size()) {
            op = new Op();
            this.ops.add(op);
        } else {
            op = this.ops.get(this.size);
        }
        ++this.size;
        op.kind = kind;
        return op;
    }

    private int faded(int argb) {
        float mul = this.alpha;
        if (mul >= 0.999f) {
            return argb;
        }
        int a = (int)((float)(argb >>> 24 & 0xFF) * mul);
        return a << 24 | argb & 0xFFFFFF;
    }

    public void quad(BoaTex tex, int u0, int v0, int uw, int vh, float m00, float m01, float m10, float m11, float m20, float m21, int argb) {
        int colour = this.faded(argb);
        if (colour >>> 24 < 2) {
            return;
        }
        Op op = this.next(Kind.QUAD);
        op.tex = tex;
        op.u0 = u0;
        op.v0 = v0;
        op.uw = Math.max(1, uw);
        op.vh = Math.max(1, vh);
        op.m00 = m00;
        op.m01 = m01;
        op.m10 = m10;
        op.m11 = m11;
        op.m20 = m20;
        op.m21 = m21;
        op.argb = colour;
    }

    public void texPoly(BoaTex tex, int u0, int v0, int uw, int vh, float m00, float m01, float m10, float m11, float m20, float m21, float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int argb) {
        int colour = this.faded(argb);
        if (colour >>> 24 < 2) {
            return;
        }
        float area = ax * by - bx * ay + (bx * cy - cx * by) + (cx * dy - dx * cy) + (dx * ay - ax * dy);
        if (Math.abs(area) < 0.02f) {
            return;
        }
        Op op = this.next(Kind.TEX_POLY);
        op.tex = tex;
        op.u0 = u0;
        op.v0 = v0;
        op.uw = Math.max(1, uw);
        op.vh = Math.max(1, vh);
        op.m00 = m00;
        op.m01 = m01;
        op.m10 = m10;
        op.m11 = m11;
        op.m20 = m20;
        op.m21 = m21;
        op.px[0] = ax;
        op.py[0] = ay;
        op.px[2] = cx;
        op.py[2] = cy;
        if (area < 0.0f) {
            op.px[1] = bx;
            op.py[1] = by;
            op.px[3] = dx;
            op.py[3] = dy;
        } else {
            op.px[1] = dx;
            op.py[1] = dy;
            op.px[3] = bx;
            op.py[3] = by;
        }
        op.argb = colour;
    }

    public void region(BoaTex tex, int u0, int v0, int uw, int vh, float cx, float cy, float w, float h, float rot, int argb) {
        float cos = (float)Math.cos(rot);
        float sin = (float)Math.sin(rot);
        float sx = w / (float)uw;
        float sy = h / (float)vh;
        this.quad(tex, u0, v0, uw, vh, sx * cos, sx * sin, -sy * sin, sy * cos, cx - 0.5f * w * cos + 0.5f * h * sin, cy - 0.5f * w * sin - 0.5f * h * cos, argb);
    }

    public void sprite(BoaTex tex, int texW, int texH, float cx, float cy, float w, float h, float rot, int argb) {
        this.region(tex, 0, 0, texW, texH, cx, cy, w, h, rot, argb);
    }

    public void line(float x0, float y0, float x1, float y1, float width, int argb) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) {
            return;
        }
        this.region(BoaTex.WHITE, 0, 0, 4, 4, (x0 + x1) * 0.5f, (y0 + y1) * 0.5f, len, width, (float)Math.atan2(dy, dx), argb);
    }

    public void fill(float x0, float y0, float x1, float y1, int argb) {
        int colour = this.faded(argb);
        if (colour >>> 24 < 2 || x1 <= x0 || y1 <= y0) {
            return;
        }
        Op op = this.next(Kind.FILL);
        op.x0 = x0;
        op.y0 = y0;
        op.x1 = x1;
        op.y1 = y1;
        op.argb = colour;
    }

    public void poly(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int argb) {
        int colour = this.faded(argb);
        if (colour >>> 24 < 2) {
            return;
        }
        float area = ax * by - bx * ay + (bx * cy - cx * by) + (cx * dy - dx * cy) + (dx * ay - ax * dy);
        if (Math.abs(area) < 0.02f) {
            return;
        }
        Op op = this.next(Kind.POLY);
        op.px[0] = ax;
        op.py[0] = ay;
        op.px[2] = cx;
        op.py[2] = cy;
        if (area < 0.0f) {
            op.px[1] = bx;
            op.py[1] = by;
            op.px[3] = dx;
            op.py[3] = dy;
        } else {
            op.px[1] = dx;
            op.py[1] = dy;
            op.px[3] = bx;
            op.py[3] = by;
        }
        op.argb = colour;
    }

    public void text(String text, float x, float y, float scale, int argb, boolean centered) {
        int colour = this.faded(argb);
        if (colour >>> 24 < 4 || text == null || text.isEmpty()) {
            return;
        }
        Op op = this.next(Kind.TEXT);
        op.text = text;
        op.x0 = x;
        op.y0 = y;
        op.scale = scale;
        op.argb = colour;
        op.centered = centered;
    }

    public static final class Op {
        public Kind kind;
        public BoaTex tex;
        public int u0;
        public int v0;
        public int uw;
        public int vh;
        public float m00;
        public float m01;
        public float m10;
        public float m11;
        public float m20;
        public float m21;
        public int argb;
        public float x0;
        public float y0;
        public float x1;
        public float y1;
        public final float[] px = new float[4];
        public final float[] py = new float[4];
        public String text;
        public float scale;
        public boolean centered;
    }

    public static enum Kind {
        QUAD,
        TEX_POLY,
        FILL,
        POLY,
        TEXT;

    }
}

