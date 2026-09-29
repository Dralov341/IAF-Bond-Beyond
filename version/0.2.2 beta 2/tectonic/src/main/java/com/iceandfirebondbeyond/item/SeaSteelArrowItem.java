package com.iceandfirebondbeyond.item;

import com.iceandfirebondbeyond.entity.SeaSteelArrowEntity;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SeaSteelArrowItem extends ArrowItem {
    public SeaSteelArrowItem() { super(new Properties().fireResistant()); }
    @Override public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new SeaSteelArrowEntity(SeaSteelContent.ARROW_ENTITY.get(), shooter, level);
    }
}
