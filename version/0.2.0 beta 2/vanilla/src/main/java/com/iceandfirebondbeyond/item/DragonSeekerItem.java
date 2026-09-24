package com.iceandfirebondbeyond.item;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/** CE ItemDragonSeeker port: same four ranges and dead/tamed filters, Mojang/Forge names. */
public final class DragonSeekerItem extends Item {
    private static final int[] RANGE = {150, 200, 300, 500};
    private final int rank;
    public DragonSeekerItem(int rank) { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); this.rank = rank; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        player.getCooldowns().addCooldown(this, 40);
        EntityDragonBase dragon = level.getEntitiesOfClass(EntityDragonBase.class,
                player.getBoundingBox().inflate(RANGE[rank]), d -> !d.isRemoved()
                        && (rank == 0 || !d.isModelDead()) && (rank < 2 || !d.isTame()))
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (dragon == null) {
            player.displayClientMessage(Component.translatable("message.iceandfire_bond_beyond.seeker.not_found"), false);
            return InteractionResultHolder.fail(stack);
        }
        Component message = Component.translatable("message.iceandfire_bond_beyond.seeker.found");
        if (rank == 3) {
            String command = "/tp @s " + dragon.blockPosition().getX() + " " + dragon.blockPosition().getY() + " " + dragon.blockPosition().getZ();
            message = Component.translatable("message.iceandfire_bond_beyond.seeker.location",
                    dragon.blockPosition().getX(), dragon.blockPosition().getY(), dragon.blockPosition().getZ())
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
        }
        player.displayClientMessage(message, false);
        return InteractionResultHolder.success(stack);
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.iceandfire_bond_beyond.seeker." + rank, RANGE[rank]).withStyle(ChatFormatting.GRAY));
    }
}
