package com.iceandfirebondbeyond.entity;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.util.IDragonProjectile;
import com.iceandfirebondbeyond.registry.ModEntities;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import com.iceandfirebondbeyond.util.SeaSerpentCombat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/** Sized, straight-flight breath projectile for Forge 1.20.1. */
public final class SeaSerpentRiderBubbleEntity extends Projectile
        implements IDragonProjectile, IEntityAdditionalSpawnData {
    public static final double FLIGHT_SPEED = 1.8D;
    public static final int MAX_LIFE_TICKS = 80; // 144-block maximum flight, in air OR water.
    private static final EntityDataAccessor<Float> DIAMETER = SynchedEntityData.defineId(
            SeaSerpentRiderBubbleEntity.class, EntityDataSerializers.FLOAT);
    private int lifeTicks;

    public SeaSerpentRiderBubbleEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public SeaSerpentRiderBubbleEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.SEA_SERPENT_RIDER_BUBBLE.get(), level);
    }

    public SeaSerpentRiderBubbleEntity(Level level, EntitySeaSerpent shooter, Vec3 direction) {
        this(ModEntities.SEA_SERPENT_RIDER_BUBBLE.get(), level);
        setOwner(shooter);
        setBubbleDiameter(diameterForScale(shooter.getSeaSerpentScale()));
        setDeltaMovement(direction.normalize().scale(FLIGHT_SPEED));
    }

    public static float diameterForScale(float serpentScale) {
        return Mth.clamp(0.65F + serpentScale * 0.22F, 0.8F, 3.1F);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DIAMETER, 0.9F);
    }

    public float getBubbleDiameter() {
        return entityData.get(DIAMETER);
    }

    private void setBubbleDiameter(float diameter) {
        entityData.set(DIAMETER, Float.isFinite(diameter) ? Mth.clamp(diameter, 0.8F, 3.1F) : 0.9F);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        float diameter = getBubbleDiameter();
        return EntityDimensions.scalable(diameter, diameter);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DIAMETER.equals(key)) refreshDimensions();
    }

    @Override
    public void tick() {
        Vec3 motion = getDeltaMovement();
        super.tick(); // Projectile bookkeeping only: no Fireball water drag or power.
        setDeltaMovement(motion);
        if (isRemoved()) return;
        if (++lifeTicks > MAX_LIFE_TICKS) {
            discard();
            return;
        }

        if (!level().isClientSide) {
            if (!(getOwner() instanceof EntitySeaSerpent serpent) || !serpent.isAlive()) {
                discard();
                return;
            }
            Vec3 start = getBoundingBox().getCenter();
            Vec3 end = start.add(motion);
            if (!level().hasChunkAt(BlockPos.containing(end))) {
                discard(); // Never load chunks on behalf of a breath projectile.
                return;
            }
            double radius = getBubbleDiameter() * 0.5D;
            BlockHitResult block = findBlockHit(start, end, radius);
            Vec3 limit = block == null ? end : block.getLocation();
            SeaSerpentCombat.TargetHit entityHit = SeaSerpentCombat.findTarget(
                    serpent, start, limit, radius, start, motion.length() + radius * Math.sqrt(3.0D), false);
            HitResult hit = block;
            // A wall wins ties; widening a shot cannot damage a mob behind it.
            if (entityHit != null && (block == null
                    || start.distanceToSqr(entityHit.rayPoint()) < start.distanceToSqr(limit))) {
                hit = new EntityHitResult(entityHit.part(), entityHit.rayPoint());
            }
            if (hit != null && !ForgeEventFactory.onProjectileImpact(this, hit)) {
                onHit(hit);
                if (isRemoved()) return;
            }
        }
        // The same vector moves the client and server projectile, with no
        // retargeting, gravity, medium-dependent drag, or drifting particle path.
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
    }

    /** Swept box collision, including the bubble's radius rather than a thin ray. */
    @Nullable
    private BlockHitResult findBlockHit(Vec3 start, Vec3 end, double radius) {
        BlockHitResult nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        Vec3 motion = end.subtract(start);
        for (VoxelShape shape : level().getBlockCollisions(this, new AABB(start, end).inflate(radius))) {
            for (AABB box : shape.toAabbs()) {
                AABB inflated = box.inflate(radius);
                Vec3 hit = inflated.contains(start) ? start : inflated.clip(start, end).orElse(null);
                if (hit == null || start.distanceToSqr(hit) >= distance) continue;
                distance = start.distanceToSqr(hit);
                nearest = new BlockHitResult(hit,
                        Direction.getNearest((float) -motion.x, (float) -motion.y, (float) -motion.z),
                        BlockPos.containing(box.getCenter()), inflated.contains(start));
            }
        }
        return nearest;
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        if (level().isClientSide || isRemoved()) return;
        // Exactly one impact, even when several parts share the same parent.
        discard();
        if (!(result instanceof EntityHitResult hit)
                || !(getOwner() instanceof EntitySeaSerpent serpent)
                || !SeaSerpentCombat.canAttack(serpent, hit.getEntity(), false)) return;
        Entity target = SeaSerpentCombat.damageTarget(hit.getEntity());
        float damage = SeaSerpentBondData.getMountedBreathDamage(serpent);
        if (target instanceof LivingEntity living) {
            SeaSerpentBondData.hurtWithMountedAbility(serpent, living, damage);
        } else if (target != null) {
            target.hurt(level().damageSources().mobAttack(serpent), damage);
        }
    }

    @Override
    public boolean isPushedByFluid() { return false; }

    @Override
    public boolean isPushedByFluid(FluidType fluid) { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) { return false; }

    @Override
    public float getPickRadius() { return 0.0F; }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BubbleDiameter", getBubbleDiameter());
        tag.putInt("BubbleLife", lifeTicks);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBubbleDiameter(tag.contains("BubbleDiameter") ? tag.getFloat("BubbleDiameter") : 0.9F);
        lifeTicks = Mth.clamp(tag.getInt("BubbleLife"), 0, MAX_LIFE_TICKS);
        setDeltaMovement(getDeltaMovement().normalize().scale(FLIGHT_SPEED));
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeFloat(getBubbleDiameter());
        buffer.writeInt(getOwner() == null ? -1 : getOwner().getId());
        Vec3 motion = getDeltaMovement();
        buffer.writeDouble(motion.x);
        buffer.writeDouble(motion.y);
        buffer.writeDouble(motion.z);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        setBubbleDiameter(buffer.readFloat());
        int ownerId = buffer.readInt();
        if (ownerId >= 0) setOwner(level().getEntity(ownerId));
        setDeltaMovement(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
