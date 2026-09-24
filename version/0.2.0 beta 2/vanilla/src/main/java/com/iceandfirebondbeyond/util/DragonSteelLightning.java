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
 * Keeps CE's defaults: 10-block search, 10 mobs, half damage per hop.
 * Credits and upstream license: META-INF/BOND-BEYOND-CE-NOTICE.txt.
 */
public final class DragonSteelLightning {
    public static final double SEARCH_RANGE = 10;
    public static final int MAX_TARGETS = 10;
    private static final float DAMAGE_REDUCTION = .5F;

    public static void strike(ItemStack weapon, LivingEntity target, LivingEntity attacker) {
        float damage = (float) weapon.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE).stream()
                .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                .mapToDouble(AttributeModifier::getAmount).sum();
        strike(Math.max(1, damage), target, attacker);
    }

    /** Shared by Dragonsteel, the lightning-blood sword, and a successful shield block. */
    public static void strike(float damage, LivingEntity target, LivingEntity attacker) {
        if (!IafConfig.dragonWeaponLightningAbility || !(attacker.level() instanceof ServerLevel level)
                || !Float.isFinite(damage) || damage <= 0 || !canStrike(target, attacker)) return;
        DamageSource source = IafDamageRegistry.causeDragonLightningDamage(attacker);

        // CE searches around the wielder. Keep the search bounded instead of
        // letting a dense crowd carry lightning indefinitely across the world.
        Vec3 origin = attacker.position();
        AABB search = new AABB(origin, origin).inflate(SEARCH_RANGE);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, search,
                mob -> canStrike(mob, attacker) && attacker.hasLineOfSight(mob));
        Set<Integer> discovered = new HashSet<>();
        List<LightningArcPacket.Arc> arcs = new ArrayList<>();
        Vec3 from = attacker.getEyePosition();
        LivingEntity current = target;
        float hopDamage = damage;
        while (current != null && discovered.size() < MAX_TARGETS) {
            discovered.add(current.getId());
            arcs.add(new LightningArcPacket.Arc(from, current.getBoundingBox().getCenter()));
            current.hurt(source, hopDamage); // Ordinary damage immunity still applies.
            LivingEntity next = null;
            double nearest = Double.POSITIVE_INFINITY;
            for (LivingEntity candidate : nearby) {
                double distance = current.distanceToSqr(candidate);
                if (!discovered.contains(candidate.getId()) && canStrike(candidate, attacker)
                        && distance <= SEARCH_RANGE * SEARCH_RANGE && distance < nearest
                        && current.hasLineOfSight(candidate)) {
                    next = candidate;
                    nearest = distance;
                }
            }
            from = current.getBoundingBox().getCenter();
            current = next;
            hopDamage *= DAMAGE_REDUCTION;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                origin.x, origin.y, origin.z, 64, level.dimension())),
                new LightningArcPacket(level.dimension().location(), arcs));
    }

    public static boolean canStrike(LivingEntity mob, LivingEntity attacker) {
        if (mob == attacker || !mob.isAlive() || mob.isSpectator() || mob.isAlliedTo(attacker)
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
