package com.iceandfirebondbeyond.item;

import com.iceandfirebondbeyond.entity.SeaSerpentCorpseEntity;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;
import javax.annotation.Nullable;

public final class SeaSerpentSkullItem extends Item {
    public SeaSerpentSkullItem() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!context.getPlayer().mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (!context.getLevel().isClientSide) {
            SeaSerpentCorpseEntity skull = new SeaSerpentCorpseEntity(SeaSteelContent.CORPSE.get(), context.getLevel());
            ItemStack stack = context.getItemInHand();
            skull.configureSkull(stage(stack), stack.hasTag() ? stack.getTag().getFloat("Scale") : 0);
            if (stack.hasTag() && stack.getTag().contains("AgeTicks")) skull.setSavedAgeTicks(stack.getTag().getInt("AgeTicks"));
            if (stack.hasCustomHoverName()) skull.setCustomName(stack.getHoverName());
            skull.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getPlayer().getYRot(), 0);
            if (!context.getLevel().noCollision(skull)) return InteractionResult.FAIL;
            if (!context.getLevel().addFreshEntity(skull)) return InteractionResult.FAIL;
            if (!context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    private static int stage(ItemStack stack) { return Mth.clamp(stack.hasTag() ? stack.getTag().getInt("Stage") : 1, 1, 5); }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.iceandfire_bond_beyond.skull.stage", stage(stack)).withStyle(ChatFormatting.GRAY));
        if (stack.hasTag() && stack.getTag().contains("AgeTicks"))
            lines.add(Component.translatable("tooltip.iceandfire_bond_beyond.skull.age",
                    stack.getTag().getInt("AgeTicks") / com.iceandfirebondbeyond.util.SeaSerpentBondData.TICKS_PER_DAY)
                    .withStyle(ChatFormatting.GRAY));
    }
}
