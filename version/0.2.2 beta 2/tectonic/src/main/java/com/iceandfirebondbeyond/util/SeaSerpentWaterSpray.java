package com.iceandfirebondbeyond.util;

import net.minecraft.world.phys.Vec3;

/** Soft, irregular spray. The main droplet keeps forge/reticle precision; its neighbors fill a cone. */
public final class SeaSerpentWaterSpray {
    private SeaSerpentWaterSpray() {}
    public static double maturity(int stage, float scale) {
        return .55D * (Math.max(1, Math.min(5, stage)) - 1) / 4
                + .45D * Math.max(0, Math.min(1, (scale - .8D) / 10.2D));
    }
    public static int bubbles(int stage, float scale, int maximum) {
        int cap = Math.max(4, Math.min(16, maximum));
        return (int) Math.round(4 + (cap - 4) * maturity(stage, scale));
    }
    public static int particles(int stage, float scale) {
        return (int) Math.round(12 + 28 * maturity(stage, scale));
    }
    public static double spread(int stage, float scale, double maximum) {
        return Math.max(3, Math.min(20, maximum)) * (.40D + .60D * maturity(stage, scale));
    }
    public static Vec3 direction(Vec3 aim, double degrees, int index, int count, double angleNoise, double radiusNoise) {
        Vec3 forward = aim.normalize();
        if (index == 0) return forward;
        Vec3 seed = Math.abs(forward.y) > .98 ? new Vec3(1,0,0) : new Vec3(0,1,0);
        Vec3 right = forward.cross(seed).normalize(), up = right.cross(forward).normalize();
        double angle = (index * 2.3999632297D) + angleNoise * Math.PI * 1.2D;
        // Spread over inner and outer lanes without a hard ring or a perfect fan edge.
        double radial = Math.sqrt((index - 1 + radiusNoise) / Math.max(1, count - 1));
        double lateral = Math.tan(Math.toRadians(degrees)) * radial;
        return forward.add(right.scale(Math.cos(angle)*lateral)).add(up.scale(Math.sin(angle)*lateral*.85D)).normalize();
    }
}
