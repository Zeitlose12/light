/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package com.skymaxx.tiki.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skymaxx.tiki.config.GameplayCorner;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class TikiConfig {
    public static final float JUDDER_DEGREES = 0.3f;
    public static final float RESISTANCE_SCALE = 3.0f;
    public static final float CAMERA_PITCH = 8.0f;
    public static final float MASK_GAP = 0.0f;
    public static final float MASK_SCREEN_FRACTION = 0.455f;
    public static final float TOTEM_SCREEN_FRACTION = 0.9f;
    public static final double TIKI_MAX_HEALTH = 7.5E7;
    public static final double TITANOBOA_MAX_HEALTH = 4.5E7;
    public static final int CURRENT_VERSION = 8;
    public int configVersion = 10;
    public boolean tikiEnabled = true;
    public boolean requireSkyblock = true;
    public String creatureName = "Wiki Tiki";
    public String excludeNames = "Laser Totem";
    public String tikiCatchMessage = "The water bubbles and froths. A massive form emerges- you have disturbed the Wiki Tiki! You shall pay the price.";
    public String lootShareMessage = "LOOT SHARE You received loot for assisting";
    public String dropItemName = "Tiki Mask";
    public String bubbleItemName = "Troubled Bubble";
    public String aquamarineItemName = "Aquamarine Dye";
    public String bobbinItemName = "Bobbin' Scriptures";
    public double killRadius = 32.0;
    public boolean tikiOwnCatchesOnly = false;
    public boolean titanoboaEnabled = true;
    public String titanoboaName = "Titanoboa";
    public String titanoboaExcludeNames = "";
    public String titanoboaCatchMessage = "A massive Titanoboa surfaces. Its body stretches as far as the eye can see.";
    public boolean titanoboaOwnCatchesOnly = false;
    public double titanoboaMuteFromHealth = 5000000.0;
    public String lilyPadItemName = "Condensed Lily Pad";
    public String shedItemName = "Titanoboa Shed";
    public String snakeEyesItemName = "Snake Eyes";
    public float boaPace = 1.0f;
    public static final float BOA_PACE_MAX = 2.0f;
    public float boaLoadPace = 1.0f;
    public float boaShotPace = 1.0f;
    public float boaRevealPace = 1.0f;
    public float boaScrimStrength = 0.82f;
    public boolean boaGameplayView = false;
    public String boaGameplayCorner = "TOP_LEFT";
    public double muteFromHealth = 5000000.0;
    public int soundHoldMs = 300;
    public float spinSeconds1 = 2.8f;
    public float spinSeconds2 = 3.0f;
    public float spinSeconds3 = 3.4f;
    public float spinSeconds4 = 4.2f;
    public float introSeconds = 0.9f;
    public float revealPauseSeconds = 0.7f;
    public float failOutroSeconds = 1.1f;
    public float winOutroSeconds = 2.6f;
    public float stampDelaySeconds = 0.35f;
    public float stampFadeMinSeconds = 2.0f;
    public float stampFadeMaxSeconds = 5.0f;
    public float scrimStrength = 0.82f;
    public boolean soundsEnabled = true;
    public float volume = 0.85f;
    public boolean drumsEnabled = true;
    public float drumVolume = 0.95f;
    public float drumBpmMin = 104.0f;
    public float drumBpmMax = 268.0f;
    public boolean chatSummary = false;
    public boolean stampsEnabled = true;
    public boolean hotspotTextOnWater = false;
    public boolean debug = false;
    public boolean lockBazaarSellSacks = true;
    public String lightAchievements = null;
    /** Achievements taken away with /light achievement remove: they stay locked until /light achievement restore. */
    public String lightAchievementsRevoked = "";
    /** Secret achievement moments Light has seen (comma separated, see Secrets). */
    public String lightSecrets = "";
    public boolean lightAchievementFx = true;
    public float lightAchievementVolume = 0.5f;
    public String lightSellSacksSlots = "";
    public boolean dyeHudEnabled = true;
    public boolean dyeHudOnlyFishing = true;
    public float dyeHudX = 0.01f;
    public float dyeHudY = 0.30f;
    public float dyeHudScale = 1.0f;
    public boolean lavaGambleEnabled = true;
    public boolean lavaGambleJawbus = true;
    public boolean lavaGambleThunder = true;
    public boolean lavaGambleRagnarok = true;
    public boolean lavaGamblePlhlegblast = true;
    public boolean lavaGambleOwnOnly = false;
    /** Old setting (scene small in a corner) - no longer used: gambling scenes are always full screen. */
    public String gambleView = "FULL";
    /** Gambling scenes: your game, very small in a corner (like the Titanoboa's "Gameplay during Animation"). */
    public boolean gambleGameplayView = false;
    public String gambleGameplayCorner = "TOP_LEFT";
    /** Hide chat, titles and the other HUDs while a gambling scene runs (no spoiler from drop messages). */
    public boolean gambleHideHud = true;
    /** Show Hypixel's bobber countdown / "!!!" in a small box while a gambling scene runs (display only). */
    public boolean gambleBobberTimer = true;
    /** A short ping from Light when the bobber shows "!!!" during an animation. */
    public boolean gambleBiteSound = true;
    /** BOTTOM, TOP_LEFT or TOP_RIGHT. */
    public String gambleBobberPos = "BOTTOM";
    public float lavaGambleVolume = 0.6f;
    public boolean lavaGamblePrices = true;
    /** Light 1.2 beta: dry streak + RNG fanfare in the gambling result (off = everything exactly as before). */
    public boolean betaFeatures = false;
    public boolean fanfareEnabled = true;
    /** Fanfare from this many million coins (dyes always). */
    public float fanfareMillions = 10.0f;
    public boolean fanfareScreenshot = false;
    /** Sack value HUD: on/off, BOX or TEXT, SELL_OFFER or INSTA_SELL, position (-1 = top left) and size. */
    public boolean sackHudEnabled = true;
    public String sackHudStyle = "BOX";
    public String sackPriceMode = "SELL_OFFER";
    public float sackHudX = -1f;
    public float sackHudY = -1f;
    public float sackHudScale = 1.0f;
    /** Compact numbers (902,97M / 1,21B) and sacks that do not count (comma separated, e.g. "Bait Sack"). */
    public boolean sackCompact = false;
    /** How often the bazaar prices for the sack value are fetched (minutes: 5, 10 or 15). */
    public int sackRefreshMinutes = 10;
    public String sackExcluded = "";
    /** Bait sacks hold fishing bait, not loot: left out unless switched on. */
    public boolean sackBaitCounted = false;
    /** Light themes: menu and HUDs separately (chosen in the Cosmetics tab; some unlock with achievements). */
    public String menuTheme = "PINK";
    public String hudTheme = "PINK";
    /** Light Crew: fishing line and bobber look (0 = Standard, see Crew.LINES / Crew.BOBBERS). Only crew members can pick. */
    public int crewLine = 0;
    public int crewBobber = 0;
    /** Show the line / bobber looks of Light Crew members (needs crew.json from Light's GitHub, every 30 min). */
    public boolean crewCosmetics = true;
    /** Banner in the middle of the screen when a Light Crew member names a big drop in party chat. */
    public boolean crewBanner = true;
    /** Fishing stats HUD (one line): which values, Marina badge, only while fishing, position. */
    public boolean fishStatsHud = true;
    public boolean fishStatSpeed = true;
    public boolean fishStatScc = true;
    public boolean fishStatDh = true;
    public boolean fishStatTrophy = false;
    public boolean fishStatTreasure = false;
    public boolean fishStatsMarina = true;
    public boolean fishStatsOnlyFishing = true;
    public float fishStatsX = -1f;
    public float fishStatsY = -1f;
    public float fishStatsScale = 1.0f;
    /** Last fishing stats Light read (Speed, SCC, Double Hook, Trophy, Treasure; empty = unknown) - shown right after a restart. */
    public String fishStatsSaved = "";
    /** Light HUDs while a menu (inventory, chest ...) is open: HIDDEN, DIMMED or SHOWN. */
    public String hudInMenus = "HIDDEN";
    /** Bestiary ranking HUD (one line, last killed mob). */
    public boolean bestiaryHud = true;
    public boolean bestiaryHudOnlyFishing = true;
    /** Which mob the ranking HUD shows: "" = the one you killed last, else a Journal key (tiki, boa, jawbus, ...). */
    public String bestiaryHudMob = "";
    public float bestiaryHudX = -1f;
    public float bestiaryHudY = -1f;
    public float bestiaryHudScale = 1.0f;
    /** Particles along crew lines and above crew bobbers. */
    public boolean crewParticles = true;
    /** How dark the game gets behind the Light menu (0 - 0.9). */
    public float menuDim = 0.55f;
    /** Achievement pop-up position (-1 = top right) and size. */
    public float achPopupX = -1f;
    public float achPopupY = -1f;
    public float achPopupScale = 1.0f;
    /** Spooky Festival / Season of Jerry gambling (test, only with Debug log). */
    public boolean spookyGambleEnabled = true;
    public boolean winterGambleEnabled = true;
    public boolean gambleGrimReaper = true;
    public boolean gambleYeti = true;
    public boolean gambleReindrake = true;
    /** Per mob animation speed (key: jawbus, thunder, ragnarok, plhlegblast, grim, yeti, reindrake), 0.5 - 2.0. */
    public java.util.Map<String, Float> gambleSpeed = new java.util.HashMap<>();

    public float gambleSpeedOf(String key) {
        Float v = this.gambleSpeed == null ? null : this.gambleSpeed.get(key);
        return v == null ? 1.0f : Math.max(0.5f, Math.min(2.0f, v));
    }
    public String priceBazaarMode = "INSTA_SELL";
    public String priceAuctionMode = "LOWEST_BIN";
    /** Dye HUD picks the dyes itself: lava -> Carmine, water -> Aquamarine (+ Iceberg / Midnight at their events). */
    public boolean dyeHudAuto = true;
    public boolean dyeShowAquamarine = true;
    public boolean dyeShowIceberg = true;
    public boolean dyeShowCarmine = true;
    public boolean dyeShowMidnight = true;
    public int dyeVincentAquamarine = 1;
    public int dyeVincentIceberg = 1;
    public int dyeVincentCarmine = 1;
    public int dyeVincentMidnight = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private transient File file;

    public float spinSecondsFor(int tier) {
        return switch (Math.max(0, Math.min(3, tier))) {
            case 0 -> this.spinSeconds1;
            case 1 -> this.spinSeconds2;
            case 2 -> this.spinSeconds3;
            default -> this.spinSeconds4;
        };
    }

    public void bind(File configDir) {
        this.file = new File(configDir, "config.json");
    }

    public void load() {
        if (this.file == null || !this.file.exists()) {
            this.save();
            return;
        }
        try (FileReader reader = new FileReader(this.file);){
            JsonObject raw = JsonParser.parseReader((Reader)reader).getAsJsonObject();
            int version = raw.has("configVersion") ? raw.get("configVersion").getAsInt() : 0;
            TikiConfig loaded = (TikiConfig)GSON.fromJson((JsonElement)raw, TikiConfig.class);
            if (loaded != null) {
                this.copyFrom(loaded);
            }
            this.migrate(version, raw);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.configVersion = 10;
        this.clamp();
        this.save();
    }

    private void migrate(int fromVersion, JsonObject raw) {
        if (fromVersion < 9) {
            // 1.2.0: drop prices are on by default now (lava gambling is live)
            this.lavaGamblePrices = true;
        }
        if (fromVersion >= 8) {
            return;
        }
        TikiConfig fresh = new TikiConfig();
        if (fromVersion < 2) {
            this.drumVolume = fresh.drumVolume;
            this.drumBpmMin = fresh.drumBpmMin;
            this.drumBpmMax = fresh.drumBpmMax;
        }
        if (fromVersion < 3) {
            float carried;
            float low = TikiConfig.readFloat(raw, "reel3MinSeconds", Float.NaN);
            float high = TikiConfig.readFloat(raw, "reel3MaxSeconds", Float.NaN);
            float f = carried = Float.isNaN(low) || Float.isNaN(high) ? Float.NaN : (low + high) * 0.5f;
            if (!Float.isNaN(carried)) {
                this.spinSeconds3 = this.spinSeconds4 = carried;
                this.spinSeconds2 = this.spinSeconds4;
                this.spinSeconds1 = this.spinSeconds4;
            }
        }
        if (fromVersion < 4) {
            float single = TikiConfig.readFloat(raw, "spinSeconds", Float.NaN);
            if (!Float.isNaN(single)) {
                this.spinSeconds3 = this.spinSeconds4 = single;
                this.spinSeconds2 = this.spinSeconds4;
                this.spinSeconds1 = this.spinSeconds4;
            }
            this.revealPauseSeconds = fresh.revealPauseSeconds;
            this.winOutroSeconds = fresh.winOutroSeconds;
        }
        if (fromVersion < 5 && "Bobbin Scriptures".equals(this.bobbinItemName)) {
            this.bobbinItemName = fresh.bobbinItemName;
        }
        if (fromVersion < 7 && raw.has("enabled") && !TikiConfig.readBoolean(raw, "enabled", true)) {
            this.tikiEnabled = false;
        }
        if (fromVersion < 6 && TikiConfig.readDouble(raw, "muteFromHealth", Double.NaN) == 1.0E7) {
            this.muteFromHealth = fresh.muteFromHealth;
        }
    }

    private static double readDouble(JsonObject raw, String name, double fallback) {
        try {
            return raw.has(name) ? raw.get(name).getAsDouble() : fallback;
        }
        catch (Exception e) {
            return fallback;
        }
    }

    private static boolean readBoolean(JsonObject raw, String name, boolean fallback) {
        try {
            return raw.has(name) ? raw.get(name).getAsBoolean() : fallback;
        }
        catch (Exception e) {
            return fallback;
        }
    }

    private static float readFloat(JsonObject raw, String name, float fallback) {
        try {
            return raw.has(name) ? raw.get(name).getAsFloat() : fallback;
        }
        catch (Exception e) {
            return fallback;
        }
    }

    public void save() {
        if (this.file == null) {
            return;
        }
        try {
            File parent = this.file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return;
            }
            try (FileWriter writer = new FileWriter(this.file);){
                GSON.toJson((Object)this, (Appendable)writer);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void clamp() {
        this.spinSeconds1 = TikiConfig.clampF(this.spinSeconds1, 0.5f, 12.0f);
        this.spinSeconds2 = TikiConfig.clampF(this.spinSeconds2, 0.5f, 12.0f);
        this.spinSeconds3 = TikiConfig.clampF(this.spinSeconds3, 0.5f, 12.0f);
        this.spinSeconds4 = TikiConfig.clampF(this.spinSeconds4, 0.5f, 12.0f);
        this.stampFadeMinSeconds = TikiConfig.clampF(this.stampFadeMinSeconds, 0.2f, 30.0f);
        this.stampFadeMaxSeconds = TikiConfig.clampF(this.stampFadeMaxSeconds, this.stampFadeMinSeconds, 30.0f);
        this.introSeconds = TikiConfig.clampF(this.introSeconds, 0.0f, 6.0f);
        this.revealPauseSeconds = TikiConfig.clampF(this.revealPauseSeconds, 0.0f, 6.0f);
        this.failOutroSeconds = TikiConfig.clampF(this.failOutroSeconds, 0.1f, 8.0f);
        this.winOutroSeconds = TikiConfig.clampF(this.winOutroSeconds, 0.1f, 12.0f);
        this.stampDelaySeconds = TikiConfig.clampF(this.stampDelaySeconds, 0.0f, 8.0f);
        this.scrimStrength = TikiConfig.clampF(this.scrimStrength, 0.0f, 1.0f);
        this.volume = TikiConfig.clampF(this.volume, 0.0f, 1.0f);
        this.drumVolume = TikiConfig.clampF(this.drumVolume, 0.0f, 1.0f);
        this.drumBpmMin = TikiConfig.clampF(this.drumBpmMin, 40.0f, 300.0f);
        this.drumBpmMax = TikiConfig.clampF(this.drumBpmMax, this.drumBpmMin, 400.0f);
        this.killRadius = Math.max(4.0, Math.min(128.0, this.killRadius));
        this.muteFromHealth = Math.max(0.0, Math.min(7.5E7, this.muteFromHealth));
        this.titanoboaMuteFromHealth = Math.max(0.0, Math.min(4.5E7, this.titanoboaMuteFromHealth));
        this.soundHoldMs = Math.max(0, Math.min(1000, this.soundHoldMs));
        this.boaPace = TikiConfig.clampF(this.boaPace, 0.6f, 2.0f);
        this.boaLoadPace = TikiConfig.clampF(this.boaLoadPace, 0.5f, 2.0f);
        this.boaShotPace = TikiConfig.clampF(this.boaShotPace, 0.5f, 2.0f);
        this.boaRevealPace = TikiConfig.clampF(this.boaRevealPace, 0.5f, 2.0f);
        this.boaScrimStrength = TikiConfig.clampF(this.boaScrimStrength, 0.0f, 1.0f);
        if (this.dropItemName == null || this.dropItemName.isBlank()) {
            this.dropItemName = "Tiki Mask";
        }
        if (this.bubbleItemName == null || this.bubbleItemName.isBlank()) {
            this.bubbleItemName = "Troubled Bubble";
        }
        if (this.aquamarineItemName == null || this.aquamarineItemName.isBlank()) {
            this.aquamarineItemName = "Aquamarine Dye";
        }
        if (this.bobbinItemName == null || this.bobbinItemName.isBlank()) {
            this.bobbinItemName = "Bobbin' Scriptures";
        }
        if (this.titanoboaName == null || this.titanoboaName.isBlank()) {
            this.titanoboaName = "Titanoboa";
        }
        if (this.titanoboaExcludeNames == null) {
            this.titanoboaExcludeNames = "";
        }
        if (this.tikiCatchMessage == null || this.tikiCatchMessage.isBlank()) {
            this.tikiCatchMessage = new TikiConfig().tikiCatchMessage;
        }
        if (this.titanoboaCatchMessage == null || this.titanoboaCatchMessage.isBlank()) {
            this.titanoboaCatchMessage = new TikiConfig().titanoboaCatchMessage;
        }
        if (this.lootShareMessage == null || this.lootShareMessage.isBlank()) {
            this.lootShareMessage = new TikiConfig().lootShareMessage;
        }
        if (this.lilyPadItemName == null || this.lilyPadItemName.isBlank()) {
            this.lilyPadItemName = "Condensed Lily Pad";
        }
        if (this.shedItemName == null || this.shedItemName.isBlank()) {
            this.shedItemName = "Titanoboa Shed";
        }
        if (this.snakeEyesItemName == null || this.snakeEyesItemName.isBlank()) {
            this.snakeEyesItemName = "Snake Eyes";
        }
        this.boaGameplayCorner = GameplayCorner.byName(this.boaGameplayCorner).name();
    }

    private static float clampF(float v, float min, float max) {
        if (Float.isNaN(v)) {
            return min;
        }
        return v < min ? min : (v > max ? max : v);
    }

    private void copyFrom(TikiConfig other) {
        for (Field f : TikiConfig.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.set(this, f.get(other));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }
}

