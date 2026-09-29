package com.iceandfirebondbeyond.entity;

import com.iceandfirebondbeyond.config.BondBeyondConfig;
import com.iceandfirebondbeyond.util.SeaSerpentOcean;
import com.iceandfirebondbeyond.util.SeaSerpentProgress;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.github.alexthe666.iceandfire.entity.util.IBlacklistedFromStatues;
import com.github.alexthe666.iceandfire.entity.util.IDeadMob;
import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import com.google.common.collect.ImmutableList;
import com.iceandfirebondbeyond.registry.ModItems;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import com.iceandfirebondbeyond.util.OceanIncubationProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public final class SeaSerpentEggEntity extends LivingEntity
        implements IBlacklistedFromStatues, IDeadMob {

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(SeaSerpentEggEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HATCH_PROGRESS =
            SynchedEntityData.defineId(SeaSerpentEggEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID =
            SynchedEntityData.defineId(SeaSerpentEggEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> INCUBATING =
            SynchedEntityData.defineId(SeaSerpentEggEntity.class, EntityDataSerializers.BOOLEAN);

    private int incubationRemainder;

    public SeaSerpentEggEntity(EntityType<? extends SeaSerpentEggEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VARIANT, EnumSeaSerpent.BLUE.ordinal());
        entityData.define(HATCH_PROGRESS, 0);
        entityData.define(OWNER_ID, Optional.empty());
        entityData.define(INCUBATING, false);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant().ordinal());
        tag.putInt("HatchProgress", getHatchProgress());
        tag.putInt("IncubationRemainder", incubationRemainder);
        tag.putInt("IncubationRateScale", SeaSerpentProgress.PRECISION);
        UUID ownerId = getOwnerId();
        if (ownerId != null) {
            tag.putUUID("OwnerUUID", ownerId);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
        setHatchProgress(Math.max(0, tag.getInt("HatchProgress")));
        incubationRemainder = SeaSerpentProgress.migrateRemainder(tag.getInt("IncubationRemainder"),
                tag.contains("IncubationRateScale") ? tag.getInt("IncubationRateScale") : 100);
        setOwnerId(tag.hasUUID("OwnerUUID") ? tag.getUUID("OwnerUUID") : null);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        setAirSupply(getMaxAirSupply());
        boolean validLocation = isValidIncubationLocation();
        double speed = validLocation ? BondBeyondConfig.INCUBATION_SPEED.get()
                * BondBeyondConfig.INCUBATION_RATES.forBiome(level().getBiome(blockPosition())) : 0.0D;
        setIncubating(validLocation && speed > 0.0D);

        if (validLocation) {
            SeaSerpentProgress.Step progress = SeaSerpentProgress.advance(getHatchProgress(),
                    incubationRemainder, speed, BondBeyondConfig.hatchTime());
            incubationRemainder = progress.remainder();
            setHatchProgress(progress.ticks());
        }

        if (getHatchProgress() >= BondBeyondConfig.hatchTime()) {
            hatch();
            return;
        }

        if (validLocation && shouldBreakFromPressure()) {
            breakFromPressure();
        }
    }

    public boolean isValidIncubationLocation() {
        BlockPos pos = blockPosition();
        Holder<Biome> biome = level().getBiome(pos);

        if (!SeaSerpentOcean.isOcean(biome)) {
            return false;
        }

        if (!level().getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }

        for (int i = 1; i <= BondBeyondConfig.eggDepth(); i++) {
            if (!level().getFluidState(pos.above(i)).is(FluidTags.WATER)) {
                return false;
            }
        }

        return true;
    }

    private boolean shouldBreakFromPressure() {
        if ((tickCount + getId()) % OceanIncubationProfile.PRESSURE_CHECK_INTERVAL != 0) {
            return false;
        }

        float breakChance = BondBeyondConfig.pressureChance(getWaterDepthAbove());
        return breakChance > 0.0F && getRandom().nextFloat() < breakChance;
    }

    private int getWaterDepthAbove() {
        BlockPos.MutableBlockPos cursor = blockPosition().mutable();
        int waterDepth = 0;

        while (cursor.getY() < level().getMaxBuildHeight() - 1) {
            cursor.move(Direction.UP);
            if (!level().getFluidState(cursor).is(FluidTags.WATER)) {
                break;
            }
            waterDepth++;
        }

        return waterDepth;
    }

    private void breakFromPressure() {
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }

        serverLevel.sendParticles(
                new ItemParticleOption(
                        ParticleTypes.ITEM,
                        ModItems.getSeaSerpentEgg(getVariant())
                ),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                18,
                0.30D,
                0.20D,
                0.30D,
                0.08D
        );
        serverLevel.playSound(
                null,
                getX(),
                getY(),
                getZ(),
                SoundEvents.TURTLE_EGG_BREAK,
                SoundSource.NEUTRAL,
                1.2F,
                0.8F + getRandom().nextFloat() * 0.2F
        );
        discard();
    }

    private void hatch() {
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }

        EntitySeaSerpent serpent = IafEntityRegistry.SEA_SERPENT.get().create(serverLevel);
        if (serpent == null) {
            return;
        }

        CompoundTag serpentTag = new CompoundTag();
        serpent.addAdditionalSaveData(serpentTag);
        serpentTag.putInt("Variant", getVariant().ordinal());
        serpentTag.putFloat("Scale", SeaSerpentBondData.BABY_SCALE);
        serpentTag.putBoolean("Ancient", false);
        serpent.readAdditionalSaveData(serpentTag);
        serpent.setAge(-24000);
        serpent.setHealth(serpent.getMaxHealth());
        serpent.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);

        SeaSerpentBondData.markAsHatchedBaby(
                serpent,
                getRandom().nextBoolean()
        );

        if (hasCustomName()) {
            serpent.setCustomName(getCustomName());
        }

        // A protection/spawn hook may veto the baby. Keep the completed egg
        // available to retry; never delete it before the replacement exists.
        if (!serverLevel.addFreshEntity(serpent)) {
            serpent.discard();
            return;
        }
        serverLevel.playSound(
                null,
                getX(),
                getY(),
                getZ(),
                IafSoundRegistry.EGG_HATCH,
                SoundSource.NEUTRAL,
                2.5F,
                1.0F
        );
        discard();
    }

    public EnumSeaSerpent getVariant() {
        int index = entityData.get(VARIANT);
        EnumSeaSerpent[] variants = EnumSeaSerpent.values();
        return variants[Math.max(0, Math.min(index, variants.length - 1))];
    }

    public void setVariant(EnumSeaSerpent variant) {
        entityData.set(VARIANT, variant.ordinal());
    }

    public void setVariant(int variant) {
        EnumSeaSerpent[] variants = EnumSeaSerpent.values();
        entityData.set(VARIANT, Math.max(0, Math.min(variant, variants.length - 1)));
    }

    public int getHatchProgress() {
        return entityData.get(HATCH_PROGRESS);
    }

    public void setHatchProgress(int progress) {
        entityData.set(HATCH_PROGRESS, progress);
    }

    @Nullable
    public UUID getOwnerId() {
        return entityData.get(OWNER_ID).orElse(null);
    }

    public void setOwnerId(@Nullable UUID ownerId) {
        entityData.set(OWNER_ID, Optional.ofNullable(ownerId));
    }

    public boolean isIncubating() {
        return entityData.get(INCUBATING);
    }

    private void setIncubating(boolean incubating) {
        entityData.set(INCUBATING, incubating);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.DROWN)) {
            return false;
        }

        if (!level().isClientSide && !isRemoved()) {
            spawnAtLocation(ModItems.getSeaSerpentEgg(getVariant()), 0.0F);
            remove(RemovalReason.KILLED);
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getPickResult() {
        return ModItems.getSeaSerpentEgg(getVariant());
    }

    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return ImmutableList.of();
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@NotNull EquipmentSlot slot, @NotNull ItemStack stack) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
    }

    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean canBeTurnedToStone() {
        return false;
    }

    @Override
    public boolean isMobDead() {
        return true;
    }
}
