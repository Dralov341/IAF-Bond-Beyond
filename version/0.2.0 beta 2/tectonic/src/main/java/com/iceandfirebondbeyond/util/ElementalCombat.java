package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntityFireDragon;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.misc.IafDamageRegistry;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import com.iceandfirebondbeyond.item.DragonArmorMaterial;
import com.iceandfirebondbeyond.item.SeaSerpentArmorItem.ArmorTier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;

/** Element is determined by the actual attack, never just by the attacker's species. */
public final class ElementalCombat {
    public enum Element { WATER, FIRE, ICE, LIGHTNING }

    /** Serpent armor is one complete set; dragons need four matching pieces. */
    public static boolean wearsCounterArmor(Entity target, Element element) {
        if (target instanceof EntitySeaSerpent serpent) {
            ArmorTier tier = SeaSerpentBondData.getArmorTier(serpent);
            return tier != null && switch (element) {
                case WATER -> tier == ArmorTier.SEA_SERPENT_STEEL;
                case FIRE -> tier == ArmorTier.DRAGONSTEEL_FIRE;
                case ICE -> tier == ArmorTier.DRAGONSTEEL_ICE;
                case LIGHTNING -> tier == ArmorTier.DRAGONSTEEL_LIGHTNING;
            };
        }
        if (!(target instanceof EntityDragonBase dragon)) return false;
        DragonArmorMaterial material = switch (element) {
            case WATER -> DragonArmorMaterial.SEA_SERPENT_STEEL;
            case FIRE -> DragonArmorMaterial.DRAGONSTEEL_FIRE;
            case ICE -> DragonArmorMaterial.DRAGONSTEEL_ICE;
            case LIGHTNING -> DragonArmorMaterial.DRAGONSTEEL_LIGHTNING;
        };
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            if (DragonArmorMaterial.fromStack(dragon.getItemBySlot(slot)) != material) return false;
        return true;
    }

    public static float breathMultiplier(Entity target) {
        if (wearsCounterArmor(target, Element.WATER)) return 1.0F;
        if (target instanceof EntityFireDragon) return 1.6F;
        if (target instanceof EntityIceDragon) return 1.5F;
        return 1.0F;
    }

    public static int breathDrowningTicks(EntitySeaSerpent serpent) {
        return 40 * SeaSerpentBondData.getKnockbackStage(serpent);
    }

    public static boolean hurtWithBreath(EntitySeaSerpent serpent, LivingEntity target, float baseDamage) {
        float damage = baseDamage * breathMultiplier(target);
        // Counter armor changes only the bonus multiplier, never base damage or Drowning.
        if (damage <= 0 || !SeaSerpentBondData.hurtWithMountedAbility(serpent, target, damage)) return false;
        if (target.isAlive()) target.addEffect(new MobEffectInstance(
                SeaSteelContent.DROWNING.get(), breathDrowningTicks(serpent), 0), serpent);
        return true;
    }

    public static float incomingMultiplier(Entity target, DamageSource source) {
        if (!(target instanceof EntitySeaSerpent)) return 1.0F;
        if (source.is(IafDamageRegistry.DRAGON_ICE_TYPE) || source.is(DamageTypes.FREEZE))
            return wearsCounterArmor(target, Element.ICE) ? 1.0F : 1.3F;
        if (source.is(IafDamageRegistry.DRAGON_LIGHTNING_TYPE) || source.is(DamageTypes.LIGHTNING_BOLT))
            return wearsCounterArmor(target, Element.LIGHTNING) ? 1.0F : 1.6F;
        return 1.0F;
    }

    public static boolean isPhysicalMelee(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
    }

    private ElementalCombat() {}
}
