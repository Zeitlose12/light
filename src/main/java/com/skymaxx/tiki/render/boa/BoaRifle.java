package com.skymaxx.tiki.render.boa;

import com.skymaxx.tiki.render.boa.BoaDrawList;
import com.skymaxx.tiki.render.boa.BoaTex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

final class BoaRifle {
    static final float SCOPE_Y = 0.66f;
    static final float EYEPIECE_Z = -0.95f;
    static final float GLASS_R = 0.2f;
    private static final float NEAR = 0.06f;
    private static final int[] WOOD = new int[]{-4949178, -7974357, -11193829};
    private static final int[] STEEL = new int[]{-7563357, -11314073, -13946567};
    private static final int[] BLUED = new int[]{-10326920, -13222586, -15131358};
    private static final int[] SCOPE = new int[]{-10985108, -13946824, -15460581};
    private static final int[] KNOB = new int[]{-4668724, -9010803, -12696242};
    private static final int[] SLEEVE = new int[]{-8553906, -10790861, -12961761};
    private static final int[] CUFF = new int[]{-10527687, -12238041, -13948394};
    private static final int[] SKIN = new int[]{-1788786, -3895190, -7446460};
    private static final float LX;
    private static final float LY;
    private static final float LZ;
    private static final int BARREL = 0;
    private static final int LEFT_ARM = 1;
    private static final int FOREND = 2;
    private static final int THUMB = 3;
    private static final int BOLT = 4;
    private static final int RECEIVER = 5;
    private static final int STOCK = 6;
    private static final int RIGHT_ARM = 7;
    private static final int MOUNTS = 8;
    private static final int TUBE = 9;
    private static final int FITTINGS = 10;
    private static final int GROUPS = 11;
    private static final int GLASS_SIDES = 24;
    private final List<Mesh> meshes = new ArrayList<Mesh>();
    private final List<List<Mesh>> groups = new ArrayList<List<Mesh>>();
    private Mesh lip;
    private final float[] glassX = new float[24];
    private final float[] glassY = new float[24];
    private float r00;
    private float r01;
    private float r02;
    private float r10;
    private float r11;
    private float r12;
    private float r20;
    private float r21;
    private float r22;
    private float c00;
    private float c01;
    private float c02;
    private float c10;
    private float c11;
    private float c12;
    private float c20;
    private float c21;
    private float c22;
    private float bCos;
    private float bSin;
    private Pose pose;
    private final float[] px = new float[64];
    private final float[] py = new float[64];
    private final float[] pz = new float[64];
    private final float[] qx = new float[64];
    private final float[] qy = new float[64];
    private final float[] qz = new float[64];
    private final float[] sx = new float[64];
    private final float[] sy = new float[64];

    BoaRifle() {
        this.build();
        for (int g = 0; g < 11; ++g) {
            ArrayList<Mesh> group = new ArrayList<Mesh>();
            for (Mesh m : this.meshes) {
                if (m.group != g) continue;
                group.add(m);
            }
            this.groups.add(group);
        }
        for (int k = 0; k < 24; ++k) {
            double a = (double)k * Math.PI * 2.0 / 24.0;
            this.glassX[k] = (float)Math.cos(a);
            this.glassY[k] = (float)Math.sin(a);
        }
    }

