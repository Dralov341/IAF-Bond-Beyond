package com.iceandfirebondbeyond.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Ordinary shield blocking/durability, with retaliation handled by ShieldBlockEvent. */
public final class ElementalShieldItem extends ShieldItem {
    public enum Element { WATER, FIRE, ICE, LIGHTNING }

    private final Element element;
    private final Supplier<Item> repairMaterial;

    public ElementalShieldItem(Element element, Supplier<Item> repairMaterial) {
        super(new Item.Properties().durability(2500).fireResistant().rarity(Rarity.RARE));
        this.element = element;
        this.repairMaterial = repairMaterial;
    }

    public Element element() { return element; }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return ingredient.is(repairMaterial.get());
    }

    @Override
    public int getEnchantmentValue() { return 14; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.iceandfire_bond_beyond.shield."
                + element.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.iceandfire_bond_beyond.shield.range")
                .withStyle(ChatFormatting.GRAY));
    }
}
