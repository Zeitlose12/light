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
    FIRST_TIKI("Tiki Time", "Kill your first Wiki Tiki", s -> s.kills >= 1),
    TIKI_100("Totem Regular", "Kill 100 Wiki Tikis", s -> s.kills >= 100),
    TIKI_500("Tiki Veteran", "Kill 500 Wiki Tikis", s -> s.kills >= 500),
    FIRST_BOA("Snake Charmer", "Kill your first Titanoboa", s -> s.boaKills >= 1),
    BOA_100("Boa Hunter", "Kill 100 Titanoboas", s -> s.boaKills >= 100),
    BOA_500("Boa Veteran", "Kill 500 Titanoboas", s -> s.boaKills >= 500),
    FOUR_MASKS("Four of a Kind", "Reach the fourth mask", s -> s.reachedFinalReel >= 1),
    HEADSHOT("Headshot", "Shoot the Titanoboa down to its head", s -> s.segmentsDown.length > 3 && s.segmentsDown[3] >= 1),
    SCRIPTURES("Well Read", "Get Bobbin' Scriptures", s -> s.scripturesDropped + s.boaScripturesDropped >= 1),
    BUBBLE("Bubble Trouble", "Get a Troubled Bubble", s -> s.bubblesDropped >= 1),
    MASK("Masked", "Get a Tiki Mask", s -> s.masksDropped >= 1),
    LILY("Green Thumb", "Get a Condensed Lily Pad", s -> s.lilyPadsDropped >= 1),
    SHED("Shed Happens", "Get a Titanoboa Shed", s -> s.shedsDropped >= 1),
    SNAKE_EYES("Snake Eyes", "Get Snake Eyes", s -> s.snakeEyesDropped >= 1),
    AQUAMARINE("1 in 50,000", "Get an Aquamarine Dye", s -> s.dyesDropped + s.boaDyesDropped >= 1),
    DRY_SPELL("Dry Spell", "Go 300 Wiki Tikis without a drop", s -> s.worstDryStreak >= 300),
    LUCKY("Born Lucky", "Be +50% lucky after 200 kills", s -> s.kills + s.boaKills >= 200 && Luck.overall(s) >= 50.0);

    public final String title;
    public final String description;
    private final Predicate<TikiStats> rule;
    private static int tickCounter;
    private static final ArrayDeque<Achievements> SHOW = new ArrayDeque<Achievements>();
    private static int showCooldown;
    private static final java.util.Set<Achievements> TESTS = java.util.EnumSet.noneOf(Achievements.class);

    private Achievements(String title, String description, Predicate<TikiStats> rule) {
        this.title = title;
        this.description = description;
        this.rule = rule;
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
        boolean changed = firstRun;
        for (Achievements a : Achievements.values()) {
            if (have.contains(a.name()) || !a.reached(stats)) continue;
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
        mc.gui.hud.setTimes(10, 70, 20);
        mc.gui.hud.setSubtitle((Component)Component.literal("\u00a7f" + a.title + " \u00a77\u00b7 " + a.description));
        mc.gui.hud.setTitle((Component)Component.literal("\u00a7d\u00a7lACHIEVEMENT UNLOCKED"));
        float volume = cfg == null ? 0.5f : Math.max(0.0f, Math.min(1.0f, cfg.lightAchievementVolume));
        if (volume > 0.0f) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, volume));
        }
        showCooldown = 110;
    }
}
