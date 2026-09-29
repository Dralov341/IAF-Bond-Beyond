package com.iceandfirebondbeyond.mixin;

import com.github.alexthe666.citadel.client.model.TabulaModel;
import com.github.alexthe666.iceandfire.client.model.animator.SeaSerpentTabulaModelAnimator;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import com.iceandfirebondbeyond.util.SeaSerpentRiderPose;
import com.iceandfirebondbeyond.util.SeaSerpentBreathPose;
import com.iceandfirebondbeyond.client.SeaSerpentDeathClient;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Modify the native model before either the body or its armor layer is drawn. */
@Mixin(SeaSerpentTabulaModelAnimator.class)
public abstract class SeaSerpentAnimatorMixin {
    @Inject(method = "setRotationAngles(Lcom/github/alexthe666/citadel/client/model/TabulaModel;Lcom/github/alexthe666/iceandfire/entity/EntitySeaSerpent;FFFFFF)V",
            at = @At("TAIL"), remap = false)
    private void bondBeyond$mountedPose(TabulaModel model, EntitySeaSerpent serpent,
            float limbSwing, float limbSwingAmount, float age, float yaw, float pitch,
            float scale, CallbackInfo ci) {
        // Restore explicitly for the next serpent: the renderer shares its model.
        // The equipment layer now sheathes fin rays. Keep the native membranes
        // visible, including after another serpent without armor was rendered.
        model.getCube("BodyFinUpper").showModel = true;
        model.getCube("BodyFinLower").showModel = true;
        if (serpent.isJumpingOutOfWater()
                || SeaSerpentBondData.isMountedBreachInProgress(serpent)) {
            SeaSerpentDeathClient.rememberPose(model, serpent);
            return;
        }

        float partial = Minecraft.getInstance().getFrameTime();
        if (serpent.getFirstPassenger() instanceof Player rider
                && SeaSerpentBondData.isOwner(serpent, rider.getUUID())) {
            float bodyYaw = Mth.rotLerp(partial, serpent.yBodyRotO, serpent.yBodyRot);
            float yawRadians = SeaSerpentRiderPose.lookYaw(rider.getViewYRot(partial), bodyYaw);
            float pitchRadians = SeaSerpentRiderPose.lookPitch(rider.getViewXRot(partial), pitch);
            // R opens the native jaw without changing the aimed head/neck.
            model.getCube("Head").rotateAngleX = SeaSerpentRiderPose.headWithoutBreathPitch(
                    model.getCube("Head").rotateAngleX, model.getCube("Head").defaultRotationX,
                    serpent.breathProgress);
            model.getCube("HeadFront").rotateAngleX = SeaSerpentRiderPose.snoutWithoutBreathPitch(
                    model.getCube("HeadFront").rotateAngleX, model.getCube("HeadFront").defaultRotationX,
                    serpent.breathProgress);
            if (serpent.getAnimation() == EntitySeaSerpent.ANIMATION_BITE) {
                // Keep IAF's jaw keyframes. Its extra skull/neck wind-up would
                // pull an underwater bite away from the crosshair; our surface
                // lift already supplies the lowering/recovery when appropriate.
                model.getCube("Neck1").rotateAngleX = model.getCube("Neck1").defaultRotationX;
                model.getCube("Neck2").rotateAngleX = model.getCube("Neck2").defaultRotationX;
                model.getCube("Neck3").rotateAngleX = model.getCube("Neck3").defaultRotationX;
                model.getCube("Head").rotateAngleX = model.getCube("Head").defaultRotationX;
                model.getCube("HeadFront").rotateAngleX = model.getCube("HeadFront").defaultRotationX;
            }
            model.getCube("BodyUpper").rotateAngleY = model.getCube("BodyUpper").defaultRotationY;
            model.getCube("Neck1").rotateAngleY = model.getCube("Neck1").defaultRotationY + yawRadians * 0.20F;
            model.getCube("Neck2").rotateAngleY = model.getCube("Neck2").defaultRotationY + yawRadians * 0.30F;
            model.getCube("Neck3").rotateAngleY = model.getCube("Neck3").defaultRotationY + yawRadians * 0.30F;
            model.getCube("Head").rotateAngleY = model.getCube("Head").defaultRotationY + yawRadians * 0.20F;
            model.getCube("Neck1").rotateAngleX += pitchRadians * SeaSerpentRiderPose.NECK1_PITCH_SHARE;
            model.getCube("Neck2").rotateAngleX += pitchRadians * SeaSerpentRiderPose.NECK2_PITCH_SHARE;
            model.getCube("Neck3").rotateAngleX += pitchRadians * SeaSerpentRiderPose.NECK3_PITCH_SHARE;
            model.getCube("Head").rotateAngleX += pitchRadians * SeaSerpentRiderPose.HEAD_PITCH_SHARE;
        }
        // Applied before armor rendering: every attached plate inherits these
        // same animated bones. No separate armor transform or texture change.
        float lift = SeaSerpentRiderPose.surfaceLift(serpent, partial);
        model.getCube("Neck1").rotateAngleX += lift * SeaSerpentRiderPose.SURFACE_NECK1_PITCH;
        model.getCube("Neck2").rotateAngleX += lift * SeaSerpentRiderPose.SURFACE_NECK2_PITCH;
        model.getCube("Neck3").rotateAngleX += lift * SeaSerpentRiderPose.SURFACE_NECK3_PITCH;
        model.getCube("Head").rotateAngleX += lift * SeaSerpentRiderPose.SURFACE_HEAD_PITCH;
        if (SeaSerpentBreathPose.active(serpent)) {
            // Freeze only the breathing chain into the server's shared pose.
            // Tail, fins and all attached armor keep their native animation.
            SeaSerpentBreathPose.Pose breathing = SeaSerpentBreathPose.of(serpent, partial);
            for (int bone = 0; bone < SeaSerpentBreathPose.BONES.length; bone++) {
                var cube = model.getCube(SeaSerpentBreathPose.BONES[bone]);
                cube.rotationPointX = SeaSerpentBreathPose.pivot(bone, 0);
                cube.rotationPointY = SeaSerpentBreathPose.pivot(bone, 1);
                cube.rotationPointZ = SeaSerpentBreathPose.pivot(bone, 2);
                cube.rotateAngleX = breathing.pitch(bone);
                cube.rotateAngleY = breathing.yaw(bone);
                cube.rotateAngleZ = 0;
            }
        }
        SeaSerpentDeathClient.rememberPose(model, serpent);
    }
}
