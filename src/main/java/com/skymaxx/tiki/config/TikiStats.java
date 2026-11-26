package com.skymaxx.tiki.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skymaxx.tiki.anim.Prize;
import com.skymaxx.tiki.boa.BoaPrize;
import com.skymaxx.tiki.config.DropItem;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;

public class TikiStats {
    public int masksDropped = 0;
    public int bubblesDropped = 0;
    public int dyesDropped = 0;
    public int scripturesDropped = 0;
    public int kills = 0;
    public int reachedFinalReel = 0;
    public int[] reelLandings = new int[4];
    public int dryStreak = 0;
    public int worstDryStreak = 0;
    public int boaKills = 0;
    public int lilyPadsDropped = 0;
    public int boaScripturesDropped = 0;
    public int boaDyesDropped = 0;
    public int shedsDropped = 0;
    public int snakeEyesDropped = 0;
    public int[] bulletRolls = new int[4];
    public int[] segmentsDown = new int[5];
    public int boaDryStreak = 0;
    public int boaWorstDryStreak = 0;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private transient File file;

    public void bind(File configDir) {
        this.file = new File(configDir, "stats.json");
    }

    public void load() {
        if (this.file == null || !this.file.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(this.file);){
            TikiStats loaded = (TikiStats)GSON.fromJson((Reader)reader, TikiStats.class);
            if (loaded != null) {
                this.masksDropped = loaded.masksDropped;
                this.bubblesDropped = loaded.bubblesDropped;
                this.dyesDropped = loaded.dyesDropped;
                this.scripturesDropped = loaded.scripturesDropped;
                this.kills = loaded.kills;
                this.reachedFinalReel = loaded.reachedFinalReel;
                this.dryStreak = loaded.dryStreak;
                this.worstDryStreak = loaded.worstDryStreak;
                if (loaded.reelLandings != null && loaded.reelLandings.length == 4) {
                    this.reelLandings = loaded.reelLandings;
                }
                this.boaKills = loaded.boaKills;
                this.lilyPadsDropped = loaded.lilyPadsDropped;
                this.boaScripturesDropped = loaded.boaScripturesDropped;
                this.boaDyesDropped = loaded.boaDyesDropped;
                this.shedsDropped = loaded.shedsDropped;
                this.snakeEyesDropped = loaded.snakeEyesDropped;
                this.boaDryStreak = loaded.boaDryStreak;
                this.boaWorstDryStreak = loaded.boaWorstDryStreak;
                if (loaded.bulletRolls != null && loaded.bulletRolls.length == 4) {
                    this.bulletRolls = loaded.bulletRolls;
                }
                if (loaded.segmentsDown != null && loaded.segmentsDown.length == 5) {
                    this.segmentsDown = loaded.segmentsDown;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
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

    public void recordKill(int masksLanded, boolean reachedFinalReel, Prize prize) {
        ++this.kills;
        int i = 0;
        while (i < masksLanded && i < 4) {
            int n = i++;
            this.reelLandings[n] = this.reelLandings[n] + 1;
        }
        if (reachedFinalReel) {
            ++this.reachedFinalReel;
        }
        this.count(prize);
        // Light: the result also goes into the History and the Journal
        com.skymaxx.tiki.light.GambleLog.recordExternal("tiki", "Wiki Tiki", prize == null || !prize.dropped() ? null : switch (prize) {
            case BOBBIN -> "Bobbin' Scriptures";
            case BUBBLE -> "Troubled Bubble";
            case MASK -> "Tiki Mask";
            case AQUAMARINE -> "Aquamarine Dye";
            default -> null;
        });
        if (prize != null && prize.dropped()) {
            this.dryStreak = 0;
        } else {
            ++this.dryStreak;
            if (this.dryStreak > this.worstDryStreak) {
                this.worstDryStreak = this.dryStreak;
            }
        }
        this.save();
    }

    public void count(Prize prize) {
        if (prize == null) {
            return;
        }
        switch (prize) {
            case MASK: {
                ++this.masksDropped;
                break;
            }
            case BUBBLE: {
                ++this.bubblesDropped;
                break;
            }
            case AQUAMARINE: {
                ++this.dyesDropped;
                break;
            }
            case BOBBIN: {
                ++this.scripturesDropped;
                break;
            }
        }
    }

    public void recordTitanoboa(int bullets, int down, BoaPrize prize) {
        ++this.boaKills;
        int roll = bullets - 5;
        if (roll >= 0 && roll < this.bulletRolls.length) {
            int n = roll;
            this.bulletRolls[n] = this.bulletRolls[n] + 1;
        }
        int i = 0;
        while (i < down && i < this.segmentsDown.length) {
            int n = i++;
            this.segmentsDown[n] = this.segmentsDown[n] + 1;
        }
        this.count(prize);
        // Light: the result also goes into the History and the Journal
        com.skymaxx.tiki.light.GambleLog.recordExternal("boa", "Titanoboa", prize == null || !prize.dropped() ? null : switch (prize) {
            case LILY_PAD -> "Condensed Lily Pad";
            case BOBBIN -> "Bobbin' Scriptures";
            case SHED -> "Titanoboa Shed";
            case SNAKE_EYES -> "Snake Eyes";
            case AQUAMARINE -> "Aquamarine Dye";
            default -> null;
        });
        if (prize != null && prize.dropped()) {
            this.boaDryStreak = 0;
        } else {
            ++this.boaDryStreak;
            if (this.boaDryStreak > this.boaWorstDryStreak) {
                this.boaWorstDryStreak = this.boaDryStreak;
            }
        }
        this.save();
    }

    public int counted(Prize prize) {
        if (prize == null) {
            return 0;
        }
        return switch (prize) {
            case Prize.MASK -> this.masksDropped;
            case Prize.BUBBLE -> this.bubblesDropped;
            case Prize.AQUAMARINE -> this.dyesDropped;
            case Prize.BOBBIN -> this.scripturesDropped;
            default -> 0;
        };
    }

    public int dyesTotal() {
        return this.dyesDropped + this.boaDyesDropped;
    }

    public int stampCount(Prize prize) {
        return prize == Prize.AQUAMARINE ? this.dyesTotal() : this.counted(prize);
    }

    public int stampCount(BoaPrize prize) {
        return prize == BoaPrize.AQUAMARINE ? this.dyesTotal() : this.counted(prize);
    }

    public void count(BoaPrize prize) {
        if (prize == null) {
            return;
        }
        switch (prize) {
            case LILY_PAD: {
                ++this.lilyPadsDropped;
                break;
            }
            case BOBBIN: {
                ++this.boaScripturesDropped;
                break;
            }
            case SHED: {
                ++this.shedsDropped;
                break;
            }
            case SNAKE_EYES: {
                ++this.snakeEyesDropped;
                break;
            }
            case AQUAMARINE: {
                ++this.boaDyesDropped;
                break;
            }
        }
    }

    public int counted(BoaPrize prize) {
        if (prize == null) {
            return 0;
        }
        return switch (prize) {
            case BoaPrize.LILY_PAD -> this.lilyPadsDropped;
            case BoaPrize.BOBBIN -> this.boaScripturesDropped;
            case BoaPrize.SHED -> this.shedsDropped;
            case BoaPrize.SNAKE_EYES -> this.snakeEyesDropped;
            case BoaPrize.AQUAMARINE -> this.boaDyesDropped;
            default -> 0;
        };
    }

    public int counted(DropItem item) {
        return switch (item) {
            default -> throw new MatchException(null, null);
            case DropItem.TIKI_MASK -> this.masksDropped;
            case DropItem.TROUBLED_BUBBLE -> this.bubblesDropped;
            case DropItem.CONDENSED_LILY_PAD -> this.lilyPadsDropped;
            case DropItem.TITANOBOA_SHED -> this.shedsDropped;
            case DropItem.SNAKE_EYES -> this.snakeEyesDropped;
            case DropItem.BOBBIN_SCRIPTURES -> this.scripturesDropped + this.boaScripturesDropped;
            case DropItem.AQUAMARINE_DYE -> this.dyesTotal();
        };
    }

    public int adjust(DropItem item, int delta) {
        switch (item) {
            case TIKI_MASK: {
                this.masksDropped = Math.max(0, this.masksDropped + delta);
                break;
            }
            case TROUBLED_BUBBLE: {
                this.bubblesDropped = Math.max(0, this.bubblesDropped + delta);
                break;
            }
            case CONDENSED_LILY_PAD: {
                this.lilyPadsDropped = Math.max(0, this.lilyPadsDropped + delta);
                break;
            }
            case TITANOBOA_SHED: {
                this.shedsDropped = Math.max(0, this.shedsDropped + delta);
                break;
            }
            case SNAKE_EYES: {
                this.snakeEyesDropped = Math.max(0, this.snakeEyesDropped + delta);
                break;
            }
            case BOBBIN_SCRIPTURES: {
                int[] split = TikiStats.moveShared(this.scripturesDropped, this.boaScripturesDropped, delta);
                this.scripturesDropped = split[0];
                this.boaScripturesDropped = split[1];
                break;
            }
            case AQUAMARINE_DYE: {
                int[] split = TikiStats.moveShared(this.dyesDropped, this.boaDyesDropped, delta);
                this.dyesDropped = split[0];
                this.boaDyesDropped = split[1];
            }
        }
        this.save();
        return this.counted(item);
    }

    private static int[] moveShared(int tiki, int boa, int delta) {
        if (delta >= 0) {
            return new int[]{tiki + delta, boa};
        }
        int take = -delta;
        int fromTiki = Math.min(tiki, take);
        int fromBoa = Math.min(boa, take - fromTiki);
        return new int[]{tiki - fromTiki, boa - fromBoa};
    }

    public void resetTitanoboa() {
        this.boaKills = 0;
        this.lilyPadsDropped = 0;
        this.boaScripturesDropped = 0;
        this.boaDyesDropped = 0;
        this.shedsDropped = 0;
        this.snakeEyesDropped = 0;
        this.bulletRolls = new int[4];
        this.segmentsDown = new int[5];
        this.boaDryStreak = 0;
        this.boaWorstDryStreak = 0;
        this.save();
    }

    public void reset() {
        this.masksDropped = 0;
        this.bubblesDropped = 0;
        this.dyesDropped = 0;
        this.scripturesDropped = 0;
        this.kills = 0;
        this.reachedFinalReel = 0;
        this.reelLandings = new int[4];
        this.dryStreak = 0;
        this.worstDryStreak = 0;
        this.save();
    }
}

