package com.iceandfirebondbeyond.util;

import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeItem;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

/** Inspect use hooks without executing them (which could consume or fire the item). */
public final class CorpseHarvestInteraction {
    // Entity/event-driven item actions have no Item.use override. Other mods or
    // datapacks can register those exceptions here without running the action.
    private static final TagKey<Item> USE_ACTION_ITEMS = ItemTags.create(new ResourceLocation(
            IceAndFireBondBeyond.MOD_ID, "corpse_harvest_requires_sneaking"));
    // Forge resolves the SRG names in both an IDE and the production game.
    private static final Method USE = ObfuscationReflectionHelper.findMethod(Item.class,
            "m_7203_", Level.class, Player.class, InteractionHand.class);
    private static final Method USE_ON = ObfuscationReflectionHelper.findMethod(Item.class,
            "m_6225_", UseOnContext.class);
    private static final Method INTERACT = ObfuscationReflectionHelper.findMethod(Item.class,
            "m_6880_", ItemStack.class, Player.class, LivingEntity.class, InteractionHand.class);
    private static final ClassValue<Boolean> HAS_USE_HOOK = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try {
                return overrides(type, USE) || overrides(type, USE_ON) || overrides(type, INTERACT)
                        || type.getMethod("onItemUseFirst", ItemStack.class, UseOnContext.class)
                        .getDeclaringClass() != IForgeItem.class;
            } catch (NoSuchMethodException exception) {
                // An unusual transformed item keeps its own interaction; Shift still harvests.
                return true;
            }
        }
    };

    public static boolean canHarvest(Player player, ItemStack stack) {
        return player.isShiftKeyDown() || stack.isEmpty() || !hasUseAction(stack);
    }

    private static boolean hasUseAction(ItemStack stack) {
        return stack.is(USE_ACTION_ITEMS) || stack.isEdible()
                || stack.getUseDuration() > 0 || stack.getUseAnimation() != UseAnim.NONE
                || HAS_USE_HOOK.get(stack.getItem().getClass());
    }

    private static boolean overrides(Class<?> type, Method base) throws NoSuchMethodException {
        return type.getMethod(base.getName(), base.getParameterTypes()).getDeclaringClass() != Item.class;
    }

    private CorpseHarvestInteraction() {}
}
