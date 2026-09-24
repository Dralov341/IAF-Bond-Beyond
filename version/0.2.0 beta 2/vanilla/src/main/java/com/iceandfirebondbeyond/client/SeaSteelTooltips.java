package com.iceandfirebondbeyond.client;

import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.item.SeaSteelArmorItem;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IceAndFireBondBeyond.MOD_ID, value = Dist.CLIENT)
public final class SeaSteelTooltips {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var item = event.getItemStack().getItem();
        if (SeaSteelContent.isMelee(item) || item == SeaSteelContent.BOW.get() || item == SeaSteelContent.ARROW.get())
            event.getToolTip().add(Component.translatable("tooltip.iceandfire_bond_beyond.steel.hit").withStyle(ChatFormatting.AQUA));
    }
    private SeaSteelTooltips() {}
}
