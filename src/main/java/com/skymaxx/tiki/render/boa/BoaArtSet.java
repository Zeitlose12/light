package com.skymaxx.tiki.render.boa;

import com.skymaxx.tiki.boa.BoaPrize;
import com.skymaxx.tiki.render.art.GlowArt;
import com.skymaxx.tiki.render.art.LeafArt;
import com.skymaxx.tiki.render.boa.BoaArt;
import com.skymaxx.tiki.render.boa.BoaTex;
import com.skymaxx.tiki.render.model.GltfLoader;
import com.skymaxx.tiki.render.model.TikiModel;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

public final class BoaArtSet {
    public static final String HEAD_MODEL = "titanoboahead";
    public static final String BODY_MODEL = "titanoboabody";
    private final Map<BoaTex, BufferedImage> images = new EnumMap<BoaTex, BufferedImage>(BoaTex.class);
    private final Map<BoaPrize, TikiModel> items = new EnumMap<BoaPrize, TikiModel>(BoaPrize.class);
    private final Map<BoaPrize, Integer> tints = new EnumMap<BoaPrize, Integer>(BoaPrize.class);
    private TikiModel head;
    private TikiModel body;

    private BoaArtSet() {
    }

    public static BoaArtSet build(ModelSource models, SkinSource skins) throws IOException {
        int v;
        BoaArtSet set = new BoaArtSet();
        set.head = BoaArtSet.load(models, skins, HEAD_MODEL);
        set.body = BoaArtSet.load(models, skins, BODY_MODEL);
        set.images.put(BoaTex.HEAD, set.head.skin());
        set.images.put(BoaTex.BODY, set.body.skin());
        for (BoaPrize prize : BoaPrize.values()) {
            if (prize.model() == null) continue;
            TikiModel model = BoaArtSet.load(models, skins, prize.model());
            set.items.put(prize, model);
            set.images.put(BoaTex.skinOf(prize), model.skin());
            set.tints.put(prize, BoaArtSet.faceTint(model));
        }
        set.images.put(BoaTex.CYLINDER, BoaArt.cylinder());
        set.images.put(BoaTex.CYLINDER_SHADE, BoaArt.cylinderShade());
        set.images.put(BoaTex.CYLINDER_FLUTES, BoaArt.cylinderFlutes());
        set.images.put(BoaTex.CHAMBER, BoaArt.chamber());
        set.images.put(BoaTex.BASE, BoaArt.cartridgeBase(false));
        set.images.put(BoaTex.BASE_GOLD, BoaArt.cartridgeBase(true));
        set.images.put(BoaTex.BULLET, BoaArt.bullet(false));
        set.images.put(BoaTex.BULLET_GOLD, BoaArt.bullet(true));
        set.images.put(BoaTex.BULLET_BACK, BoaArt.bulletBack());
        set.images.put(BoaTex.SCOPE_MASK, BoaArt.scopeMask());
        set.images.put(BoaTex.RETICLE, BoaArt.reticle());
        set.images.put(BoaTex.SKY_DUSK, BoaArt.skyDusk());
        set.images.put(BoaTex.SKY_NIGHT, BoaArt.skyNight());
        set.images.put(BoaTex.SKY_MIST, BoaArt.skyMist());
        set.images.put(BoaTex.SKY_GOLDEN, BoaArt.skyGolden());
        set.images.put(BoaTex.REEDS, BoaArt.reeds());
        set.images.put(BoaTex.TRI, BoaArt.triangle());
        set.images.put(BoaTex.DISC, BoaArt.disc());
        set.images.put(BoaTex.SPLASH, BoaArt.splash());
        set.images.put(BoaTex.PUFF, BoaArt.puff());
        set.images.put(BoaTex.FIRE, BoaArt.fire());
        set.images.put(BoaTex.RING, BoaArt.ring());
        set.images.put(BoaTex.SOFT_RING, BoaArt.softRing());
        set.images.put(BoaTex.SPARK, BoaArt.spark());
        set.images.put(BoaTex.GLOW, GlowArt.halo());
        set.images.put(BoaTex.RIPPLE, BoaArt.ripple());
        for (v = 0; v < 3; ++v) {
            set.images.put(BoaTex.smoke(v), BoaArt.smoke(v));
        }
        set.images.put(BoaTex.CROSS, BoaArt.cross());
        set.images.put(BoaTex.WHITE, BoaArt.white());
        set.images.put(BoaTex.STAMP_SHED, BoaArt.stamp(set.items.get(BoaPrize.SHED).skin(), 1517L));
        set.images.put(BoaTex.STAMP_SNAKE_EYES, BoaArt.stamp(set.items.get(BoaPrize.SNAKE_EYES).skin(), 24293L));
        set.images.put(BoaTex.STAMP_AQUAMARINE, BoaArt.stamp(set.items.get(BoaPrize.AQUAMARINE).skin(), 43274L));
        for (v = 0; v < 6; ++v) {
            BufferedImage leaf = LeafArt.leaf(v);
            set.images.put(BoaTex.leaf(v, false), leaf);
            set.images.put(BoaTex.leaf(v, true), BoaArtSet.mirror(leaf));
        }
        return set;
    }

    private static BufferedImage mirror(BufferedImage in) {
        int w = in.getWidth();
        int h = in.getHeight();
        BufferedImage out = new BufferedImage(w, h, 2);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                out.setRGB(w - 1 - x, y, in.getRGB(x, y));
            }
        }
        return out;
    }

    private static TikiModel load(ModelSource models, SkinSource skins, String name) throws IOException {
        try (InputStream in = models.open(name);){
            if (in == null) {
                throw new IOException("missing Titanoboa model " + name);
            }
            TikiModel tikiModel = GltfLoader.load(in, skins == null ? null : skins.skin(name), false);
            return tikiModel;
        }
    }

    public BufferedImage image(BoaTex tex) {
        return this.images.get((Object)tex);
    }

    public int width(BoaTex tex) {
        BufferedImage img = this.images.get((Object)tex);
        return img == null ? 1 : img.getWidth();
    }

    public int height(BoaTex tex) {
        BufferedImage img = this.images.get((Object)tex);
        return img == null ? 1 : img.getHeight();
    }

    public TikiModel head() {
        return this.head;
    }

    public TikiModel body() {
        return this.body;
    }

    public TikiModel item(BoaPrize prize) {
        return this.items.get(prize);
    }

    public int tint(BoaPrize prize) {
        Integer tint = this.tints.get(prize);
        return tint == null || tint == 0 ? -13238 : tint;
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

    public static interface ModelSource {
        public InputStream open(String var1) throws IOException;
    }

    public static interface SkinSource {
        public BufferedImage skin(String var1);
    }
}

