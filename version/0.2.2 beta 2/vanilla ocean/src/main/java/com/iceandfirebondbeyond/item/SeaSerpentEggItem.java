package com.iceandfirebondbeyond.item;

import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.iceandfirebondbeyond.entity.SeaSerpentEggEntity;
import com.iceandfirebondbeyond.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class SeaSerpentEggItem extends Item {
    private final EnumSeaSerpent variant;

    public SeaSerpentEggItem(EnumSeaSerpent variant) {
        super(new Item.Properties().stacksTo(1));
        this.variant = variant;
    }

    public EnumSeaSerpent getVariant() {
        return variant;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());

        if (player == null || !player.mayUseItemAt(spawnPos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }

        SeaSerpentEggEntity egg = ModEntities.SEA_SERPENT_EGG.get().create(level);
        if (egg == null) {
            return InteractionResult.FAIL;
        }

        egg.setVariant(variant);
        egg.setOwnerId(player.getUUID());
        egg.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                player.getYRot(),
                0.0F
        );

        if (!level.noCollision(egg, egg.getBoundingBox())) {
            return InteractionResult.FAIL;
        }

        if (stack.hasCustomHoverName()) {
            egg.setCustomName(stack.getHoverName());
        }

        if (!level.isClientSide) {
            if (!level.addFreshEntity(egg)) {
                egg.discard();
                return InteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
