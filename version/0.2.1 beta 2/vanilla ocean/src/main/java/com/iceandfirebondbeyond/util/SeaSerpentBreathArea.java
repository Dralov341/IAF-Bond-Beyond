package com.iceandfirebondbeyond.util;

/** Terrain footprint of IAF beta-5's sustained fire, ice and lightning breath. */
public final class SeaSerpentBreathArea {
    public static int radius(int stage) {
        return stage <= 3 ? 1 : stage == 4 ? 2 : 3;
    }

    public static boolean contains(int stage, int x, int y, int z) {
        int radius = radius(stage);
        if (Math.abs(x) > radius || Math.abs(y) > radius || Math.abs(z) > radius) return false;
        if (stage <= 3) return true;
        // Keep the same rounded boundary as IafDragonDestructionManager.destroyAreaBreath.
        float reach = (radius + radius + radius) * 0.333F + 0.5F;
        return x * x + y * y + z * z <= reach * reach;
    }

    /** Jitter the pressure center on each impact; preserve the full stage footprint. */
    public static double erosionChance(int stage, int x, int y, int z,
            double driftX, double driftY, double driftZ) {
        if (!contains(stage, x, y, z)) return 0.0D;
        double reach = radius(stage) + 0.75D;
        double dx = x - driftX, dy = y - driftY, dz = z - driftZ;
        double falloff = (dx * dx + dy * dy + dz * dz) / (reach * reach);
        return Math.max(0.08D, Math.min(0.90D, 0.90D - falloff * 0.70D));
    }

    /** Water spreading across the bed under a 3x3 forge, not through its bricks. */
    public static double forgeErosionChance(int stage, int x, int y, int z, double driftX, double driftZ) {
        int radius = radius(stage) + 1;
        if (Math.abs(y) > 1 || Math.abs(x) > radius || Math.abs(z) > radius) return 0.0D;
        double dx = x - driftX, dz = z - driftZ;
        double fraction = (dx * dx + dz * dz) / ((radius + 0.5D) * (radius + 0.5D));
        if (fraction > 1.0D) return 0.0D;
        return (0.65D - fraction * 0.50D) * (y == 0 ? 1.0D : 0.65D);
    }

    private SeaSerpentBreathArea() {}
}
