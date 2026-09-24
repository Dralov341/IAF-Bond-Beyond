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

    private SeaSerpentBreathArea() {}
}
