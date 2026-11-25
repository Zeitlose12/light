package com.skymaxx.tiki.render.model;

import com.skymaxx.tiki.render.model.TikiModel;
import java.util.ArrayList;
import java.util.List;

public final class ModelProjection {
    private static final float KX = -0.42f;
    private static final float KY = 0.62f;
    private static final float KZ = 0.66f;
    private static final float KEY = 0.55f;
    private static final float FX = 0.25f;
    private static final float FY = -0.85f;
    private static final float FZ = 0.45f;
    private static final float FILL = 0.3f;
    private static final float AMBIENT = 0.3f;
    public static final int BANDS = 6;
    private static final float[] LEVELS = new float[]{0.4f, 0.52f, 0.63f, 0.74f, 0.87f, 1.0f};
    private static final float SHOULDER = 0.55f;
    private static final float FACING_EPSILON = 0.02f;
    private static final float INTERIOR_SHADE = 0.55f;
    private static final float CAP_DEPTH_BIAS = 0.01f;

    private ModelProjection() {
    }

    public static void project(TikiModel model, boolean blanked, float yawDeg, float pitchDeg, float size, float liftY, int tier, List<Face> out, FacePool pool) {
        ModelProjection.project(model, blanked, yawDeg, pitchDeg, size, 0.0f, liftY, 0.0f, tier, out, pool);
    }

    public static void project(TikiModel model, boolean blanked, float yawDeg, float pitchDeg, float size, float offX, float liftY, float offZ, int tier, List<Face> out, FacePool pool) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        float cy = (float)Math.cos(yaw);
        float sy = (float)Math.sin(yaw);
        float cp = (float)Math.cos(pitch);
        float sp = (float)Math.sin(pitch);
        int[] bottom = model.bottomRect();
        for (TikiModel.Quad q : model.quads()) {
            float nz1;
            float nz2;
            int u0 = q.u0;
            int v0 = q.v0;
            int uw = q.uw;
            int vh = q.vh;
            if (blanked && q.isFront()) {
                if (q.blank == null) continue;
                u0 = q.blank[0];
                v0 = q.blank[1];
                uw = q.blank[2];
                vh = q.blank[3];
            }
            if (Math.abs(nz2 = -q.ny * sp + (nz1 = -q.nx * sy + q.nz * cy) * cp) <= 0.02f) continue;
            boolean interior = nz2 < 0.0f;
            Face face = pool.take(out);
            face.tier = tier;
            face.facing = Math.abs(nz2);
            face.u0 = u0;
            face.v0 = v0;
            face.uw = uw;
            face.vh = vh;
            face.interior = interior;
            face.front = q.isFront() && !interior;
            face.underside = !interior && q.ny < -0.5f && bottom != null && q.u0 == bottom[0] && q.v0 == bottom[1] && q.uw == bottom[2] && q.vh == bottom[3];
            float ox = q.ox * cy + q.oz * sy + offX;
            float oz1 = -q.ox * sy + q.oz * cy + offZ;
            float oyw = q.oy + liftY;
            face.ox = ox * size;
            face.oy = -(oyw * cp + oz1 * sp) * size;
            float ux = q.ux * cy + q.uz * sy;
            float uz1 = -q.ux * sy + q.uz * cy;
            face.ux = ux * size;
            face.uy = -(q.uy * cp + uz1 * sp) * size;
            float vx = q.vx * cy + q.vz * sy;
            float vz1 = -q.vx * sy + q.vz * cy;
            face.vx = vx * size;
            face.vy = -(q.vy * cp + vz1 * sp) * size;
            float cxm = q.centreX();
            float cym = q.centreY() + liftY;
            float czm = q.centreZ();
            float cz1 = -cxm * sy + czm * cy + offZ;
            face.depth = -cym * sp + cz1 * cp;
            float wnx = q.nx * cy + q.nz * sy;
            ModelProjection.shade(face, wnx, q.ny, nz1, interior);
        }
    }

    private static void shade(Face face, float nx, float ny, float nz, boolean interior) {
        float within;
        float t;
        float key = (nx * -0.42f + ny * 0.62f + nz * 0.66f) * 0.5f + 0.5f;
        float fill = Math.max(0.0f, nx * 0.25f + ny * -0.85f + nz * 0.45f);
        float lit = 0.3f + 0.55f * key + 0.3f * fill;
        if (interior) {
            lit *= 0.55f;
        }
        face.tone = lit = lit < 0.0f ? 0.0f : (lit > 1.0f ? 1.0f : lit);
        float scaled = lit * 6.0f;
        int band = (int)scaled;
        if (band >= 6) {
            band = 5;
        }
        t = (t = ((within = scaled - (float)band) - 0.45f) / 0.55f) < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
        t = t * t * (3.0f - 2.0f * t);
        float low = LEVELS[band];
        float high = LEVELS[Math.min(5, band + 1)];
        face.band = band;
        face.brightness = low + (high - low) * t;
    }

    public static void projectUnderside(TikiModel model, float yawDeg, float pitchDeg, float size, float planeY, int tier, List<Face> out, FacePool pool) {
        int[] rect = model.bottomRect();
        if (rect == null) {
            return;
        }
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        float cy = (float)Math.cos(yaw);
        float sy = (float)Math.sin(yaw);
        float cp = (float)Math.cos(pitch);
        float sp = (float)Math.sin(pitch);
        float hx = model.coreHalfX();
        float hz = model.coreHalfZ();
        float y = planeY;
        if (hx <= 0.0f || hz <= 0.0f) {
            return;
        }
        Face face = pool.take(out);
        face.tier = tier;
        face.facing = Math.abs(sp);
        face.u0 = rect[0];
        face.v0 = rect[1];
        face.uw = rect[2];
        face.vh = rect[3];
        face.front = false;
        face.interior = false;
        face.underside = true;
        float ox = -hx * cy + -hz * sy;
        float oz = hx * sy + -hz * cy;
        face.ox = ox * size;
        face.oy = -(y * cp + oz * sp) * size;
        float ux = 2.0f * hx * cy;
        float uz = -(2.0f * hx) * sy;
        face.ux = ux * size;
        face.uy = -(uz * sp) * size;
        float vx = 2.0f * hz * sy;
        float vz = 2.0f * hz * cy;
        face.vx = vx * size;
        face.vy = -(vz * sp) * size;
        face.depth = -y * sp + 0.01f;
        ModelProjection.shade(face, 0.0f, -1.0f, 0.0f, false);
    }

    public static final class FacePool {
        private final ArrayList<Face> store = new ArrayList();
        private int used;

        public void reset() {
            this.used = 0;
        }

        Face take(List<Face> out) {
            if (this.used == this.store.size()) {
                this.store.add(new Face());
            }
            Face face = this.store.get(this.used++);
            face.front = false;
            face.interior = false;
            out.add(face);
            return face;
        }
    }

    public static final class Face {
        public int tier;
        public float ox;
        public float oy;
        public float ux;
        public float uy;
        public float vx;
        public float vy;
        public int u0;
        public int v0;
        public int uw;
        public int vh;
        public float depth;
        public float brightness;
        public int band;
        public float tone;
        public boolean front;
        public boolean interior;
        public boolean underside;
        public float facing = 1.0f;
    }
}

