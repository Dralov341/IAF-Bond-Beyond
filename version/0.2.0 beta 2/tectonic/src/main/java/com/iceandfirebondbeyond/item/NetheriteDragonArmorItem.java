package com.iceandfirebondbeyond.item;

import com.github.alexthe666.iceandfire.item.ItemDragonArmor;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * CE Netherite armor adapted to IAF beta-5's native ItemDragonArmor contract.
 * Inheriting it preserves the native GUI, shift-click, equipment sync and NBT.
 */
public final class NetheriteDragonArmorItem extends ItemDragonArmor {
    private static final String[] PART_NAMES = {"head", "neck", "body", "tail"};

    public NetheriteDragonArmorItem(int slot) {
        // IAF's enum cannot be extended. DragonArmorMixin supplies ordinal 9 and
        // CE protection; this legacy value is only a non-null constructor token.
        super(DragonArmorType.DIAMOND, slot);
        if (slot < 0 || slot >= PART_NAMES.length) {
            throw new IllegalArgumentException("Invalid dragon armor slot: " + slot);
        }
    }

    @Override
    public @NotNull String getDescriptionId() {
        return "item." + IceAndFireBondBeyond.MOD_ID
                + ".dragonarmor_netherite_" + PART_NAMES[dragonSlot];
    }

    @Override
    public boolean isFireResistant() {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
            @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(
                "item.iceandfire_bond_beyond.dragon_armor.protection",
                (int) DragonArmorMaterial.NETHERITE.getProtection()
        ).withStyle(ChatFormatting.BLUE));
    }
}
