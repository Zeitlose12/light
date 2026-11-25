package com.skymaxx.tiki.detect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class InventoryDiff {
    public static final long PEAK_MEMORY_MS = 20000L;
    private final Map<Item, Integer> last = new HashMap<Item, Integer>();
    private final Map<Item, Peak> peaks = new HashMap<Item, Peak>();
    private boolean primed;

    public void reset() {
        this.last.clear();
        this.peaks.clear();
        this.primed = false;
    }

    public List<Gain> update(Map<Item, Integer> counts, long nowMs, boolean quiet) {
        ArrayList<Gain> gains = new ArrayList<Gain>();
        if (!this.primed || quiet) {
            this.last.clear();
            this.last.putAll(counts);
            this.peaks.clear();
            for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
                this.peaks.put(entry.getKey(), new Peak(entry.getValue(), nowMs));
            }
            this.primed = true;
            return gains;
        }
        HashSet<Item> seen = new HashSet<Item>(counts.keySet());
        seen.addAll(this.last.keySet());
        seen.addAll(this.peaks.keySet());
        for (Item item : seen) {
            int high;
            int now = counts.getOrDefault(item, 0);
            int before = this.last.getOrDefault(item, 0);
            Peak peak = this.peaks.get(item);
            if (peak != null && nowMs - peak.atMs() > 20000L) {
                peak = null;
            }
            if (now > (high = Math.max(before, peak == null ? 0 : peak.count()))) {
                gains.add(new Gain(item, now - high));
            }
            if (peak == null || now >= peak.count()) {
                if (now > 0) {
                    this.peaks.put(item, new Peak(now, nowMs));
                    continue;
                }
                this.peaks.remove(item);
                continue;
            }
            this.peaks.put(item, peak);
        }
        this.last.clear();
        this.last.putAll(counts);
        return gains;
    }

    public record Item(String name, String id) {
        private String key() {
            return this.id == null || this.id.isEmpty() ? "name:" + this.name : "id:" + this.id;
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Item)) return false;
            Item item = (Item)other;
            if (!this.key().equals(item.key())) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return this.key().hashCode();
        }
    }

    private record Peak(int count, long atMs) {
    }

    public record Gain(Item item, int count) {
    }
}

