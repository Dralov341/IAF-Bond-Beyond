package com.iceandfirebondbeyond.mixin;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.EntityMutlipartPart;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentBondGoals;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentBondLookControl;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentBondMoveControl;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentBondNavigation;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentControlAccess;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import com.iceandfirebondbeyond.util.SeaSerpentRiderPose;
import com.iceandfirebondbeyond.util.SeaSerpentDeath;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks the beta-5 entity without registering a replacement Sea Serpent type. */
@Mixin(EntitySeaSerpent.class)
public abstract class SeaSerpentMixin extends Animal implements SeaSerpentControlAccess {
    @Shadow(remap = false) private EntityMutlipartPart[] segments;
    @Unique private PathNavigation bondBeyond$nativeNavigation;
    @Unique private SeaSerpentBondNavigation bondBeyond$ownerNavigation;
    // Presentation state belongs to this entity instance, not persistent NBT
    // or a static map shared by the integrated server and render thread.
    @Unique private float bondBeyond$previousSurfacePose;
    @Unique private float bondBeyond$surfacePose;

    protected SeaSerpentMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    // EntitySeaSerpent inherits this method from LivingEntity. An override is
    // deliberate: injecting into a nonexistent IAF tickDeath would fail at load.
    @Override
    protected void tickDeath() {
        SeaSerpentDeath.tick((EntitySeaSerpent) (Object) this);
    }

    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void bondBeyond$stopDeadActions(CallbackInfo ci) {
        if (isDeadOrDying()) ci.cancel();
    }

