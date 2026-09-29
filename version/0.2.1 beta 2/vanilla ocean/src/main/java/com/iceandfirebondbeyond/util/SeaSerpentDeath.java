package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.entity.SeaSerpentCorpseEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;

/** The living entity finishes its fall before becoming a saved harvestable body. */
public final class SeaSerpentDeath {
    public static final int DURATION = 50;
    public static final String PENDING = "BondBeyondCorpseCreated";
    private static final String YAW = "BondBeyondDeathYaw";

    public static void tick(EntitySeaSerpent serpent) {
        serpent.deathTime++;
        if (!serpent.getPersistentData().contains(YAW)) serpent.getPersistentData().putFloat(YAW, serpent.yBodyRot);
        float yaw = serpent.getPersistentData().getFloat(YAW);
        serpent.setYRot(yaw);
        serpent.yBodyRot = serpent.yBodyRotO = serpent.yHeadRot = serpent.yHeadRotO = yaw;
        if (serpent.level().isClientSide) return;
        serpent.ejectPassengers();
        serpent.setTarget(null);
        serpent.getNavigation().stop();
        serpent.setBreathing(false);
        serpent.setJumpingOutOfWater(false);
        sink(serpent);
        if (serpent.deathTime >= DURATION) {
            SeaSerpentCorpseEntity body = SeaSerpentCorpseEntity.from(serpent);
            if (serpent.level().addFreshEntity(body)) serpent.discard();
        }
    }

    public static void sink(Entity entity) {
        Vec3 movement = entity.getDeltaMovement();
        double fall = entity.isInWater() ? Math.max(-.10, movement.y - .012) : Math.max(-.7, movement.y - .05);
        entity.setDeltaMovement(movement.x * .8, fall, movement.z * .8);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        if (entity.onGround()) entity.setDeltaMovement(Vec3.ZERO);
    }

    public static float progress(float ticks) {
        float t = Math.max(0, Math.min(1, ticks / DURATION));
        return t * t * (3 - 2 * t);
    }

    public static Vec3 worldPoint(Entity entity, float scale, float yaw, double[] point) {
        double angle = Math.toRadians(yaw), c = Math.cos(angle), s = Math.sin(angle), unit = scale / 16.0;
        return entity.position().add((c * point[0] + s * point[2]) * unit,
                (SeaSerpentRemainsPose.MODEL_ORIGIN_Y - point[1]) * unit,
                (s * point[0] - c * point[2]) * unit);
    }

    public static SeaSerpentRemainsPose pose(Entity entity, float scale, float yaw, float curl, float settling) {
        Map<Long, Double> sampled = new HashMap<>();
        return SeaSerpentRemainsPose.create(curl, settling, (x, z) -> {
            Vec3 point = worldPoint(entity, scale, yaw, new double[]{x, 0, z});
            // Nearby IK iterations share a quarter-block support sample; the
            // cache lives only for this solve, so edited terrain is rechecked.
            long key = ((long) Math.floor(point.x * 4) << 32) ^ ((long) Math.floor(point.z * 4) & 0xffffffffL);
            Double cached = sampled.get(key);
            if (cached != null) return cached;
            if (!entity.level().hasChunkAt(BlockPos.containing(point.x, entity.getY(), point.z))) return Double.NaN;
            Vec3 from = new Vec3(point.x, entity.getY() + Math.max(1.5, scale * 1.5), point.z);
            Vec3 to = new Vec3(point.x, entity.getY() - Math.max(5, scale * 4), point.z);
            var hit = entity.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            double height = hit.getType() == HitResult.Type.MISS ? Double.NaN : (hit.getLocation().y - entity.getY()) * 16 / scale;
            sampled.put(key, height);
            return height;
        });
    }
    private SeaSerpentDeath() {}
}
