package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The client supplies bounded camera settings, never a victim, hit position or damage. */
public final class SeaSerpentAim {
    public static final double RANGE = 128.0D;
    private SeaSerpentAim() {}

    public static float maxDistance(float scale) { return 4.0F + Mth.clamp(scale, 1, 11) * 6.0F; }
    public static float maxHeight(float scale) { return Mth.clamp(scale, 1, 11) * 3.0F; }

    public static Vec3 origin(EntitySeaSerpent serpent, Player rider) {
        SeaSerpentBondData.RiderInput input = SeaSerpentBondData.getRiderInput(serpent);
        Vec3 eye = rider.getEyePosition();
        // Recheck against SERVER blocks and the current rider position/rotation.
        // No remote ray origins, stale world coordinates or camera-through-wall shots.
        double height = Mth.clamp(input.cameraHeight(), 0.0D, maxHeight(serpent.getSeaSerpentScale()));
        double distance = Mth.clamp(input.cameraDistance(), 0.0D, maxDistance(serpent.getSeaSerpentScale()));
        Vec3 pivot = clip(rider, eye, eye.add(0, height, 0));
        return clip(rider, pivot, pivot.subtract(rider.getLookAngle().scale(distance)));
    }

    private static Vec3 clip(Player rider, Vec3 from, Vec3 to) {
        HitResult hit = rider.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
        return hit.getType() == HitResult.Type.MISS ? to
                : hit.getLocation().subtract(to.subtract(from).normalize().scale(0.1D));
    }

    public static Vec3 target(EntitySeaSerpent serpent, Player rider) {
        Vec3 start = origin(serpent, rider);
        double reach = RANGE + start.distanceTo(rider.getEyePosition());
        Vec3 end = start.add(rider.getLookAngle().scale(reach));
        HitResult block = serpent.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        SeaSerpentCombat.TargetHit hit = SeaSerpentCombat.findTarget(serpent,
                start, end, 0.3D, start, reach + 0.5D, false);
        return hit != null && (block.getType() == HitResult.Type.MISS
                || start.distanceToSqr(hit.rayPoint()) < start.distanceToSqr(end)) ? hit.contact() : end;
    }
}
