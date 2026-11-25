package com.skymaxx.tiki.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.detect.DropLines;
import com.skymaxx.tiki.detect.SeaCreatureDetector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class HotspotText {
    private static final int SEARCH_DEPTH = 10;
    private static final float MAX_SCALE = 0.08f;
    private static final float FILL = 0.85f;
    private static final double LIFT = 0.02;
    private static final int LINE_HEIGHT = 10;
    private static final double RANGE = 64.0;
    private static final double RING_SEARCH = 10.0;
    private static final double RING_HEIGHT = 2.5;
    private static final double BIN = 0.1;
    private static final double RING_MIN = 1.0;
    private static final int RING_SAMPLES = 16;
    private static final long RING_WAIT_MS = 3000L;
    private static final double FALLBACK_RADIUS = 2.0;
    private static final String[][] SHORT = new String[][]{{"Sea Creature Chance", "SCC"}, {"Double Hook Chance", "DHC"}, {"Trophy Fish Chance", "TFC"}, {"Treasure Chance", "TC"}, {"Trophy Chance", "TFC"}, {"Fishing Speed", "FS"}};
    private static final Map<Integer, Spot> SPOTS = new HashMap<Integer, Spot>();
    private static final Set<Integer> HIDDEN = new HashSet<Integer>();

    private HotspotText() {
    }

    public static void tick(Minecraft mc, TikiConfig cfg) {
        HIDDEN.clear();
        if (!cfg.hotspotTextOnWater || mc.level == null || mc.player == null || cfg.requireSkyblock && !SeaCreatureDetector.onHypixel()) {
            SPOTS.clear();
            return;
        }
        long now = System.currentTimeMillis();
        ArrayList<ArmorStand> named = new ArrayList<ArmorStand>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            ArmorStand stand;
            if (!(entity instanceof ArmorStand) || (stand = (ArmorStand)entity).getCustomName() == null) continue;
            named.add(stand);
        }
        for (Spot spot2 : SPOTS.values()) {
            spot2.seen = false;
        }
        for (ArmorStand hotspot : named) {
            if (!HotspotText.isHotspot(hotspot)) continue;
            Spot spot3 = SPOTS.computeIfAbsent(hotspot.getId(), id -> new Spot(hotspot.getX(), hotspot.getZ(), now));
            spot3.seen = true;
            if (Double.isNaN(spot3.surface)) {
                spot3.surface = HotspotText.surfaceUnder((Level)mc.level, hotspot.getX(), hotspot.getY(), hotspot.getZ());
            }
            ArrayList<ArmorStand> under = new ArrayList<ArmorStand>();
            for (ArmorStand other : named) {
                double drop = hotspot.getY() - other.getY();
                if (other == hotspot || HotspotText.isHotspot(other) || !(Math.abs(other.getX() - hotspot.getX()) < 0.05) || !(Math.abs(other.getZ() - hotspot.getZ()) < 0.05) || !(drop > 0.0) || !(drop <= 2.5)) continue;
                under.add(other);
            }
            under.sort((a, b) -> Double.compare(b.getY(), a.getY()));
            ArrayList<Component> lines = new ArrayList<Component>();
            lines.add(hotspot.getCustomName());
            spot3.stands.clear();
            spot3.stands.add(hotspot.getId());
            for (ArmorStand line : under) {
                lines.add(HotspotText.shorten(line.getCustomName()));
                spot3.stands.add(line.getId());
            }
            spot3.lines = lines;
            if (Double.isNaN(spot3.radius) && now - spot3.firstSeenMs > 3000L) {
                spot3.radius = 2.0;
            }
            if (!spot3.ready()) continue;
            HIDDEN.addAll(spot3.stands);
        }
        SPOTS.values().removeIf(spot -> !spot.seen);
    }

    public static void onParticle(double x, double y, double z) {
        if (SPOTS.isEmpty()) {
            return;
        }
        for (Spot spot : SPOTS.values()) {
            double distance;
            if (!Double.isNaN(spot.radius) || Double.isNaN(spot.surface) || Math.abs(y - spot.surface) > 2.5 || (distance = Math.hypot(x - spot.x, z - spot.z)) < 1.0 || distance > 10.0) continue;
            int n = (int)(distance / 0.1);
            spot.tally[n] = spot.tally[n] + 1;
            HotspotText.measure(spot);
        }
    }

    private static void measure(Spot spot) {
        int best = -1;
        int bestCount = 0;
        int total = 0;
        for (int i = 0; i < spot.tally.length; ++i) {
            total += spot.tally[i];
            int around = spot.tally[i] + (i > 0 ? spot.tally[i - 1] : 0) + (i + 1 < spot.tally.length ? spot.tally[i + 1] : 0);
            if (around <= bestCount) continue;
            bestCount = around;
            best = i;
        }
        if (best >= 0 && bestCount >= 16 && bestCount * 4 >= total) {
            spot.radius = Math.max(1.0, (double)(best - 1) * 0.1);
        }
    }

    private static boolean isHotspot(ArmorStand stand) {
        Component name = stand.getCustomName();
        return name != null && DropLines.stripFormatting(name.getString()).trim().toUpperCase(Locale.ROOT).equals("HOTSPOT");
    }

    public static Component shorten(Component line) {
        if (line == null) {
            return Component.empty();
        }
        StringBuilder text = new StringBuilder();
        ArrayList<Style> styles = new ArrayList<Style>();
        line.visit((style, part) -> {
            text.append(part);
            for (int i = 0; i < part.length(); ++i) {
                styles.add(style);
            }
            return Optional.empty();
        }, Style.EMPTY);
        boolean changed = false;
        for (String[] pair : SHORT) {
            int at;
            while ((at = text.toString().toLowerCase(Locale.ROOT).indexOf(pair[0].toLowerCase(Locale.ROOT))) >= 0) {
                Style style2 = (Style)styles.get(at);
                text.replace(at, at + pair[0].length(), pair[1]);
                styles.subList(at, at + pair[0].length()).clear();
                for (int i = 0; i < pair[1].length(); ++i) {
                    styles.add(at, style2);
                }
                changed = true;
            }
        }
        if (!changed) {
            return line;
        }
        MutableComponent out = Component.empty();
        int from = 0;
        for (int i = 1; i <= text.length(); ++i) {
            if (i != text.length() && ((Style)styles.get(i)).equals(styles.get(from))) continue;
            out.append((Component)Component.literal((String)text.substring(from, i)).withStyle((Style)styles.get(from)));
            from = i;
        }
        return out;
    }

    private static double surfaceUnder(Level level, double x, double y, double z) {
        int top;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bx = Mth.floor((double)x);
        int bz = Mth.floor((double)z);
        for (int by = top = Mth.floor((double)y); by >= top - 10; --by) {
            int up;
            if (level.getFluidState((BlockPos)pos.set(bx, by, bz)).isEmpty()) continue;
            for (up = by; up < top + 4 && !level.getFluidState((BlockPos)pos.set(bx, up + 1, bz)).isEmpty(); ++up) {
            }
            FluidState fluid = level.getFluidState((BlockPos)pos.set(bx, up, bz));
            return (float)up + fluid.getHeight((BlockGetter)level, (BlockPos)pos);
        }
        return Double.NaN;
    }

    public static boolean hides(Entity entity) {
        return !HIDDEN.isEmpty() && HIDDEN.contains(entity.getId());
    }

    public static void submit(LevelRenderContext ctx) {
        if (SPOTS.isEmpty()) {
            return;
        }
        CameraRenderState camera = ctx.levelState().cameraRenderState;
        if (camera == null || camera.pos == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        PoseStack pose = ctx.poseStack();
        SubmitNodeCollector out = ctx.submitNodeCollector();
        float yaw = camera.yRot * ((float)Math.PI / 180);
        float fx = -Mth.sin((double)yaw);
        float fz = Mth.cos((double)yaw);
        Quaternionf turn = new Quaternionf().setFromNormalized((Matrix3fc)new Matrix3f((Vector3fc)new Vector3f(-fz, 0.0f, fx), (Vector3fc)new Vector3f(fx, 0.0f, fz), (Vector3fc)new Vector3f(0.0f, 1.0f, 0.0f)));
        for (Spot spot : SPOTS.values()) {
            double dz;
            double dy;
            double dx;
            if (!spot.ready() || (dx = spot.x - camera.pos.x) * dx + (dy = spot.surface + 0.02 - camera.pos.y) * dy + (dz = spot.z - camera.pos.z) * dz > 4096.0) continue;
            ArrayList<FormattedCharSequence> lines = new ArrayList<FormattedCharSequence>(spot.lines.size());
            int widest = 1;
            for (Component line : spot.lines) {
                FormattedCharSequence text = line.getVisualOrderText();
                lines.add(text);
                widest = Math.max(widest, font.width(text));
            }
            float height = lines.size() * 10 - 1;
            float halfDiagonal = (float)Math.sqrt((float)(widest * widest) + height * height) * 0.5f;
            float scale = Math.min(0.08f, (float)(spot.radius * (double)0.85f) / halfDiagonal);
            pose.pushPose();
            pose.translate(dx, dy, dz);
            pose.mulPose((Quaternionfc)turn);
            pose.scale(scale, -scale, scale);
            float y = -height / 2.0f;
            for (FormattedCharSequence text : lines) {
                out.submitText(pose, (float)(-font.width(text)) / 2.0f, y, text, false, Font.DisplayMode.POLYGON_OFFSET, 0xF000F0, -1, 0, 0);
                y += 10.0f;
            }
            pose.popPose();
        }
    }

    public static void clear() {
        SPOTS.clear();
        HIDDEN.clear();
    }

    private static final class Spot {
        final double x;
        final double z;
        double surface = Double.NaN;
        final long firstSeenMs;
        List<Component> lines = List.of();
        final Set<Integer> stands = new HashSet<Integer>();
        final int[] tally = new int[101];
        double radius = Double.NaN;
        boolean seen;

        Spot(double x, double z, long now) {
            this.x = x;
            this.z = z;
            this.firstSeenMs = now;
        }

        boolean ready() {
            return !Double.isNaN(this.surface) && !Double.isNaN(this.radius);
        }
    }
}

