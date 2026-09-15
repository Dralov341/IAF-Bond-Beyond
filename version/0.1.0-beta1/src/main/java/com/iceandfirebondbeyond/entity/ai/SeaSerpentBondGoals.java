package com.iceandfirebondbeyond.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;

public final class SeaSerpentBondGoals {
    private SeaSerpentBondGoals() {}

    public static boolean controlsMovement(EntitySeaSerpent serpent) {
        if (!SeaSerpentBondData.isTamed(serpent)) {
            return false;
        }
        if (serpent.isVehicle() || SeaSerpentBondData.isMountedBreachInProgress(serpent)) {
            return true;
        }
        int command = SeaSerpentBondData.getCommand(serpent);
        return command == SeaSerpentBondData.COMMAND_SIT
                || command == SeaSerpentBondData.COMMAND_ESCORT;
    }

    public static void install(EntitySeaSerpent serpent) {
        if (serpent.goalSelector.getAvailableGoals().stream()
                .anyMatch(entry -> entry.getGoal() instanceof OwnerControlGoal)) {
            return;
        }
        for (WrappedGoal entry : new ArrayList<>(serpent.goalSelector.getAvailableGoals())) {
            Goal original = entry.getGoal();
            // Some native jump goals have no flags; gate those as well.
            serpent.goalSelector.removeGoal(original);
            serpent.goalSelector.addGoal(entry.getPriority(),
                    new NativeGoalGate(serpent, original, false));
        }
        for (WrappedGoal entry : new ArrayList<>(serpent.targetSelector.getAvailableGoals())) {
            serpent.targetSelector.removeGoal(entry.getGoal());
            serpent.targetSelector.addGoal(entry.getPriority(),
                    new NativeGoalGate(serpent, entry.getGoal(), true));
        }
        serpent.goalSelector.addGoal(0, new OwnerControlGoal(serpent));
    }

    /** Gate continuation as well as activation, including uninterruptible jump goals. */
    private static final class NativeGoalGate extends Goal {
        private final EntitySeaSerpent serpent;
        private final Goal original;
        private final boolean targetGoal;

        private NativeGoalGate(EntitySeaSerpent serpent, Goal original, boolean targetGoal) {
            this.serpent = serpent;
            this.original = original;
            this.targetGoal = targetGoal;
            setFlags(original.getFlags());
        }

        private boolean permitted() {
            return targetGoal
                    ? !SeaSerpentBondData.isTamed(serpent) && !SeaSerpentBondData.isNewborn(serpent)
                    : !controlsMovement(serpent);
        }

        @Override public boolean canUse() { return permitted() && original.canUse(); }
        @Override public boolean canContinueToUse() { return permitted() && original.canContinueToUse(); }
        @Override public boolean isInterruptable() { return original.isInterruptable(); }
        @Override public boolean requiresUpdateEveryTick() { return original.requiresUpdateEveryTick(); }
        @Override public void start() { original.start(); }
        @Override public void tick() { if (permitted()) original.tick(); }
        @Override public void stop() { original.stop(); }
    }

    private static final class OwnerControlGoal extends Goal {
        private final EntitySeaSerpent serpent;
        private long nextPathTick;
        private long nextCourseTick;
        private Vec3 checkedDestination = Vec3.ZERO;
        private boolean courseClear;
        private boolean following;

        private OwnerControlGoal(EntitySeaSerpent serpent) {
            this.serpent = serpent;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override public boolean canUse() { return controlsMovement(serpent); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            following = false;
            nextPathTick = serpent.level().getGameTime();
            nextCourseTick = nextPathTick;
            courseClear = false;
            serpent.getNavigation().stop();
        }

        @Override public void stop() {
            serpent.getNavigation().stop();
            if (serpent.getMoveControl() instanceof SeaSerpentBondMoveControl control) {
                control.holdPosition();
            }
        }

        @Override public void tick() {
            if (!(serpent.getMoveControl() instanceof SeaSerpentBondMoveControl control)) {
                return;
            }
            if (serpent.isVehicle() || SeaSerpentBondData.isMountedBreachInProgress(serpent)) {
                serpent.getNavigation().stop();
                return;
            }
            Player owner = SeaSerpentBondData.getOwner(serpent);
            if (SeaSerpentBondData.getCommand(serpent) != SeaSerpentBondData.COMMAND_ESCORT
                    || owner == null || !owner.isAlive() || owner.isSpectator()) {
                serpent.getNavigation().stop();
                control.holdPosition();
                following = false;
                return;
            }

            Vec3 destination = owner.position();
            double distance = serpent.position().distanceTo(destination);
            double stopDistance = Math.max(2.5D, serpent.getBbWidth() * 0.65D + 1.0D);
            if (distance <= stopDistance + 0.2D) {
                following = false;
            } else if (distance > stopDistance + 1.0D) {
                following = true;
            }
            if (!following) {
                serpent.getNavigation().stop();
                control.holdPosition();
                return;
            }

            // Direct swimming is allowed only through a clear, loaded corridor.
            // Blocked routes go through amphibious pathfinding, never a wall push.
            long now = serpent.level().getGameTime();
            if (serpent.isInWater() && (now >= nextCourseTick
                    || checkedDestination.distanceToSqr(destination) > 4.0D)) {
                nextCourseTick = now + 5L;
                checkedDestination = destination;
                courseClear = clearWaterCourse(destination);
            }
            if (serpent.isInWater() && courseClear) {
                serpent.getNavigation().stop();
                control.followDirectly(destination, stopDistance);
                return;
            }
            control.followPath(stopDistance, destination);
            if (now >= nextPathTick) {
                nextPathTick = now + 10L; // Applies even after a failed search.
                serpent.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0D);
            }
        }

        private boolean clearWaterCourse(Vec3 destination) {
            Vec3 delta = destination.subtract(serpent.position());
            int steps = (int) Math.ceil(delta.length());
            if (steps > 32) {
                return false;
            }
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = 1; i <= steps; i++) {
                Vec3 sample = serpent.position().add(delta.scale(i / (double) steps));
                pos.set(sample.x, sample.y, sample.z);
                if (!serpent.level().hasChunkAt(pos)) {
                    return false;
                }
                if (!serpent.level().getFluidState(pos).is(FluidTags.WATER)) {
                    // Permit the last two blocks above water to follow a boat
                    // or climb a shore. No ascending through a column of air.
                    if (steps - i > 2 || !serpent.level().getFluidState(pos.below()).is(FluidTags.WATER)) {
                        return false;
                    }
                }
            }
            return serpent.level().noCollision(serpent,
                    serpent.getBoundingBox().expandTowards(delta));
        }
    }
}
