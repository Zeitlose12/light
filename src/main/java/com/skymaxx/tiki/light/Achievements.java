package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.config.TikiStats;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Light: achievements built from the statistics the mod already counts. */
public enum Achievements {
    FIRST_TIKI(Category.KILLS, "Tiki Time", "Kill your first Wiki Tiki", s -> s.kills >= 1),
    TIKI_100(Category.KILLS, "Totem Regular", "Kill 100 Wiki Tikis", s -> s.kills >= 100),
    TIKI_500(Category.KILLS, "Tiki Veteran", "Kill 500 Wiki Tikis", s -> s.kills >= 500),
    FIRST_BOA(Category.KILLS, "Snake Charmer", "Kill your first Titanoboa", s -> s.boaKills >= 1),
    BOA_100(Category.KILLS, "Boa Hunter", "Kill 100 Titanoboas", s -> s.boaKills >= 100),
    BOA_500(Category.KILLS, "Boa Veteran", "Kill 500 Titanoboas", s -> s.boaKills >= 500),
    FOUR_MASKS(Category.ANIMATIONS, "Four of a Kind", "Reach the fourth mask", s -> s.reachedFinalReel >= 1),
    HEADSHOT(Category.ANIMATIONS, "Headshot", "Shoot the Titanoboa down to its head", s -> s.segmentsDown.length > 3 && s.segmentsDown[3] >= 1),
    SCRIPTURES(Category.DROPS, "Well Read", "Get Bobbin' Scriptures", s -> s.scripturesDropped + s.boaScripturesDropped >= 1),
    BUBBLE(Category.DROPS, "Bubble Trouble", "Get a Troubled Bubble", s -> s.bubblesDropped >= 1),
    MASK(Category.DROPS, "Masked", "Get a Tiki Mask", s -> s.masksDropped >= 1),
    LILY(Category.DROPS, "Green Thumb", "Get a Condensed Lily Pad", s -> s.lilyPadsDropped >= 1),
    SHED(Category.DROPS, "Shed Happens", "Get a Titanoboa Shed", s -> s.shedsDropped >= 1),
    SNAKE_EYES(Category.DROPS, "Snake Eyes", "Get Snake Eyes", s -> s.snakeEyesDropped >= 1),
    AQUAMARINE(Category.DYES, "1 in 50,000", "Get an Aquamarine Dye", s -> s.dyesDropped + s.boaDyesDropped >= 1 || Achievements.dye(com.skymaxx.tiki.light.dye.Dye.AQUAMARINE) >= 1),
    ICEBERG(Category.DYES, "Ice Cold", "Get an Iceberg Dye", s -> Achievements.dye(com.skymaxx.tiki.light.dye.Dye.ICEBERG) >= 1),
    CARMINE(Category.DYES, "Seeing Red", "Get a Carmine Dye", s -> Achievements.dye(com.skymaxx.tiki.light.dye.Dye.CARMINE) >= 1),
    MIDNIGHT(Category.DYES, "Midnight Fisher", "Get a Midnight Dye", s -> Achievements.dye(com.skymaxx.tiki.light.dye.Dye.MIDNIGHT) >= 1),
    RAINBOW(Category.DYES, "Full Palette", "Get all four fishing dyes", s -> Achievements.dyeKinds() >= 4),
    BOOKWORM(Category.DYES, "Bookworm", "Load your kills with /pv", s -> com.skymaxx.tiki.light.dye.DyeTracker.hasData()),
    OVERDUE(Category.DYES, "Overdue", "Reach a 90% dye chance without that dye", s -> Achievements.overdue()),
    DRY_SPELL(Category.LUCK, "Dry Spell", "Go 300 Wiki Tikis without a drop", s -> s.worstDryStreak >= 300),
    LUCKY(Category.LUCK, "Born Lucky", "Be +50% lucky after 200 kills", s -> s.kills + s.boaKills >= 200 && Luck.overall(s) >= 50.0),
    JACKPOT(Category.LAVA, "Jackpot!", "Win a gambling game", s -> GambleLog.drops(null, false) >= 1),
    GLOWING(Category.LAVA, "Glowing", "Get a Radioactive Vial from Lord Jawbus", s -> GambleLog.dropped("jawbus", "radioactive vial")),
    STORM_CHASER(Category.LAVA, "Storm Chaser", "Get a drop from Thunder", s -> GambleLog.dropped("thunder", null)),
    BOUNTY_HUNTER(Category.LAVA, "Bounty Hunter", "Hit in Ragnarok's shooting gallery", s -> GambleLog.dropped("ragnarok", null)),
    BUBBLE_POPPER(Category.LAVA, "Bubble Popper", "Get a drop from Plhlegblast", s -> GambleLog.dropped("plhlegblast", null)),
    HARVEST_MOON(Category.FESTIVALS, "Harvest Moon", "Get a drop from the Grim Reaper", s -> GambleLog.dropped("grim", null)),
    CLAW_MASTER(Category.FESTIVALS, "Claw Master", "Get a drop from the Yeti", s -> GambleLog.dropped("yeti", null)),
    UNWRAPPED(Category.FESTIVALS, "Unwrapped", "Get a drop from the Reindrake", s -> GambleLog.dropped("reindrake", null)),
    TEN_MILLION(Category.GAMBLING, "Ten Million Club", "One gambling drop worth 10,000,000 coins", s -> GambleLog.bestPrice() >= 10_000_000L),
    HIGH_ROLLER(Category.GAMBLING, "High Roller", "100,000,000 coins from gambling drops", s -> GambleLog.total(false) >= 100_000_000L),
    PATIENCE(Category.GAMBLING, "Patience", "50 lava mobs of one kind without a drop", s -> GambleLog.longestDry(GambleLog.LAVA) >= 50),
    FULL_SWEEP(Category.GAMBLING, "Full Sweep", "A drop from every gambling mob", s -> Achievements.fullSweep());

