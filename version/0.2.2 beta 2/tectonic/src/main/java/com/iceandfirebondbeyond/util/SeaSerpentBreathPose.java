package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Shared breathing bones: the renderer and server use the same jaw opening and muzzle. */
public final class SeaSerpentBreathPose {
    public static final String[] BONES = {"BodyUpper", "Neck1", "Neck2", "Neck3", "Head", "HeadFront", "Jaw"};
    // Native beta-5 bind pivots; BodyUpper includes the animator's +9 pixel offset.
    private static final float[][] PIVOTS = {{0,20,-10}, {0,.6F,1.1F}, {0,-.7F,-7.3F},
            {0,.3F,-7.5F}, {0,2.4F,-2.7F}, {0,-.7F,-3.9F}, {0,-.6F,-3.8F}};
    private SeaSerpentBreathPose() {}

    public static boolean active(EntitySeaSerpent serpent) {
        return !serpent.isJumpingOutOfWater() && !SeaSerpentBondData.isMountedBreachInProgress(serpent)
                && serpent.getAnimation() == EntitySeaSerpent.NO_ANIMATION
                && (serpent.isBreathing() || serpent.breathProgress > 0);
    }

    public static Pose of(EntitySeaSerpent serpent, float partial) {
        float bodyYaw = Mth.rotLerp(partial, serpent.yBodyRotO, serpent.yBodyRot);
        float bodyPitch = serpent.getViewXRot(partial);
        boolean mounted = serpent.getFirstPassenger() instanceof Player rider
                && SeaSerpentBondData.isOwner(serpent, rider.getUUID());
        Player rider = mounted ? (Player) serpent.getFirstPassenger() : null;
        float lookYaw = mounted ? rider.getViewYRot(partial) : Mth.rotLerp(partial, serpent.yHeadRotO, serpent.yHeadRot);
        float lookPitch = mounted ? rider.getViewXRot(partial) : bodyPitch;
        return new Pose(bodyYaw, bodyPitch, SeaSerpentRiderPose.lookYaw(lookYaw, bodyYaw),
                SeaSerpentRiderPose.lookPitch(lookPitch, bodyPitch),
                SeaSerpentRiderPose.surfaceLift(serpent, partial), Mth.clamp(serpent.breathProgress / 20, 0, 1), mounted);
    }

    public static Vec3 mouth(EntitySeaSerpent serpent, float partial) {
        return serpent.getPosition(partial).add(of(serpent, partial).lips(serpent.getSeaSerpentScale()).center());
    }

    public static float pivot(int bone, int axis) { return PIVOTS[bone][axis]; }
    public record Lips(Vec3 upper, Vec3 lower) {
        public Vec3 center() { return upper.lerp(lower, .5D); }
    }
    public record Pose(float bodyYaw, float bodyPitch, float turn, float bend, float lift, float open, boolean mounted) {
        public float yaw(int bone) {
            return switch (bone) {
                case 0 -> -10.43F * Mth.DEG_TO_RAD;
                case 1,4 -> turn * .20F;
                case 2,3 -> turn * .30F;
                default -> 0;
            };
        }
        public float pitch(int bone) {
            return switch (bone) {
                case 0 -> (2 - bodyPitch) * Mth.DEG_TO_RAD;
                case 1 -> bend * SeaSerpentRiderPose.NECK1_PITCH_SHARE + lift * SeaSerpentRiderPose.SURFACE_NECK1_PITCH;
                case 2 -> bend * SeaSerpentRiderPose.NECK2_PITCH_SHARE + lift * SeaSerpentRiderPose.SURFACE_NECK2_PITCH;
                case 3 -> bend * SeaSerpentRiderPose.NECK3_PITCH_SHARE + lift * SeaSerpentRiderPose.SURFACE_NECK3_PITCH;
                case 4 -> bend * SeaSerpentRiderPose.HEAD_PITCH_SHARE + lift * SeaSerpentRiderPose.SURFACE_HEAD_PITCH
                        - (mounted ? 0 : 15 * open * Mth.DEG_TO_RAD);
                case 5 -> (mounted ? .8F : Mth.lerp(open, .8F, -20)) * Mth.DEG_TO_RAD;
                case 6 -> 60 * open * Mth.DEG_TO_RAD;
                default -> 0;
            };
        }
        public Matrix4f joint(Matrix4f parent, int bone) {
            return parent.translate(pivot(bone,0)/16, pivot(bone,1)/16, pivot(bone,2)/16)
                    .rotateZYX(0, yaw(bone), pitch(bone));
        }
        public Lips lips(float scale) {
            Matrix4f head = new Matrix4f().rotateY((180 - bodyYaw) * Mth.DEG_TO_RAD)
                    .scale(-scale, -scale, scale).translate(0, -1.501F, 0);
            for (int bone=0;bone<=4;bone++) joint(head,bone);
            // Bottom of the upper lip and top of the lower lip, close to their tips.
            // Both points follow their OWN hinge; the stream begins between the jaws.
            return new Lips(point(joint(new Matrix4f(head),5), 0,.2F,-6.6F),
                    point(joint(new Matrix4f(head),6), 0,.1F,-6.6F));
        }
        private static Vec3 point(Matrix4f matrix, float x, float y, float z) {
            Vector3f p = matrix.transformPosition(new Vector3f(x/16,y/16,z/16));
            return new Vec3(p.x,p.y,p.z);
        }
    }
}
