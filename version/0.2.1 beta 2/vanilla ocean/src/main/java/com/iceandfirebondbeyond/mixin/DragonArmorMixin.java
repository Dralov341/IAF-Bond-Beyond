package com.iceandfirebondbeyond.mixin;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.iceandfirebondbeyond.item.DragonArmorMaterial;
import com.iceandfirebondbeyond.item.NetheriteDragonArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extend IAF's armor calculation with CE's Netherite protection per piece. */
@Mixin(EntityDragonBase.class)
public abstract class DragonArmorMixin {
    @Inject(method = "getArmorOrdinal", at = @At("HEAD"),
            cancellable = true, remap = false)
    private void bondBeyond$netheriteOrdinal(ItemStack stack,
            CallbackInfoReturnable<Integer> cir) {
        if (!stack.isEmpty() && stack.getItem() instanceof NetheriteDragonArmorItem) {
            cir.setReturnValue(DragonArmorMaterial.NETHERITE.getArmorOrdinal());
        }
    }

    @Inject(method = "calculateArmorModifier", at = @At("RETURN"),
            cancellable = true, remap = false)
    private void bondBeyond$netheriteProtection(CallbackInfoReturnable<Double> cir) {
        EntityDragonBase dragon = (EntityDragonBase) (Object) this;
        double addedProtection = 0.0D;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = dragon.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof NetheriteDragonArmorItem) {
                addedProtection += DragonArmorMaterial.NETHERITE.getProtection();
            }
        }
        // IAF ignores ordinal 9, so only the new pieces are added here. Native
        // Dragonsteel remains +10 per piece; mixed sets keep their native values.
        if (addedProtection != 0.0D) {
            cir.setReturnValue(cir.getReturnValue() + addedProtection);
        }
    }
}
