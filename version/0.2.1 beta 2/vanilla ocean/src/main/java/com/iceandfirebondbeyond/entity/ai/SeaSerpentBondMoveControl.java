package com.iceandfirebondbeyond.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.block.SeaSerpentForgeBlockEntity;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** One movement authority; IAF's original helper remains active for wild/combat AI. */
public final class SeaSerpentBondMoveControl extends MoveControl {
    private final EntitySeaSerpent serpent;
    private final MoveControl nativeControl;
    @Nullable private Vec3 directDestination;
    @Nullable private Vec3 ownerDestination;
    @Nullable private Vec3 combatDestination;
    private double combatStopDistance;
    private double stopDistance;
    private boolean followPath;
    // Snapshot before assigning the requested velocity, for acceleration limiting.
    private Vec3 previousMotion = Vec3.ZERO;
    private float controlledYaw;
    private boolean wasControlling;

    public SeaSerpentBondMoveControl(EntitySeaSerpent serpent, MoveControl nativeControl) {
        super(serpent);
        this.serpent = serpent;
        this.nativeControl = nativeControl;
        resetHeading();
    }

    public void resetHeading() {
        controlledYaw = serpent.getYRot();
    }

    /** Native aiStep/body-look logic runs after MoveControl; restore its final heading. */
    public void finishControlledTick() {
        if (!SeaSerpentBondGoals.controlsMovement(serpent)
                || SeaSerpentBondData.isMountedBreachInProgress(serpent)) return;
        serpent.setYRot(controlledYaw);
        serpent.yBodyRot = controlledYaw;
        serpent.yHeadRot = serpent.getFirstPassenger() instanceof Player rider
                ? controlledYaw + Mth.clamp(Mth.wrapDegrees(rider.getYRot() - controlledYaw), -75.0F, 75.0F)
                : controlledYaw;
    }

    @Override
    public void setWantedPosition(double x, double y, double z, double speed) {
        super.setWantedPosition(x, y, z, speed);
        nativeControl.setWantedPosition(x, y, z, speed);
    }

    @Override
    public void strafe(float forward, float sideways) {
        super.strafe(forward, sideways);
        nativeControl.strafe(forward, sideways);
    }

    @Override
    public boolean hasWanted() {
        return SeaSerpentBondGoals.controlsMovement(serpent)
                ? super.hasWanted() : nativeControl.hasWanted();
    }

    public void holdPosition() {
        combatDestination = null;
        directDestination = null;
        ownerDestination = null;
        followPath = false;
        operation = Operation.WAIT;
    }

    public void followDirectly(Vec3 destination, double radius) {
        directDestination = destination;
        ownerDestination = destination;
        stopDistance = radius;
        followPath = false;
    }

    public void followPath(double radius, Vec3 destination) {
        directDestination = null;
        ownerDestination = destination;
        stopDistance = radius;
        followPath = true;
    }

    public void followCombatCourse(Vec3 destination, double stopRadius) {
        combatDestination = destination;
        combatStopDistance = stopRadius;
    }

    public void clearCombatCourse() { combatDestination = null; }

    private void swimCombatCourse() {
        Vec3 offset = combatDestination.subtract(serpent.position());
        double remaining = Math.max(0.0D, offset.length() - combatStopDistance);
        SeaSerpentBondData.MovementLimits limits = SeaSerpentBondData.getMovementLimits(serpent);
        Vec3 requested = offset.normalize().scale(Math.min(limits.maxSpeed(), remaining * 0.18D));
        float desiredYaw = (float) Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0F;
        serpent.setYRot(serpent.getYRot() + Mth.clamp(Mth.wrapDegrees(desiredYaw - serpent.getYRot()), -10.0F, 10.0F));
        serpent.yBodyRot = serpent.yHeadRot = serpent.getYRot();
        serpent.setSpeed(0);
        serpent.setZza(0);
        serpent.setXxa(0);
        serpent.setYya(0);
        serpent.setDeltaMovement(SeaSerpentBondData.limitMotion(serpent.getDeltaMovement(), requested, limits));
    }