    private void build() {
        this.add(BoaRifle.lathe(0, BLUED, 2, 0.0f, 0.02f, new float[]{1.4f, 2.2f, 3.0f, 3.8f, 4.6f, 5.4f, 6.2f, 6.9f, 6.9f, 7.05f}, new float[]{0.135f, 0.126f, 0.118f, 0.111f, 0.104f, 0.098f, 0.093f, 0.09f, 0.102f, 0.102f}, 10));
        this.add(BoaRifle.arm(1, SLEEVE, -0.95f, -3.3f, 0.2f, -0.28f, -1.1f, 1.95f, 0.43f));
        this.add(BoaRifle.arm(1, CUFF, -0.36f, -1.34f, 1.74f, -0.27f, -1.02f, 2.03f, 0.46f));
        this.add(BoaRifle.box(1, SKIN, -0.47f, 0.4f, -0.98f, -0.4f, 1.95f, 2.85f));
        this.add(BoaRifle.hexa(2, WOOD, new float[][]{{-0.27f, -0.62f, 1.15f}, {0.27f, -0.62f, 1.15f}, {0.27f, -0.04f, 1.15f}, {-0.27f, -0.04f, 1.15f}, {-0.21f, -0.46f, 4.4f}, {0.21f, -0.46f, 4.4f}, {0.21f, -0.06f, 4.4f}, {-0.21f, -0.06f, 4.4f}}));
        this.add(BoaRifle.box(3, SKIN, -0.45f, -0.25f, -0.52f, -0.14f, 2.18f, 2.62f));
        this.add(BoaRifle.box(5, BLUED, -0.2f, 0.2f, -0.3f, 0.3f, -0.9f, 1.5f));
        this.add(BoaRifle.box(5, SCOPE, -0.15f, 0.15f, -0.56f, -0.3f, -0.05f, 0.95f));
        this.add(BoaRifle.box(5, STEEL, -0.14f, 0.14f, 0.0f, 0.26f, -1.18f, -0.9f));
        this.add(BoaRifle.hexa(6, WOOD, new float[][]{{-0.2f, -0.35f, -1.5f}, {0.2f, -0.35f, -1.5f}, {0.2f, -0.35f, -0.75f}, {-0.2f, -0.35f, -0.75f}, {-0.18f, -1.45f, -1.95f}, {0.18f, -1.45f, -1.95f}, {0.18f, -1.45f, -1.3f}, {-0.18f, -1.45f, -1.3f}}));
        this.add(BoaRifle.hexa(6, WOOD, new float[][]{{-0.22f, -0.6f, -0.9f}, {0.22f, -0.6f, -0.9f}, {0.22f, -0.02f, -0.9f}, {-0.22f, -0.02f, -0.9f}, {-0.25f, -1.5f, -4.3f}, {0.25f, -1.5f, -4.3f}, {0.25f, -0.2f, -4.3f}, {-0.25f, -0.2f, -4.3f}}));
        this.add(BoaRifle.arm(7, SLEEVE, 1.35f, -2.5f, -3.4f, 0.42f, -1.34f, -1.98f, 0.43f));
        this.add(BoaRifle.arm(7, CUFF, 0.52f, -1.44f, -2.1f, 0.38f, -1.28f, -1.9f, 0.46f));
        this.add(BoaRifle.arm(7, SKIN, 0.06f, -0.5f, -1.14f, 0.06f, -1.28f, -1.52f, 0.46f));
        Mesh handle = BoaRifle.arm(4, STEEL, 0.19f, 0.08f, -0.55f, 0.6f, -0.04f, -0.63f, 0.055f);
        handle.bolt = true;
        this.add(handle);
        Mesh knob = BoaRifle.lathe(4, KNOB, 0, -0.05f, -0.64f, new float[]{0.56f, 0.62f, 0.74f, 0.8f}, new float[]{0.07f, 0.105f, 0.105f, 0.07f}, 10);
        knob.bolt = true;
        this.add(knob);
        this.add(BoaRifle.box(8, BLUED, -0.11f, 0.11f, 0.3f, 0.56f, -0.1f, 0.12f));
        this.add(BoaRifle.box(8, BLUED, -0.11f, 0.11f, 0.3f, 0.56f, 1.55f, 1.77f));
        this.add(BoaRifle.lathe(9, SCOPE, 2, 0.0f, 0.66f, new float[]{-0.95f, -0.72f, -0.58f, -0.42f, -0.28f, 0.1f, 0.5f, 0.9f, 1.3f, 1.7f, 2.1f, 2.35f, 2.6f, 2.85f, 3.25f}, new float[]{0.27f, 0.27f, 0.235f, 0.2f, 0.165f, 0.165f, 0.165f, 0.165f, 0.165f, 0.165f, 0.165f, 0.2f, 0.26f, 0.29f, 0.29f}, 14));
        this.add(BoaRifle.lathe(10, BLUED, 2, 0.0f, 0.66f, new float[]{-0.1f, 0.12f}, new float[]{0.205f, 0.205f}, 14));
        this.add(BoaRifle.lathe(10, BLUED, 2, 0.0f, 0.66f, new float[]{1.55f, 1.77f}, new float[]{0.205f, 0.205f}, 14));
        this.add(BoaRifle.sticksOut(BoaRifle.lathe(10, SCOPE, 1, 0.0f, 0.8f, new float[]{0.81000006f, 1.0600001f}, new float[]{0.13f, 0.13f}, 10), 0.0f, 1.0f, 0.0f));
        this.add(BoaRifle.sticksOut(BoaRifle.lathe(10, SCOPE, 0, 0.66f, 0.8f, new float[]{0.15f, 0.38f}, new float[]{0.12f, 0.12f}, 10), 1.0f, 0.0f, 0.0f));
        this.lip = BoaRifle.annulus(10, SCOPE, -0.954f, 0.2f, 0.27f, 24);
    }

    private void add(Mesh mesh) {
        this.meshes.add(mesh);
    }

