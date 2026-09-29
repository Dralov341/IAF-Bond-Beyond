package com.iceandfirebondbeyond.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Count impact opportunities, not droplets. Pure timing code, exercised by release tests. */
public final class SeaSerpentBreathBudget {
    private long nextEmission = Long.MIN_VALUE, nextErosion = Long.MIN_VALUE;
    private final Cooldowns<UUID> victims = new Cooldowns<>(4096);

    public boolean emit(long now) {
        if (now < nextEmission) return false;
        nextEmission = now + SeaSerpentBreathTuning.PULSE_TICKS;
        return true;
    }

    public boolean erode(long now) {
        if (now < nextErosion) return false;
        nextErosion = now + SeaSerpentBreathTuning.EROSION_TICKS;
        return true;
    }

    public boolean hit(UUID victim, long now) {
        return victims.take(victim, now, SeaSerpentBreathTuning.HIT_TICKS);
    }

    public void blocked(UUID victim, long now) {
        victims.release(victim, now + SeaSerpentBreathTuning.HIT_TICKS);
    }

    public static final class Cooldowns<K> {
        private final Map<K, Long> until = new HashMap<>();
        private final int capacity;
        private long nextPrune = Long.MIN_VALUE;

        public Cooldowns(int capacity) { this.capacity = Math.max(1, capacity); }

        public boolean take(K key, long now, int interval) {
            if (now >= nextPrune) {
                until.values().removeIf(expiry -> expiry <= now);
                nextPrune = now + 10;
            }
            Long previous = until.get(key);
            if (previous != null && now < previous) return false;
            // A full table fails closed; eviction must never permit extra damage/erosion.
            if (previous == null && until.size() >= capacity) return false;
            until.put(key, now + Math.max(1, interval));
            return true;
        }

        public void release(K key, long reservedUntil) {
            until.remove(key, reservedUntil);
        }
    }
}
