package com.skymaxx.tiki.boa;

import com.skymaxx.tiki.audio.BoaAudio;
import com.skymaxx.tiki.boa.BoaPrize;
import com.skymaxx.tiki.boa.BoaRun;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.config.TikiStats;
import com.skymaxx.tiki.detect.SeaCreatureDetector;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class BoaDirector
implements SeaCreatureDetector.Reveal<BoaPrize> {
    private static final long ARM_WINDOW_MS = 2500L;
    public static final float CHAMBER_QUICKEN = 1.35f;
    private static final float LOAD_INTRO = 0.3f;
    private static final float LOAD_FLY = 0.13f;
    private static final float[] LOAD_BEATS = new float[]{0.26f, 0.24f, 0.22f, 0.21f, 0.19f};
    private static final float LOAD_BEAT = 0.24f;
    private static final float[] BONUS_BEATS = new float[]{0.34f, 0.38f, 0.44f};
    public static final float LOAD_FULL = 0.95f;
    public static final float LOAD_END = 0.22f;
    public static final float SPIN_TIME = 0.8f;
    public static final float FADE_TIME = 0.38f;
    public static final float RISE_TIME = 0.7f;
    public static final float HOLD_TIME = 0.22f;
    public static final float ZOOM_TIME = 0.62f;
    public static final float IRIS_TIME = 0.4f;
    public static final float SEARCH_TIME = 0.75f;
    private static final float[] AIM = new float[]{0.62f, 0.72f, 0.92f, 1.35f, 1.55f};
    private static final float[] FLIGHT = new float[]{0.16f, 0.16f, 0.18f, 0.3f, 0.85f};
    private static final float LAST_AIM = 0.85f;
    private static final float LAST_FLIGHT = 0.72f;
    private static final float[] HIT_STOP = new float[]{0.06f, 0.07f, 0.09f, 0.14f, 0.22f};
    private static final float EVAPORATE = 0.52f;
    private static final float SPLASH = 0.52f;
    public static final float OPEN_TIME = 1.15f;
    public static final float GLINT_AT = 0.48f;
    public static final float POP_AT = 0.74f;
    public static final float EXPLODE_TIME = 1.0f;
    public static final float BURST_AT = 0.16f;
    public static final float FAIL_TIME = 2.8f;
    public static final float DIVE_AT = 0.45f;
    public static final float ESCAPE_TIME = 2.35f;
    public static final float ESCAPE_DIVE_AT = 0.2f;
    public static final float SPENT_BEAT = 0.3f;
    public static final float EMPTY_TO_HEAD = 0.34f;
    public static final float TOSS_TIME = 0.42f;
    public static final float SPLIT_TIME = 0.55f;
    public static final float RECENTRE_TIME = 0.85f;
    private static final float PAIR_LEAVE_RATE = 4.5f;
    private static final float PAIR_FAIL_TIME = 1.35f;
    private static final float DUO_TRIM = 0.62f;
    public static final float PAIR_SEEN = 1.1f;
    private final TikiConfig cfg;
    private final TikiStats stats;
    private final BoaAudio audio;
    private final Hooks hooks;
    private Random rng = new Random();
    private final EnumSet<BoaPrize> drops = EnumSet.noneOf(BoaPrize.class);
    private BoaPrize extra = BoaPrize.NONE;
    private boolean extraOut;
    private BoaDirector second;
    private BoaDirector first;
    private boolean isSecond;
    private float split;
    private boolean hurried;
    private boolean forced;
    private boolean untrimmed;
    private boolean secondIsTest;
    private BoaPrize pendingSecond = BoaPrize.NONE;
    private float pendingSecondIn = -1.0f;
    private float holdTime;
    private float holdLeft = -1.0f;
    private final List<BoaPrize> heldStamps = new ArrayList<BoaPrize>();
    private final List<Integer> heldNumbers = new ArrayList<Integer>();
    private Phase phase = Phase.IDLE;
    private Phase releasedFrom = Phase.IDLE;
    private float phaseTime;
    private float phaseLength;
    private float clock;
    private float sceneClock;
    private float hitStop;
    private final BoaRun run = new BoaRun();
    private BoaPrize confirmed = BoaPrize.NONE;
    private BoaPrize shown = BoaPrize.NONE;
    private boolean forcedTest;
    private int forcedBullets;
    private boolean recorded;
    private float takeover;
    private long lastUpdateNanos;
    private long armedUntilMs;
    private float releasedTime;
    private LoadStep loadStep = LoadStep.INTRO;
    private float loadTime;
    private float loadLength;
    private int loaded;
    private boolean plannedHit;
    private float plannedSide;
    private boolean plannedNear;
    private int plannedSeed;
    private boolean lastShot;
    private int headAttempts;
    private float aimLength;
    private float flightLength;
    private boolean glinted;
    private boolean popped;
    private boolean emptied;
    private float emptiedAt;
    private final List<Event> events = new ArrayList<Event>();
    private final List<float[]> stops = new ArrayList<float[]>();
    private int seeds;
    private int runSeed;
    private BoaPrize stamped = BoaPrize.NONE;

    public BoaDirector(TikiConfig cfg, TikiStats stats, BoaAudio audio, Hooks hooks) {
        this.cfg = cfg;
        this.stats = stats;
        this.audio = audio;
        this.hooks = hooks;
    }

    public void seed(long seed) {
        this.rng = new Random(seed);
    }

    @Override
    public void arm() {
        if (this.phase != Phase.IDLE) {
            return;
        }
        boolean wasArmed = this.armed();
        this.armedUntilMs = System.currentTimeMillis() + 2500L;
        if (!wasArmed) {
            this.audio.setMuted(true);
        }
    }

    public boolean armed() {
        return this.armedUntilMs != 0L && System.currentTimeMillis() < this.armedUntilMs;
    }

    @Override
    public boolean idle() {
        return this.phase == Phase.IDLE && !this.armed();
    }

    @Override
    public void begin() {
        this.begin(BoaPrize.NONE);
    }

    public void begin(BoaPrize known) {
        if (!(this.phase != Phase.RELEASING || this.isSecond || this.second != null && this.second.owningScreen())) {
            this.phase = Phase.IDLE;
        }
        if (this.phase != Phase.IDLE) {
            this.openSecond(known);
            return;
        }
        this.resetRun(known);
        if (this.second == null || !this.second.owningScreen()) {
            this.split = 0.0f;
            this.audio.setLead(true);
            this.audio.setEnsemble(1.0f);
        }
        this.enter(Phase.LOAD, 0.0f);
        float intro = this.secChamber(0.3f);
        this.loadStep(LoadStep.INTRO, intro);
        this.audio.setMuted(true);
        this.audio.onRunStart();
        this.audio.onCylinderIn(intro);
    }

    private void resetRun(BoaPrize known) {
        this.run.reset();
        this.run.know(known);
        this.confirmed = this.run.prize();
        this.drops.clear();
        if (this.confirmed.dropped()) {
            this.drops.add(this.confirmed);
        }
        this.extra = BoaPrize.NONE;
        this.extraOut = false;
        this.hurried = false;
        this.forced = false;
        this.stamped = BoaPrize.NONE;
        this.shown = BoaPrize.NONE;
        this.forcedTest = false;
        this.forcedBullets = 0;
        this.recorded = false;
        this.events.clear();
        this.stops.clear();
        this.seeds = 0;
        this.runSeed = this.rng.nextInt();
        this.clock = 0.0f;
        this.sceneClock = 0.0f;
        this.hitStop = 0.0f;
        this.headAttempts = 0;
        this.loaded = 0;
        this.glinted = false;
        this.popped = false;
        this.emptied = false;
        this.armedUntilMs = 0L;
        this.releasedTime = 0.0f;
        this.plannedSeed = 0;
        this.holdTime = 0.0f;
        this.holdLeft = -1.0f;
        if (!this.isSecond) {
            this.pendingSecondIn = -1.0f;
        }
    }

    private boolean loading() {
        return this.phase == Phase.LOAD || this.phase == Phase.SPIN || this.phase == Phase.RAISE || this.phase == Phase.JOIN;
    }

    private void openSecond(BoaPrize known) {
        if (this.isSecond || this.phase == Phase.RELEASING || this.second != null && this.second.owningScreen()) {
            return;
        }
        if (this.second == null) {
            this.second = new BoaDirector(this.cfg, this.stats, this.audio.twin(), this.hooks);
            this.second.isSecond = true;
            this.second.first = this;
        }
        this.second.rng = new Random(this.rng.nextLong());
        this.second.audio.setLead(false);
        this.untrimmed = false;
        boolean test = this.secondIsTest;
        this.secondIsTest = false;
        this.second.beginJoined(known, test);
    }

    private void beginJoined(BoaPrize known, boolean test) {
        this.resetRun(known);
        this.forcedTest = test;
        this.split = 1.0f;
        this.enter(Phase.JOIN, 0.0f);
        this.audio.setMuted(true);
        this.audio.onRunStart();
        if (this.first == null || !this.first.playing() || this.first.scopeOpen()) {
            this.joinScope();
        }
    }

    private boolean scopeOpen() {
        return switch (this.phase.ordinal()) {
            case 0, 1, 2, 3, 14, 15 -> false;
            case 4 -> {
                if (this.phaseTime >= this.secLoad(0.4f)) {
                    yield true;
                }
                yield false;
            }
            default -> true;
        };
    }

    private void joinScope() {
        this.lockChamber();
        this.loaded = this.run.rolled();
        this.enter(Phase.SCOPE, this.secLoad(1.15f));
        this.audio.onScopeIn();
        if (this.first != null && this.first.playing()) {
            this.first.untrimmed = false;
            this.audio.setEnsemble(0.62f);
        }
    }

    private boolean joined() {
        return this.phase != Phase.JOIN;
    }

    private BoaDirector other() {
        BoaDirector twin = this.isSecond ? this.first : this.second;
        return twin != null && twin.owningScreen() ? twin : null;
    }

    private boolean playing() {
        return this.phase != Phase.IDLE && this.phase != Phase.RELEASING;
    }

    private boolean twinPlaying() {
        BoaDirector twin = this.other();
        return twin != null && twin.playing();
    }

    private boolean failing() {
        return this.phase == Phase.FAIL || this.phase == Phase.ESCAPE;
    }

    private BoaDirector owner() {
        return this.isSecond && this.first != null ? this.first : this;
    }

    private void handOverLead() {
        BoaDirector twin = this.other();
        if (twin == null || !twin.playing() || !this.audio.lead()) {
            return;
        }
        this.audio.setLead(false);
        twin.audio.setLead(true);
    }

    private void printStamp(BoaPrize printed, int number) {
        BoaDirector twin;
        if (this.hooks == null) {
            return;
        }
        BoaDirector boaDirector = twin = this.isSecond ? this.first : this.second;
        if (!this.forced && twin != null && twin.owningScreen() && twin.playing()) {
            this.owner().heldStamps.add(printed);
            this.owner().heldNumbers.add(number);
            return;
        }
        this.owner().releaseHeldStamps();
        this.hooks.stamp(printed, number);
    }

    private void releaseHeldStamps() {
        if (this.heldStamps.isEmpty() || this.hooks == null) {
            return;
        }
        for (int i = 0; i < this.heldStamps.size(); ++i) {
            this.hooks.stamp(this.heldStamps.get(i), this.heldNumbers.get(i));
        }
        this.heldStamps.clear();
        this.heldNumbers.clear();
    }

    @Override
    public boolean acceptsSecond() {
        return !this.isSecond && this.playing() && (this.second == null || !this.second.owningScreen());
    }

    private boolean giveToSecond(BoaPrize dropped) {
        if (this.second == null || !this.second.owningScreen() || !this.second.playing()) {
            return false;
        }
        if (!this.confirmed.dropped() && this.canStillShow(dropped)) {
            return false;
        }
        if (!this.second.confirmed.dropped()) {
            this.second.notifyDrop(dropped);
            return true;
        }
        if (!this.confirmed.dropped()) {
            return false;
        }
        if (this.second.canStillShow(dropped)) {
            this.second.notifyDrop(dropped);
            return true;
        }
        return false;
    }

    private boolean canStillShow(BoaPrize candidate) {
        if (!this.playing() || this.recorded) {
            return false;
        }
        return !this.run.locked() || this.revisable(candidate) && this.run.canShow(candidate, this.fourthUnseen());
    }

    public BoaDirector second() {
        return this.second != null && this.second.owningScreen() ? this.second : null;
    }

    public boolean isSecond() {
        return this.isSecond;
    }

    public float split() {
        return this.split;
    }

    public int backdropSeed() {
        return this.isSecond && this.first != null ? this.first.runSeed : this.runSeed;
    }

    @Override
    public void notifyDrop(BoaPrize dropped) {
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
                this.confirmed = BoaPrize.best(this.confirmed, dropped);
                this.lateDrop(dropped);
            }
            return;
        }
        if (!this.drops.add(dropped)) {
            return;
        }
        this.confirmed = BoaPrize.best(this.confirmed, dropped);
        if (!this.run.locked()) {
            this.run.know(dropped);
            this.considerExtra();
            return;
        }
        boolean unopened = this.fourthUnseen();
        BoaPrize before = this.run.prize();
        if (dropped.rank() <= before.rank()) {
            this.considerExtra();
            return;
        }
        if (!this.revisable(dropped) || !this.run.canShow(dropped, unopened)) {
            return;
        }
        int added = this.run.raise(dropped, unopened);
        if (before.dropped() && !before.inHead() && dropped.inHead()) {
            this.extra = before;
            if (this.shown == before) {
                this.shown = BoaPrize.NONE;
                this.extraOut = true;
            }
        }
        this.considerExtra();
        if (added > 0) {
            this.events.add(new Event(Kind.RELOAD, this.clock, added, this.nextSeed(), 0.0f, false));
            this.audio.onReload(added);
        }
        if (!this.run.over()) {
            this.events.replaceAll(e -> e.kind() == Kind.EMPTY && e.near() ? new Event(Kind.EMPTY, e.time(), e.segment(), e.seed(), e.side(), false) : e);
        }
        if (this.phase == Phase.AIM) {
            this.plan();
        } else if (this.phase == Phase.ESCAPE) {
            this.events.removeIf(e -> e.kind() == Kind.DIVE);
            this.startAim();
        }
    }

    @Override
    public void lateDrop(BoaPrize dropped) {
        if (dropped == null || !dropped.dropped() || this.forcedTest) {
            return;
        }
        this.stats.count(dropped);
        this.stats.boaDryStreak = 0;
        this.stats.save();
        if (dropped.stamps()) {
            this.printStamp(dropped, this.stats.stampCount(dropped));
        }
    }

    private void considerExtra() {
        if (this.extra.dropped() || !this.run.prize().inHead() || this.isSecond || this.second() != null) {
            return;
        }
        if (this.run.locked() && this.run.hits() >= 4 && !this.fourthUnseen()) {
            return;
        }
        BoaPrize best = BoaPrize.NONE;
        for (BoaPrize drop : this.drops) {
            if (!drop.dropped() || drop.inHead() || drop.rank() <= best.rank()) continue;
            best = drop;
        }
        this.extra = best;
    }

    private boolean revisable() {
        return this.revisable(BoaPrize.NONE);
    }

    private boolean revisable(BoaPrize raisingTo) {
        return switch (this.phase.ordinal()) {
            case 1, 2, 3, 4, 5, 6, 7, 14 -> true;
            case 8 -> {
                if (this.fourthUnseen() || this.run.prize() == BoaPrize.NONE || this.run.prize().inHead() || raisingTo != null && raisingTo.inHead()) {
                    yield true;
                }
                yield false;
            }
            case 13 -> {
                if (this.phaseTime < this.secEnd(0.2f)) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private boolean fourthUnseen() {
        return (this.phase == Phase.IMPACT || this.phase == Phase.OPEN) && this.run.hits() == 4 && !this.glinted && !this.emptied;
    }

    public void beginTest(BoaPrize forced) {
        this.beginTest(forced, 0);
    }

    public void beginTest(BoaPrize forced, int bullets) {
        this.dropPair();
        this.phase = Phase.IDLE;
        this.split = 0.0f;
        this.begin(forced == null ? BoaPrize.NONE : forced);
        this.forcedTest = true;
        this.forcedBullets = bullets;
    }

    public void beginPreview(List<BoaPrize> rolled) {
        this.beginTest(BoaPrize.NONE, 0);
        for (BoaPrize drop : rolled) {
            this.notifyDrop(drop);
        }
    }

    public void beginTestPair(BoaPrize firstPrize, BoaPrize secondPrize) {
        this.beginTestPair(firstPrize, secondPrize, 0.0f);
    }

    public void beginTestPair(BoaPrize firstPrize, BoaPrize secondPrize, float delaySeconds) {
        this.beginTest(firstPrize, 0);
        this.pendingSecond = secondPrize == null ? BoaPrize.NONE : secondPrize;
        this.pendingSecondIn = Math.max(0.0f, delaySeconds);
        if (this.pendingSecondIn <= 0.0f) {
            this.openPendingSecond();
        }
    }

    public void beginTestDrops(BoaPrize a, BoaPrize b, int bullets) {
        BoaPrize best = BoaPrize.best(a, b);
        BoaPrize other = best == a ? b : a;
        this.beginTest(best, bullets);
        this.notifyDrop(other);
    }

    private void openPendingSecond() {
        boolean free;
        BoaPrize next = this.pendingSecond;
        if (this.acceptsSecond()) {
            this.pendingSecondIn = -1.0f;
            this.pendingSecond = BoaPrize.NONE;
            this.secondIsTest = true;
            this.openSecond(next);
            return;
        }
        boolean bl = free = !this.owningScreen() || this.phase == Phase.RELEASING && (this.second == null || !this.second.owningScreen());
        if (free) {
            this.pendingSecondIn = -1.0f;
            this.pendingSecond = BoaPrize.NONE;
            this.beginTest(next, 0);
        }
    }

    private void dropPair() {
        if (this.second != null) {
            this.second.abort();
            this.second = null;
        }
        this.heldStamps.clear();
        this.heldNumbers.clear();
        this.audio.setEnsemble(1.0f);
        this.audio.setLead(true);
    }

    public void skipStep() {
        if (this.second != null) {
            this.second.skipStep();
        }
        if (this.loading()) {
            this.skipToShooting();
        } else {
            this.skipOnly();
        }
    }

    public void skipToShooting() {
        if (this.phase == Phase.JOIN) {
            this.joinScope();
            return;
        }
        if (this.phase != Phase.LOAD && this.phase != Phase.SPIN && this.phase != Phase.RAISE) {
            return;
        }
        this.lockChamber();
        this.loaded = this.run.rolled();
        this.audio.onSkipToScope();
        this.enter(Phase.SCOPE, this.secLoad(1.15f));
        this.audio.onScopeIn();
    }

    public void skip() {
        if (this.second != null) {
            this.second.skip();
        }
        this.skipOnly();
    }

    private void skipOnly() {
        if (this.phase == Phase.IDLE || this.phase == Phase.RELEASING) {
            return;
        }
        this.forced = true;
        if (this.phase == Phase.REVEAL && this.shown == BoaPrize.AQUAMARINE) {
            this.audio.onJackpotSkipped();
        }
        if (!this.run.locked()) {
            this.lockChamber();
        }
        this.run.finish(this.rng);
        this.finish();
    }

    public void update(float dt) {
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
                this.audio.setLead(true);
            }
        }
        if (this.isSecond) {
            return;
        }
        BoaDirector pair = this.second();
        if (pair != null) {
            boolean theirs;
            boolean mine = this.playing();
            boolean bl = theirs = pair.playing() && pair.joined();
            if (mine && theirs) {
                this.split = BoaDirector.approach(this.split, 1.0f, dt / 0.55f);
            } else if (mine && !this.failing()) {
                this.split = BoaDirector.approach(this.split, 0.0f, dt / 0.85f);
            } else if (theirs && !pair.failing()) {
                pair.split = BoaDirector.approach(pair.split, 0.0f, dt / 0.85f);
            }
            if (mine != theirs && !this.untrimmed) {
                float f = mine ? pair.takeover : this.takeover;
                if (f <= 0.02f) {
                    this.untrimmed = true;
                    this.audio.setEnsemble(1.0f);
                }
            }
            if (!mine && !theirs) {
                this.releaseHeldStamps();
            }
        } else {
            this.releaseHeldStamps();
            if (this.phase == Phase.IDLE && this.takeover <= 0.001f) {
                this.split = 0.0f;
            } else if (this.playing() && !this.failing()) {
                this.split = BoaDirector.approach(this.split, 0.0f, dt / 0.85f);
            }
        }
    }

    private void step(float dt) {
        float target;
        if (this.phase == Phase.IDLE) {
            this.takeover = BoaDirector.approach(this.takeover, 0.0f, dt * 4.0f);
            if (this.armedUntilMs != 0L && !this.armed()) {
                this.armedUntilMs = 0L;
            }
            return;
        }
        this.clock += dt;
        if (this.hitStop > 0.0f) {
            this.hitStop -= dt;
        } else {
            this.sceneClock += dt;
        }
        this.phaseTime += dt;
        float f = target = this.phase == Phase.RELEASING || this.phase == Phase.JOIN ? 0.0f : 1.0f;
        float rate = this.phase == Phase.RELEASING ? (this.hurried || this.twinPlaying() ? 4.5f : 2.2f) : 3.0f;
        this.takeover = BoaDirector.approach(this.takeover, target, dt * rate);
        this.audio.update(dt, this.run.hits(), this.phase == Phase.AIM, this.aimProgress(), this.holdingBreath());
        switch (this.phase.ordinal()) {
            case 1: {
                this.updateLoad(dt);
                break;
            }
            case 2: {
                if (!(this.phaseTime >= this.phaseLength)) break;
                this.enter(Phase.RAISE, this.secLoad(1.54f));
                this.audio.onRifleRaise();
                break;
            }
            case 3: {
                if (!(this.phaseTime >= this.phaseLength)) break;
                this.enter(Phase.SCOPE, this.secLoad(1.15f));
                this.audio.onScopeIn();
                break;
            }
            case 4: {
                if (!(this.phaseTime >= this.phaseLength)) break;
                this.startAim();
                break;
            }
            case 5: {
                if (!(this.phaseTime >= this.phaseLength)) break;
                this.fire();
                break;
            }
            case 6: {
                if (!(this.phaseTime >= this.phaseLength)) break;
                this.land();
                break;
            }
            case 7: {
                if (!(this.phaseTime >= this.phaseLength)) break;
                this.afterImpact();
                break;
            }
            case 8: {
                this.updateOpen();
                break;
            }
            case 9: {
                if (!(this.phaseTime >= this.phaseLength)) break;
                this.reveal();
                break;
            }
            case 14: {
                if (this.first != null && this.first.playing() && !this.first.scopeOpen()) break;
                this.joinScope();
                break;
            }
            case 10: {
                this.updateHold(dt);
                break;
            }
            case 11: 
            case 12: 
            case 13: {
                if (!(this.phaseTime >= this.phaseLength)) break;
                this.finish();
                break;
            }
            case 15: {
                boolean waiting;
                boolean bl = waiting = !this.isSecond && this.second != null && this.second.owningScreen();
                if (!(this.takeover <= 0.02f) || !(this.phaseTime >= 0.3f) || waiting) break;
                this.phase = Phase.IDLE;
                this.phaseTime = 0.0f;
                break;
            }
        }
    }

    private void updateLoad(float dt) {
        this.loadTime += dt;
        if (this.loadTime < this.loadLength) {
            return;
        }
        switch (this.loadStep.ordinal()) {
            case 0: {
                this.flyNext();
                break;
            }
            case 1: {
                ++this.loaded;
                boolean bonus = this.loaded > 5;
                this.audio.onBulletLoad(this.loaded - 1, bonus);
                float beat = bonus ? BONUS_BEATS[Math.min(BONUS_BEATS.length - 1, this.loaded - 5 - 1)] : LOAD_BEATS[Math.min(LOAD_BEATS.length - 1, this.loaded - 1)];
                this.loadStep(LoadStep.SEAT, this.secChamber(beat - 0.13f));
                break;
            }
            case 2: {
                if (this.loaded < 5) {
                    this.flyNext();
                    break;
                }
                if (!this.run.locked()) {
                    this.loadStep(LoadStep.BEAT, this.secChamber(0.24f));
                    break;
                }
                this.afterRound();
                break;
            }
            case 3: {
                this.lockChamber();
                this.afterRound();
                break;
            }
            case 4: {
                this.loadStep(LoadStep.END, this.secChamber(0.22f));
                break;
            }
            case 5: {
                this.enter(Phase.SPIN, this.secChamber(1.1800001f));
                this.audio.onCylinderSpin(this.secChamber(0.8f));
            }
        }
    }

    private void afterRound() {
        if (this.loaded < this.loadTarget()) {
            this.flyNext();
        } else if (this.loaded >= 8) {
            this.audio.onChamberFull();
            this.loadStep(LoadStep.FULL, this.secChamber(0.95f));
        } else {
            this.loadStep(LoadStep.END, this.secChamber(0.22f));
        }
    }

    private int loadTarget() {
        return Math.max(5, Math.min(8, this.run.bullets()));
    }

    private void flyNext() {
        this.loadStep(LoadStep.FLY, this.secChamber(0.13f));
    }

    private void loadStep(LoadStep step, float length) {
        this.loadStep = step;
        this.loadTime = 0.0f;
        this.loadLength = length;
    }

    private void lockChamber() {
        if (this.run.locked()) {
            return;
        }
        if (this.forcedBullets >= 5) {
            this.run.lock(this.forcedBullets);
        } else {
            this.run.lock(this.rng);
        }
    }

    private void startAim() {
        int stage = this.run.hits();
        this.lastShot = this.run.left() == 1;
        float length = AIM[Math.min(AIM.length - 1, stage)];
        if (stage >= 4) {
            length = Math.max(1.35f, length - 0.2f * (float)this.headAttempts);
        }
        if (this.lastShot) {
            length += 0.85f;
        }
        this.aimLength = this.secShot(length);
        this.plan();
        this.enter(Phase.AIM, this.aimLength);
        this.audio.onAim(stage, this.aimLength, this.lastShot, this.run.fired() > 0);
    }

    private void plan() {
        this.plannedHit = this.run.decide(this.rng);
        this.plannedSide = (this.rng.nextBoolean() ? 1.0f : -1.0f) * (0.55f + this.rng.nextFloat() * 0.45f);
        this.plannedNear = !this.plannedHit && this.run.hits() >= 3 && this.rng.nextFloat() < 0.6f;
        this.plannedSeed = this.nextSeed();
    }

    private void fire() {
        int stage = this.run.hits();
        if (stage >= 4) {
            ++this.headAttempts;
        }
        boolean last = this.lastShot();
        float flight = FLIGHT[Math.min(FLIGHT.length - 1, stage)];
        this.flightLength = this.secShot(last ? Math.max(flight, 0.72f) : flight);
        this.events.add(new Event(Kind.SHOT, this.clock, stage, this.nextSeed(), 0.0f, last));
        this.enter(Phase.FIRE, this.flightLength);
        this.audio.onFire(stage, this.slowMotion(), last);
    }

    private void land() {
        int stage = this.run.hits();
        boolean hit = this.run.fire(this.rng);
        if (hit != this.plannedHit) {
            this.plannedHit = hit;
        }
        if (hit) {
            float stop;
            if (stage >= 4) {
                this.events.add(new Event(Kind.EXPLODE, this.clock, stage, this.nextSeed(), 0.0f, false));
                this.hitStop = this.secShot(HIT_STOP[stage]);
                this.stops.add(new float[]{this.clock, this.hitStop});
                this.enter(Phase.EXPLODE, this.secEnd(1.0f));
                this.audio.onExplode(this.run.prize().celebration(), this.secEnd(0.16f));
                return;
            }
            this.events.add(new Event(Kind.HIT, this.clock, stage, this.nextSeed(), 0.0f, false));
            this.hitStop = stop = this.secShot(HIT_STOP[stage]);
            this.stops.add(new float[]{this.clock, stop});
            float evaporate = this.secShot(0.52f + (stage >= 3 ? 0.15f : 0.0f));
            this.events.add(new Event(Kind.EVAPORATE, this.clock + stop, stage, this.nextSeed(), 0.0f, false));
            this.enter(Phase.IMPACT, stop + evaporate);
            this.audio.onHit(stage);
            if (stage == 3 && !this.fourthHolds()) {
                this.emptied = true;
                boolean spentOut = this.run.over();
                this.events.add(new Event(Kind.EMPTY, this.clock + stop, 3, this.nextSeed(), 0.0f, spentOut));
                this.audio.onEmpty(!spentOut, stop);
            }
        } else {
            this.events.add(new Event(Kind.MISS, this.clock, stage, this.plannedSeed, this.plannedSide, this.plannedNear));
            this.enter(Phase.IMPACT, this.secShot(0.52f + (this.plannedNear ? 0.18f : 0.0f)));
            this.audio.onMiss(stage, this.plannedNear);
        }
    }

    private void afterImpact() {
        if (this.run.hits() == 4 && this.lastEventWasHit() && !this.emptied) {
            this.enter(Phase.OPEN, this.secEnd(1.15f));
            this.audio.onOpenHold();
            return;
        }
        if (this.run.over()) {
            this.fail();
        } else if (this.hopeless()) {
            this.escape();
        } else {
            this.startAim();
        }
    }

    private boolean hopeless() {
        return this.run.target() <= 0 && this.run.hits() + this.run.left() < 4;
    }

    private boolean fourthHolds() {
        return this.run.prize().dropped() && !this.run.prize().inHead() || this.extra.dropped() && this.run.prize().inHead();
    }

    private boolean lastEventWasHit() {
        for (int i = this.events.size() - 1; i >= 0; --i) {
            Kind kind = this.events.get(i).kind();
            if (kind == Kind.HIT || kind == Kind.EVAPORATE) {
                return true;
            }
            if (kind != Kind.MISS) continue;
            return false;
        }
        return false;
    }

    private void updateOpen() {
        boolean holds;
        boolean common = this.run.prize().dropped() && !this.run.prize().inHead();
        boolean onTheWay = !common && this.extra.dropped() && this.run.prize().inHead();
        boolean bl = holds = common || onTheWay;
        if (this.phaseTime >= this.secEnd(0.48f)) {
            if (holds && !this.glinted) {
                this.glinted = true;
                if (common) {
                    this.shown = this.run.prize();
                    this.handOverLead();
                } else {
                    this.extraOut = true;
                }
                this.events.add(new Event(Kind.GLINT, this.clock, 3, this.nextSeed(), 0.0f, false));
                this.audio.onGlint();
            } else if (!holds && !this.emptied) {
                this.emptied = true;
                this.emptiedAt = this.phaseTime;
                boolean spentOut = this.run.over();
                this.events.add(new Event(Kind.EMPTY, this.clock, 3, this.nextSeed(), 0.0f, spentOut));
                this.audio.onEmpty(!spentOut, 0.0f);
            }
        }
        if (!holds && this.emptied && this.run.over() && this.phaseTime >= this.emptiedAt + this.secEnd(0.3f)) {
            this.fail();
            return;
        }
        if (!holds && this.emptied && !this.run.over() && this.phaseTime >= this.emptiedAt + this.secEnd(0.34f)) {
            this.startAim();
            return;
        }
        if (holds && this.glinted && !this.popped && this.phaseTime >= this.secEnd(0.74f)) {
            this.popped = true;
            this.events.add(new Event(Kind.POP, this.clock, 3, this.nextSeed(), 0.0f, false));
            this.audio.onPop(this.fourthItem().celebration());
        }
        if (this.phaseTime < this.phaseLength) {
            return;
        }
        if (common) {
            this.revealCommon();
        } else if (onTheWay) {
            this.toss();
        } else if (this.run.over()) {
            this.fail();
        } else {
            this.startAim();
        }
    }

    private void toss() {
        this.events.add(new Event(Kind.TOSS, this.clock, 3, this.nextSeed(), 0.0f, false));
        this.audio.onToss(this.secEnd(0.42f));
        if (this.run.over()) {
            this.fail();
        } else {
            this.startAim();
        }
    }

    private void revealCommon() {
        this.settle();
    }

    private void reveal() {
        this.shown = this.run.prize();
        this.settle();
    }

    private void settle() {
        BoaDirector twin = this.other();
        if (this.forced || twin == null || !twin.playing()) {
            this.startReveal();
            return;
        }
        if (twin.phase == Phase.HOLD && twin.holdLeft < 0.0f) {
            if (this.shown.rank() > twin.shown.rank()) {
                twin.leaveHold();
                this.startReveal();
            } else {
                this.enterHold(1.1f);
            }
            return;
        }
        this.enterHold(-1.0f);
    }

    private void enterHold(float seen) {
        this.handOverLead();
        this.holdLeft = seen;
        this.enter(Phase.HOLD, 0.0f);
    }

    private void updateHold(float dt) {
        boolean twinDone;
        this.holdTime += dt * this.endPace();
        BoaDirector twin = this.other();
        boolean bl = twinDone = twin == null || !twin.playing();
        if (this.holdLeft >= 0.0f) {
            this.holdLeft -= dt;
            if (this.holdLeft <= 0.0f || twinDone) {
                this.leaveHold();
            }
        } else if (twinDone) {
            this.startReveal();
        }
    }

    private void leaveHold() {
        this.hurried = true;
        this.holdLeft = -1.0f;
        this.finish();
    }

    private void startReveal() {
        this.holdLeft = -1.0f;
        this.handOverLead();
        if (this.shown.inHead() && this.shown == BoaPrize.AQUAMARINE) {
            this.enter(Phase.REVEAL, this.secEnd(8.6f));
            this.audio.onJackpot();
            return;
        }
        this.enter(Phase.REVEAL, this.secEnd(this.shown.inHead() ? 5.4f : 5.0f));
        this.audio.onReveal(this.shown.celebration());
    }

    private void fail() {
        this.hurried = this.twinPlaying();
        this.handOverLead();
        this.events.add(new Event(Kind.DIVE, this.clock + this.secEnd(0.45f), 0, this.nextSeed(), 0.0f, false));
        this.enter(Phase.FAIL, this.secEnd(this.hurried ? 1.35f : 2.8f));
        this.audio.onFail(this.secEnd(0.45f));
    }

    private void escape() {
        this.hurried = this.twinPlaying();
        this.handOverLead();
        this.events.add(new Event(Kind.DIVE, this.clock + this.secEnd(0.2f), 0, this.nextSeed(), 0.0f, true));
        this.enter(Phase.ESCAPE, this.secEnd(this.hurried ? 1.35f : 2.35f));
        this.audio.onEscape(this.secEnd(0.2f));
    }

    private void finish() {
        if (this.phase == Phase.RELEASING || this.phase == Phase.IDLE) {
            return;
        }
        int target = this.run.target();
        if (target > 0 && this.run.hits() >= target) {
            this.shown = this.run.prize();
        }
        this.record();
        if (this.confirmed.stamps() && this.hooks != null) {
            int number = this.stats.stampCount(this.confirmed);
            if (this.forcedTest) {
                number += 1 + (this.previewStampedBeside(this.confirmed) ? 1 : 0);
            }
            this.stamped = this.confirmed;
            this.printStamp(this.confirmed, number);
        } else {
            BoaDirector twin = this.other();
            if (this.forced || twin == null || !twin.playing()) {
                this.owner().releaseHeldStamps();
            }
        }
        if (this.cfg.chatSummary && this.hooks != null) {
            String line = this.confirmed.announcement();
            StringBuilder also = new StringBuilder();
            for (BoaPrize drop : this.drops) {
                if (drop == this.confirmed) continue;
                also.append(" \u00a77+ ").append(drop.title());
            }
            this.hooks.chat(line != null ? line + String.valueOf(also) : "\u00a78The Titanoboa got away with \u00a77" + this.run.hits() + "\u00a78/5 segments shot.");
        }
        this.handOverLead();
        this.audio.onEnd();
        this.releasedFrom = this.phase;
        this.releasedTime = this.phaseTime;
        this.enter(Phase.RELEASING, 0.0f);
    }

    private void record() {
        if (this.recorded) {
            return;
        }
        this.recorded = true;
        if (!this.forcedTest) {
            this.stats.recordTitanoboa(this.run.rolled(), this.run.hits(), this.confirmed);
            boolean more = false;
            for (BoaPrize drop : this.drops) {
                if (drop == this.confirmed) continue;
                this.stats.count(drop);
                more = true;
            }
            if (more) {
                this.stats.save();
            }
        }
    }

    private boolean previewStampedBeside(BoaPrize printed) {
        BoaDirector twin = this.isSecond ? this.first : this.second;
        return twin != null && twin.forcedTest && twin.stamped == printed;
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
        if (this.phase != Phase.IDLE && this.phase != Phase.RELEASING && this.run.locked()) {
            this.record();
        }
        this.phase = Phase.IDLE;
        this.phaseTime = 0.0f;
        this.takeover = 0.0f;
        this.split = 0.0f;
        this.lastUpdateNanos = 0L;
        this.armedUntilMs = 0L;
        this.heldStamps.clear();
        this.heldNumbers.clear();
        this.audio.onEnd();
        if (!this.isSecond) {
            this.audio.setEnsemble(1.0f);
            this.audio.setLead(true);
        }
    }

    private void enter(Phase next, float length) {
        this.phase = next;
        this.phaseTime = 0.0f;
        this.phaseLength = length;
        if (next == Phase.OPEN) {
            this.glinted = false;
            this.popped = false;
            this.emptied = false;
        }
    }

    private float sec(float seconds) {
        return seconds / this.pace();
    }

    private float secLoad(float seconds) {
        return seconds / this.loadPace();
    }

    private float secChamber(float seconds) {
        return seconds / this.chamberPace();
    }

    private float secShot(float seconds) {
        return seconds / this.shotPace();
    }

    private float secEnd(float seconds) {
        return seconds / this.endPace();
    }

    private int nextSeed() {
        return (int)(2654435769L * (long)(++this.seeds)) ^ 0x5BD1E995 ^ this.runSeed * -2048144789;
    }

    private static float approach(float current, float target, float step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    public Phase phase() {
        return this.phase;
    }

    public float releasedTime() {
        return this.releasedTime;
    }

    public Phase releasedFrom() {
        return this.releasedFrom;
    }

    public float heldFor() {
        return this.holdTime;
    }

    public float phaseTime() {
        return this.phaseTime;
    }

    public float phaseLength() {
        return this.phaseLength;
    }

    public float phaseProgress() {
        return this.phaseLength <= 0.0f ? 1.0f : Math.min(1.0f, this.phaseTime / this.phaseLength);
    }

    public float clock() {
        return this.clock;
    }

    public float sceneClock() {
        return this.sceneClock;
    }

    public float sceneClockAt(float when) {
        float scene = this.sceneClock - (this.clock - when);
        for (float[] stop : this.stops) {
            float from = Math.max(when, stop[0]);
            float to = Math.min(this.clock, stop[0] + stop[1]);
            if (!(to > from)) continue;
            scene += to - from;
        }
        return Math.min(this.sceneClock, scene);
    }

    public boolean hitStopping() {
        return this.hitStop > 0.0f;
    }

    public float pace() {
        return BoaDirector.clampPace(this.cfg.boaPace);
    }

    public float loadPace() {
        return BoaDirector.clampPace(this.cfg.boaPace * this.cfg.boaLoadPace);
    }

    public float chamberPace() {
        return this.loadPace() * 1.35f;
    }

    public float shotPace() {
        return BoaDirector.clampPace(this.cfg.boaPace * this.cfg.boaShotPace);
    }

    public float endPace() {
        return BoaDirector.clampPace(this.cfg.boaPace * this.cfg.boaRevealPace);
    }

    private static float clampPace(float value) {
        return Float.isNaN(value) ? 1.0f : Math.max(0.2f, Math.min(4.0f, value));
    }

    public LoadStep loadStep() {
        return this.loadStep;
    }

    public float loadTime() {
        return this.loadTime;
    }

    public float loadLength() {
        return this.loadLength;
    }

    public int loaded() {
        return this.loaded;
    }

    public int roundsFired() {
        return Math.min(this.run.bullets(), this.run.fired() + (this.phase == Phase.FIRE ? 1 : 0));
    }

    public BoaRun run() {
        return this.run;
    }

    public BoaPrize prize() {
        return this.run.prize();
    }

    public BoaPrize shown() {
        return this.shown;
    }

    public BoaPrize fourthItem() {
        if (this.shown.dropped() && !this.shown.inHead()) {
            return this.shown;
        }
        return this.extraOut ? this.extra : BoaPrize.NONE;
    }

    public BoaPrize extra() {
        return this.extra;
    }

    public BoaPrize confirmed() {
        return this.confirmed;
    }

    public boolean plannedHit() {
        return this.plannedHit;
    }

    public float plannedSide() {
        return this.plannedSide;
    }

    public boolean plannedNear() {
        return this.plannedNear;
    }

    public int plannedSeed() {
        return this.plannedSeed;
    }

    public boolean lastShot() {
        return this.lastShot && this.run.left() <= 1;
    }

    public boolean slowMotion() {
        return this.run.hits() >= 4 || this.lastShot();
    }

    public float aimProgress() {
        return this.phase == Phase.AIM ? this.phaseProgress() : 0.0f;
    }

    public boolean holdingBreath() {
        if (this.phase != Phase.AIM) {
            return false;
        }
        return this.run.hits() >= 4 && this.aimProgress() > 0.35f || this.lastShot() && this.aimProgress() > 0.2f;
    }

    public List<Event> events() {
        return this.events;
    }

    public int runSeed() {
        return this.runSeed;
    }

    public boolean testRun() {
        return this.forcedTest;
    }

    public float takeover() {
        return this.takeover;
    }

    public boolean owningScreen() {
        return this.phase != Phase.IDLE || this.takeover > 0.001f || this.armed() || this.second != null && this.second.owningScreen();
    }

    @Override
    public boolean running() {
        return this.playing() || this.second != null && this.second.running();
    }

    public float anticipation() {
        if (this.phase != Phase.AIM) {
            return 0.0f;
        }
        float weight = 0.25f + 0.75f * (float)this.run.hits() / 4.0f;
        if (this.lastShot()) {
            weight = Math.max(weight, 0.85f);
        }
        return weight * this.aimProgress();
    }

    public static enum Phase {
        IDLE,
        LOAD,
        SPIN,
        RAISE,
        SCOPE,
        AIM,
        FIRE,
        IMPACT,
        OPEN,
        EXPLODE,
        HOLD,
        REVEAL,
        FAIL,
        ESCAPE,
        JOIN,
        RELEASING;

    }

    public static enum LoadStep {
        INTRO,
        FLY,
        SEAT,
        BEAT,
        FULL,
        END;

    }

    public static interface Hooks {
        public void chat(String var1);

        public void stamp(BoaPrize var1, int var2);
    }

    public record Event(Kind kind, float time, int segment, int seed, float side, boolean near) {
    }

    public static enum Kind {
        SHOT,
        HIT,
        MISS,
        EVAPORATE,
        EMPTY,
        GLINT,
        POP,
        EXPLODE,
        RELOAD,
        DIVE,
        TOSS;

    }
}