    private static Mesh sticksOut(Mesh mesh, float x, float y, float z) {
        mesh.outX = x;
        mesh.outY = y;
        mesh.outZ = z;
        mesh.hasOut = true;
        return mesh;
    }

    private static Mesh box(int group, int[] pal, float x0, float x1, float y0, float y1, float z0, float z1) {
        return BoaRifle.hexa(group, pal, new float[][]{{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}});
    }

    private static Mesh hexa(int group, int[] pal, float[][] c) {
        Builder b = new Builder();
        int[] v = new int[8];
        for (int i = 0; i < 8; ++i) {
            v[i] = b.v(c[i][0], c[i][1], c[i][2]);
        }
        float[] in = b.centroid();
        b.face(in, v[0], v[1], v[2], v[3]);
        b.face(in, v[4], v[5], v[6], v[7]);
        b.face(in, v[0], v[1], v[5], v[4]);
        b.face(in, v[1], v[2], v[6], v[5]);
        b.face(in, v[2], v[3], v[7], v[6]);
        b.face(in, v[3], v[0], v[4], v[7]);
        return new Mesh(group, pal, b.verts, b.faces, b.insides);
    }

    private static Mesh arm(int group, int[] pal, float ax, float ay, float az, float bx, float by, float bz, float half) {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float l = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        float hx = Math.abs(dy /= l) > 0.9f ? 1.0f : 0.0f;
        float hy = Math.abs(dy) > 0.9f ? 0.0f : 1.0f;
        float sx = -(dz /= l) * hy;
        float sy = dz * hx;
        float sz = (dx /= l) * hy - dy * hx;
        float sl = (float)Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx = sx / sl * half;
        sy = sy / sl * half;
        sz = sz / sl * half;
        float ux = dy * sz - dz * sy;
        float uy = dz * sx - dx * sz;
        float uz = dx * sy - dy * sx;
        float[][] c = new float[8][];
        float[][] ends = new float[][]{{ax, ay, az}, {bx, by, bz}};
        for (int e = 0; e < 2; ++e) {
            float[] p = ends[e];
            c[e * 4] = new float[]{p[0] - sx - ux, p[1] - sy - uy, p[2] - sz - uz};
            c[e * 4 + 1] = new float[]{p[0] + sx - ux, p[1] + sy - uy, p[2] + sz - uz};
            c[e * 4 + 2] = new float[]{p[0] + sx + ux, p[1] + sy + uy, p[2] + sz + uz};
            c[e * 4 + 3] = new float[]{p[0] - sx + ux, p[1] - sy + uy, p[2] - sz + uz};
        }
        return BoaRifle.hexa(group, pal, c);
    }

    private static Mesh lathe(int group, int[] pal, int axis, float a, float b, float[] at, float[] r, int sides) {
        int s;
        Builder bl = new Builder();
        int[][] ring = new int[at.length][sides];
        for (s = 0; s < at.length; ++s) {
            for (int k = 0; k < sides; ++k) {
                double th = ((double)k + 0.5) * Math.PI * 2.0 / (double)sides;
                float u = (float)Math.cos(th) * r[s];
                float w = (float)Math.sin(th) * r[s];
                int[] nArray = ring[s];
                nArray[k] = switch (axis) {
                    case 0 -> bl.v(at[s], a + w, b + u);
                    case 1 -> bl.v(a + u, at[s], b + w);
                    default -> bl.v(a + u, b + w, at[s]);
                };
            }
        }
        s = 0;
        while (s + 1 < at.length) {
            float mid = (at[s] + at[s + 1]) * 0.5f;
            float[] in = BoaRifle.axisPoint(axis, a, b, mid);
            if (Math.abs(at[s + 1] - at[s]) < 1.0E-5f) {
                float[] off = BoaRifle.axisPoint(axis, a, b, mid + (r[s + 1] > r[s] ? 1.0f : -1.0f));
                for (int k = 0; k < sides; ++k) {
                    int k1 = (k + 1) % sides;
                    bl.face(off, ring[s][k], ring[s][k1], ring[s + 1][k1], ring[s + 1][k]);
                }
            } else {
                for (int k = 0; k < sides; ++k) {
                    int k1 = (k + 1) % sides;
                    bl.face(in, ring[s][k], ring[s][k1], ring[s + 1][k1], ring[s + 1][k]);
                }
            }
            ++s;
        }
        int last = at.length - 1;
        bl.face(BoaRifle.axisPoint(axis, a, b, at[0] + 1.0f), ring[0]);
        bl.face(BoaRifle.axisPoint(axis, a, b, at[last] - 1.0f), ring[last]);
        return new Mesh(group, pal, bl.verts, bl.faces, bl.insides);
    }

