package com.iceandfirebondbeyond.util;

/** One age/size curve for living serpents and saved remains. No spawn-only stage table. */
public final class SeaSerpentGrowth {
    public static final int DAYS_PER_STAGE = 25, MAX_DAYS = 125, TICKS_PER_DAY = 24000;
    public static final float BABY_SCALE = 0.8F, ANCIENT_SCALE = 6.0F, MAX_SCALE = 11.0F;

    public static int firstDay(int stage) { return (Math.max(1, Math.min(5, stage)) - 1) * DAYS_PER_STAGE; }
    public static int lastDay(int stage) { return stage >= 5 ? MAX_DAYS : firstDay(stage) + DAYS_PER_STAGE - 1; }
    public static int stageForTicks(int ticks) { return Math.max(1, Math.min(5, ticks / TICKS_PER_DAY / DAYS_PER_STAGE + 1)); }
    public static float scaleForTicks(int ticks) {
        float days = Math.max(0, Math.min(MAX_DAYS * TICKS_PER_DAY, ticks)) / (float) TICKS_PER_DAY;
        float ancientDay = firstDay(5);
        return days < ancientDay
                ? BABY_SCALE + (ANCIENT_SCALE - BABY_SCALE) * days / ancientDay
                : ANCIENT_SCALE + (MAX_SCALE - ANCIENT_SCALE) * (days - ancientDay) / DAYS_PER_STAGE;
    }
    public static int ticksForScale(float scale) {
        if (!Float.isFinite(scale)) return 0;
        scale = Math.max(BABY_SCALE, Math.min(MAX_SCALE, scale));
        float days = scale < ANCIENT_SCALE
                ? firstDay(5) * (scale - BABY_SCALE) / (ANCIENT_SCALE - BABY_SCALE)
                : firstDay(5) + DAYS_PER_STAGE * (scale - ANCIENT_SCALE) / (MAX_SCALE - ANCIENT_SCALE);
        return Math.round(days * TICKS_PER_DAY);
    }
    public static int stageForScale(float scale) {
        for (int stage = 5; stage >= 2; stage--)
            if (scale >= scaleForTicks(firstDay(stage) * TICKS_PER_DAY)) return stage;
        return 1;
    }
    public static int randomSpawnTicks(float scale, java.util.function.IntUnaryOperator nextInt) {
        if (scale >= MAX_SCALE) return MAX_DAYS * TICKS_PER_DAY;
        int stage = stageForScale(scale), first = firstDay(stage);
        return (first + nextInt.applyAsInt(lastDay(stage) - first + 1)) * TICKS_PER_DAY;
    }
    private SeaSerpentGrowth() {}
}
