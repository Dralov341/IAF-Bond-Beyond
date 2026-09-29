package com.iceandfirebondbeyond.entity;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import com.iceandfirebondbeyond.util.CorpseHarvestInteraction;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import com.iceandfirebondbeyond.util.SeaSerpentDeath;
import com.iceandfirebondbeyond.util.SeaSerpentRemainsPose;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;

/** A saved, harvestable body. It has no living AI, breathing or attack methods. */
public final class SeaSerpentCorpseEntity extends Entity {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SeaSerpentCorpseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(SeaSerpentCorpseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(SeaSerpentCorpseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HARVEST = SynchedEntityData.defineId(SeaSerpentCorpseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RESTING = SynchedEntityData.defineId(SeaSerpentCorpseEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SKULL = SynchedEntityData.defineId(SeaSerpentCorpseEntity.class, EntityDataSerializers.BOOLEAN);
    private final CorpsePart[] parts;
    private long nextHarvest;
    private boolean lootEnabled = true;
    private int ageTicks;
    private SeaSerpentRemainsPose previousPose, currentPose, targetPose;

    public SeaSerpentCorpseEntity(EntityType<? extends SeaSerpentCorpseEntity> type, Level level) {
        super(type, level);
        parts = new CorpsePart[SeaSerpentRemainsPose.NAMES.length * 2];
        for (int i = 0; i < parts.length; i++) parts[i] = new CorpsePart(this);
        noCulling = true;
    }
    public static SeaSerpentCorpseEntity from(EntitySeaSerpent serpent) {
        SeaSerpentCorpseEntity body = new SeaSerpentCorpseEntity(SeaSteelContent.CORPSE.get(), serpent.level());
        body.entityData.set(SCALE, Mth.clamp(serpent.getSeaSerpentScale(), 0.8F, 16.0F));
        body.entityData.set(VARIANT, serpent.getVariant());
        body.entityData.set(STAGE, SeaSerpentBondData.getKnockbackStage(serpent));
        body.ageTicks = SeaSerpentBondData.getGrowthTicks(serpent);
        body.lootEnabled = serpent.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT);
        body.moveTo(serpent.getX(), serpent.getY(), serpent.getZ(), serpent.yBodyRot, 0);
        body.setDeltaMovement(serpent.getDeltaMovement());
        body.entityData.set(RESTING, serpent.onGround());
        if (serpent.hasCustomName()) body.setCustomName(serpent.getCustomName());
        return body;
    }
    public void configureSkull(int stage, float scale) {
        entityData.set(SKULL, true);
        entityData.set(STAGE, Mth.clamp(stage, 1, 5));
        entityData.set(SCALE, validScale(scale, getStage()));
        ageTicks = com.iceandfirebondbeyond.util.SeaSerpentGrowth.ticksForScale(getScale());
        refreshDimensions();
    }
    public void setSavedAgeTicks(int ticks) { ageTicks = Mth.clamp(ticks, 0, SeaSerpentBondData.MAX_GROWTH_TICKS); }
    @Override protected void defineSynchedData() {
        entityData.define(SCALE, 1.0F); entityData.define(VARIANT, 0);
        entityData.define(STAGE, 1); entityData.define(HARVEST, 0);
        entityData.define(RESTING, false); entityData.define(SKULL, false);
    }
    public float getScale() { return entityData.get(SCALE); }
    public int getStage() { return entityData.get(STAGE); }
    public int getVariant() { return Mth.clamp(entityData.get(VARIANT), 0, 6); }
    public int getHarvest() { return entityData.get(HARVEST); }
    public int getHarvestLimit() { return getStage() * 5; }
    public boolean isSkeleton() { return getHarvest() >= getHarvestLimit() / 2; }
    public boolean isSkull() { return entityData.get(SKULL); }
    public SeaSerpentRemainsPose getRemainsPose(float partial) {
        if (currentPose == null) {
            currentPose = calculatePose();
            previousPose = targetPose = currentPose;
        }
        return SeaSerpentRemainsPose.blend(previousPose, currentPose, partial);
    }
    private SeaSerpentRemainsPose calculatePose() {
        boolean resting = entityData.get(RESTING);
        return SeaSerpentDeath.pose(this, getScale(), getYRot(), resting ? .7F : 1, resting ? 1 : 0);
    }
    public static float scaleForStage(int stage) {
        return com.iceandfirebondbeyond.util.SeaSerpentGrowth.scaleForTicks(
                com.iceandfirebondbeyond.util.SeaSerpentGrowth.firstDay(stage) * SeaSerpentBondData.TICKS_PER_DAY);
    }
    public static float validScale(float scale, int stage) {
        return Float.isFinite(scale) && scale > 0 ? Mth.clamp(scale, .8F, 16) : scaleForStage(stage);
    }
    @Override public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(Math.max(0.4F, getScale() * (isSkull() ? .82F : .45F)),
                Math.max(0.2F, getScale() * .3F));
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key) || SKULL.equals(key)) refreshDimensions();
        if (SCALE.equals(key) || RESTING.equals(key)) targetPose = null;
    }
    @Override public void setId(int id) {
        super.setId(id);
        if (parts != null) for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
    }
    @Override public boolean isMultipartEntity() { return true; }
    @Override public PartEntity<?>[] getParts() { return parts; }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean isPushedByFluid(net.minecraftforge.fluids.FluidType type) { return false; }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) {
            SeaSerpentDeath.sink(this);
            entityData.set(RESTING, onGround());
        }
        if (!isSkull() && parts != null) {
            if (targetPose == null || tickCount % 5 == 0) targetPose = calculatePose();
            previousPose = currentPose == null ? targetPose : currentPose;
            currentPose = SeaSerpentRemainsPose.blend(previousPose, targetPose, .25F);
            for (int i = 0; i < parts.length; i++) {
                CorpsePart part = parts[i];
                Vec3 center = SeaSerpentDeath.worldPoint(this, getScale(), getYRot(), currentPose.harvestPoint(i));
                part.refreshDimensions();
                part.setPos(center.x, center.y - part.getBbHeight() / 2, center.z);
            }
        }
        if (getY() < level().getMinBuildHeight() - 64) discard();
    }
    @Override public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(isSkull() ? getScale() : getScale() * 5.0D);
    }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        return harvest(player, hand, position().add(0, getBbHeight() / 2, 0));
    }
    private InteractionResult harvest(Player player, InteractionHand hand, Vec3 dropPosition) {
        if (!player.mayBuild() || hand != InteractionHand.MAIN_HAND || isRemoved()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (isSkull()) {
            if (!level().isClientSide && player.isShiftKeyDown()) setYRot(player.getYRot());
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        boolean blood = held.is(Items.GLASS_BOTTLE) && !isSkeleton();
        if (!blood && !CorpseHarvestInteraction.canHarvest(player, held)) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (level().getGameTime() < nextHarvest) return InteractionResult.CONSUME;
        nextHarvest = level().getGameTime() + 4;
        if (!lootEnabled) return InteractionResult.CONSUME;
        int harvested = getHarvest();
        // Shared flesh budget: taking blood replaces that portion of skin/meat,
        // as with native dragon corpses, so reloads cannot replenish either.
        if (blood) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            dropLoot(new ItemStack(SeaSteelContent.BLOOD.get()), dropPosition);
            playSound(SoundEvents.BOTTLE_FILL, 0.8F, 0.85F);
        } else if (harvested == getHarvestLimit() - 1) {
            dropLoot(skullStack(), dropPosition);
            entityData.set(HARVEST, harvested + 1);
            discard();
            return InteractionResult.CONSUME;
        } else if (isSkeleton()) {
            dropLoot(new ItemStack(SeaSteelContent.BONE.get(), 1 + getStage() / 2), dropPosition);
            if ((harvested & 1) == 0) dropLoot(new ItemStack(IafItemRegistry.SERPENT_FANG.get()), dropPosition);
            playSound(SoundEvents.BONE_BLOCK_BREAK, 0.8F, 0.9F);
        } else {
            dropLoot(new ItemStack(EnumSeaSerpent.values()[getVariant()].scale.get(), 2 + getStage()), dropPosition);
            dropLoot(new ItemStack(SeaSteelContent.MEAT.get(), 1 + getStage() / 2), dropPosition);
            playSound(SoundEvents.SLIME_BLOCK_BREAK, 0.6F, 0.8F);
        }
        entityData.set(HARVEST, harvested + 1);
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResult interactAt(Player player, Vec3 point, InteractionHand hand) {
        return harvest(player, hand, position().add(point));
    }
    private void dropLoot(ItemStack stack, Vec3 at) {
        ItemEntity drop = new ItemEntity(level(), at.x, at.y + .15, at.z, stack);
        drop.setDefaultPickUpDelay();
        level().addFreshEntity(drop);
    }
    private ItemStack skullStack() {
        ItemStack skull = new ItemStack(SeaSteelContent.SKULL_ITEM.get());
        skull.getOrCreateTag().putInt("Stage", getStage());
        skull.getOrCreateTag().putFloat("Scale", getScale());
        skull.getOrCreateTag().putInt("AgeTicks", ageTicks);
        skull.getOrCreateTag().putInt("SizeVersion", 2);
        if (hasCustomName()) skull.setHoverName(getCustomName());
        return skull;
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || !isSkull() || !(source.getEntity() instanceof Player player)
                || !player.mayBuild()) return false;
        // Native IAF decorations return their item in Creative as well.
        dropLoot(skullStack(), position().add(0, getBbHeight() / 2, 0));
        discard();
        return true;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Scale", getScale()); tag.putInt("Variant", getVariant());
        tag.putInt("Stage", getStage()); tag.putInt("Harvest", getHarvest());
        tag.putBoolean("Resting", entityData.get(RESTING)); tag.putBoolean("Skull", isSkull());
        tag.putBoolean("LootEnabled", lootEnabled);
        tag.putInt("SizeVersion", 2); tag.putInt("AgeTicks", ageTicks);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(VARIANT, Mth.clamp(tag.getInt("Variant"), 0, 6));
        entityData.set(STAGE, Mth.clamp(tag.getInt("Stage"), 1, 5));
        entityData.set(SKULL, tag.getBoolean("Skull"));
        // Old placed skulls stored a display multiplier, not the animal's size.
        float savedScale = isSkull() && !tag.contains("SizeVersion") ? 0 : tag.getFloat("Scale");
        entityData.set(SCALE, validScale(savedScale, getStage()));
        ageTicks = tag.contains("AgeTicks") ? Mth.clamp(tag.getInt("AgeTicks"), 0, SeaSerpentBondData.MAX_GROWTH_TICKS)
                : com.iceandfirebondbeyond.util.SeaSerpentGrowth.ticksForScale(getScale());
        entityData.set(HARVEST, Mth.clamp(tag.getInt("Harvest"), 0, getHarvestLimit()));
        entityData.set(RESTING, tag.getBoolean("Resting")); entityData.set(SKULL, tag.getBoolean("Skull"));
        lootEnabled = !tag.contains("LootEnabled") || tag.getBoolean("LootEnabled");
        if (getHarvest() >= getHarvestLimit()) discard();
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    public static final class CorpsePart extends PartEntity<SeaSerpentCorpseEntity> {
        CorpsePart(SeaSerpentCorpseEntity parent) { super(parent); }
        @Override public EntityDimensions getDimensions(Pose pose) {
            // Overlap even the longest tail span so harvesting has no gaps.
            float size = Math.max(0.5F, getParent().getScale() * 0.75F);
            return EntityDimensions.scalable(size, size);
        }
        @Override public boolean isPickable() { return !getParent().isRemoved() && !getParent().isSkull(); }
        @Override public boolean is(Entity other) { return other == this || other == getParent(); }
        @Override public InteractionResult interact(Player player, InteractionHand hand) {
            return getParent().harvest(player, hand, position().add(0, getBbHeight() / 2, 0));
        }
        @Override public InteractionResult interactAt(Player player, Vec3 point, InteractionHand hand) {
            return getParent().harvest(player, hand, position().add(point));
        }
        @Override public boolean hurt(DamageSource source, float amount) { return getParent().hurt(source, amount); }
        @Override protected void defineSynchedData() {}
        @Override protected void readAdditionalSaveData(CompoundTag tag) {}
        @Override protected void addAdditionalSaveData(CompoundTag tag) {}
        @Override public boolean shouldBeSaved() { return false; }
    }
}
