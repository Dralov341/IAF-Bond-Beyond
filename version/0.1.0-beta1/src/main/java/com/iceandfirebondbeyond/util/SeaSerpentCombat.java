package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntityMutlipartPart;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;

/** Server combat queries include IAF's separate head/neck entities and Forge parts. */
public final class SeaSerpentCombat {
    private SeaSerpentCombat() {}

    @Nullable
    public static Entity damageTarget(Entity part) {
        Entity target = part;
        // resetParts can initially chain IAF's slow parts; tolerate a complete
        // head/tail chain as well as the root parents assigned by updatePart.
        // Keep a bound so a malformed/cyclic modded parent link cannot hang.
        for (int depth = 0; depth < 16; depth++) {
            if (target instanceof EntityMutlipartPart iafPart) {
                target = iafPart.getParent();
            } else if (target instanceof PartEntity<?> forgePart) {
                target = forgePart.getParent();
            } else {
                return target;
            }
            if (target == null || target == part) return null;
        }
        return null;
    }

    public static boolean canAttack(EntitySeaSerpent serpent, Entity part, boolean livingOnly) {
        Entity target = damageTarget(part);
        if (target == null || target == serpent || !target.isAlive() || target.isSpectator()
                || target.getRootVehicle() == serpent || target instanceof Projectile
                || serpent.isAlliedTo(target)) return false;
        if (target instanceof LivingEntity living) {
            return !SeaSerpentBondData.isProtectedOwnedAlly(serpent, living);
        }
        return !livingOnly && target.isPickable();
    }

    /** One aimed victim per bite, including contacts with native multipart boxes. */
    @Nullable
    public static TargetHit findBiteTarget(EntitySeaSerpent serpent, Player rider,
            Vec3 mouth, SeaSerpentBiteVolume volume) {
        TargetHit nearest = null;
        boolean nearestAimed = false;
        double nearestDistance = Double.POSITIVE_INFINITY;
        Vec3 eye = rider.getEyePosition();
        Vec3 aimEnd = eye.add(rider.getLookAngle().scale(
                Math.sqrt(eye.distanceToSqr(mouth)) + SeaSerpentBondData.getMountedBiteReach(serpent)));
        for (Entity part : serpent.level().getEntities(serpent, volume.bounds(),
                entity -> canAttack(serpent, entity, true))) {
            AABB body = part.getBoundingBox();
            Vec3 contact = volume.contact(body);
            if (contact == null) continue;

            // Keep a directly aimed mob ahead of a bystander at the edge of the
            // wider bite. An eye ray is a preference, not the physical hitbox.
            Vec3 aimedContact = body.contains(eye) ? eye : body.clip(eye, aimEnd).orElse(null);
            boolean aimed = aimedContact != null && volume.contains(aimedContact)
                    && visible(serpent, eye, aimedContact) && visible(serpent, mouth, aimedContact);
            if (aimed) contact = aimedContact;
            else if (!visible(serpent, mouth, contact)) continue;

            double distance = (aimed ? eye : mouth).distanceToSqr(contact);
            if (nearest == null || (aimed && !nearestAimed)
                    || (aimed == nearestAimed && distance < nearestDistance)) {
                Entity target = damageTarget(part);
                if (target == null) continue;
                nearest = new TargetHit(target, part, contact, contact);
                nearestAimed = aimed;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /**
     * Sweep an aimed volume, then check the REAL contacted part for reach and
     * visibility. A head hit damages its parent once, even if the body is far
     * behind it. Expanding a target's box must never allow hits through a wall.
     */
    @Nullable
    public static TargetHit findTarget(EntitySeaSerpent serpent, Vec3 start, Vec3 end,
            double radius, Vec3 mouth, double reach, boolean livingOnly) {
        TargetHit nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        Vec3 direction = end.subtract(start).normalize();
        for (Entity part : serpent.level().getEntities(serpent,
                new AABB(start, end).inflate(radius), entity -> canAttack(serpent, entity, livingOnly))) {
            AABB body = part.getBoundingBox();
            AABB volume = body.inflate(radius);
            Vec3 hit = volume.contains(start) ? start : volume.clip(start, end).orElse(null);
            if (hit == null) continue;
            Vec3 contact = closestPoint(body, hit);
            if (mouth.distanceToSqr(contact) > reach * reach
                    || contact.subtract(mouth).dot(direction) < -radius
                    || !visible(serpent, start, contact)
                    || (mouth.distanceToSqr(start) > 1.0E-8D
                    && !visible(serpent, mouth, contact))) continue;
            double distance = start.distanceToSqr(hit);
            if (distance < nearestDistance) {
                Entity target = damageTarget(part);
                if (target == null) continue;
                nearestDistance = distance;
                nearest = new TargetHit(target, part, hit, contact);
            }
        }
        return nearest;
    }

    public static Vec3 closestPoint(AABB box, Vec3 point) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX),
                Mth.clamp(point.y, box.minY, box.maxY), Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    private static boolean visible(EntitySeaSerpent serpent, Vec3 from, Vec3 to) {
        return serpent.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, serpent)).getType() == HitResult.Type.MISS;
    }

    public record TargetHit(Entity target, Entity part, Vec3 rayPoint, Vec3 contact) {}
}
