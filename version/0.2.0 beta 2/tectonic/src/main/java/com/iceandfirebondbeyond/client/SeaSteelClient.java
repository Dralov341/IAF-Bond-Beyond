package com.iceandfirebondbeyond.client;

import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.client.renderer.*;
import com.iceandfirebondbeyond.client.screen.SeaSerpentForgeScreen;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = IceAndFireBondBeyond.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SeaSteelClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SeaSteelContent.CORPSE.get(), SeaSerpentCorpseRenderer::new);
        event.registerEntityRenderer(SeaSteelContent.ARROW_ENTITY.get(), SeaSteelArrowRenderer::new);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(SeaSteelContent.FORGE_MENU.get(), SeaSerpentForgeScreen::new);
            ItemProperties.register(SeaSteelContent.BOW.get(), new ResourceLocation("pull"), (stack, level, entity, seed) ->
                    entity != null && entity.getUseItem() == stack ? (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F : 0.0F);
            ItemProperties.register(SeaSteelContent.BOW.get(), new ResourceLocation("pulling"), (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            for (var shield : java.util.List.of(SeaSteelContent.SHIELD, SeaSteelContent.FIRE_SHIELD,
                    SeaSteelContent.ICE_SHIELD, SeaSteelContent.LIGHTNING_SHIELD)) {
                ItemProperties.register(shield.get(), new ResourceLocation("blocking"), (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            }
        });
    }
    private SeaSteelClient() {}
}
