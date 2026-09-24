package com.iceandfirebondbeyond.inventory;

import com.iceandfirebondbeyond.registry.SeaSteelContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class SeaSerpentForgeMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    public SeaSerpentForgeMenu(int id, Inventory player, FriendlyByteBuf buffer) {
        this(id, player, new SimpleContainer(3), new SimpleContainerData(5));
        buffer.readBlockPos();
    }
    public SeaSerpentForgeMenu(int id, Inventory player, Container container, ContainerData data) {
        super(SeaSteelContent.FORGE_MENU.get(), id);
        checkContainerSize(container, 3); checkContainerDataCount(data, 5);
        this.container = container; this.data = data;
        addSlot(new Slot(container, 0, 44, 35) { @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(0, stack); } });
        addSlot(new Slot(container, 1, 44, 62) { @Override public boolean mayPlace(ItemStack stack) { return stack.is(SeaSteelContent.BLOOD.get()); } });
        addSlot(new Slot(container, 2, 116, 45) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 161));
        addDataSlots(data);
    }
    public int progress() { return data.get(0); }
    public int duration() { return Math.max(1, data.get(1)); }
    public boolean powered() { return data.get(2) > 0; }
    public boolean assembled() { return data.get(3) > 0; }
    public int sourceStage() { return data.get(4); }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < 3) { if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY; }
        else if (stack.is(SeaSteelContent.BLOOD.get())) { if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }
}
