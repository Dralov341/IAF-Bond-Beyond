package com.iceandfirebondbeyond.item;

import com.github.alexthe666.iceandfire.item.ItemDragonArmor;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Armor material data ported from IceAndFire-CE's DragonArmorMaterial (LGPL-3.0).
 * Original IAF already registers Dragonsteel; keep its items, inventory slots and
 * ordinals instead of registering a second set. Netherite extends the old IDs.
 */
public enum DragonArmorMaterial {
    NETHERITE("netherite", 9, 7.0D),
    DRAGONSTEEL_FIRE("dragon_steel_fire", 5, 10.0D),
    DRAGONSTEEL_ICE("dragon_steel_ice", 6, 10.0D),
    DRAGONSTEEL_LIGHTNING("dragon_steel_lightning", 8, 10.0D),
    SEA_SERPENT_STEEL("sea_serpent_steel", 6, 10.0D);

    private final String textureName;
    private final int armorOrdinal;
    private final double protection;

    DragonArmorMaterial(String textureName, int armorOrdinal, double protection) {
        this.textureName = textureName;
        this.armorOrdinal = armorOrdinal;
        this.protection = protection;
    }

    public String getTextureName() {
        return textureName;
    }

    public int getArmorOrdinal() {
        return armorOrdinal;
    }

    public double getProtection() {
        return protection;
    }

    @Nullable
    public static DragonArmorMaterial fromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof NetheriteDragonArmorItem) return NETHERITE;
        if (stack.getItem() instanceof SeaSteelDragonArmorItem) return SEA_SERPENT_STEEL;
        if (!(stack.getItem() instanceof ItemDragonArmor armor)) return null;
        return switch (armor.type) {
            case FIRE -> DRAGONSTEEL_FIRE;
            case ICE -> DRAGONSTEEL_ICE;
            case LIGHTNING -> DRAGONSTEEL_LIGHTNING;
            default -> null;
        };
    }
}
