package com.iceandfirebondbeyond.inventory;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.item.SeaSerpentArmorItem;
import com.iceandfirebondbeyond.registry.ModMenus;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public final class SeaSerpentMenu extends AbstractContainerMenu {
    private static final int EQUIPMENT_SLOTS = 1;
    private static final int DATA_COUNT = 7;

    private static final int DATA_HUNGER = 0;
    private static final int DATA_STAGE = 1;
    private static final int DATA_DAYS = 2;
    private static final int DATA_MALE = 3;
    private static final int DATA_COMMAND = 4;
    private static final int DATA_AGING_DISABLED = 5;
    private static final int DATA_MAX_HUNGER = 6;

    @Nullable
    private final EntitySeaSerpent serpent;
    private final Container equipment;
    private final ContainerData data;
    private final String ownerName;

    public SeaSerpentMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf buffer
    ) {
        this(
                containerId,
                playerInventory,
                findSerpent(playerInventory, buffer.readVarInt()),
                new SimpleContainerData(DATA_COUNT),
                buffer.readUtf(128)
        );
    }

    public SeaSerpentMenu(
            int containerId,
            Inventory playerInventory,
            EntitySeaSerpent serpent,
            String ownerName
    ) {
        this(
                containerId,
                playerInventory,
                serpent,
                createServerData(serpent),
                ownerName
        );
    }

    private SeaSerpentMenu(
            int containerId,
            Inventory playerInventory,
            @Nullable EntitySeaSerpent serpent,
            ContainerData data,
            String ownerName
    ) {
        super(ModMenus.SEA_SERPENT.get(), containerId);
        this.serpent = serpent;
        this.data = data;
        this.ownerName = ownerName;
        this.equipment = serpent == null
                ? new SimpleContainer(EQUIPMENT_SLOTS)
                : new SeaSerpentEquipmentContainer(serpent);

        equipment.startOpen(playerInventory.player);
        addDataSlots(data);

        addSlot(new Slot(
                equipment,
                SeaSerpentBondData.ARMOR_SLOT,
                8,
                18
        ) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof SeaSerpentArmorItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        132 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(
                    playerInventory,
                    column,
                    8 + column * 18,
                    190
            ));
        }
    }

    @Nullable
    private static EntitySeaSerpent findSerpent(Inventory inventory, int entityId) {
        Entity entity = inventory.player.level().getEntity(entityId);
        return entity instanceof EntitySeaSerpent serpent ? serpent : null;
    }

    private static ContainerData createServerData(EntitySeaSerpent serpent) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_HUNGER -> SeaSerpentBondData.getHunger(serpent);
                    case DATA_STAGE -> SeaSerpentBondData.getGrowthStage(serpent);
                    case DATA_DAYS -> SeaSerpentBondData.getAgeInDays(serpent);
                    case DATA_MALE -> SeaSerpentBondData.isMale(serpent) ? 1 : 0;
                    case DATA_COMMAND -> SeaSerpentBondData.getCommand(serpent);
                    case DATA_AGING_DISABLED ->
                            SeaSerpentBondData.isAgingDisabled(serpent) ? 1 : 0;
                    case DATA_MAX_HUNGER ->
                            SeaSerpentBondData.getMaxHunger(serpent);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Server-authoritative, read-only profile values.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return serpent != null
                && serpent.isAlive()
                && serpent.distanceTo(player) < 8.0F
                && (player.level().isClientSide
                || SeaSerpentBondData.isOwner(serpent, player.getUUID()));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index < EQUIPMENT_SLOTS) {
            if (!moveItemStackTo(source, EQUIPMENT_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (source.getItem() instanceof SeaSerpentArmorItem
                && !getSlot(SeaSerpentBondData.ARMOR_SLOT).hasItem()) {
            if (!moveItemStackTo(
                    source,
                    SeaSerpentBondData.ARMOR_SLOT,
                    SeaSerpentBondData.ARMOR_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (index < EQUIPMENT_SLOTS + 27) {
            if (!moveItemStackTo(
                    source,
                    EQUIPMENT_SLOTS + 27,
                    slots.size(),
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(
                source,
                EQUIPMENT_SLOTS,
                EQUIPMENT_SLOTS + 27,
                false
        )) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, source);
        return original;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        equipment.stopOpen(player);
    }

    @Nullable
    public EntitySeaSerpent getSerpent() {
        return serpent;
    }

    public int getHunger() {
        return data.get(DATA_HUNGER);
    }

    public int getMaxHunger() {
        return data.get(DATA_MAX_HUNGER);
    }

    public int getStage() {
        return data.get(DATA_STAGE);
    }

    public int getDays() {
        return data.get(DATA_DAYS);
    }

    public boolean isMale() {
        return data.get(DATA_MALE) != 0;
    }

    public int getCommand() {
        return data.get(DATA_COMMAND);
    }

    public boolean isAgingDisabled() {
        return data.get(DATA_AGING_DISABLED) != 0;
    }

    public String getOwnerName() {
        return ownerName;
    }
}