    @Override
    public void tick() {
        if (!SeaSerpentBondGoals.controlsMovement(serpent)) {
            wasControlling = false;
            if (combatDestination != null && serpent.isInWater() && !serpent.isJumpingOutOfWater()) {
                swimCombatCourse();
                return;
            }
            nativeControl.tick();
            return;
        }
        combatDestination = null;
        if (!wasControlling) {
            resetHeading();
            wasControlling = true;
        }
        previousMotion = serpent.getDeltaMovement();
        serpent.setSpeed(0.0F);
        serpent.setZza(0.0F);
        serpent.setXxa(0.0F);
        serpent.setYya(0.0F);
        if (SeaSerpentBondData.isMountedBreachInProgress(serpent)) {
            return; // The locked slam trajectory owns velocity until impact.
        }
        serpent.setJumpingOutOfWater(false);
        if (SeaSerpentForgeBlockEntity.isFueling(serpent)) {
            holdPosition();
            serpent.getNavigation().stop();
            Vec3 target = SeaSerpentForgeBlockEntity.fuelingTarget(serpent);
            if (target != null) faceMovement(target.subtract(serpent.position()));
            serpent.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (serpent.getFirstPassenger() instanceof Player rider) {
            if (SeaSerpentBondData.canBeRidden(serpent)
                    && SeaSerpentBondData.isOwner(serpent, rider.getUUID())) {
                steerRider(rider);
            }
            return;
        }

        Vec3 waypoint = directDestination;
        if (followPath && !serpent.getNavigation().isDone() && operation == Operation.MOVE_TO) {
            waypoint = new Vec3(wantedX, wantedY, wantedZ);
        }
        if (!serpent.isInWater() && waypoint != null) {
            nativeControl.tick(); // Ground movement retains stepping and gravity.
            controlledYaw = serpent.getYRot();
            return;
        }
        Vec3 requested = Vec3.ZERO;
        if (waypoint != null) {
            Vec3 offset = waypoint.subtract(serpent.position());
            double remaining = ownerDestination == null ? offset.length()
                    : Math.max(0.0D, serpent.position().distanceTo(ownerDestination) - stopDistance);
            double speed = Math.min(SeaSerpentBondData.getMovementLimits(serpent).maxSpeed(), remaining * 0.20D);
            // Slow for nearby path corners too, avoiding overshoot at high stages.
            speed = Math.min(speed, offset.length() * 0.35D);
            requested = offset.normalize().scale(speed);
            faceMovement(requested);
        }
        if (!serpent.isInWater()) {
            requested = new Vec3(0.0D, previousMotion.y, 0.0D);
        }
        serpent.setDeltaMovement(requested);
    }

    private void steerRider(Player rider) {
        SeaSerpentBondData.RiderInput input = SeaSerpentBondData.getRiderInput(serpent);
        float forwardInput = input.forward();
        if (forwardInput < 0) {
            forwardInput *= 0.35F;
        }
        double yaw = Math.toRadians(rider.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 requested = forward.scale(forwardInput).add(right.scale(input.strafe() * 0.5D));
        if (serpent.isInWater()) {
            int vertical = (input.up() ? 1 : 0) - (input.down() ? 1 : 0);
            if (vertical != 0) {
                // Rise/dive diagonally even without W. Opposite vertical keys cancel.
                if (requested.horizontalDistanceSqr() < 0.01D) {
                    requested = forward.scale(0.65D);
                }
                requested = requested.add(0.0D, vertical * 0.65D, 0.0D);
            } else if (Math.abs(forwardInput) > 0.01F) {
                double pitch = Math.toRadians(Mth.clamp(rider.getXRot(), -45.0F, 45.0F));
                requested = requested.add(0.0D, -Math.sin(pitch) * forwardInput, 0.0D);
            }
        }
        if (requested.lengthSqr() > 1.0D) {
            requested = requested.normalize();
        }
        double speed = SeaSerpentBondData.getMovementLimits(serpent).maxSpeed();
        if (!serpent.isInWater()) {
            speed = Math.min(0.24D, speed * 0.5D);
        }
        requested = requested.scale(speed);
        if (!serpent.isInWater()) {
            requested = SeaSerpentBondData.limitMotion(previousMotion,
                    new Vec3(requested.x, previousMotion.y, requested.z),
                    new SeaSerpentBondData.MovementLimits(Math.max(speed, previousMotion.length()), 0.025D));
        }
        turnToward(rider.getYRot(), 7.0F);
        finishControlledTick();
        serpent.setDeltaMovement(requested);
    }

    private void faceMovement(Vec3 movement) {
        if (movement.horizontalDistanceSqr() < 1.0E-5D) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(movement.z, movement.x)) - 90.0F;
        turnToward(yaw, 12.0F);
        finishControlledTick();
    }

    private void turnToward(float yaw, float maxTurn) {
        // Keep yaw continuous across north; IAF's tail history subtracts angles
        // directly, so wrapping 359 -> 0 would otherwise twist the tail once.
        controlledYaw += Mth.clamp(Mth.wrapDegrees(yaw - controlledYaw), -maxTurn, maxTurn);
    }

    public Vec3 getPreviousMotion() { return previousMotion; }
}
