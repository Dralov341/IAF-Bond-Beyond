package com.iceandfirebondbeyond.registry;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.item.IafTabRegistry;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.block.SeaSerpentForgeBlock;
import com.iceandfirebondbeyond.block.SeaSerpentForgeBlockEntity;
import com.iceandfirebondbeyond.block.SeaSerpentForgePartBlock;
import com.iceandfirebondbeyond.effect.DrowningEffect;
import com.iceandfirebondbeyond.entity.SeaSteelArrowEntity;
import com.iceandfirebondbeyond.entity.SeaSerpentCorpseEntity;
import com.iceandfirebondbeyond.inventory.SeaSerpentForgeMenu;
import com.iceandfirebondbeyond.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** New content only. Existing IAF scale blocks and tools retain their native IDs. */
public final class SeaSteelContent {
    private static final List<RegistryObject<? extends Item>> CREATIVE = new ArrayList<>();
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(
            ForgeRegistries.MOB_EFFECTS, IceAndFireBondBeyond.MOD_ID);
    public static final RegistryObject<MobEffect> DROWNING = EFFECTS.register("drowning", DrowningEffect::new);
    public static final RegistryObject<Item> BONE = item("sea_serpent_bone", () -> new Item(new Item.Properties()));
    public static final RegistryObject<SeaSerpentSkullItem> SKULL_ITEM = item("sea_serpent_skull", SeaSerpentSkullItem::new);
    public static final RegistryObject<Item> BLOOD = item("sea_serpent_blood", () -> new Item(
            new Item.Properties().craftRemainder(Items.GLASS_BOTTLE)));
    public static final RegistryObject<Item> MEAT = item("sea_serpent_meat", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(3).saturationMod(0.3F).meat().build())));
    public static final RegistryObject<Item> COOKED_MEAT = item("cooked_sea_serpent_meat", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(8).saturationMod(0.8F).meat().build())));
    public static final RegistryObject<Item> INGOT = item("sea_serpent_steel_ingot", () -> new Item(new Item.Properties().fireResistant()));
    public static final RegistryObject<Block> BONE_BLOCK = block("sea_serpent_bone_block",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.BONE_BLOCK).strength(3.0F, 9.0F)));
    public static final RegistryObject<Block> STEEL_BLOCK = block("sea_serpent_steel_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).mapColor(MapColor.COLOR_CYAN)));
    public static final RegistryObject<SeaSerpentForgeBlock> FORGE_CORE = block("sea_serpent_forge_core", SeaSerpentForgeBlock::new);
    public static final RegistryObject<Block> FORGE_INPUT = block("sea_serpent_forge_input", SeaSerpentForgePartBlock::new);
    public static final List<RegistryObject<Block>> BRICKS = List.of(
            block("sea_serpent_brick_blue", SeaSerpentForgePartBlock::new),
            block("sea_serpent_brick_bronze", SeaSerpentForgePartBlock::new),
            block("sea_serpent_brick_darkblue", SeaSerpentForgePartBlock::new),
            block("sea_serpent_brick_green", SeaSerpentForgePartBlock::new),
            block("sea_serpent_brick_purple", SeaSerpentForgePartBlock::new),
            block("sea_serpent_brick_red", SeaSerpentForgePartBlock::new),
            block("sea_serpent_brick_teal", SeaSerpentForgePartBlock::new));
    // Retain the old ID for commands/saves, without a second manual in the creative tab.
    public static final RegistryObject<GuideBookItem> GUIDE = ModItems.ITEMS.register("bond_beyond_guide", GuideBookItem::new);
    public static final RegistryObject<BlockEntityType<SeaSerpentForgeBlockEntity>> FORGE_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("sea_serpent_forge", () -> BlockEntityType.Builder.of(
                    SeaSerpentForgeBlockEntity::new, FORGE_CORE.get()).build(null));
    public static final RegistryObject<MenuType<SeaSerpentForgeMenu>> FORGE_MENU = ModMenus.MENUS.register(
            "sea_serpent_forge", () -> IForgeMenuType.create(SeaSerpentForgeMenu::new));
    public static final RegistryObject<EntityType<SeaSteelArrowEntity>> ARROW_ENTITY = ModEntities.ENTITIES.register(
            "sea_serpent_steel_arrow", () -> EntityType.Builder.<SeaSteelArrowEntity>of(SeaSteelArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1).build("sea_serpent_steel_arrow"));
    public static final RegistryObject<EntityType<SeaSerpentCorpseEntity>> CORPSE = ModEntities.ENTITIES.register(
            "sea_serpent_corpse", () -> EntityType.Builder.<SeaSerpentCorpseEntity>of(SeaSerpentCorpseEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.5F).clientTrackingRange(16).updateInterval(3).fireImmune().build("sea_serpent_corpse"));
    public static final Tier TIER = TierSortingRegistry.registerTier(new ForgeTier(4, 8000, 10.0F, 21.0F, 15,
            BlockTags.create(new ResourceLocation(IceAndFireBondBeyond.MOD_ID, "needs_sea_serpent_steel")),
            () -> Ingredient.of(INGOT.get())), new ResourceLocation(IceAndFireBondBeyond.MOD_ID, "sea_serpent_steel"),
            List.of(Tiers.NETHERITE), List.of());
    public static final RegistryObject<SwordItem> SWORD = item("sea_serpent_steel_sword", () -> new SwordItem(TIER, 3, -2.4F, steel()));
    public static final Tier BONE_TIER = new ForgeTier(3, 1660, 10.0F, 4.0F, 22,
            BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(BONE.get()));
    public static final Tier BLOOD_TIER = new ForgeTier(3, 2000, 10.0F, 5.5F, 22,
            BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(BONE.get()));
    public static final RegistryObject<SwordItem> BONE_SWORD = item("sea_serpent_bone_sword",
            () -> new SwordItem(BONE_TIER, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<SwordItem> BLOOD_SWORD = item("sea_serpent_blood_sword",
            () -> new SwordItem(BLOOD_TIER, 3, -2.4F, new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<ElementalShieldItem> SHIELD = item("sea_serpent_steel_shield",
            () -> new ElementalShieldItem(ElementalShieldItem.Element.WATER, INGOT::get));
    public static final RegistryObject<ElementalShieldItem> FIRE_SHIELD = item("dragonsteel_fire_shield",
            () -> new ElementalShieldItem(ElementalShieldItem.Element.FIRE, IafItemRegistry.DRAGONSTEEL_FIRE_INGOT::get));
    public static final RegistryObject<ElementalShieldItem> ICE_SHIELD = item("dragonsteel_ice_shield",
            () -> new ElementalShieldItem(ElementalShieldItem.Element.ICE, IafItemRegistry.DRAGONSTEEL_ICE_INGOT::get));
    public static final RegistryObject<ElementalShieldItem> LIGHTNING_SHIELD = item("dragonsteel_lightning_shield",
            () -> new ElementalShieldItem(ElementalShieldItem.Element.LIGHTNING, IafItemRegistry.DRAGONSTEEL_LIGHTNING_INGOT::get));
    public static final RegistryObject<AxeItem> AXE = item("sea_serpent_steel_axe", () -> new AxeItem(TIER, 5.0F, -3.0F, steel()));
    public static final RegistryObject<PickaxeItem> PICKAXE = item("sea_serpent_steel_pickaxe", () -> new PickaxeItem(TIER, 1, -2.8F, steel()));
    public static final RegistryObject<ShovelItem> SHOVEL = item("sea_serpent_steel_shovel", () -> new ShovelItem(TIER, 1.5F, -3.0F, steel()));
    public static final RegistryObject<HoeItem> HOE = item("sea_serpent_steel_hoe", () -> new HoeItem(TIER, -4, 0.0F, steel()));
    public static final RegistryObject<SeaSteelBowItem> BOW = item("sea_serpent_steel_bow", SeaSteelBowItem::new);
    public static final RegistryObject<SeaSteelArrowItem> ARROW = item("sea_serpent_steel_arrow", SeaSteelArrowItem::new);
    public static final RegistryObject<SeaSteelArmorItem> HELMET = item("sea_serpent_steel_helmet", () -> new SeaSteelArmorItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<SeaSteelArmorItem> CHESTPLATE = item("sea_serpent_steel_chestplate", () -> new SeaSteelArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<SeaSteelArmorItem> LEGGINGS = item("sea_serpent_steel_leggings", () -> new SeaSteelArmorItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<SeaSteelArmorItem> BOOTS = item("sea_serpent_steel_boots", () -> new SeaSteelArmorItem(ArmorItem.Type.BOOTS));
    public static final RegistryObject<DragonSeekerItem> SEEKER = item("dragon_seeker", () -> new DragonSeekerItem(0));
    public static final RegistryObject<DragonSeekerItem> EPIC_SEEKER = item("epic_dragon_seeker", () -> new DragonSeekerItem(1));
    public static final RegistryObject<DragonSeekerItem> LEGENDARY_SEEKER = item("legendary_dragon_seeker", () -> new DragonSeekerItem(2));
    public static final RegistryObject<DragonSeekerItem> GODLY_SEEKER = item("godly_dragon_seeker", () -> new DragonSeekerItem(3));

    private static Item.Properties steel() { return new Item.Properties().fireResistant(); }
    private static <T extends Item> RegistryObject<T> item(String name, Supplier<T> factory) {
        RegistryObject<T> item = ModItems.ITEMS.register(name, factory); CREATIVE.add(item); return item;
    }
    private static <T extends Block> RegistryObject<T> block(String name, Supplier<T> factory) {
        RegistryObject<T> block = ModBlocks.BLOCKS.register(name, factory);
        item(name, () -> new BlockItem(block.get(), new Item.Properties())); return block;
    }
    public static boolean isMelee(Item item) {
        return item == BLOOD_SWORD.get() || item == SWORD.get() || item == AXE.get()
                || item == PICKAXE.get() || item == SHOVEL.get() || item == HOE.get();
    }
    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (IafTabRegistry.TAB_ITEMS.getKey().equals(event.getTabKey())) CREATIVE.forEach(i -> event.accept(i.get()));
        });
    }
    private SeaSteelContent() {}
}
