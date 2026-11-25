package com.skymaxx.tiki.synth;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.synth.Reverb;
import com.skymaxx.tiki.synth.Voice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public final class SynthEngine {
    private static final float SAMPLE_RATE = 44100.0f;
    private static final int BLOCK = 512;
    private static final int BUFFER_BLOCKS = 16;
    private static final int PRIME_BLOCKS = 6;
    private static final int MAX_VOICES = 192;
    private static final int FAILURES_BEFORE_GIVING_UP = 8;
    private static final long IDLE_CLOSE_NANOS = 4000000000L;
    private static final long STUCK_NANOS = 1500000000L;
    private static final float DUCK_RECOVERY = 4.6f;
    private static final float DUCK_STEP = 1.0f - (float)Math.exp(-0.053405895829200745);
    private final ConcurrentLinkedQueue<Queued> pending = new ConcurrentLinkedQueue();
    private final List<Voice> active = new ArrayList<Voice>();
    private final List<Voice> activeBed = new ArrayList<Voice>();
    private final Reverb reverb = new Reverb(44100.0f);
    private final ConcurrentLinkedQueue<Delayed> queuedActions = new ConcurrentLinkedQueue();
    private final List<Delayed> timers = new ArrayList<Delayed>();
    private final float[] eventL = new float[512];
    private final float[] eventR = new float[512];
    private final float[] bedL = new float[512];
    private final float[] bedR = new float[512];
    private final float[] send = new float[512];
    private final AtomicInteger silenceRequests = new AtomicInteger();
    private int silenceHandled;
    private final AtomicInteger bedSilenceRequests = new AtomicInteger();
    private int bedSilenceHandled;
    private final AtomicInteger duckRequest = new AtomicInteger();
    private float duck = 1.0f;
    private volatile float master = 0.85f;
    private volatile boolean running;
    private volatile boolean broken;
    private Thread thread;
    private volatile SourceDataLine line;
    private volatile int generation;
    private final Object mixLock = new Object();
    private volatile long lastWriteNanos;
    private volatile long awakeUntilNanos;

    public void setMaster(float value) {
        this.master = value < 0.0f ? 0.0f : (value > 1.0f ? 1.0f : value);
    }

    public boolean broken() {
        return this.broken;
    }

    public synchronized void start() {
        if (this.running) {
            return;
        }
        this.running = true;
        this.startThread();
    }

    private synchronized void startThread() {
        int mine = ++this.generation;
        this.lastWriteNanos = 0L;
        this.thread = new Thread(() -> this.run(mine), "SkyMaxx-Synth");
        this.thread.setDaemon(true);
        try {
            this.thread.setPriority(10);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.thread.start();
    }

    public synchronized void shutdown() {
        this.running = false;
        ++this.generation;
        if (this.thread != null) {
            try {
                this.thread.join(400L);
            }
            catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            this.thread = null;
        }
        SynthEngine.closeLineAsync(this.line);
        this.line = null;
    }

    public void keepAwake() {
        this.awakeUntilNanos = System.nanoTime() + 4000000000L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void watchdog() {
        long last = this.lastWriteNanos;
        if (!this.running || last == 0L || this.line == null) {
            return;
        }
        if (System.nanoTime() - last > 1500000000L) {
            TikiGambleClient.LOGGER.warn("[SkyMaxx] the sound output stopped taking audio; reopening it");
            SourceDataLine stuck = this.line;
            this.line = null;
            SynthEngine synthEngine = this;
            synchronized (synthEngine) {
                if (!this.running) {
                    return;
                }
                this.startThread();
            }
            SynthEngine.closeLineAsync(stuck);
        }
    }

    private static SourceDataLine openLine(AudioFormat format) throws Exception {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        Exception firstProblem = null;
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (!mixer.getClass().getName().startsWith("com.sun.media.sound.") || !mixer.isLineSupported(info)) continue;
                SourceDataLine candidate = (SourceDataLine)mixer.getLine(info);
                candidate.open(format, 32768);
                candidate.start();
                return candidate;
            }
            catch (Exception e) {
                if (firstProblem != null) continue;
                firstProblem = e;
            }
        }
        if (!AudioSystem.isLineSupported(info)) {
            throw firstProblem != null ? firstProblem : new IllegalStateException("no audio output");
        }
        SourceDataLine fallback = (SourceDataLine)AudioSystem.getLine(info);
        fallback.open(format, 32768);
        fallback.start();
        return fallback;
    }

    private static void closeLineAsync(SourceDataLine old) {
        if (old == null) {
            return;
        }
        Thread closer = new Thread(() -> {
            try {
                old.stop();
                old.flush();
                old.close();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }, "SkyMaxx-Synth-Close");
        closer.setDaemon(true);
        closer.start();
    }

    public void play(Voice voice) {
        this.enqueue(voice, false);
    }

    public void playBed(Voice voice) {
        this.enqueue(voice, true);
    }

    private void enqueue(Voice voice, boolean bed) {
        if (!this.running || voice == null) {
            return;
        }
        if (this.pending.size() < 512) {
            this.pending.add(new Queued(voice, bed));
        }
    }

    public void after(float seconds, Runnable action) {
        if (!this.running || action == null) {
            return;
        }
        if (this.queuedActions.size() < 512) {
            this.queuedActions.add(new Delayed(seconds, action));
        }
    }

    public void duck(float depth) {
        int milli = (int)(Math.max(0.0f, Math.min(1.0f, depth)) * 1000.0f);
        this.duckRequest.accumulateAndGet(milli, Math::max);
    }

    public void silence() {
        this.pending.clear();
        this.queuedActions.clear();
        this.silenceRequests.incrementAndGet();
        this.duckRequest.set(0);
    }

    public void silenceBed() {
        this.bedSilenceRequests.incrementAndGet();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void run(int mine) {
        byte[] out = new byte[2048];
        AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, false);
        SourceDataLine own = null;
        int failures = 0;
        long quietSince = 0L;
        while (this.running && mine == this.generation) {
            try {
                long now;
                boolean sounding;
                if (own == null) {
                    if (!this.hasWork() && System.nanoTime() > this.awakeUntilNanos) {
                        this.lastWriteNanos = 0L;
                        Thread.sleep(15L);
                        continue;
                    }
                    own = SynthEngine.openLine(format);
                    if (mine != this.generation) {
                        SynthEngine.closeLineAsync(own);
                        return;
                    }
                    this.line = own;
                    this.lastWriteNanos = System.nanoTime();
                    this.broken = false;
                    failures = 0;
                    quietSince = 0L;
                    Arrays.fill(out, (byte)0);
                    for (int i = 0; i < 6; ++i) {
                        own.write(out, 0, out.length);
                    }
                    this.lastWriteNanos = System.nanoTime();
                }
                Object object = this.mixLock;
                synchronized (object) {
                    if (mine != this.generation) {
                        return;
                    }
                    this.renderBlock(out);
                    sounding = this.hasWork();
                }
                own.write(out, 0, out.length);
                if (mine != this.generation) {
                    return;
                }
                this.lastWriteNanos = now = System.nanoTime();
                failures = 0;
                if (sounding || now < this.awakeUntilNanos) {
                    quietSince = 0L;
                    continue;
                }
                if (quietSince == 0L) {
                    quietSince = now;
                    continue;
                }
                if (now - quietSince <= 4000000000L) continue;
                this.line = null;
                this.lastWriteNanos = 0L;
                SynthEngine.closeLineAsync(own);
                own = null;
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            catch (Throwable t) {
                if (mine != this.generation) {
                    return;
                }
                if (++failures == 1 || failures == 8) {
                    TikiGambleClient.LOGGER.warn("[SkyMaxx] sound output failed ({}); trying again", (Object)failures, (Object)t);
                }
                if (failures >= 8) {
                    this.broken = true;
                }
                this.line = null;
                this.lastWriteNanos = 0L;
                SynthEngine.closeLineAsync(own);
                own = null;
                Object now = this.mixLock;
                synchronized (now) {
                    this.active.clear();
                    this.activeBed.clear();
                    this.timers.clear();
                }
                try {
                    Thread.sleep(Math.min(5000L, 100L * (long)failures * (long)failures));
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        if (own != null && mine != this.generation) {
            SynthEngine.closeLineAsync(own);
        }
    }

    private boolean hasWork() {
        return !this.pending.isEmpty() || !this.queuedActions.isEmpty() || !this.active.isEmpty() || !this.activeBed.isEmpty() || !this.timers.isEmpty();
    }

    public void renderBlock(byte[] out) {
        Delayed d;
        Queued q;
        int bedRequested;
        int requested = this.silenceRequests.get();
        if (requested != this.silenceHandled) {
            this.silenceHandled = requested;
            this.active.clear();
            this.activeBed.clear();
            this.timers.clear();
            this.reverb.reset();
            this.duck = 1.0f;
        }
        if ((bedRequested = this.bedSilenceRequests.get()) != this.bedSilenceHandled) {
            this.bedSilenceHandled = bedRequested;
            this.activeBed.clear();
            this.duck = 1.0f;
        }
        while ((q = this.pending.poll()) != null) {
            List<Voice> into;
            List<Voice> list = into = q.bed() ? this.activeBed : this.active;
            if (into.size() >= 192) {
                into.remove(0);
            }
            into.add(q.voice());
        }
        while ((d = this.queuedActions.poll()) != null) {
            this.timers.add(d);
        }
        float blockSeconds = 0.011609977f;
        for (int i = this.timers.size() - 1; i >= 0; --i) {
            Delayed timer = this.timers.get(i);
            float left = timer.seconds() - blockSeconds;
            if (left > 0.0f) {
                this.timers.set(i, new Delayed(left, timer.action()));
                continue;
            }
            this.timers.remove(i);
            try {
                timer.action().run();
                continue;
            }
            catch (Throwable t) {
                TikiGambleClient.LOGGER.warn("[SkyMaxx] a scheduled sound failed", t);
            }
        }
        while ((q = this.pending.poll()) != null) {
            List<Voice> into;
            List<Voice> list = into = q.bed() ? this.activeBed : this.active;
            if (into.size() >= 192) {
                into.remove(0);
            }
            into.add(q.voice());
        }
        Arrays.fill(this.eventL, 0.0f);
        Arrays.fill(this.eventR, 0.0f);
        Arrays.fill(this.bedL, 0.0f);
        Arrays.fill(this.bedR, 0.0f);
        Arrays.fill(this.send, 0.0f);
        this.render(this.active, this.eventL, this.eventR);
        this.render(this.activeBed, this.bedL, this.bedR);
        this.reverb.process(this.send, this.eventL, this.eventR, 512);
        int wanted = this.duckRequest.getAndSet(0);
        if (wanted > 0) {
            this.duck = Math.min(this.duck, 1.0f - (float)wanted / 1000.0f);
        }
        float start = this.duck;
        this.duck += (1.0f - this.duck) * DUCK_STEP;
        float slope = (this.duck - start) / 512.0f;
        float g = this.master * 0.28f;
        int k = 0;
        for (int i = 0; i < 512; ++i) {
            float bedGain = start + slope * (float)i;
            int li = SynthEngine.pcm(SynthEngine.softClip((this.eventL[i] + this.bedL[i] * bedGain) * g));
            int ri = SynthEngine.pcm(SynthEngine.softClip((this.eventR[i] + this.bedR[i] * bedGain) * g));
            out[k++] = (byte)(li & 0xFF);
            out[k++] = (byte)(li >> 8 & 0xFF);
            out[k++] = (byte)(ri & 0xFF);
            out[k++] = (byte)(ri >> 8 & 0xFF);
        }
    }

    private void render(List<Voice> voices, float[] l, float[] r) {
        for (int i = voices.size() - 1; i >= 0; --i) {
            boolean alive;
            try {
                alive = voices.get(i).render(l, r, this.send, 512, 44100.0f);
            }
            catch (Throwable t) {
                alive = false;
                TikiGambleClient.LOGGER.warn("[SkyMaxx] a voice failed to render", t);
            }
            if (alive) continue;
            voices.remove(i);
        }
    }

    public void startOffline() {
        this.running = true;
    }

    public static float sampleRate() {
        return 44100.0f;
    }

    public static int blockSize() {
        return 512;
    }

    private static float softClip(float x) {
        if (x > 1.4f) {
            return 1.0f;
        }
        if (x < -1.4f) {
            return -1.0f;
        }
        return x - x * x * x / 5.88f;
    }

    private static int pcm(float x) {
        int v = (int)(x * 32767.0f);
        return v > Short.MAX_VALUE ? Short.MAX_VALUE : (v < Short.MIN_VALUE ? Short.MIN_VALUE : v);
    }

    private record Queued(Voice voice, boolean bed) {
    }

    private record Delayed(float seconds, Runnable action) {
    }
}

