package com.iceandfirebondbeyond.item;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;

public final class SeaSteelBowItem extends BowItem {
    public static final String IMBUED = "BondBeyondSeaSteelArrow";
    public SeaSteelBowItem() { super(new Properties().durability(3000).fireResistant()); }
    @Override public AbstractArrow customArrow(AbstractArrow arrow) {
        // Preserve tipped/spectral/custom ammunition, pickup type and enchantments.
        arrow.getPersistentData().putBoolean(IMBUED, true);
        arrow.setBaseDamage(arrow.getBaseDamage() + 2.0D);
        return arrow;
    }
}
