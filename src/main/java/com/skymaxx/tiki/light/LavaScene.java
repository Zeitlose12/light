package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

/**
 * Light (test feature): Lava Gambling - a full-screen scene after a Lord Jawbus, Thunder, Ragnarok or
 * Plhlegblast kill. The real mob rises out of the lava and a game of chance runs until your real drop is
 * known (RARE DROP line or the [Sacks] message): slot reels (Jawbus), storm roulette (Thunder), the wheel of
 * the end times (Ragnarok), popping bubbles (Plhlegblast). It always lands on what you really got - never a
 * fake win. Then the mob bursts and the result is shown.
 * Display only: reads chat and the mob the client already has and draws on the HUD. Nothing is sent.
 * Live, still marked as a test feature. Also: Grim Reaper (graveyard), Yeti (ice claw machine), Reindrake (dragon gifts).
 */
public final class LavaScene {
    private static final float RISE_AT = 0.5f;
    private static final float GAMBLE_AT = 1.4f;
    private static final float MIN_SPIN = 2.2f;
    private static final float STOP_LEN = 1.6f;
    private static final float WAIT_MAX = 10.0f;
    private static final int PARTICLES = 50;
    private static final int CHUNKS = 30;
    private static final int SLOT_MACHINE_H = 182;
    private static final float SLOT_SCALE = 0.70f;
    /** Jawbus: machine a bit lower, golem a bit higher. */
    private static final int SLOT_DROP = 40;
    /** Jawbus: how far the golem stands above the machine (more = head higher, face free). */
    private static final int GOLEM_LIFT = 145;
    private static final float RIDE = 3.2f;

    private enum Game { SLOTS, ROULETTE, WHEEL, BUBBLES, SHOOT, GRAVES, CLAW, GIFTS }

    private record Sym(String label, int color, String match) {
    }

    private static final Sym NONE = new Sym("x", 0x4A4A4A, null);
    private static final Sym OTHER = new Sym("???", 0xFFCC33, "");
    private static final Sym VIAL = new Sym("VIAL", 0x5FD35F, "radioactive vial");
    private static final Sym DYE = new Sym("DYE", 0xB0102A, "carmine dye");
    private static final Sym FLASH = new Sym("PROSP", 0xC9A0FF, "flash");
    private static final Sym FRAG = new Sym("FRAG", 0xFFAA00, "magma lord fragment");
    private static final Sym TFRAG = new Sym("TFRAG", 0xAA55FF, "thunder fragment");
    private static final Sym ADYE = new Sym("ADYE", 0x2AB0B0, "aquamarine dye");
    private static final Sym TEXTS = new Sym("TEXTS", 0xE07A2A, "burnt texts");
    private static final Sym CHAIN = new Sym("CHAIN", 0x9A8A80, "chain of the end times");
    private static final Sym HANDLE = new Sym("HANDLE", 0xFFB347, "brimstone handle");
    private static final Sym BOOK = new Sym("BOOK", 0xC9A0FF, "bobbin' scriptures");
    private static final Sym FANG = new Sym("FANG", 0xE8E4D8, "vampire fang");
    private static final Sym ORB = new Sym("ORB", 0x3A7AFF, "deep sea orb");
    private static final Sym MDYE = new Sym("MDYE", 0x3A1A6A, "midnight dye");
    private static final Sym BYETI = new Sym("BYETI", 0xFFAA00, "baby yeti");
    private static final Sym ROD = new Sym("ROD", 0x7AD0FF, "yeti rod");
    private static final Sym HILT = new Sym("HILT", 0xBFE9FF, "true ice");
    private static final Sym IDYE = new Sym("IDYE", 0x4AB0D0, "iceberg dye");

    private record Look(String creature, String key, String cat, int color, Game game, EntityType<?> standIn, int[] palette, Sym[] pool) {
    }

    private static final Look[] LOOKS = {
        new Look("Lord Jawbus", "jawbus", "lava", 0xFF5A2A, Game.SLOTS, EntityTypes.IRON_GOLEM,
            new int[]{0xD8D3CC, 0x9E9890, 0x6E6A64, 0xFF7A1A}, new Sym[]{VIAL, DYE, FRAG, BOOK, OTHER, NONE}),
        new Look("Thunder", "thunder", "lava", 0x4AA3FF, Game.ROULETTE, EntityTypes.ELDER_GUARDIAN,
            new int[]{0x5E9C8C, 0x3F6F68, 0xE8D9B8, 0x6FD0FF}, new Sym[]{DYE, FLASH, TFRAG, NONE, OTHER, NONE}),
        new Look("Ragnarok", "ragnarok", "lava", 0xD0202E, Game.SHOOT, EntityTypes.WITHER_SKELETON,
            new int[]{0x3A0C10, 0x7A1A20, 0x2A2A2A, 0xFF3B3B}, new Sym[]{TEXTS, CHAIN, HANDLE, BOOK, DYE, OTHER, NONE}),
        new Look("Plhlegblast", "plhlegblast", "lava", 0x5FD35F, Game.BUBBLES, EntityTypes.SQUID,
            new int[]{0x2F6E9A, 0x1F4A6A, 0x9CFF6B, 0x5FD35F}, new Sym[]{DYE, NONE, OTHER, NONE, NONE}),
        new Look("Grim Reaper", "grim", "spooky", 0x78FFC8, Game.GRAVES, EntityTypes.WITHER_SKELETON,
            new int[]{0x1A1A22, 0x2E2A3A, 0x78FFC8, 0x3AD08A}, new Sym[]{FANG, ORB, NONE, BOOK, MDYE, ADYE, OTHER, NONE}),
        new Look("Yeti", "yeti", "winter", 0xBFE9FF, Game.CLAW, EntityTypes.ZOMBIE,
            new int[]{0xF0F6FC, 0xBFE9FF, 0x8AB8D8, 0xFFFFFF}, new Sym[]{BYETI, ROD, NONE, HILT, BOOK, IDYE, ADYE, OTHER, NONE}),
        new Look("Reindrake", "reindrake", "winter", 0xD03030, Game.GIFTS, EntityTypes.ENDER_DRAGON,
            new int[]{0xD03030, 0x30A050, 0xF0F0F0, 0xFFD24A}, new Sym[]{IDYE, ADYE, BOOK, OTHER, NONE}),
    };

    /** The animation speed for a mob (Gambling tab, 50 - 200 %). */
    private static float speedFactor() {
        TikiConfig c = cfg();
        return c == null || look == null ? 1.0f : c.gambleSpeedOf(look.key);
    }

    private static boolean active;
    private static long startNs;
    private static Look look;
    private static String name = "";
    private static boolean lootShare;
    private static boolean test;
    /** Light 1.2: the logged result of this scene (History) + the beta extras (dry streak, RNG fanfare). */
    private static GambleLog.Entry entry;
    private static float fanfareAt = -1;
    private static boolean fanfareShot;
    private static boolean forceFanfare;
    private static boolean betaBroken;
    private static LivingEntity mob;
    private static LivingEntity horse;
    private static float finalShot = -1;
    private static final int BULLETS = 6;
    private static final float[] SHOT_AT = new float[BULLETS];
    private static int hitShot = -1;
    private static final List<String> DROPS = new ArrayList<>();
    private static float resultAt = -1;
    private static float stopAt = -1;
    private static int resultIdx = -1;
    private static String resultName;
    private static int[] reelFinal = new int[3];
    private static final float[] STOP_P = new float[5];
    private static final float[] TARGET_P = new float[5];
    private static int lastTick = Integer.MIN_VALUE;
    private static long lastTickMs;
    private static final boolean[] SOUNDS = new boolean[16];
    private static final float[] PX = new float[PARTICLES];
    private static final float[] PSPEED = new float[PARTICLES];
    private static final float[] PPHASE = new float[PARTICLES];
    private static final float[] CVX = new float[CHUNKS];
    private static final float[] CVY = new float[CHUNKS];
    private static final int[] CSIZE = new int[CHUNKS];
    private static final int[] CCOL = new int[CHUNKS];
    private static final Random RNG = new Random();

    private LavaScene() {
    }

    private static Look lookOf(String creature) {
        if (creature == null) return null;
        for (Look l : LOOKS) if (l.creature.equals(creature) || l.key.equals(creature) || l.creature.equalsIgnoreCase(creature)) return l;
        if ("grimreaper".equalsIgnoreCase(creature)) return lookOf("grim");
        return null;
    }

    private static TikiConfig cfg() {
        return TikiGambleClient.config;
    }

    /** Lava Gambling: live (still marked test). */
    public static boolean enabled() {
        TikiConfig c = cfg();
        return c != null && c.lavaGambleEnabled;
    }

    /** Is the category of this mob on? (Lava, Spooky, Winter - all live, still marked as test.) */
    private static boolean catOn(Look l) {
        TikiConfig c = cfg();
        if (c == null) return false;
        return switch (l.cat) {
            case "spooky" -> c.spookyGambleEnabled;
            case "winter" -> c.winterGambleEnabled;
            default -> c.lavaGambleEnabled;
        };
    }

    private static boolean mobOn(Look l) {
        TikiConfig c = cfg();
        if (c == null) return false;
        return switch (l.key) {
            case "jawbus" -> c.lavaGambleJawbus;
            case "thunder" -> c.lavaGambleThunder;
            case "ragnarok" -> c.lavaGambleRagnarok;
            case "grim" -> c.gambleGrimReaper;
            case "yeti" -> c.gambleYeti;
            case "reindrake" -> c.gambleReindrake;
            default -> c.lavaGamblePlhlegblast;
        };
    }

