package com.iceandfirebondbeyond.util;

/** Fixed-point progress keeps fractional speeds across saves without rounding every tick away. */
public final class SeaSerpentProgress {
    public static final int PRECISION = 10000;
    private SeaSerpentProgress() {}

    public static Step advance(int ticks, int remainder, double speed, int maximum) {
        int cap = Math.max(0, maximum);
        int current = Math.max(0, Math.min(cap, ticks));
        if (current == cap) return new Step(cap, 0);
        long fraction = Math.max(0, Math.min(PRECISION - 1, remainder));
        if (Double.isFinite(speed) && speed > 0.0D) {
            fraction += Math.round(Math.min(1000.0D, speed) * PRECISION);
        }
        long next = current + fraction / PRECISION;
        return next >= cap ? new Step(cap, 0) : new Step((int) next, (int) (fraction % PRECISION));
    }

    public static int migrateRemainder(int remainder, int oldPrecision) {
        int precision = Math.max(1, oldPrecision);
        return (int) (Math.max(0L, Math.min(precision - 1L, remainder)) * PRECISION / precision);
    }

    public record Step(int ticks, int remainder) {}
}
