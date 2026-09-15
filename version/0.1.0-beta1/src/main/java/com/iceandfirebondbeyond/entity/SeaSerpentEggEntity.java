package com.iceandfirebondbeyond.entity;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.config.BiomeConfig;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.github.alexthe666.iceandfire.entity.util.IBlacklistedFromStatues;
import com.github.alexthe666.iceandfire.entity.util.IDeadMob;
import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import com.google.common.collect.ImmutableList;
import com.iceandfirebondbeyond.registry.ModItems;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
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
import net.minecraft.tags.BiomeTags;
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
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public final class SeaSerpentEggEntity extends LivingEntity
        implements IBlacklistedFromStatues, IDeadMob {

    private static final int INCUBATION_RATE_SCALE = 100;

    private static final int FROZEN_OCEAN_RATE = 75;
    private static final int DEEP_FROZEN_OCEAN_RATE = 80;
    private static final int COLD_OCEAN_RATE = 85;
    private static final int DEEP_COLD_OCEAN_RATE = 90;
    private static final int OCEAN_RATE = 100;
    private static final int DEEP_OCEAN_RATE = 105;
    private static final int LUKEWARM_OCEAN_RATE = 115;
    private static final int DEEP_LUKEWARM_OCEAN_RATE = 120;
    private static final int WARM_OCEAN_RATE = 130;

    private static final int SAFE_WATER_DEPTH = 20;
    private static final int PRESSURE_CHECK_INTERVAL = 100;
    private static final float PRESSURE_BREAK_CHANCE_PER_BLOCK = 0.001F;
    private static final float MAX_PRESSURE_BREAK_CHANCE = 0.15F;

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
        incubationRemainder = Math.max(
                0,
                Math.min(INCUBATION_RATE_SCALE - 1, tag.getInt("IncubationRemainder"))
        );
        setOwnerId(tag.hasUUID("OwnerUUID") ? tag.getUUID("OwnerUUID") : null);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        setAirSupply(getMaxAirSupply());
        int incubationRate = getIncubationRate();
        boolean validLocation = incubationRate > 0;
        setIncubating(validLocation);

        if (validLocation) {
            int accumulated = incubationRemainder + incubationRate;
            int addedProgress = accumulated / INCUBATION_RATE_SCALE;
            incubationRemainder = accumulated % INCUBATION_RATE_SCALE;
            setHatchProgress(getHatchProgress() + addedProgress);
        }

        if (getHatchProgress() >= IafConfig.dragonEggTime) {
            hatch();
            return;
        }

        if (validLocation && shouldBreakFromPressure()) {
            breakFromPressure();
        }
    }

    public boolean isValidIncubationLocation() {
        return getIncubationRate() > 0;
    }

    private int getIncubationRate() {
        BlockPos pos = blockPosition();
        Holder<Biome> biome = level().getBiome(pos);

        if (!isSeaSerpentOcean(biome)) {
            return 0;
        }

        if (!level().getFluidState(pos).is(FluidTags.WATER)) {
            return 0;
        }

        for (int i = 1; i <= 15; i++) {
            if (!level().getFluidState(pos.above(i)).is(FluidTags.WATER)) {
                return 0;
            }
        }

        return getBiomeIncubationRate(biome);
    }

    private static boolean isSeaSerpentOcean(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_OCEAN)) {
            return true;
        }

        try {
            return BiomeConfig.test(BiomeConfig.seaSerpentBiomes, biome);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int getBiomeIncubationRate(Holder<Biome> biome) {
        if (biome.is(Biomes.FROZEN_OCEAN)) {
            return FROZEN_OCEAN_RATE;
        }
        if (biome.is(Biomes.DEEP_FROZEN_OCEAN)) {
            return DEEP_FROZEN_OCEAN_RATE;
        }
        if (biome.is(Biomes.COLD_OCEAN)) {
            return COLD_OCEAN_RATE;
        }
        if (biome.is(Biomes.DEEP_COLD_OCEAN)) {
            return DEEP_COLD_OCEAN_RATE;
        }
        if (biome.is(Biomes.OCEAN)) {
            return OCEAN_RATE;
        }
        if (biome.is(Biomes.DEEP_OCEAN)) {
            return DEEP_OCEAN_RATE;
        }
        if (biome.is(Biomes.LUKEWARM_OCEAN)) {
            return LUKEWARM_OCEAN_RATE;
        }
        if (biome.is(Biomes.DEEP_LUKEWARM_OCEAN)) {
            return DEEP_LUKEWARM_OCEAN_RATE;
        }
        if (biome.is(Biomes.WARM_OCEAN)) {
            return WARM_OCEAN_RATE;
        }

        // Ocean từ mod: ưu tiên tag nhiệt độ chuẩn Forge, còn thiếu tag thì 1.0x.
        if (biome.is(Tags.Biomes.IS_COLD)) {
            return COLD_OCEAN_RATE;
        }
        if (biome.is(Tags.Biomes.IS_HOT)) {
            return WARM_OCEAN_RATE;
        }
        return OCEAN_RATE;
    }

    private boolean shouldBreakFromPressure() {
        if ((tickCount + getId()) % PRESSURE_CHECK_INTERVAL != 0) {
            return false;
        }

        int waterDepth = getWaterDepthAbove();
        if (waterDepth <= SAFE_WATER_DEPTH) {
            return false;
        }

        int dangerousDepth = waterDepth - SAFE_WATER_DEPTH;
        float breakChance = Math.min(
                MAX_PRESSURE_BREAK_CHANCE,
                dangerousDepth * PRESSURE_BREAK_CHANCE_PER_BLOCK
        );
        return getRandom().nextFloat() < breakChance;
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

        serverLevel.addFreshEntity(serpent);
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
