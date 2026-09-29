package com.iceandfirebondbeyond.review;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
@Mod.EventBusSubscriber(modid="iceandfire_bond_beyond",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class LightningTestItems {
 @SubscribeEvent public static void register(RegisterEvent event) {
  event.register(Registries.ITEM,new ResourceLocation("bond_test","lightning_hammer"),()->new SwordItem(Tiers.IRON,3,-2.4F,new Item.Properties()));
  event.register(Registries.ITEM,new ResourceLocation("bond_test","lightning_excluded_sword"),()->new SwordItem(Tiers.IRON,3,-2.4F,new Item.Properties()));
  event.register(Registries.ITEM,new ResourceLocation("bond_test","lightning_bow"),()->new BowItem(new Item.Properties().durability(384)));
 }
}