    private static float[] axisPoint(int axis, float a, float b, float t) {
        float[] fArray;
        switch (axis) {
            case 0: {
                float[] fArray2 = new float[3];
                fArray2[0] = t;
                fArray2[1] = a;
                fArray = fArray2;
                fArray2[2] = b;
                break;
            }
            case 1: {
                float[] fArray3 = new float[3];
                fArray3[0] = a;
                fArray3[1] = t;
                fArray = fArray3;
                fArray3[2] = b;
                break;
            }
            default: {
                float[] fArray4 = new float[3];
                fArray4[0] = a;
                fArray4[1] = b;
                fArray = fArray4;
                fArray4[2] = t;
            }
        }
        return fArray;
    }

    private static Mesh annulus(int group, int[] pal, float z, float inner, float outer, int sides) {
        Builder bl = new Builder();
        int[] in = new int[sides];
        int[] out = new int[sides];
        for (int k = 0; k < sides; ++k) {
            double th = ((double)k + 0.5) * Math.PI * 2.0 / (double)sides;
            in[k] = bl.v((float)Math.cos(th) * inner, 0.66f + (float)Math.sin(th) * inner, z);
            out[k] = bl.v((float)Math.cos(th) * outer, 0.66f + (float)Math.sin(th) * outer, z);
        }
        float[] behind = new float[]{0.0f, 0.66f, z + 1.0f};
        for (int k = 0; k < sides; ++k) {
            int k1 = (k + 1) % sides;
            bl.face(behind, in[k], in[k1], out[k1], out[k]);
        }
        return new Mesh(group, pal, bl.verts, bl.faces, bl.insides);
    }

    void draw(BoaDrawList out, Pose p) {
        this.pose = p;
        this.setup(p);
        for (Mesh m : this.meshes) {
            this.transform(m);
        }
        this.transform(this.lip);
        for (int g = 0; g < this.groups.size(); ++g) {
            List<Mesh> group = this.groups.get(g);
            group.sort((a, b) -> Float.compare(b.meshDepth, a.meshDepth));
            if (g == 9) {
                for (Mesh m : this.groups.get(10)) {
                    if (!m.hasOut || !m.outAway) continue;
                    this.drawMesh(out, m);
                }
            }
            for (Mesh m : group) {
                if (g == 10 && m.hasOut && m.outAway) continue;
                this.drawMesh(out, m);
            }
        }
        this.glass(out);
    }

    private void setup(Pose p) {
        float fz;
        float cy = (float)Math.cos(Math.toRadians(p.yaw));
        float sy = (float)Math.sin(Math.toRadians(p.yaw));
        float cp = (float)Math.cos(Math.toRadians(p.pitch));
        float sp = (float)Math.sin(Math.toRadians(p.pitch));
        float cr = (float)Math.cos(Math.toRadians(p.roll));
        float sr = (float)Math.sin(Math.toRadians(p.roll));
        float[][] rz = new float[][]{{cr, -sr, 0.0f}, {sr, cr, 0.0f}, {0.0f, 0.0f, 1.0f}};
        float[][] rx = new float[][]{{1.0f, 0.0f, 0.0f}, {0.0f, cp, sp}, {0.0f, -sp, cp}};
        float[][] ry = new float[][]{{cy, 0.0f, sy}, {0.0f, 1.0f, 0.0f}, {-sy, 0.0f, cy}};
        float[][] r = BoaRifle.mul(ry, BoaRifle.mul(rx, rz));
        this.r00 = r[0][0];
        this.r01 = r[0][1];
        this.r02 = r[0][2];
        this.r10 = r[1][0];
        this.r11 = r[1][1];
        this.r12 = r[1][2];
        this.r20 = r[2][0];
        this.r21 = r[2][1];
        this.r22 = r[2][2];
        float cyaw = (float)Math.toRadians(p.camYaw);
        float cpit = (float)Math.toRadians(p.camPitch);
        float fx = (float)(Math.sin(cyaw) * Math.cos(cpit));
        float fy = (float)Math.sin(cpit);
        float rxv = fz = (float)(Math.cos(cyaw) * Math.cos(cpit));
        float ryv = 0.0f;
        float rzv = -fx;
        float rl = (float)Math.sqrt(rxv * rxv + rzv * rzv);
        float uxv = fy * (rzv /= rl) - fz * ryv;
        float uyv = fz * (rxv /= rl) - fx * rzv;
        float uzv = fx * ryv - fy * rxv;
        float crl = (float)Math.cos(Math.toRadians(p.camRoll));
        float srl = (float)Math.sin(Math.toRadians(p.camRoll));
        float rX = rxv * crl + uxv * srl;
        float rY = ryv * crl + uyv * srl;
        float rZ = rzv * crl + uzv * srl;
        float uX = -rxv * srl + uxv * crl;
        float uY = -ryv * srl + uyv * crl;
        float uZ = -rzv * srl + uzv * crl;
        this.c00 = rX;
        this.c01 = rY;
        this.c02 = rZ;
        this.c10 = uX;
        this.c11 = uY;
        this.c12 = uZ;
        this.c20 = fx;
        this.c21 = fy;
        this.c22 = fz;
        this.bCos = (float)Math.cos(Math.toRadians(p.boltLift));
        this.bSin = (float)Math.sin(Math.toRadians(p.boltLift));
    }

