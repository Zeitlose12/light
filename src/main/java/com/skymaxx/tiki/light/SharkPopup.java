package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

/**
 * Light (test feature): a tiny pop-up for a Great White Shark kill during the Fishing Festival.
 * A strip of water, a fin passes, the shark shoots up and spits out the drop (Megalodon or Shark Tooth).
 * Display only: reads chat and kills the client already has, draws a small movable box. Only while
 * "Debug log" is on.
 */
public final class SharkPopup {
    public static final int W = 84;
    public static final int H = 54;
    private static final int WATER = 44;
    private static final long DROP_END = 4500L;
    private static final long BUST_END = 1500L;
    private static final long WAIT_FOR_DROP = 1500L;

    private static boolean active;
    private static long startMs;
    private static String drop;
    private static boolean soundSplash;
    private static boolean soundReveal;
    private static long pendingKillMs;
    private static long lastDropMs;
    private static LivingEntity pendingBody;
    private static LivingEntity lastBody;
    private static long lastBodyMs;
    private static LivingEntity mob;
    private static ItemStack icon;
    private static boolean iconBuilt;
    private static boolean marsh;

    private SharkPopup() {
    }

    private static boolean enabled() {
        return TikiGambleClient.config != null && TikiGambleClient.config.debug;
    }

    /** A counted Great White Shark kill (own or loot share). */
    public static void onKill(String creature, LivingEntity body) {
        try {
            if (!enabled()) return;
            long now = System.currentTimeMillis();
            if (!"Great White Shark".equals(creature)) return;
            lastBody = body;
            lastBodyMs = now;
            if (now - lastDropMs < 2500L) {
                if (active && mob == null && body != null) mob = MobView.snapshot(body, net.minecraft.world.entity.EntityTypes.DOLPHIN);
                return; // the drop already started the pop-up
            }
            pendingKillMs = now;
            pendingBody = body;
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] shark pop-up failed", t);
        }
    }

    /** RARE DROP / PET DROP lines: Megalodon or Great White Shark Tooth. */
    public static void onChat(Component message, boolean overlay) {
        if (overlay || message == null || !enabled()) return;
        try {
            String s = message.getString().replaceAll("§.", "").trim().toLowerCase(Locale.ROOT);
            if (!s.startsWith("rare drop!") && !s.startsWith("pet drop!")) return;
            String what;
            if (s.contains("megalodon")) what = "Megalodon";
            else if (s.contains("shark tooth")) what = "Shark Tooth";
            else return;
            lastDropMs = System.currentTimeMillis();
            pendingKillMs = 0L;
            long now = System.currentTimeMillis();
            play(what, now - lastBodyMs < 3000L ? lastBody : null);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] shark pop-up chat failed", t);
        }
    }

    /** Megalodon / Shark Tooth that went straight into your inventory or sacks without a chat line. */
    public static void onItemGained(String name) {
        if (name == null || !enabled()) return;
        String s = name.replaceAll("\u00a7.", "").toLowerCase(Locale.ROOT);
        String what = s.contains("megalodon") ? "Megalodon" : (s.contains("shark tooth") ? "Shark Tooth" : null);
        if (what == null) return;
        long now = System.currentTimeMillis();
        if (now - lastDropMs < 5000L) return; // the chat line already started it
        lastDropMs = now;
        pendingKillMs = 0L;
        play(what, now - lastBodyMs < 15000L ? lastBody : null);
    }

    /** Starts the pop-up. {@code what == null} plays the short one without a drop. */
    public static void play(String what, LivingEntity body) {
        start(what, body, false);
    }

    private static void start(String what, LivingEntity body, boolean isMarsh) {
        marsh = isMarsh;
        drop = what;
        mob = MobView.snapshot(body, isMarsh ? net.minecraft.world.entity.EntityTypes.GUARDIAN : net.minecraft.world.entity.EntityTypes.DOLPHIN);
        icon = null;
        startMs = System.currentTimeMillis();
        soundSplash = false;
        soundReveal = false;
        active = true;
    }

    private static void sound(SoundEvent ev, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, vol));
    }

    /** Top-left corner of the box on screen; -1 in the config means "centered over the hotbar". */
    public static int boxX(TikiConfig cfg, int sw) {
        float s = cfg.sharkPopupScale;
        if (cfg.sharkPopupX < 0) return Math.round((sw - W * s) / 2.0f);
        return Math.round(cfg.sharkPopupX * sw);
    }

    public static int boxY(TikiConfig cfg, int sh) {
        float s = cfg.sharkPopupScale;
        if (cfg.sharkPopupY < 0) return Math.round(sh - 62 - H * s);
        return Math.round(cfg.sharkPopupY * sh);
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        try {
            long now = System.currentTimeMillis();
            TikiConfig cfg = TikiGambleClient.config;
            if (cfg == null) return;
            if (pendingKillMs != 0L && now - pendingKillMs > WAIT_FOR_DROP) {
                pendingKillMs = 0L;
                if (enabled() && cfg.sharkPopupNoDrop && !active) play(null, pendingBody);
                pendingBody = null;
            }
            if (!active) return;
            long t = now - startMs;
            if (t > (drop == null ? BUST_END : DROP_END)) {
                active = false;
                mob = null;
                icon = null;
                return;
            }
            if (drop != null && (icon == null || iconBuilt)) {
                ItemStack found = findIcon(drop);
                if (found != null) {
                    icon = found;
                    iconBuilt = false;
                } else if (icon == null) {
                    icon = buildIcon(drop);
                    iconBuilt = icon != null;
                }
            }
            if (drop != null && t >= 900 && !soundSplash) {
                soundSplash = true;
                sound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.2f, 0.35f);
            }
            if (drop == null && t >= 400 && !soundSplash) {
                soundSplash = true;
                sound(SoundEvents.GENERIC_SPLASH, 1.3f, 0.25f);
            }
            if (drop != null && t >= 1150 && !soundReveal) {
                soundReveal = true;
                sound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 0.4f);
            }
            int sw = ctx.guiWidth();
            int sh = ctx.guiHeight();
            draw(ctx, Minecraft.getInstance().font, boxX(cfg, sw), boxY(cfg, sh), cfg.sharkPopupScale, t, drop, mob, icon, marsh);
        } catch (Throwable e) {
            active = false;
            TikiGambleClient.LOGGER.warn("[Light] shark pop-up render failed", e);
        }
    }

    /** 0..1 how far the shark is out of the water at time t. */
    private static float jump(long t, boolean bust) {
        long t0 = bust ? 400 : 900;
        long up = bust ? 350 : 300;
        long down = bust ? 400 : 450;
        long hold = bust ? 50 : 150;
        long st = t - t0;
        if (st < 0) return 0;
        float r;
        if (st < up) r = clamp(st / (float) up);
        else if (st < up + hold) r = 1.0f;
        else r = 1.0f - clamp((st - up - hold) / (float) down);
        return bust ? r * 0.55f : r;
    }

    private static int argb(float a, int rgb) {
        int al = Math.max(0, Math.min(255, Math.round(a * 255.0f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    /** Draws one frame. Also used by the move screen as a looping preview. */
    public static void draw(GuiGraphicsExtractor ctx, Font font, int x, int y, float scale, long t, String what) {
        draw(ctx, font, x, y, scale, t, what, null, null, false);
    }

    private static final String MEGALODON_SKIN = "eyJ0aW1lc3RhbXAiOjE1MzQxMTI1MjMxNzgsInByb2ZpbGVJZCI6IjVjZjYyNWJhOGY4ZTQwNjliY2ZlYWY1ZmJiMzVhM2Y0IiwicHJvZmlsZU5hbWUiOiJMZWZ0U2hhcmsiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2E5NGFlNDMzYjMwMWM3ZmI3YzY4Y2JhNjI1YjBiZDM2YjBiMTQxOTBmMjBlMzRhN2M4ZWUwZDlkZTA2ZDUzYjkifX19";
    private static final String TOOTH_MODEL = "hypixel_skyblock:item/community_center/mayor/marina/great_white_shark_tooth";
    private static ItemStack head(String skin, String uuid) {
        com.google.common.collect.Multimap<String, com.mojang.authlib.properties.Property> m = com.google.common.collect.LinkedHashMultimap.create();
        m.put("textures", new com.mojang.authlib.properties.Property("textures", skin));
        com.mojang.authlib.GameProfile gp = new com.mojang.authlib.GameProfile(java.util.UUID.fromString(uuid), "", new com.mojang.authlib.properties.PropertyMap(m));
        ItemStack head = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
        head.set(net.minecraft.core.component.DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createResolved(gp));
        return head;
    }

    private static ItemStack modelItem(String model) {
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.parse(model);
        Object m = Minecraft.getInstance().getModelManager().getItemModel(id);
        if (m == null || m instanceof net.minecraft.client.renderer.item.MissingItemModel) return null;
        ItemStack s = new ItemStack(net.minecraft.world.item.Items.PAPER);
        s.set(net.minecraft.core.component.DataComponents.ITEM_MODEL, id);
        return s;
    }

    /** How Hypixel shows the drop (pet head skin / item model, from the NEU repo) - for tests or before it is in your inventory. */
    private static ItemStack buildIcon(String what) {
        try {
            if ("Megalodon".equals(what)) {
                com.google.common.collect.Multimap<String, com.mojang.authlib.properties.Property> m = com.google.common.collect.LinkedHashMultimap.create();
                m.put("textures", new com.mojang.authlib.properties.Property("textures", MEGALODON_SKIN));
                com.mojang.authlib.GameProfile gp = new com.mojang.authlib.GameProfile(java.util.UUID.fromString("3d43eb05-8b93-318e-acee-4ad3e3fe9019"), "", new com.mojang.authlib.properties.PropertyMap(m));
                ItemStack head = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
                head.set(net.minecraft.core.component.DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createResolved(gp));
                return head;
            }
            net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.parse(TOOTH_MODEL);
            Object model = Minecraft.getInstance().getModelManager().getItemModel(id);
            if (model == null || model instanceof net.minecraft.client.renderer.item.MissingItemModel) return null;
            ItemStack tooth = new ItemStack(net.minecraft.world.item.Items.PAPER);
            tooth.set(net.minecraft.core.component.DataComponents.ITEM_MODEL, id);
            return tooth;
        } catch (Throwable t) {
            return null;
        }
    }

    /** The real drop from your inventory (Megalodon pet item / Shark Tooth), if it is there already. */
    private static ItemStack findIcon(String what) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return null;
            String key = what.toLowerCase(Locale.ROOT);
            var inv = mc.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s == null || s.isEmpty()) continue;
                String n = s.getHoverName().getString().replaceAll("\u00a7.", "").toLowerCase(Locale.ROOT);
                if (n.contains(key)) return s.copy();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void draw(GuiGraphicsExtractor ctx, Font font, int x, int y, float scale, long t, String what, LivingEntity real, ItemStack item, boolean marsh) {
        boolean bust = what == null;
        long end = bust ? BUST_END : DROP_END;
        float a = clamp(t / 200.0f) * clamp((end - t) / 300.0f);
        if (a <= 0.0f) return;
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(scale, scale);

        // small glass box (darker), the real shark behind the water, then the water strip
        ctx.fill(0, 0, W, H, argb(0.65f * a, 0x0A1420));
        long t0r = bust ? 400 : 900;
        boolean useMob = real != null && MobView.available() && t >= t0r;
        if (useMob) {
            float riseM = jump(t, bust);
            if (riseM > 0.01f) {
                pose.popMatrix();
                int px = Math.round(30 * scale);
                int mcx = x + Math.round((W / 2.0f) * scale);
                int mcy = y + Math.round((WATER + 16 - 30 * riseM) * scale);
                ctx.nextStratum();
                MobView.draw(ctx, real, mcx, mcy, px, 0.8f);
                ctx.nextStratum();
                pose.pushMatrix();
                pose.translate((float) x, (float) y);
                pose.scale(scale, scale);
            }
        }
        ctx.fill(0, WATER, W, H, argb(0.95f * a, marsh ? 0x1E4A3C : 0x2D6FB0));
        ctx.fill(0, WATER - 1, W, WATER, argb(a, marsh ? 0x4FA88A : 0xBFE3FF));
        for (int i = 0; i < 6; i++) {
            int wx = (int) ((i * 17 + t / 40) % W);
            ctx.fill(wx, WATER + 3, Math.min(W, wx + 4), WATER + 4, argb(0.5f * a, marsh ? 0x6FC4A0 : 0x8CC4F0));
        }

        int cx = W / 2 - 2;
        long finIn = bust ? 400 : 500;
        // fin travelling to the middle
        if (t < (bust ? 400 : 900)) {
            float p = clamp(t / (float) finIn);
            int fx = (int) (6 + (cx - 6) * p);
            ctx.fill(fx, WATER - 6, fx + 4, WATER - 1, argb(a, 0x55606B));
            ctx.fill(fx + 2, WATER - 9, fx + 4, WATER - 6, argb(a, 0x55606B));
            if (p >= 1.0f && ((t / 120) % 2 == 0)) {
                ctx.fill(fx - 5, WATER - 2, fx - 3, WATER - 1, argb(a, 0xBFE3FF));
                ctx.fill(fx + 7, WATER - 2, fx + 9, WATER - 1, argb(a, 0xBFE3FF));
            }
        } else {
            // shark shoots up and falls back
            long t0 = bust ? 400 : 900;
            long up = bust ? 350 : 300;
            long down = bust ? 400 : 450;
            int maxH = bust ? 14 : 26;
            float rise;
            long st = t - t0;
            if (st < up) rise = clamp(st / (float) up);
            else if (st < up + (bust ? 50 : 150)) rise = 1.0f;
            else rise = 1.0f - clamp((st - up - (bust ? 50 : 150)) / (float) down);
            int hgt = Math.round(maxH * rise);
            if (hgt > 0 && !useMob) {
                int top = WATER - 1 - hgt;
                ctx.fill(cx - 2, top, cx + 6, WATER - 1, argb(a, 0x6B7782));
                ctx.fill(cx + 3, top + 2, cx + 6, WATER - 1, argb(a, 0xE8ECEF));
                ctx.fill(cx - 1, top - 2, cx + 5, top, argb(a, 0x6B7782));
                ctx.fill(cx - 1, top + 3, cx + 1, top + 5, argb(a, 0x111111));
                ctx.fill(cx - 4, top + 8, cx - 2, top + 12, argb(a, 0x55606B));
                if (rise > 0.6f && st < up + 200) {
                    ctx.fill(cx + 3, top - 1, cx + 6, top, argb(a, 0xFFFFFF));
                }
            }
            if (st < 260 || (st > up + 100 && st < up + down + 200)) {
                int k = (int) ((st / 60) % 3);
                ctx.fill(cx - 8 - k, WATER - 3 - k, cx - 6 - k, WATER - 1 - k, argb(a, 0xBFE3FF));
                ctx.fill(cx + 10 + k, WATER - 3 - k, cx + 12 + k, WATER - 1 - k, argb(a, 0xBFE3FF));
                ctx.fill(cx - 4, WATER - 2, cx - 3, WATER - 1, argb(a, 0xFFFFFF));
            }
        }

        // the drop: spat out, floats, name on top
        if (!bust && t >= 1150) {
            float p = clamp((t - 1150) / 250.0f);
            int iy = (int) (WATER - 12 - (WATER - 12 - 20) * p);
            int bob = t > 1400 ? (int) Math.round(Math.sin(t / 180.0) * 1.0) : 0;
            int ix = W / 2 - 4;
            boolean mega = "Megalodon".equals(what);
            int main = mega ? 0xFFCC33 : 0xF3F3F3;
            int light = mega ? 0xFFF3C4 : 0xFFFFFF;
            int iyb = iy + bob;
            if (item != null && !item.isEmpty()) {
                pose.pushMatrix();
                pose.translate(ix + 4.0f, iyb + 2.0f);
                ctx.item(item, -8, -8);
                pose.popMatrix();
            } else {
                ctx.fill(ix, iyb, ix + 8, iyb + 8, argb(a, main));
                ctx.fill(ix + 2, iyb + 2, ix + 5, iyb + 5, argb(a, light));
            }
            if (((t / 150) % 2) == 0) {
                ctx.fill(ix - 4, iyb - 2, ix - 3, iyb - 1, argb(a, 0xFFFFFF));
                ctx.fill(ix + 11, iyb + 6, ix + 12, iyb + 7, argb(a, 0xFFFFFF));
            } else {
                ctx.fill(ix + 10, iyb - 3, ix + 11, iyb - 2, argb(a, 0xFFFFFF));
                ctx.fill(ix - 3, iyb + 7, ix - 2, iyb + 8, argb(a, 0xFFFFFF));
            }
            float ta = clamp((t - 1300) / 250.0f) * a;
            if (ta > 0.02f) {
                String label = what + "!";
                int col = mega ? 0xFFCC33 : (marsh ? 0x7F9CFF : 0xE8F4FF);
                Component lc = Component.literal(label).withStyle(net.minecraft.ChatFormatting.BOLD);
                float fit = Math.min(1.0f, (W - 4) / (float) Math.max(1, font.width(lc)));
                pose.pushMatrix();
                pose.translate(W / 2.0f, 2.0f);
                pose.scale(fit, fit);
                ctx.centeredText(font, lc, 0, 0, argb(ta, col));
                pose.popMatrix();
            }
        }
        pose.popMatrix();
    }
}
