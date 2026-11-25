package com.skymaxx.tiki.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.anim.Prize;
import com.skymaxx.tiki.render.art.GlowArt;
import com.skymaxx.tiki.render.art.LeafArt;
import com.skymaxx.tiki.render.boa.BoaArt;
import com.skymaxx.tiki.render.model.GltfLoader;
import com.skymaxx.tiki.render.model.TikiModel;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class TikiAssets {
    public static final int TIERS = 4;
    private static final String MODEL_RESOURCE = "/assets/tikigamble/models/tiki%d.gltf";
    private static final String PRIZE_RESOURCE = "/assets/tikigamble/models/%s.gltf";
    public static final String SCARY_FACE = "scarytiki";
    private static final String STAMP_RESOURCE = "/assets/tikigamble/textures/stamp.png";
    private static final String TITLE_RESOURCE = "/assets/tikigamble/textures/title.png";
    private static final String MANIFEST = "generated.properties";
    private static final String[] LEGACY = new String[]{"stamp", "glow", "leaf0", "leaf1", "leaf2", "leaf3", "leaf4", "leaf5", "bush0", "bush1", "bush2"};
    private static final TikiModel[] MODELS = new TikiModel[4];
    private static final Identifier[] SKINS = new Identifier[4];
    private static final Identifier[] BARE_BOTTOMS = new Identifier[4];
    private static final Identifier[] LEAVES = new Identifier[6];
    private static final Identifier[] LEAVES_MIRRORED = new Identifier[6];
    private static final Map<String, Identifier> PRIZE_SKINS = new HashMap<String, Identifier>();
    private static final Map<String, Integer> PRIZE_TINTS = new HashMap<String, Integer>();
    private static final Map<String, Identifier> PRIZE_STAMPS = new HashMap<String, Identifier>();
    private static Identifier stamp;
    private static Identifier glow;
    private static Identifier ring;
    private static Identifier spark;
    private static Identifier bubbleFx;
    private static Identifier mote;
    private static Identifier title;
    private static final Map<Identifier, int[]> SIZES;
    private static final Map<Identifier, BufferedImage> IMAGES;
    private static final Properties manifest;
    private static boolean manifestDirty;
    private static final Set<String> written;
    private static File textureDir;
    private static File modelDir;
    private static boolean ready;
    private static boolean failed;

    private TikiAssets() {
    }

    public static void bind(File configDir) {
        textureDir = new File(configDir, "textures");
        modelDir = new File(configDir, "models");
    }

    public static File textureDir() {
        return textureDir;
    }

    public static File modelDir() {
        return modelDir;
    }

    public static boolean ready() {
        return ready;
    }

    public static TikiModel model(int tier) {
        return MODELS[Math.floorMod(tier, 4)];
    }

    public static Identifier skin(int tier) {
        return SKINS[Math.floorMod(tier, 4)];
    }

    public static Identifier bareBottomSkin(int tier) {
        return BARE_BOTTOMS[Math.floorMod(tier, 4)];
    }

    public static Identifier prizeSkin(Prize prize) {
        return prize == null || prize.faceAsset() == null ? null : PRIZE_SKINS.get(prize.faceAsset());
    }

    public static Identifier scarySkin() {
        return PRIZE_SKINS.get(SCARY_FACE);
    }

    public static int prizeTint(Prize prize) {
        if (prize == null || !prize.tintedHalo() || prize.faceAsset() == null) {
            return 0;
        }
        Integer tint = PRIZE_TINTS.get(prize.faceAsset());
        return tint == null ? 0 : tint;
    }

    public static Identifier leaf(int variant) {
        return LEAVES[Math.floorMod(variant, LEAVES.length)];
    }

    public static Identifier leafMirrored(int variant) {
        return LEAVES_MIRRORED[Math.floorMod(variant, LEAVES_MIRRORED.length)];
    }

    public static Identifier stamp() {
        return stamp;
    }

    public static Identifier stampFor(Prize prize) {
        if (prize == null || !prize.stamps()) {
            return null;
        }
        if (prize.faceAsset() == null) {
            return stamp;
        }
        Identifier own = PRIZE_STAMPS.get(prize.faceAsset());
        return own != null ? own : stamp;
    }

    public static Identifier glow() {
        return glow;
    }

    public static Identifier ring() {
        return ring;
    }

    public static Identifier spark() {
        return spark;
    }

    public static Identifier bubbleFx() {
        return bubbleFx;
    }

    public static Identifier mote() {
        return mote;
    }

    public static Identifier title() {
        return title;
    }

    public static int widthOf(Identifier id) {
        int[] size = SIZES.get(id);
        return size == null ? 64 : size[0];
    }

    public static int heightOf(Identifier id) {
        int[] size = SIZES.get(id);
        return size == null ? 64 : size[1];
    }

    public static BufferedImage imageOf(Identifier id) {
        return id == null ? null : IMAGES.get(id);
    }

    public static void init() {
        if (ready || failed) {
            return;
        }
        try {
            written.clear();
            TikiAssets.mkdirs(textureDir);
            TikiAssets.mkdirs(modelDir);
            if (!TikiAssets.readManifest()) {
                TikiAssets.retireLegacyArt();
            }
            for (int i = 0; i < 4; ++i) {
                TikiAssets.MODELS[i] = TikiAssets.loadModel(i);
                TikiAssets.SKINS[i] = TikiAssets.upload("tiki" + (i + 1), MODELS[i].skin());
                TikiAssets.BARE_BOTTOMS[i] = TikiAssets.upload("tiki" + (i + 1) + "_bare_bottom", MODELS[i].bareBottomSkin());
            }
            PRIZE_SKINS.clear();
            PRIZE_TINTS.clear();
            PRIZE_STAMPS.clear();
            for (Prize prize : Prize.values()) {
                if (prize.faceAsset() == null) continue;
                TikiAssets.loadPrize(prize.faceAsset(), true);
            }
            TikiAssets.loadPrize(SCARY_FACE, false);
            for (int i = 0; i < 6; ++i) {
                int v = i;
                BufferedImage art = TikiAssets.orGenerate("leaf" + i, () -> LeafArt.leaf(v));
                TikiAssets.LEAVES[i] = TikiAssets.upload("leaf" + i, art);
                TikiAssets.LEAVES_MIRRORED[i] = TikiAssets.upload("leaf" + i + "_mirrored", TikiAssets.flipHorizontally(art));
            }
            stamp = TikiAssets.upload("stamp", TikiAssets.orGenerate("stamp", () -> TikiAssets.resourceImage(STAMP_RESOURCE)));
            glow = TikiAssets.upload("glow", TikiAssets.orGenerate("glow", GlowArt::halo));
            ring = TikiAssets.upload("ring", TikiAssets.orGenerate("ring", BoaArt::softRing));
            spark = TikiAssets.upload("spark", TikiAssets.orGenerate("spark", BoaArt::spark));
            bubbleFx = TikiAssets.upload("bubblefx", TikiAssets.orGenerate("bubblefx", GlowArt::bubble));
            mote = TikiAssets.upload("mote", TikiAssets.orGenerate("mote", GlowArt::mote));
            title = TikiAssets.loadTitle();
            TikiAssets.sweepUnusedArt();
            TikiAssets.writeManifest();
            ready = true;
        }
        catch (Throwable t) {
            TikiGambleClient.LOGGER.error("[SkyMaxx] could not build the totem", t);
            failed = true;
        }
    }

    public static void reload() {
        Minecraft mc = Minecraft.getInstance();
        for (Identifier id : new ArrayList<Identifier>(SIZES.keySet())) {
            try {
                mc.getTextureManager().release(id);
            }
            catch (Throwable throwable) {}
        }
        SIZES.clear();
        IMAGES.clear();
        ready = false;
        failed = false;
        TikiAssets.init();
    }

    private static TikiModel loadModel(int tier) throws IOException {
        String name = "tiki" + (tier + 1);
        BufferedImage override = TikiAssets.readIfPresent(new File(textureDir, name + ".png"));
        File custom = new File(modelDir, name + ".gltf");
        if (custom.isFile()) {
            try (FileInputStream in = new FileInputStream(custom);){
                TikiModel model = GltfLoader.load(in, override, false);
                TikiGambleClient.LOGGER.info("[SkyMaxx] using custom model {}", (Object)custom.getName());
                TikiAssets.writeIfAbsent(new File(textureDir, name + ".png"), model.skin());
                return model;
            }
            catch (Exception e) {
                TikiGambleClient.LOGGER.warn("[SkyMaxx] could not read {}, using the built-in model", (Object)custom.getName(), (Object)e);
            }
        }
        String path = String.format(MODEL_RESOURCE, tier + 1);
        try (InputStream in = TikiAssets.class.getResourceAsStream(path);){
            if (in == null) {
                throw new IOException("missing model resource " + path);
            }
            TikiModel model = GltfLoader.load(in, override, false);
            TikiAssets.writeIfAbsent(new File(textureDir, name + ".png"), model.skin());
            TikiModel tikiModel = model;
            return tikiModel;
        }
    }

    private static void loadPrize(String name, boolean stamps) {
        try {
            TikiModel model;
            BufferedImage override = TikiAssets.readIfPresent(new File(textureDir, name + ".png"));
            File custom = new File(modelDir, name + ".gltf");
            if (custom.isFile()) {
                try (FileInputStream in = new FileInputStream(custom);){
                    model = GltfLoader.load(in, override, false);
                }
            } else {
                String path = String.format(PRIZE_RESOURCE, name);
                try (InputStream in = TikiAssets.class.getResourceAsStream(path);){
                    if (in == null) {
                        throw new IOException("missing model resource " + path);
                    }
                    model = GltfLoader.load(in, override, false);
                }
            }
            TikiAssets.writeIfAbsent(new File(textureDir, name + ".png"), model.skin());
            PRIZE_SKINS.put(name, TikiAssets.upload(name, model.skin()));
            PRIZE_TINTS.put(name, TikiAssets.faceTint(model));
            if (!stamps) {
                return;
            }
            PRIZE_STAMPS.put(name, TikiAssets.upload("stamp_" + name, TikiAssets.orGenerate("stamp_" + name, () -> BoaArt.stamp(model.skin(), (long)name.hashCode() * 2654435761L))));
        }
        catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not load the {} prize model", (Object)name, (Object)e);
            PRIZE_SKINS.remove(name);
            PRIZE_TINTS.remove(name);
            PRIZE_STAMPS.remove(name);
        }
    }

    private static int faceTint(TikiModel model) {
        BufferedImage skin = model.skin();
        double r = 0.0;
        double g = 0.0;
        double b = 0.0;
        double weight = 0.0;
        for (TikiModel.Quad quad : model.quads()) {
            if (!quad.isFront()) continue;
            for (int y = quad.v0; y < quad.v0 + quad.vh && y < skin.getHeight(); ++y) {
                for (int x = quad.u0; x < quad.u0 + quad.uw && x < skin.getWidth(); ++x) {
                    int argb = skin.getRGB(x, y);
                    if (argb >>> 24 <= 12) continue;
                    int pr = argb >> 16 & 0xFF;
                    int pg = argb >> 8 & 0xFF;
                    int pb = argb & 0xFF;
                    int max = Math.max(pr, Math.max(pg, pb));
                    int min = Math.min(pr, Math.min(pg, pb));
                    if (max == 0) continue;
                    float saturation = (float)(max - min) / (float)max;
                    double w = saturation * saturation;
                    r += (double)pr * w;
                    g += (double)pg * w;
                    b += (double)pb * w;
                    weight += w;
                }
            }
        }
        if (weight <= 1.0E-4) {
            return 0;
        }
        float[] hsb = Color.RGBtoHSB((int)(r / weight), (int)(g / weight), (int)(b / weight), null);
        float saturation = Math.max(0.45f, Math.min(0.8f, hsb[1]));
        return 0xFF000000 | Color.HSBtoRGB(hsb[0], saturation, 1.0f) & 0xFFFFFF;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static Identifier loadTitle() {
        BufferedImage custom = TikiAssets.readIfPresent(new File(textureDir, "title.png"));
        if (custom != null) {
            return TikiAssets.upload("title", custom);
        }
        try (InputStream in = TikiAssets.class.getResourceAsStream(TITLE_RESOURCE);){
            if (in == null) {
                Identifier identifier = null;
                return identifier;
            }
            BufferedImage shipped = ImageIO.read(in);
            if (shipped == null) {
                Identifier identifier = null;
                return identifier;
            }
            TikiAssets.writeIfAbsent(new File(textureDir, "title.png"), shipped);
            Identifier identifier = TikiAssets.upload("title", shipped);
            return identifier;
        }
        catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not read the title artwork", (Throwable)e);
            return null;
        }
    }

    private static BufferedImage orGenerate(String name, Supplier<BufferedImage> builtIn) {
        BufferedImage custom;
        File file = new File(textureDir, name + ".png");
        byte[] onDisk = TikiAssets.readBytes(file);
        if (onDisk != null && !TikiAssets.isOurs(name, onDisk) && (custom = TikiAssets.decode(onDisk, file)) != null) {
            return custom;
        }
        BufferedImage image = builtIn.get();
        byte[] encoded = TikiAssets.encode(image);
        if (!(encoded == null || onDisk != null && TikiAssets.sameBytes(onDisk, encoded))) {
            TikiAssets.write(file, encoded);
            TikiAssets.remember(name, encoded);
        } else if (encoded != null) {
            TikiAssets.remember(name, encoded);
        }
        return image;
    }

    private static BufferedImage resourceImage(String path) {
        try (InputStream in = TikiAssets.class.getResourceAsStream(path);){
            if (in == null) {
                throw new IOException("missing resource " + path);
            }
            return ImageIO.read(in);
        }
        catch (Exception e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }

    private static BufferedImage readIfPresent(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image != null && TikiGambleClient.config != null && TikiGambleClient.config.debug) {
                TikiGambleClient.LOGGER.info("[SkyMaxx] using custom texture {}", (Object)file.getName());
            }
            return image;
        }
        catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not read {}, using the built-in art", (Object)file.getName(), (Object)e);
            return null;
        }
    }

    private static void writeIfAbsent(File file, BufferedImage image) {
        if (file == null || file.exists() || image == null) {
            return;
        }
        byte[] encoded = TikiAssets.encode(image);
        if (encoded != null) {
            TikiAssets.write(file, encoded);
        }
    }

    private static void mkdirs(File dir) {
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not create {}", (Object)dir);
        }
    }

    private static boolean readManifest() {
        manifest.clear();
        manifestDirty = false;
        File file = new File(textureDir, MANIFEST);
        if (!file.isFile()) {
            return false;
        }
        try (FileInputStream in = new FileInputStream(file);){
            manifest.load(in);
        }
        catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not read {}", (Object)MANIFEST, (Object)e);
        }
        return true;
    }

    private static void retireLegacyArt() {
        File attic = new File(textureDir, "replaced");
        boolean moved = false;
        for (String name : LEGACY) {
            File file = new File(textureDir, name + ".png");
            if (!file.isFile()) continue;
            TikiAssets.mkdirs(attic);
            File target = new File(attic, name + ".png");
            try {
                Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                moved = true;
            }
            catch (Exception e) {
                TikiGambleClient.LOGGER.warn("[SkyMaxx] could not move aside {}", (Object)file, (Object)e);
            }
        }
        if (moved) {
            TikiGambleClient.LOGGER.info("[SkyMaxx] older generated art moved to {} and redrawn; put a file back in textures/ to keep using it", (Object)attic);
        }
        manifestDirty = true;
    }

    private static void writeManifest() {
        if (!manifestDirty || textureDir == null) {
            return;
        }
        File file = new File(textureDir, MANIFEST);
        try (OutputStream out = Files.newOutputStream(file.toPath(), new OpenOption[0]);){
            manifest.store(out, "Checksums of the art SkyMaxx generated. Edit a PNG and it is yours; this file is how the mod knows not to overwrite it.");
            manifestDirty = false;
        }
        catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not write {}", (Object)MANIFEST, (Object)e);
        }
    }

    private static boolean isOurs(String name, byte[] bytes) {
        String recorded = manifest.getProperty(name);
        return recorded != null && recorded.equals(Long.toString(TikiAssets.crc(bytes)));
    }

    private static void sweepUnusedArt() {
        for (String name : new ArrayList<String>(manifest.stringPropertyNames())) {
            File file;
            byte[] onDisk;
            if (written.contains(name) || (onDisk = TikiAssets.readBytes(file = new File(textureDir, name + ".png"))) != null && !TikiAssets.isOurs(name, onDisk)) continue;
            if (onDisk != null && !file.delete()) {
                TikiGambleClient.LOGGER.warn("[SkyMaxx] could not remove unused art {}", (Object)file);
                continue;
            }
            manifest.remove(name);
            manifestDirty = true;
        }
    }

    private static void remember(String name, byte[] bytes) {
        written.add(name);
        String value = Long.toString(TikiAssets.crc(bytes));
        if (!value.equals(manifest.getProperty(name))) {
            manifest.setProperty(name, value);
            manifestDirty = true;
        }
    }

    private static long crc(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    private static boolean sameBytes(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }

    private static byte[] readBytes(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            return Files.readAllBytes(file.toPath());
        }
        catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage decode(byte[] bytes, File from) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IOException("not an image");
            }
            if (TikiGambleClient.config != null && TikiGambleClient.config.debug) {
                TikiGambleClient.LOGGER.info("[SkyMaxx] using custom texture {}", (Object)from.getName());
            }
            return image;
        }
        catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not read {}, using the built-in art", (Object)from.getName(), (Object)e);
            return null;
        }
    }

    private static byte[] encode(BufferedImage image) {
        if (image == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write((RenderedImage)image, "png", out);
            return out.toByteArray();
        }
        catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not encode a texture", (Throwable)e);
            return null;
        }
    }

    private static void write(File file, byte[] bytes) {
        try {
            Files.write(file.toPath(), bytes, new OpenOption[0]);
        }
        catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not write {}", (Object)file, (Object)e);
        }
    }

    private static BufferedImage flipHorizontally(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, 2);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                out.setRGB(w - 1 - x, y, src.getRGB(x, y));
            }
        }
        return out;
    }

    private static Identifier upload(String name, BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        NativeImage image = new NativeImage(w, h, false);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                int argb = img.getRGB(x, y);
                int a = argb >>> 24 & 0xFF;
                int r = argb >> 16 & 0xFF;
                int g = argb >> 8 & 0xFF;
                int b = argb & 0xFF;
                image.setPixelABGR(x, y, a << 24 | b << 16 | g << 8 | r);
            }
        }
        Identifier id = TikiGambleClient.id("totem_" + name);
        Minecraft.getInstance().getTextureManager().register(id, (AbstractTexture)new DynamicTexture(() -> name, image));
        SIZES.put(id, new int[]{w, h});
        IMAGES.put(id, img);
        return id;
    }

    static {
        SIZES = new HashMap<Identifier, int[]>();
        IMAGES = new HashMap<Identifier, BufferedImage>();
        manifest = new Properties();
        written = new HashSet<String>();
    }
}