    /** Called after a counted kill. */
    public static void onKill(String creature, boolean share, LivingEntity body) {
        try {
            Look l = lookOf(creature);
            if (l == null || !catOn(l) || !mobOn(l)) return;
            if (share && cfg().lavaGambleOwnOnly) return;
            if (active && look == l && !test) return; // the same kill reported twice (chat + nametag)
            start(l, share, false, body);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] lava gambling failed", t);
        }
    }

    /** /light anim: plays the scene with a random result from the real drop pool (sometimes nothing). */
    public static boolean test(String which, boolean share) {
        Look l = lookOf(which);
        if (l == null) return false;
        start(l, share, true, null);
        Sym pick = l.pool[RNG.nextInt(l.pool.length)];
        if (pick == OTHER) pick = NONE;
        if (pick != NONE) addDrop(capital(pick.match));
        return true;
    }

    /** /light test fanfare: plays the scene with this drop and always fires the RNG fanfare. */
    public static boolean testWith(String which, String loot, boolean fanfare) {
        Look l = lookOf(which);
        if (l == null) return false;
        start(l, false, true, null);
        forceFanfare = fanfare;
        if (loot != null && !loot.isBlank()) addDrop(capital(loot.trim().toLowerCase(Locale.ROOT)));
        return true;
    }

    private static boolean beta() {
        TikiConfig c = cfg();
        return c != null && c.betaFeatures && !betaBroken;
    }

    private static void betaFailed(Throwable t) {
        betaBroken = true;
        TikiGambleClient.LOGGER.warn("[Light] beta feature switched off", t);
        try {
            if (cfg() != null && cfg().debug) TikiGambleClient.chat("\u00a7c[Light] Beta feature switched off (animation keeps running): " + t);
        } catch (Throwable ignored) {
        }
    }

    private static String capital(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split(" ")) {
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }

    private static void start(Look l, boolean share, boolean isTest, LivingEntity body) {
        look = l;
        name = l.creature.toUpperCase(Locale.ROOT);
        lootShare = share;
        test = isTest;
        horse = null;
        entry = null;
        fanfareAt = -1;
        fanfareShot = false;
        forceFanfare = false;
        finalShot = -1;
        hitShot = -1;
        if (l.game == Game.SHOOT) {
            // Ragnarok rides a skeleton horse: find rider and horse from whichever body was found
            LivingEntity riderBody = body, horseBody = null;
            try {
                if (body != null && body.getVehicle() instanceof LivingEntity v) horseBody = v;
                else if (body != null && body.getFirstPassenger() instanceof LivingEntity r) {
                    horseBody = body;
                    riderBody = r;
                }
            } catch (Throwable ignored) {
            }
            mob = MobView.snapshot(riderBody, l.standIn);
            horse = MobView.snapshot(horseBody, EntityTypes.SKELETON_HORSE);
            try {
                if (mob != null && horse != null) mob.startRiding(horse, true, false);
            } catch (Throwable ignored) {
            }
        } else if (l.game == Game.GIFTS) {
            // the Reindrake is a normal Minecraft Ender Dragon
            mob = MobView.snapshot(null, EntityTypes.ENDER_DRAGON);
        } else {
            mob = MobView.snapshot(body, l.standIn);
            // no usable body (none, invisible or only an armor stand): the stand-in dressed like the SkyBlock mob
            if (mob == null || mob.isInvisible() || mob instanceof net.minecraft.world.entity.decoration.ArmorStand) {
                mob = MobView.snapshot(null, l.standIn);
                if (mob != null) dressStandIn(l.game);
            } else if (body == null) {
                dressStandIn(l.game);
            }
        }
        // the snowman next to the Yeti's claw machine / in the Reindrake's snow
        if (l.game == Game.CLAW || l.game == Game.GIFTS) horse = MobView.snapshot(null, EntityTypes.SNOW_GOLEM);
        if (l.game == Game.GIFTS) {
            for (int i = 0; i < SWAPS.length; i++) SWAPS[i] = RNG.nextInt(3);
        }
        if (cfg() != null && cfg().debug) {
            TikiGambleClient.chat("§8[Light] scene: body=" + (body == null ? "none" : body.getType().toShortString())
                + " mob=" + (mob == null ? "none" : mob.getType().toShortString()) + " 3d=" + MobView.available());
        }
        DROPS.clear();
        resultAt = -1;
        stopAt = -1;
        resultIdx = -1;
        resultName = null;
        lastTick = Integer.MIN_VALUE;
        java.util.Arrays.fill(SOUNDS, false);
        for (int i = 0; i < PARTICLES; i++) {
            PX[i] = RNG.nextFloat();
            PSPEED[i] = 0.2f + RNG.nextFloat() * 0.5f;
            PPHASE[i] = RNG.nextFloat();
        }
        for (int i = 0; i < CHUNKS; i++) {
            double ang = -Math.PI * (0.1 + 0.8 * RNG.nextFloat());
            float sp = 60 + RNG.nextFloat() * 140;
            CVX[i] = (float) (Math.cos(ang) * sp);
            CVY[i] = (float) (Math.sin(ang) * sp);
            CSIZE[i] = 2 + RNG.nextInt(5);
            CCOL[i] = l.palette[RNG.nextInt(l.palette.length)];
        }
        startNs = System.nanoTime();
        active = true;
    }

    /** Test scenes (no real body): dress the stand-in like the SkyBlock mob - its real head skin from the NEU repo. */
    private static void dressStandIn(Game g) {
        try {
            var head = net.minecraft.world.entity.EquipmentSlot.HEAD;
            if (g == Game.GRAVES) {
                mob.setItemSlot(head, GambleArt.icon("GRIMHEAD").copy());
                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.NETHERITE_HOE));
            } else if (g == Game.CLAW) {
                mob.setItemSlot(head, GambleArt.icon("YETIHEAD").copy());
                var white = new net.minecraft.world.item.component.DyedItemColor(0xF2F6FA);
                ItemStack chest = new ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE);
                ItemStack legs = new ItemStack(net.minecraft.world.item.Items.LEATHER_LEGGINGS);
                ItemStack boots = new ItemStack(net.minecraft.world.item.Items.LEATHER_BOOTS);
                for (ItemStack s : new ItemStack[]{chest, legs, boots}) s.set(net.minecraft.core.component.DataComponents.DYED_COLOR, white);
                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, legs);
                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, boots);
            }
        } catch (Throwable ignored) {
        }
    }

    /** RARE DROP / PET DROP lines while the scene runs. */
    public static void onChat(Component message, boolean overlay) {
        if (!active || overlay || message == null || test) return;
        try {
            String s = message.getString().replaceAll("§.", "").trim();
            String up = s.toUpperCase(Locale.ROOT);
            String item = null;
            if (up.startsWith("RARE DROP!")) item = s.substring(10).trim();
            else if (up.startsWith("PET DROP!")) item = s.substring(9).trim();
            if (item == null || item.isEmpty()) return;
            int mf = item.indexOf("(+");
            if (mf > 0) item = item.substring(0, mf).trim();
            addDrop(item);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] lava gambling chat failed", t);
        }
    }

    /** Rare items that went straight into sacks/inventory (some drops have no RARE DROP line). */
    public static void onItemGained(String name, String id, int count, boolean viaSack) {
        if (!active || name == null || test) return;
        try {
            String l = name.replaceAll("§.", "").trim();
            String low = l.toLowerCase(Locale.ROOT);
            boolean rare = low.contains("dye") || low.contains("radioactive vial") || low.contains("burnt texts")
                || low.contains("chain of the end times") || low.contains("brimstone handle") || low.contains("bobbin")
                || low.contains("flaming heart") || low.contains("enrager") || low.contains("flash")
                || low.contains("vampire fang") || low.contains("deep sea orb") || low.contains("baby yeti")
                || low.contains("yeti rod") || low.contains("true ice") || low.contains("magma lord fragment") || low.contains("thunder fragment");
            if (rare) addDrop(l);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] lava gambling item failed", t);
        }
    }

    private static void addDrop(String item) {
        if (DROPS.size() >= 6 || item == null) return;
        // no shards of any kind (attribute / hunting shards) - they are never a gambling result
        if (item.toLowerCase(Locale.ROOT).contains("shard")) return;
        // guaranteed drops are never a gambling result (Reindrake: Prosperity I for the spawner)
        if (item.toLowerCase(Locale.ROOT).contains("prosperity")) return;
        for (String d : DROPS) if (d.equalsIgnoreCase(item)) return;
        DROPS.add(item);
        LavaPrices.price(item); // fetch early so the price is there when the result shows
    }

    /** ESC while the scene runs: end it instead of opening the pause menu. */
    public static boolean skip() {
        if (!active) return false;
        active = false;
        mob = null;
        return true;
    }

    private static float time() {
        return (System.nanoTime() - startNs) / 1.0e9f * speedFactor();
    }

    private static int argb(float a, int rgb) {
        int al = Math.max(0, Math.min(255, Math.round(a * 255.0f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static float vol() {
        TikiConfig c = cfg();
        return c == null ? 0.6f : Math.max(0f, Math.min(1f, c.lavaGambleVolume));
    }

    private static void sound(int slot, SoundEvent ev, float pitch, float v) {
        if (slot >= 0) {
            if (SOUNDS[slot]) return;
            SOUNDS[slot] = true;
        }
        float volume = v * vol();
        if (volume <= 0.01f) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, volume));
    }

    /** Locks the result: first drop that is on this mob's board, otherwise ??? with its name, otherwise nothing. */
    private static void decide() {
        resultIdx = indexOf(NONE);
        resultName = null;
        // several drops at once (e.g. Magma Lord Fragment + Radioactive Vial): the best one wins -
        // a dye first, then the order of the board (most valuable first)
        int bestRank = Integer.MAX_VALUE;
        for (String d : DROPS) {
            String low = d.toLowerCase(Locale.ROOT);
            for (int i = 0; i < look.pool.length; i++) {
                Sym s = look.pool[i];
                if (s.match != null && !s.match.isEmpty() && low.contains(s.match)) {
                    int rank = s.match.contains("dye") ? -1 : i;
                    if (rank < bestRank) {
                        bestRank = rank;
                        resultIdx = i;
                        resultName = d;
                    }
                    break;
                }
            }
        }
        if (resultName != null) return;
        if (!DROPS.isEmpty()) {
            resultIdx = indexOf(OTHER);
            resultName = DROPS.get(0);
        }
    }

    private static int indexOf(Sym s) {
        for (int i = 0; i < look.pool.length; i++) if (look.pool[i] == s) return i;
        return 0;
    }

    private static boolean win() {
        return resultIdx >= 0 && look.pool[resultIdx] != NONE;
    }

    /** Spin position (in symbols) of track k at time t; settles exactly on its target after the stop. */
    private static float pos(int k, float t, float speed, float stopStart, float stopLen) {
        if (stopAt < 0 || t < stopStart) return t * speed + k * 1.37f;
        float u = clamp((t - stopStart) / stopLen);
        float e = 1.0f - (1.0f - u) * (1.0f - u) * (1.0f - u);
        return STOP_P[k] + (TARGET_P[k] - STOP_P[k]) * e;
    }

    private static void planStop(int k, int finalIdx, float speed, float stopStart, int n) {
        float p = stopStart * speed + k * 1.37f;
        STOP_P[k] = p;
        int base = (int) Math.ceil(p) + 2 * n;
        TARGET_P[k] = base + Math.floorMod(finalIdx - base, n);
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        if (!active) return;
        try {
            draw(ctx);
        } catch (Throwable t) {
            active = false;
            mob = null;
            TikiGambleClient.LOGGER.warn("[Light] lava gambling render failed", t);
        }
    }

    private static void draw(GuiGraphicsExtractor ctx) {
        Minecraft mc = Minecraft.getInstance();
        float t = time();
        Game game = look.game;
        int n = look.pool.length;

        if (resultAt < 0 && (test || !DROPS.isEmpty() || t >= WAIT_MAX)) resultAt = t;
        if (stopAt < 0 && resultAt >= 0 && t >= GAMBLE_AT + MIN_SPIN) {
            stopAt = Math.max(t, resultAt + 0.2f);
            decide();
            try {
                entry = GambleLog.record(look.key, look.creature, win() ? resultName : null, lootShare, test);
            } catch (Throwable e) {
                TikiGambleClient.LOGGER.warn("[Light] gamble log failed", e);
            }
            float speed = speedOf(game);
            if (game == Game.SLOTS) {
                if (win()) {
                    reelFinal = new int[]{resultIdx, resultIdx, resultIdx};
                } else {
                    int v = indexOf(VIAL);
                    reelFinal = new int[]{v, v, resultIdx}; // a near miss
                }
                for (int k = 0; k < 3; k++) planStop(k, reelFinal[k], speed, stopAt + k * 0.45f, n);
            } else {
                planStop(0, resultIdx, speed, stopAt, n);
            }
            sound(3, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.8f, 0.5f);
        }
        if (game == Game.SHOOT && stopAt >= 0 && finalShot < 0) {
            // 6 bullets: the ones already fired while waiting missed; a win hits on a random one of the rest,
            // nothing = all six miss and it escapes
            float scopeAt = GAMBLE_AT + com.skymaxx.tiki.render.boa.LightRifle.AIM_TIME;
            int fired = 0;
            for (int k = 0; k < BULLETS - 1; k++) if (preShot(k, scopeAt) < stopAt + 0.2f) fired = k + 1;
            float next = Math.max(stopAt + 0.4f, fired == 0 ? scopeAt + 0.5f : preShot(fired - 1, scopeAt) + 0.9f);
            for (int k = 0; k < BULLETS; k++) {
                if (k < fired) SHOT_AT[k] = preShot(k, scopeAt);
                else {
                    SHOT_AT[k] = next;
                    next += 0.9f;
                }
            }
            hitShot = win() ? fired + RNG.nextInt(BULLETS - fired) : -1;
            finalShot = SHOT_AT[hitShot >= 0 ? hitShot : BULLETS - 1];
        }
        float settled = stopAt < 0 ? Float.MAX_VALUE : (game == Game.SHOOT ? finalShot + (win() ? 0.9f : 1.6f)
            : game == Game.GIFTS ? giftEnd() + 1.2f
            : stopAt + STOP_LEN + (game == Game.SLOTS ? 0.9f : 0f) + (game == Game.BUBBLES ? 1.2f : 0f)
                + (game == Game.GRAVES ? 0.9f : 0f) + (game == Game.CLAW ? CLAW_GRAB : 0f));
        float burstAt = settled + (stopAt >= 0 && win() ? 1.8f : 1.0f);
        float nameAt = burstAt + 0.3f;
        float fadeAt = nameAt + 2.8f + (fanfareAt >= 0 ? 2.4f : 0f);
        float length = fadeAt + 0.7f;
        if (t >= length) {
            active = false;
            mob = null;
            return;
        }

        float in = clamp(t / RISE_AT);
        float out = t > fadeAt ? clamp(1.0f - (t - fadeAt) / (length - fadeAt)) : 1.0f;
        float a = in * out;
        int sw = ctx.guiWidth();
        int sh = ctx.guiHeight();
        Font font = mc.font;
        int poolTop = (int) (sh * (1.0f - 0.26f * in));
        int hot = 0xFFD15C;

        if (game == Game.SHOOT) {
            drawShoot(ctx, font, t, a, sw, sh, settled, burstAt);
        } else if (game == Game.GRAVES) {
            drawGraves(ctx, font, t, a, sw, sh, settled, burstAt);
        } else if (game == Game.CLAW) {
            drawClaw(ctx, font, t, a, sw, sh, settled, burstAt);
        } else if (game == Game.GIFTS) {
            drawGifts(ctx, font, t, a, sw, sh, settled, burstAt);
        } else {
        int sky = game == Game.WHEEL ? 0x1A0303 : (game == Game.ROULETTE ? 0x06121C : (game == Game.BUBBLES ? 0x03101A : 0x0C0604));
        ctx.fill(0, 0, sw, sh, argb(0.8f * a, sky));
        ctx.fillGradient(0, poolTop - sh / 3, sw, poolTop, argb(0.0f, look.color), argb(0.35f * a, look.color));
        drawWeather(ctx, game, t, a, sw, sh, poolTop);

        int cx = sw / 2;
        int mobPx = (int) (sh * 0.40f);
        int standCy = poolTop - mobPx / 2 + mobPx / 8;
        boolean gambleOn = t >= GAMBLE_AT && t < burstAt + 0.2f;
        float ga = clamp((t - GAMBLE_AT) / 0.25f) * clamp((burstAt + 0.2f - t) / 0.2f) * out;
        if (gambleOn && game == Game.WHEEL) {
            drawWheel(ctx, font, cx, standCy - mobPx / 6, Math.min((int) (mobPx * 0.62f), sh / 3), t, ga);
            ctx.nextStratum();
        }
        boolean burst = t >= burstAt;
        if (t >= RISE_AT) sound(0, SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 0.3f);
        if (!burst && t >= RISE_AT && mob != null && MobView.available()) {
            float rise = clamp((t - RISE_AT) / (GAMBLE_AT - RISE_AT));
            float ease = 1.0f - (1.0f - rise) * (1.0f - rise);
            int px = mobPx;
            int stand = standCy;
            float lookDown = -0.15f;
            if (game == Game.SLOTS) {
                // so big that the slot machine sits in its chest, its head above it looking down
                // the torso is about a third of an iron golem: the machine covers it, head above, arms beside
                // the machine (drawn at SLOT_SCALE) sits in its upper torso: torso around it, head above
                px = 480;
                int chest = standCy - mobPx / 8 - 10;
                stand = chest + (int) (0.16f * px) - GOLEM_LIFT; // head clearly above the machine
                lookDown = 3.0f;
            }
            int cy = (int) (poolTop + px * 0.6f - (poolTop + px * 0.6f - stand) * ease);
            float shake = 0;
            switch (game) {
                case SLOTS -> {
                    float beat = (t * 1.4f) % 1.0f;
                    cy += (int) (beat < 0.2f ? -beat * 30 : Math.max(0, 6 - (beat - 0.2f) * 30));
                    if (beat < 0.08f) shake = 3;
                }
                case ROULETTE -> mob.hurtTime = (((int) (t * 7)) % 5 == 0) ? 6 : 0;
                case WHEEL -> cy += (int) (Math.sin(t * 1.5f) * 3);
                case BUBBLES -> px = (int) (mobPx * (1.0f + 0.35f * clamp((t - GAMBLE_AT) / 6.0f)));
            }
            if (t >= settled) {
                float c = clamp((t - settled) / (burstAt - settled));
                shake = 2 + 4 * c;
                mob.hurtTime = ((int) (t * 12) % 2 == 0) ? 8 : 0;
            }
            int sx = (int) (shake * (RNG.nextFloat() * 2 - 1));
            int sy = (int) (shake * (RNG.nextFloat() * 2 - 1));
            ctx.nextStratum();
            if (game == Game.SLOTS) {
                // leans over its own chest and looks down at the reels
                float lean = -10.0f * clamp((t - GAMBLE_AT + 0.3f) / 0.6f);
                MobView.drawPosed(ctx, mob, cx + sx, cy + sy, px, (float) Math.sin(t * 0.6f) * 4.0f, lean, 45.0f * clamp((t - RISE_AT) / 0.6f));
            } else {
                MobView.draw(ctx, mob, cx + sx, cy + sy, px, (float) Math.sin(t * 0.9f) * 0.6f, lookDown);
            }
            ctx.nextStratum();
            if (t >= settled) drawCracks(ctx, cx + sx, stand + sy, Math.min(px, mobPx), clamp((t - settled) / (burstAt - settled)), a, hot);
        }
        if (burst) {
            sound(1, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.55f);
            float bt = t - burstAt;
            if (bt < 0.15f) ctx.fill(0, 0, sw, sh, argb((1.0f - bt / 0.15f) * 0.45f * a, game == Game.ROULETTE ? 0xE8F4FF : hot));
            for (int i = 0; i < CHUNKS; i++) {
                float x = cx + CVX[i] * bt;
                float y = standCy + CVY[i] * bt + 260.0f * bt * bt;
                if (y > poolTop + 4) continue;
                int s = CSIZE[i];
                ctx.fill((int) x, (int) y, (int) x + s, (int) y + s, argb(a, CCOL[i]));
            }
            if (game == Game.ROULETTE && bt < 0.6f && ((int) (bt * 20) % 3 != 0)) {
                drawBolt(ctx, cx - 40, 0, standCy, a);
                drawBolt(ctx, cx + 35, 0, standCy - 10, a);
            }
        }

        ctx.fill(0, poolTop, sw, sh, argb(a, 0xC2370F));
        for (int x = 0; x < sw; x += 4) {
            int wave = (int) (Math.sin(x * 0.05 + t * 3.0) * 2 + Math.sin(x * 0.13 - t * 2.0));
            ctx.fill(x, poolTop - 2 + wave, x + 4, poolTop + 2 + wave, argb(a, 0xFF7A1A));
        }
        for (int i = 0; i < 10; i++) {
            float bl = (t * 0.8f + i * 0.37f) % 1.0f;
            int bx = (int) ((i * 97 + 31) % Math.max(1, sw));
            int by = poolTop + 8 + (i * 13) % Math.max(1, (sh - poolTop - 12));
            ctx.fill(bx, by, bx + 2, by + 2, argb(a * (1 - bl), hot));
        }
        for (int i = 0; i < PARTICLES; i++) {
            float life = (t * PSPEED[i] + PPHASE[i]) % 1.0f;
            int x = (int) (PX[i] * sw + Math.sin((t + PPHASE[i] * 7) * 2.0f) * 6);
            int y = (int) (poolTop - life * poolTop * 0.9f);
            float fa = a * (1.0f - life) * (0.5f + 0.5f * (float) Math.sin(t * 15 + i));
            int c = (i % 3 == 0) ? 0xFFE08A : (i % 3 == 1 ? 0xFFB347 : look.color);
            int s = i % 4 == 0 ? 2 : 1;
            ctx.fill(x, y, x + s, y + s, argb(fa, c));
        }

        if (gambleOn) {
            ctx.nextStratum();
            switch (game) {
                case SLOTS -> {
                    int mcy = standCy - mobPx / 8 + SLOT_DROP;
                    Matrix3x2fStack ps = ctx.pose();
                    ps.pushMatrix();
                    ps.translate(cx, mcy - 10);
                    ps.scale(SLOT_SCALE, SLOT_SCALE);
                    ps.translate(-cx, -(mcy - 10));
                    chestCavity(ctx, cx, mcy - 10, t, ga);
                    drawSlots(ctx, font, cx, mcy, t, ga);
                    ps.popMatrix();
                }
                case ROULETTE -> drawRoulette(ctx, font, cx, standCy, (int) (mobPx * 0.62f), t, ga);
                case BUBBLES -> drawBubbles(ctx, font, cx, standCy, (int) (mobPx * 0.6f), t, ga, poolTop);
                default -> {
                }
            }
            if (stopAt < 0 && t > GAMBLE_AT + MIN_SPIN && !test) {
                String dots = ".".repeat(1 + (int) (t * 3) % 3);
                ctx.centeredText(font, Component.literal("Waiting for your drop" + dots), cx, poolTop - 14, argb(ga * 0.8f, 0xF3D9C9));
            }
        }
        if (t >= settled && !SOUNDS[4]) {
            if (win()) sound(4, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.55f);
            else sound(4, SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, 0.6f);
        }

        }
        if (t < nameAt) return;

        float st = t - nameAt;
        float scale = st < 0.15f ? 5.0f - 1.8f * (st / 0.15f) : 3.2f;
        float shake = st < 0.4f ? (1.0f - st / 0.4f) * 4.0f : 0.0f;
        float cy = sh * (look.game == Game.CLAW ? 0.09f : (look.game == Game.GRAVES ? 0.12f : 0.22f));
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(sw / 2.0f + shake * (RNG.nextFloat() * 2 - 1), cy + shake * (RNG.nextFloat() * 2 - 1));
        pose.scale(scale, scale);
        ctx.centeredText(font, Component.literal(name).withStyle(ChatFormatting.BOLD), 0, -4, argb(a, look.color));
        pose.popMatrix();

        String sub = (lootShare ? "Loot share" : "Your kill") + (test ? " - test" : "");
        float ra = clamp(st / 0.3f) * a;
        pose.pushMatrix();
        pose.translate(sw / 2.0f, cy + 4.0f * scale + 4.0f);
        pose.scale(1.3f, 1.3f);
        ctx.centeredText(font, Component.literal(sub), 0, 0, argb(ra, 0xF3D9C9));
        pose.popMatrix();

        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        Long resultPrice = null;
        if (resultName != null) {
            Long price = LavaPrices.price(resultName);
            resultPrice = price;
            if (price != null && entry != null) GambleLog.setPrice(entry, price);
            lines.add("★ " + resultName + (price != null ? "  §7" + LavaPrices.format(price) : ""));
            colors.add(0xFFCC33);
        }
        for (String d : DROPS) {
            if (d.equals(resultName)) continue;
            Long price = LavaPrices.price(d);
            lines.add(d + (price != null ? "  §7" + LavaPrices.format(price) : ""));
            colors.add(0xD9D9D9);
        }
        if (lines.isEmpty()) {
            lines.add("No rare drop");
            colors.add(0x9A8A80);
        }
        if (beta() && entry != null) {
            try {
                String mobs = look.creature + (entry.dry == 1 ? "" : (look.creature.endsWith("s") ? "" : "s"));
                lines.add(win() ? "after " + String.format(Locale.GERMANY, "%,d", entry.dry) + " " + mobs
                    : String.format(Locale.GERMANY, "%,d", entry.dry) + " " + mobs + " without a drop");
                colors.add(0xBFE9FF);
                if (fanfareAt < 0 && win() && st < 2.0f && fanfareWorthy(resultName, resultPrice)) {
                    fanfareAt = t;
                    GambleLog.setFanfare(entry);
                }
            } catch (Throwable e) {
                betaFailed(e);
            }
        }
        if (fanfareAt >= 0 && beta()) {
            try {
                drawFanfare(ctx, font, t - fanfareAt, a, sw, sh, cy, scale, cy + 4.0f * scale + 22.0f + (10 + lines.size() * 12) * 1.5f);
            } catch (Throwable e) {
                betaFailed(e);
            }
        }
        int w = 120;
        for (String l : lines) w = Math.max(w, font.width(l) + 20);
        int h = 10 + lines.size() * 12;
        pose.pushMatrix();
        pose.translate(sw / 2.0f, cy + 4.0f * scale + 22.0f);
        pose.scale(1.5f, 1.5f);
        ctx.fill(-w / 2, 0, w / 2, h, argb(0.88f * ra, 0x140A08));
        ctx.outline(-w / 2, 0, w, h, argb(ra, win() ? 0xFFCC33 : look.color));
        for (int i = 0; i < lines.size(); i++) {
            ctx.centeredText(font, Component.literal(lines.get(i)), 0, 6 + i * 12, argb(ra, colors.get(i)));
        }
        pose.popMatrix();
    }

    /** RNG fanfare: a dye, or a drop worth at least the threshold from the Settings tab. */
    private static boolean fanfareWorthy(String drop, Long price) {
        TikiConfig c = cfg();
        if (c == null || !c.fanfareEnabled) return false;
        if (forceFanfare) return true;
        if (drop != null && drop.toLowerCase(Locale.ROOT).contains("dye")) return true;
        return price != null && price >= (long) (c.fanfareMillions * 1_000_000.0);
    }

    /** A golden glow and a short ray burst behind the result, falling coins, "RNG!" clear of the mob name. */
    private static void drawFanfare(GuiGraphicsExtractor ctx, Font font, float ft, float a, int sw, int sh, float cy, float nameScale, float cardBottom) {
        float in = clamp(ft / 0.3f) * a;
        if (ft < 0.12f) ctx.fill(0, 0, sw, sh, argb((1 - ft / 0.12f) * 0.45f * a, 0xFFF0B4));
        Matrix3x2fStack pose = ctx.pose();
        float cx = sw / 2.0f;
        float my = (cy + cardBottom) / 2.0f;
        // soft pulsing glow around the result
        float pulse = 0.75f + 0.25f * (float) Math.sin(ft * 4);
        for (int g = 6; g >= 1; g--) {
            int rw = (int) (sw * 0.18f + g * 22), rh = (int) ((cardBottom - cy) * 0.6f + g * 14);
            ctx.fill((int) cx - rw, (int) my - rh, (int) cx + rw, (int) my + rh, argb(in * 0.05f * pulse, 0xFFD24A));
        }
        // one short burst of rays that shoots out and fades (no spinning)
        if (ft < 0.9f) {
            float u = ft / 0.9f;
            float len = Math.max(sw, sh) * (0.25f + 0.6f * u);
            pose.pushMatrix();
            pose.translate(cx, my);
            for (int i = 0; i < 16; i++) {
                pose.pushMatrix();
                pose.rotate(i * (float) (Math.PI * 2 / 16));
                ctx.fillGradient(-2, (int) (len * 0.35f), 2, (int) len, argb(a * 0.5f * (1 - u), 0xFFF3B0), argb(0, 0xFFD24A));
                pose.popMatrix();
            }
            pose.popMatrix();
        }
        // coins
        for (int i = 0; i < 70; i++) {
            float l = (ft * 0.45f + i * 0.137f) % 1.0f;
            float x = (i * 97) % Math.max(1, sw) + (float) Math.sin(ft * 2 + i) * 10;
            float y = l * sh * 1.15f - 20;
            int s = i % 4 == 0 ? 5 : 4;
            ctx.fill((int) x, (int) y, (int) x + s, (int) y + s, argb(in * 0.9f, i % 3 == 0 ? 0xFFF3B0 : 0xFFD24A));
            ctx.fill((int) x, (int) y, (int) x + s, (int) y + 1, argb(in, 0xFFFFFF));
        }
        // "RNG!" above the mob name, or below the result card when there is no room above
        float sc = ft < 0.2f ? 5.0f - 2.0f * (ft / 0.2f) : 3.0f + 0.15f * (float) Math.sin(ft * 6);
        float nameTop = cy - 6.0f * nameScale;
        float ry = nameTop - 6 - 5 * sc;
        if (ry - 5 * sc < 4) ry = cardBottom + 8 + 5 * sc;
        float shake = ft < 0.5f ? (1 - ft / 0.5f) * 4 : 0;
        pose.pushMatrix();
        pose.translate(cx + shake * (RNG.nextFloat() * 2 - 1), ry + shake * (RNG.nextFloat() * 2 - 1));
        pose.scale(sc, sc);
        ctx.centeredText(font, Component.literal("RNG!").withStyle(ChatFormatting.BOLD), 0, -4, argb(in, 0xFFD24A));
        pose.popMatrix();
        if (ft < 0.1f) {
            sound(12, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 0.9f);
            sound(13, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 0.8f);
        }
        if (ft > 0.5f && ft < 0.6f) sound(14, SoundEvents.PLAYER_LEVELUP, 1.0f, 0.7f);
        TikiConfig c = cfg();
        if (!fanfareShot && ft > 0.9f && c != null && c.fanfareScreenshot && !test) {
            fanfareShot = true;
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                try {
                    net.minecraft.client.Screenshot.grab(mc, false);
                } catch (Throwable e) {
                    TikiGambleClient.LOGGER.warn("[Light] screenshot failed", e);
                }
            });
        }
    }

    private static float speedOf(Game g) {
        return switch (g) {
            case SLOTS -> 9.0f;
            case ROULETTE -> 8.0f;
            case WHEEL -> 5.0f;
            case SHOOT -> 1.0f;
            case BUBBLES -> 6.0f;
            case GRAVES -> 7.0f;
            case CLAW, GIFTS -> 1.0f;
        };
    }

    /** A click each time a symbol passes - at most one every 45 ms. */
    private static void tick(int i, float pitch) {
        if (i == lastTick) return;
        lastTick = i;
        long now = System.currentTimeMillis();
        if (now - lastTickMs < 45L) return;
        lastTickMs = now;
        sound(-1, SoundEvents.NOTE_BLOCK_HAT.value(), pitch, 0.35f);
    }

    private static ItemStack iconOf(Sym s) {
        return GambleArt.icon(s.label);
    }

    /** The icon for a logged drop (History / Journal), or null for an unknown item ("?"). */
    public static ItemStack iconForDrop(String drop) {
        if (drop == null) return GambleArt.icon("x");
        String low = drop.toLowerCase(Locale.ROOT);
        for (Look l : LOOKS) {
            for (Sym s : l.pool) {
                if (s.match != null && !s.match.isEmpty() && low.contains(s.match)) return iconOf(s);
            }
        }
        return null;
    }

    /** The title color of a gambling mob (Journal). */
    public static int colorOf(String key) {
        Look l = lookOf(key);
        return l == null ? 0x3A2410 : l.color;
    }

    /** Lord Jawbus: an iron slot machine with lava in its seams and three real, scrolling reels. */
    private static void drawSlots(GuiGraphicsExtractor ctx, Font font, int cx, int cy, float t, float a) {
        int n = look.pool.length;
        int ww = 56, wh = 124, gap = 10, pad = 16;
        int mw = 3 * ww + 2 * gap + 2 * pad;
        int mh = SLOT_MACHINE_H;
        int mx = cx - mw / 2, my = cy - mh / 2 - 10;
        // iron cabinet
        ctx.fill(mx - 5, my - 5, mx + mw + 5, my + mh + 5, GambleArt.argb(a, 0x2A2622));
        ctx.fillGradient(mx, my, mx + mw, my + mh, GambleArt.argb(a, 0xB8B2AA), GambleArt.argb(a, 0x6E6A64));
        for (int yy = my + 31; yy < my + mh; yy += 31) {
            float gl = 0.5f + 0.5f * (float) Math.sin(t * 4 + yy);
            ctx.fill(mx, yy, mx + mw, yy + 2, GambleArt.argb(a * (0.6f + 0.4f * gl), gl > 0.5f ? 0xFFB347 : 0xFF6A1A));
        }
        float vs = 0.7f + 0.3f * (float) Math.sin(t * 5);
        ctx.fill(mx + pad + ww + gap / 2 - 1, my, mx + pad + ww + gap / 2 + 1, my + mh, GambleArt.argb(a * vs, 0xFF6A1A));
        ctx.fill(mx + pad + 2 * ww + gap + gap / 2 - 1, my, mx + pad + 2 * ww + gap + gap / 2 + 1, my + mh, GambleArt.argb(a * vs, 0xFF6A1A));
        for (int x = mx + 6; x < mx + mw - 4; x += 16) {
            ctx.fill(x, my + 3, x + 3, my + 6, GambleArt.argb(a, 0xD8D3CC));
            ctx.fill(x, my + mh - 6, x + 3, my + mh - 3, GambleArt.argb(a, 0xD8D3CC));
        }
        // golem eyes on the corners
        boolean eyes = ((int) (t * 3)) % 2 == 0;
        for (int ex : new int[]{mx + 10, mx + mw - 20}) {
            ctx.fill(ex - 2, my - 14, ex + 12, my - 6, GambleArt.argb(a, 0x3A3630));
            ctx.fill(ex, my - 12, ex + 10, my - 8, GambleArt.argb(a, eyes ? 0xFF2A1A : 0x7A1A10));
        }
        // marquee
        ctx.fill(mx + 40, my + 6, mx + mw - 40, my + 22, GambleArt.argb(a, 0x2A2622));
        int molten = 0xFF0000 | ((120 + (int) (60 * Math.sin(t * 6))) << 8) | 0x28;
        ctx.centeredText(font, Component.literal("IRON JACKPOT").withStyle(ChatFormatting.BOLD), cx, my + 10, GambleArt.argb(a, molten));
        int wy = my + pad + 20;
        int center = wy + wh / 2;
        boolean all = stopAt >= 0 && t >= stopAt + 0.9f + STOP_LEN;
        for (int k = 0; k < 3; k++) {
            int wx = mx + pad + k * (ww + gap);
            float stopStart = stopAt < 0 ? Float.MAX_VALUE : stopAt + k * 0.45f;
            float p = pos(k, t, speedOf(Game.SLOTS), stopStart, STOP_LEN);
            boolean stopped = stopAt >= 0 && t >= stopStart + STOP_LEN;
            if (k == 2 || stopAt < 0) tick((int) Math.floor(p) * 3 + k, 1.6f);
            ctx.fill(wx - 2, wy - 2, wx + ww + 2, wy + wh + 2, GambleArt.argb(a, 0x1A1612));
            ctx.fill(wx, wy, wx + ww, wy + wh, GambleArt.argb(a, 0x3A3630));
            ctx.enableScissor(wx, wy, wx + ww, wy + wh);
            int base = (int) Math.floor(p);
            float frac = p - base;
            for (int j = -2; j <= 2; j++) {
                int idx = Math.floorMod(base + j, n);
                float y = center + (frac - j) * 50;
                GambleArt.ironCard(ctx, font, iconOf(look.pool[idx]), wx + ww / 2, Math.round(y), 40, a, stopped && j == 0 && all && win(), t);
            }
            ctx.fillGradient(wx, wy, wx + ww, wy + 28, GambleArt.argb(a * 0.75f, 0x000000), GambleArt.argb(0, 0x000000));
            ctx.fillGradient(wx, wy + wh - 28, wx + ww, wy + wh, GambleArt.argb(0, 0x000000), GambleArt.argb(a * 0.75f, 0x000000));
            ctx.disableScissor();
        }
        ctx.fill(mx + pad - 6, center - 1, mx + mw - pad + 6, center + 1, GambleArt.argb(a * 0.9f, 0xFF5A1E));
        for (int i = 0; i < 6; i++) {
            ctx.fill(mx + 4 + i, center - 6 + i, mx + 5 + i, center + 6 - i, GambleArt.argb(a, 0xFF5A1E));
            ctx.fill(mx + mw - 5 - i, center - 6 + i, mx + mw - 4 - i, center + 6 - i, GambleArt.argb(a, 0xFF5A1E));
        }
        // lever
        float pull = clamp((t - GAMBLE_AT) / 0.25f) * clamp((GAMBLE_AT + 0.7f - t) / 0.45f);
        int lx = mx + mw + 10;
        int ly = my + 20 + (int) (pull * 40);
        ctx.fill(lx, my + 26, lx + 4, my + 80, GambleArt.argb(a, 0x6E6A64));
        GambleArt.line(ctx, lx + 2, my + 52, lx + 2, ly, 3, GambleArt.argb(a, 0x9E9890));
        GambleArt.disc(ctx, lx + 2, ly, 7, GambleArt.argb(a, 0xD0202E), 1);
        GambleArt.disc(ctx, lx, ly - 2, 2, GambleArt.argb(a, 0xFF9A9A), 1);
        if (all) {
            // jackpot and iron rain at full size, outside the (scaled) machine
            Matrix3x2fStack ps = ctx.pose();
            ps.pushMatrix();
            ps.identity();
            float topY = (cy - 10) - (mh / 2f) * SLOT_SCALE;
            float jt = t - (stopAt + 0.9f + STOP_LEN);
            if (win()) {
                ironRain(ctx, jt, a);
                float sc = jt < 0.2f ? 7.0f - 3.0f * (jt / 0.2f) : 4.0f + 0.25f * (float) Math.sin(jt * 10);
                GambleArt.bigText(ctx, font, "JACKPOT!", cx, topY - 34, sc, 0xFF8A2A, a);
                for (int k = 0; k < 7; k++) {
                    float dl = (jt * 1.5f + k * 0.3f) % 1.0f;
                    int dx = cx - 60 + k * 20;
                    ctx.fill(dx, (int) topY - 22 + (int) (dl * 22), dx + 3, (int) topY - 16 + (int) (dl * 22), GambleArt.argb(a * (1 - dl), 0xFF7A1A));
                }
            } else {
                GambleArt.bigText(ctx, font, "SO CLOSE...", cx, topY - 22, 2.0f, 0x9E9890, a);
            }
            ps.popMatrix();
        }
    }

    /** Glowing iron chunks raining over the screen. */
    private static void ironRain(GuiGraphicsExtractor ctx, float jt, float a) {
        int sw = ctx.guiWidth(), sh = ctx.guiHeight();
        Random r = new Random(77L);
        for (int i = 0; i < 50; i++) {
            float x = r.nextFloat() * sw;
            float delay = r.nextFloat() * 0.8f;
            float y = -10 + (jt - delay) * (110 + r.nextFloat() * 150);
            if (jt < delay || y > sh) continue;
            int s = 4 + i % 3;
            ctx.fill((int) x, (int) y, (int) x + s, (int) y + s, GambleArt.argb(a, i % 3 == 0 ? 0xFF7A1A : 0x9E9890));
            ctx.fill((int) x, (int) y + s, (int) x + s, (int) y + s + 1, GambleArt.argb(a, 0xFFB347));
        }
    }

    /** The hole in Jawbus' chest the machine sits in: broken iron edge glowing with lava. */
    private static void chestCavity(GuiGraphicsExtractor ctx, int cx, int cy, float t, float a) {
        int w = 3 * 56 + 2 * 10 + 2 * 16 + 40, h = SLOT_MACHINE_H + 40;
        int x = cx - w / 2 + 10, y = cy - h / 2;
        ctx.fill(x, y, x + w, y + h, GambleArt.argb(a, 0x1A1612));
        Random r = new Random(5L);
        for (int i = 0; i < 28; i++) {
            int ex = x + r.nextInt(w), ey = r.nextBoolean() ? y + r.nextInt(8) : y + h - r.nextInt(8);
            if (i % 2 == 0) {
                ex = r.nextBoolean() ? x + r.nextInt(8) : x + w - r.nextInt(8);
                ey = y + r.nextInt(h);
            }
            ctx.fill(ex - 3, ey - 3, ex + 4, ey + 4, GambleArt.argb(a, 0x6E6A64));
        }
        float g = 0.6f + 0.4f * (float) Math.sin(t * 4);
        ctx.outline(x + 4, y + 4, w - 8, h - 8, GambleArt.argb(a * g, 0xFF6A1A));
        ctx.outline(x + 6, y + 6, w - 12, h - 12, GambleArt.argb(a * g * 0.5f, 0xFFB347));
    }

    private static void bulb(GuiGraphicsExtractor ctx, int x, int y, float t, int i, float a) {
        boolean on = (i + (int) (t * 10)) % 3 == 0;
        ctx.fill(x, y, x + 4, y + 4, GambleArt.argb(a, on ? 0xFFF3B0 : 0x6A4A18));
        if (on) ctx.fill(x - 1, y - 1, x + 5, y + 5, GambleArt.argb(a * 0.3f, 0xFFF3B0));
    }

    /** Gold coins raining over the whole screen. */
    private static void coins(GuiGraphicsExtractor ctx, float jt, float a) {
        int sw = ctx.guiWidth(), sh = ctx.guiHeight();
        Random r = new Random(77L);
        for (int i = 0; i < 60; i++) {
            float x = r.nextFloat() * sw;
            float delay = r.nextFloat() * 0.8f;
            float sp = 120 + r.nextFloat() * 160;
            float y = -10 + (jt - delay) * sp;
            if (jt < delay || y > sh) continue;
            int w = ((int) ((jt * 8) + i) % 4 < 2) ? 6 : 2;
            ctx.fill((int) x - w / 2, (int) y, (int) x + w / 2 + 1, (int) y + 6, GambleArt.argb(a, 0xFFCC33));
            ctx.fill((int) x - w / 2, (int) y, (int) x + w / 2 + 1, (int) y + 2, GambleArt.argb(a, 0xFFF3B0));
        }
    }

    /** Thunder: fields on a ring, chain lightning runs around, a bolt from the sky strikes the result. */
    private static void drawRoulette(GuiGraphicsExtractor ctx, Font font, int cx, int cy, int r, float t, float a) {
        int n = look.pool.length;
        float p = pos(0, t, speedOf(Game.ROULETTE), stopAt < 0 ? Float.MAX_VALUE : stopAt, STOP_LEN);
        int lit = Math.floorMod((int) Math.floor(p), n);
        tick((int) Math.floor(p), 1.2f);
        float endAt = stopAt < 0 ? Float.MAX_VALUE : stopAt + STOP_LEN;
        boolean stopped = t >= endAt;
        int size = 50;
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) {
            double ang = -Math.PI / 2 + i * 2 * Math.PI / n;
            xs[i] = cx + (int) (Math.cos(ang) * r * 1.15);
            ys[i] = cy + (int) (Math.sin(ang) * r * 0.78);
        }
        long frame = (long) (t * 18);
        // chain lightning between the fields
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            if ((frame + i) % 3 != 0) GambleArt.bolt(ctx, xs[i], ys[i], xs[j], ys[j], a * 0.45f, frame * 31 + i, 0x6FD0FF);
        }
        // the guardian's beam to the lit field
        GambleArt.line(ctx, cx, cy, xs[lit], ys[lit], 5, GambleArt.argb(a * 0.35f, 0x6FD0FF));
        GambleArt.line(ctx, cx, cy, xs[lit], ys[lit], 2, GambleArt.argb(a * 0.9f, 0xE8FAFF));
        for (int i = 0; i < n; i++) {
            GambleArt.prismarineCard(ctx, font, iconOf(look.pool[i]), xs[i], ys[i], size, a, i == lit, t);
        }
        // the strike from the sky
        if (!stopped || ((int) (t * 12)) % 2 == 0) {
            GambleArt.bolt(ctx, xs[lit] + (frame % 7) * 6 - 18, 0, xs[lit], ys[lit] - size / 2, a, frame * 17, 0xFFF36B);
        }
        Random sp = new Random(frame);
        for (int k = 0; k < 10; k++) {
            float ang = sp.nextFloat() * 6.28f, d = 20 + sp.nextFloat() * 22;
            int x = xs[lit] + (int) (Math.cos(ang) * d), y = ys[lit] + (int) (Math.sin(ang) * d);
            ctx.fill(x, y, x + 2, y + 2, GambleArt.argb(a, k % 2 == 0 ? 0xFFF36B : 0xE8FAFF));
        }
        if (stopped) {
            float st = t - endAt;
            if (st < 0.6f) {
                if (st < 0.12f) ctx.fill(0, 0, ctx.guiWidth(), ctx.guiHeight(), GambleArt.argb(a * (1 - st / 0.12f) * 0.6f, 0xE8F4FF));
                for (int b = 0; b < 3; b++) GambleArt.bolt(ctx, xs[lit] - 60 + b * 60, 0, xs[lit], ys[lit], a, (long) (st * 40) * 7 + b, 0xFFF36B);
                GambleArt.ring(ctx, xs[lit], ys[lit], (int) (30 + st * 220), 3, GambleArt.argb(a * (1 - st / 0.6f), 0xBFE9FF), 2);
                sound(15, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
            }
            if (win()) GambleArt.bigText(ctx, font, "STRUCK!", cx, cy - r - 40, 3.0f, 0xFFF36B, a);
        }
    }

    /** Ragnarok: a real wheel of fortune with wedges, a lit rim, spokes, a hub and a pointer at the top. */
    private static void drawWheel(GuiGraphicsExtractor ctx, Font font, int cx, int cy, int r, float t, float a) {
        int n = look.pool.length;
        float p = pos(0, t, speedOf(Game.WHEEL), stopAt < 0 ? Float.MAX_VALUE : stopAt, STOP_LEN);
        tick((int) Math.floor(p + 0.5f), 0.9f);
        float endAt = stopAt < 0 ? Float.MAX_VALUE : stopAt + STOP_LEN;
        boolean stopped = t >= endAt;
        double seg = 2 * Math.PI / n;
        double rot = -(p + 0.5) * seg;
        // flames around it
        for (int i = 0; i < 64; i++) {
            double ang = i * 2 * Math.PI / 64 + t * 0.4;
            float fl = (float) Math.sin(t * 9 + i * 1.7) * 0.5f + 0.5f;
            int rr = r + 16 + (int) (fl * 10);
            int x = cx + (int) (Math.cos(ang) * rr), y = cy + (int) (Math.sin(ang) * rr);
            ctx.fill(x - 2, y - 2, x + 2, y + 2, GambleArt.argb(a * (0.4f + 0.6f * fl), i % 3 == 0 ? 0xFFD15C : 0xFF5A1A));
        }
        // rim, face, spokes
        GambleArt.disc(ctx, cx, cy + 4, r + 12, GambleArt.argb(a * 0.5f, 0x000000), 2);
        GambleArt.ring(ctx, cx, cy, r + 12, 12, GambleArt.argb(a, 0xC99A2E), 2);
        GambleArt.ring(ctx, cx, cy, r + 12, 2, GambleArt.argb(a, 0x6A4A10), 2);
        int[] colors = new int[n];
        for (int i = 0; i < n; i++) {
            Sym s = look.pool[i];
            colors[i] = s == NONE ? 0x161010 : (s == OTHER ? 0x6A5010 : (i % 2 == 0 ? 0x8A1A1A : 0x3A0C0C));
        }
        GambleArt.wheel(ctx, cx, cy, r, n, rot, colors, a, 2);
        for (int i = 0; i < n; i++) {
            double ang = rot + i * seg;
            GambleArt.line(ctx, cx, cy, cx + (float) Math.sin(ang) * r, cy - (float) Math.cos(ang) * r, 2, GambleArt.argb(a, 0xE0B84A));
        }
        // rim bulbs
        for (int i = 0; i < 18; i++) {
            double ang = i * 2 * Math.PI / 18;
            int x = cx + (int) (Math.sin(ang) * (r + 6)), y = cy - (int) (Math.cos(ang) * (r + 6));
            boolean on = stopped ? ((int) (t * 8) % 2 == 0) : (i + (int) (t * 12)) % 3 == 0;
            GambleArt.disc(ctx, x, y, 3, GambleArt.argb(a, on ? 0xFFF3B0 : 0x7A5A20), 1);
        }
        // icons on the wedges
        for (int i = 0; i < n; i++) {
            double ang = rot + (i + 0.5) * seg;
            float ix = cx + (float) Math.sin(ang) * r * 0.66f;
            float iy = cy - (float) Math.cos(ang) * r * 0.66f;
            boolean top = stopped && Math.floorMod(Math.round(p), n) == i;
            if (top) GambleArt.disc(ctx, (int) ix, (int) iy, 20, GambleArt.argb(a * (0.5f + 0.3f * (float) Math.sin(t * 12)), 0xFFD24A), 2);
            GambleArt.item(ctx, font, iconOf(look.pool[i]), ix, iy, 28, a);
        }
        // hub
        GambleArt.disc(ctx, cx, cy, 16, GambleArt.argb(a, 0xC99A2E), 1);
        GambleArt.disc(ctx, cx, cy, 10, GambleArt.argb(a, 0x3A0C10), 1);
        GambleArt.disc(ctx, cx - 3, cy - 3, 3, GambleArt.argb(a, 0xFFE7A0), 1);
        // pointer (flicks when a wedge passes)
        float frac = (float) ((p + 0.5) - Math.floor(p + 0.5));
        int tilt = stopped ? 0 : (int) ((1 - frac) * (frac < 0.25f ? 6 : 0));
        int py = cy - r - 22;
        for (int row = 0; row < 22; row++) {
            int half = 11 - row / 2;
            ctx.fill(cx - half - 1 + tilt, py + row, cx + half + 2 + tilt, py + row + 1, GambleArt.argb(a, 0x2A1400));
            ctx.fill(cx - half + tilt, py + row, cx + half + 1 + tilt, py + row + 1, GambleArt.argb(a, row < 4 ? 0xFFE7A0 : 0xFFCC33));
        }
        if (stopped && win()) GambleArt.bigText(ctx, font, "WINNER!", cx, cy + r + 30, 3.0f, 0xFFCC33, a);
    }

    /** Plhlegblast: wobbling jelly bubbles on its tentacles; a tentacle slaps each one until only the result is left. */
    private static void drawBubbles(GuiGraphicsExtractor ctx, Font font, int cx, int cy, int r, float t, float a, int poolTop) {
        int n = look.pool.length;
        float p = t * speedOf(Game.BUBBLES);
        int lit = Math.floorMod((int) Math.floor(p), n);
        int br = 26;
        int popped = 0;
        float gapT = 0.45f;
        float lastPop = stopAt + 0.45f + (n - 2) * gapT;
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) {
            double ang = -Math.PI / 2 + i * 2 * Math.PI / n + Math.sin(t * 0.7) * 0.15;
            xs[i] = cx + (int) (Math.cos(ang) * r * 1.2);
            ys[i] = cy - r / 3 + (int) (Math.sin(ang) * r * 0.75 + Math.sin(t * 2.2 + i) * 5);
        }
        for (int i = 0; i < n; i++) {
            int x = xs[i], y = ys[i];
            int ox = x + (i % 2 == 0 ? -18 : 18), oy = poolTop + 6;
            boolean isResult = i == resultIdx;
            float reach = clamp((t - GAMBLE_AT) / 0.5f);
            boolean alive = true;
            boolean slap = false;
            if (stopAt >= 0 && !isResult) {
                int order = popped++;
                float popAt = stopAt + 0.45f + order * gapT;
                if (t >= popAt - 0.3f && t < popAt) slap = true;
                if (t >= popAt) {
                    alive = false;
                    float pt = t - popAt;
                    reach = Math.max(0, 1.0f - pt / 0.4f);
                    if (pt < 0.5f) {
                        sound(5 + Math.min(9, order), SoundEvents.SLIME_SQUISH, 1.2f + order * 0.08f, 0.6f);
                        for (int k = 0; k < 12; k++) {
                            double d = k * 0.52;
                            float dd = 20 + pt * 90;
                            int px = x + (int) (Math.cos(d) * dd), py = y + (int) (Math.sin(d) * dd + pt * pt * 120);
                            int sz = 3 + k % 3;
                            GambleArt.disc(ctx, px, py, sz, GambleArt.argb(a * (1 - pt * 2), k % 2 == 0 ? 0x0A0A14 : 0x78FF5A), 1);
                        }
                    }
                }
            }
            if (slap) {
                float u = clamp((t - (stopAt + 0.45f + (popped - 1) * gapT - 0.3f)) / 0.3f);
                tentacle(ctx, ox, oy, x + (i % 2 == 0 ? 30 : -30) * (1 - u), y - br * u, 1.0f, a, t, i, true);
            } else {
                tentacle(ctx, ox, oy, x, alive ? y + br - 2 : y, reach, a, t, i, false);
            }
            if (!alive) continue;
            boolean on = stopAt < 0 ? i == lit : (isResult && t >= lastPop + 0.35f);
            if (stopAt < 0) tick((int) Math.floor(p), 1.4f);
            int w = (int) (Math.sin(t * 5 + i) * 3);
            int rr = br + (on ? 4 + (int) (Math.sin(t * 14) * 2) : 0);
            GambleArt.ellipse(ctx, x, y, rr + w, rr - w, GambleArt.argb(a * 0.24f, on ? 0xFFE7A0 : 0x78FF5A), 2);
            GambleArt.ellipseRing(ctx, x, y, rr + w, rr - w, 2, GambleArt.argb(a * 0.9f, on ? 0xFFCC33 : 0x9CFF6B));
            GambleArt.disc(ctx, x - rr / 3, y - rr / 3, Math.max(2, rr / 6), GambleArt.argb(a * 0.7f, 0xFFFFFF), 1);
            GambleArt.item(ctx, font, iconOf(look.pool[i]), x, y, 30, a);
        }
        if (stopAt >= 0 && t >= lastPop + 0.35f && win()) {
            GambleArt.bigText(ctx, font, "POP!", cx, cy - r - 30, 3.0f, 0x9CFF6B, a);
        }
    }

    /** A squid tentacle (vanilla squid colour) rising out of the lava at (x0,y0) toward (x1,y1); reach 0..1. */
    private static void tentacle(GuiGraphicsExtractor ctx, float x0, float y0, float x1, float y1, float reach, float a, float t, int i, boolean lash) {
        if (reach <= 0.02f) return;
        int segs = 18;
        float px = x0, py = y0;
        for (int k = 1; k <= segs; k++) {
            float u = k / (float) segs;
            if (u > reach) break;
            float sway = (float) Math.sin(t * (lash ? 9 : 3) + i * 1.7f + u * 5) * (lash ? 6 : 10) * u;
            float bend = (float) Math.sin(u * Math.PI) * (i % 2 == 0 ? -22 : 22);
            float x = x0 + (x1 - x0) * u + bend + sway;
            float y = y0 + (y1 - y0) * u;
            int th = Math.max(3, (int) (12 - u * 8));
            // the squid's own tentacle texture is one dark blue (18,39,55); light from the top-left
            GambleArt.line(ctx, px, py, x, y, th, GambleArt.argb(a, 0x122737));
            GambleArt.line(ctx, px - th / 4f, py, x - th / 4f, y, Math.max(1, th / 3), GambleArt.argb(a, 0x1E3A52));
            GambleArt.line(ctx, px + th / 3f, py, x + th / 3f, y, Math.max(1, th / 4), GambleArt.argb(a, 0x0A1824));
            px = x;
            py = y;
        }
        ctx.fill((int) x0 - 10, (int) y0 - 3, (int) x0 + 10, (int) y0, GambleArt.argb(a * 0.8f, 0xFFB347));
    }

    /**
     * Ragnarok: like Titanoboa - you raise the Titanoboa rifle and look through its scope. The camera follows
     * Ragnarok galloping on its skeleton horse through the Nether; every shot misses until your real drop is
     * known, then the deciding shot hits (it falls, the drop flies up) or misses (it escapes). The bounty board
     * at the top shows its real drops.
     */
    private static void drawShoot(GuiGraphicsExtractor ctx, Font font, float t, float a, int sw, int sh, float settled, float burstAt) {
        float aimPt = t - GAMBLE_AT;
        float aimT = com.skymaxx.tiki.render.boa.LightRifle.AIM_TIME;
        boolean inScope = aimPt >= aimT;
        float lx = com.skymaxx.tiki.render.boa.LightRifle.lensX(sw);
        float ly = com.skymaxx.tiki.render.boa.LightRifle.lensY(sh);
        float lr = com.skymaxx.tiki.render.boa.LightRifle.lensR(sh);
        float zoom = inScope ? 1.4f : 1.0f;
        int horseBase = (int) (sh * 0.28f * zoom);
        // in the scope the rider (not the horse) sits in the middle of the cross hairs
        int ground = inScope ? (int) Math.min(ly + lr * 0.95f, ly + horseBase * 0.95f) : (int) (sh * 0.80f);
        float scroll = t * (inScope ? 260 : 140);

        // the Nether, scrolling past (the camera follows the rider)
        ctx.fillGradient(0, 0, sw, ground, argb(a * 0.95f, 0x2A0505), argb(a * 0.95f, 0x6A1A0A));
        for (int i = 0; i < sw / 90 + 2; i++) {
            float x = ((i * 90 - scroll * 0.1f) % (sw + 90) + sw + 90) % (sw + 90) - 45;
            int len = (int) ((40 + (i % 3) * 18) * zoom);
            ctx.fill((int) x, 0, (int) x + 3, len, argb(a * 0.6f, 0x7A3A10));
            ctx.fill((int) x - 4, len, (int) x + 7, len + 8, argb(a, 0xFFD27A));
        }
        hills(ctx, scroll * 0.25f, ground - (int) (50 * zoom), (int) (60 * zoom), 0x4A1010, 0.012f / zoom, a, sw, ground);
        for (int i = 0; i < 5; i++) {
            float x = ((i * 210 - scroll * 0.4f) % (sw + 80) + sw + 80) % (sw + 80) - 40;
            ctx.fillGradient((int) x, ground - (int) (120 * zoom), (int) x + (int) (10 * zoom), ground, argb(a, 0xFFB347), argb(a, 0xFF5A1A));
        }
        hills(ctx, scroll * 0.7f, ground + 5, (int) (36 * zoom), 0x6E1E14, 0.02f / zoom, a, sw, ground + 10);
        ctx.fill(0, ground, sw, sh, argb(a, 0x7A1A10));
        for (int i = 0; i < sw / 30 + 2; i++) {
            float x = ((i * 53 - scroll) % (sw + 30) + sw + 30) % (sw + 30) - 15;
            ctx.fill((int) x, ground + 6 + (i % 3) * 12, (int) x + (int) (14 * zoom), ground + 12 + (i % 3) * 12, argb(a, 0x5A120A));
        }
        for (int i = 0; i < sw / 60 + 2; i++) {
            float x = ((i * 71 - scroll * 0.9f) % (sw + 40) + sw + 40) % (sw + 40) - 20;
            float f = (float) Math.sin(t * 12 + i) * 0.5f + 0.5f;
            ctx.fill((int) x, ground - 8 - (int) (f * 8), (int) x + 4, ground, argb(a * (0.4f + 0.4f * f), 0x50C8FF));
        }
        for (int i = 0; i < 40; i++) {
            float l = (t * 0.4f + i * 0.11f) % 1.0f;
            int ex = (int) (((i * 67 - scroll * 0.5f) % sw + sw) % sw);
            ctx.fill(ex, ground - (int) (l * ground), ex + 2, ground - (int) (l * ground) + 2, argb(a * (1 - l), 0xFFB450));
        }

        // shots: while waiting one every 1.4 s (at most 5, all misses); once the result is known see SHOT_AT
        float scopeAt = GAMBLE_AT + aimT;
        boolean decided = finalShot >= 0 && t >= finalShot;
        int lastIdx = -1;
        for (int k = 0; k < BULLETS; k++) {
            if (finalShot >= 0 && hitShot >= 0 && k > hitShot) break; // hit = no more shots
            float at = finalShot >= 0 ? SHOT_AT[k] : (k < BULLETS - 1 ? preShot(k, scopeAt) : Float.MAX_VALUE);
            if (inScope && at <= t) lastIdx = k;
        }
        float shotT = lastIdx < 0 ? -9f : (finalShot >= 0 ? SHOT_AT[lastIdx] : preShot(lastIdx, scopeAt));
        int shotIdx = lastIdx;
        boolean finalFired = decided;
        boolean missing = lastIdx >= 0 && !(finalFired && t - finalShot < 9f && shotT == finalShot);
        float since = lastIdx >= 0 ? t - shotT : 9f;
        float kick = since < 0.3f ? 1.0f - since / 0.3f : 0.0f;
        float shakeX = kick * 6 * (RNG.nextFloat() - 0.5f), shakeY = -kick * 8;

        // the rider, followed by the camera
        boolean hit = decided && win();
        boolean escaped = decided && !win();
        float drift = (float) Math.sin(t * 1.3f) * (inScope ? lr * 0.25f : sw * 0.05f);
        float jump = missing && t - shotT < 0.45f ? (float) Math.sin((t - shotT) / 0.45f * Math.PI) * 26 * zoom : 0;
        int horsePx = (int) (sh * 0.28f * zoom);
        float gallop = (float) Math.abs(Math.sin(t * 9)) * 4 * zoom;
        float baseX = (inScope ? lx : sw * 0.45f) + drift;
        float hx = baseX;
        float hy = ground - horsePx / 2f - gallop - jump;
        float ft = decided ? t - finalShot : 0;
        if (hit) hx += ft * ft * 500;
        if (escaped) hx += ft * ft * 420;
        if (t >= RISE_AT && MobView.available()) {
            ctx.nextStratum();
            if (horse != null) MobView.drawPosed(ctx, horse, (int) (hx + shakeX), (int) (hy + shakeY), horsePx, -90.0f, 0.0f, 0.0f);
            if (mob != null) {
                float rh = MobView.height(mob) / Math.max(0.5f, horse != null ? MobView.height(horse) : 1.6f);
                int riderPx = (int) (horsePx * rh);
                float rx = hit ? baseX : hx;
                float ry = hy - horsePx * 0.45f + (hit ? ft * ft * 520 - ft * 60 : 0);
                if (!hit || ry < ground + riderPx) MobView.drawPosed(ctx, mob, (int) (rx + shakeX), (int) (ry + shakeY), riderPx, -90.0f, hit ? ft * 120.0f : 0.0f, 0.0f);
            }
            ctx.nextStratum();
        }

        // muzzle flash + sound
        if ((missing && t - shotT < 0.07f) || (decided && ft < 0.1f)) {
            ctx.fill(0, 0, sw, sh, argb(a * 0.45f, 0xFFF0C8));
        }
        if (lastIdx >= 0 && since < 0.05f) sound(10 + Math.min(5, lastIdx), SoundEvents.GENERIC_EXPLODE.value(), 1.9f, 0.3f);
        if (decided && ft < 0.05f) sound(9, SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.45f);

        // the rifle (raise + zoom), then the scope
        boolean rifleOk = com.skymaxx.tiki.render.boa.LightRifle.ready();
        if (t >= GAMBLE_AT && !inScope) {
            ctx.nextStratum();
            com.skymaxx.tiki.render.boa.LightRifle.aim(ctx, sw, sh, aimPt, a);
        }
        if (inScope && t < burstAt) {
            ctx.nextStratum();
            int ret = hit ? 0xFFCC33 : (escaped ? 0x7A7470 : 0x0C0C0D);
            if (rifleOk) {
                com.skymaxx.tiki.render.boa.LightRifle.scope(ctx, sw, sh, shakeX, shakeY, argb(a, ret));
            } else {
                int col = argb(a, 0xFF3B3B);
                GambleArt.ring(ctx, (int) lx, (int) ly, (int) lr, 3, col, 2);
                ctx.fill((int) (lx - lr), (int) ly - 1, (int) (lx + lr), (int) ly + 1, col);
                ctx.fill((int) lx - 1, (int) (ly - lr), (int) lx + 1, (int) (ly + lr), col);
            }
            if (missing && t - shotT < 0.6f) GambleArt.bigText(ctx, font, "MISS", lx + lr * 0.45f, ly - lr * 0.55f, 2.0f, 0xC8C2BA, a);
            if (hit) {
                GambleArt.ring(ctx, (int) lx, (int) ly, (int) (10 + ft * 160), 3, argb(a * clamp(1 - ft), 0xFF3B3B), 2);
                float sc = ft < 0.15f ? 6.0f - 2.0f * ft / 0.15f : 4.0f;
                GambleArt.bigText(ctx, font, "HIT!", lx, ly - lr * 0.4f, sc, 0xFFCC33, a);
                GambleArt.item(ctx, font, iconOf(look.pool[resultIdx]), lx, Math.max(ly - lr * 0.1f, ground - horsePx - ft * 80), 40, a);
            } else if (escaped) {
                if (ft < 0.6f) GambleArt.bigText(ctx, font, "MISS", lx + lr * 0.45f, ly - lr * 0.55f, 2.0f, 0xC8C2BA, a);
                if (ft > 0.6f) GambleArt.bigText(ctx, font, "ESCAPED", lx, ly - lr * 0.4f, 3.0f, 0x9E9890, a);
            }
            if (finalShot < 0 && stopAt < 0 && t > GAMBLE_AT + MIN_SPIN && !test) {
                String dots = ".".repeat(1 + (int) (t * 3) % 3);
                ctx.centeredText(font, Component.literal("Waiting for your drop" + dots), (int) lx, (int) (ly + lr + 4), argb(a * 0.8f, 0xF3D9C9));
            }
        }

        // bullets, like Titanoboa: one goes with every shot, the casing flies out
        if (t >= GAMBLE_AT) {
            int fired = lastIdx + 1;
            int total = BULLETS;
            int bx0 = sw - 16 - total * 9, by0 = sh - 30;
            for (int i = 0; i < total; i++) {
                boolean spent = i < fired;
                int x = bx0 + i * 9;
                ctx.fill(x, by0 + 4, x + 6, by0 + 20, argb(a, spent ? 0x3A3630 : 0xC9A23A));
                ctx.fill(x + 1, by0, x + 5, by0 + 5, argb(a, spent ? 0x2A2622 : 0xB87333));
                ctx.fill(x, by0 + 18, x + 6, by0 + 20, argb(a, spent ? 0x2A2622 : 0x8A6A20));
            }
            if (since < 0.5f) {
                float ex = sw - 60 + since * 140, ey = sh - 60 - since * 160 + since * since * 700;
                ctx.fill((int) ex, (int) ey, (int) ex + 3, (int) ey + 8, argb(a, 0xC9A23A));
            }
        }

        // bounty board on top
        int n = look.pool.length;
        int cw = 30, bw = n * (cw + 8) + 12;
        int bx = sw / 2 - bw / 2, by = 4;
        ctx.fill(bx, by, bx + bw, by + cw + 20, argb(a * 0.88f, 0x140604));
        ctx.outline(bx, by, bw, cw + 20, argb(a, 0xFF3B3B));
        ctx.centeredText(font, Component.literal("BOUNTY").withStyle(ChatFormatting.BOLD), sw / 2, by + 3, argb(a, 0xFFCC33));
        for (int i = 0; i < n; i++) {
            GambleArt.card(ctx, font, iconOf(look.pool[i]), bx + 6 + i * (cw + 8) + cw / 2, by + 14 + cw / 2, cw, a, hit && i == resultIdx, t, look.pool[i].color);
        }
        if (t >= settled && !SOUNDS[4]) {
            if (win()) sound(4, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.55f);
            else sound(4, SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, 0.6f);
        }
    }

    // ===================================================================================================
    // Spooky Festival + Season of Jerry (live, still marked as test)
    // ===================================================================================================

    private static final float CLAW_GRAB = 2.0f;
    private static final float SWAP_T = 0.38f;
    private static final int[] SWAPS = new int[64];
    private static int lastFlap = -1;

    private static float ease(float u) {
        u = clamp(u);
        return 1.0f - (1.0f - u) * (1.0f - u) * (1.0f - u);
    }

    private static float wrap(float v, float m) {
        return ((v % m) + m) % m;
    }

    private static void snowfall(GuiGraphicsExtractor ctx, float t, float a, int sw, int sh, int count, float wind, int seed) {
        for (int i = 0; i < count; i++) {
            int k = i + seed * 131;
            float sp = 16 + (k * 37 % 40);
            float y = wrap(k * 53 + t * sp, sh + 10) - 5;
            float x = wrap(k * 97 + t * wind * (0.5f + (k % 5) * 0.2f) + (float) Math.sin(t * 1.3f + k) * 8, sw + 10) - 5;
            int s = k % 7 == 0 ? 3 : (k % 3 == 0 ? 2 : 1);
            ctx.fill((int) x, (int) y, (int) x + s, (int) y + s, argb(a * (0.5f + 0.15f * (k % 4)), 0xFFFFFF));
        }
    }

    private static void pine(GuiGraphicsExtractor ctx, int x, int base, int h, int color, int snow, float a) {
        int layers = 4;
        for (int l = 0; l < layers; l++) {
            int top = base - h + l * h / (layers + 1);
            int bot = top + h / 3 + 4;
            int lw = (h / 2) * (l + 2) / (layers + 1);
            for (int yy = top; yy < bot; yy += 3) {
                int half = (int) (lw * (float) (yy - top) / Math.max(1, bot - top));
                boolean cap = (yy - top) < (bot - top) * 0.35f;
                ctx.fill(x - half, yy, x + half + 1, yy + 3, argb(a, cap ? snow : color));
            }
        }
        ctx.fill(x - 2, base - h / 6, x + 2, base, argb(a, 0x3A2616));
    }

    private static void peak(GuiGraphicsExtractor ctx, int x, int base, int h, int w, int rock, int snow, float a) {
        for (int yy = 0; yy < h; yy += 2) {
            int hw = w * yy / Math.max(1, h) / 2;
            boolean cap = yy < h * 0.32f + Math.sin(x + yy) * 3;
            ctx.fill(x - hw, base - h + yy, x + hw + 1, base - h + yy + 2, argb(a, cap ? snow : rock));
        }
    }

    /** A thick line made of few, larger blocks (cheap: about one fill per 3 pixels). */
    private static void fastLine(GuiGraphicsExtractor ctx, float x0, float y0, float x1, float y1, int th, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        int steps = (int) Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)) / 3.0f);
        int s = Math.max(2, th) + 1;
        for (int i = 0; i <= steps; i++) {
            float u = i / (float) steps;
            int x = (int) (x0 + dx * u), y = (int) (y0 + dy * u);
            ctx.fill(x - s / 2, y - s / 2, x - s / 2 + s, y - s / 2 + s, color);
        }
    }

    private static void deadTree(GuiGraphicsExtractor ctx, int x, int base, int h, int color, float a, long seed) {
        Random r = new Random(seed);
        int c = argb(a, color);
        ctx.fill(x - 2, base - h, x + 3, base, c);
        for (int i = 0; i < 5; i++) {
            float y = base - h * (0.35f + 0.6f * r.nextFloat());
            float dir = r.nextBoolean() ? 1 : -1;
            float len = h * (0.18f + 0.25f * r.nextFloat());
            float ex = x + dir * len, ey = y - len * 0.6f;
            fastLine(ctx, x, y, ex, ey, 2, c);
            fastLine(ctx, ex, ey, ex + dir * len * 0.3f, ey - len * 0.45f, 1, c);
        }
    }

    private static void bat(GuiGraphicsExtractor ctx, float x, float y, float t, float a, int i) {
        float f = (float) Math.sin(t * 18 + i) * 3;
        int c = argb(a, 0x050308);
        ctx.fill((int) x - 1, (int) y - 1, (int) x + 2, (int) y + 2, c);
        fastLine(ctx, x, y, x - 6, y - 2 - f, 1, c);
        fastLine(ctx, x - 6, y - 2 - f, x - 9, y + 1, 1, c);
        fastLine(ctx, x + 1, y, x + 7, y - 2 - f, 1, c);
        fastLine(ctx, x + 7, y - 2 - f, x + 10, y + 1, 1, c);
    }

    private static void waiting(GuiGraphicsExtractor ctx, Font font, float t, float a, int x, int y) {
        if (stopAt < 0 && t > GAMBLE_AT + MIN_SPIN && !test) {
            String dots = ".".repeat(1 + (int) (t * 3) % 3);
            ctx.centeredText(font, Component.literal("Waiting for your drop" + dots), x, y, argb(a * 0.85f, 0xF3F0FF));
        }
    }

    private static void endSound(float t, float settled) {
        if (t >= settled && !SOUNDS[4]) {
            if (win()) sound(4, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.55f);
            else sound(4, SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, 0.6f);
        }
    }

    /** The mob breaks apart into pieces of its own colors. */
    private static void burstPieces(GuiGraphicsExtractor ctx, float t, float burstAt, float a, int cx, int cy, int floor, int flash) {
        if (t < burstAt) return;
        float bt = t - burstAt;
        if (bt < 0.15f) ctx.fill(0, 0, ctx.guiWidth(), ctx.guiHeight(), argb((1.0f - bt / 0.15f) * 0.4f * a, flash));
        for (int i = 0; i < CHUNKS; i++) {
            float x = cx + CVX[i] * bt;
            float y = cy + CVY[i] * bt + 260.0f * bt * bt;
            if (y > floor) continue;
            int s = CSIZE[i];
            ctx.fill((int) x, (int) y, (int) x + s, (int) y + s, argb(a, CCOL[i]));
        }
    }

    // ---------------------------------------------------------------- Grim Reaper: the graveyard

    private static void drawGraves(GuiGraphicsExtractor ctx, Font font, float t, float a, int sw, int sh, float settled, float burstAt) {
        int ground = (int) (sh * 0.80f);
        // night sky, stars, blood moon, clouds, lightning
        ctx.fillGradient(0, 0, sw, ground, argb(a * 0.97f, 0x030108), argb(a * 0.97f, 0x1C0E24));
        for (int i = 0; i < 45; i++) {
            int x = (i * 131) % Math.max(1, sw), y = (i * 71) % Math.max(1, ground / 2);
            float tw = 0.25f + 0.25f * (float) Math.sin(t * 2 + i);
            ctx.fill(x, y, x + 1, y + 1, argb(a * tw, 0xC8C0E0));
        }
        int mx = (int) (sw * 0.80f), my = (int) (sh * 0.19f), mr = Math.max(12, (int) (sh * 0.08f));
        for (int g = 4; g >= 1; g--) GambleArt.disc(ctx, mx, my, mr + g * 7, argb(a * 0.07f, 0xC02818), 2);
        GambleArt.disc(ctx, mx, my, mr, argb(a, 0xE2CDAE), 2);
        GambleArt.disc(ctx, mx - mr / 3, my - mr / 4, mr / 5, argb(a * 0.5f, 0xB89878), 1);
        GambleArt.disc(ctx, mx + mr / 3, my + mr / 3, mr / 6, argb(a * 0.5f, 0xB89878), 1);
        GambleArt.disc(ctx, mx, my, mr, argb(a * 0.22f, 0xC02010), 2);
        for (int i = 0; i < 4; i++) {
            float x = wrap(i * 230 + t * 14, sw + 300) - 150;
            int y = (int) (sh * 0.12f + i * 14);
            ctx.fill((int) x, y, (int) x + 170, y + 6, argb(a * 0.6f, 0x100818));
            ctx.fill((int) x + 25, y - 4, (int) x + 125, y, argb(a * 0.6f, 0x100818));
        }
        float lf = t % 5.3f;
        if (t > 0.6f && (lf < 0.08f || (lf > 0.16f && lf < 0.2f))) {
            ctx.fill(0, 0, sw, ground, argb(a * 0.2f, 0xD8D0FF));
            GambleArt.bolt(ctx, sw * 0.35f, 0, sw * 0.38f, ground * 0.55f, a * 0.8f, (long) (t / 5.3f), 0xE8E0FF);
        }
        // far hills, chapel with flickering windows, dead trees, bats
        hills(ctx, 0, ground - (int) (sh * 0.10f), (int) (sh * 0.06f), 0x0A0612, 0.008f, a, sw, ground);
        int chx = (int) (sw * 0.66f), chb = ground - (int) (sh * 0.08f);
        int sil = argb(a, 0x07040C);
        ctx.fill(chx - 30, chb - 40, chx + 30, chb + 5, sil);
        for (int yy = 0; yy < 24; yy += 2) {
            int hw = 32 * yy / 24 + 2;
            ctx.fill(chx - hw, chb - 64 + yy, chx + hw, chb - 62 + yy, sil);
        }
        ctx.fill(chx - 5, chb - 92, chx + 5, chb - 60, sil);
        ctx.fill(chx - 1, chb - 106, chx + 1, chb - 90, sil);
        ctx.fill(chx - 5, chb - 101, chx + 5, chb - 99, sil);
        float glow = 0.55f + 0.45f * (float) (Math.sin(t * 7) * Math.sin(t * 3.1f));
        ctx.fill(chx - 14, chb - 30, chx - 7, chb - 18, argb(a * glow, 0xFF8A20));
        ctx.fill(chx + 7, chb - 30, chx + 14, chb - 18, argb(a * glow, 0xFF8A20));
        ctx.fill(chx - 2, chb - 84, chx + 2, chb - 78, argb(a * glow, 0xFF8A20));
        deadTree(ctx, (int) (sw * 0.06f), ground, (int) (sh * 0.46f), 0x060309, a, 11);
        deadTree(ctx, (int) (sw * 0.95f), ground, (int) (sh * 0.38f), 0x060309, a, 23);
        deadTree(ctx, (int) (sw * 0.40f), ground - (int) (sh * 0.06f), (int) (sh * 0.20f), 0x0A0610, a * 0.9f, 37);
        for (int i = 0; i < 7; i++) {
            float bx = wrap(i * 170 + t * (40 + i * 9), sw + 60) - 30;
            float by = sh * 0.12f + i * sh * 0.045f + (float) Math.sin(t * 2 + i) * 10;
            bat(ctx, bx, by, t, a, i);
        }
        // ground, grass, iron fence
        ctx.fill(0, ground, sw, sh, argb(a, 0x0D090F));
        for (int x = 0; x < sw; x += 6) {
            int gh = 2 + (int) (Math.abs(Math.sin(x * 0.37)) * 4);
            ctx.fill(x, ground - gh, x + 3, ground, argb(a, 0x14201A));
        }
        int fenceTop = ground - (int) (sh * 0.09f);
        for (int x = 6; x < sw; x += 14) {
            ctx.fill(x, fenceTop, x + 2, ground - 2, argb(a, 0x1C1822));
            ctx.fill(x - 1, fenceTop - 3, x + 3, fenceTop, argb(a, 0x2A2430));
        }
        ctx.fill(0, fenceTop + 6, sw, fenceTop + 8, argb(a, 0x1C1822));
        ctx.fill(0, ground - 10, sw, ground - 8, argb(a, 0x1C1822));
        // jack o'lanterns with a candle glow
        for (int i = 0; i < 3; i++) {
            int jx = (int) (sw * (0.30f + 0.33f * i)), jy = ground + (int) (sh * 0.13f) + (i % 2) * 6;
            float f = 0.5f + 0.5f * (float) Math.sin(t * 9 + i * 2);
            GambleArt.disc(ctx, jx, jy, 16, argb(a * 0.12f * (0.6f + 0.4f * f), 0xFF8A20), 2);
            GambleArt.item(ctx, font, GambleArt.icon("PUMPKIN"), jx, jy, 20, a);
        }
        if (t < 1.2f) sound(5, SoundEvents.WITHER_SKELETON_AMBIENT, 0.55f, 0.45f);

        // the tombstones (one per field)
        Sym[] pool = look.pool;
        int n = pool.length;
        int gap = 8;
        int tw = Math.max(26, Math.min(40, (int) (sw * 0.52f / n) - gap));
        int th = (int) (tw * 1.45f);
        int rowW = n * tw + (n - 1) * gap;
        int rx0 = Math.max((int) (sw * 0.34f), (int) (sw * 0.62f) - rowW / 2);
        int tb = ground + (int) (sh * 0.07f);
        float ga = clamp((t - GAMBLE_AT) / 0.4f) * a;
        boolean on = t >= GAMBLE_AT;
        float p = pos(0, t, speedOf(Game.GRAVES), stopAt < 0 ? Float.MAX_VALUE : stopAt, STOP_LEN);
        int lit = Math.floorMod(Math.round(p), n);
        boolean stopped = stopAt >= 0 && t >= stopAt + STOP_LEN;
        float ot = stopped ? t - (stopAt + STOP_LEN) : -1;
        if (on && !stopped) tick(Math.round(p), 0.6f);
        if (on) {
            for (int i = 0; i < n; i++) {
                int x = rx0 + i * (tw + gap) + tw / 2;
                tomb(ctx, font, x, tb, tw, th, iconOf(pool[i]), ga, i == lit && !stopped, stopped && i == resultIdx, ot, t);
            }
        }
        // the 3D Grim Reaper floats on the left, rising out of the grave fog
        int rpx = (int) (sh * 0.50f);
        int rcx = (int) (sw * 0.19f);
        int rStand = ground - rpx / 2 + (int) (sh * 0.03f);
        if (t >= RISE_AT && t < burstAt && mob != null && MobView.available()) {
            float rise = ease((t - RISE_AT) / (GAMBLE_AT - RISE_AT));
            int cy = (int) (ground + rpx * 0.6f - (ground + rpx * 0.6f - rStand) * rise + Math.sin(t * 1.6f) * 5);
            float shake = t >= settled ? 2 + 4 * clamp((t - settled) / Math.max(0.01f, burstAt - settled)) : 0;
            mob.hurtTime = t >= settled && ((int) (t * 12) % 2 == 0) ? 8 : 0;
            ctx.nextStratum();
            MobView.drawPosed(ctx, mob, rcx + (int) (shake * (RNG.nextFloat() * 2 - 1)), cy, rpx, -55.0f, -6.0f, 18.0f);
            ctx.nextStratum();
        }
        // grave fog in front
        for (int i = 0; i < 6; i++) {
            float x = wrap(i * 190 - t * (10 + i * 3), sw + 260) - 130;
            int y = ground + (int) (sh * 0.02f) + (i % 3) * (int) (sh * 0.04f);
            ctx.fill((int) x, y, (int) x + 220, y + 8, argb(a * 0.13f, 0x9A8AB8));
            ctx.fill((int) x + 30, y - 5, (int) x + 170, y, argb(a * 0.09f, 0x9A8AB8));
        }
        // the scythe: sweeps over the stones, then slashes the chosen one open
        if (on && t < burstAt) {
            float sx = rx0 + tw / 2.0f + wrap(p, n) * (tw + gap);
            if (stopped) sx = rx0 + tw / 2.0f + resultIdx * (tw + gap);
            float sy = tb - th - 34;
            float ang = (float) Math.sin(t * 9) * 0.3f;
            float sa = ga;
            if (stopped) {
                ang = ot < 0.25f ? -0.9f + ot / 0.25f * 1.9f : 1.0f;
                sy += Math.min(ot, 0.25f) / 0.25f * 18;
                sa = ga * clamp(1.0f - (ot - 0.5f) / 0.4f);
                if (ot < 0.1f) sound(6, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 0.7f);
            }
            if (sa > 0.02f) scythe(ctx, sx + 22, sy, ang, 1.0f, sa);
        }
        // souls rising
        for (int i = 0; i < 26; i++) {
            float l = wrap(t * 0.35f + i * 0.13f, 1.0f);
            float x = rx0 + wrap(i * 61, Math.max(1, rowW)) + (float) Math.sin(t * 2 + i) * 6;
            ctx.fill((int) x, (int) (tb - l * sh * 0.55f), (int) x + 2, (int) (tb - l * sh * 0.55f) + 2, argb(ga * 0.6f * (1 - l), 0x78FFC8));
        }
        if (stopped && ot > 0.3f) {
            float rt = ot - 0.3f;
            int x = rx0 + resultIdx * (tw + gap) + tw / 2;
            String msg = win() ? "HARVESTED!" : "THE SOUL ESCAPES";
            float sc = win() ? (rt < 0.15f ? 5.0f - 2.0f * rt / 0.15f : 3.0f) : 2.0f;
            GambleArt.bigText(ctx, font, msg, Math.max(sw * 0.3f, Math.min(sw * 0.7f, x)), tb - th - 70, sc, win() ? 0x78FFC8 : 0x9E9890, a * clamp(rt / 0.2f));
            if (rt < 0.05f) sound(7, win() ? SoundEvents.SOUL_ESCAPE.value() : SoundEvents.NOTE_BLOCK_BASS.value(), win() ? 1.2f : 0.5f, 0.6f);
        }
        burstPieces(ctx, t, burstAt, a, rcx, rStand, ground + 10, 0x78FFC8);
        waiting(ctx, font, t, ga, (int) (sw * 0.62f), sh - 14);
        endSound(t, settled);
    }

    private static void tomb(GuiGraphicsExtractor ctx, Font font, int x, int base, int w, int h, ItemStack icon, float a, boolean lit, boolean open, float ot, float t) {
        int top = base - h;
        int r = w / 2;
        GambleArt.ellipse(ctx, x, base, (int) (w * 0.75f), 4, argb(a, 0x1A1218), 2);
        int stone = lit ? 0x6E6E7E : 0x4A4A56;
        if (lit) {
            float pulse = 0.6f + 0.4f * (float) Math.sin(t * 14);
            for (int g = 1; g <= 3; g++) ctx.outline(x - r - g * 2, top - g * 2, w + g * 4, h + g * 4, argb(a * pulse * (0.45f - g * 0.12f), 0x78FFC8));
        }
        if (!open) {
            GambleArt.disc(ctx, x, top + r, r, argb(a, stone), 2);
            ctx.fill(x - r, top + r, x + r, base, argb(a, stone));
            ctx.fill(x + r - 3, top + r, x + r, base, argb(a, 0x2E2E36));
            ctx.fill(x - r, base - 4, x - r + w / 3, base, argb(a, 0x2E4A2A));
            GambleArt.line(ctx, x - r + 4, top + r + 4, x - r + 9, top + r + 12, 1, argb(a, 0x2A2A32));
            ctx.centeredText(font, Component.literal("RIP"), x, top + r - 6, argb(a * 0.85f, 0x24242C));
            GambleArt.item(ctx, font, icon, x, top + r + h * 0.28f, w * 0.6f, a);
            return;
        }
        // split open: the halves slide apart and sink, a green soul light shines out of the grave
        float u = ease(ot / 0.5f);
        int off = (int) (u * w * 0.55f);
        int sink = (int) (u * 10);
        ctx.fillGradient(x - r + 2, top - (int) (u * 60), x + r - 2, base, argb(a * 0.0f, 0x78FFC8), argb(a * 0.55f * u, 0x78FFC8));
        ctx.fill(x - r + 3, base - 6, x + r - 3, base, argb(a, 0x020104));
        ctx.fill(x - r - off, top + r + sink, x - off, base + sink, argb(a, stone));
        ctx.fill(x + off, top + r + sink, x + r + off, base + sink, argb(a, stone));
        ctx.fill(x + r + off - 3, top + r + sink, x + r + off, base + sink, argb(a, 0x2E2E36));
        Sym s = look.pool[resultIdx];
        float iy = base - 10 - Math.min(1.0f, ot / 0.6f) * (h + 20);
        float isz = w * (0.6f + 0.8f * Math.min(1.0f, ot / 0.6f));
        for (int i = 0; i < 10; i++) {
            float l = wrap(t * 1.4f + i * 0.1f, 1.0f);
            ctx.fill(x - r + (i * 7) % w, (int) (base - l * h * 1.4f), x - r + (i * 7) % w + 2, (int) (base - l * h * 1.4f) + 3, argb(a * (1 - l), 0x78FFC8));
        }
        GambleArt.item(ctx, font, iconOf(s), x, iy, isz, a);
    }

    private static void scythe(GuiGraphicsExtractor ctx, float x, float y, float ang, float scale, float a) {
        Matrix3x2fStack ps = ctx.pose();
        ps.pushMatrix();
        ps.translate(x, y);
        ps.rotate(ang);
        ps.scale(scale, scale);
        ctx.fill(-2, -44, 2, 54, argb(a, 0x2E2018));
        ctx.fill(-1, -44, 0, 54, argb(a, 0x56402E));
        ctx.fill(-3, 12, 3, 15, argb(a, 0x111111));
        ctx.fill(-3, 32, 3, 35, argb(a, 0x111111));
        for (int i = 0; i < 28; i++) {
            float u = i / 27.0f;
            int bx = (int) (-u * 50);
            int by = (int) (-44 + u * u * 24);
            int bh = (int) (9 * (1 - u) + 2);
            ctx.fill(bx - 2, by, bx + 1, by + bh, argb(a, 0xC8C8D4));
            ctx.fill(bx - 2, by, bx + 1, by + 1, argb(a, 0xFFFFFF));
            ctx.fill(bx - 2, by + bh - 1, bx + 1, by + bh, argb(a, 0x78FFC8));
        }
        ps.popMatrix();
    }

    // ---------------------------------------------------------------- Yeti: the ice claw machine

    private static void drawClaw(GuiGraphicsExtractor ctx, Font font, float t, float a, int sw, int sh, float settled, float burstAt) {
        int ground = (int) (sh * 0.78f);
        // a snowy mountain evening: sky, pale moon, peaks, pines, snow
        ctx.fillGradient(0, 0, sw, ground, argb(a * 0.97f, 0x0B1A30), argb(a * 0.97f, 0x5A82AA));
        GambleArt.disc(ctx, (int) (sw * 0.14f), (int) (sh * 0.15f), (int) (sh * 0.07f), argb(a * 0.15f, 0xEAF4FF), 2);
        GambleArt.disc(ctx, (int) (sw * 0.14f), (int) (sh * 0.15f), (int) (sh * 0.045f), argb(a * 0.9f, 0xEAF4FF), 1);
        snowfall(ctx, t, a * 0.6f, sw, ground, 70, -12, 1);
        for (int i = 0; i < 6; i++) {
            int px = (int) (sw * (i / 5.0f)) + (i % 2) * 30 - 20;
            peak(ctx, px, ground - (int) (sh * 0.10f), (int) (sh * (0.26f + 0.08f * ((i * 7) % 3))), (int) (sh * 0.5f), 0x5A7696, 0xF2F8FF, a);
        }
        hills(ctx, 0, ground - (int) (sh * 0.06f), (int) (sh * 0.05f), 0xD4E4F2, 0.011f, a, sw, ground);
        for (int i = 0; i < sw / 34 + 1; i++) {
            int x = i * 34 + (i % 2) * 11;
            pine(ctx, x, ground - 4 - (i % 3) * 3, (int) (sh * (0.09f + 0.025f * (i % 3))), 0x1C4636, 0xF4FAFF, a * 0.95f);
        }
        ctx.fill(0, ground, sw, sh, argb(a, 0xEDF4FB));
        for (int x = 0; x < sw; x += 4) {
            int d = (int) (Math.sin(x * 0.03) * 4 + Math.sin(x * 0.11) * 2);
            ctx.fill(x, ground + 10 + d, x + 4, ground + 13 + d, argb(a, 0xC8DCEC));
        }
        if (t < 1.0f) sound(5, SoundEvents.POLAR_BEAR_WARNING, 0.6f, 0.4f);

        // the snowman (a real snow golem)
        if (horse != null && t >= RISE_AT && MobView.available()) {
            int spx = (int) (sh * 0.24f);
            ctx.nextStratum();
            MobView.draw(ctx, horse, (int) (sw * 0.91f), ground - spx / 2 + 6, spx, (float) Math.sin(t * 0.8f) * 0.7f, -0.1f);
            ctx.nextStratum();
        }

        // the claw machine: compact, with big ice cubes in two stacked rows and a big claw
        Sym[] pool = look.pool;
        int n = pool.length;
        int cols = (n + 1) / 2;
        int cs = Math.max(30, Math.min(46, (int) (sh * 0.13f)));
        int cell = cs + 6;
        int mw = (int) (cols * cell + cell * 0.5f) + 24;
        int mh = Math.max((int) (sh * 0.44f), cs * 2 + 120);
        int mx = (int) (sw * 0.60f) - mw / 2;
        int my = ground + (int) (sh * 0.08f) - mh;
        float ga = clamp((t - GAMBLE_AT) / 0.4f) * a;
        boolean on = t >= GAMBLE_AT;
        if (on) {
            ctx.fill(mx - 7, my - 34, mx + mw + 7, my + mh + 7, argb(ga, 0x1E3A58));
            ctx.fill(mx - 5, my - 32, mx + mw + 5, my + mh + 5, argb(ga, 0x7AA8C8));
            ctx.fillGradient(mx, my, mx + mw, my + mh, argb(ga * 0.30f, 0xDDF3FF), argb(ga * 0.55f, 0x6AA6D6));
            for (int i = 0; i < 5; i++) ctx.fill(mx + 8 + i * 6, my + 4 + i * 10, mx + 10 + i * 6, my + 30 + i * 10, argb(ga * 0.15f, 0xFFFFFF));
            ctx.fill(mx, my - 30, mx + mw, my - 4, argb(ga, 0x16304C));
            boolean blink = ((int) (t * 4)) % 2 == 0;
            for (int i = 0; i < mw / 12; i++) {
                ctx.fill(mx + 4 + i * 12, my - 28, mx + 7 + i * 12, my - 25, argb(ga, (i % 2 == 0) == blink ? 0x9AF0FF : 0x2A5070));
            }
            GambleArt.bigText(ctx, font, "YETI CLAW", mx + mw / 2.0f, my - 15, 1.4f, 0xDDF6FF, ga);
            for (int i = 0; i < 10; i++) {
                ctx.fill(mx, my + i, mx + 10 - i, my + i + 1, argb(ga * 0.8f, 0xFFFFFF));
                ctx.fill(mx + mw - 10 + i, my + i, mx + mw, my + i + 1, argb(ga * 0.8f, 0xFFFFFF));
            }
            ctx.fill(mx + 4, my + 8, mx + mw - 4, my + 12, argb(ga, 0x9AB8D0));
            ctx.fill(mx, my + mh - 6, mx + mw, my + mh, argb(ga, 0xEAF6FF));
        }
        // cube i: bottom row = 0..cols-1, top row (offset by half a cube) = the rest
        float[] cxs = new float[n], cys = new float[n];
        float floorY = my + mh - 6;
        for (int i = 0; i < n; i++) {
            int row = i < cols ? 0 : 1;
            int col = row == 0 ? i : i - cols;
            cxs[i] = mx + 12 + cs / 2.0f + col * cell + (row == 1 ? cell * 0.5f : 0);
            cys[i] = floorY - cs / 2.0f - row * (cs + 2);
        }
        float xL = cxs[0], xR = cxs[n - 1];
        float sweep = xL + (0.5f + 0.5f * (float) Math.sin((stopAt >= 0 ? stopAt : t) * 1.6f)) * (xR - xL);
        float clawX = stopAt < 0 || t < stopAt ? xL + (0.5f + 0.5f * (float) Math.sin(t * 1.6f)) * (xR - xL)
            : sweep + (cxs[resultIdx] - sweep) * ease((t - stopAt) / STOP_LEN);
        float clawScale = cs / 20.0f;
        float arrive = stopAt >= 0 ? stopAt + STOP_LEN : Float.MAX_VALUE;
        float d = t - arrive;
        int railY = my + 12;
        float top = railY + 6;
        float bottom = stopAt >= 0 ? cys[resultIdx] - cs / 2.0f - 15 * clawScale : top;
        float clawY = top + (float) Math.sin(t * 3) * 2;
        float grip = 0.0f;
        if (d >= 0) {
            if (d < 0.6f) clawY = top + (bottom - top) * ease(d / 0.6f);
            else if (d < 0.8f) {
                clawY = bottom;
                grip = (d - 0.6f) / 0.2f;
            } else {
                clawY = bottom - (bottom - top) * ease((d - 0.8f) / 0.8f);
                grip = 1;
            }
            if (d < 0.05f) sound(6, SoundEvents.PISTON_EXTEND, 1.4f, 0.4f);
            if (d > 0.6f && d < 0.65f) sound(7, SoundEvents.CHAIN_PLACE, 1.0f, 0.5f);
        }
        // the cube under the claw: the nearest one, top row first
        int lit = 0;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            float dist = Math.abs(cxs[i] - clawX) - (i >= cols ? cs * 0.3f : 0);
            if (dist < best) {
                best = dist;
                lit = i;
            }
        }
        if (on && stopAt < 0) tick(lit, 1.8f);
        boolean held = d >= 0.8f && t < settled;
        boolean shattered = t >= settled;
        float heldY = clawY + 15 * clawScale + cs / 2.0f;
        if (on) {
            for (int i = 0; i < n; i++) {
                if (i == resultIdx && (held || shattered) && stopAt >= 0) continue;
                iceCube(ctx, font, cxs[i], cys[i], cs, iconOf(pool[i]), ga, i == lit && stopAt < 0, t);
            }
            int cxI = (int) clawX;
            ctx.fill(cxI - 9, railY - 3, cxI + 9, railY + 5, argb(ga, 0x3A5A7A));
            ctx.fill(cxI - 1, railY + 5, cxI + 2, (int) clawY, argb(ga, 0xD8E4EE));
            if (held) iceCube(ctx, font, clawX + (d > 1.6f ? (RNG.nextFloat() - 0.5f) * 3 : 0), heldY, cs, iconOf(pool[resultIdx]), ga, true, t);
            Matrix3x2fStack ps = ctx.pose();
            ps.pushMatrix();
            ps.translate(clawX, clawY);
            ps.scale(clawScale, clawScale);
            claw(ctx, 0, 0, grip, ga);
            ps.popMatrix();
        }
        if (shattered && stopAt >= 0) {
            float st = t - settled;
            float hx = clawX, hy = heldY;
            sound(8, SoundEvents.GLASS_BREAK, 1.1f, 0.6f);
            for (int k = 0; k < 16; k++) {
                double ang = k * Math.PI * 2 / 16;
                float sx = hx + (float) Math.cos(ang) * st * 120, sy = hy + (float) Math.sin(ang) * st * 90 + st * st * 160;
                ctx.fill((int) sx, (int) sy, (int) sx + 4, (int) sy + 4, argb(a * clamp(1 - st), k % 2 == 0 ? 0xDDF6FF : 0x9ACCEE));
            }
            GambleArt.item(ctx, font, iconOf(pool[resultIdx]), hx, hy - Math.min(st, 0.4f) * 60, cs + Math.min(st, 0.4f) * 50, a);
            GambleArt.bigText(ctx, font, win() ? "GOT IT!" : "EMPTY...", mx + mw / 2.0f, my + mh * 0.30f, win() ? 2.6f : 2.0f, win() ? 0xBFE9FF : 0x9E9890, a * clamp(st / 0.2f));
        }

        // the 3D Yeti on the left, working the joystick
        int ypx = (int) (sh * 0.46f);
        int ycx = (int) (sw * 0.19f);
        int yStand = ground - ypx / 2 + (int) (sh * 0.04f);
        if (on) {
            float vel = stopAt < 0 ? (float) Math.cos(t * 1.6f) : 0;
            int jx = ycx + ypx / 3 + 14, jy = ground + (int) (sh * 0.06f);
            ctx.fill(jx - 16, jy, jx + 16, jy + 12, argb(ga, 0x1E3A58));
            ctx.fill(jx - 14, jy + 2, jx + 14, jy + 10, argb(ga, 0x7AA8C8));
            GambleArt.line(ctx, jx, jy, jx + vel * 9, jy - 18, 2, argb(ga, 0x2A2A2A));
            GambleArt.disc(ctx, (int) (jx + vel * 9), jy - 19, 4, argb(ga, 0xD03030), 1);
        }
        if (t >= RISE_AT && t < burstAt && mob != null && MobView.available()) {
            float rise = ease((t - RISE_AT) / (GAMBLE_AT - RISE_AT));
            int cy = (int) (ground + ypx * 0.6f - (ground + ypx * 0.6f - yStand) * rise);
            float shake = t >= settled ? 2 + 4 * clamp((t - settled) / Math.max(0.01f, burstAt - settled)) : 0;
            mob.hurtTime = t >= settled && ((int) (t * 12) % 2 == 0) ? 8 : 0;
            ctx.nextStratum();
            MobView.drawPosed(ctx, mob, ycx + (int) (shake * (RNG.nextFloat() * 2 - 1)), cy, ypx, -45.0f, -4.0f, 12.0f);
            ctx.nextStratum();
        }
        snowfall(ctx, t, a, sw, sh, 45, -18, 2);
        burstPieces(ctx, t, burstAt, a, ycx, yStand, sh, 0xEAF6FF);
        waiting(ctx, font, t, ga, mx + mw / 2, sh - 14);
        endSound(t, settled);
    }

    private static void iceCube(GuiGraphicsExtractor ctx, Font font, float cx, float cy, int s, ItemStack icon, float a, boolean lit, float t) {
        int x = (int) cx - s / 2, y = (int) cy - s / 2;
        if (lit) {
            float pulse = 0.6f + 0.4f * (float) Math.sin(t * 12);
            for (int g = 1; g <= 3; g++) ctx.outline(x - g * 2, y - g * 2, s + g * 4, s + g * 4, argb(a * pulse * (0.5f - g * 0.13f), 0xBFF4FF));
        }
        ctx.fill(x, y, x + s, y + s, argb(a * 0.55f, 0xBFE9FF));
        GambleArt.item(ctx, font, icon, cx, cy, s * 0.74f, a);
        ctx.fill(x, y, x + s, y + s, argb(a * 0.22f, 0xDDF6FF));
        ctx.outline(x, y, s, s, argb(a, 0xF0FAFF));
        ctx.fill(x + 2, y + 2, x + s / 2, y + 4, argb(a * 0.7f, 0xFFFFFF));
        ctx.fill(x + 2, y + 2, x + 4, y + s / 2, argb(a * 0.7f, 0xFFFFFF));
    }

    private static void claw(GuiGraphicsExtractor ctx, float x, float y, float grip, float a) {
        int c = argb(a, 0xC8D2DA), dark = argb(a, 0x5A6670);
        ctx.fill((int) x - 9, (int) y, (int) x + 9, (int) y + 6, dark);
        ctx.fill((int) x - 8, (int) y + 1, (int) x + 8, (int) y + 5, c);
        float open = 1.0f - grip;
        for (int side : new int[]{-1, 1}) {
            float ex = x + side * (6 + 8 * open), ey = y + 12;
            GambleArt.line(ctx, x + side * 6, y + 5, ex, ey, 2, c);
            GambleArt.line(ctx, ex, ey, ex - side * (5 + 3 * grip), ey + 9, 2, c);
        }
        GambleArt.line(ctx, x, y + 5, x, y + 16, 2, c);
    }

    // ---------------------------------------------------------------- Reindrake: the dragon's gifts

    private static float shuffleStart() {
        return GAMBLE_AT + 1.6f;
    }

    private static int shuffleSteps() {
        if (stopAt < 0) return Integer.MAX_VALUE;
        float end = Math.max(stopAt + STOP_LEN * 0.5f, shuffleStart() + 2.0f);
        return Math.min(SWAPS.length - 1, (int) Math.ceil((end - shuffleStart()) / SWAP_T));
    }

    private static float giftEnd() {
        int steps = shuffleSteps();
        return steps == Integer.MAX_VALUE ? Float.MAX_VALUE : shuffleStart() + steps * SWAP_T;
    }

    private static void drawGifts(GuiGraphicsExtractor ctx, Font font, float t, float a, int sw, int sh, float settled, float burstAt) {
        int ground = (int) (sh * 0.78f);
        // winter night: stars, aurora, moon, snowy hills with pines, falling snow
        ctx.fillGradient(0, 0, sw, ground, argb(a * 0.97f, 0x020612), argb(a * 0.97f, 0x10264A));
        for (int i = 0; i < 50; i++) {
            int x = (i * 137) % Math.max(1, sw), y = (i * 59) % Math.max(1, (int) (ground * 0.7f));
            float tw = 0.3f + 0.35f * (float) Math.sin(t * 3 + i * 1.7f);
            ctx.fill(x, y, x + 1, y + 1, argb(a * tw, 0xFFFFFF));
        }
        for (int x = 0; x < sw; x += 6) {
            float y0 = sh * 0.08f + (float) Math.sin(x * 0.008 + t * 0.3) * sh * 0.05f;
            float h = sh * 0.12f + (float) (Math.sin(x * 0.012 + t * 0.6) * sh * 0.05f + Math.sin(x * 0.031 - t * 0.9) * sh * 0.02f);
            float pulse = 0.6f + 0.4f * (float) Math.sin(x * 0.02 + t * 1.2);
            ctx.fillGradient(x, (int) y0, x + 6, (int) (y0 + h), argb(a * 0.04f, 0x9A6AFF), argb(a * 0.30f * pulse, 0x3AFFB0));
            ctx.fillGradient(x, (int) (y0 + h), x + 6, (int) (y0 + h + 14), argb(a * 0.30f * pulse, 0x3AFFB0), argb(0, 0x3AFFB0));
        }
        GambleArt.disc(ctx, (int) (sw * 0.86f), (int) (sh * 0.14f), (int) (sh * 0.04f), argb(a * 0.9f, 0xF4F8FF), 1);
        hills(ctx, 0, ground - (int) (sh * 0.09f), (int) (sh * 0.07f), 0x8AA6C2, 0.007f, a, sw, ground);
        hills(ctx, 40, ground - (int) (sh * 0.03f), (int) (sh * 0.04f), 0xC4D6E8, 0.013f, a, sw, ground);
        for (int i = 0; i < sw / 46 + 1; i++) {
            int x = i * 46 + (i % 3) * 9;
            if (x > sw * 0.30f && x < sw * 0.70f) continue;
            pine(ctx, x, ground - 2, (int) (sh * (0.12f + 0.03f * (i % 3))), 0x0E2E24, 0xF4FAFF, a);
        }
        ctx.fill(0, ground, sw, sh, argb(a, 0xE9F1F9));
        // a small snowy hill on the left (in front of the horizon)
        {
            int bx = (int) (sw * 0.11f), bb = ground + (int) (sh * 0.12f), bh = (int) (sh * 0.24f), bw = (int) (sw * 0.26f);
            for (int yy = 0; yy < bh; yy += 2) {
                float f = yy / (float) bh;
                int hw = (int) (bw / 2.0f * Math.sqrt(f));
                int col = f < 0.30f ? 0xFFFFFF : (f < 0.75f ? 0xE4EEF8 : 0xC8DAEA);
                ctx.fill(bx - hw, bb - bh + yy, bx + hw, bb - bh + yy + 2, argb(a, col));
                ctx.fill(bx + hw - hw / 3, bb - bh + yy, bx + hw, bb - bh + yy + 2, argb(a * 0.35f, 0x9AB4CC));
            }
            pine(ctx, bx - bw / 5, bb - bh / 2, (int) (sh * 0.10f), 0x0E2E24, 0xF4FAFF, a);
            pine(ctx, bx + bw / 6, bb - bh / 3, (int) (sh * 0.08f), 0x0E2E24, 0xF4FAFF, a);
        }
        for (int x = 0; x < sw; x += 8) {
            int d = (int) (Math.sin(x * 0.025) * 4 + Math.sin(x * 0.09) * 2);
            ctx.fill(x, ground + 12 + d, x + 8, ground + 15 + d, argb(a, 0xC4D6E8));
        }
        snowfall(ctx, t, a * 0.7f, sw, sh, 60, -10, 3);

        // gift slots on the snow
        int gs = Math.max(40, (int) (sh * 0.17f));
        int gy = ground + (int) (sh * 0.11f);
        float[] slotX = {sw * 0.5f - sw * 0.16f, sw * 0.5f, sw * 0.5f + sw * 0.16f};
        String[] giftIcon = {"RGIFT", "GGIFT", "WGIFT"};

        // the Reindrake (an Ender Dragon): big fly-by dropping three gifts, then circles in the sky
        float flyEnd = GAMBLE_AT + 1.0f;
        float flyDur = flyEnd - RISE_AT;
        float startX = sw * 1.35f, endX = -sw * 0.35f;
        float dragonX, dragonY, yaw, span, pitch = 0;
        float death = 0;
        float ge = giftEnd();
        if (t < flyEnd) {
            float u = (t - RISE_AT) / flyDur;
            dragonX = startX + (endX - startX) * u;
            dragonY = sh * 0.30f - (float) Math.sin(clamp(u) * Math.PI) * sh * 0.06f;
            yaw = 90.0f;
            span = sw * 0.55f;
            pitch = 8.0f;
        } else if (t < ge) {
            float ct = t - flyEnd;
            float w = 0.75f;
            dragonX = sw * 0.5f - (float) Math.cos(ct * w) * sw * 0.58f;
            dragonY = sh * 0.20f + (float) Math.sin(ct * w * 2) * sh * 0.04f;
            float vx = (float) Math.sin(ct * w);
            yaw = -90.0f * vx;
            span = sw * (0.24f + 0.06f * (float) Math.abs(vx));
        } else {
            // swoops down above the chosen gift and dies there once the gift is open
            float ct = ge - flyEnd;
            float w = 0.75f;
            float fx = sw * 0.5f - (float) Math.cos(ct * w) * sw * 0.58f;
            float fy = sh * 0.20f + (float) Math.sin(ct * w * 2) * sh * 0.04f;
            float u = ease((t - ge) / 0.9f);
            dragonX = fx + (sw * 0.5f - fx) * u;
            dragonY = fy + (sh * 0.34f - fy) * u + (float) Math.sin(t * 2) * 4;
            yaw = -90.0f * (float) Math.sin(ct * w) * (1 - u);
            span = sw * (0.26f + 0.24f * u);
            pitch = 10.0f * u;
            if (t >= settled) death = Math.min(200.0f, (t - settled) / Math.max(0.01f, burstAt - settled) * 170.0f);
            if (t - ge < 0.1f) sound(9, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 0.4f);
            if (t >= settled) sound(10, SoundEvents.ENDER_DRAGON_DEATH, 1.2f, 0.25f);
        }
        float flap = t * 1.3f;
        if (t >= RISE_AT && t < burstAt) {
            int f = (int) flap;
            if (f != lastFlap) {
                lastFlap = f;
                if (death <= 0) sound(-1, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.25f);
            }
            if (t < RISE_AT + 0.1f) sound(11, SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 0.35f);
            if (mob != null && MobView.available()) {
                ctx.nextStratum();
                MobView.drawDragon(ctx, mob, (int) dragonX, (int) dragonY, (int) span, yaw, pitch, flap, 1.2f, t, death);
                ctx.nextStratum();
            }
        }

        // the gifts: fall from the dragon, land in the snow, get shuffled, the middle one opens
        float g = 900.0f;
        int steps = shuffleSteps();
        float tau = t - shuffleStart();
        int k = tau < 0 ? 0 : (int) Math.floor(tau / SWAP_T);
        float frac = tau < 0 ? 0 : (tau / SWAP_T) - k;
        if (k >= steps) {
            k = steps;
            frac = 0;
        }
        int[] at = {0, 1, 2}; // gift -> slot
        for (int j = 0; j < Math.min(k, SWAPS.length); j++) swap(at, SWAPS[j % SWAPS.length]);
        int sa = -1, sb = -1;
        if (frac > 0 && k < SWAPS.length) {
            int sw2 = SWAPS[k % SWAPS.length];
            sa = sw2 == 1 ? 1 : 0;
            sb = sw2 == 0 ? 1 : 2;
            tick(k, 1.3f);
        }
        float revealT = t - ge;
        for (int i = 0; i < 3; i++) {
            float drop = RISE_AT + (startX - slotX[i]) / (startX - endX) * flyDur;
            if (t < drop) continue;
            float x = slotX[at[i]];
            float y;
            float fall = t - drop;
            float startY = sh * 0.30f + sh * 0.04f;
            y = Math.min(gy, startY + 0.5f * g * fall * fall);
            if (y >= gy && fall < 0.9f) {
                float landT = (float) Math.sqrt(2 * (gy - startY) / g);
                float bt = fall - landT;
                y = gy - (float) Math.abs(Math.sin(bt * 9)) * 10 * Math.max(0, 1 - bt * 2.5f);
                if (bt < 0.05f) sound(5 + i, SoundEvents.SNOW_PLACE, 0.9f, 0.5f);
            }
            if (frac > 0 && (at[i] == sa || at[i] == sb)) {
                float from = slotX[at[i]], to = slotX[at[i] == sa ? sb : sa];
                float e = ease(frac);
                x = from + (to - from) * e;
                y = gy - (float) Math.sin(frac * Math.PI) * (at[i] == sa ? 26 : -6);
            }
            boolean picked = at[i] == 1 && revealT >= 0;
            float ia = a;
            if (revealT >= 0 && !picked) ia = a * Math.max(0.35f, 1 - revealT * 1.5f);
            GambleArt.ellipse(ctx, (int) x, gy + gs / 2 - 2, gs / 2, 4, argb(ia * 0.35f, 0x7A90A8), 1);
            if (picked) {
                float lift = ease(revealT / 0.5f) * gs * 0.6f;
                if (revealT < 0.6f) {
                    float pulse = 0.6f + 0.4f * (float) Math.sin(t * 16);
                    GambleArt.disc(ctx, (int) x, (int) (y - lift), gs, argb(ia * 0.15f * pulse, 0xFFD24A), 2);
                    GambleArt.item(ctx, font, GambleArt.icon(giftIcon[i]), x + (RNG.nextFloat() - 0.5f) * 3 * revealT, y - lift, gs, ia);
                } else {
                    float ot = revealT - 0.6f;
                    sound(8, SoundEvents.PLAYER_LEVELUP, 1.4f, 0.5f);
                    for (int c = 0; c < 24; c++) {
                        double ang = c * Math.PI * 2 / 24;
                        float cx = x + (float) Math.cos(ang) * ot * 140, cy = y - lift + (float) Math.sin(ang) * ot * 100 + ot * ot * 200;
                        int col = c % 3 == 0 ? 0xFFD24A : (c % 3 == 1 ? 0xD03030 : 0x30A050);
                        ctx.fill((int) cx, (int) cy, (int) cx + 3, (int) cy + 3, argb(ia * clamp(1.2f - ot), col));
                    }
                    GambleArt.disc(ctx, (int) x, (int) (y - lift), (int) (gs * 0.9f), argb(ia * 0.2f, win() ? 0xFFD24A : 0x7A7470), 2);
                    GambleArt.item(ctx, font, iconOf(look.pool[resultIdx]), x, y - lift - Math.min(ot, 0.3f) * 40, gs * (1.0f + Math.min(ot, 0.3f) * 1.5f), ia);
                    GambleArt.bigText(ctx, font, win() ? "UNWRAPPED!" : "EMPTY GIFT", sw / 2.0f, sh * 0.48f, win() ? 3.0f : 2.0f,
                        win() ? 0xFFD24A : 0x9E9890, a * clamp(ot / 0.2f));
                }
            } else {
                GambleArt.item(ctx, font, GambleArt.icon(giftIcon[i]), x, y, gs, ia);
            }
        }
        // the snow golem on the right
        if (horse != null && t >= RISE_AT && MobView.available()) {
            int spx = (int) (sh * 0.26f);
            ctx.nextStratum();
            MobView.draw(ctx, horse, (int) (sw * 0.89f), ground + (int) (sh * 0.10f) - spx / 2, spx, (float) Math.sin(t * 0.8f) * 0.7f, -0.1f);
            ctx.nextStratum();
        }
        snowfall(ctx, t, a, sw, sh, 30, -16, 4);
        burstPieces(ctx, t, burstAt, a, sw / 2, (int) (sh * 0.34f), sh, 0xC080FF);
        waiting(ctx, font, t, a, sw / 2, sh - 14);
        endSound(t, settled);
    }

    private static void swap(int[] at, int which) {
        int sa = which == 1 ? 1 : 0;
        int sb = which == 0 ? 1 : 2;
        for (int i = 0; i < 3; i++) {
            if (at[i] == sa) at[i] = sb;
            else if (at[i] == sb) at[i] = sa;
        }
    }

    /** When the k-th shot falls while the result is not known yet. */
    private static float preShot(int k, float scopeAt) {
        return scopeAt + 0.5f + k * 1.4f;
    }

    private static void hills(GuiGraphicsExtractor ctx, float off, int base, int amp, int color, float f, float a, int sw, int bottom) {
        for (int x = 0; x < sw; x += 4) {
            double v = Math.abs(Math.sin((x + off) * f)) * amp + Math.sin((x + off) * f * 2.3) * amp * 0.3;
            ctx.fill(x, base - (int) v, x + 4, bottom, argb(a, color));
        }
    }

    /** Meteors (Ragnarok), blue flicker (Thunder), lava bursts when Jawbus pounds. */
    private static void drawWeather(GuiGraphicsExtractor ctx, Game g, float t, float a, int sw, int sh, int poolTop) {
        switch (g) {
            case WHEEL -> {
                for (int i = 0; i < 8; i++) {
                    float life = (t * (0.4f + i * 0.05f) + i * 0.29f) % 1.0f;
                    int x = (int) ((i * 131 + 40) % Math.max(1, sw)) - (int) (life * 120);
                    int y = (int) (life * poolTop);
                    for (int k = 0; k < 6; k++) {
                        ctx.fill(x + k * 3, y - k * 4, x + k * 3 + 3, y - k * 4 + 3, argb(a * (1 - k / 6f), k == 0 ? 0xFFE08A : 0xFF5A1A));
                    }
                }
            }
            case ROULETTE -> {
                if (((int) (t * 5)) % 7 == 0) ctx.fill(0, 0, sw, sh, argb(0.12f * a, 0xBFE3FF));
                for (int i = 0; i < 7; i++) {
                    int cxl = (int) ((i * sw / 6.0) + Math.sin(t * 0.3 + i) * 20);
                    GambleArt.ellipse(ctx, cxl, 6, 60, 26, argb(a * 0.8f, 0x0E1A26), 3);
                }
                for (int i = 0; i < 70; i++) {
                    int x = (int) ((i * 37 + t * 60) % (sw + 20)) - 10;
                    int y = (int) ((i * 53 + t * 300) % (poolTop + 10)) - 10;
                    ctx.fill(x, y, x + 1, y + 7, argb(a * 0.35f, 0x96BEDC));
                }
            }
            case BUBBLES -> {
                for (int i = 0; i < 6; i++) {
                    int x = (int) ((i * 97 + t * 10) % (sw + 120)) - 60;
                    GambleArt.ellipse(ctx, x, 40 + i * (poolTop / 7), 50, 12, argb(a * 0.35f, 0x000000), 3);
                }
                for (int i = 0; i < sw / 40; i++) {
                    float l = ((t * 0.3f + i * 0.2f) % 1.0f) * 40;
                    int x = i * 40 + 10;
                    ctx.fill(x, 0, x + 3, (int) (10 + l), argb(a * 0.75f, 0x78FF5A));
                    GambleArt.disc(ctx, x + 1, (int) (12 + l), 3, argb(a * 0.75f, 0x78FF5A), 1);
                }
            }
            case SLOTS -> {
                float beat = (t * 1.4f) % 1.0f;
                if (t > RISE_AT && beat < 0.25f) {
                    for (int k = 0; k < 10; k++) {
                        int x = sw / 2 - 60 + k * 13;
                        int y = poolTop - (int) (beat * 80 * (1 + k % 3));
                        ctx.fill(x, y, x + 3, y + 3, argb(a * (1 - beat * 4), 0xFF7A1A));
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void drawCracks(GuiGraphicsExtractor ctx, int cx, int cy, int px, float p, float a, int hot) {
        Random r = new Random(1234L);
        int lines = 3 + (int) (p * 5);
        for (int i = 0; i < lines; i++) {
            int x = cx + (int) ((r.nextFloat() - 0.5f) * px * 0.35f);
            int y = cy + (int) ((r.nextFloat() - 0.5f) * px * 0.6f);
            int steps = 3 + (int) (p * 5);
            for (int s = 0; s < steps; s++) {
                int nx = x + r.nextInt(7) - 3;
                int ny = y + 3 + r.nextInt(4);
                int x0 = Math.min(x, nx);
                ctx.fill(x0, y, x0 + Math.abs(nx - x) + 2, ny, argb(a * (0.6f + 0.4f * p), hot));
                x = nx;
                y = ny;
            }
        }
    }

    private static void drawBolt(GuiGraphicsExtractor ctx, int x, int y0, int y1, float a) {
        Random r = new Random(x * 31L + System.nanoTime() / 80_000_000L);
        int y = y0;
        while (y < y1) {
            int nx = x + r.nextInt(13) - 6;
            int ny = Math.min(y1, y + 8 + r.nextInt(8));
            ctx.fill(Math.min(x, nx), y, Math.min(x, nx) + Math.abs(nx - x) + 2, ny, argb(a, 0xFFF36B));
            x = nx;
            y = ny;
        }
    }
}
