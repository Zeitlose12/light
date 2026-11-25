package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Light: draws a real mob model in the HUD, like the player in the inventory.
 * For a real kill it copies the mob that just died (its type and what it wears), so it looks exactly
 * like the one on Hypixel. The copy is never added to the world. On the first error the 3D view
 * switches itself off until the game restarts; the animations then simply run without the mob.
 */
public final class MobView {
    private static boolean broken;
    private static final java.util.Set<Object> LOGGED = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private static int nextId;

    private MobView() {
    }

    /** Turns the 3D view back on after a failure (a new jar or a fix in the same session). */
    public static void reset() {
        broken = false;
        dragonBroken = false;
    }

    public static boolean available() {
        return !broken;
    }

    /** A stand-alone copy of {@code body}, or a fresh {@code fallback} when there is no body. */
    public static LivingEntity snapshot(LivingEntity body, EntityType<?> fallback) {
        if (broken) return null;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return null;
            EntityType<?> type = body != null ? body.getType() : fallback;
            if (type == null) return null;
            Entity e = type.create(mc.level, EntitySpawnReason.LOAD);
            if (!(e instanceof LivingEntity) && fallback != null && type != fallback) e = fallback.create(mc.level, EntitySpawnReason.LOAD);
            if (!(e instanceof LivingEntity copy)) return null;
            // a copy that is never added to the world has no id yet - the renderer needs one
            copy.setId(-100000 - (nextId++ & 0xFFFF));
            if (body != null) {
                for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                    try {
                        ItemStack s = body.getItemBySlot(slot);
                        if (s != null && !s.isEmpty()) copy.setItemSlot(slot, s.copy());
                    } catch (Throwable ignored) {
                    }
                }
                copy.setInvisible(body.isInvisible());
            }
            // the copy is not in the world; put it where you stand so the game lights it like you
            // (at 0,0,0 it is usually inside blocks and renders pitch black)
            if (mc.player != null) copy.setPos(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
            copy.hurtTime = 0;
            copy.deathTime = 0;
            return copy;
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    /** Body height in blocks (for scaling), at least 0.5. */
    public static float height(LivingEntity e) {
        try {
            return Math.max(0.5f, e.getBbHeight());
        } catch (Throwable t) {
            return 1.0f;
        }
    }

    /**
     * Draws {@code e} with its middle at (cx, cy), {@code px} GUI pixels tall. {@code turn} turns it
     * left/right (-1..1).
     */
    public static void draw(GuiGraphicsExtractor ctx, LivingEntity e, int cx, int cy, int px, float turn) {
        draw(ctx, e, cx, cy, px, turn, -0.15f);
    }

    /** {@code look}: how far it looks down (positive) or up (negative), in body heights. */
    public static void draw(GuiGraphicsExtractor ctx, LivingEntity e, int cx, int cy, int px, float turn, float look) {
        if (broken || e == null || px < 4) return;
        try {
            float h = height(e);
            int size = Math.max(1, Math.round(px / h));
            // generous box: spikes, arms and turned bodies must not be cut off
            int half = Math.round(px * 0.95f) + 10;
            int halfW = Math.round(Math.max(px, e.getBbWidth() * size) * 1.0f) + 12;
            if (TikiGambleClient.config != null && TikiGambleClient.config.debug && LOGGED.add(e)) {
                TikiGambleClient.LOGGER.info("[Light] 3D mob: {} at {},{} size {} invisible={}", e.getType(), cx, cy, size, e.isInvisible());
                TikiGambleClient.chat("\u00a78[Light] 3D mob: " + e.getType().toShortString() + (e.isInvisible() ? " (invisible)" : ""));
            }
            InventoryScreen.extractEntityInInventoryFollowsMouse(ctx, cx - halfW, cy - half, cx + halfW, cy + half,
                size, 0.0625f, cx + turn * 60.0f, cy + px * look, e);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Draws {@code e} with a full pose: yaw (degrees, -90 = facing right), the whole body tilted forward
     * (bodyPitch degrees, negative = leans toward you) and the head looking down (headPitch degrees, positive).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void drawPosed(GuiGraphicsExtractor ctx, LivingEntity e, int cx, int cy, int px, float yaw, float bodyPitch, float headPitch) {
        if (broken || e == null || px < 4) return;
        try {
            float h = height(e);
            int size = Math.max(1, Math.round(px / h));
            int half = Math.round(px * 0.95f) + 10;
            int halfW = Math.round(Math.max(px, e.getBbWidth() * size) * 1.0f) + 12;
            if (TikiGambleClient.config != null && TikiGambleClient.config.debug && LOGGED.add(e)) {
                TikiGambleClient.chat("\u00a78[Light] 3D mob: " + e.getType().toShortString() + (e.isInvisible() ? " (invisible)" : ""));
            }
            net.minecraft.client.renderer.entity.EntityRenderer r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
            net.minecraft.client.renderer.entity.state.EntityRenderState st = r.createRenderState(e, 1.0f);
            st.shadowPieces.clear();
            st.outlineColor = 0;
            if (st instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState ls) {
                ls.bodyRot = 180.0f + yaw;
                ls.yRot = yaw;
                ls.xRot = headPitch;
                ls.boundingBoxWidth /= ls.scale;
                ls.boundingBoxHeight /= ls.scale;
                ls.scale = 1.0f;
            }
            org.joml.Vector3f tr = new org.joml.Vector3f(0.0f, st.boundingBoxHeight / 2.0f + 0.0625f, 0.0f);
            org.joml.Quaternionf tilt = new org.joml.Quaternionf().rotateX((float) Math.toRadians(bodyPitch));
            org.joml.Quaternionf rot = new org.joml.Quaternionf().rotateZ((float) Math.PI).mul(tilt);
            ctx.entity(st, (float) size, tr, rot, tilt, cx - halfW, cy - half, cx + halfW, cy + half);
        } catch (Throwable t) {
            fail(t);
        }
    }

    private static boolean dragonBroken;
    /** Debug fine-tuning for the dragon view (/light dragonfix yaw pitch y). */
    public static float dragonYawFix = 180.0f;
    public static float dragonPitchFix = 0.0f;
    public static float dragonYFix = 1.5f;

    /**
     * Draws an Ender Dragon (the Reindrake) {@code span} GUI pixels wide with its body at (cx, cy):
     * {@code yaw} like {@link #drawPosed} (-90 = flies to the right, 90 = to the left), {@code pitch} nose
     * down (positive), {@code flap} wing cycles, {@code sway} how much neck and tail swing, {@code death}
     * 0..200 = the vanilla death (light rays, dissolving). On an error only the dragon is switched off.
     */
    public static void drawDragon(GuiGraphicsExtractor ctx, LivingEntity e, int cx, int cy, int span, float yaw, float pitch,
                                  float flap, float sway, float t, float death) {
        if (broken || dragonBroken || e == null || span < 8) return;
        try {
            net.minecraft.client.renderer.entity.EntityRenderer r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
            @SuppressWarnings("unchecked")
            net.minecraft.client.renderer.entity.state.EntityRenderState st = ((net.minecraft.client.renderer.entity.EntityRenderer<net.minecraft.world.entity.Entity, ?>) r).createRenderState(e, 1.0f);
            st.shadowPieces.clear();
            st.outlineColor = 0;
            if (st instanceof net.minecraft.client.renderer.entity.state.EnderDragonRenderState d) {
                d.flapTime = flap;
                d.deathTime = death;
                d.hasRedOverlay = false;
                d.beamOffset = null;
                d.isLandingOrTakingOff = false;
                d.isSitting = false;
                d.partialTicks = 0.0f;
                // a short flight history: the neck and tail swing gently behind the body
                int len = net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory.LENGTH;
                for (int i = len - 1; i >= 0; i--) {
                    float tt = t - i * 0.05f;
                    double y = Math.sin(tt * 2.2f) * sway;
                    float yr = yaw + dragonYawFix + (float) Math.sin(tt * 1.4f) * sway * 6.0f;
                    d.flightHistory.record(y, yr);
                }
            }
            float size = span / 14.0f;
            int halfW = Math.round(span * 0.8f) + 16;
            int halfH = Math.round(span * 0.55f) + 16;
            org.joml.Vector3f tr = new org.joml.Vector3f(0.0f, dragonYFix, 0.0f);
            org.joml.Quaternionf tilt = new org.joml.Quaternionf().rotateX((float) Math.toRadians(pitch + dragonPitchFix));
            org.joml.Quaternionf rot = new org.joml.Quaternionf().rotateZ((float) Math.PI).mul(tilt);
            ctx.entity(st, size, tr, rot, tilt, cx - halfW, cy - halfH, cx + halfW, cy + halfH);
        } catch (Throwable t2) {
            dragonBroken = true;
            TikiGambleClient.LOGGER.warn("[Light] dragon view switched off", t2);
            try {
                if (TikiGambleClient.config != null && TikiGambleClient.config.debug) {
                    TikiGambleClient.chat("\u00a7c[Light] dragon view switched off: " + t2);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void fail(Throwable t) {
        broken = true;
        TikiGambleClient.LOGGER.warn("[Light] 3D mob view switched off", t);
        try {
            if (TikiGambleClient.config != null && TikiGambleClient.config.debug) {
                TikiGambleClient.chat("\u00a7c[Light] 3D mob view switched off: " + t);
            }
        } catch (Throwable ignored) {
        }
    }
}