    public final Category category;
    public final String title;
    public final String description;
    private final Predicate<TikiStats> rule;
    private static int tickCounter;
    private static final ArrayDeque<Achievements> SHOW = new ArrayDeque<Achievements>();
    private static int showCooldown;
    private static final java.util.Set<Achievements> TESTS = java.util.EnumSet.noneOf(Achievements.class);

    private Achievements(Category category, String title, String description, Predicate<TikiStats> rule) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.rule = rule;
    }

    private static boolean fullSweep() {
        for (String m : GambleLog.MOBS) if (!GambleLog.dropped(m, null)) return false;
        return true;
    }

    /** Debug: takes an achievement away again (e.g. one unlocked by a bug). It comes back if its rule is still met. */
    public static boolean remove(TikiConfig cfg, Achievements a) {
        Set<String> have = Achievements.unlocked(cfg);
        boolean had = have.remove(a.name());
        Set<String> rev = Achievements.revoked(cfg);
        rev.add(a.name());
        cfg.lightAchievements = String.join(",", have);
        cfg.lightAchievementsRevoked = String.join(",", rev);
        cfg.save();
        return had;
    }

    /** Undoes remove: the achievement may unlock again (at once, if its rule is met). */
    public static boolean restore(TikiConfig cfg, Achievements a) {
        Set<String> rev = Achievements.revoked(cfg);
        if (!rev.remove(a.name())) return false;
        cfg.lightAchievementsRevoked = String.join(",", rev);
        cfg.save();
        return true;
    }

    public static Set<String> revoked(TikiConfig cfg) {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        if (cfg != null && cfg.lightAchievementsRevoked != null && !cfg.lightAchievementsRevoked.isBlank()) {
            out.addAll(Arrays.asList(cfg.lightAchievementsRevoked.split(",")));
        }
        return out;
    }

    private static int dye(com.skymaxx.tiki.light.dye.Dye dye) {
        return com.skymaxx.tiki.light.dye.DyeTracker.data().found.getOrDefault(dye.name(), 0);
    }

    private static int dyeKinds() {
        int n = 0;
        for (com.skymaxx.tiki.light.dye.Dye d : com.skymaxx.tiki.light.dye.Dye.values()) {
            if (Achievements.dye(d) >= 1) {
                ++n;
            }
        }
        return n;
    }

    private static boolean overdue() {
        for (com.skymaxx.tiki.light.dye.Dye d : com.skymaxx.tiki.light.dye.Dye.values()) {
            com.skymaxx.tiki.light.dye.DyeTracker.Progress p = com.skymaxx.tiki.light.dye.DyeTracker.progress(d);
            if (p.found() == 0 && p.chance() >= 0.9) {
                return true;
            }
        }
        return false;
    }

    public boolean reached(TikiStats stats) {
        try {
            return stats != null && this.rule.test(stats);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static Set<String> unlocked(TikiConfig cfg) {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        if (cfg != null && cfg.lightAchievements != null && !cfg.lightAchievements.isBlank()) {
            out.addAll(Arrays.asList(cfg.lightAchievements.split(",")));
        }
        return out;
    }

    public boolean isUnlocked(TikiConfig cfg) {
        return Achievements.unlocked(cfg).contains(this.name());
    }

    public static int count(TikiConfig cfg) {
        int n = 0;
        for (Achievements a : Achievements.values()) {
            if (a.isUnlocked(cfg)) {
                ++n;
            }
        }
        return n;
    }

    /** Called every client tick; checks once per second and announces new ones in chat. */
    public static void tick() {
        Achievements.showNext();
        if (++tickCounter < 20) {
            return;
        }
        tickCounter = 0;
        TikiConfig cfg = TikiGambleClient.config;
        TikiStats stats = TikiGambleClient.stats;
        if (cfg == null || stats == null) {
            return;
        }
        boolean firstRun = cfg.lightAchievements == null;
        Set<String> have = Achievements.unlocked(cfg);
        Set<String> revoked = Achievements.revoked(cfg);
        boolean changed = firstRun;
        for (Achievements a : Achievements.values()) {
            if (have.contains(a.name()) || revoked.contains(a.name()) || !a.reached(stats)) continue;
            have.add(a.name());
            changed = true;
            if (!firstRun) {
                TikiGambleClient.chat("§d[Light] §fAchievement unlocked: §d" + a.title + " §7- " + a.description);
            }
        }
        if (changed) {
            cfg.lightAchievements = String.join(",", have);
            cfg.save();
        }
    }

    /** Light: /light testachievement [name] (debug only) shows the unlock effect once, without unlocking anything. */
    public static void preview(Achievements which) {
        Achievements a = which == null ? FOUR_MASKS : which;
        TESTS.add(a);
        SHOW.addLast(a);
        showCooldown = 0;
    }

    public static Achievements byName(String name) {
        for (Achievements a : Achievements.values()) {
            if (a.name().equalsIgnoreCase(name)) {
                return a;
            }
        }
        return null;
    }

    /** Shows queued unlocks one by one as a title with a sound, after any drop animation is over. */
    private static void showNext() {
        if (showCooldown > 0) {
            --showCooldown;
            return;
        }
        if (SHOW.isEmpty() || TikiGambleClient.gameShouldBeSilent()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gui == null) {
            return;
        }
        Achievements a = SHOW.removeFirst();
        boolean test = TESTS.remove(a);
        if (!test) {
            TikiGambleClient.chat("\u00a7d[Light] \u00a7fAchievement unlocked: \u00a7d" + a.title + " \u00a77- " + a.description);
        }
        TikiConfig cfg = TikiGambleClient.config;
        if (cfg != null && !cfg.lightAchievementFx) {
            return;
        }
        AchievementPopup.show(a.title, a.description);
        float volume = cfg == null ? 0.5f : Math.max(0.0f, Math.min(1.0f, cfg.lightAchievementVolume));
        if (volume > 0.0f) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, volume));
        }
        showCooldown = 110;
    }

    /** Light: groups shown in the Achievements tab. */
    public enum Category {
        KILLS("Kills"),
        DROPS("Drops"),
        DYES("Dyes"),
        ANIMATIONS("Animations"),
        LUCK("Luck"),
        LAVA("Lava"),
        FESTIVALS("Festivals"),
        GAMBLING("Gambling");

        public final String title;

        Category(String title) {
            this.title = title;
        }
    }
}
