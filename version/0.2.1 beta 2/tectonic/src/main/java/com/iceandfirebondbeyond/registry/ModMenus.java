package com.iceandfirebondbeyond.registry;

import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.inventory.SeaSerpentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    ForgeRegistries.MENU_TYPES,
                    IceAndFireBondBeyond.MOD_ID
            );

    public static final RegistryObject<MenuType<SeaSerpentMenu>> SEA_SERPENT =
            MENUS.register(
                    "sea_serpent",
                    () -> IForgeMenuType.create(SeaSerpentMenu::new)
            );

    private ModMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