    @Inject(method = "registerGoals", at = @At("RETURN"))
    private void bondBeyond$gateNativeGoals(CallbackInfo ci) {
        SeaSerpentBondGoals.install((EntitySeaSerpent) (Object) this);
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void bondBeyond$guardNativeBite(Entity target, CallbackInfoReturnable<Boolean> cir) {
        EntitySeaSerpent serpent = (EntitySeaSerpent) (Object) this;
        if (SeaSerpentBondData.hasRestrictedCombat(serpent)
                && (!(target instanceof LivingEntity living)
                || !SeaSerpentBondData.isAllowedCombatTarget(serpent, living))) {
            cir.setReturnValue(false);
        }
    }

    // These methods belong to IAF itself (not mapped Minecraft method names).
    // Guard the attack entry points too: a goal/direct call must not bypass
    // our target event or keep firing at an old victim after taming.
    @Inject(method = {"shoot", "hurtMob"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void bondBeyond$guardNativeAttack(LivingEntity target, CallbackInfo ci) {
        EntitySeaSerpent serpent = (EntitySeaSerpent) (Object) this;
        if (SeaSerpentBondData.hasRestrictedCombat(serpent)
                && !SeaSerpentBondData.isAllowedCombatTarget(serpent, target)) {
            SeaSerpentBondData.suppressCombat(serpent);
            ci.cancel();
        }
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void bondBeyond$finishOwnerControl(CallbackInfo ci) {
        EntitySeaSerpent serpent = (EntitySeaSerpent) (Object) this;
        if (isVehicle() && serpent.isBreathing() && !serpent.isJumpingOutOfWater()
                && !SeaSerpentBondData.isMountedBreachInProgress(serpent)
                && serpent.getAnimation() != EntitySeaSerpent.ANIMATION_BITE
                && serpent.getAnimation() != EntitySeaSerpent.ANIMATION_ROAR) {
            // Native pose, quicker wind-up for a key-held attack (10 ticks total).
            serpent.breathProgress = Math.min(20.0F, serpent.breathProgress + 1.5F);
        }
        if (level().isClientSide) return;
        if (moveControl instanceof SeaSerpentBondMoveControl control) control.finishControlledTick();
        if (isVehicle() && SeaSerpentBondData.isTamed(serpent)) {
            serpent.setBreathing(SeaSerpentBondData.shouldUseMountedBreath(serpent));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bondBeyond$finalHeading(CallbackInfo ci) {
        EntitySeaSerpent serpent = (EntitySeaSerpent) (Object) this;
        bondBeyond$previousSurfacePose = bondBeyond$surfacePose;
        bondBeyond$surfacePose = SeaSerpentRiderPose.advanceSurfacePose(
                bondBeyond$surfacePose, SeaSerpentRiderPose.isAtWaterSurface(serpent));
        if (!level().isClientSide) {
            if (moveControl instanceof SeaSerpentBondMoveControl control) control.finishControlledTick();
        } else if (SeaSerpentBondGoals.controlsMovement(serpent)
                && !SeaSerpentBondData.isMountedBreachInProgress(serpent)) {
            // Keep the displayed body on its tracked server heading. This is
            // visual rotation only; the client never runs our movement limiter.
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    @Override
    public float bondBeyond$getSurfacePose(float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F),
                bondBeyond$previousSurfacePose, bondBeyond$surfacePose);
    }

    // IAF's own methods keep their names. Minecraft overrides below use the
    // generated refmap so the same source works in runClient and a reobf jar.
    @Inject(method = "switchNavigator", at = @At("RETURN"), remap = false)
    private void bondBeyond$wrapNativeController(boolean onLand, CallbackInfo ci) {
        EntitySeaSerpent serpent = (EntitySeaSerpent) (Object) this;
        moveControl = new SeaSerpentBondMoveControl(serpent, moveControl);
        if (!(lookControl instanceof SeaSerpentBondLookControl)) {
            lookControl = new SeaSerpentBondLookControl(serpent, lookControl);
        }
        bondBeyond$nativeNavigation = navigation;
        bondBeyond$updateNavigation();
    }

    @Override
    public void bondBeyond$updateNavigation() {
        if (level().isClientSide || bondBeyond$nativeNavigation == null) {
            return;
        }
        EntitySeaSerpent serpent = (EntitySeaSerpent) (Object) this;
        PathNavigation wanted = bondBeyond$nativeNavigation;
        if (SeaSerpentBondGoals.controlsMovement(serpent)) {
            if (bondBeyond$ownerNavigation == null) {
                bondBeyond$ownerNavigation = new SeaSerpentBondNavigation(serpent, level());
            }
            wanted = bondBeyond$ownerNavigation;
        }
        if (navigation != wanted) {
            navigation.stop();
            wanted.stop();
            navigation = wanted;
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void bondBeyond$travelWithLimits(Vec3 input, CallbackInfo ci) {
        EntitySeaSerpent serpent = (EntitySeaSerpent) (Object) this;
        if (com.iceandfirebondbeyond.block.SeaSerpentForgeBlockEntity.isFueling(serpent)) {
            setDeltaMovement(Vec3.ZERO);
            ci.cancel();
            return;
        }
        if (level().isClientSide || !isEffectiveAi() || !isInWater()
                || !SeaSerpentBondData.wasHatched(serpent)
                || serpent.isJumpingOutOfWater()
                || SeaSerpentBondData.isMountedBreachInProgress(serpent)) {
            return;
        }
        // IAF's water travel order, with the clamp AFTER acceleration and BEFORE
        // collision-aware movement. The owner controller supplies braking;
        // unmounted AI retains IAF's drag/sinking. Only the server moves the mob.
        moveRelative(getSpeed(), input);
        SeaSerpentBondData.limitSwimMovementByStage(serpent);
        move(MoverType.SELF, getDeltaMovement());
        if (!SeaSerpentBondGoals.controlsMovement(serpent)) {
            setDeltaMovement(getDeltaMovement().scale(0.9D));
        }
        if (serpent.getTarget() == null && !SeaSerpentBondGoals.controlsMovement(serpent)) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.005D, 0.0D));
        }
        ci.cancel();
    }

    @Inject(method = "setBreathing", at = @At("HEAD"), cancellable = true, remap = false)
    private void bondBeyond$keepRiderBreath(boolean breathing, CallbackInfo ci) {
        if (isAlive() && !breathing && !level().isClientSide
                && (SeaSerpentBondData.shouldUseMountedBreath((EntitySeaSerpent) (Object) this)
                || com.iceandfirebondbeyond.block.SeaSerpentForgeBlockEntity.isFueling((EntitySeaSerpent) (Object) this))) {
            // Native aiStep would otherwise clear Breathing for a targetless
            // mount, preventing its synced Head/HeadFront/Jaw breath animation.
            ci.cancel();
        }
    }

    @Inject(method = "setAnimation", at = @At("HEAD"), cancellable = true, remap = false)
    private void bondBeyond$doNotRoarOverRiderBreath(Animation animation, CallbackInfo ci) {
        EntitySeaSerpent serpent = (EntitySeaSerpent) (Object) this;
        if (animation == EntitySeaSerpent.ANIMATION_ROAR
                && (SeaSerpentBondData.shouldUseMountedBreath(serpent)
                || com.iceandfirebondbeyond.block.SeaSerpentForgeBlockEntity.isFueling(serpent)
                || (level().isClientSide && isVehicle() && serpent.isBreathing()))) {
            ci.cancel();
        }
    }

    @Override
    public Vec3 bondBeyond$getMouthPosition() {
        EntitySeaSerpent serpent = (EntitySeaSerpent) (Object) this;
        if (segments == null || segments.length == 0 || segments[0] == null) {
            return getEyePosition();
        }
        double angle = Math.toRadians(getYRot() + 90.0F);
        double scale = serpent.getSeaSerpentScale();
        // Same mouth anchor as EntitySeaSerpent.shoot in beta-5.
        return segments[0].position().add(1.3D * scale * Math.cos(angle),
                0.2D * scale, 1.3D * scale * Math.sin(angle));
    }
}
