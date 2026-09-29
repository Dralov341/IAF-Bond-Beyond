package com.iceandfirebondbeyond.registry;

import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.entity.SeaSerpentEggEntity;
import com.iceandfirebondbeyond.entity.SeaSerpentRiderBubbleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(
        modid = IceAndFireBondBeyond.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, IceAndFireBondBeyond.MOD_ID);

    public static final RegistryObject<EntityType<SeaSerpentEggEntity>> SEA_SERPENT_EGG =
            ENTITIES.register(
                    "sea_serpent_egg",
                    () -> EntityType.Builder
                            .of(SeaSerpentEggEntity::new, MobCategory.MISC)
                            .sized(0.65F, 0.8F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("sea_serpent_egg")
            );

    public static final RegistryObject<EntityType<SeaSerpentRiderBubbleEntity>>
            SEA_SERPENT_RIDER_BUBBLE = ENTITIES.register(
            "sea_serpent_rider_bubble",
            () -> EntityType.Builder
                    .<SeaSerpentRiderBubbleEntity>of(
                            SeaSerpentRiderBubbleEntity::new,
                            MobCategory.MISC
                    )
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .setCustomClientFactory(SeaSerpentRiderBubbleEntity::new)
                    .build("sea_serpent_rider_bubble")
    );

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SEA_SERPENT_EGG.get(), SeaSerpentEggEntity.createAttributes().build());
    }
}
