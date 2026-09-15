package com.iceandfirebondbeyond.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * One item represents the complete armor visible on the supplied model.
 */
public final class SeaSerpentArmorItem extends Item {
    private final ArmorTier tier;

    public SeaSerpentArmorItem(ArmorTier tier) {
        super(new Item.Properties().stacksTo(1));
        this.tier = tier;
    }

    public ArmorTier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @Nullable Level level,
            @NotNull List<Component> tooltip,
            @NotNull TooltipFlag flag
    ) {
        tooltip.add(Component.translatable(
                "item.iceandfire_bond_beyond.sea_serpent_armor.full_set"
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "item.iceandfire_bond_beyond.sea_serpent_armor.protection",
                (int) tier.getArmorBonus()
        ).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable(
                "item.iceandfire_bond_beyond.sea_serpent_armor.integrated_saddle"
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                "item.iceandfire_bond_beyond.sea_serpent_armor.ability"
        ).withStyle(ChatFormatting.AQUA));
    }

    public enum ArmorTier {
        IRON("iron", 9.0D),
        SILVER("silver", 13.0D),
        GOLD("gold", 13.0D),
        DIAMOND("diamond", 21.0D),
        DRAGONSTEEL_FIRE("firesteel", 41.0D),
        DRAGONSTEEL_ICE("icesteel", 41.0D),
        DRAGONSTEEL_LIGHTNING("lightningsteel", 41.0D);

        private static final ArmorTier[] VALUES = values();

        private final String textureName;
        private final double armorBonus;

        ArmorTier(String textureName, double armorBonus) {
            this.textureName = textureName;
            this.armorBonus = armorBonus;
        }

        public String getTextureName() {
            return textureName;
        }

        public double getArmorBonus() {
            return armorBonus;
        }

        @Nullable
        public static ArmorTier fromNetworkId(int id) {
            int index = id - 1;
            return index >= 0 && index < VALUES.length ? VALUES[index] : null;
        }
    }
}