    private static float[][] mul(float[][] a, float[][] b) {
        float[][] c = new float[3][3];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                c[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return c;
    }

    private void transform(Mesh m) {
        Pose p = this.pose;
        float depthSum = 0.0f;
        for (int i = 0; i < m.mx.length; ++i) {
            float x = m.mx[i];
            float y = m.my[i];
            float z = m.mz[i];
            if (m.bolt) {
                float ly = y - 0.08f;
                float nx = x * this.bCos - ly * this.bSin;
                float ny = x * this.bSin + ly * this.bCos;
                x = nx;
                y = ny + 0.08f;
                z -= p.boltBack;
            }
            float wx = this.world(0, x, y, z);
            float wy = this.world(1, x, y, z);
            float wz = this.world(2, x, y, z);
            float dx = wx - p.camX;
            float dy = wy - p.camY;
            float dz = wz - p.camZ;
            m.cx[i] = this.c00 * dx + this.c01 * dy + this.c02 * dz;
            m.cy[i] = this.c10 * dx + this.c11 * dy + this.c12 * dz;
            m.cz[i] = this.c20 * dx + this.c21 * dy + this.c22 * dz;
            depthSum += m.cz[i];
        }
        m.meshDepth = depthSum / (float)m.mx.length;
        if (m.hasOut) {
            float wx = this.r00 * m.outX + this.r01 * m.outY + this.r02 * m.outZ;
            float wy = this.r10 * m.outX + this.r11 * m.outY + this.r12 * m.outZ;
            float wz = this.r20 * m.outX + this.r21 * m.outY + this.r22 * m.outZ;
            m.outAway = this.c20 * wx + this.c21 * wy + this.c22 * wz > 0.0f;
        }
        for (int f = 0; f < m.faces.length; ++f) {
            float nx = m.nx[f];
            float ny = m.ny[f];
            float nz = m.nz[f];
            if (m.bolt) {
                float bx = nx * this.bCos - ny * this.bSin;
                float by = nx * this.bSin + ny * this.bCos;
                nx = bx;
                ny = by;
            }
            float wnx = this.r00 * nx + this.r01 * ny + this.r02 * nz;
            float wny = this.r10 * nx + this.r11 * ny + this.r12 * nz;
            float wnz = this.r20 * nx + this.r21 * ny + this.r22 * nz;
            float cnx = this.c00 * wnx + this.c01 * wny + this.c02 * wnz;
            float cny = this.c10 * wnx + this.c11 * wny + this.c12 * wnz;
            float cnz = this.c20 * wnx + this.c21 * wny + this.c22 * wnz;
            int[] face = m.faces[f];
            float gx = 0.0f;
            float gy = 0.0f;
            float gz = 0.0f;
            for (int k : face) {
                gx += m.cx[k];
                gy += m.cy[k];
                gz += m.cz[k];
            }
            m.front[f] = cnx * (gx /= (float)face.length) + cny * (gy /= (float)face.length) + cnz * (gz /= (float)face.length) < 0.0f;
            m.depth[f] = (float)Math.sqrt(gx * gx + gy * gy + gz * gz);
            float tone = 0.5f + 0.5f * (wnx * LX + wny * LY + wnz * LZ);
            m.colour[f] = tone > 0.78f ? m.palette[0] : (tone > 0.52f ? m.palette[1] : m.palette[2]);
        }
    }

    private float world(int axis, float x, float y, float z) {
        Pose p = this.pose;
        float lx = x - p.pivotX;
        float ly = y - p.pivotY;
        float lz = z - p.pivotZ;
        return switch (axis) {
            case 0 -> this.r00 * lx + this.r01 * ly + this.r02 * lz + p.pivotX + p.moveX;
            case 1 -> this.r10 * lx + this.r11 * ly + this.r12 * lz + p.pivotY + p.moveY;
            default -> this.r20 * lx + this.r21 * ly + this.r22 * lz + p.pivotZ + p.moveZ;
        };
    }

    private void drawMesh(BoaDrawList out, Mesh m) {
        Integer[] order = m.order;
        Arrays.sort(order, (a, b) -> Float.compare(m.depth[b], m.depth[a]));
        for (Integer f : order) {
            if (!m.front[f]) continue;
            int[] face = m.faces[f];
            for (int k = 0; k < face.length; ++k) {
                this.px[k] = m.cx[face[k]];
                this.py[k] = m.cy[face[k]];
                this.pz[k] = m.cz[face[k]];
            }
            this.polygon(out, face.length, m.colour[f]);
        }
        for (int[] e : m.edges) {
            boolean b2;
            boolean a2 = m.front[e[2]];
            boolean bl = b2 = e[3] >= 0 && m.front[e[3]];
            if (a2 == b2) continue;
            this.edge(out, m.cx[e[0]], m.cy[e[0]], m.cz[e[0]], m.cx[e[1]], m.cy[e[1]], m.cz[e[1]]);
        }
    }

    private void polygon(BoaDrawList out, int n, int argb) {
        int m = this.clip(n);
        if (m < 3) {
            return;
        }
        for (int k = 0; k < m; ++k) {
            this.sx[k] = this.pose.centreX + this.pose.focal * this.qx[k] / this.qz[k];
            this.sy[k] = this.pose.centreY - this.pose.focal * this.qy[k] / this.qz[k];
        }
        this.fill(out, m, argb);
    }

    private void fill(BoaDrawList out, int m, int argb) {
        if (m == 3) {
            float mx = (this.sx[1] + this.sx[2]) * 0.5f;
            float my = (this.sy[1] + this.sy[2]) * 0.5f;
            BoaRifle.tri(out, this.sx[1], this.sy[1], this.sx[0], this.sy[0], mx, my, argb);
            BoaRifle.tri(out, this.sx[2], this.sy[2], this.sx[0], this.sy[0], mx, my, argb);
            return;
        }
        for (int i = 1; i <= m - 2; ++i) {
            if (i == m - 2) {
                BoaRifle.tri(out, this.sx[m - 1], this.sy[m - 1], this.sx[m - 2], this.sy[m - 2], this.sx[0], this.sy[0], argb);
                continue;
            }
            BoaRifle.tri(out, this.sx[i], this.sy[i], this.sx[0], this.sy[0], this.sx[i + 1], this.sy[i + 1], argb);
        }
    }

    private static void tri(BoaDrawList out, float ox, float oy, float ax, float ay, float bx, float by, int argb) {
        float gx = (ox + ax + bx) / 3.0f;
        float gy = (oy + ay + by) / 3.0f;
        float reach = Math.max(Math.max(BoaRifle.dist(ox, oy, gx, gy), BoaRifle.dist(ax, ay, gx, gy)), BoaRifle.dist(bx, by, gx, gy));
        if (reach < 0.2f) {
            return;
        }
        float k = 1.0f + 0.5f / reach;
        float ux = (ax = gx + (ax - gx) * k) - (ox = gx + (ox - gx) * k);
        float vy = (by = gy + (by - gy) * k) - (oy = gy + (oy - gy) * k);
        float uy = (ay = gy + (ay - gy) * k) - oy;
        float vx = (bx = gx + (bx - gx) * k) - ox;
        float det = ux * vy - uy * vx;
        if (Math.abs(det) < 0.01f) {
            return;
        }
        if (det < 0.0f) {
            float tx = ux;
            float ty = uy;
            ux = vx;
            uy = vy;
            vx = tx;
            vy = ty;
        }
        float s = 256.0f;
        out.quad(BoaTex.TRI, 0, 0, 256, 256, ux / s, uy / s, vx / s, vy / s, ox, oy, argb);
    }

    private static float dist(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    private int clip(int n) {
        int m = 0;
        for (int i = 0; i < n; ++i) {
            boolean next;
            int j = (i + 1) % n;
            boolean in = this.pz[i] >= 0.06f;
            boolean bl = next = this.pz[j] >= 0.06f;
            if (in) {
                this.qx[m] = this.px[i];
                this.qy[m] = this.py[i];
                this.qz[m] = this.pz[i];
                ++m;
            }
            if (in == next) continue;
            float t = (0.06f - this.pz[i]) / (this.pz[j] - this.pz[i]);
            this.qx[m] = this.px[i] + (this.px[j] - this.px[i]) * t;
            this.qy[m] = this.py[i] + (this.py[j] - this.py[i]) * t;
            this.qz[m] = 0.06f;
            ++m;
        }
        return m;
    }

    private void edge(BoaDrawList out, float ax, float ay, float az, float bx, float by, float bz) {
        if (az < 0.06f && bz < 0.06f) {
            return;
        }
        float t;
        if (az < 0.06f) {
            t = (0.06f - az) / (bz - az);
            ax += (bx - ax) * t;
            ay += (by - ay) * t;
            az = 0.06f;
        } else if (bz < 0.06f) {
            t = (0.06f - bz) / (az - bz);
            bx += (ax - bx) * t;
            by += (ay - by) * t;
            bz = 0.06f;
        }
        float x0 = this.pose.centreX + this.pose.focal * ax / az;
        float y0 = this.pose.centreY - this.pose.focal * ay / az;
        float x1 = this.pose.centreX + this.pose.focal * bx / bz;
        float y1 = this.pose.centreY - this.pose.focal * by / bz;
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) {
            return;
        }
        float ex = dx / len * this.pose.ink * 0.5f;
        float ey = dy / len * this.pose.ink * 0.5f;
        out.line(x0 - ex, y0 - ey, x1 + ex, y1 + ey, this.pose.ink, -16315383);
    }

    private void glass(BoaDrawList out) {
        float dark = this.pose.glassDark;
        int base = BoaRifle.lerp(-14987688, -16645116, dark);
        int deep = BoaRifle.lerp(-15981785, -16645116, dark);
        this.disc(out, 0.0f, 0.0f, 0.2f, base);
        this.disc(out, 0.035f, -0.035f, 0.194f, deep);
        this.drawMesh(out, this.lip);
        if (dark < 0.95f) {
            int glint = (int)(216.75f * (1.0f - dark)) << 24 | 0xFFFFFF;
            this.disc(out, -0.085f, 0.085f, 0.035f, glint);
        }
    }

    private void disc(BoaDrawList out, float ox, float oy, float r, int argb) {
        for (int k = 0; k < 24; ++k) {
            float x = ox + this.glassX[k] * r;
            float y = 0.66f + oy + this.glassY[k] * r;
            float z = -0.95199996f;
            float wx = this.world(0, x, y, z);
            float wy = this.world(1, x, y, z);
            float wz = this.world(2, x, y, z);
            float dx = wx - this.pose.camX;
            float dy = wy - this.pose.camY;
            float dz = wz - this.pose.camZ;
            this.px[k] = this.c00 * dx + this.c01 * dy + this.c02 * dz;
            this.py[k] = this.c10 * dx + this.c11 * dy + this.c12 * dz;
            this.pz[k] = this.c20 * dx + this.c21 * dy + this.c22 * dz;
        }
        this.polygon(out, 24, argb);
    }

    float[] project(Pose p, float x, float y, float z) {
        this.pose = p;
        this.setup(p);
        float wx = this.world(0, x, y, z);
        float wy = this.world(1, x, y, z);
        float wz = this.world(2, x, y, z);
        float dx = wx - this.pose.camX;
        float dy = wy - this.pose.camY;
        float dz = wz - this.pose.camZ;
        float cxv = this.c00 * dx + this.c01 * dy + this.c02 * dz;
        float cyv = this.c10 * dx + this.c11 * dy + this.c12 * dz;
        float czv = this.c20 * dx + this.c21 * dy + this.c22 * dz;
        if (czv < 0.06f) {
            return null;
        }
        return new float[]{this.pose.centreX + this.pose.focal * cxv / czv, this.pose.centreY - this.pose.focal * cyv / czv};
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        return 0xFF000000 | (int)((float)ar + (float)((b >> 16 & 0xFF) - ar) * t) << 16 | (int)((float)ag + (float)((b >> 8 & 0xFF) - ag) * t) << 8 | (int)((float)ab + (float)((b & 0xFF) - ab) * t);
    }

    static {
        float x = -0.45f;
        float y = 0.85f;
        float z = 0.1f;
        float l = (float)Math.sqrt(x * x + y * y + z * z);
        LX = x / l;
        LY = y / l;
        LZ = z / l;
    }

    private static final class Mesh {
        final int group;
        final int[] palette;
        boolean bolt;
        final float[] mx;
        final float[] my;
        final float[] mz;
        final int[][] faces;
        final float[] nx;
        final float[] ny;
        final float[] nz;
        final int[][] edges;
        final float[] cx;
        final float[] cy;
        final float[] cz;
        final boolean[] front;
        final int[] colour;
        final float[] depth;
        final Integer[] order;
        float meshDepth;
        float outX;
        float outY;
        float outZ;
        boolean hasOut;
        boolean outAway;

        Mesh(int group, int[] palette, List<float[]> verts, List<int[]> faceList, List<float[]> insides) {
            int i;
            this.group = group;
            this.palette = palette;
            int n = verts.size();
            this.mx = new float[n];
            this.my = new float[n];
            this.mz = new float[n];
            for (int i2 = 0; i2 < n; ++i2) {
                this.mx[i2] = verts.get(i2)[0];
                this.my[i2] = verts.get(i2)[1];
                this.mz[i2] = verts.get(i2)[2];
            }
            this.faces = faceList.toArray(new int[0][]);
            int f = this.faces.length;
            this.nx = new float[f];
            this.ny = new float[f];
            this.nz = new float[f];
            for (int i3 = 0; i3 < f; ++i3) {
                this.normal(i3, insides.get(i3));
            }
            HashMap<Long, int[]> byKey = new HashMap<Long, int[]>();
            ArrayList<int[]> edgeList = new ArrayList<int[]>();
            for (i = 0; i < f; ++i) {
                int[] face = this.faces[i];
                for (int k = 0; k < face.length; ++k) {
                    int a = face[k];
                    int b = face[(k + 1) % face.length];
                    long key = (long)Math.min(a, b) * 100000L + (long)Math.max(a, b);
                    int[] edge = (int[])byKey.get(key);
                    if (edge == null) {
                        edge = new int[]{a, b, i, -1};
                        byKey.put(key, edge);
                        edgeList.add(edge);
                        continue;
                    }
                    edge[3] = i;
                }
            }
            this.edges = edgeList.toArray(new int[0][]);
            this.cx = new float[n];
            this.cy = new float[n];
            this.cz = new float[n];
            this.front = new boolean[f];
            this.colour = new int[f];
            this.depth = new float[f];
            this.order = new Integer[f];
            for (i = 0; i < f; ++i) {
                this.order[i] = i;
            }
        }

        private void normal(int i, float[] inside) {
            int[] face = this.faces[i];
            float x = 0.0f;
            float y = 0.0f;
            float z = 0.0f;
            float gx = 0.0f;
            float gy = 0.0f;
            float gz = 0.0f;
            for (int k = 0; k < face.length; ++k) {
                int a = face[k];
                int b = face[(k + 1) % face.length];
                x += (this.my[a] - this.my[b]) * (this.mz[a] + this.mz[b]);
                y += (this.mz[a] - this.mz[b]) * (this.mx[a] + this.mx[b]);
                z += (this.mx[a] - this.mx[b]) * (this.my[a] + this.my[b]);
                gx += this.mx[a];
                gy += this.my[a];
                gz += this.mz[a];
            }
            gx /= (float)face.length;
            gy /= (float)face.length;
            gz /= (float)face.length;
            float l = (float)Math.sqrt(x * x + y * y + z * z);
            if (l < 1.0E-9f) {
                l = 1.0f;
            }
            if ((x /= l) * (gx - inside[0]) + (y /= l) * (gy - inside[1]) + (z /= l) * (gz - inside[2]) < 0.0f) {
                x = -x;
                y = -y;
                z = -z;
            }
            this.nx[i] = x;
            this.ny[i] = y;
            this.nz[i] = z;
        }
    }

    private static final class Builder {
        final List<float[]> verts = new ArrayList<float[]>();
        final List<int[]> faces = new ArrayList<int[]>();
        final List<float[]> insides = new ArrayList<float[]>();

        private Builder() {
        }

        int v(float x, float y, float z) {
            this.verts.add(new float[]{x, y, z});
            return this.verts.size() - 1;
        }

        void face(float[] inside, int ... idx) {
            this.faces.add(idx);
            this.insides.add(inside);
        }

        float[] centroid() {
            float x = 0.0f;
            float y = 0.0f;
            float z = 0.0f;
            for (float[] v : this.verts) {
                x += v[0];
                y += v[1];
                z += v[2];
            }
            int n = Math.max(1, this.verts.size());
            return new float[]{x / (float)n, y / (float)n, z / (float)n};
        }
    }

    static final class Pose {
        float yaw;
        float pitch;
        float roll;
        float pivotX;
        float pivotY = -0.6f;
        float pivotZ = -3.2f;
        float moveX;
        float moveY;
        float moveZ;
        float boltLift;
        float boltBack;
        float camX;
        float camY;
        float camZ;
        float camYaw;
        float camPitch;
        float camRoll;
        float focal;
        float centreX;
        float centreY;
        float ink;
        float glassDark;

        Pose() {
        }
    }
}

