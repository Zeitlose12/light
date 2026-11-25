package com.skymaxx.tiki.boa;

import com.skymaxx.tiki.boa.BoaOdds;
import com.skymaxx.tiki.boa.BoaPrize;
import java.util.Random;

public final class BoaRun {
    private BoaPrize prize = BoaPrize.NONE;
    private boolean locked;
    private int rolled;
    private int bullets;
    private int fired;
    private int hits;
    private int reloads;
    private boolean thrashing;
    private Boolean next;

    public void reset() {
        this.prize = BoaPrize.NONE;
        this.locked = false;
        this.rolled = 0;
        this.bullets = 0;
        this.fired = 0;
        this.hits = 0;
        this.reloads = 0;
        this.thrashing = false;
        this.next = null;
    }

    public BoaPrize prize() {
        return this.prize;
    }

    public void know(BoaPrize known) {
        if (!this.locked) {
            this.prize = BoaPrize.best(this.prize, known);
        }
    }

    public int lock(Random rng) {
        return this.lock(BoaOdds.rollBullets(rng, this.prize));
    }

    public int lock(int count) {
        this.locked = true;
        this.bullets = this.rolled = Math.max(5, Math.min(8, count));
        this.fired = 0;
        this.hits = 0;
        this.thrashing = false;
        this.next = null;
        return this.rolled;
    }

    public boolean locked() {
        return this.locked;
    }

    public int rolled() {
        return this.rolled;
    }

    public int bullets() {
        return this.bullets;
    }

    public int fired() {
        return this.fired;
    }

    public int left() {
        return this.bullets - this.fired;
    }

    public int hits() {
        return this.hits;
    }

    public int reloads() {
        return this.reloads;
    }

    public int target() {
        return this.prize.segments();
    }

    public boolean over() {
        if (!this.locked) {
            return false;
        }
        int target = this.target();
        if (target > 0 && this.hits >= target) {
            return true;
        }
        return this.fired >= this.bullets;
    }

    public boolean canShow(BoaPrize candidate) {
        return this.canShow(candidate, false);
    }

    public boolean canShow(BoaPrize candidate, boolean unopened) {
        boolean fourthUnseen;
        if (candidate == null || !candidate.dropped()) {
            return false;
        }
        if (!this.locked) {
            return true;
        }
        boolean bl = fourthUnseen = unopened && this.hits == 4;
        if (this.target() > 0 && this.hits >= this.target() && !fourthUnseen) {
            return candidate.inHead() && !this.prize.inHead() && this.hits < 5;
        }
        if (candidate.inHead()) {
            return this.hits < 5;
        }
        return this.hits < 4 || fourthUnseen;
    }

    public int raise(BoaPrize to) {
        return this.raise(to, false);
    }

    public int raise(BoaPrize to, boolean unopened) {
        if (!this.canShow(to, unopened) || to.rank() <= this.prize.rank()) {
            return 0;
        }
        this.prize = to;
        this.next = null;
        int short_ = this.target() - this.hits - this.left();
        if (short_ > 0) {
            this.bullets += short_;
            this.reloads += short_;
            return short_;
        }
        return 0;
    }

    public boolean decide(Random rng) {
        if (this.next == null) {
            this.next = rng.nextDouble() < BoaOdds.nextHit(this.target(), this.hits, this.left(), this.thrashing);
        }
        return this.next;
    }

    public Boolean upcoming() {
        return this.next;
    }

    public boolean thrashing() {
        return this.thrashing;
    }

    public boolean fire(Random rng) {
        boolean hit = this.decide(rng);
        this.next = null;
        ++this.fired;
        if (hit) {
            ++this.hits;
        }
        this.thrashing = hit;
        return hit;
    }

    public void finish(Random rng) {
        int guard = 0;
        while (!this.over() && guard++ < 64) {
            this.fire(rng);
        }
    }
}

