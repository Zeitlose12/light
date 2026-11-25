package com.skymaxx.tiki.anim;

import com.skymaxx.tiki.anim.ReelProfile;
import com.skymaxx.tiki.anim.SpinCurve;
import com.skymaxx.tiki.config.TikiConfig;
import java.util.Random;

public class Reel {
    public final int index;
    private final ReelProfile profile;
    private State state = State.WAITING;
    private boolean won;
    private float duration;
    private float elapsed;
    private SpinCurve curve;
    private float landFlash;
    private float sinceLanded = -1.0f;
    private static final float RECOIL_SECONDS = 0.4f;
    private static final float RECOIL_HZ = 7.0f;
    private static final float RECOIL_DECAY = 9.0f;

    public Reel(int index) {
        this.index = index;
        this.profile = ReelProfile.forTier(index);
    }

    public void setOutcome(boolean willWin) {
        this.won = willWin;
        this.state = State.WAITING;
        this.elapsed = 0.0f;
        this.landFlash = 0.0f;
        this.sinceLanded = -1.0f;
        this.curve = null;
    }

    public void reviseOutcome(boolean willWin) {
        if (this.state == State.WAITING) {
            this.setOutcome(willWin);
        } else {
            this.won = willWin;
        }
    }

    public void start(float durationSeconds, TikiConfig cfg, Random rng) {
        this.duration = Math.max(0.2f, durationSeconds);
        this.elapsed = 0.0f;
        this.state = State.SPINNING;
        this.sinceLanded = -1.0f;
        float judder = 0.3f * (0.8f + rng.nextFloat() * 0.4f);
        this.curve = SpinCurve.build(180.0f, ReelProfile.forTier(this.index, this.duration), 3.0f, judder);
    }

    public void tick(float deltaSeconds) {
        if (this.state == State.WAITING) {
            return;
        }
        if (this.state == State.SPINNING) {
            this.elapsed += deltaSeconds;
            if (this.elapsed >= this.duration) {
                this.elapsed = this.duration;
                this.state = State.LANDED;
                this.landFlash = 0.0f;
                this.sinceLanded = 0.0f;
            }
        } else if (this.state == State.LANDED) {
            if (this.landFlash < 1.0f) {
                this.landFlash = Math.min(1.0f, this.landFlash + deltaSeconds * 2.4f);
            }
            if (this.sinceLanded >= 0.0f) {
                this.sinceLanded += deltaSeconds;
                if (this.sinceLanded > 0.4f) {
                    this.sinceLanded = -1.0f;
                }
            }
        }
    }

    public void forceLanded() {
        this.state = State.LANDED;
        this.elapsed = this.duration;
        this.landFlash = 1.0f;
        this.sinceLanded = -1.0f;
    }

    public float angle() {
        if (this.state == State.WAITING) {
            return 0.0f;
        }
        if (this.state == State.LANDED || this.curve == null) {
            return 180.0f + this.recoil();
        }
        return this.curve.degrees(this.elapsed / this.duration);
    }

    private float recoil() {
        if (this.sinceLanded < 0.0f) {
            return 0.0f;
        }
        float envelope = (float)Math.exp(-9.0f * this.sinceLanded);
        return ReelProfile.recoilFor(this.index) * envelope * (float)Math.sin(43.982297150257104 * (double)this.sinceLanded);
    }

    public float secondsToCorner() {
        if (this.state == State.LANDED) {
            return 0.0f;
        }
        if (this.state != State.SPINNING || this.curve == null) {
            return Float.MAX_VALUE;
        }
        float corner = 90.0f;
        float now = this.elapsed / this.duration;
        if (this.curve.degrees(now) >= corner) {
            return 0.0f;
        }
        for (int i = 1; i <= 200; ++i) {
            float f = now + (1.0f - now) * (float)i / 200.0f;
            if (!(this.curve.degrees(f) >= corner)) continue;
            return (f - now) * this.duration;
        }
        return (1.0f - now) * this.duration;
    }

    public float secondsToLanding() {
        if (this.state == State.LANDED) {
            return 0.0f;
        }
        return this.state == State.SPINNING ? this.duration - this.elapsed : Float.MAX_VALUE;
    }

    public float sinceLanded() {
        return this.sinceLanded;
    }

    public float yaw() {
        return 180.0f + this.angle();
    }

    public float angularVelocity() {
        if (this.state != State.SPINNING || this.curve == null) {
            return 0.0f;
        }
        return this.curve.velocity(this.elapsed / this.duration, this.duration);
    }

    public float progress() {
        if (this.state == State.WAITING) {
            return 0.0f;
        }
        if (this.state == State.LANDED) {
            return 1.0f;
        }
        return this.elapsed / this.duration;
    }

    public State state() {
        return this.state;
    }

    public boolean won() {
        return this.won;
    }

    public boolean landed() {
        return this.state == State.LANDED;
    }

    public boolean showsMask() {
        return this.state == State.LANDED && this.won;
    }

    public boolean blanked() {
        return !this.won;
    }

    public float landFlash() {
        return this.landFlash;
    }

    public static enum State {
        WAITING,
        SPINNING,
        LANDED;

    }
}

