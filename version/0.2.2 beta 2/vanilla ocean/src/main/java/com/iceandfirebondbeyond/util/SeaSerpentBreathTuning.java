package com.iceandfirebondbeyond.util;

/** One age curve and impact cadence for wild, owned, ridden and forging water breath. */
public final class SeaSerpentBreathTuning {
    public static final int PULSE_TICKS = 2, HIT_TICKS = 10, EROSION_TICKS = 10, BLOCK_COOLDOWN_TICKS = 40;
    public static final double SPEED = 2.4D, RANGE = 144.0D;
    // DPS relative to IAF's configured fire-breath base, at days 0/25/50/75/100/125.
    private static final double[] PRESSURE = {1, 2, 3.5, 6, 9, 11};
    private SeaSerpentBreathTuning() {}

    public static double damagePerSecond(int ageTicks, double elementalBase, double multiplier) {
        if (!Double.isFinite(elementalBase) || !Double.isFinite(multiplier)) return 0;
        double age = Math.max(0, Math.min(SeaSerpentGrowth.MAX_DAYS * SeaSerpentGrowth.TICKS_PER_DAY, ageTicks))
                / (double) (SeaSerpentGrowth.DAYS_PER_STAGE * SeaSerpentGrowth.TICKS_PER_DAY);
        int index = Math.min(4, (int) age);
        double progress = age - index;
        return Math.max(0, elementalBase) * Math.max(0, Math.min(10, multiplier))
                * (PRESSURE[index] + (PRESSURE[index + 1] - PRESSURE[index]) * progress);
    }

    public static float damagePerHit(int ageTicks, double elementalBase, double multiplier) {
        return (float) (damagePerSecond(ageTicks, elementalBase, multiplier) * HIT_TICKS / 20.0D);
    }

    public static float diameter(float scale, boolean leader, double randomSize, double multiplier) {
        double base = .18D + Math.max(SeaSerpentGrowth.BABY_SCALE, Math.min(SeaSerpentGrowth.MAX_SCALE, scale)) * .045D;
        // Even the centered forge carrier is a small droplet, not a giant leading ball.
        // Squared noise favors small beads with occasional larger bubbles throughout the spray.
        double size = .30D + .95D * unit(randomSize) * unit(randomSize);
        return (float) Math.max(.12D, Math.min(1.5D, base * size * multiplier));
    }

    public static int erosionLimit(int stage, int maximumBlocksPerSecond) {
        if (maximumBlocksPerSecond <= 0) return 0;
        return Math.max(0, (int) Math.floor(Math.min(100, maximumBlocksPerSecond)
                * Math.max(1, Math.min(5, stage)) / 5.0D * EROSION_TICKS / 20.0D));
    }

    public static double erosionRadius(int stage, double multiplier) {
        return Math.max(.5D, Math.min(3.0D, SeaSerpentBreathArea.radius(stage) * multiplier));
    }

    public static boolean insideErosion(int dx, int dy, int dz, double radius) {
        return dx * dx + dy * dy + dz * dz <= (radius + .5D) * (radius + .5D);
    }

    public static boolean insideForgeBed(int dx, int dz, double radius) {
        return dx * dx + dz * dz <= (radius + .75D) * (radius + .75D);
    }

    /** Young wild serpents can breach too; they must not inherit the Stage-3 fallback. */
    public static float youngSlamDamage(int ageTicks) {
        double days = Math.max(0, ageTicks) / (double) SeaSerpentGrowth.TICKS_PER_DAY;
        return days < 25 ? (float) (4 + Math.min(1, days / 25) * 8)
                : (float) (12 + Math.min(1, (days - 25) / 25) * 23);
    }

    private static double unit(double value) { return Math.max(0, Math.min(1, value)); }
}
