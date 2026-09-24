package com.iceandfirebondbeyond.item;

import com.github.alexthe666.iceandfire.item.ItemDragonArmor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;
import javax.annotation.Nullable;

/** Uses IAF's four real equipment slots and Dragonsteel protection (+10 per part). */
public final class SeaSteelDragonArmorItem extends ItemDragonArmor {
    private static final String[] PARTS = {"head", "neck", "body", "tail"};
    public SeaSteelDragonArmorItem(int slot) { super(DragonArmorType.ICE, slot); }
    @Override public String getDescriptionId() {
        return "item.iceandfire_bond_beyond.dragonarmor_sea_serpent_steel_" + PARTS[dragonSlot];
    }
    @Override public boolean isFireResistant() { return true; }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("tooltip.iceandfire_bond_beyond.dragonarmor_seasteel").withStyle(ChatFormatting.AQUA));
    }
}
