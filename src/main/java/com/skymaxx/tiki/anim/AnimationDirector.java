package com.skymaxx.tiki.anim;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.anim.Prize;
import com.skymaxx.tiki.anim.Reel;
import com.skymaxx.tiki.audio.AudioDirector;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.config.TikiStats;
import com.skymaxx.tiki.detect.SeaCreatureDetector;
import com.skymaxx.tiki.render.StampOverlay;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

public class AnimationDirector
implements SeaCreatureDetector.Reveal<Prize> {
    private static final float[] CHANCES = new float[]{0.7f, 0.5f, 0.23f};
    public static final int SCARE_ODDS = 500;
    public static final float SCARE_AUDIO_LEAD = 0.14f;
    public static final float SCARE_LINE_ONE = 1.8f;
    public static final float SCARE_LINE_ONE_OUT = 5.3f;
    public static final float SCARE_FLASH = 5.55f;
    public static final float SCARE_FLASH_SECONDS = 0.07f;
    public static final float SCARE_LINE_TWO = 5.6f;
    public static final float SCARE_TYPE_SECONDS = 1.1f;
    public static final float SCARE_HOLD_UNTIL = 10.8f;
    public static final float SCARE_FADE_SECONDS = 2.6f;
    private static final long SCARE_AFTERMATH_MS = 6000L;
    public static final float SCARE_AFTERMATH_FLASH = 3.2f;
    private static final float JACKPOT_HOLD = 7.2f;
    public static final float CHAIN_AFTER_PAUSE = 0.1f;
    public static final float CHAIN_STEP = 0.22f;
    public static final float DYE_CHAIN_STEP = 0.3f;
    public static final float TURN_SECONDS = 0.7f;
    public static final float DOUBLE_TURN_SECONDS = 1.0f;
    private static final float[] TURN_AT = new float[]{0.0f, 0.14f, 0.46f, 0.8f, 0.91f, 1.0f};
    private static final float[] TURN_DEGREES = new float[]{0.0f, -14.0f, 180.0f, 372.0f, 357.5f, 360.0f};
    private static final float[] TURN_SPEED = new float[]{0.0f, 0.0f, 1100.0f, 0.0f, 0.0f, 0.0f};
    public static final float BUBBLE_MORPH_FROM_DEGREES = 100.0f;
    public static final float BUBBLE_MORPH_TO_DEGREES = 260.0f;
    public static final float DYE_MORPH_FROM_DEGREES = 460.0f;
    public static final float DYE_MORPH_TO_DEGREES = 620.0f;
    public static final float CHAIN_AUDIO_LEAD = 0.14f;
    public static final float BREAK_AUDIO_LEAD = 0.14f;
    public static final float BUILD_FROM_DEGREES = 135.0f;
    private static final long ARM_WINDOW_MS = 2500L;
    private final TikiConfig cfg;
    private final TikiStats stats;
    private final AudioDirector audio;
    private final StampOverlay stamp;
    private final Random rng = new Random();
    private static final SplittableRandom SEEDS = new SplittableRandom();
    private Phase phase = Phase.IDLE;
    private float phaseTime;
    private final Reel[] reels = new Reel[4];
    private int active;
    private Prize prize = Prize.NONE;
    private Prize confirmed = Prize.NONE;
    private final EnumSet<Prize> drops = EnumSet.noneOf(Prize.class);
    private Prize extra = Prize.NONE;
    private boolean outcomesLocked;
    private boolean forcedTest;
    private boolean recorded;
    private float takeover;
    private int lastMasksLanded;
    private long lastUpdateNanos;
    private long armedUntilMs;
    public static final float SPLIT_TIME = 0.55f;
    public static final float RECENTRE_TIME = 0.85f;
    public static final float PAIR_FAIL_PAUSE = 0.4f;
    public static final float PAIR_FAIL_OUTRO = 0.15f;
    private static final float PAIR_LEAVE_RATE = 4.5f;
    private static final float SLIDE_ONCE_VISIBLE = 0.35f;
    private static final float DUO_TRIM = 0.68f;
    private AnimationDirector second;
    private AnimationDirector first;
    private boolean isSecond;
    private float split;
    private boolean handedOver;
    private boolean hurried;
    private boolean untrimmed;
    private final Prize[] heldStamps = new Prize[]{Prize.NONE, Prize.NONE};
    private final int[] heldNumbers = new int[2];
    private boolean spent;
    private boolean forced;
    private boolean secondIsTest;
    private Prize pendingSecond = Prize.NONE;
    private float pendingSecondIn = -1.0f;
    private boolean scare;
    private boolean scareShown;
    private boolean scareCued;
    private boolean breakCued;
    private float crownTime = -1.0f;
    private int chainTurnsHeard;
    private Prize stamped = Prize.NONE;
    private boolean scareForced;
    private long aftermathUntilMs;

    public AnimationDirector(TikiConfig cfg, TikiStats stats, AudioDirector audio, StampOverlay stamp) {
        this.cfg = cfg;
        this.stats = stats;
        this.audio = audio;
        this.stamp = stamp;
        for (int i = 0; i < 4; ++i) {
            this.reels[i] = new Reel(i);
        }
    }

    @Override
    public void arm() {
        if (this.phase != Phase.IDLE) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean wasArmed = this.armed();
        this.armedUntilMs = now + 2500L;
        if (!wasArmed) {
            this.audio.setMuted(true);
        }
    }

    @Override
    public boolean idle() {
        return this.phase == Phase.IDLE && !this.armed();
    }

    public boolean armed() {
        return this.armedUntilMs != 0L && System.currentTimeMillis() < this.armedUntilMs;
    }

    private void disarm() {
        this.armedUntilMs = 0L;
    }

    @Override
    public void begin() {
        this.begin(Prize.NONE);
    }

    public void begin(Prize known) {
        if (!(this.phase != Phase.RELEASING || this.isSecond || this.second != null && this.second.owningScreen())) {
            this.phase = Phase.IDLE;
        }
        if (this.phase != Phase.IDLE) {
            this.openSecond(known);
            return;
        }
        for (int i = 0; i < 4; ++i) {
            this.reels[i] = new Reel(i);
        }
        this.rng.setSeed(SEEDS.nextLong());
        if (!this.isSecond) {
            this.pendingSecondIn = -1.0f;
        }
        this.confirmed = this.prize = known != null && known.dropped() ? known : Prize.NONE;
        this.drops.clear();
        if (this.prize.dropped()) {
            this.drops.add(this.prize);
        }
        this.extra = Prize.NONE;
        this.outcomesLocked = false;
        this.forcedTest = false;
        this.recorded = false;
        this.spent = false;
        this.forced = false;
        this.scare = false;
        this.scareShown = false;
        this.scareCued = false;
        this.crownTime = -1.0f;
        this.chainTurnsHeard = 0;
        this.stamped = Prize.NONE;
        this.scareForced = false;
        this.aftermathUntilMs = 0L;
        this.active = 0;
        this.handedOver = false;
        this.hurried = false;
        if (this.isSecond) {
            this.split = 1.0f;
        } else if (this.second == null) {
            this.split = 0.0f;
        }
        this.phase = Phase.INTRO;
        this.phaseTime = 0.0f;
        this.disarm();
        this.audio.setMuted(true);
        if (!this.isSecond) {
            this.audio.onAnimationStart();
        }
    }

    private void openSecond(Prize known) {
        if (this.isSecond || !this.scareMayGiveWay() || this.second != null && this.second.owningScreen()) {
            return;
        }
        if (this.scare) {
            this.scare = false;
            this.reels[this.reels.length - 1].reviseOutcome(false);
        }
        if (this.second == null) {
            this.second = new AnimationDirector(this.cfg, this.stats, this.audio, this.stamp);
            this.second.isSecond = true;
            this.second.first = this;
        }
        if (this.visibility() < 0.35f) {
            this.split = 1.0f;
            if (this.phase == Phase.INTRO) {
                this.phaseTime = 0.0f;
                this.takeover = 0.0f;
            }
        }
        this.untrimmed = false;
        this.second.begin(known);
        if (this.secondIsTest) {
            this.second.forcedTest = true;
            this.second.rollOutcomes(-1, this.second.prize.dropped());
            this.second.outcomesLocked = true;
            this.secondIsTest = false;
        }
        this.audio.setEnsemble(0.68f);
    }

    private AnimationDirector other() {
        AnimationDirector twin = this.isSecond ? this.first : this.second;
        return twin != null && twin.owningScreen() ? twin : null;
    }

    private boolean maySettle() {
        if (this.forced) {
            return true;
        }
        AnimationDirector twin = this.other();
        return twin == null || !twin.playing() || twin.spent || !twin.inEnding();
    }

    private boolean playing() {
        return this.phase != Phase.IDLE && this.phase != Phase.RELEASING;
    }

    private boolean twinPlaying() {
        AnimationDirector twin = this.other();
        return twin != null && twin.playing();
    }

    private float failPauseSeconds() {
        return this.hurried ? Math.min(this.cfg.revealPauseSeconds, 0.4f) : this.cfg.revealPauseSeconds;
    }

    private float failOutroSeconds() {
        return this.hurried ? Math.min(this.cfg.failOutroSeconds, 0.15f) : this.cfg.failOutroSeconds;
    }

    private boolean failing() {
        return this.phase == Phase.FAIL_OUTRO || this.phase == Phase.REVEAL && !this.reels[this.active].won();
    }

    private boolean inEnding() {
        if (this.phase == Phase.FAIL_OUTRO || this.phase == Phase.WIN_OUTRO || this.phase == Phase.SCARE) {
            return true;
        }
        return this.phase == Phase.REVEAL && (!this.reels[this.active].won() || this.active >= this.prize.crownTier());
    }

    private boolean leads() {
        return this.isSecond ? this.first != null && this.first.handedOver : !this.handedOver;
    }

    private AnimationDirector owner() {
        return this.isSecond && this.first != null ? this.first : this;
    }

    private void printStamp(Prize printed, int number) {
        AnimationDirector twin;
        AnimationDirector animationDirector = twin = this.isSecond ? this.first : this.second;
        if (!this.forced && twin != null && twin.owningScreen() && twin.playing() && !twin.spent) {
            this.owner().hold(printed, number);
            return;
        }
        this.owner().releaseHeldStamps();
        this.stamp.trigger(printed, number, this.cfg, this.rng);
    }

    private void hold(Prize printed, int number) {
        for (int i = 0; i < this.heldStamps.length; ++i) {
            if (this.heldStamps[i].dropped()) continue;
            this.heldStamps[i] = printed;
            this.heldNumbers[i] = number;
            return;
        }
    }

    private void releaseHeldStamps() {
        for (int i = 0; i < this.heldStamps.length; ++i) {
            if (!this.heldStamps[i].dropped()) continue;
            this.stamp.trigger(this.heldStamps[i], this.heldNumbers[i], this.cfg, this.rng);
            this.heldStamps[i] = Prize.NONE;
        }
    }

    private float visibility() {
        if (this.phase != Phase.INTRO) {
            return this.takeover;
        }
        float t = Math.min(1.0f, this.phaseTime / Math.max(0.05f, this.cfg.introSeconds));
        float rise = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
        return this.takeover * rise;
    }

    public AnimationDirector second() {
        return this.second != null && this.second.owningScreen() ? this.second : null;
    }

    public float split() {
        return this.split;
    }

    public boolean leaving() {
        return !this.playing();
    }

    @Override
    public boolean acceptsSecond() {
        return !this.isSecond && !this.spent && this.scareMayGiveWay() && (this.second == null || !this.second.owningScreen());
    }

    private boolean scareMayGiveWay() {
        return !this.scare || !this.scareShown && !this.scareCued && !this.scareForced;
    }

    @Override
    public void notifyDrop(Prize dropped) {
        boolean overBobbin;
        if (dropped == null || !dropped.dropped()) {
            return;
        }
        if (!this.isSecond && this.giveToSecond(dropped)) {
            return;
        }
        if (this.phase == Phase.IDLE) {
            this.begin(dropped);
            return;
        }
        if (this.recorded) {
            if (this.drops.add(dropped)) {
                this.confirmed = Prize.best(this.confirmed, dropped);
                this.lateDrop(dropped);
            }
            return;
        }
        if (!this.drops.add(dropped)) {
            return;
        }
        this.confirmed = Prize.best(this.confirmed, dropped);
        Prize raised = Prize.best(this.prize, dropped);
        if (raised == this.prize) {
            this.considerExtra();
            return;
        }
        if (!this.outcomesLocked) {
            this.prize = raised;
            this.considerExtra();
            return;
        }
        boolean bl = overBobbin = this.prize == Prize.BOBBIN && raised.crownTier() > this.prize.crownTier() && this.faceUnseen(Prize.BOBBIN.crownTier());
        if (this.revisable() || overBobbin) {
            this.prize = raised;
            this.rollOutcomes(-1, true);
            this.considerExtra();
        }
    }

    private void considerExtra() {
        if (this.extra.dropped() || this.scare || this.isSecond || this.second != null && this.second.owningScreen()) {
            return;
        }
        Prize lesser = Prize.BOBBIN;
        if (this.drops.contains(lesser) && this.prize.crownTier() > lesser.crownTier() && this.faceUnseen(lesser.crownTier())) {
            this.extra = lesser;
        }
    }

    private boolean faceUnseen(int tier) {
        if (this.phase == Phase.IDLE || this.phase == Phase.INTRO) {
            return true;
        }
        if (this.active < tier) {
            return true;
        }
        return this.active == tier && this.phase == Phase.SPINNING && this.reels[tier].angle() < 90.0f;
    }

    @Override
    public void lateDrop(Prize dropped) {
        if (dropped == null || !dropped.dropped()) {
            return;
        }
        this.stats.count(dropped);
        this.stats.dryStreak = 0;
        this.stats.save();
        if (dropped.stamps()) {
            this.printStamp(dropped, this.stats.stampCount(dropped));
        }
    }

    private boolean giveToSecond(Prize dropped) {
        if (this.second == null || !this.second.owningScreen()) {
            return false;
        }
        if (!this.confirmed.dropped() && this.revisable()) {
            return false;
        }
        if (!this.second.confirmed.dropped()) {
            this.second.notifyDrop(dropped);
            return true;
        }
        if (!this.confirmed.dropped()) {
            return false;
        }
        if (this.second.revisable()) {
            this.second.notifyDrop(dropped);
            return true;
        }
        return false;
    }

    private boolean revisable() {
        if (this.phase == Phase.INTRO) {
            return true;
        }
        return this.phase == Phase.SPINNING && this.active == 0 && this.reels[0].angle() < 90.0f;
    }

    public void beginPreview(List<Prize> rolled) {
        this.phase = Phase.IDLE;
        this.second = null;
        this.split = 0.0f;
        this.begin(Prize.NONE);
        this.forcedTest = true;
        for (Prize drop : rolled) {
            this.notifyDrop(drop);
        }
    }

    public void beginTest(Prize forced) {
        this.beginTest(forced == null || !forced.dropped() ? 0 : forced.tiers(), forced);
    }

    public void beginTest(int masksToLand, Prize forced) {
        this.phase = Phase.IDLE;
        this.second = null;
        this.split = 0.0f;
        this.begin(forced);
        this.forcedTest = true;
        this.rollOutcomes(masksToLand, this.prize.dropped());
        this.outcomesLocked = true;
    }

    public void beginTestScare() {
        this.phase = Phase.IDLE;
        this.second = null;
        this.split = 0.0f;
        this.begin(Prize.NONE);
        this.forcedTest = true;
        this.scareForced = true;
        this.rollOutcomes(-1, false);
        this.outcomesLocked = true;
    }

    public void beginTestPair(Prize firstPrize, Prize secondPrize) {
        this.beginTestPair(firstPrize, secondPrize, 0.0f);
    }

    public void beginTestPair(Prize firstPrize, Prize secondPrize, float delaySeconds) {
        if (firstPrize != null && firstPrize.dropped()) {
            this.beginTest(firstPrize);
        } else {
            this.beginRolledTest();
        }
        this.pendingSecond = secondPrize == null ? Prize.NONE : secondPrize;
        this.pendingSecondIn = Math.max(0.0f, delaySeconds);
        if (this.pendingSecondIn <= 0.0f) {
            this.openPendingSecond();
        }
    }

    public void beginTestDrops(Prize a, Prize b) {
        Prize other;
        Prize best = Prize.best(a, b);
        Prize prize = other = best == a ? b : a;
        if (best.dropped()) {
            this.beginTest(best);
        } else {
            this.beginRolledTest();
        }
        this.notifyDrop(other);
    }

    private void beginRolledTest() {
        this.phase = Phase.IDLE;
        this.second = null;
        this.split = 0.0f;
        this.begin(Prize.NONE);
        this.forcedTest = true;
    }

    private void openPendingSecond() {
        boolean free;
        if (this.acceptsSecond()) {
            Prize next = this.pendingSecond;
            this.pendingSecondIn = -1.0f;
            this.pendingSecond = Prize.NONE;
            this.secondIsTest = true;
            this.openSecond(next);
            return;
        }
        boolean bl = free = !this.owningScreen() || this.phase == Phase.RELEASING && (this.second == null || !this.second.owningScreen());
        if (free) {
            Prize next = this.pendingSecond;
            this.pendingSecondIn = -1.0f;
            this.pendingSecond = Prize.NONE;
            if (next.dropped()) {
                this.beginTest(next);
            } else {
                this.beginRolledTest();
            }
        }
    }

    public void skip() {
        if (this.second != null) {
            this.second.skip();
        }
        if (this.phase == Phase.IDLE || this.phase == Phase.RELEASING) {
            return;
        }
        this.forced = true;
        if (this.prize == Prize.AQUAMARINE && this.crownTime >= 0.0f) {
            this.audio.onJackpotSkipped();
        }
        if (!this.outcomesLocked) {
            this.rollOutcomes(-1, this.prize.dropped());
            this.outcomesLocked = true;
        }
        if (this.scare && !this.scareShown) {
            if (this.scareCued) {
                this.audio.onScareSkipped();
            }
            this.scare = false;
            this.reels[this.reels.length - 1].reviseOutcome(false);
        } else if (this.scareShown && this.phase == Phase.SCARE) {
            this.audio.onScareSkipped();
            this.scareShown = false;
        }
        for (int i = 0; i <= this.prize.crownTier(); ++i) {
            this.reels[i].forceLanded();
            this.active = i;
            if (!this.reels[i].won()) break;
        }
        if (!this.scare && this.crowned()) {
            this.crownTime = 99.0f;
            this.chainTurnsHeard = this.prize.crownTier() + 1;
        }
        this.finish(this.scare ? false : this.crowned());
    }

    public void update(float dt) {
        AnimationDirector pair;
        this.lastUpdateNanos = System.nanoTime();
        if (!this.isSecond && this.pendingSecondIn >= 0.0f) {
            this.pendingSecondIn = Math.max(0.0f, this.pendingSecondIn - dt);
            if (this.pendingSecondIn <= 0.0f) {
                this.openPendingSecond();
            }
        }
        this.step(dt);
        if (this.second != null) {
            this.second.update(dt);
            if (this.phase == Phase.IDLE && !this.second.owningScreen()) {
                this.second = null;
                this.audio.setEnsemble(1.0f);
            }
        }
        if (this.isSecond) {
            return;
        }
        AnimationDirector animationDirector = pair = this.second != null && this.second.owningScreen() ? this.second : null;
        if (pair != null) {
            boolean mine = this.playing();
            boolean theirs = pair.playing();
            if (mine && theirs) {
                this.split = AnimationDirector.approach(this.split, 1.0f, dt / 0.55f);
            } else if (mine && !this.failing()) {
                this.split = AnimationDirector.approach(this.split, 0.0f, dt / 0.85f);
            } else if (theirs && !pair.failing()) {
                pair.split = AnimationDirector.approach(pair.split, 0.0f, dt / 0.85f);
            }
            if (mine != theirs && !this.untrimmed) {
                float f = mine ? pair.takeover : this.takeover;
                if (f <= 0.02f) {
                    this.untrimmed = true;
                    this.audio.setEnsemble(1.0f);
                }
            }
        } else if (this.phase == Phase.IDLE && this.takeover <= 0.001f) {
            this.split = 0.0f;
        } else if (this.playing() && !this.failing()) {
            this.split = AnimationDirector.approach(this.split, 0.0f, dt / 0.85f);
        }
    }

    private void step(float dt) {
        float rate;
        if (this.phase == Phase.IDLE) {
            this.takeover = AnimationDirector.approach(this.takeover, 0.0f, dt * 4.0f);
            if (this.armedUntilMs != 0L && !this.armed()) {
                this.disarm();
            }
            return;
        }
        this.phaseTime += dt;
        float target = this.phase == Phase.RELEASING ? 0.0f : 1.0f;
        float f = rate = this.phase == Phase.INTRO ? 3.2f : 2.6f;
        if (this.phase == Phase.RELEASING && this.twinPlaying()) {
            rate = 4.5f;
        }
        if (this.phase == Phase.RELEASING && this.scareShown) {
            rate = 0.3846154f;
        }
        this.takeover = AnimationDirector.approach(this.takeover, target, dt * rate);
        for (Reel reel : this.reels) {
            reel.tick(dt);
        }
        if (this.crownTime >= 0.0f) {
            this.crownTime += dt;
            this.chainSounds();
        }
        if (this.leads()) {
            this.audio.update(dt, this.phase == Phase.SPINNING, this.active, this.reels[this.active].angle(), this.climb());
        }
        switch (this.phase.ordinal()) {
            case 1: {
                if (!(this.phaseTime >= this.cfg.introSeconds)) break;
                if (!this.outcomesLocked) {
                    this.rollOutcomes(-1, this.prize.dropped());
                    this.outcomesLocked = true;
                }
                this.startReel(0);
                break;
            }
            case 2: {
                Reel reel;
                if (this.scare && this.active == this.reels.length - 1) {
                    reel = this.reels[this.active];
                    float toCorner = reel.secondsToCorner();
                    if (!this.scareCued && toCorner <= 0.14f) {
                        this.scareCued = true;
                        this.audio.onScareRevealed(toCorner);
                    }
                    if (!(toCorner <= 0.0f)) break;
                    if (!this.scareCued) {
                        this.scareCued = true;
                        this.audio.onScareRevealed(0.0f);
                    }
                    this.phase = Phase.SCARE;
                    this.phaseTime = 0.0f;
                    this.scareShown = true;
                    break;
                }
                if (!this.reels[this.active].landed()) {
                    reel = this.reels[this.active];
                    if (this.breakCued || !this.leads() || !(reel.secondsToLanding() <= 0.14f)) break;
                    this.breakCued = true;
                    this.audio.onBreakAhead();
                    break;
                }
                this.breakCued = false;
                this.phase = Phase.REVEAL;
                this.phaseTime = 0.0f;
                if (this.active == this.prize.crownTier() && this.reels[this.active].won() && this.prize.dropped()) {
                    this.crownTime = 0.0f;
                }
                if (this.prize == Prize.AQUAMARINE && this.active == this.prize.crownTier() && this.reels[this.active].won()) {
                    this.audio.onJackpotLanded();
                    break;
                }
                if (this.leads()) {
                    this.audio.onReelLanded(this.active, this.reels[this.active].won());
                    if (this.active != this.extraTier() || !this.reels[this.active].won()) break;
                    this.audio.onExtraLanded();
                    break;
                }
                this.audio.onSecondLanded(this.active, this.reels[this.active].won());
                break;
            }
            case 3: {
                boolean bare;
                boolean bl = bare = !this.reels[this.active].won();
                if (bare && this.twinPlaying()) {
                    this.hurried = true;
                }
                float f2 = bare ? this.failPauseSeconds() : this.cfg.revealPauseSeconds;
                if (!(this.phaseTime >= f2)) break;
                if (bare) {
                    this.phase = Phase.FAIL_OUTRO;
                    this.phaseTime = 0.0f;
                    if (!this.leads() || this.twinPlaying()) break;
                    this.audio.onFailOutro();
                    break;
                }
                if (this.active < this.prize.crownTier()) {
                    this.startReel(this.active + 1);
                    break;
                }
                this.phase = Phase.WIN_OUTRO;
                this.phaseTime = 0.0f;
                if (this.prize != Prize.AQUAMARINE && this.leads()) {
                    this.audio.onWin(this.prize.celebration());
                    break;
                }
                if (this.prize == Prize.AQUAMARINE) break;
                this.audio.onSecondWin(this.prize.celebration());
                break;
            }
            case 4: {
                if (!(this.phaseTime >= this.failOutroSeconds())) break;
                this.finish(false);
                break;
            }
            case 5: {
                if (!(this.phaseTime >= this.winOutroSeconds())) break;
                this.finish(true);
                break;
            }
            case 6: {
                if (!(this.phaseTime >= 10.8f)) break;
                this.finish(false);
                break;
            }
            case 7: {
                boolean waiting;
                boolean bl = waiting = !this.isSecond && this.second != null && this.second.owningScreen();
                if (!(this.takeover <= 0.02f) || !(this.phaseTime >= this.cfg.stampDelaySeconds) || waiting) break;
                if (this.scareShown) {
                    this.aftermathUntilMs = System.currentTimeMillis() + 6000L;
                }
                this.phase = Phase.IDLE;
                this.phaseTime = 0.0f;
                break;
            }
        }
    }

    private void startReel(int i) {
        this.active = i;
        this.phase = Phase.SPINNING;
        this.phaseTime = 0.0f;
        this.breakCued = false;
        float duration = this.cfg.spinSecondsFor(i);
        this.reels[i].start(duration, this.cfg, this.rng);
        if (this.leads()) {
            this.audio.onReelStart(i, duration);
        } else {
            this.audio.onSecondReelStart(i, duration);
        }
    }

    private void rollOutcomes(int forceMasks, boolean dropped) {
        int i;
        boolean[] win = new boolean[4];
        this.scare = false;
        if (forceMasks >= 0) {
            for (i = 0; i < 4; ++i) {
                win[i] = i < forceMasks;
            }
        } else if (dropped) {
            for (i = 0; i < 4; ++i) {
                win[i] = i <= this.prize.crownTier();
            }
        } else if (this.scareForced || !this.isSecond && this.second == null && this.rng.nextInt(500) == 0) {
            this.scare = true;
            for (i = 0; i < 4; ++i) {
                win[i] = true;
            }
        } else {
            win[0] = this.rng.nextFloat() < CHANCES[0];
            win[1] = win[0] && this.rng.nextFloat() < CHANCES[1];
            win[2] = win[1] && this.rng.nextFloat() < CHANCES[2];
            win[3] = false;
        }
        for (i = 0; i < 4; ++i) {
            this.reels[i].reviseOutcome(win[i]);
        }
    }

    private void finish(boolean won) {
        int landed;
        this.spent = true;
        if (won && !this.maySettle()) {
            return;
        }
        AnimationDirector twin = this.other();
        boolean early = !this.forced && twin != null && twin.playing() && !twin.spent;
        this.lastMasksLanded = landed = this.scare ? this.reels.length - 1 : this.masksLanded();
        boolean reachedFinalReel = this.scare || landed >= 3 && this.prize.crownTier() == this.reels.length - 1;
        this.record(landed, reachedFinalReel);
        if (this.confirmed.stamps()) {
            int number = this.stats.stampCount(this.confirmed);
            if (this.forcedTest) {
                number += 1 + (this.previewStampedBeside(this.confirmed) ? 1 : 0);
            }
            this.stamped = this.confirmed;
            this.printStamp(this.confirmed, number);
        } else if (!early) {
            this.owner().releaseHeldStamps();
        }
        if (this.cfg.chatSummary) {
            String line = this.confirmed == Prize.MASK ? "\u00a76Tiki Mask \u00a7e#" + (this.stats.masksDropped + (this.forcedTest ? 1 : 0)) + "\u00a76!" : this.confirmed.announcement();
            StringBuilder also = new StringBuilder();
            for (Prize drop : this.drops) {
                if (drop == this.confirmed) continue;
                also.append(" \u00a77+ ").append(drop.title());
            }
            TikiGambleClient.chat(line != null ? line + String.valueOf(also) : "\u00a78Totem stopped at \u00a77" + landed + "\u00a78/4.");
        }
        if (early && this.leads()) {
            this.handedOver = true;
            if (twin.leads() && !twin.failing()) {
                twin.takeLead();
            }
        } else if (this.leads()) {
            this.audio.onAnimationEnd(won);
        }
        this.phase = Phase.RELEASING;
        this.phaseTime = 0.0f;
    }

    private void takeLead() {
        this.audio.resumeBed(this.active, this.phase == Phase.SPINNING ? this.reels[this.active].angle() : 0.0f, this.climb());
    }

    private boolean previewStampedBeside(Prize printed) {
        AnimationDirector twin = this.isSecond ? this.first : this.second;
        return twin != null && twin.forcedTest && twin.stamped == printed;
    }

    private void record(int landed, boolean reachedFinalReel) {
        if (this.recorded) {
            return;
        }
        this.recorded = true;
        if (!this.forcedTest) {
            this.stats.recordKill(landed, reachedFinalReel, this.confirmed);
            boolean more = false;
            for (Prize drop : this.drops) {
                if (drop == this.confirmed) continue;
                this.stats.count(drop);
                more = true;
            }
            if (more) {
                this.stats.save();
            }
        }
    }

    private float winOutroSeconds() {
        float seconds = this.cfg.winOutroSeconds * (0.6f + 0.4f * this.prize.celebration());
        if (this.prize == Prize.AQUAMARINE) {
            seconds = Math.max(seconds, 7.2f - this.cfg.revealPauseSeconds);
        }
        return seconds;
    }

    public static int chainTurns(Prize prize) {
        return switch (prize) {
            case Prize.BUBBLE, Prize.MASK -> 1;
            case Prize.AQUAMARINE -> 2;
            default -> 0;
        };
    }

    public static float chainSeconds(Prize prize) {
        return AnimationDirector.chainTurns(prize) >= 2 ? 1.0f : 0.7f;
    }

    public static float chainStep(Prize prize) {
        return prize == Prize.AQUAMARINE ? 0.3f : 0.22f;
    }

    public static boolean chainChangesSkin(Prize prize) {
        return prize == Prize.BUBBLE || prize == Prize.AQUAMARINE;
    }

    private float chainStart(int tier) {
        return this.cfg.revealPauseSeconds + 0.1f + (float)(this.prize.crownTier() - tier) * AnimationDirector.chainStep(this.prize);
    }

    private void chainSounds() {
        if (AnimationDirector.chainTurns(this.prize) == 0 || !this.crowned()) {
            return;
        }
        int crown = this.prize.crownTier();
        while (this.chainTurnsHeard <= crown && this.crownTime >= this.chainStart(crown - this.chainTurnsHeard) - 0.14f) {
            float lead = Math.max(0.0f, this.chainStart(crown - this.chainTurnsHeard) - this.crownTime);
            this.audio.onChainTurn(this.prize, this.chainTurnsHeard, lead, this.chainTurnsHeard == crown);
            ++this.chainTurnsHeard;
        }
    }

    public float chainSince(int tier) {
        if (AnimationDirector.chainTurns(this.prize) == 0 || this.crownTime < 0.0f || tier < 0 || tier > this.prize.crownTier()) {
            return -1.0f;
        }
        return this.crownTime - this.chainStart(tier);
    }

    public float chainTurn(int tier) {
        float since = this.chainSince(tier);
        return since < 0.0f ? -1.0f : Math.min(1.0f, since / AnimationDirector.chainSeconds(this.prize));
    }

    public float chainSpin(int tier) {
        float turn = this.chainTurn(tier);
        return turn <= 0.0f || turn >= 1.0f ? 0.0f : AnimationDirector.turnDegrees(turn, AnimationDirector.chainTurns(this.prize));
    }

    public float chainMorph(int tier) {
        if (!AnimationDirector.chainChangesSkin(this.prize)) {
            return 0.0f;
        }
        float turn = this.chainTurn(tier);
        if (turn < 0.0f) {
            return 0.0f;
        }
        if (turn >= TURN_AT[3]) {
            return 1.0f;
        }
        float from = this.prize == Prize.AQUAMARINE ? 460.0f : 100.0f;
        float to = this.prize == Prize.AQUAMARINE ? 620.0f : 260.0f;
        float k = (AnimationDirector.turnDegrees(turn, AnimationDirector.chainTurns(this.prize)) - from) / (to - from);
        return k < 0.0f ? 0.0f : (k > 1.0f ? 1.0f : k);
    }

    public boolean chainChanged(int tier) {
        return this.chainMorph(tier) >= 0.5f;
    }

    public float chainChangeDegrees() {
        return this.prize == Prize.AQUAMARINE ? 540.0f : 180.0f;
    }

    public static float turnDegrees(float turn) {
        return AnimationDirector.turnDegrees(turn, 1);
    }

    public static float turnDegrees(float turn, int turns) {
        if (turn <= 0.0f) {
            return TURN_DEGREES[0];
        }
        float times = Math.max(1, turns);
        for (int i = 1; i < TURN_AT.length; ++i) {
            if (!(turn <= TURN_AT[i])) continue;
            float span = TURN_AT[i] - TURN_AT[i - 1];
            float t = (turn - TURN_AT[i - 1]) / span;
            float t2 = t * t;
            float t3 = t2 * t;
            return (2.0f * t3 - 3.0f * t2 + 1.0f) * AnimationDirector.keyDegrees(i - 1, times) + (t3 - 2.0f * t2 + t) * span * TURN_SPEED[i - 1] * times + (-2.0f * t3 + 3.0f * t2) * AnimationDirector.keyDegrees(i, times) + (t3 - t2) * span * TURN_SPEED[i] * times;
        }
        return TURN_DEGREES[TURN_DEGREES.length - 1] * times;
    }

    private static float keyDegrees(int key, float times) {
        float whole = 360.0f * times;
        return switch (key) {
            case 0, 1 -> TURN_DEGREES[key];
            case 2 -> TURN_DEGREES[key] * times;
            default -> whole + (TURN_DEGREES[key] - 360.0f);
        };
    }

    public static float turnSpeed(float turn) {
        if (turn <= 0.0f || turn >= 1.0f) {
            return 0.0f;
        }
        float a = Math.max(0.0f, turn - 0.004f);
        float b = Math.min(1.0f, turn + 0.004f);
        float perTurn = (AnimationDirector.turnDegrees(b) - AnimationDirector.turnDegrees(a)) / (b - a);
        return Math.min(1.0f, Math.abs(perTurn) / TURN_SPEED[2]);
    }

    public float crownTime() {
        return this.crownTime;
    }

    public boolean stalled() {
        return this.owningScreen() && this.lastUpdateNanos != 0L && System.nanoTime() - this.lastUpdateNanos > 1500000000L;
    }

    public void abort() {
        if (this.second != null) {
            this.second.abort();
            this.second = null;
        }
        this.pendingSecondIn = -1.0f;
        if (this.scareShown) {
            this.audio.onScareSkipped();
            this.scareShown = false;
        }
        this.aftermathUntilMs = 0L;
        if (this.phase != Phase.IDLE && this.phase != Phase.RELEASING) {
            this.record(this.masksLanded(), false);
        }
        this.phase = Phase.IDLE;
        this.phaseTime = 0.0f;
        this.takeover = 0.0f;
        this.split = 0.0f;
        this.handedOver = false;
        this.heldStamps[0] = Prize.NONE;
        this.heldStamps[1] = Prize.NONE;
        this.lastUpdateNanos = 0L;
        this.disarm();
        if (!this.isSecond) {
            this.audio.setEnsemble(1.0f);
            this.audio.onAnimationEnd(false);
        }
    }

    private static float approach(float current, float target, float step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    public Phase phase() {
        return this.phase;
    }

    public float phaseTime() {
        return this.phaseTime;
    }

    public Reel[] reels() {
        return this.reels;
    }

    public int activeIndex() {
        return this.active;
    }

    public Reel activeReel() {
        return this.reels[this.active];
    }

    public Prize prize() {
        return this.prize;
    }

    public Prize extra() {
        return this.extra;
    }

    public int extraTier() {
        return this.extra.dropped() ? this.extra.crownTier() : -1;
    }

    public boolean crowned() {
        return this.reels[this.prize.crownTier()].showsMask();
    }

    public boolean owningScreen() {
        return this.phase != Phase.IDLE || this.takeover > 0.001f || this.armed() || this.second != null && this.second.owningScreen();
    }

    @Override
    public boolean running() {
        if (this.phase != Phase.IDLE && this.phase != Phase.RELEASING) {
            return true;
        }
        return this.second != null && this.second.running();
    }

    public float takeover() {
        return this.takeover;
    }

    public boolean scare() {
        return this.scare;
    }

    public boolean scareShown() {
        return this.scareShown && (this.phase == Phase.SCARE || this.phase == Phase.RELEASING);
    }

    public float scareTime() {
        if (this.phase == Phase.SCARE) {
            return this.phaseTime;
        }
        return this.phase == Phase.RELEASING && this.scareShown ? 10.8f + this.phaseTime : 0.0f;
    }

    public float aftermathSeconds() {
        if (!this.inAftermath()) {
            return -1.0f;
        }
        return 6.0f - (float)(this.aftermathUntilMs - System.currentTimeMillis()) / 1000.0f;
    }

    public boolean inAftermath() {
        return this.aftermathUntilMs != 0L && System.currentTimeMillis() < this.aftermathUntilMs;
    }

    public float aftermathProgress() {
        if (!this.inAftermath()) {
            return 1.0f;
        }
        return 1.0f - (float)(this.aftermathUntilMs - System.currentTimeMillis()) / 6000.0f;
    }

    public int masksLanded() {
        int n = 0;
        for (Reel reel : this.reels) {
            if (!reel.showsMask()) continue;
            ++n;
        }
        return n;
    }

    public int lastMasksLanded() {
        return this.lastMasksLanded;
    }

    public boolean testRun() {
        return this.forcedTest;
    }

    public float anticipation() {
        if (this.phase != Phase.SPINNING) {
            return 0.0f;
        }
        float remaining = 180.0f - this.reels[this.active].angle();
        if (remaining <= 90.0f) {
            return 1.0f;
        }
        float span = 45.0f;
        float t = (135.0f - remaining) / Math.max(1.0f, span);
        return t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
    }

    public float foliageAgitation() {
        if (this.phase == Phase.IDLE || this.phase == Phase.INTRO) {
            return 0.0f;
        }
        float perMask = (float)this.active / (float)(this.reels.length - 1);
        return Math.min(1.35f, perMask + this.anticipation() * 0.45f);
    }

    public float foliageHurry() {
        return this.phase == Phase.SPINNING && this.active >= this.reels.length - 1 ? 1.0f : 0.0f;
    }

    public float climb() {
        if (this.phase == Phase.IDLE || this.phase == Phase.INTRO) {
            return 0.0f;
        }
        float reelProgress = this.phase == Phase.SPINNING ? this.reels[this.active].progress() : 1.0f;
        return Math.min(1.0f, ((float)this.active + reelProgress) / 4.0f);
    }

    public static enum Phase {
        IDLE,
        INTRO,
        SPINNING,
        REVEAL,
        FAIL_OUTRO,
        WIN_OUTRO,
        SCARE,
        RELEASING;

    }
}

