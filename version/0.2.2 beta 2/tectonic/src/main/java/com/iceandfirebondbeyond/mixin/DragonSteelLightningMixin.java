package com.iceandfirebondbeyond.mixin;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.item.DragonSteelOverrides;
import com.github.alexthe666.iceandfire.item.DragonSteelTier;
import com.github.alexthe666.iceandfire.item.ItemModSword;
import com.github.alexthe666.iceandfire.item.ItemModAxe;
import com.github.alexthe666.iceandfire.item.ItemModPickaxe;
import com.github.alexthe666.iceandfire.item.ItemModShovel;
import com.github.alexthe666.iceandfire.item.ItemModHoe;
import com.iceandfirebondbeyond.util.DragonSteelLightning;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replace the shared ability call, keeping each native tool's durability handling. */
@Mixin({ItemModSword.class, ItemModAxe.class, ItemModPickaxe.class, ItemModShovel.class, ItemModHoe.class})
public abstract class DragonSteelLightningMixin {
    @Redirect(method = "hurtEnemy(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At(value = "INVOKE", remap = false,
                    target = "hurtEnemy(Lnet/minecraft/world/item/TieredItem;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V"))
    @SuppressWarnings("unchecked")
    private void bondBeyond$chainLightning(@Coerce Object receiver, TieredItem item, ItemStack stack,
            LivingEntity target, LivingEntity attacker) {
        if (item.getTier() == DragonSteelTier.DRAGONSTEEL_TIER_LIGHTNING) {
            DragonSteelLightning.strike(stack, target, attacker);
            // Replacing the sky bolt must retain the native weapon's knockback.
            if (IafConfig.dragonWeaponLightningAbility && !attacker.level().isClientSide) {
                target.knockback(1F, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            }
        } else {
            ((DragonSteelOverrides<TieredItem>) receiver).hurtEnemy(item, stack, target, attacker);
        }
    }
}
