package com.skymaxx.tiki.render.boa;

import com.mojang.blaze3d.platform.NativeImage;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.render.boa.BoaArtSet;
import com.skymaxx.tiki.render.boa.BoaTex;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class BoaAssets {
    private static final String RESOURCE = "/assets/tikigamble/models/boa/%s.gltf";
    private static final Map<BoaTex, Identifier> IDS = new EnumMap<BoaTex, Identifier>(BoaTex.class);
    private static BoaArtSet art;
    private static File modelDir;
    private static File textureDir;
    private static boolean ready;
    private static boolean failed;

    private BoaAssets() {
    }

    public static void bind(File configDir) {
        modelDir = new File(configDir, "models/boa");
        textureDir = new File(configDir, "textures/boa");
    }

    public static boolean ready() {
        return ready;
    }

    public static BoaArtSet art() {
        return art;
    }

    public static Identifier id(BoaTex tex) {
        return IDS.get((Object)tex);
    }

    public static void init() {
        if (ready || failed) {
            return;
        }
        try {
            art = BoaArtSet.build(BoaAssets::open, BoaAssets::skin);
            for (BoaTex tex : BoaTex.values()) {
                BufferedImage image = art.image(tex);
                if (image == null) continue;
                IDS.put(tex, BoaAssets.upload(tex.name().toLowerCase(Locale.ROOT), image));
            }
            ready = true;
        }
        catch (Throwable t) {
            TikiGambleClient.LOGGER.error("[SkyMaxx] could not build the Titanoboa's art", t);
            failed = true;
        }
    }

    public static void reload() {
        Minecraft mc = Minecraft.getInstance();
        for (Identifier id : IDS.values()) {
            try {
                mc.getTextureManager().release(id);
            }
            catch (Throwable throwable) {}
        }
        IDS.clear();
        ready = false;
        failed = false;
        BoaAssets.init();
    }

    private static InputStream open(String name) throws IOException {
        File custom;
        if (modelDir != null && (custom = new File(modelDir, name + ".gltf")).isFile()) {
            TikiGambleClient.LOGGER.info("[SkyMaxx] using custom Titanoboa model {}", (Object)custom);
            return new FileInputStream(custom);
        }
        return BoaAssets.class.getResourceAsStream(String.format(RESOURCE, name));
    }

    private static BufferedImage skin(String name) {
        if (textureDir == null) {
            return null;
        }
        File file = new File(textureDir, name + ".png");
        if (!file.isFile()) {
            return null;
        }
        try {
            return ImageIO.read(file);
        }
        catch (Exception e) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] could not read {}, using the built-in skin", (Object)file, (Object)e);
            return null;
        }
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
        Identifier id = TikiGambleClient.id("boa_" + name);
        Minecraft.getInstance().getTextureManager().register(id, (AbstractTexture)new DynamicTexture(() -> name, image));
        return id;
    }
}

