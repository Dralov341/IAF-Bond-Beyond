package com.iceandfirebondbeyond.block;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.iceandfirebondbeyond.entity.SeaSerpentRiderBubbleEntity;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentControlAccess;
import com.iceandfirebondbeyond.inventory.SeaSerpentForgeMenu;
import com.iceandfirebondbeyond.recipe.SeaSteelForgeRecipe;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import java.util.Comparator;
import javax.annotation.Nullable;

/** Submerged 3x3x3 forge: 8 bone corners, 17 same-color Sea Serpent bricks, core, input. */
public final class SeaSerpentForgeBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final String FUEL_UNTIL = "BondBeyondForgeUntil", FUEL_POS = "BondBeyondForgePos";
    private NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress, powerTicks, color = -1, sourceStage;
    private String activeRecipe = "";
    private Direction input;
    private LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);
    public final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return switch (index) {
            case 0 -> progress; case 1 -> recipe() == null ? 200 : recipe().cookTime();
            case 2 -> powerTicks; case 3 -> color + 1; case 4 -> sourceStage; default -> 0; }; }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 5; }
    };
    public SeaSerpentForgeBlockEntity(BlockPos pos, BlockState state) { super(SeaSteelContent.FORGE_ENTITY.get(), pos, state); }
    public record Structure(int color, Direction input) {}
    @Nullable public Structure inspectStructure() {
        if (level == null || !isSubmerged()) return null;
        int matched = -1;
        Direction inlet = null;
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            BlockPos pos = worldPosition.offset(x, y, z);
            if (!level.hasChunkAt(pos)) return null;
            BlockState state = level.getBlockState(pos);
            if (y != 0 && x != 0 && z != 0) {
                if (!state.is(SeaSteelContent.BONE_BLOCK.get())) return null;
            } else if (y == 0 && Math.abs(x) + Math.abs(z) == 1 && state.is(SeaSteelContent.FORGE_INPUT.get())) {
                if (inlet != null) return null;
                inlet = x == 1 ? Direction.EAST : x == -1 ? Direction.WEST : z == 1 ? Direction.SOUTH : Direction.NORTH;
            } else {
                int current = brickColor(state);
                if (current < 0 || (matched >= 0 && current != matched)) return null;
                matched = current;
            }
        }
        return inlet == null || matched < 0 ? null : new Structure(matched, inlet);
    }
    public static int brickColor(BlockState state) {
        for (int i = 0; i < SeaSteelContent.BRICKS.size(); i++) if (state.is(SeaSteelContent.BRICKS.get(i).get())) return i;
        return -1;
    }
    private boolean isSubmerged() {
        // Full blocks displace their own water. Check the water touching all
        // four outside faces and the roof instead; a solid seabed is allowed.
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            if (!hasWater(worldPosition.offset(x, 2, z))) return false;
        for (int y = -1; y <= 1; y++) for (int side = -1; side <= 1; side++) {
            if (!hasWater(worldPosition.offset(-2, y, side)) || !hasWater(worldPosition.offset(2, y, side))
                    || !hasWater(worldPosition.offset(side, y, -2)) || !hasWater(worldPosition.offset(side, y, 2))) return false;
        }
        return true;
    }
    private boolean hasWater(BlockPos pos) {
        return level.hasChunkAt(pos) && level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER);
    }
    public static boolean isFueling(EntitySeaSerpent serpent) {
        CompoundTag tag = serpent.getPersistentData();
        return !serpent.level().isClientSide && serpent.isAlive() && !serpent.isVehicle()
                && !serpent.isPassenger() && SeaSerpentBondData.isTamed(serpent)
                && SeaSerpentBondData.getCommand(serpent) != SeaSerpentBondData.COMMAND_ESCORT
                && !SeaSerpentBondData.isMountedBreachInProgress(serpent)
                && tag.getLong(FUEL_UNTIL) > serpent.level().getGameTime();
    }
    /** Only reads a loaded core; expiry releases movement even if its chunk unloads. */
    @Nullable public static Vec3 fuelingTarget(EntitySeaSerpent serpent) {
        if (!isFueling(serpent)) return null;
        BlockPos pos = BlockPos.of(serpent.getPersistentData().getLong(FUEL_POS));
        if (serpent.level().hasChunkAt(pos)
                && serpent.level().getBlockEntity(pos) instanceof SeaSerpentForgeBlockEntity forge
                && forge.input != null) return Vec3.atCenterOf(pos.relative(forge.input));
        return null;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, SeaSerpentForgeBlockEntity forge) {
        int previousProgress = forge.progress;
        String previousRecipe = forge.activeRecipe;
        if (forge.powerTicks > 0) forge.powerTicks--;
        Structure structure = forge.inspectStructure();
        forge.color = structure == null ? -1 : structure.color();
        forge.input = structure == null ? null : structure.input();
        SeaSteelForgeRecipe recipe = forge.recipe();
        boolean valid = structure != null && recipe != null && forge.canAcceptResult(recipe);
        String key = recipe == null ? "" : recipe.getId().toString();
        if (!key.equals(forge.activeRecipe)) { forge.progress = 0; forge.activeRecipe = key; }
        if (!valid) { forge.progress = 0; forge.powerTicks = 0; forge.sourceStage = 0; }
        else {
            if (level.getGameTime() % 5 == 0) forge.callSerpent();
            if (forge.powerTicks > 0) {
                forge.progress++;
                if (forge.progress >= recipe.cookTime()) {
                    ItemStack output = recipe.getResultItem(level.registryAccess());
                    if (forge.inventory.get(2).isEmpty()) forge.inventory.set(2, output.copy());
                    else forge.inventory.get(2).grow(output.getCount());
                    forge.inventory.get(0).shrink(1);
                    forge.inventory.get(1).shrink(1);
                    // Return the bottle; never overwrite the remaining blood stack.
                    BlockPos bottleExit = pos.relative(structure.input(), 2);
                    Containers.dropItemStack(level, bottleExit.getX() + 0.25, bottleExit.getY() + 0.25, bottleExit.getZ() + 0.25,
                            new ItemStack(Items.GLASS_BOTTLE));
                    forge.progress = 0;
                }
            } else forge.progress = Math.max(0, forge.progress - 1);
        }
        boolean lit = valid && forge.powerTicks > 0;
        if (state.getValue(SeaSerpentForgeBlock.ACTIVE) != lit) level.setBlock(pos, state.setValue(SeaSerpentForgeBlock.ACTIVE, lit), 3);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(side);
            BlockState part = level.getBlockState(neighbor);
            if (part.getBlock() instanceof SeaSerpentForgePartBlock && part.getValue(SeaSerpentForgePartBlock.ACTIVE) != lit)
                level.setBlock(neighbor, part.setValue(SeaSerpentForgePartBlock.ACTIVE, lit), 3);
        }
        if (forge.progress != previousProgress || !forge.activeRecipe.equals(previousRecipe)
                || level.getGameTime() % 20 == 0) forge.setChanged();
    }
    private void callSerpent() {
        if (level == null || input == null) return;
        BlockPos inlet = worldPosition.relative(input);
        Vec3 aim = Vec3.atCenterOf(inlet);
        EntitySeaSerpent serpent = level.getEntitiesOfClass(EntitySeaSerpent.class,
                new AABB(worldPosition).inflate(48.0D), s -> s.isAlive() && SeaSerpentBondData.isTamed(s)
                        && !s.isVehicle() && !s.isPassenger() && s.getTarget() == null
                        && SeaSerpentBondData.getCommand(s) != SeaSerpentBondData.COMMAND_ESCORT
                        && !SeaSerpentBondData.isMountedBreachInProgress(s)
                        && SeaSerpentBondData.getKnockbackStage(s) >= 3
                        && (!isFueling(s) || s.getPersistentData().getLong(FUEL_POS) == worldPosition.asLong()))
                .stream().filter(s -> canReachInput(s, inlet)).min(Comparator.comparingDouble(s -> s.distanceToSqr(aim))).orElse(null);
        if (serpent == null) return;
        serpent.getPersistentData().putLong(FUEL_POS, worldPosition.asLong());
        serpent.getPersistentData().putLong(FUEL_UNTIL, level.getGameTime() + 10);
        serpent.getNavigation().stop();
        if (serpent.getMoveControl() instanceof com.iceandfirebondbeyond.entity.ai.SeaSerpentBondMoveControl control)
            control.holdPosition();
        serpent.setDeltaMovement(Vec3.ZERO);
        serpent.setJumpingOutOfWater(false);
        serpent.setAnimation(EntitySeaSerpent.NO_ANIMATION);
        serpent.getLookControl().setLookAt(aim.x, aim.y, aim.z, 30.0F, 30.0F);
        serpent.setBreathing(true);
        SeaSerpentRiderBubbleEntity.fireAt(serpent, aim);
    }
    private boolean canReachInput(EntitySeaSerpent serpent, BlockPos inlet) {
        Vec3 mouth = ((SeaSerpentControlAccess) serpent).bondBeyond$getMouthPosition();
        if (mouth.distanceToSqr(Vec3.atCenterOf(inlet)) > 64.0D * 64.0D) return false;
        BlockHitResult hit = level.clip(new ClipContext(mouth, Vec3.atCenterOf(inlet), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, serpent));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(inlet);
    }
    public static boolean receiveBreath(Level level, BlockPos hit, EntitySeaSerpent serpent) {
        return receiveBreathAt(level, hit, serpent) != null;
    }
    /** Returns the actual powered core, for erosion of its surrounding seabed. */
    @Nullable public static BlockPos receiveBreathAt(Level level, BlockPos hit, EntitySeaSerpent serpent) {
        if (level.isClientSide || !serpent.isAlive() || SeaSerpentBondData.getKnockbackStage(serpent) < 3) return null;
        // The sized bubble can clip an adjacent brick at the input's edge.
        // Accept only the exposed input neighborhood of a valid assembled core.
        for (BlockPos pos : BlockPos.betweenClosed(hit.offset(-2, -1, -2), hit.offset(2, 1, 2))) {
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof SeaSerpentForgeBlockEntity forge)) continue;
            Structure structure = forge.inspectStructure();
            if (structure == null || forge.recipe() == null || !forge.canAcceptResult(forge.recipe())) continue;
            BlockPos inlet = pos.relative(structure.input());
            if (hit.distSqr(inlet) > 2.0D || !forge.canReachInput(serpent, inlet)) continue;
            forge.powerTicks = 12;
            forge.sourceStage = SeaSerpentBondData.getKnockbackStage(serpent);
            forge.setChanged();
            return pos.immutable();
        }
        return null;
    }
    @Nullable private SeaSteelForgeRecipe recipe() {
        return level == null ? null : level.getRecipeManager().getRecipeFor(SeaSteelForgeRecipe.TYPE.get(), this, level).orElse(null);
    }
    private boolean canAcceptResult(SeaSteelForgeRecipe recipe) {
        ItemStack result = recipe.getResultItem(level.registryAccess()), output = inventory.get(2);
        return output.isEmpty() || (ItemStack.isSameItemSameTags(result, output)
                && output.getCount() + result.getCount() <= Math.min(getMaxStackSize(), output.getMaxStackSize()));
    }
    @Override protected Component getDefaultName() { return Component.translatable("container.iceandfire_bond_beyond.sea_serpent_forge"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory player) { return new SeaSerpentForgeMenu(id, player, this, data); }
    @Override public int getContainerSize() { return 3; }
    @Override public boolean isEmpty() { return inventory.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return inventory.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { ItemStack result = ContainerHelper.removeItem(inventory, slot, count); setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack result = ContainerHelper.takeItem(inventory, slot); setChanged(); return result; }
    @Override public void setItem(int slot, ItemStack stack) { inventory.set(slot, stack); stack.setCount(Math.min(stack.getCount(), stack.getMaxStackSize())); setChanged(); }
    @Override public void clearContent() { inventory = NonNullList.withSize(3, ItemStack.EMPTY); setChanged(); }
    @Override public boolean stillValid(Player player) { return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 ? !stack.is(SeaSteelContent.BLOOD.get()) : slot == 1 && stack.is(SeaSteelContent.BLOOD.get()); }
    @Override public int[] getSlotsForFace(Direction face) { return face == Direction.DOWN ? new int[]{2} : face == Direction.UP ? new int[]{0} : new int[]{1}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction face) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return slot == 2; }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); ContainerHelper.saveAllItems(tag, inventory);
        tag.putInt("Progress", progress); tag.putString("Recipe", activeRecipe);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); inventory = NonNullList.withSize(3, ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, inventory);
        progress = Math.max(0, Math.min(72000, tag.getInt("Progress"))); activeRecipe = tag.getString("Recipe");
        powerTicks = 0; sourceStage = 0;
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction face) {
        if (!isRemoved() && face != null && cap == ForgeCapabilities.ITEM_HANDLER)
            return handlers[face == Direction.UP ? 0 : face == Direction.DOWN ? 1 : 2].cast();
        return super.getCapability(cap, face);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); for (var handler : handlers) handler.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); handlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH); }
}
