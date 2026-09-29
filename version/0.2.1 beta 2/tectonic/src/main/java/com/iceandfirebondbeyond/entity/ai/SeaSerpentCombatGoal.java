package com.iceandfirebondbeyond.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import com.iceandfirebondbeyond.entity.SeaSerpentRiderBubbleEntity;
import com.iceandfirebondbeyond.util.SeaSerpentAttackCycle;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import com.iceandfirebondbeyond.util.SeaSerpentCombat;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Shared wild/AI-tamed pursuit, animation-timed bite and fixed bubble breath. */
public final class SeaSerpentCombatGoal extends Goal {
    private final EntitySeaSerpent serpent;
    private final SeaSerpentAttackCycle cycle = new SeaSerpentAttackCycle();
    private LivingEntity biteTarget;
    private long nextPath;
    private long nextCourseCheck;
    private long nextBreach;
    private long breachStarted;
    private boolean clearCourse;
    private boolean breaching;
    private boolean airborne;
    private Vec3 courseTarget = Vec3.ZERO;

    public SeaSerpentCombatGoal(EntitySeaSerpent serpent) {
        this.serpent = serpent;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private boolean allowed() {
        return serpent.isAlive() && !serpent.isNoAi() && !serpent.isVehicle()
                && !serpent.isPassenger() && !SeaSerpentBondGoals.controlsMovement(serpent);
    }

    @Override public boolean canUse() {
        LivingEntity target = serpent.getTarget();
        return allowed() && SeaSerpentCombat.canUseAi(serpent, target)
                && serpent.isWithinRestriction(target.blockPosition());
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { nextPath = nextCourseCheck = 0; clearCourse = false; }

    @Override public void stop() {
        cycle.stop();
        biteTarget = null;
        stopMovement();
        serpent.setBreathing(false);
    }

    private void stopMovement() {
        serpent.getNavigation().stop();
        if (serpent.getMoveControl() instanceof SeaSerpentBondMoveControl control) control.clearCombatCourse();
    }

    @Override public void tick() {
        LivingEntity target = serpent.getTarget();
        if (!canUse() || target == null) return;
        long now = serpent.level().getGameTime();
        serpent.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (breaching) { stopMovement(); return; }
        if (tryBreach(target, now)) return;
        if (SeaSerpentCombat.inBiteRange(serpent, target)) {
            stopMovement();
            tryBite(target);
            return;
        }
        Vec3 destination = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        if (now >= nextCourseCheck || courseTarget.distanceToSqr(destination) > 4.0D) {
            nextCourseCheck = now + 5L;
            courseTarget = destination;
            clearCourse = clearWaterCourse(destination);
        }
        if (clearCourse && serpent.isInWater()
                && serpent.getMoveControl() instanceof SeaSerpentBondMoveControl control) {
            serpent.getNavigation().stop();
            // Stop the body before its long neck passes the victim.
            control.followCombatCourse(destination, 1.0D + serpent.getSeaSerpentScale() * 2.0D);
        } else {
            if (serpent.getMoveControl() instanceof SeaSerpentBondMoveControl control) control.clearCombatCourse();
            if (now >= nextPath) {
                boolean found = serpent.getNavigation().moveTo(target, 1.0D);
                nextPath = now + (found ? 10L : 20L);
            }
        }
    }

    public boolean tryBite(LivingEntity target) {
        if (!allowed() || !SeaSerpentCombat.canUseAi(serpent, target)
                || serpent.getAnimation() != EntitySeaSerpent.NO_ANIMATION
                || !SeaSerpentCombat.inBiteRange(serpent, target)
                || !cycle.startBite(serpent.level().getGameTime())) return false;
        biteTarget = target;
        serpent.setBreathing(false);
        serpent.setAnimation(EntitySeaSerpent.ANIMATION_BITE);
        return true;
    }

    /** Called after native aiStep, so native attackDecision cannot override our mode. */
    public void finishTick() {
        if (!allowed()) {
            cycle.stop(); biteTarget = null; breaching = airborne = false;
            return;
        }
        finishBreach();
        LivingEntity target = serpent.getTarget();
        if (!SeaSerpentCombat.canUseAi(serpent, target)) {
            cycle.stop(); biteTarget = null; serpent.setBreathing(false);
            return;
        }
        if (serpent.getAnimation() != EntitySeaSerpent.ANIMATION_BITE) {
            cycle.cancelBite(); biteTarget = null;
        } else if (cycle.consumeBite(serpent.getAnimationTick())) {
            // Keep the victim selected at wind-up; changing targets cannot transfer a bite.
            if (biteTarget == target) SeaSerpentCombat.bite(serpent, biteTarget);
            biteTarget = null;
        }
        Vec3 mouth = SeaSerpentBondData.getMouthPosition(serpent);
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        double distance = mouth.distanceTo(SeaSerpentCombat.closestPoint(target.getBoundingBox(), mouth));
        boolean wasBreathing = cycle.isBreathing();
        boolean breath = cycle.updateBreath(!breaching && serpent.isInWater() && target.isInWater()
                        && !serpent.isJumpingOutOfWater() && serpent.hasLineOfSight(target)
                        && serpent.getAnimation() == EntitySeaSerpent.NO_ANIMATION && distance <= 128.0D,
                distance, 1.25D + serpent.getSeaSerpentScale() * 0.5D);
        serpent.attackDecision = !breath;
        serpent.setBreathing(breath);
        if (breath && (!wasBreathing || serpent.tickCount % 40 == 0)) serpent.playSound(IafSoundRegistry.SEA_SERPENT_BREATH, 4, 1);
        if (cycle.takeShot(serpent.level().getGameTime(), SeaSerpentBondData.getMountedBreathIntervalTicks(serpent))) {
            SeaSerpentRiderBubbleEntity.fireAt(serpent, aim);
        }
    }

    public boolean isBreathing() { return cycle.isBreathing() && allowed(); }

    private boolean tryBreach(LivingEntity target, long now) {
        if (now < nextBreach || !serpent.isInWater() || !serpent.shouldUseJumpAttack(target)
                || serpent.getAnimation() != EntitySeaSerpent.NO_ANIMATION
                || !serpent.hasLineOfSight(target) || SeaSerpentCombat.inBiteRange(serpent, target)
                || serpent.distanceToSqr(target) > 144.0D) return false;
        BlockPos above = serpent.blockPosition().above(2);
        if (!serpent.level().hasChunkAt(above) || !serpent.level().getBlockState(above).isAir()
                || !serpent.level().noCollision(serpent, serpent.getBoundingBox().move(0, 2, 0))) return false;
        stopMovement();
        cycle.stop();
        serpent.setBreathing(false);
        Vec3 offset = target.position().subtract(serpent.position());
        Vec3 horizontal = new Vec3(offset.x, 0, offset.z).normalize().scale(Math.min(0.8D, offset.horizontalDistance() * 0.09D));
        serpent.setDeltaMovement(horizontal.add(0, 1.0D + serpent.getRandom().nextFloat() * 0.4D, 0));
        serpent.setJumpingOutOfWater(true);
        serpent.jumpCooldown = 40;
        nextBreach = now + SeaSerpentBondData.getMountedSlamCooldownTicks(serpent);
        breachStarted = now;
        breaching = true;
        airborne = false;
        return true;
    }

    private void finishBreach() {
        if (!breaching) return;
        if (!serpent.isInWater()) airborne = true;
        if (airborne && (serpent.isInWater() || serpent.onGround())) {
            SeaSerpentBondData.performSlamImpact(serpent, serpent.isInWater());
            serpent.setJumpingOutOfWater(false);
            breaching = airborne = false;
        } else if (serpent.level().getGameTime() - breachStarted > 100L) {
            serpent.setJumpingOutOfWater(false);
            breaching = airborne = false;
        } else if (airborne && serpent.getTarget() != null) {
            tryBite(serpent.getTarget());
        }
    }

    private boolean clearWaterCourse(Vec3 destination) {
        if (!serpent.isInWater()) return false;
        Vec3 delta = destination.subtract(serpent.position());
        int steps = (int) Math.ceil(delta.length());
        if (steps > 32) return false;
        for (int i = 1; i <= steps; i++) {
            BlockPos pos = BlockPos.containing(serpent.position().add(delta.scale(i / (double) steps)));
            if (!serpent.level().hasChunkAt(pos) || !serpent.level().getFluidState(pos).is(FluidTags.WATER)) return false;
        }
        return serpent.level().noCollision(serpent, serpent.getBoundingBox().expandTowards(delta));
    }
}
