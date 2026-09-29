package com.iceandfirebondbeyond.registry;

import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.github.alexthe666.iceandfire.item.IafTabRegistry;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.item.NetheriteDragonArmorItem;
import com.iceandfirebondbeyond.item.SeaSteelDragonArmorItem;
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

    public static final RegistryObject<NetheriteDragonArmorItem> DRAGONARMOR_NETHERITE_HEAD =
            registerDragonArmor("dragonarmor_netherite_head", 0);
    public static final RegistryObject<NetheriteDragonArmorItem> DRAGONARMOR_NETHERITE_NECK =
            registerDragonArmor("dragonarmor_netherite_neck", 1);
    public static final RegistryObject<NetheriteDragonArmorItem> DRAGONARMOR_NETHERITE_BODY =
            registerDragonArmor("dragonarmor_netherite_body", 2);
    public static final RegistryObject<NetheriteDragonArmorItem> DRAGONARMOR_NETHERITE_TAIL =
            registerDragonArmor("dragonarmor_netherite_tail", 3);

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
    public static final RegistryObject<SeaSerpentArmorItem> SEA_SERPENT_ARMOR_NETHERITE =
            registerArmor("sea_serpent_armor_netherite", SeaSerpentArmorItem.ArmorTier.NETHERITE);
    public static final RegistryObject<SeaSerpentArmorItem> SEA_SERPENT_ARMOR_SEA_SERPENT_STEEL =
            registerArmor("sea_serpent_armor_sea_serpent_steel", SeaSerpentArmorItem.ArmorTier.SEA_SERPENT_STEEL);

    private ModItems() {}

    public static final RegistryObject<SeaSteelDragonArmorItem> DRAGONARMOR_SEASTEEL_HEAD =
            ITEMS.register("dragonarmor_sea_serpent_steel_head", () -> new SeaSteelDragonArmorItem(0));
    public static final RegistryObject<SeaSteelDragonArmorItem> DRAGONARMOR_SEASTEEL_NECK =
            ITEMS.register("dragonarmor_sea_serpent_steel_neck", () -> new SeaSteelDragonArmorItem(1));
    public static final RegistryObject<SeaSteelDragonArmorItem> DRAGONARMOR_SEASTEEL_BODY =
            ITEMS.register("dragonarmor_sea_serpent_steel_body", () -> new SeaSteelDragonArmorItem(2));
    public static final RegistryObject<SeaSteelDragonArmorItem> DRAGONARMOR_SEASTEEL_TAIL =
            ITEMS.register("dragonarmor_sea_serpent_steel_tail", () -> new SeaSteelDragonArmorItem(3));

    private static RegistryObject<NetheriteDragonArmorItem> registerDragonArmor(
            String name, int slot) {
        return ITEMS.register(name, () -> new NetheriteDragonArmorItem(slot));
    }

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
        event.accept(SEA_SERPENT_ARMOR_NETHERITE.get());
        event.accept(SEA_SERPENT_ARMOR_DRAGONSTEEL_FIRE.get());
        event.accept(SEA_SERPENT_ARMOR_DRAGONSTEEL_ICE.get());
        event.accept(SEA_SERPENT_ARMOR_DRAGONSTEEL_LIGHTNING.get());
        event.accept(SEA_SERPENT_ARMOR_SEA_SERPENT_STEEL.get());

        event.accept(DRAGONARMOR_NETHERITE_HEAD.get());
        event.accept(DRAGONARMOR_NETHERITE_NECK.get());
        event.accept(DRAGONARMOR_NETHERITE_BODY.get());
        event.accept(DRAGONARMOR_NETHERITE_TAIL.get());
        event.accept(DRAGONARMOR_SEASTEEL_HEAD.get());
        event.accept(DRAGONARMOR_SEASTEEL_NECK.get());
        event.accept(DRAGONARMOR_SEASTEEL_BODY.get());
        event.accept(DRAGONARMOR_SEASTEEL_TAIL.get());
    }
}
