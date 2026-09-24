package com.iceandfirebondbeyond.mixin;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.github.alexthe666.iceandfire.item.ItemAlchemySword;
import com.iceandfirebondbeyond.util.DragonSteelLightning;
import com.iceandfirebondbeyond.util.ElementalCombat;
import com.github.alexthe666.iceandfire.entity.EntityFireDragon;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the lightning variant is replaced; fire/ice and vanilla sword wear stay native. */
@Mixin(ItemAlchemySword.class)
public abstract class AlchemySwordLightningMixin extends SwordItem {
    protected AlchemySwordLightningMixin(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void bondBeyond$electricArcs(ItemStack stack, LivingEntity target, LivingEntity attacker,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != IafItemRegistry.DRAGONBONE_SWORD_LIGHTNING.get()) return;
        if (IafConfig.dragonWeaponLightningAbility && !attacker.level().isClientSide) {
            if ((target instanceof EntityFireDragon || target instanceof EntityIceDragon)
                    && !ElementalCombat.wearsCounterArmor(target, ElementalCombat.Element.LIGHTNING))
                target.hurt(attacker.level().damageSources().lightningBolt(), 9.5F);
            DragonSteelLightning.strike(stack, target, attacker);
            target.knockback(1.0F, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        }
        cir.setReturnValue(super.hurtEnemy(stack, target, attacker));
    }

    /** IAF encodes its fire/ice sword weakness as a separate 13.5 damage hit.
     * Only that extra hit is suppressed; normal melee damage and freeze/burn stay native. */
    @Redirect(method = "hurtEnemy", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean bondBeyond$counterArmor(LivingEntity target, DamageSource source, float amount) {
        if ((Object) this == IafItemRegistry.DRAGONBONE_SWORD_FIRE.get()
                && ElementalCombat.wearsCounterArmor(target, ElementalCombat.Element.FIRE)) return false;
        if ((Object) this == IafItemRegistry.DRAGONBONE_SWORD_ICE.get()
                && ElementalCombat.wearsCounterArmor(target, ElementalCombat.Element.ICE)) return false;
        return target.hurt(source, amount);
    }
}
