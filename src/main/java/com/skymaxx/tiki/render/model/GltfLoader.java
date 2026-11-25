package com.skymaxx.tiki.render.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skymaxx.tiki.render.model.TikiModel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

public final class GltfLoader {
    private static final int ALPHA_CUTOFF = 12;

    private GltfLoader() {
    }

    public static TikiModel load(InputStream in, BufferedImage override, boolean posed) throws IOException {
        BufferedImage skin;
        JsonObject root;
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);){
            root = JsonParser.parseReader((Reader)reader).getAsJsonObject();
        }
        byte[][] buffers = GltfLoader.readBuffers(root);
        BufferedImage bufferedImage = skin = override != null ? override : GltfLoader.readImage(root);
        if (skin == null) {
            throw new IOException("no texture in the model and no override supplied");
        }
        ArrayList<TikiModel.Quad> quads = new ArrayList<TikiModel.Quad>();
        ArrayList<Box> boxes = new ArrayList<Box>();
        JsonArray nodes = GltfLoader.array(root, "nodes");
        for (JsonElement sceneNode : GltfLoader.sceneRoots(root)) {
            GltfLoader.walk(root, buffers, skin, nodes, sceneNode.getAsInt(), 0.0f, 0.0f, 0.0f, GltfLoader.identity(), posed, quads, boxes);
        }
        if (quads.isEmpty()) {
            throw new IOException("model has no visible faces");
        }
        float minY = Float.MAX_VALUE;
        float maxY = -3.4028235E38f;
        float radiusSq = 0.0f;
        float halfX = 0.0f;
        float halfZ = 0.0f;
        for (TikiModel.Quad q : quads) {
            for (int corner = 0; corner < 4; ++corner) {
                float a = (corner & 1) == 0 ? 0.0f : 1.0f;
                float b = (corner & 2) == 0 ? 0.0f : 1.0f;
                float x = q.ox + q.ux * a + q.vx * b;
                float y = q.oy + q.uy * a + q.vy * b;
                float z = q.oz + q.uz * a + q.vz * b;
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                radiusSq = Math.max(radiusSq, x * x + z * z);
                halfX = Math.max(halfX, Math.abs(x));
                halfZ = Math.max(halfZ, Math.abs(z));
            }
        }
        int[] bottomRect = null;
        int bottomInk = 0;
        for (TikiModel.Quad q : quads) {
            int ink;
            if (q.ny > -0.9f || (ink = GltfLoader.inkCount(skin, new int[]{q.u0, q.v0, q.u0 + q.uw, q.v0 + q.vh})) <= bottomInk) continue;
            bottomInk = ink;
            bottomRect = new int[]{q.u0, q.v0, q.uw, q.vh};
        }
        Box core = GltfLoader.core(boxes);
        float coreHeight = core == null ? 0.0f : core.maxY - core.minY;
        float coreCentre = core == null ? 0.0f : (core.minY + core.maxY) * 0.5f;
        float coreHalfX = core == null ? 0.0f : core.halfX;
        float coreHalfZ = core == null ? 0.0f : core.halfZ;
        return new TikiModel(quads, skin, minY, maxY, (float)Math.sqrt(radiusSq), coreHeight, coreCentre, halfX, halfZ, coreHalfX, coreHalfZ, bottomRect);
    }

    private static Box core(List<Box> boxes) {
        Box best = null;
        for (Box box : boxes) {
            if (box.ink <= 0 || best != null && box.ink <= best.ink && (box.ink != best.ink || !(box.maxY - box.minY > best.maxY - best.minY))) continue;
            best = box;
        }
        return best;
    }

    private static void walk(JsonObject root, byte[][] buffers, BufferedImage skin, JsonArray nodes, int index, float tx, float ty, float tz, float[] rot, boolean posed, List<TikiModel.Quad> out, List<Box> boxes) {
        if (index < 0 || index >= nodes.size()) {
            return;
        }
        JsonObject node = nodes.get(index).getAsJsonObject();
        float[] local = GltfLoader.identity();
        if (posed && node.has("rotation")) {
            local = GltfLoader.quaternionToMatrix(GltfLoader.floats(node.getAsJsonArray("rotation")));
        }
        float[] combined = GltfLoader.multiply(rot, local);
        float lx = 0.0f;
        float ly = 0.0f;
        float lz = 0.0f;
        if (node.has("translation")) {
            float[] t = GltfLoader.floats(node.getAsJsonArray("translation"));
            lx = t[0];
            ly = t[1];
            lz = t[2];
        }
        float wx = tx + rot[0] * lx + rot[1] * ly + rot[2] * lz;
        float wy = ty + rot[3] * lx + rot[4] * ly + rot[5] * lz;
        float wz = tz + rot[6] * lx + rot[7] * ly + rot[8] * lz;
        if (node.has("mesh")) {
            GltfLoader.emit(root, buffers, skin, node.get("mesh").getAsInt(), wx, wy, wz, combined, out, boxes);
        }
        if (node.has("children")) {
            for (JsonElement child : node.getAsJsonArray("children")) {
                GltfLoader.walk(root, buffers, skin, nodes, child.getAsInt(), wx, wy, wz, combined, posed, out, boxes);
            }
        }
    }

    private static void emit(JsonObject root, byte[][] buffers, BufferedImage skin, int meshIndex, float tx, float ty, float tz, float[] rot, List<TikiModel.Quad> out, List<Box> boxes) {
        JsonArray meshes = GltfLoader.array(root, "meshes");
        if (meshIndex < 0 || meshIndex >= meshes.size()) {
            return;
        }
        for (JsonElement primElement : meshes.get(meshIndex).getAsJsonObject().getAsJsonArray("primitives")) {
            JsonObject prim = primElement.getAsJsonObject();
            JsonObject attributes = prim.getAsJsonObject("attributes");
            if (!attributes.has("POSITION") || !attributes.has("TEXCOORD_0") || !attributes.has("NORMAL")) continue;
            float[][] pos = GltfLoader.readAccessor(root, buffers, attributes.get("POSITION").getAsInt(), 3);
            float[][] nrm = GltfLoader.readAccessor(root, buffers, attributes.get("NORMAL").getAsInt(), 3);
            float[][] uv = GltfLoader.readAccessor(root, buffers, attributes.get("TEXCOORD_0").getAsInt(), 2);
            if (pos == null || nrm == null || uv == null || pos.length % 4 != 0 || nrm.length < pos.length || uv.length < pos.length) continue;
            int faces = pos.length / 4;
            int[][] rects = new int[faces][];
            float[][] normals = new float[faces][];
            for (int f = 0; f < faces; ++f) {
                rects[f] = GltfLoader.rectFor(uv, f * 4, skin.getWidth(), skin.getHeight());
                normals[f] = nrm[f * 4];
            }
            Box box = new Box();
            for (int f = 0; f < faces; ++f) {
                int ink;
                int[] rect = rects[f];
                if (rect == null || (ink = GltfLoader.inkCount(skin, rect)) <= 0) continue;
                box.ink += ink;
                int[] blank = GltfLoader.blankRectFor(rects, normals, f, skin);
                TikiModel.Quad quad = GltfLoader.buildQuad(pos, uv, normals[f], f * 4, rect, blank, tx, ty, tz, rot, skin);
                if (quad == null) continue;
                out.add(quad);
                for (int corner = 0; corner < 4; ++corner) {
                    float a = (corner & 1) == 0 ? 0.0f : 1.0f;
                    float b = (corner & 2) == 0 ? 0.0f : 1.0f;
                    float y = quad.oy + quad.uy * a + quad.vy * b;
                    box.minY = Math.min(box.minY, y);
                    box.maxY = Math.max(box.maxY, y);
                    box.halfX = Math.max(box.halfX, Math.abs(quad.ox + quad.ux * a + quad.vx * b));
                    box.halfZ = Math.max(box.halfZ, Math.abs(quad.oz + quad.uz * a + quad.vz * b));
                }
            }
            if (box.ink <= 0 || !(box.maxY > box.minY)) continue;
            boxes.add(box);
        }
    }

    private static int[] blankRectFor(int[][] rects, float[][] normals, int face, BufferedImage skin) {
        float[] n = normals[face];
        if (n[0] == 0.0f && n[1] == 0.0f && n[2] == 0.0f) {
            return null;
        }
        for (int other = 0; other < rects.length; ++other) {
            float[] m;
            if (other == face || rects[other] == null || n[0] + (m = normals[other])[0] != 0.0f || n[1] + m[1] != 0.0f || n[2] + m[2] != 0.0f) continue;
            return GltfLoader.hasInk(skin, rects[other]) ? rects[other] : null;
        }
        return null;
    }

    private static TikiModel.Quad buildQuad(float[][] pos, float[][] uv, float[] normal, int base, int[] rect, int[] blank, float tx, float ty, float tz, float[] rot, BufferedImage skin) {
        int[] nArray;
        int origin = -1;
        int alongU = -1;
        int alongV = -1;
        for (int i = 0; i < 4; ++i) {
            boolean top;
            int u = Math.round(uv[base + i][0] * (float)skin.getWidth());
            int v = Math.round(uv[base + i][1] * (float)skin.getHeight());
            boolean left = u == rect[0];
            boolean bl = top = v == rect[1];
            if (left && top) {
                origin = base + i;
                continue;
            }
            if (!left && top) {
                alongU = base + i;
                continue;
            }
            if (!left) continue;
            alongV = base + i;
        }
        if (origin < 0 || alongU < 0 || alongV < 0) {
            return null;
        }
        float[] o = GltfLoader.transform(pos[origin], rot, tx, ty, tz);
        float[] a = GltfLoader.transform(pos[alongU], rot, tx, ty, tz);
        float[] b = GltfLoader.transform(pos[alongV], rot, tx, ty, tz);
        float[] n = GltfLoader.rotate(normal, rot);
        o = GltfLoader.faceCamera(o);
        a = GltfLoader.faceCamera(a);
        b = GltfLoader.faceCamera(b);
        boolean front = (n = GltfLoader.faceCamera(n))[2] > 0.5f;
        float f = o[0];
        float f2 = o[1];
        float f3 = o[2];
        float f4 = a[0] - o[0];
        float f5 = a[1] - o[1];
        float f6 = a[2] - o[2];
        float f7 = b[0] - o[0];
        float f8 = b[1] - o[1];
        float f9 = b[2] - o[2];
        float f10 = n[0];
        float f11 = n[1];
        float f12 = n[2];
        int n2 = rect[0];
        int n3 = rect[1];
        int n4 = rect[2] - rect[0];
        int n5 = rect[3] - rect[1];
        if (blank == null) {
            nArray = null;
        } else {
            int[] nArray2 = new int[4];
            nArray2[0] = blank[0];
            nArray2[1] = blank[1];
            nArray2[2] = blank[2] - blank[0];
            nArray = nArray2;
            nArray2[3] = blank[3] - blank[1];
        }
        return new TikiModel.Quad(f, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, n2, n3, n4, n5, nArray, front);
    }

    private static float[] faceCamera(float[] p) {
        return new float[]{-p[0], p[1], -p[2]};
    }

    private static float[] transform(float[] p, float[] m, float tx, float ty, float tz) {
        float[] r = GltfLoader.rotate(p, m);
        return new float[]{r[0] + tx, r[1] + ty, r[2] + tz};
    }

    private static float[] rotate(float[] p, float[] m) {
        return new float[]{m[0] * p[0] + m[1] * p[1] + m[2] * p[2], m[3] * p[0] + m[4] * p[1] + m[5] * p[2], m[6] * p[0] + m[7] * p[1] + m[8] * p[2]};
    }

    private static int[] rectFor(float[][] uv, int base, int width, int height) {
        int[] nArray;
        int u0 = Integer.MAX_VALUE;
        int v0 = Integer.MAX_VALUE;
        int u1 = Integer.MIN_VALUE;
        int v1 = Integer.MIN_VALUE;
        for (int i = 0; i < 4; ++i) {
            int u = Math.round(uv[base + i][0] * (float)width);
            int v = Math.round(uv[base + i][1] * (float)height);
            u0 = Math.min(u0, u);
            v0 = Math.min(v0, v);
            u1 = Math.max(u1, u);
            v1 = Math.max(v1, v);
        }
        u0 = Math.max(0, u0);
        v0 = Math.max(0, v0);
        u1 = Math.min(width, u1);
        v1 = Math.min(height, v1);
        if (u1 <= u0 || v1 <= v0) {
            nArray = null;
        } else {
            int[] nArray2 = new int[4];
            nArray2[0] = u0;
            nArray2[1] = v0;
            nArray2[2] = u1;
            nArray = nArray2;
            nArray2[3] = v1;
        }
        return nArray;
    }

    private static boolean hasInk(BufferedImage skin, int[] rect) {
        return GltfLoader.inkCount(skin, rect) > 0;
    }

    private static int inkCount(BufferedImage skin, int[] rect) {
        int ink = 0;
        for (int y = rect[1]; y < rect[3]; ++y) {
            for (int x = rect[0]; x < rect[2]; ++x) {
                if (skin.getRGB(x, y) >>> 24 <= 12) continue;
                ++ink;
            }
        }
        return ink;
    }

    private static BufferedImage readImage(JsonObject root) throws IOException {
        if (!root.has("images")) {
            return null;
        }
        JsonArray images = root.getAsJsonArray("images");
        if (images.isEmpty()) {
            return null;
        }
        JsonObject image = images.get(0).getAsJsonObject();
        if (!image.has("uri")) {
            return null;
        }
        byte[] png = GltfLoader.decodeDataUri(image.get("uri").getAsString());
        return png == null ? null : ImageIO.read(new ByteArrayInputStream(png));
    }

    private static byte[][] readBuffers(JsonObject root) throws IOException {
        JsonArray buffers = GltfLoader.array(root, "buffers");
        byte[][] out = new byte[buffers.size()][];
        for (int i = 0; i < buffers.size(); ++i) {
            JsonObject buffer = buffers.get(i).getAsJsonObject();
            if (!buffer.has("uri")) {
                throw new IOException("buffer " + i + " has no uri; .glb files are not supported");
            }
            out[i] = GltfLoader.decodeDataUri(buffer.get("uri").getAsString());
            if (out[i] != null) continue;
            throw new IOException("buffer " + i + " is not embedded; an external .bin is not supported");
        }
        return out;
    }

    private static byte[] decodeDataUri(String uri) {
        int comma = uri.indexOf(44);
        if (!uri.startsWith("data:") || comma < 0) {
            return null;
        }
        return Base64.getDecoder().decode(uri.substring(comma + 1));
    }

    private static float[][] readAccessor(JsonObject root, byte[][] buffers, int index, int components) {
        JsonArray accessors = GltfLoader.array(root, "accessors");
        if (index < 0 || index >= accessors.size()) {
            return null;
        }
        JsonObject accessor = accessors.get(index).getAsJsonObject();
        if (!accessor.has("bufferView") || accessor.get("componentType").getAsInt() != 5126) {
            return null;
        }
        int count = accessor.get("count").getAsInt();
        JsonArray views = GltfLoader.array(root, "bufferViews");
        int viewIndex = accessor.get("bufferView").getAsInt();
        if (viewIndex < 0 || viewIndex >= views.size()) {
            return null;
        }
        JsonObject view = views.get(viewIndex).getAsJsonObject();
        int bufferIndex = view.get("buffer").getAsInt();
        if (bufferIndex < 0 || bufferIndex >= buffers.length) {
            return null;
        }
        byte[] data = buffers[bufferIndex];
        int offset = GltfLoader.optInt(view, "byteOffset", 0) + GltfLoader.optInt(accessor, "byteOffset", 0);
        int stride = GltfLoader.optInt(view, "byteStride", components * 4);
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        float[][] out = new float[count][components];
        for (int i = 0; i < count; ++i) {
            int base = offset + i * stride;
            if (base < 0 || base + components * 4 > data.length) {
                return null;
            }
            for (int c = 0; c < components; ++c) {
                out[i][c] = bb.getFloat(base + c * 4);
            }
        }
        return out;
    }

    private static float[] identity() {
        return new float[]{1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    }

    private static float[] multiply(float[] a, float[] b) {
        float[] out = new float[9];
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 3; ++c) {
                out[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
            }
        }
        return out;
    }

    private static float[] quaternionToMatrix(float[] q) {
        float x = q[0];
        float y = q[1];
        float z = q[2];
        float w = q[3];
        return new float[]{1.0f - 2.0f * (y * y + z * z), 2.0f * (x * y - z * w), 2.0f * (x * z + y * w), 2.0f * (x * y + z * w), 1.0f - 2.0f * (x * x + z * z), 2.0f * (y * z - x * w), 2.0f * (x * z - y * w), 2.0f * (y * z + x * w), 1.0f - 2.0f * (x * x + y * y)};
    }

    private static JsonArray array(JsonObject root, String name) {
        return root.has(name) ? root.getAsJsonArray(name) : new JsonArray();
    }

    private static JsonArray sceneRoots(JsonObject root) {
        JsonObject scene;
        JsonArray scenes = GltfLoader.array(root, "scenes");
        if (scenes.isEmpty()) {
            return new JsonArray();
        }
        int index = GltfLoader.optInt(root, "scene", 0);
        if (index < 0 || index >= scenes.size()) {
            index = 0;
        }
        return (scene = scenes.get(index).getAsJsonObject()).has("nodes") ? scene.getAsJsonArray("nodes") : new JsonArray();
    }

    private static int optInt(JsonObject object, String name, int fallback) {
        return object.has(name) ? object.get(name).getAsInt() : fallback;
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; ++i) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    private static final class Box {
        float minY = Float.MAX_VALUE;
        float maxY = -3.4028235E38f;
        float halfX;
        float halfZ;
        int ink;

        private Box() {
        }
    }
}

