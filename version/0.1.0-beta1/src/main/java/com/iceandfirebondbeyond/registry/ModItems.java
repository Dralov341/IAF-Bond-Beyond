package com.iceandfirebondbeyond.registry;

import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.github.alexthe666.iceandfire.item.IafTabRegistry;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.item.SeaSerpentArmorItem;
import com.iceandfirebondbeyond.item.SeaSerpentEggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, IceAndFireBondBeyond.MOD_ID);

    public static final RegistryObject<SeaSerpentEggItem> SEA_SERPENT_EGG_BLUE =
            registerEgg("sea_serpent_egg_blue", EnumSeaSerpent.BLUE);

    public static final RegistryObject<SeaSerpentEggItem> SEA_SERPENT_EGG_BRONZE =
            registerEgg("sea_serpent_egg_bronze", EnumSeaSerpent.BRONZE);

    // IAF calls the Java enum DEEPBLUE, but its serpent skin is darkblue.png.
    // Keep the native enum/ordinal for saved entities; name our assets darkblue.
    public static final RegistryObject<SeaSerpentEggItem> SEA_SERPENT_EGG_DARKBLUE =
            registerEgg("sea_serpent_egg_darkblue", EnumSeaSerpent.DEEPBLUE);

    public static final RegistryObject<SeaSerpentEggItem> SEA_SERPENT_EGG_GREEN =
            registerEgg("sea_serpent_egg_green", EnumSeaSerpent.GREEN);

    public static final RegistryObject<SeaSerpentEggItem> SEA_SERPENT_EGG_PURPLE =
            registerEgg("sea_serpent_egg_purple", EnumSeaSerpent.PURPLE);

    public static final RegistryObject<SeaSerpentEggItem> SEA_SERPENT_EGG_RED =
            registerEgg("sea_serpent_egg_red", EnumSeaSerpent.RED);

    public static final RegistryObject<SeaSerpentEggItem> SEA_SERPENT_EGG_TEAL =
            registerEgg("sea_serpent_egg_teal", EnumSeaSerpent.TEAL);

    public static final RegistryObject<SeaSerpentArmorItem> SEA_SERPENT_ARMOR_IRON =
            registerArmor(
                    "sea_serpent_armor_iron",
                    SeaSerpentArmorItem.ArmorTier.IRON
            );
    public static final RegistryObject<SeaSerpentArmorItem> SEA_SERPENT_ARMOR_SILVER =
            registerArmor(
                    "sea_serpent_armor_silver",
                    SeaSerpentArmorItem.ArmorTier.SILVER
            );
    public static final RegistryObject<SeaSerpentArmorItem> SEA_SERPENT_ARMOR_GOLD =
            registerArmor(
                    "sea_serpent_armor_gold",
                    SeaSerpentArmorItem.ArmorTier.GOLD
            );
    public static final RegistryObject<SeaSerpentArmorItem> SEA_SERPENT_ARMOR_DIAMOND =
            registerArmor(
                    "sea_serpent_armor_diamond",
                    SeaSerpentArmorItem.ArmorTier.DIAMOND
            );
    public static final RegistryObject<SeaSerpentArmorItem>
            SEA_SERPENT_ARMOR_DRAGONSTEEL_FIRE = registerArmor(
            "sea_serpent_armor_dragonsteel_fire",
            SeaSerpentArmorItem.ArmorTier.DRAGONSTEEL_FIRE
    );
    public static final RegistryObject<SeaSerpentArmorItem>
            SEA_SERPENT_ARMOR_DRAGONSTEEL_ICE = registerArmor(
            "sea_serpent_armor_dragonsteel_ice",
            SeaSerpentArmorItem.ArmorTier.DRAGONSTEEL_ICE
    );
    public static final RegistryObject<SeaSerpentArmorItem>
            SEA_SERPENT_ARMOR_DRAGONSTEEL_LIGHTNING = registerArmor(
            "sea_serpent_armor_dragonsteel_lightning",
            SeaSerpentArmorItem.ArmorTier.DRAGONSTEEL_LIGHTNING
    );

    private ModItems() {}

    private static RegistryObject<SeaSerpentEggItem> registerEgg(
            String name,
            EnumSeaSerpent variant
    ) {
        RegistryObject<SeaSerpentEggItem> egg =
                ITEMS.register(name, () -> new SeaSerpentEggItem(variant));
        return egg;
    }

    private static RegistryObject<SeaSerpentArmorItem> registerArmor(
            String name,
            SeaSerpentArmorItem.ArmorTier tier
    ) {
        RegistryObject<SeaSerpentArmorItem> armor =
                ITEMS.register(name, () -> new SeaSerpentArmorItem(tier));
        return armor;
    }

    public static ItemStack getSeaSerpentEgg(EnumSeaSerpent variant) {
        return switch (variant) {
            case BRONZE -> new ItemStack(SEA_SERPENT_EGG_BRONZE.get());
            case DEEPBLUE -> new ItemStack(SEA_SERPENT_EGG_DARKBLUE.get());
            case GREEN -> new ItemStack(SEA_SERPENT_EGG_GREEN.get());
            case PURPLE -> new ItemStack(SEA_SERPENT_EGG_PURPLE.get());
            case RED -> new ItemStack(SEA_SERPENT_EGG_RED.get());
            case TEAL -> new ItemStack(SEA_SERPENT_EGG_TEAL.get());
            default -> new ItemStack(SEA_SERPENT_EGG_BLUE.get());
        };
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeTabContents);
    }

    private static void addCreativeTabContents(
            BuildCreativeModeTabContentsEvent event
    ) {
        if (!IafTabRegistry.TAB_ITEMS.getKey().equals(event.getTabKey())) {
            return;
        }

        event.accept(SEA_SERPENT_EGG_BLUE.get());
        event.accept(SEA_SERPENT_EGG_BRONZE.get());
        event.accept(SEA_SERPENT_EGG_DARKBLUE.get());
        event.accept(SEA_SERPENT_EGG_GREEN.get());
        event.accept(SEA_SERPENT_EGG_PURPLE.get());
        event.accept(SEA_SERPENT_EGG_RED.get());
        event.accept(SEA_SERPENT_EGG_TEAL.get());

        event.accept(SEA_SERPENT_ARMOR_IRON.get());
        event.accept(SEA_SERPENT_ARMOR_SILVER.get());
        event.accept(SEA_SERPENT_ARMOR_GOLD.get());
        event.accept(SEA_SERPENT_ARMOR_DIAMOND.get());
        event.accept(SEA_SERPENT_ARMOR_DRAGONSTEEL_FIRE.get());
        event.accept(SEA_SERPENT_ARMOR_DRAGONSTEEL_ICE.get());
        event.accept(SEA_SERPENT_ARMOR_DRAGONSTEEL_LIGHTNING.get());
    }
}
