package com.skymaxx.tiki.detect;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.detect.DropLines;
import com.skymaxx.tiki.detect.NameplateHealth;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SeaCreatureDetector<P extends DropLines.Named> {
    private static final int MIN_SIGHTING_TICKS = 6;
    private static final long CATCH_AFTER_MS = 4000L;
    private static final long CATCH_BEFORE_MS = 2000L;
    private static final double CATCH_REACH = 12.0;
    private static final long HOOK_MEMORY_MS = 5000L;
    private static final long LINE_WAIT_MS = 1000L;
    private static final long LINE_MATCH_MS = 2000L;
    private static final long LOOT_SHARE_WINDOW_MS = 6000L;
    private static final long SHARING_LATELY_MS = 300000L;
    private static final long NAMED_SHARE_WINDOW_MS = 8000L;
    private static final long DEATH_BEFORE_ITEM_MS = 3000L;
    private static final long ITEM_BEFORE_DEATH_MS = 1500L;
    private static final long SACK_AFTER_DEATH_MS = 40000L;
    private static final long SECOND_KILL_WAIT_MS = 400L;
    private static final int MOST_AT_ONCE = 4;
    private static final int SETTLE_TICKS = 60;
    private static final double CLOSE_ENOUGH = 0.8;
    private static final float DEATH_HEALTH_FRACTION = 0.5f;
    private static final double TELEPORT_DISTANCE_SQ = 64.0;
    private static final int BULK_VANISH = 2;
    private static final double BULK_UNLOAD_FRACTION = 0.5;
    private static final long SAME_KILL_MS = 2000L;
    private static final long ENGAGED_MS = 1500L;
    private static final double PART_OF_SAME_BODY = 24.0;
    private static final int NAME_LOST_GRACE_TICKS = 40;
    private static final int FORGET_NAMELESS_TICKS = 400;
    private final TikiConfig cfg;
    private final Quarry quarry;
    private final Reveal<P> reveal;
    private final DropReader<P> reader;
    private final Supplier<Boolean> screenClaimedElsewhere;
    private final Map<Integer, Sighting> sightings = new HashMap<Integer, Sighting>();
    private long tickCount;
    private int ticksInWorld;
    private int lastEntityCount;
    private Vec3 lastPlayerPos;
    private long lastCreatureNearbyMs;
    private long lastOwnedNearbyMs;
    private long lastCatchMs;
    private Vec3 catchPos;
    private boolean catchResolved = true;
    private Vec3 lastHookPos;
    private long lastHookMs;
    private final ArrayDeque<Gone> recentlyGone = new ArrayDeque();
    private long pendingLineMs;
    private boolean pendingLineNamed;
    private String pendingLineWhy;
    private long lastLootShareMs;
    private long lastAnyShareMs;
    private long lastNamedShareMs;
    private long lastHealthyVanishMs;
    private int deathsAlreadyShared;
    private long lastOwnedDeathMs;
    private final ArrayDeque<Long> unclaimedDeaths = new ArrayDeque();
    private long lastDeathMs;
    private long lastRunStartMs;
    private final List<P> pending = new ArrayList<P>();
    private long pendingSinceMs;
    private boolean pendingForNextKill;
    private boolean ignoredKillDuringRun;
    private long startedFromDropMs;
    private static final int LOG_LINES = 20;
    private final ArrayDeque<String> decisions = new ArrayDeque();

    public SeaCreatureDetector(TikiConfig cfg, Quarry quarry, Reveal<P> reveal, DropReader<P> reader, Supplier<Boolean> screenClaimedElsewhere) {
        this.cfg = cfg;
        this.quarry = quarry;
        this.reveal = reveal;
        this.reader = reader;
        this.screenClaimedElsewhere = screenClaimedElsewhere;
    }

    public void onChat(Component message, boolean overlay) {
        if (overlay || message == null || !this.enabled()) {
            return;
        }
        String line = DropLines.stripFormatting(message.getString()).trim();
        long now = System.currentTimeMillis();
        if (DropLines.isWholeGameLine(line, this.quarry.catchMessage().get())) {
            this.lastCatchMs = now;
            this.catchPos = this.catchSpot(now);
            this.catchResolved = false;
            int marked = 0;
            for (Sighting sighting : this.sightings.values()) {
                if (sighting.owned || now - sighting.firstSeenMs > 2000L || !this.nearCatch(sighting)) continue;
                sighting.owned = true;
                ++marked;
            }
            if (marked > 0) {
                this.catchResolved = true;
            }
            this.decide("\u00a7aYou fished up a " + this.quarry.name().get() + (marked > 0 ? "" : "\u00a78 (waiting for it to surface)"));
            return;
        }
        if (DropLines.charm(line, this.quarry.name().get()) == DropLines.Share.THIS_CREATURE) {
            if (this.ownOnly()) {
                this.ownOnlyLine(now, true, "\u00a7aYou charmed a " + this.quarry.name().get() + " you fished up");
                return;
            }
            this.creditedKill(now, "\u00a7aYou charmed a " + this.quarry.name().get(), false);
            return;
        }
        DropLines.Share share = DropLines.lootShare(line, this.quarry.lootShareMessage().get(), this.quarry.name().get());
        if (share != DropLines.Share.NONE) {
            this.lastAnyShareMs = now;
        }
        if (this.ownOnly() && (share == DropLines.Share.THIS_CREATURE || share == DropLines.Share.UNNAMED)) {
            this.ownOnlyLine(now, share == DropLines.Share.THIS_CREATURE, "\u00a7aLoot share on a " + this.quarry.name().get() + " you fished up");
            return;
        }
        switch (share) {
            case THIS_CREATURE: {
                this.creditedKill(now, "\u00a7aLoot share on a " + this.quarry.name().get(), false);
                break;
            }
            case UNNAMED: {
                this.unnamedShare(now);
                break;
            }
        }
    }

    private void ownOnlyLine(long now, boolean named, String why) {
        if (this.lastOwnedDeathMs != 0L && now - this.lastOwnedDeathMs <= 2000L) {
            return;
        }
        Boolean whose = this.goneOwner(now - 2000L);
        if (whose == null) {
            this.pendingLineMs = now;
            this.pendingLineNamed = named;
            this.pendingLineWhy = why;
            return;
        }
        this.settleLine(now, whose, named, why);
    }

    private void settlePendingLine(long now) {
        if (this.pendingLineMs == 0L) {
            return;
        }
        if (this.lastOwnedDeathMs != 0L && this.lastOwnedDeathMs >= this.pendingLineMs - 2000L) {
            this.pendingLineMs = 0L;
            return;
        }
        Boolean whose = this.goneOwner(this.pendingLineMs - 2000L);
        if (whose == null && now - this.pendingLineMs < 1000L) {
            return;
        }
        this.pendingLineMs = 0L;
        this.settleLine(now, whose, this.pendingLineNamed, this.pendingLineWhy);
    }

    private void settleLine(long now, Boolean whose, boolean named, String why) {
        if (Boolean.TRUE.equals(whose)) {
            this.creditedKill(now, why, true);
            return;
        }
        if (Boolean.FALSE.equals(whose)) {
            this.decide("\u00a78" + (String)(named ? "A line about a " + this.quarry.name().get() : "A loot share") + " you did not fish up; own catches only");
            return;
        }
        if (named && this.onlyYoursNearby()) {
            this.creditedKill(now, why, false);
            return;
        }
        this.decide("\u00a78" + (named ? "A line about a " + this.quarry.name().get() + " out of sight" : "A loot share with no " + this.quarry.name().get() + " of yours dying") + "; own catches only");
    }

    private Boolean goneOwner(long since) {
        Boolean whose = null;
        for (Gone gone : this.recentlyGone) {
            if (gone.ms() < since) continue;
            if (gone.owned()) {
                return Boolean.TRUE;
            }
            whose = Boolean.FALSE;
        }
        return whose;
    }

    private boolean onlyYoursNearby() {
        double radiusSq = this.cfg.killRadius * this.cfg.killRadius;
        boolean mine = false;
        for (Sighting sighting : this.sightings.values()) {
            if (sighting.lastDistanceSq > radiusSq) continue;
            if (!sighting.owned) {
                return false;
            }
            mine = true;
        }
        return mine;
    }

    private Vec3 catchSpot(long now) {
        if (this.lastHookPos != null && now - this.lastHookMs <= 5000L) {
            return this.lastHookPos;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.player == null ? null : mc.player.position();
    }

    private boolean nearCatch(Sighting sighting) {
        if (this.catchPos == null) {
            return true;
        }
        double dx = sighting.x - this.catchPos.x;
        double dz = sighting.z - this.catchPos.z;
        return dx * dx + dz * dz <= 144.0;
    }

    private void resolveCatch(long now) {
        if (this.catchResolved || this.lastCatchMs == 0L || now - this.lastCatchMs <= 4000L) {
            return;
        }
        this.catchResolved = true;
        Sighting nearest = null;
        double best = Double.MAX_VALUE;
        for (Sighting sighting : this.sightings.values()) {
            if (sighting.owned) {
                return;
            }
            if (sighting.firstSeenMs < this.lastCatchMs - 2000L || sighting.firstSeenMs > this.lastCatchMs + 4000L) continue;
            double d = this.catchPos == null ? sighting.lastDistanceSq : Math.pow(sighting.x - this.catchPos.x, 2.0) + Math.pow(sighting.z - this.catchPos.z, 2.0);
            double d2 = d;
            if (!(d2 < best)) continue;
            best = d2;
            nearest = sighting;
        }
        if (nearest == null) {
            return;
        }
        nearest.owned = true;
        if (this.quarry.manyNametags()) {
            double reachSq = 576.0;
            for (Sighting sighting : this.sightings.values()) {
                double dx = sighting.x - nearest.x;
                double dy = sighting.y - nearest.y;
                double dz = sighting.z - nearest.z;
                if (sighting.firstSeenMs < this.lastCatchMs - 2000L || sighting.firstSeenMs > this.lastCatchMs + 4000L || !(dx * dx + dy * dy + dz * dz <= reachSq)) continue;
                sighting.owned = true;
            }
        }
        this.decide("\u00a7aYour " + this.quarry.name().get() + " came up further from your hook than usual; it is yours");
    }

    private void creditedKill(long now, String why, boolean alreadyGone) {
        if (this.lastOwnedDeathMs != 0L && now - this.lastOwnedDeathMs <= 2000L) {
            return;
        }
        while (!this.unclaimedDeaths.isEmpty() && now - this.unclaimedDeaths.peekFirst() > 8000L) {
            this.unclaimedDeaths.removeFirst();
        }
        if (alreadyGone) {
            this.lastOwnedDeathMs = now;
        } else if (!this.unclaimedDeaths.isEmpty()) {
            this.unclaimedDeaths.removeLast();
        } else {
            ++this.deathsAlreadyShared;
        }
        this.lastNamedShareMs = now;
        this.lastDeathMs = Math.max(this.lastDeathMs, now);
        this.decide(why);
        this.kill(now);
        this.settlePending(now);
    }

    private void unnamedShare(long now) {
        if (this.lastOwnedDeathMs != 0L && now - this.lastOwnedDeathMs <= 2000L) {
            return;
        }
        if (this.lastNamedShareMs != 0L && now - this.lastNamedShareMs <= 2000L) {
            return;
        }
        while (!this.unclaimedDeaths.isEmpty() && now - this.unclaimedDeaths.peekFirst() > LOOT_SHARE_WINDOW_MS) {
            this.unclaimedDeaths.removeFirst();
        }
        if (this.unclaimedDeaths.isEmpty() && this.lastHealthyVanishMs != 0L && now - this.lastHealthyVanishMs <= LOOT_SHARE_WINDOW_MS) {
            // Light: the creature was one-shot from high health (nameplate never showed it low)
            this.lastHealthyVanishMs = 0L;
            this.lastDeathMs = Math.max(this.lastDeathMs, now);
            this.decide("\u00a7aLoot share on a " + this.quarry.name().get() + " that was killed in one hit next to you");
            this.kill(now);
            this.settlePending(now);
            return;
        }
        if (!this.unclaimedDeaths.isEmpty()) {
            this.unclaimedDeaths.removeLast();
            this.decide("\u00a7aLoot share on a " + this.quarry.name().get() + " that died next to you");
            this.kill(now);
            this.settlePending(now);
            return;
        }
        this.lastLootShareMs = now;
    }

    public boolean engaged() {
        long last = this.ownOnly() ? this.lastOwnedNearbyMs : this.lastCreatureNearbyMs;
        return this.enabled() && System.currentTimeMillis() - last < 1500L;
    }

    public void onItemGained(String name, String id, int count, boolean viaSack) {
        if (!this.enabled()) {
            return;
        }
        DropLines.Named prize = (DropLines.Named)this.reader.prizeFor(name, id);
        if (prize == this.reader.none()) {
            return;
        }
        int copies = Math.max(1, Math.min(4, count));
        for (int i = 0; i < copies; ++i) {
            this.gained((P)prize, viaSack);
        }
    }

    private void gained(P prize, boolean viaSack) {
        boolean anotherInSight;
        long now = System.currentTimeMillis();
        if (this.ticksInWorld < 60) {
            this.decide("\u00a7c" + String.valueOf(prize) + "\u00a78 arrived while the world was still loading; ignored");
            return;
        }
        if (viaSack) {
            if (this.lastDeathMs == 0L || now - this.lastDeathMs > 40000L) {
                this.decide("\u00a7c" + String.valueOf(prize) + "\u00a78 went into a sack, but no " + this.quarry.name().get() + " had died near you; ignored");
                return;
            }
            this.accept(prize, true, now);
            return;
        }
        boolean bl = anotherInSight = !this.sightings.isEmpty();
        if (anotherInSight && this.reveal.running() && this.reveal.acceptsSecond()) {
            if (this.pending.isEmpty()) {
                this.pendingSinceMs = now;
                this.pendingForNextKill = true;
            }
            this.pending.add(prize);
            return;
        }
        if (this.lastDeathMs != 0L && now - this.lastDeathMs <= 3000L) {
            this.accept(prize, false, now);
            return;
        }
        if (this.pending.isEmpty()) {
            this.pendingSinceMs = now;
            this.pendingForNextKill = false;
        }
        this.pending.add(prize);
    }

    private void settlePending(long now) {
        boolean deathFits;
        if (this.pending.isEmpty()) {
            return;
        }
        deathFits = this.pendingForNextKill ? this.lastDeathMs >= this.pendingSinceMs || now - this.pendingSinceMs > 400L && this.lastDeathMs != 0L && this.pendingSinceMs - this.lastDeathMs <= 3000L : this.lastDeathMs != 0L && this.lastDeathMs >= this.pendingSinceMs - 3000L;
        if (deathFits) {
            ArrayList<P> ready = new ArrayList<P>(this.pending);
            this.pending.clear();
            this.pendingForNextKill = false;
            for (P prize : ready) {
                this.accept(prize, false, now);
            }
        } else if (now - this.pendingSinceMs > 1500L && this.pendingLineMs == 0L) {
            for (DropLines.Named prize : this.pending) {
                this.decide("\u00a7c" + String.valueOf(prize) + "\u00a78 arrived with no " + this.quarry.name().get() + " dying near you; ignored");
            }
            this.pending.clear();
            this.pendingForNextKill = false;
        }
    }

    private void accept(P prize, boolean viaSack, long now) {
        boolean animating;
        boolean bl = animating = !this.reveal.idle();
        if (animating && this.ignoredKillDuringRun) {
            this.decide("\u00a7c" + String.valueOf(prize) + "\u00a78 ignored; a second kill happened this run");
            return;
        }
        if (!animating && this.screenClaimedElsewhere.get().booleanValue()) {
            this.decide("\u00a7c" + String.valueOf(prize) + "\u00a78 ignored; the other reveal owns the screen");
            return;
        }
        if (!animating && this.lastRunStartMs != 0L && this.lastRunStartMs >= this.lastDeathMs - 3000L) {
            this.decide("\u00a7e" + String.valueOf(prize) + "\u00a78 " + (viaSack ? "reported by the sack" : "arrived") + " after the reveal; counted");
            TikiGambleClient.LOGGER.info("[SkyMaxx] {} arrived after the reveal; counted", prize);
            this.reveal.lateDrop(prize);
            return;
        }
        TikiGambleClient.LOGGER.info("[SkyMaxx] drop recognised as {} ({})", prize, (Object)(viaSack ? "sack" : "inventory"));
        this.decide("\u00a7a" + String.valueOf(prize) + "\u00a78 from your " + (viaSack ? "sack" : "inventory"));
        if (!this.reveal.running()) {
            this.startedFromDropMs = now;
            this.lastRunStartMs = now;
        }
        this.reveal.notifyDrop(prize);
    }

    private void decide(String line) {
        this.decisions.addLast(line);
        while (this.decisions.size() > 20) {
            this.decisions.removeFirst();
        }
        if (this.cfg.debug) {
            TikiGambleClient.LOGGER.info("[SkyMaxx] {}", (Object)DropLines.stripFormatting(line));
        }
    }

    public List<String> recentDecisions() {
        return new ArrayList<String>(this.decisions);
    }

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!this.enabled() || mc.level == null || mc.player == null) {
            this.reset();
            return;
        }
        ++this.ticksInWorld;
        if (this.reveal.idle()) {
            this.ignoredKillDuringRun = false;
            this.startedFromDropMs = 0L;
        }
        Vec3 playerPos = mc.player.position();
        boolean teleported = this.lastPlayerPos != null && playerPos.distanceToSqr(this.lastPlayerPos) > 64.0;
        this.lastPlayerPos = playerPos;
        int entityCount = mc.level.getEntityCount();
        boolean bulkUnload = this.lastEntityCount > 20 && (double)entityCount < (double)this.lastEntityCount * 0.5;
        this.lastEntityCount = entityCount;
        long now = System.currentTimeMillis();
        if (mc.player.fishing != null) {
            this.lastHookPos = mc.player.fishing.position();
            this.lastHookMs = now;
        }
        long tick = ++this.tickCount;
        double radiusSq = this.cfg.killRadius * this.cfg.killRadius;
        HashMap<Integer, Sighting> current = new HashMap<Integer, Sighting>();
        HashSet<Integer> matched = new HashSet<Integer>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!this.matchesName(entity)) continue;
            double distanceSq = entity.distanceToSqr((Entity)mc.player);
            Sighting sighting = this.sightings.get(entity.getId());
            if (sighting == null) {
                sighting = new Sighting();
                sighting.id = entity.getId();
                sighting.firstSeenMs = now;
                sighting.x = entity.getX();
                sighting.y = entity.getY();
                sighting.z = entity.getZ();
                if (this.lastCatchMs != 0L && now - this.lastCatchMs <= 4000L && this.nearCatch(sighting)) {
                    sighting.owned = true;
                    this.catchResolved = true;
                }
                this.sightings.put(entity.getId(), sighting);
            }
            ++sighting.ticksSeen;
            sighting.lastMatchTick = tick;
            sighting.lastDistanceSq = distanceSq;
            sighting.x = entity.getX();
            sighting.y = entity.getY();
            sighting.z = entity.getZ();
            String tag = SeaCreatureDetector.nameOf(entity);
            sighting.health = NameplateHealth.fraction(tag);
            sighting.currentHealth = NameplateHealth.current(tag);
            sighting.maxHealth = NameplateHealth.max(tag);
            matched.add(entity.getId());
            if (!(distanceSq <= radiusSq)) continue;
            current.put(entity.getId(), sighting);
        }
        if (!current.isEmpty()) {
            this.lastCreatureNearbyMs = now;
            for (Sighting sighting : current.values()) {
                if (!sighting.owned) continue;
                this.lastOwnedNearbyMs = now;
                break;
            }
        }
        if (!this.screenClaimedElsewhere.get().booleanValue() && this.aboutToDie(current, now)) {
            this.reveal.arm();
        }
        ArrayList<Sighting> gone = new ArrayList<Sighting>();
        Iterator<Map.Entry<Integer, Sighting>> it = this.sightings.entrySet().iterator();
        while (it.hasNext()) {
            Sighting sighting = it.next().getValue();
            if (matched.contains(sighting.id)) continue;
            Entity still = mc.level.getEntity(sighting.id);
            if (still != null) {
                sighting.lastDistanceSq = still.distanceToSqr((Entity)mc.player);
                if (tick - sighting.lastMatchTick <= 400L) continue;
                it.remove();
                continue;
            }
            it.remove();
            gone.add(sighting);
        }
        while (!this.unclaimedDeaths.isEmpty() && now - this.unclaimedDeaths.peekFirst() > LOOT_SHARE_WINDOW_MS) {
            this.unclaimedDeaths.removeFirst();
        }
        this.resolveCatch(now);
        while (!this.recentlyGone.isEmpty() && now - this.recentlyGone.peekFirst().ms() > 4000L) {
            this.recentlyGone.removeFirst();
        }
        if (!gone.isEmpty()) {
            gone.sort(Comparator.comparingDouble(s -> s.lastDistanceSq));
            List<Sighting> deaths = this.bodies(gone);
            for (Sighting sighting : deaths) {
                if (!teleported && sighting.lastDistanceSq <= radiusSq * 2.25) {
                    this.recentlyGone.addLast(new Gone(now, sighting.owned));
                }
                String refused = this.whyNotDeath(sighting, deaths.size(), teleported, bulkUnload, radiusSq, tick);
                if (refused != null && refused.startsWith("still at")) {
                    // Light: a big hit can kill it before the nameplate drops below 50%.
                    if (sighting.owned || this.lastLootShareMs != 0L && now - this.lastLootShareMs <= LOOT_SHARE_WINDOW_MS) {
                        refused = null;
                    } else {
                        this.lastHealthyVanishMs = now;
                    }
                }
                if (this.cfg.debug) {
                    TikiGambleClient.LOGGER.info("[SkyMaxx] {} {} vanished; {}; {}", new Object[]{deaths.size(), this.quarry.name().get(), this.describe(sighting, deaths.size(), teleported, bulkUnload), refused == null ? "a death" : refused});
                }
                if (refused != null) {
                    if (!(sighting.lastDistanceSq <= radiusSq * 2.25) || sighting.ticksSeen < 6) continue;
                    this.decide("\u00a78A " + this.quarry.name().get() + (sighting.owned ? " of yours" : "") + " went out of sight, not counted: " + refused);
                    continue;
                }
                this.onDeath(sighting, now);
            }
        }
        this.settlePendingLine(now);
        this.settlePending(now);
    }

    private void onDeath(Sighting sighting, long now) {
        if (this.ownOnly() && !sighting.owned) {
            this.decide("\u00a78A " + this.quarry.name().get() + " you did not fish up died near you; own catches only");
            return;
        }
        this.lastDeathMs = now;
        if (this.deathsAlreadyShared > 0 && now - this.lastNamedShareMs <= 8000L) {
            --this.deathsAlreadyShared;
            if (sighting.owned) {
                this.lastOwnedDeathMs = now;
            }
            this.decide("\u00a78The " + this.quarry.name().get() + " from that line went out of sight");
            return;
        }
        if (sighting.owned) {
            this.lastOwnedDeathMs = now;
            if (now - this.lastLootShareMs <= LOOT_SHARE_WINDOW_MS) {
                this.lastLootShareMs = 0L;
            }
            this.decide("\u00a7aYour " + this.quarry.name().get() + " died");
            this.kill(now);
            return;
        }
        if (this.lastLootShareMs != 0L && now - this.lastLootShareMs <= LOOT_SHARE_WINDOW_MS) {
            this.lastLootShareMs = 0L;
            this.decide("\u00a7aLoot share on a " + this.quarry.name().get() + " that died next to you");
            this.kill(now);
            return;
        }
        this.unclaimedDeaths.addLast(now);
        this.decide("\u00a78A " + this.quarry.name().get() + " you did not fish up died near you; no loot share yet");
    }

    private void kill(long now) {
        if (this.reveal.running()) {
            if (this.startedFromDropMs != 0L && now - this.startedFromDropMs < 2000L) {
                this.startedFromDropMs = 0L;
                if (this.cfg.debug) {
                    TikiGambleClient.LOGGER.info("[SkyMaxx] the kill behind the drop that started this run");
                }
                return;
            }
            if (this.reveal.acceptsSecond()) {
                this.lastRunStartMs = now;
                this.decide("\u00a7aA second " + this.quarry.name().get() + " died mid-animation; showing it alongside");
                this.reveal.begin();
                return;
            }
            this.ignoredKillDuringRun = true;
            this.decide("\u00a78A second " + this.quarry.name().get() + " died mid-animation; this reveal shows one");
            return;
        }
        if (this.screenClaimedElsewhere.get().booleanValue()) {
            this.decide("\u00a78A " + this.quarry.name().get() + " died while the other reveal owned the screen");
            return;
        }
        this.lastRunStartMs = now;
        this.ignoredKillDuringRun = false;
        this.reveal.begin();
    }

    private boolean aboutToDie(Map<Integer, Sighting> current, long now) {
        double threshold = this.quarry.muteFromHealth().get();
        double radiusSq = this.cfg.killRadius * this.cfg.killRadius * 0.8 * 0.8;
        boolean sharing = !this.ownOnly() && this.lastAnyShareMs != 0L && now - this.lastAnyShareMs <= 300000L;
        for (Sighting sighting : current.values()) {
            if (!sighting.owned && !sharing || sighting.ticksSeen < 6 || sighting.lastDistanceSq > radiusSq || sighting.currentHealth < 0.0f || (double)sighting.currentHealth > threshold || sighting.maxHealth >= 0.0f && (double)sighting.maxHealth <= threshold) continue;
            return true;
        }
        return false;
    }

    private List<Sighting> bodies(List<Sighting> gone) {
        if (!this.quarry.manyNametags() || gone.size() < 2) {
            return gone;
        }
        ArrayList<Sighting> out = new ArrayList<Sighting>();
        double reachSq = 576.0;
        for (Sighting sighting : gone) {
            Sighting body = null;
            for (Sighting kept : out) {
                double dx = kept.x - sighting.x;
                double dy = kept.y - sighting.y;
                double dz = kept.z - sighting.z;
                if (!(dx * dx + dy * dy + dz * dz <= reachSq)) continue;
                body = kept;
                break;
            }
            if (body == null) {
                out.add(sighting);
                continue;
            }
            body.owned |= sighting.owned;
            body.ticksSeen = Math.max(body.ticksSeen, sighting.ticksSeen);
            body.lastMatchTick = Math.max(body.lastMatchTick, sighting.lastMatchTick);
            if (!(sighting.health >= 0.0f) || !(body.health < 0.0f) && !(sighting.health < body.health)) continue;
            body.health = sighting.health;
        }
        return out;
    }

    private String whyNotDeath(Sighting sighting, int vanished, boolean teleported, boolean bulkUnload, double radiusSq, long tick) {
        double reach;
        if (sighting == null) {
            return "nothing tracked";
        }
        if (teleported) {
            return "you moved too far in one tick";
        }
        if (bulkUnload || vanished > 2) {
            return "the world unloaded around it";
        }
        if (this.ticksInWorld < 60) {
            return "the world was still loading";
        }
        if (sighting.ticksSeen < 6) {
            return "seen for only " + sighting.ticksSeen + " ticks";
        }
        if (tick - sighting.lastMatchTick > 40L) {
            return "its nametag had changed long before";
        }
        double d = reach = sighting.owned ? 1.0 : 0.8;
        if (sighting.lastDistanceSq > radiusSq * reach * reach) {
            return String.format(Locale.ROOT, "%.0f blocks away", Math.sqrt(sighting.lastDistanceSq));
        }
        if (sighting.health >= 0.0f && sighting.health > 0.5f) {
            return String.format(Locale.ROOT, "still at %.0f%% health", Float.valueOf(sighting.health * 100.0f));
        }
        return null;
    }

    private String describe(Sighting sighting, int vanished, boolean teleported, boolean bulkUnload) {
        if (sighting == null) {
            return "nothing tracked";
        }
        return String.format("seen=%dt dist=%.1f health=%s yours=%b vanished=%d teleport=%b unload=%b", sighting.ticksSeen, Math.sqrt(sighting.lastDistanceSq), sighting.health < 0.0f ? "unknown" : String.format("%.0f%%", Float.valueOf(sighting.health * 100.0f)), sighting.owned, vanished, teleported, bulkUnload);
    }

    private boolean matchesName(Entity entity) {
        Component name = entity.getCustomName();
        if (name == null) {
            return false;
        }
        String clean = DropLines.stripFormatting(name.getString());
        String wanted = this.quarry.name().get();
        if (wanted == null || wanted.isBlank() || !clean.contains(wanted)) {
            return false;
        }
        String excludes = this.quarry.excludeNames().get();
        if (excludes == null || excludes.isBlank()) {
            return true;
        }
        String lower = clean.toLowerCase(Locale.ROOT);
        for (String exclude : excludes.split(",")) {
            String trimmed = exclude.trim().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty() || !lower.contains(trimmed)) continue;
            return false;
        }
        return true;
    }

    private static String nameOf(Entity entity) {
        Component name = entity.getCustomName();
        return name == null ? "" : DropLines.stripFormatting(name.getString());
    }

    public void reset() {
        this.sightings.clear();
        this.pending.clear();
        this.pendingForNextKill = false;
        this.unclaimedDeaths.clear();
        this.ignoredKillDuringRun = false;
        this.startedFromDropMs = 0L;
        this.ticksInWorld = 0;
        this.lastEntityCount = 0;
        this.lastPlayerPos = null;
        this.lastDeathMs = 0L;
        this.lastCatchMs = 0L;
        this.lastLootShareMs = 0L;
        this.lastNamedShareMs = 0L;
        this.lastHealthyVanishMs = 0L;
        this.deathsAlreadyShared = 0;
        this.lastOwnedDeathMs = 0L;
        this.catchPos = null;
        this.catchResolved = true;
        this.lastHookPos = null;
        this.lastHookMs = 0L;
        this.recentlyGone.clear();
        this.pendingLineMs = 0L;
    }

    private boolean ownOnly() {
        return Boolean.TRUE.equals(this.quarry.ownOnly().get());
    }

    private boolean enabled() {
        if (!Boolean.TRUE.equals(this.quarry.enabled().get())) {
            return false;
        }
        return !this.cfg.requireSkyblock || SeaCreatureDetector.onHypixel();
    }

    public static boolean onHypixel() {
        Minecraft mc = Minecraft.getInstance();
        ServerData server = mc.getCurrentServer();
        if (server == null || server.ip == null) {
            return false;
        }
        return server.ip.toLowerCase(Locale.ROOT).contains("hypixel.net");
    }

    public record Quarry(Supplier<Boolean> enabled, Supplier<String> name, Supplier<String> excludeNames, Supplier<Double> muteFromHealth, Supplier<String> catchMessage, Supplier<String> lootShareMessage, boolean manyNametags, Supplier<Boolean> ownOnly) {
    }

    public static interface Reveal<P> {
        public boolean idle();

        public boolean running();

        public void arm();

        public void begin();

        public void notifyDrop(P var1);

        default public void lateDrop(P prize) {
        }

        default public boolean acceptsSecond() {
            return false;
        }
    }

    public static interface DropReader<P> {
        public P prizeFor(String var1, String var2);

        public P none();
    }

    private static final class Sighting {
        int id;
        int ticksSeen;
        double lastDistanceSq;
        double x;
        double y;
        double z;
        long lastMatchTick;
        long firstSeenMs;
        boolean owned;
        float health = -1.0f;
        float currentHealth = -1.0f;
        float maxHealth = -1.0f;

        private Sighting() {
        }
    }

    private record Gone(long ms, boolean owned) {
    }
}

