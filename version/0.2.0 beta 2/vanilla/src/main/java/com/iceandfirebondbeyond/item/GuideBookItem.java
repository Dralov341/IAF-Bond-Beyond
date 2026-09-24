package com.iceandfirebondbeyond.item;

import com.iceandfirebondbeyond.util.GuideBooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Command/legacy item: opens the same prewritten manual as the first-login gift. */
public final class GuideBookItem extends Item {
    public GuideBookItem() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack book = GuideBooks.create();
            player.setItemInHand(hand, book);
            serverPlayer.openItemGui(book, hand);
            return InteractionResultHolder.success(book);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
