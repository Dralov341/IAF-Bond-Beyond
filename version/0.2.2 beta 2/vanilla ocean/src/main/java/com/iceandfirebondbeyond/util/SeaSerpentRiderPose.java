package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentControlAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Shared mounted aiming geometry. Bind pivots come from beta-5's seaserpent_base.tbl. */
public final class SeaSerpentRiderPose {
    // Most of the vertical turn belongs to the long neck, not the skull.
    public static final float NECK1_PITCH_SHARE = 0.30F;
    public static final float NECK2_PITCH_SHARE = 0.35F;
    public static final float NECK3_PITCH_SHARE = 0.25F;
    public static final float HEAD_PITCH_SHARE = 0.10F;
    // Neck curve from beta-5's surface ROAR pose, with a neutral skull angle:
    // -31 -15 +28 +18 = 0. Lift the head's POSITION without stealing its aim.
    public static final float SURFACE_NECK1_PITCH = -31.0F * Mth.DEG_TO_RAD;
    public static final float SURFACE_NECK2_PITCH = -15.0F * Mth.DEG_TO_RAD;
    public static final float SURFACE_NECK3_PITCH = 28.0F * Mth.DEG_TO_RAD;
    public static final float SURFACE_HEAD_PITCH = 18.0F * Mth.DEG_TO_RAD;
    private static final float BODY_REST_PITCH = 2.0F;
    private static final float SNOUT_REST_PITCH = 0.8F;
    private static final float HEAD_BREATH_PITCH = -15.0F;
    private static final float SNOUT_BREATH_PITCH = -20.0F;
    private static final float MAX_NECK_BEND = 135.0F;

    private SeaSerpentRiderPose() {}

    /** Constant-cost, size-aware version of IAF's water-below / air-above check. */
    public static boolean isAtWaterSurface(EntitySeaSerpent serpent) {
        if (!SeaSerpentBondData.wasHatched(serpent) || !serpent.isAlive()
                || !serpent.isInWater() || serpent.isJumpingOutOfWater()
                || SeaSerpentBondData.isMountedBreachInProgress(serpent)) return false;
        BlockPos feet = serpent.blockPosition();
        BlockPos top = BlockPos.containing(serpent.getX(),
                serpent.getY() + Math.max(1.0D, serpent.getBbHeight()), serpent.getZ());
        return serpent.level().getBlockState(top).isAir()
                && (serpent.level().getFluidState(feet).is(FluidTags.WATER)
                || serpent.level().getFluidState(feet.below()).is(FluidTags.WATER));
    }

    public static float advanceSurfacePose(float previous, boolean atSurface) {
        // Half a second to raise/lower on surfacing/diving. This is visual only;
        // it never changes the mob's velocity, target, or synchronized breath flag.
        return Mth.clamp(previous + (atSurface ? 0.10F : -0.10F), 0.0F, 1.0F);
    }

    public static float surfaceLift(EntitySeaSerpent serpent, float partialTick) {
        if (!(serpent instanceof SeaSerpentControlAccess access)) return 0.0F;
        float lift = access.bondBeyond$getSurfacePose(partialTick);
        float time = serpent.getAnimationTick() + Mth.clamp(partialTick, 0.0F, 1.0F);
        if (serpent.getAnimation() == EntitySeaSerpent.ANIMATION_BITE) {
            // Lower through the first keyframe, hit at tick 6, then recover.
            // A submerged, straight-neck bite has lift=0 and never dips.
            lift *= restingLiftDuringAnimation(time, 5.0F, 10.0F, 15.0F);
        } else if (serpent.getAnimation() == EntitySeaSerpent.ANIMATION_ROAR) {
            // Let the existing native raised ROAR pose take over smoothly.
            lift *= restingLiftDuringAnimation(time, 10.0F, 30.0F, 40.0F);
        }
        return lift;
    }

    public static float restingLiftDuringAnimation(float tick, float lowerEnd,
            float recoverStart, float end) {
        if (tick < lowerEnd) return 1.0F - smoothUnit(tick / lowerEnd);
        return smoothUnit((tick - recoverStart) / (end - recoverStart));
    }

