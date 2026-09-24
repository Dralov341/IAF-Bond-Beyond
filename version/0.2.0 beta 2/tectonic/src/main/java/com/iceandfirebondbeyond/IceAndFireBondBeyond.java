package com.iceandfirebondbeyond;

import com.iceandfirebondbeyond.registry.ModBlockEntities;
import com.iceandfirebondbeyond.registry.ModBlocks;
import com.iceandfirebondbeyond.registry.ModEntities;
import com.iceandfirebondbeyond.registry.ModItems;
import com.iceandfirebondbeyond.registry.ModMenus;
import com.iceandfirebondbeyond.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(IceAndFireBondBeyond.MOD_ID)
public final class IceAndFireBondBeyond {
    public static final String MOD_ID = "iceandfire_bond_beyond";

    public IceAndFireBondBeyond() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModEntities.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        com.iceandfirebondbeyond.registry.SeaSteelContent.register(modBus);
        com.iceandfirebondbeyond.recipe.SeaSteelForgeRecipe.register(modBus);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
