package com.iceandfirebondbeyond.client;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.client.model.SeaSerpentArmorModel;
import com.iceandfirebondbeyond.client.model.SeaSerpentEggModel;
import com.iceandfirebondbeyond.client.renderer.layer.SeaSerpentArmorLayer;
import com.iceandfirebondbeyond.client.renderer.SeaSerpentEggRenderer;
import com.iceandfirebondbeyond.client.renderer.SeaSerpentBubbleRenderer;
import com.iceandfirebondbeyond.client.screen.SeaSerpentScreen;
import com.iceandfirebondbeyond.registry.ModEntities;
import com.iceandfirebondbeyond.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = IceAndFireBondBeyond.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SeaSerpentKeys.BITE);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event
    ) {
        event.registerLayerDefinition(
                SeaSerpentEggModel.LAYER_LOCATION,
                SeaSerpentEggModel::createBodyLayer
        );
        event.registerLayerDefinition(
                SeaSerpentArmorModel.LAYER_LOCATION,
                SeaSerpentArmorModel::createBodyLayer
        );
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.SEA_SERPENT_EGG.get(),
                SeaSerpentEggRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.SEA_SERPENT_RIDER_BUBBLE.get(),
                SeaSerpentBubbleRenderer::new
        );
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(
                ModMenus.SEA_SERPENT.get(),
                SeaSerpentScreen::new
        ));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        LivingEntityRenderer rawRenderer = event.getRenderer(
                IafEntityRegistry.SEA_SERPENT.get()
        );
        if (rawRenderer == null) {
            return;
        }

        LivingEntityRenderer<
                EntitySeaSerpent,
                AdvancedEntityModel<EntitySeaSerpent>
                > renderer = (LivingEntityRenderer<
                EntitySeaSerpent,
                AdvancedEntityModel<EntitySeaSerpent>
                >) rawRenderer;
        renderer.addLayer(new SeaSerpentArmorLayer(
                renderer,
                new SeaSerpentArmorModel<>(
                        event.getEntityModels().bakeLayer(
                                SeaSerpentArmorModel.LAYER_LOCATION
                        )
                )
        ));
    }
}
