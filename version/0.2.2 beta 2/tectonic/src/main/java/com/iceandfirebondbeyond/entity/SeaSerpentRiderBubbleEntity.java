package com.iceandfirebondbeyond.entity;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.util.IDragonProjectile;
import com.iceandfirebondbeyond.registry.ModEntities;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import com.iceandfirebondbeyond.util.SeaSerpentCombat;
import com.iceandfirebondbeyond.util.SeaSerpentErosion;
import com.iceandfirebondbeyond.util.SeaSerpentBreathArea;
import com.iceandfirebondbeyond.config.BondBeyondConfig;
import com.iceandfirebondbeyond.util.SeaSerpentBreathControl;
import com.iceandfirebondbeyond.util.SeaSerpentBreathTuning;
import com.iceandfirebondbeyond.util.SeaSerpentWaterSpray;
import com.iceandfirebondbeyond.network.ModNetwork;
import com.iceandfirebondbeyond.network.SeaSerpentSplashPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
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
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/** One physical droplet in a shared water stream; droplet count never multiplies impact budgets. */
public final class SeaSerpentRiderBubbleEntity extends Projectile
        implements IDragonProjectile, IEntityAdditionalSpawnData {
    public static final double FLIGHT_SPEED = SeaSerpentBreathTuning.SPEED;
    public static final int MAX_LIFE_TICKS = 80;
    private static final EntityDataAccessor<Float> DIAMETER = SynchedEntityData.defineId(
            SeaSerpentRiderBubbleEntity.class, EntityDataSerializers.FLOAT);
    private int lifeTicks;
    private double travelled;
    private boolean terrainCarrier = true;

    public SeaSerpentRiderBubbleEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public SeaSerpentRiderBubbleEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.SEA_SERPENT_RIDER_BUBBLE.get(), level);
    }

    private SeaSerpentRiderBubbleEntity(Level level, EntitySeaSerpent shooter, Vec3 velocity, float diameter, boolean leader) {
        this(ModEntities.SEA_SERPENT_RIDER_BUBBLE.get(), level);
        setOwner(shooter);
        setBubbleDiameter(diameter);
        setDeltaMovement(velocity);
        terrainCarrier = leader;
    }

    /** All sources emit a dense, moderately flared spray from the animated mouth. */
    public static boolean fire(EntitySeaSerpent serpent, Vec3 center, Vec3 direction) {
        if (serpent.level().isClientSide || !serpent.isAlive()
                || !Double.isFinite(center.lengthSqr()) || !Double.isFinite(direction.lengthSqr())
                || direction.lengthSqr() < 1.0E-8D
                || !SeaSerpentBreathControl.budget(serpent).emit(serpent.level().getGameTime())) return false;
        int stage = SeaSerpentBondData.getGrowthStage(serpent);
        float scale = Mth.clamp(serpent.getSeaSerpentScale(), .8F, 11F);
        int count = SeaSerpentWaterSpray.bubbles(stage, scale, BondBeyondConfig.BUBBLES_PER_PULSE.get());
        double spread = SeaSerpentWaterSpray.spread(stage, scale, BondBeyondConfig.BREATH_SPREAD.get());
        // Cosmetic spray is sent once from the mouth, never attached to or multiplied by live bubbles.
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, 128, serpent.level().dimension())),
                new com.iceandfirebondbeyond.network.SeaSerpentSprayPacket(
                        serpent.level().dimension().location(), center, direction.normalize(), stage, scale, (float) spread));
        boolean emitted = false;
        for (int i = 0; i < count; i++) {
            boolean leader = i == 0;
            Vec3 ray = SeaSerpentWaterSpray.direction(direction, spread,
                    i, count, serpent.getRandom().nextDouble(), serpent.getRandom().nextDouble());
            double speed = FLIGHT_SPEED * (leader ? 1 : .80D + serpent.getRandom().nextDouble() * .35D);
            float diameter = SeaSerpentBreathTuning.diameter(serpent.getSeaSerpentScale(), leader,
                    serpent.getRandom().nextDouble(), BondBeyondConfig.BUBBLE_SIZE.get());
            SeaSerpentRiderBubbleEntity bubble = new SeaSerpentRiderBubbleEntity(serpent.level(), serpent,
                    ray.scale(speed), diameter, leader);
            // No lateral spawn offsets: every droplet is collision-checked from the real mouth.
            bubble.setPos(center.x, center.y - diameter * .5D, center.z);
            emitted |= serpent.level().addFreshEntity(bubble);
        }
        return emitted;
    }

    public static boolean fireAt(EntitySeaSerpent serpent, Vec3 target) {
        Vec3 mouth = SeaSerpentBondData.getMouthPosition(serpent);
        return fire(serpent, mouth, target.subtract(mouth));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DIAMETER, 0.9F);
    }

    public float getBubbleDiameter() {
        return entityData.get(DIAMETER);
    }

    private void setBubbleDiameter(float diameter) {
        entityData.set(DIAMETER, Float.isFinite(diameter) ? Mth.clamp(diameter, 0.12F, 6.2F) : 0.9F);
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
        if (++lifeTicks > MAX_LIFE_TICKS || travelled >= SeaSerpentBreathTuning.RANGE
                || !Double.isFinite(motion.lengthSqr()) || motion.lengthSqr() < 1.0E-8D) {
            discard();
            return;
        }
        double remaining = SeaSerpentBreathTuning.RANGE - travelled;
        if (motion.length() > remaining) motion = motion.normalize().scale(remaining);
        Vec3 start = getBoundingBox().getCenter();
        Vec3 end = start.add(motion);
        if (level().isClientSide) flightParticles(start, end, motion);

        if (!level().isClientSide) {
            if (!(getOwner() instanceof EntitySeaSerpent serpent) || !serpent.isAlive()) {
                discard();
                return;
            }
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
        travelled += motion.length();
        // The same vector moves the client and server projectile, with no
        // retargeting, gravity, medium-dependent drag, or drifting particle path.
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
    }

    private void flightParticles(Vec3 start, Vec3 end, Vec3 motion) {
        double density = BondBeyondConfig.WATER_PARTICLES.get();
        if (density <= 0) return;
        boolean underwater = level().getFluidState(blockPosition()).is(FluidTags.WATER);
        int attempts = lifeTicks == 1 ? 3 : 1;
        for (int i = 0; i < attempts; i++) {
            if (random.nextDouble() > density * (lifeTicks == 1 ? 1.0D : .16D)) continue;
            Vec3 point = start.lerp(end, random.nextDouble());
            double width = getBubbleDiameter() * .25D;
            level().addParticle(underwater ? ParticleTypes.BUBBLE : ParticleTypes.SPLASH,
                    point.x + (random.nextDouble() - .5D) * width,
                    point.y + (random.nextDouble() - .5D) * width,
                    point.z + (random.nextDouble() - .5D) * width,
                    motion.x * .08D, motion.y * .08D + .015D, motion.z * .08D);
        }
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
        // Commit exactly one impact before applying callbacks or multipart damage.
        if (!(getOwner() instanceof EntitySeaSerpent source)) { discard(); return; }
        discard();
        Vec3 impact = result.getLocation();
        // A point-blank droplet can die before its first tracking update. Send
        // the confirmed splash to nearby observers even in that case.
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        impact.x, impact.y, impact.z, 128, level().dimension())),
                new SeaSerpentSplashPacket(level().dimension().location(), impact, getBubbleDiameter()));
        int stage = SeaSerpentBondData.getGrowthStage(source);
        double radius = BondBeyondConfig.AREA_DAMAGE.get()
                ? SeaSerpentBreathArea.damageRadius(stage, BondBeyondConfig.AREA_RADIUS.get()) : 0;
        if (!terrainCarrier) radius = Math.min(radius, .5D + getBubbleDiameter() * .65D);
        // Check cover BEFORE erosion changes the terrain.
        SeaSerpentCombat.breathImpact(source, result.getLocation(), getDeltaMovement(), radius,
                result instanceof EntityHitResult hit ? hit.getEntity() : null);
        if (terrainCarrier && result instanceof BlockHitResult blockHit) {
            BlockPos forge = com.iceandfirebondbeyond.block.SeaSerpentForgeBlockEntity
                    .receiveBreathAt(level(), blockHit.getBlockPos(), source);
            SeaSerpentErosion.erodeImpact(level(), blockHit.getBlockPos(), source, forge);
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
        tag.putDouble("WaterTravelled", travelled);
        tag.putBoolean("TerrainCarrier", terrainCarrier);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBubbleDiameter(tag.contains("BubbleDiameter") ? tag.getFloat("BubbleDiameter") : 0.9F);
        lifeTicks = Mth.clamp(tag.getInt("BubbleLife"), 0, MAX_LIFE_TICKS);
        travelled = tag.contains("WaterTravelled") ? tag.getDouble("WaterTravelled") : lifeTicks * 1.8D;
        if (!Double.isFinite(travelled)) travelled = SeaSerpentBreathTuning.RANGE;
        travelled = Mth.clamp(travelled, 0, SeaSerpentBreathTuning.RANGE);
        terrainCarrier = !tag.contains("TerrainCarrier") || tag.getBoolean("TerrainCarrier");
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeFloat(getBubbleDiameter());
        buffer.writeInt(getOwner() == null ? -1 : getOwner().getId());
        Vec3 motion = getDeltaMovement();
        buffer.writeDouble(motion.x);
        buffer.writeDouble(motion.y);
        buffer.writeDouble(motion.z);
        buffer.writeVarInt(lifeTicks);
        buffer.writeDouble(travelled);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        setBubbleDiameter(buffer.readFloat());
        int ownerId = buffer.readInt();
        if (ownerId >= 0) setOwner(level().getEntity(ownerId));
        setDeltaMovement(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
        lifeTicks = Mth.clamp(buffer.readVarInt(), 0, MAX_LIFE_TICKS);
        travelled = Mth.clamp(buffer.readDouble(), 0, SeaSerpentBreathTuning.RANGE);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
