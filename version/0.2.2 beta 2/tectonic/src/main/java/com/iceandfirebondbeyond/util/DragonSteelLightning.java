package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.misc.IafDamageRegistry;
import com.iceandfirebondbeyond.network.LightningArcPacket;
import com.iceandfirebondbeyond.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/** CE DragonSteelOverrides chain lightning, adapted to Forge/IAF beta-5.
 * Bounded 12-block search around the impact, 10 targets, half damage per hop.
 * Credits and upstream license: META-INF/BOND-BEYOND-CE-NOTICE.txt.
 */
public final class DragonSteelLightning {
    public static final double SEARCH_RANGE = 12;
    public static final int MAX_TARGETS = 10;
    private static final float DAMAGE_REDUCTION = .5F;

    private static final Map<ServerLevel, LinkedHashMap<HitKey, PendingHit>> PENDING = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);
    private record HitKey(UUID attacker, UUID target, long tick) {}
    private record PendingHit(float damage, LivingEntity target, LivingEntity attacker, Vec3 start, Vec3 end) {}

    public static boolean isProcessing() { return PROCESSING.get(); }

    /** Only replace an attributed weapon bolt matching a hit already accepted this tick.
     * Natural lightning, tridents, dragon skills and unrelated spell bolts are untouched. */
    public static boolean replacesWeaponBolt(LightningBolt bolt) {
        if (!(bolt.level() instanceof ServerLevel level)
                || !bolt.getTags().contains(com.github.alexthe666.iceandfire.event.ServerEvents.BOLT_DONT_DESTROY_LOOT)) return false;
        Map<HitKey, PendingHit> hits = PENDING.get(level);
        if (hits == null) return false;
        for (PendingHit hit : hits.values()) {
            if ((bolt.getCause() == hit.attacker || bolt.getTags().contains(hit.attacker.getStringUUID()))
                    && bolt.position().distanceToSqr(hit.end) <= 16) return true;
        }
        return false;
    }

    public static void strike(ItemStack weapon, LivingEntity target, LivingEntity attacker) {
        if (weapon.is(LightningWeapons.EXCLUDED)) return;
        float damage = (float) weapon.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE).stream()
                .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                .mapToDouble(AttributeModifier::getAmount).sum();
        if (!IafConfig.dragonWeaponLightningAbility || isProcessing()
                || !(attacker.level() instanceof ServerLevel level)
                || !Float.isFinite(damage) || !eligibleTarget(target, attacker)) return;
        // Native hurtEnemy and Forge's successful-damage event can describe the SAME hit.
        // Queue once and apply after melee has committed its health change. Otherwise a
        // nested hurt() inside LivingDamageEvent may be overwritten by the outer hit.
        PENDING.computeIfAbsent(level, ignored -> new LinkedHashMap<>()).putIfAbsent(
                new HitKey(attacker.getUUID(), target.getUUID(), level.getGameTime()),
                new PendingHit(Math.max(1, damage), target, attacker, attacker.getEyePosition(),
                        target.getBoundingBox().getCenter()));
    }

    /** Drain once after entity combat, including lethal hits and addon item callbacks. */
    public static void flush(ServerLevel level) {
        Map<HitKey, PendingHit> hits = PENDING.remove(level);
        if (hits == null) return;
        for (PendingHit hit : hits.values()) {
            if (hit.attacker.level() == level && !hit.attacker.isRemoved())
                emit(hit.damage, hit.target, hit.attacker, hit.start, hit.end);
        }
    }

    /** Immediate shield retaliation; weapon hits use the deduplicated queue above. */
    public static void strike(float damage, LivingEntity target, LivingEntity attacker) {
        emit(damage, target, attacker, attacker.getEyePosition(), target.getBoundingBox().getCenter());
    }

    private static void emit(float damage, LivingEntity target, LivingEntity attacker, Vec3 start, Vec3 impact) {
        if (!IafConfig.dragonWeaponLightningAbility || isProcessing()
                || !(attacker.level() instanceof ServerLevel level)
                || !Float.isFinite(damage) || damage <= 0 || !eligibleTarget(target, attacker)) return;
        PROCESSING.set(true);
        try {
            DamageSource source = IafDamageRegistry.causeDragonLightningDamage(attacker);
            // Search around the actual hit, also making long-reach/thrown weapons useful.
            // Every candidate remains inside this bounded impact region.
            AABB search = new AABB(impact, impact).inflate(SEARCH_RANGE);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, search,
                    mob -> canStrike(mob, attacker)
                            && mob.getBoundingBox().getCenter().distanceToSqr(impact) <= SEARCH_RANGE * SEARCH_RANGE);
            Set<Integer> discovered = new HashSet<>();
            List<LightningArcPacket.Arc> arcs = new ArrayList<>();
            Vec3 from = start;
            LivingEntity current = target;
            float hopDamage = damage;
            while (current != null && discovered.size() < MAX_TARGETS) {
                discovered.add(current.getId());
                Vec3 endpoint = current == target ? impact : current.getBoundingBox().getCenter();
                // Always send the first arc, even if melee killed the initial victim.
                arcs.add(new LightningArcPacket.Arc(from, endpoint));
                if (current.isAlive()) current.hurt(source, hopDamage);
                LivingEntity next = null;
                double nearest = Double.POSITIVE_INFINITY;
                for (LivingEntity candidate : nearby) {
                    double distance = endpoint.distanceToSqr(candidate.getBoundingBox().getCenter());
                    if (!discovered.contains(candidate.getId()) && canStrike(candidate, attacker)
                            && distance <= SEARCH_RANGE * SEARCH_RANGE && distance < nearest
                            && current.hasLineOfSight(candidate)) {
                        next = candidate;
                        nearest = distance;
                    }
                }
                from = endpoint;
                // No random failure: eligible additional targets chain with 100% chance.
                current = next;
                hopDamage *= DAMAGE_REDUCTION;
            }
            LightningArcPacket packet = new LightningArcPacket(level.dimension().location(), arcs);
            // Reach observers at BOTH ends, including ranged shots >64 blocks from shooter.
            for (net.minecraft.server.level.ServerPlayer viewer : level.players()) {
                if (viewer == attacker || viewer.position().distanceToSqr(start) <= 96 * 96
                        || viewer.position().distanceToSqr(impact) <= 96 * 96)
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), packet);
            }
        } finally {
            PROCESSING.remove();
        }
    }

    public static boolean canStrike(LivingEntity mob, LivingEntity attacker) {
        return mob.isAlive() && eligibleTarget(mob, attacker);
    }

    private static boolean eligibleTarget(LivingEntity mob, LivingEntity attacker) {
        if (mob.level() != attacker.level() || mob == attacker || mob.isSpectator() || mob.isAlliedTo(attacker)
                || attacker.isAlliedTo(mob) || mob.isPassengerOfSameVehicle(attacker)) return false;
        if (mob instanceof Player victim && (victim.isCreative()
                || (attacker instanceof Player player && (!player.canHarmPlayer(victim)
                || (attacker.level() instanceof ServerLevel server && !server.getServer().isPvpAllowed()))))) return false;
        UUID owner = mob instanceof OwnableEntity pet ? pet.getOwnerUUID()
                : mob instanceof EntitySeaSerpent serpent ? SeaSerpentBondData.getOwnerId(serpent) : null;
        if (owner == null) return true;
        if (owner.equals(attacker.getUUID())) return false;
        Player player = mob.level().getPlayerByUUID(owner);
        return player == null || !attacker.isAlliedTo(player);
    }

    private DragonSteelLightning() {}
}