    private static float smoothUnit(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static float lookYaw(float riderYaw, float bodyYaw) {
        return Mth.clamp(Mth.wrapDegrees(riderYaw - bodyYaw), -75, 75) * Mth.DEG_TO_RAD;
    }

    public static float lookPitch(float riderPitch, float bodyPitch) {
        // The mounted animator removes ONLY the native breath's skull/snout
        // rotation. Keep the neck aim independent of R, including its fade-out:
        // compensating for breath here would make the neck dip when firing.
        float nativePitch = BODY_REST_PITCH - bodyPitch + SNOUT_REST_PITCH;
        float correction = Mth.clamp(riderPitch, -90, 90) - nativePitch;
        return Mth.clamp(correction, -MAX_NECK_BEND, MAX_NECK_BEND) * Mth.DEG_TO_RAD;
    }

    public static float headWithoutBreathPitch(float animatedPitch, float defaultPitch, float progress) {
        return animatedPitch - Mth.clamp(progress / 20.0F, 0, 1)
                * (HEAD_BREATH_PITCH * Mth.DEG_TO_RAD - defaultPitch);
    }

    public static float snoutWithoutBreathPitch(float animatedPitch, float defaultPitch, float progress) {
        return animatedPitch - Mth.clamp(progress / 20.0F, 0, 1)
                * (SNOUT_BREATH_PITCH * Mth.DEG_TO_RAD - defaultPitch);
    }

    /** Server-side mouth uses the same R-independent neck/skull aim as the renderer. */
    public static Vec3 mouth(EntitySeaSerpent serpent, Player rider) {
        return traceNeck(serpent, rider, null);
    }

    /** BodyUpper's rear face, then each posed neck/head centre, ending at the muzzle. */
    public static Vec3[] biteAnchors(EntitySeaSerpent serpent, Player rider) {
        Vec3[] anchors = new Vec3[6];
        traceNeck(serpent, rider, anchors);
        return anchors;
    }

    private static Vec3 traceNeck(EntitySeaSerpent serpent, Player rider, Vec3[] anchors) {
        float scale = serpent.getSeaSerpentScale();
        float yaw = lookYaw(rider.getYRot(), serpent.yBodyRot);
        float pitch = lookPitch(rider.getXRot(), serpent.getXRot());
        float lift = surfaceLift(serpent, 1.0F);
        // LivingEntityRenderer transform, including IAF's +9 model-pixel correction.
        Matrix4f pose = new Matrix4f().rotateY((180 - serpent.yBodyRot) * Mth.DEG_TO_RAD)
                .scale(-scale, -scale, scale).translate(0, -1.501F, 0)
                .translate(0, 20.0F / 16, -10.0F / 16)
                .rotateZYX(0, -10.43F * Mth.DEG_TO_RAD,
                        (BODY_REST_PITCH - serpent.getXRot()) * Mth.DEG_TO_RAD);
        if (anchors != null) anchors[0] = point(serpent, pose, 0, 1.2F, 7.0F);
        joint(pose, 0, 0.6F, 1.1F, yaw * 0.20F,
                pitch * NECK1_PITCH_SHARE + lift * SURFACE_NECK1_PITCH);
        if (anchors != null) anchors[1] = point(serpent, pose, 0, 0.5F, -3.0F);
        joint(pose, 0, -0.7F, -7.3F, yaw * 0.30F,
                pitch * NECK2_PITCH_SHARE + lift * SURFACE_NECK2_PITCH);
        if (anchors != null) anchors[2] = point(serpent, pose, 0, 1.2F, -3.0F);
        joint(pose, 0, 0.3F, -7.5F, yaw * 0.30F,
                pitch * NECK3_PITCH_SHARE + lift * SURFACE_NECK3_PITCH);
        if (anchors != null) anchors[3] = point(serpent, pose, 0, 0.9F, -1.91F);
        joint(pose, 0, 2.4F, -2.7F, yaw * 0.20F,
                pitch * HEAD_PITCH_SHARE + lift * SURFACE_HEAD_PITCH);
        if (anchors != null) anchors[4] = point(serpent, pose, 0, -1.5F, -2.1F);
        joint(pose, 0, -0.7F, -3.9F, 0, SNOUT_REST_PITCH * Mth.DEG_TO_RAD);
        Vec3 mouth = point(serpent, pose, 0, -1.0F, -7.0F);
        if (anchors != null) anchors[5] = mouth;
        return mouth;
    }

    private static Vec3 point(EntitySeaSerpent serpent, Matrix4f pose, float x, float y, float z) {
        Vector3f point = pose.transformPosition(new Vector3f(x / 16, y / 16, z / 16));
        return serpent.position().add(point.x, point.y, point.z);
    }

    private static void joint(Matrix4f pose, float x, float y, float z, float yaw, float pitch) {
        pose.translate(x / 16, y / 16, z / 16).rotateZYX(0, yaw, pitch);
    }
}
