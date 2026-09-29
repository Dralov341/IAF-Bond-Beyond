package com.iceandfirebondbeyond.client;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.iceandfire.client.render.entity.layer.LayerDragonArmor;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.client.renderer.layer.DragonArmorLayer;
import com.iceandfirebondbeyond.mixin.LivingEntityRendererAccess;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IceAndFireBondBeyond.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DragonArmorClientEvents {
    private DragonArmorClientEvents() {}

    @SubscribeEvent
    public static void colorSeaSteel(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> 0x4CA3AD,
                com.iceandfirebondbeyond.registry.ModItems.DRAGONARMOR_SEASTEEL_HEAD.get(),
                com.iceandfirebondbeyond.registry.ModItems.DRAGONARMOR_SEASTEEL_NECK.get(),
                com.iceandfirebondbeyond.registry.ModItems.DRAGONARMOR_SEASTEEL_BODY.get(),
                com.iceandfirebondbeyond.registry.ModItems.DRAGONARMOR_SEASTEEL_TAIL.get());
    }

    @SubscribeEvent
    public static void addArmorLayers(EntityRenderersEvent.AddLayers event) {
        replaceArmorLayer(event, IafEntityRegistry.FIRE_DRAGON.get());
        replaceArmorLayer(event, IafEntityRegistry.ICE_DRAGON.get());
        replaceArmorLayer(event, IafEntityRegistry.LIGHTNING_DRAGON.get());
    }

    @SuppressWarnings("unchecked")
    private static void replaceArmorLayer(EntityRenderersEvent.AddLayers event,
            EntityType<? extends EntityDragonBase> type) {
        EntityRenderer<?> raw = event.getRenderer(type);
        if (!(raw instanceof LivingEntityRenderer<?, ?> living)) return;
        LivingEntityRendererAccess access = (LivingEntityRendererAccess) living;
        access.bondBeyond$getLayers().removeIf(layer ->
                layer instanceof LayerDragonArmor || layer instanceof DragonArmorLayer);
        LivingEntityRenderer<EntityDragonBase, AdvancedEntityModel<EntityDragonBase>> renderer =
                (LivingEntityRenderer<EntityDragonBase, AdvancedEntityModel<EntityDragonBase>>) living;
        renderer.addLayer(new DragonArmorLayer(renderer));
    }
}
