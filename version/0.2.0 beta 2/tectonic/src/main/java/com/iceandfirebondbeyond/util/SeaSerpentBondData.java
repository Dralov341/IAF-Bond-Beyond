package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import com.iceandfirebondbeyond.entity.SeaSerpentRiderBubbleEntity;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentBondGoals;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentBondMoveControl;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentControlAccess;
import com.iceandfirebondbeyond.item.SeaSerpentArmorItem;
import com.iceandfirebondbeyond.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Persistent bond data for Ice and Fire's Sea Serpent.
 *
 * <p>The original entity is an {@code Animal}, not a tameable mob, so the
 * addon stores ownership, growth, equipment and commands in Forge's persistent
 * entity tag. Only short-lived movement state is kept in weak in-memory maps.</p>
 */
public final class SeaSerpentBondData {
    public static final int DAYS_PER_STAGE = SeaSerpentGrowth.DAYS_PER_STAGE;
    public static final int MAX_GROWTH_DAYS = SeaSerpentGrowth.MAX_DAYS;
    public static final int TICKS_PER_DAY = SeaSerpentGrowth.TICKS_PER_DAY;
    public static final int MAX_GROWTH_TICKS = MAX_GROWTH_DAYS * TICKS_PER_DAY;
    public static final int NEWBORN_SPEED_LIMIT_TICKS = 30 * 20;
    public static final int BREEDING_COOLDOWN_TICKS = 5 * 60 * 20;
    public static final double HEART_OF_THE_SEA_HEALTH_BONUS = 10.0D;
    private static final int UNDERWATER_BREACH_TIMEOUT_TICKS = 30 * 20;
    private static final int WILD_BREACH_TIMEOUT_TICKS = 10 * 20;
    private static final float WILD_GROUND_SLAM_DAMAGE = 50.0F;

    // Larger stages have a larger stomach. Stage 1 intentionally keeps the
    // old 100-point cap so existing hatchlings retain the same early balance.
    private static final int[] HUNGER_CAP_BY_STAGE = {
            0,
            100,
            250,
            450,
            700,
            1000
    };

    public static final int COMMAND_STAND = 0;
    public static final int COMMAND_SIT = 1;
    public static final int COMMAND_ESCORT = 2;

    public static final int ARMOR_SLOT = 0;

    public static final float BABY_SCALE = SeaSerpentGrowth.BABY_SCALE;
    public static final float ANCIENT_SCALE = SeaSerpentGrowth.ANCIENT_SCALE;
    public static final float MAX_SCALE = SeaSerpentGrowth.MAX_SCALE;

    private static final String OWNER_UUID = "BondBeyondOwnerUUID";
    private static final String HATCHED = "BondBeyondHatched";
    private static final String GENDER_SET = "BondBeyondGenderSet";
    private static final String MALE = "BondBeyondMale";
    private static final String GROWTH_TICKS = "BondBeyondGrowthTicks";
    private static final String HATCH_AGE_TICKS = "BondBeyondHatchAgeTicks";
    private static final String HUNGER = "BondBeyondHunger";
    private static final String HUNGER_SET = "BondBeyondHungerSet";
    private static final String HEART_HEALTH_BONUS =
            "BondBeyondHeartHealthBonus";
    private static final String BREEDING_TUTORIAL_SHOWN =
            "BondBeyondBreedingTutorialShown";
    private static final String SLAM_COOLDOWN_END =
            "BondBeyondSlamCooldownEnd";
    private static final String AGING_DISABLED = "BondBeyondAgingDisabled";
    private static final String COMMAND = "BondBeyondCommand";
    private static final String COMMAND_SET = "BondBeyondCommandSet";
    // Kept only long enough to refund saddles stored by the previous test build.
    private static final String LEGACY_SADDLE_ITEM = "BondBeyondSaddleItem";
    private static final String ARMOR_ITEM = "BondBeyondArmorItem";
    private static final String ARMOR_TIER = "BondBeyondArmorTier";
    private static final String HAS_HOME = "BondBeyondHasHome";
    private static final String HOME_X = "BondBeyondHomeX";
    private static final String HOME_Y = "BondBeyondHomeY";
    private static final String HOME_Z = "BondBeyondHomeZ";
    private static final String HOME_DIMENSION = "BondBeyondHomeDimension";

    // Render snapshot populated on clients by the equipment sync packet.
    private static final String CLIENT_ARMOR_TIER = "BondBeyondClientArmorTier";
    private static final String CLIENT_BREACH = "BondBeyondClientBreach";
    // Only these presentation/interaction fields cross the network, never arbitrary NBT.
    private static final String[] CLIENT_STATE_KEYS = {OWNER_UUID, HATCHED, GENDER_SET, MALE,
            GROWTH_TICKS, HATCH_AGE_TICKS, HUNGER, HUNGER_SET, AGING_DISABLED, COMMAND, COMMAND_SET};

    private static final UUID ARMOR_MODIFIER_UUID =
            UUID.fromString("4de8b476-8b69-48f9-b525-caf9d99653b5");
    private static final UUID GROWTH_KNOCKBACK_MODIFIER_UUID =
            UUID.fromString("647591a0-b5cd-4db5-a9b1-3f5d9b91d42c");
    private static final UUID HEART_HEALTH_MODIFIER_UUID =
            UUID.fromString("046d90ca-a9c7-4fc7-b5ab-59d014ab2884");

    private static final Map<EntitySeaSerpent, Vec3> LAST_MOTION_CACHE =
            new WeakHashMap<>();
    private static final Map<EntitySeaSerpent, Integer> LAST_ARMOR_CACHE =
            new WeakHashMap<>();
    private static final Map<EntitySeaSerpent, Double> LAST_HEALTH_BONUS_CACHE =
            new WeakHashMap<>();
    private static final Map<EntitySeaSerpent, RiderInput> RIDER_INPUT = new WeakHashMap<>();
    private static final Map<EntitySeaSerpent, Long> NEXT_MOUNTED_BITE = new WeakHashMap<>();
    private static final Set<EntitySeaSerpent> PENDING_MOUNTED_BITE =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<EntitySeaSerpent> MOUNTED_ABILITY_ACTIVE =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<EntitySeaSerpent, Long> NEXT_MOUNTED_SHOT =
            new WeakHashMap<>();
    private static final Set<EntitySeaSerpent> MOUNTED_BREACH_ACTIVE =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<EntitySeaSerpent, Long> MOUNTED_BREACH_STARTED =
            new WeakHashMap<>();
    private static final Map<EntitySeaSerpent, UnderwaterBreachAscent>
            MOUNTED_BREACH_ASCENT = new WeakHashMap<>();
    private static final Map<EntitySeaSerpent, LockedBreachTrajectory>
            MOUNTED_BREACH_TRAJECTORY = new WeakHashMap<>();
    private static final Set<EntitySeaSerpent> MOUNTED_BREACH_AIRBORNE =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<EntitySeaSerpent> MOUNTED_BREACH_AI_LOCK =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<EntitySeaSerpent> WILD_BREACH_AIRBORNE =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<EntitySeaSerpent, Long> WILD_BREACH_STARTED =
            new WeakHashMap<>();
    private static final Map<EntitySeaSerpent, Boolean> WILD_WAS_IN_WATER =
            new WeakHashMap<>();
    private static final ThreadLocal<EntitySeaSerpent> MOUNTED_ABILITY_ATTACK =
            new ThreadLocal<>();

    private SeaSerpentBondData() {
    }

    public static void markAsHatchedBaby(EntitySeaSerpent serpent, boolean male) {
        CompoundTag data = serpent.getPersistentData();
        data.remove(OWNER_UUID);
        data.putBoolean(HATCHED, true);
        setMale(serpent, male);
        data.putInt(GROWTH_TICKS, 0);
        data.putInt(HATCH_AGE_TICKS, 0);
        data.putBoolean(HUNGER_SET, true);
        data.putInt(HUNGER, 50);
        data.remove(HEART_HEALTH_BONUS);
        data.remove(BREEDING_TUTORIAL_SHOWN);
        data.remove(SLAM_COOLDOWN_END);
        data.putBoolean(AGING_DISABLED, false);
        data.putBoolean(COMMAND_SET, true);
        data.putInt(COMMAND, COMMAND_STAND);
        data.remove(LEGACY_SADDLE_ITEM);
        data.remove(ARMOR_ITEM);
        data.putInt(ARMOR_TIER, 0);
        LAST_MOTION_CACHE.remove(serpent);
        LAST_ARMOR_CACHE.remove(serpent);
        LAST_HEALTH_BONUS_CACHE.remove(serpent);
        clearOwnerFollowState(serpent);
        clearMountedAbility(serpent);
        clearMountedBreach(serpent);
        applyGrowthScale(serpent);
    }

    public static void ensureDefaults(EntitySeaSerpent serpent) {
        ensureGender(serpent);
        CompoundTag data = serpent.getPersistentData();
        refundLegacySaddle(serpent, data);
        if (wasHatched(serpent) && !data.getBoolean(HUNGER_SET)) {
            data.putBoolean(HUNGER_SET, true);
            data.putInt(HUNGER, 50);
        }
        if (isTamed(serpent) && !data.getBoolean(COMMAND_SET)) {
            data.putBoolean(COMMAND_SET, true);
            data.putInt(COMMAND, COMMAND_ESCORT);
        }
    }

    /** Called after native worldgen/finalizeSpawn has chosen scale and Ancient status. */
    public static void initializeWildAge(EntitySeaSerpent serpent, boolean loadedFromDisk) {
        CompoundTag data = serpent.getPersistentData();
        if (serpent.level().isClientSide || wasHatched(serpent) || data.contains(GROWTH_TICKS)) return;
        float scale = serpent.getSeaSerpentScale();
        int ticks = SeaSerpentGrowth.ticksForScale(scale);
        if (!loadedFromDisk) {
            // A preset maximum-sized animal stays at its maximum; other spawns
            // keep their chosen stage and get a random day within that stage.
            ticks = SeaSerpentGrowth.randomSpawnTicks(scale, bound -> serpent.getRandom().nextInt(bound));
        }
        data.putInt(GROWTH_TICKS, ticks);
        data.putInt(HATCH_AGE_TICKS, ticks);
        // Migration infers age without resizing an already saved wild serpent.
        if (!loadedFromDisk) applyGrowthScale(serpent);
    }

    private static void refundLegacySaddle(
            EntitySeaSerpent serpent,
            CompoundTag data
    ) {
        if (serpent.level().isClientSide || !data.contains(LEGACY_SADDLE_ITEM)) {
            return;
        }
        ItemStack oldSaddle = ItemStack.of(data.getCompound(LEGACY_SADDLE_ITEM));
        data.remove(LEGACY_SADDLE_ITEM);
        if (!oldSaddle.isEmpty()) {
            serpent.spawnAtLocation(oldSaddle.copy());
        }
    }

    public static void ensureGender(EntitySeaSerpent serpent) {
        CompoundTag data = serpent.getPersistentData();
        if (!data.getBoolean(GENDER_SET) && !serpent.level().isClientSide) {
            setMale(serpent, serpent.getRandom().nextBoolean());
        }
    }

    public static void setMale(EntitySeaSerpent serpent, boolean male) {
        CompoundTag data = serpent.getPersistentData();
        data.putBoolean(GENDER_SET, true);
        data.putBoolean(MALE, male);
    }

    public static boolean isMale(EntitySeaSerpent serpent) {
        ensureGender(serpent);
        return serpent.getPersistentData().getBoolean(MALE);
    }

    public static boolean isFemale(EntitySeaSerpent serpent) {
        return !isMale(serpent);
    }

    @Nullable
    public static UUID getOwnerId(EntitySeaSerpent serpent) {
        CompoundTag data = serpent.getPersistentData();
        return data.hasUUID(OWNER_UUID) ? data.getUUID(OWNER_UUID) : null;
    }

    @Nullable
    public static Player getOwner(EntitySeaSerpent serpent) {
        UUID ownerId = getOwnerId(serpent);
        return ownerId == null ? null : serpent.level().getPlayerByUUID(ownerId);
    }

    public static boolean isOwner(EntitySeaSerpent serpent, UUID playerId) {
        UUID ownerId = getOwnerId(serpent);
        return ownerId != null && ownerId.equals(playerId);
    }

    public static boolean isTamed(EntitySeaSerpent serpent) {
        return getOwnerId(serpent) != null;
    }

    public static void tame(EntitySeaSerpent serpent, UUID ownerId) {
        CompoundTag data = serpent.getPersistentData();
        data.putUUID(OWNER_UUID, ownerId);
        data.putBoolean(COMMAND_SET, true);
        data.putInt(COMMAND, COMMAND_ESCORT);
        serpent.setPersistenceRequired();
        serpent.setTarget(null);
        serpent.setBreathing(false);
        serpent.attackDecision = true;
        serpent.getNavigation().stop();
    }

    public static boolean wasHatched(EntitySeaSerpent serpent) {
        return serpent.getPersistentData().getBoolean(HATCHED);
    }

    public static boolean isNewborn(EntitySeaSerpent serpent) {
        return wasHatched(serpent)
                && getHatchAgeTicks(serpent) < NEWBORN_SPEED_LIMIT_TICKS;
    }

    public static int getGrowthTicks(EntitySeaSerpent serpent) {
        return Mth.clamp(
                serpent.getPersistentData().getInt(GROWTH_TICKS),
                0,
                MAX_GROWTH_TICKS
        );
    }

    public static int getHatchAgeTicks(EntitySeaSerpent serpent) {
        return Math.max(0, serpent.getPersistentData().getInt(HATCH_AGE_TICKS));
    }

    public static int getAgeInDays(EntitySeaSerpent serpent) {
        return getGrowthTicks(serpent) / TICKS_PER_DAY;
    }

    public static int getGrowthStage(EntitySeaSerpent serpent) {
        return SeaSerpentGrowth.stageForTicks(getGrowthTicks(serpent));
    }

    public static boolean isFullyGrown(EntitySeaSerpent serpent) {
        return getGrowthTicks(serpent) >= MAX_GROWTH_TICKS;
    }

    /** Age and scale share the same boundaries; also works before spawn initialization. */
    public static int getKnockbackStage(EntitySeaSerpent serpent) {
        return SeaSerpentGrowth.stageForScale(serpent.getSeaSerpentScale());
    }

    /** Equal-sized wild and hatch-raised adults resist the same incoming impacts. */
    public static double getGrowthKnockbackResistance(EntitySeaSerpent serpent) {
        return switch (getKnockbackStage(serpent)) {
            case 4 -> 0.99D;
            case 5 -> 1.0D;
            default -> 0.0D;
        };
    }

    /** Normal entity shoves do not consult the vanilla knockback attribute. */
    public static double getCollisionPushMultiplier(EntitySeaSerpent serpent) {
        return 1.0D - getGrowthKnockbackResistance(serpent);
    }

    public static void tickGrowthKnockbackModifier(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide) return;
        AttributeInstance attribute = serpent.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute == null) return;
        double resistance = getGrowthKnockbackResistance(serpent);
        AttributeModifier current = attribute.getModifier(GROWTH_KNOCKBACK_MODIFIER_UUID);
        if (resistance == 0.0D && current == null) return;
        if (current != null && current.getAmount() == resistance) return;
        attribute.removeModifier(GROWTH_KNOCKBACK_MODIFIER_UUID);
        if (resistance > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(
                    GROWTH_KNOCKBACK_MODIFIER_UUID, "Bond Beyond Sea Serpent mass", resistance,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    public static boolean isAgingDisabled(EntitySeaSerpent serpent) {
        return serpent.getPersistentData().getBoolean(AGING_DISABLED);
    }

    public static void setAgingDisabled(EntitySeaSerpent serpent, boolean disabled) {
        serpent.getPersistentData().putBoolean(AGING_DISABLED, disabled);
    }

    public static int getHunger(EntitySeaSerpent serpent) {
        ensureDefaults(serpent);
        return Mth.clamp(
                serpent.getPersistentData().getInt(HUNGER),
                0,
                getMaxHunger(serpent)
        );
    }

    public static int getMaxHunger(EntitySeaSerpent serpent) {
        int stage = Mth.clamp(getGrowthStage(serpent), 1, 5);
        return HUNGER_CAP_BY_STAGE[stage];
    }

    public static void setHunger(EntitySeaSerpent serpent, int hunger) {
        CompoundTag data = serpent.getPersistentData();
        data.putBoolean(HUNGER_SET, true);
        data.putInt(HUNGER, Mth.clamp(hunger, 0, getMaxHunger(serpent)));
    }

    public static void addHunger(EntitySeaSerpent serpent, int amount) {
        setHunger(serpent, getHunger(serpent) + amount);
    }

    /**
     * Ice and Fire uses this configurable value as a 125-day Stage 5
     * dragon's maximum health (500 by default).
     */
    public static double getStageFiveDragonHealthCap() {
        return Math.max(1.0D, IafConfig.dragonHealth);
    }

    public static double getStoredHeartHealthBonus(
            EntitySeaSerpent serpent
    ) {
        return Math.max(
                0.0D,
                serpent.getPersistentData().getDouble(HEART_HEALTH_BONUS)
        );
    }

    public static boolean hasReachedHeartHealthCap(
            EntitySeaSerpent serpent
    ) {
        return serpent.getMaxHealth()
                >= getStageFiveDragonHealthCap() - 1.0E-3D;
    }

    /** Stage 5 is the maximum stage; the final 25 days only finish its scale. */
    public static boolean hasBreedingStats(EntitySeaSerpent serpent) {
        return wasHatched(serpent)
                && isTamed(serpent)
                && getGrowthStage(serpent) >= 5
                && hasReachedHeartHealthCap(serpent);
    }

    public static boolean isReadyForBreeding(EntitySeaSerpent serpent) {
        return hasBreedingStats(serpent)
                && serpent.getAge() == 0
                && serpent.isAlive()
                && !serpent.isVehicle();
    }

    public static boolean wasBreedingTutorialShown(
            EntitySeaSerpent serpent
    ) {
        return serpent.getPersistentData().getBoolean(
                BREEDING_TUTORIAL_SHOWN
        );
    }

    public static void markBreedingTutorialShown(
            EntitySeaSerpent serpent
    ) {
        serpent.getPersistentData().putBoolean(
                BREEDING_TUTORIAL_SHOWN,
                true
        );
    }

    /**
     * Adds up to ten immediate max-health points. The stored investment is
     * retained even when natural growth later replaces part of the modifier;
     * the effective total can never pass a Stage 5 dragon's health cap.
     */
    public static float increaseMaxHealthWithHeart(
            EntitySeaSerpent serpent
    ) {
        if (!wasHatched(serpent)
                || !isTamed(serpent)
                || serpent.level().isClientSide) {
            return 0.0F;
        }

        tickHeartHealthModifier(serpent);
        double remaining = getStageFiveDragonHealthCap()
                - serpent.getMaxHealth();
        if (remaining <= 1.0E-4D) {
            return 0.0F;
        }

        double added = Math.min(HEART_OF_THE_SEA_HEALTH_BONUS, remaining);
        CompoundTag data = serpent.getPersistentData();
        data.putDouble(
                HEART_HEALTH_BONUS,
                getStoredHeartHealthBonus(serpent) + added
        );
        LAST_HEALTH_BONUS_CACHE.remove(serpent);
        tickHeartHealthModifier(serpent);
        serpent.heal((float) added);
        return (float) added;
    }

    /** Reapplies the persistent Heart of the Sea bonus after loads/growth. */
    public static void tickHeartHealthModifier(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide) {
            return;
        }

        AttributeInstance healthAttribute =
                serpent.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttribute == null) {
            return;
        }

        double effectiveBonus = Math.min(
                getStoredHeartHealthBonus(serpent),
                Math.max(
                        0.0D,
                        getStageFiveDragonHealthCap()
                                - healthAttribute.getBaseValue()
                )
        );
        AttributeModifier current = healthAttribute.getModifier(
                HEART_HEALTH_MODIFIER_UUID
        );
        Double cached = LAST_HEALTH_BONUS_CACHE.get(serpent);
        if (cached != null
                && Double.compare(cached, effectiveBonus) == 0
                && ((effectiveBonus <= 0.0D && current == null)
                || (effectiveBonus > 0.0D
                && current != null
                && Double.compare(current.getAmount(), effectiveBonus) == 0))) {
            return;
        }

        healthAttribute.removeModifier(HEART_HEALTH_MODIFIER_UUID);
        if (effectiveBonus > 0.0D) {
            healthAttribute.addTransientModifier(new AttributeModifier(
                    HEART_HEALTH_MODIFIER_UUID,
                    "Bond Beyond Heart of the Sea health",
                    effectiveBonus,
                    AttributeModifier.Operation.ADDITION
            ));
        }
        LAST_HEALTH_BONUS_CACHE.put(serpent, effectiveBonus);
        if (serpent.getHealth() > serpent.getMaxHealth()) {
            serpent.setHealth(serpent.getMaxHealth());
        }
    }

    public static int getCommand(EntitySeaSerpent serpent) {
        ensureDefaults(serpent);
        return Mth.clamp(serpent.getPersistentData().getInt(COMMAND), 0, 2);
    }

    public static void setCommand(EntitySeaSerpent serpent, int command) {
        int safeCommand = Mth.clamp(command, 0, 2);
        CompoundTag data = serpent.getPersistentData();
        data.putBoolean(COMMAND_SET, true);
        data.putInt(COMMAND, safeCommand);
        clearOwnerFollowState(serpent);
        if (safeCommand == COMMAND_SIT) {
            suppressCombat(serpent);
        }
        if (serpent instanceof SeaSerpentControlAccess access) {
            access.bondBeyond$updateNavigation();
        }
        if (serpent.getMoveControl() instanceof SeaSerpentBondMoveControl control) {
            control.resetHeading();
        }
        ModNetwork.syncEquipment(serpent);
    }

    public static int cycleCommand(EntitySeaSerpent serpent) {
        int next = (getCommand(serpent) + 1) % 3;
        setCommand(serpent, next);
        return next;
    }

    public static boolean hasHome(EntitySeaSerpent serpent) {
        return serpent.getPersistentData().getBoolean(HAS_HOME);
    }

    public static void clearHome(EntitySeaSerpent serpent) {
        serpent.getPersistentData().putBoolean(HAS_HOME, false);
    }

    public static void setHomeHere(EntitySeaSerpent serpent) {
        CompoundTag data = serpent.getPersistentData();
        BlockPos pos = serpent.blockPosition();
        data.putBoolean(HAS_HOME, true);
        data.putInt(HOME_X, pos.getX());
        data.putInt(HOME_Y, pos.getY());
        data.putInt(HOME_Z, pos.getZ());
        data.putString(HOME_DIMENSION, serpent.level().dimension().location().toString());
    }

    public static BlockPos getHomePos(EntitySeaSerpent serpent) {
        CompoundTag data = serpent.getPersistentData();
        return new BlockPos(data.getInt(HOME_X), data.getInt(HOME_Y), data.getInt(HOME_Z));
    }

    public static String getHomeDimension(EntitySeaSerpent serpent) {
        return serpent.getPersistentData().getString(HOME_DIMENSION);
    }

    public static void tickGrowthAndHunger(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide || !serpent.isAlive()) {
            return;
        }

        ensureDefaults(serpent);
        int hatchAgeTicks = getHatchAgeTicks(serpent);
        if (hatchAgeTicks < Integer.MAX_VALUE) {
            serpent.getPersistentData().putInt(HATCH_AGE_TICKS, hatchAgeTicks + 1);
        }

        int growthTicks = getGrowthTicks(serpent);
        if (!isAgingDisabled(serpent) && growthTicks < MAX_GROWTH_TICKS) {
            growthTicks++;
            serpent.getPersistentData().putInt(GROWTH_TICKS, growthTicks);
            if (growthTicks % 200 == 0 || growthTicks == MAX_GROWTH_TICKS) {
                applyGrowthScale(serpent);
            }
        }

        // Match Ice and Fire's IafDragonLogic: one hunger point is removed
        // every configured dragonHungerTickRate ticks (3000 by default).
        int hungerRate = IafConfig.dragonHungerTickRate;
        if (isTamed(serpent)
                && hungerRate > 0
                && serpent.tickCount % hungerRate == 0
                && getHunger(serpent) > 0) {
            setHunger(serpent, getHunger(serpent) - 1);
        }

        if (!wasHatched(serpent)) return;
        if (getGrowthStage(serpent) < 2) {
            serpent.setAge(-24000);
        } else if (serpent.getAge() < 0) {
            serpent.setAge(0);
        }
    }

    public static void tickCommandAndFollow(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide) {
            return;
        }
        if (serpent instanceof SeaSerpentControlAccess access) {
            access.bondBeyond$updateNavigation();
        }
        if (wasHatched(serpent) && isTamed(serpent) && !serpent.isVehicle()
                && getCommand(serpent) == COMMAND_STAND) {
            tickHomeReturn(serpent);
        }
        // Follow/sit are real selector goals now, not competing tick events.
    }

    private static void clearOwnerFollowState(EntitySeaSerpent serpent) {
        serpent.getNavigation().stop();
        if (serpent.getMoveControl() instanceof SeaSerpentBondMoveControl control) {
            control.holdPosition();
        }
    }

    private static void tickHomeReturn(EntitySeaSerpent serpent) {
        if (com.iceandfirebondbeyond.block.SeaSerpentForgeBlockEntity.isFueling(serpent)
                || !hasHome(serpent)
                || !getHomeDimension(serpent).equals(
                serpent.level().dimension().location().toString())) {
            return;
        }

        BlockPos home = getHomePos(serpent);
        if (serpent.distanceToSqr(
                home.getX() + 0.5D,
                home.getY() + 0.5D,
                home.getZ() + 0.5D
        ) > 32.0D * 32.0D && (serpent.tickCount + serpent.getId()) % 20 == 0) {
            serpent.getNavigation().moveTo(
                    home.getX() + 0.5D,
                    home.getY() + 0.5D,
                    home.getZ() + 0.5D,
                    0.8D
            );
        }
    }

    public static void tickCombatRules(EntitySeaSerpent serpent) {
        if (!hasRestrictedCombat(serpent) || serpent.level().isClientSide) {
            return;
        }

        if (isNewborn(serpent)) {
            suppressCombat(serpent);
            return;
        }
        if (!isTamed(serpent)) {
            return;
        }
        if (getCommand(serpent) == COMMAND_SIT || serpent.isVehicle()) {
            suppressCombat(serpent);
            return;
        }

        LivingEntity current = serpent.getTarget();
        if (current != null && !isAllowedCombatTarget(serpent, current)) {
            suppressCombat(serpent);
            current = null;
        }
        if (current == null) {
            LivingEntity requested = getRequestedOwnerTarget(serpent);
            if (requested != null) {
                serpent.setTarget(requested);
            }
        }
    }

    public static boolean hasRestrictedCombat(EntitySeaSerpent serpent) {
        return wasHatched(serpent) || isTamed(serpent);
    }

    public static boolean isAllowedCombatTarget(
            EntitySeaSerpent serpent,
            @Nullable LivingEntity target
    ) {
        if (target == null || !target.isAlive() || target.isSpectator() || target == serpent) {
            return false;
        }
        if (isNewborn(serpent)) {
            return false;
        }
        if (!isTamed(serpent)) {
            return true;
        }
        if (getCommand(serpent) == COMMAND_SIT || serpent.isVehicle()
                || isOwnedAlly(serpent, target) || serpent.isAlliedTo(target)) {
            return false;
        }

        Player owner = getOwner(serpent);
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            return false;
        }
        return target == owner.getLastHurtMob() || target == owner.getLastHurtByMob();
    }

    @Nullable
    private static LivingEntity getRequestedOwnerTarget(EntitySeaSerpent serpent) {
        Player owner = getOwner(serpent);
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            return null;
        }

        LivingEntity ownerTarget = owner.getLastHurtMob();
        if (ownerTarget != null && isAllowedCombatTarget(serpent, ownerTarget)) {
            return ownerTarget;
        }
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker != null && isAllowedCombatTarget(serpent, attacker)) {
            return attacker;
        }
        return null;
    }

    private static boolean isOwnedAlly(EntitySeaSerpent serpent, LivingEntity target) {
        UUID ownerId = getOwnerId(serpent);
        if (ownerId == null) {
            return false;
        }
        if (target instanceof Player player && ownerId.equals(player.getUUID())) {
            return true;
        }
        if (target instanceof TamableAnimal tameable
                && ownerId.equals(tameable.getOwnerUUID())) {
            return true;
        }
        return target instanceof EntitySeaSerpent other
                && ownerId.equals(getOwnerId(other));
    }

    public static void suppressCombat(EntitySeaSerpent serpent) {
        serpent.setTarget(null);
        serpent.setBreathing(false);
        serpent.attackDecision = true;
    }

    public static boolean addGrowthDays(EntitySeaSerpent serpent, int days) {
        if (!wasHatched(serpent) || days <= 0 || isFullyGrown(serpent)
                || isAgingDisabled(serpent)) {
            return false;
        }

        long addedTicks = (long) days * TICKS_PER_DAY;
        int newTicks = (int) Math.min(
                MAX_GROWTH_TICKS,
                getGrowthTicks(serpent) + addedTicks
        );
        serpent.getPersistentData().putInt(GROWTH_TICKS, newTicks);
        applyGrowthScale(serpent);
        return true;
    }

    /** Stages 1..5: [0.8,2.1), [2.1,3.4), [3.4,4.7), [4.7,6), [6,11]. */
    public static void applyGrowthScale(EntitySeaSerpent serpent) {
        int growthTicks = getGrowthTicks(serpent);
        float ageInDays = growthTicks / (float) TICKS_PER_DAY;
        float scale = SeaSerpentGrowth.scaleForTicks(growthTicks);
        float healthRatio = serpent.getMaxHealth() <= 0.0F
                ? 1.0F
                : serpent.getHealth() / serpent.getMaxHealth();

        // Ice and Fire beta-5 keeps setSeaSerpentScale private; its own NBT
        // read path is the stable way to update scale and dependent attributes.
        CompoundTag entityData = new CompoundTag();
        serpent.addAdditionalSaveData(entityData);
        entityData.putFloat("Scale", scale);
        entityData.putBoolean("Ancient", ageInDays >= 100.0F);
        serpent.readAdditionalSaveData(entityData);
        serpent.setHealth(Mth.clamp(
                serpent.getMaxHealth() * healthRatio,
                1.0F,
                serpent.getMaxHealth()
        ));
        serpent.refreshDimensions();
    }

    public static MovementLimits getMovementLimits(EntitySeaSerpent serpent) {
        if (isNewborn(serpent)) {
            return new MovementLimits(0.10D, 0.006D);
        }

        return switch (getGrowthStage(serpent)) {
            case 1 -> new MovementLimits(0.14D, 0.010D);
            case 2 -> new MovementLimits(0.20D, 0.014D);
            case 3 -> new MovementLimits(0.28D, 0.020D);
            case 4 -> new MovementLimits(0.70D, 0.050D);
            default -> new MovementLimits(1.00D, 0.075D);
        };
    }

    public static void limitSwimMovementByStage(EntitySeaSerpent serpent) {
        if (!wasHatched(serpent) || serpent.level().isClientSide) {
            return;
        }
        // Ice and Fire's breach animation needs its full launch velocity.
        // Clamping it here would turn the flip into a small, broken hop.
        if (serpent.isJumpingOutOfWater()
                || isMountedBreachInProgress(serpent)) {
            LAST_MOTION_CACHE.remove(serpent);
            return;
        }
        if (!serpent.isInWater()) {
            LAST_MOTION_CACHE.remove(serpent);
            return;
        }

        MovementLimits limits = getMovementLimits(serpent);
        Vec3 previous = serpent.getMoveControl() instanceof SeaSerpentBondMoveControl control
                && SeaSerpentBondGoals.controlsMovement(serpent)
                ? control.getPreviousMotion()
                : LAST_MOTION_CACHE.getOrDefault(serpent, Vec3.ZERO);
        Vec3 limited = limitMotion(
                previous,
                serpent.getDeltaMovement(),
                limits
        );
        serpent.setDeltaMovement(limited);
        LAST_MOTION_CACHE.put(serpent, limited);
    }

    public static Vec3 limitMotion(Vec3 previous, Vec3 requested, MovementLimits limits) {
        Vec3 acceleration = requested.subtract(previous);
        double accelerationLength = acceleration.length();
        if (accelerationLength > limits.maxAcceleration()) {
            acceleration = acceleration.scale(limits.maxAcceleration() / accelerationLength);
        }

        Vec3 limited = previous.add(acceleration);
        double speed = limited.length();
        if (speed > limits.maxSpeed()) {
            limited = limited.scale(limits.maxSpeed() / speed);
        }
        return limited;
    }

    public static boolean canBeRidden(EntitySeaSerpent serpent) {
        return wasHatched(serpent)
                && isTamed(serpent)
                && getGrowthStage(serpent) >= 3
                && getArmorTierFromServerData(serpent) != null;
    }

    public static void tickRiderControl(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide) {
            return;
        }
        if (!(serpent.getFirstPassenger() instanceof Player rider)) {
            RIDER_INPUT.remove(serpent);
            PENDING_MOUNTED_BITE.remove(serpent);
            clearMountedAbility(serpent);
            return;
        }
        if (!canBeRidden(serpent) || !isOwner(serpent, rider.getUUID())) {
            RIDER_INPUT.remove(serpent);
            PENDING_MOUNTED_BITE.remove(serpent);
            clearMountedAbility(serpent);
            rider.stopRiding();
            return;
        }
        suppressCombat(serpent);
        setMountedAbilityActive(serpent, rider, getRiderInput(serpent).breath());
        // Velocity is set by the hooked MoveControl immediately before travel.
    }

    public static void positionRiderOnSaddle(Player rider, EntitySeaSerpent serpent) {
        if (!serpent.hasPassenger(rider)) {
            return;
        }
        // Tabula BodyUpper Y=11 plus animator's +9, body plate top Y=-3.
        // The painted saddle sits one model pixel below the old raised seat.
        // LivingEntityRenderer translates 1.501 model units before rendering.
        double scale = serpent.getSeaSerpentScale();
        double saddleHeight = Math.max(0.25D, scale * 0.4385D - 0.35D);
        double forward = scale * 0.1875D;
        double yaw = Math.toRadians(serpent.yBodyRot);
        rider.setPos(serpent.getX() - Math.sin(yaw) * forward,
                serpent.getY() + saddleHeight, serpent.getZ() + Math.cos(yaw) * forward);
    }

    public static ItemStack getArmorStack(EntitySeaSerpent serpent) {
        CompoundTag data = serpent.getPersistentData();
        return data.contains(ARMOR_ITEM)
                ? ItemStack.of(data.getCompound(ARMOR_ITEM))
                : ItemStack.EMPTY;
    }

    public static void setArmorStack(EntitySeaSerpent serpent, ItemStack stack) {
        CompoundTag data = serpent.getPersistentData();
        if (stack.isEmpty() || !(stack.getItem() instanceof SeaSerpentArmorItem armor)) {
            data.remove(ARMOR_ITEM);
            data.putInt(ARMOR_TIER, 0);
        } else {
            ItemStack one = stack.copy();
            one.setCount(1);
            data.put(ARMOR_ITEM, one.save(new CompoundTag()));
            data.putInt(ARMOR_TIER, armor.getTier().ordinal() + 1);
        }
    }

    @Nullable
    public static SeaSerpentArmorItem.ArmorTier getArmorTier(EntitySeaSerpent serpent) {
        int encoded;
        if (serpent.level().isClientSide
                && serpent.getPersistentData().contains(CLIENT_ARMOR_TIER)) {
            encoded = serpent.getPersistentData().getInt(CLIENT_ARMOR_TIER);
        } else {
            encoded = serpent.getPersistentData().getInt(ARMOR_TIER);
        }
        return SeaSerpentArmorItem.ArmorTier.fromNetworkId(encoded);
    }

    public static int getArmorTierNetworkId(EntitySeaSerpent serpent) {
        SeaSerpentArmorItem.ArmorTier tier = getArmorTierFromServerData(serpent);
        return tier == null ? 0 : tier.ordinal() + 1;
    }

    @Nullable
    private static SeaSerpentArmorItem.ArmorTier getArmorTierFromServerData(
            EntitySeaSerpent serpent
    ) {
        int encoded = serpent.getPersistentData().getInt(ARMOR_TIER);
        SeaSerpentArmorItem.ArmorTier tier =
                SeaSerpentArmorItem.ArmorTier.fromNetworkId(encoded);
        if (tier == null) {
            ItemStack armor = getArmorStack(serpent);
            if (armor.getItem() instanceof SeaSerpentArmorItem armorItem) {
                tier = armorItem.getTier();
                serpent.getPersistentData().putInt(ARMOR_TIER, tier.ordinal() + 1);
            }
        }
        return tier;
    }

    public static CompoundTag createClientState(EntitySeaSerpent serpent) {
        CompoundTag state = new CompoundTag();
        CompoundTag data = serpent.getPersistentData();
        for (String key : CLIENT_STATE_KEYS) {
            if (data.contains(key)) state.put(key, data.get(key).copy());
        }
        state.putBoolean(CLIENT_BREACH, isMountedBreachInProgress(serpent));
        return state;
    }

    public static void setClientEquipmentState(EntitySeaSerpent serpent, int armorTier,
            @Nullable CompoundTag state) {
        if (!serpent.level().isClientSide) return;
        CompoundTag data = serpent.getPersistentData();
        data.putInt(CLIENT_ARMOR_TIER, armorTier);
        data.putInt(ARMOR_TIER, armorTier);
        if (state == null) return;
        for (String key : CLIENT_STATE_KEYS) {
            if (state.contains(key)) data.put(key, state.get(key).copy());
            else data.remove(key);
        }
        data.putBoolean(CLIENT_BREACH, state.getBoolean(CLIENT_BREACH));
    }

    public static void setMountedAbilityActive(
            EntitySeaSerpent serpent,
            Player rider,
            boolean active
    ) {
        if (serpent.level().isClientSide) {
            return;
        }
        if (!active
                || serpent.getFirstPassenger() != rider
                || !isOwner(serpent, rider.getUUID())
                || !canBeRidden(serpent)) {
            clearMountedAbility(serpent);
            return;
        }

        if (MOUNTED_ABILITY_ACTIVE.add(serpent)) {
            if (serpent.getAnimation() == EntitySeaSerpent.ANIMATION_ROAR) {
                AnimationHandler.INSTANCE.sendAnimationMessage(serpent, EntitySeaSerpent.NO_ANIMATION);
                serpent.setAnimationTick(0);
            }
            NEXT_MOUNTED_SHOT.put(
                    serpent,
                    serpent.level().getGameTime()
            );
            serpent.playSound(
                    IafSoundRegistry.SEA_SERPENT_BREATH,
                    4.0F,
                    1.0F
            );
        }
    }

    public static void tickMountedAbility(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide
                || !MOUNTED_ABILITY_ACTIVE.contains(serpent)) {
            return;
        }
        if (!(serpent.getFirstPassenger() instanceof Player rider)
                || !isOwner(serpent, rider.getUUID())
                || !canBeRidden(serpent)) {
            clearMountedAbility(serpent);
            return;
        }

        // Breath pauses during the breach and resumes automatically if the
        // rider is still holding the fire key after landing.
        if (serpent.isJumpingOutOfWater()
                || isMountedBreachInProgress(serpent)
                || serpent.getAnimation() == EntitySeaSerpent.ANIMATION_BITE) {
            serpent.setBreathing(false);
            return;
        }

        serpent.setBreathing(true);
        long gameTime = serpent.level().getGameTime();
        long nextShot = NEXT_MOUNTED_SHOT.getOrDefault(serpent, gameTime);
        if (gameTime < nextShot) {
            return;
        }
        NEXT_MOUNTED_SHOT.put(
                serpent,
                gameTime + getMountedBreathIntervalTicks(serpent)
        );

        Vec3 muzzle = getMouthPosition(serpent);
        Vec3 direction = getMountedAimPoint(serpent, rider).subtract(muzzle).normalize();
        if (direction.lengthSqr() < 1.0E-7D) {
            return;
        }
        SeaSerpentRiderBubbleEntity bubble =
                new SeaSerpentRiderBubbleEntity(
                        serpent.level(),
                        serpent,
                        direction
                );
        // Entity positions are at the bottom of the box. Put its CENTER at the
        // mouth so scaling the bubble does not shift the shot above the crosshair.
        bubble.setPos(muzzle.x, muzzle.y - bubble.getBubbleDiameter() * 0.5D, muzzle.z);
        serpent.level().addFreshEntity(bubble);

        if (serpent.tickCount % 40 == 0) {
            serpent.playSound(IafSoundRegistry.SEA_SERPENT_BREATH, 4.0F, 1.0F);
        }
    }

    /** Raycast from the rider's eyes, then converge from the mouth to that hit point. */
    public static Vec3 getMountedAimPoint(EntitySeaSerpent serpent, Player rider) {
        Vec3 start = rider.getEyePosition();
        Vec3 end = start.add(rider.getLookAngle().scale(128.0D));
        HitResult block = serpent.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        SeaSerpentCombat.TargetHit hit = SeaSerpentCombat.findTarget(serpent,
                start, end, 0.3D, start, 128.5D, false);
        return hit == null ? end : hit.contact();
    }

    public static boolean shouldUseMountedBreath(EntitySeaSerpent serpent) {
        return !serpent.level().isClientSide
                && MOUNTED_ABILITY_ACTIVE.contains(serpent)
                && getRiderInput(serpent).breath()
                && serpent.getFirstPassenger() instanceof Player rider
                && isOwner(serpent, rider.getUUID()) && canBeRidden(serpent)
                && !isMountedBreachInProgress(serpent) && !serpent.isJumpingOutOfWater()
                && serpent.getAnimation() != EntitySeaSerpent.ANIMATION_BITE;
    }

    public static void acceptRiderInput(EntitySeaSerpent serpent, Player rider,
            float forward, float strafe, boolean up, boolean down, boolean breath, boolean bite) {
        if (serpent.level().isClientSide || serpent.getFirstPassenger() != rider
                || !isOwner(serpent, rider.getUUID()) || !canBeRidden(serpent)
                || !Float.isFinite(forward) || !Float.isFinite(strafe)) {
            return;
        }
        RIDER_INPUT.put(serpent, new RiderInput(rider.getUUID(),
                Mth.clamp(forward, -1.0F, 1.0F), Mth.clamp(strafe, -1.0F, 1.0F),
                up, down, breath, bite, serpent.level().getGameTime()));
    }

    public static RiderInput getRiderInput(EntitySeaSerpent serpent) {
        RiderInput input = RIDER_INPUT.get(serpent);
        if (input == null || serpent.level().getGameTime() - input.receivedAt() > 30L
                || !(serpent.getFirstPassenger() instanceof Player rider)
                || !rider.getUUID().equals(input.riderId())) {
            return RiderInput.IDLE;
        }
        return input;
    }

    public static Vec3 getMouthPosition(EntitySeaSerpent serpent) {
        if (serpent.getFirstPassenger() instanceof Player rider
                && isOwner(serpent, rider.getUUID())) {
            return SeaSerpentRiderPose.mouth(serpent, rider);
        }
        return serpent instanceof SeaSerpentControlAccess access
                ? access.bondBeyond$getMouthPosition() : serpent.getEyePosition();
    }

    /** Uses the original 15-tick BITE animation; impact is on its native tick 6. */
    public static void tickMountedBite(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide) {
            return;
        }
        if (!(serpent.getFirstPassenger() instanceof Player rider)
                || !isOwner(serpent, rider.getUUID()) || !canBeRidden(serpent)
                || isMountedBreachInProgress(serpent) || serpent.isJumpingOutOfWater()) {
            PENDING_MOUNTED_BITE.remove(serpent);
            return;
        }
        if (PENDING_MOUNTED_BITE.contains(serpent)) {
            if (serpent.getAnimation() != EntitySeaSerpent.ANIMATION_BITE) {
                PENDING_MOUNTED_BITE.remove(serpent);
            } else if (serpent.getAnimationTick() >= 6) {
                PENDING_MOUNTED_BITE.remove(serpent); // At most one hit per animation.
                LivingEntity target = findMountedBiteTarget(serpent, rider);
                if (target != null) {
                    hurtWithMountedAbility(serpent, target, getMountedBiteDamage(serpent));
                }
            }
        }
        long now = serpent.level().getGameTime();
        if (getRiderInput(serpent).bite()
                && serpent.getAnimation() != EntitySeaSerpent.ANIMATION_BITE
                && now >= NEXT_MOUNTED_BITE.getOrDefault(serpent, 0L)) {
            NEXT_MOUNTED_BITE.put(serpent, now + 20L);
            PENDING_MOUNTED_BITE.add(serpent);
            serpent.setAnimation(EntitySeaSerpent.ANIMATION_BITE);
            serpent.setAnimationTick(0);
            serpent.setBreathing(false);
            serpent.setTarget(null); // Native hurtMob must not deal a second hit.
        }
    }

    /**
     * Growth already updates IAF's ATTACK_DAMAGE from scale and Ancient status.
     * Default stage ranges: 4..8.4, 8.4..13.6, 13.6..18.8, 18.8..24, 36..66.
     * Read the attribute so config and combat modifiers still work; multiplying
     * by stage again would double-count growth and overwhelm the main slam.
     */
    public static float getMountedBiteDamage(EntitySeaSerpent serpent) {
        return (float) Math.max(0.0D, serpent.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    /** Full lateral width, not a radius. Scale 11: 24 blocks. */
    public static double getMountedBiteWidth(EntitySeaSerpent serpent) {
        return 2.0D + Mth.clamp(serpent.getSeaSerpentScale(), BABY_SCALE, MAX_SCALE) * 2.0D;
    }

    /** Full height perpendicular to aim. Scale 11: 18.5 blocks. */
    public static double getMountedBiteHeight(EntitySeaSerpent serpent) {
        return 2.0D + Mth.clamp(serpent.getSeaSerpentScale(), BABY_SCALE, MAX_SCALE) * 1.5D;
    }

    /** Forward reach measured from the animated mouth. Scale 11: 67 blocks. */
    public static double getMountedBiteReach(EntitySeaSerpent serpent) {
        return 12.0D + Mth.clamp(serpent.getSeaSerpentScale(), BABY_SCALE, MAX_SCALE) * 5.0D;
    }

    /** Small overlap behind the muzzle; the posed chain covers the rest of the neck/body. */
    public static double getMountedBiteRearReach(EntitySeaSerpent serpent) {
        return 1.0D + Mth.clamp(serpent.getSeaSerpentScale(), BABY_SCALE, MAX_SCALE);
    }

    @Nullable
    private static LivingEntity findMountedBiteTarget(EntitySeaSerpent serpent, Player rider) {
        Vec3[] anchors = SeaSerpentRiderPose.biteAnchors(serpent, rider);
        Vec3 mouth = anchors[anchors.length - 1];
        // beta-5's root is only 0.5*scale wide/high. Its Tabula neck and snout
        // extend about 2.3*scale FORWARD of that root, while the saddle eye is
        // higher and much farther back. Do not centre a melee volume on that eye
        // or require the victim's torso to lie under a narrow eye ray.
        SeaSerpentBiteVolume volume = new SeaSerpentBiteVolume(anchors, rider.getLookAngle(),
                rider.getYRot(), getMountedBiteReach(serpent), getMountedBiteRearReach(serpent),
                getMountedBiteWidth(serpent), getMountedBiteHeight(serpent));
        SeaSerpentCombat.TargetHit hit = SeaSerpentCombat.findBiteTarget(serpent, rider, mouth, volume);
        return hit != null && hit.target() instanceof LivingEntity living ? living : null;
    }

    /**
     * Continuous rider breath has no ability cooldown. Its integer fire
     * interval gives Stage 3 about 2.2 shots/s, Stage 4 5.0-6.7 shots/s,
     * and Stage 5 6.7-10.0 shots/s.
     */
    public static int getMountedBreathIntervalTicks(
            EntitySeaSerpent serpent
    ) {
        int stage = getGrowthStage(serpent);
        float progress = getProgressWithinStage(serpent, stage);
        return switch (stage) {
            case 3 -> 9;
            case 4 -> Math.round(Mth.lerp(progress, 4.0F, 3.0F));
            case 5 -> Math.round(Mth.lerp(progress, 3.0F, 2.0F));
            default -> 9;
        };
    }

    /**
     * IAF beta-5 continuous dragon breath uses stage * configured elemental damage.
     * Use the fire-dragon baseline (default 2), interpolate growth up to stage 5,
     * and retain normal hurt immunity. At defaults: 6..8, 8..10, and 10 damage.
     * Higher stage-5 shot cadence improves tracking, not immunity-bypassing DPS.
     */
    public static float getMountedBreathDamage(EntitySeaSerpent serpent) {
        int stage = getGrowthStage(serpent);
        float effectiveStage = Math.min(5.0F, stage + getProgressWithinStage(serpent, stage));
        return (float) (effectiveStage * Math.max(0.0D, IafConfig.dragonAttackDamageFire));
    }

    private static float getProgressWithinStage(
            EntitySeaSerpent serpent,
            int stage
    ) {
        float ageInDays = getGrowthTicks(serpent) / (float) TICKS_PER_DAY;
        float stageStart = (Mth.clamp(stage, 1, 5) - 1) * DAYS_PER_STAGE;
        return Mth.clamp(
                (ageInDays - stageStart) / DAYS_PER_STAGE,
                0.0F,
                1.0F
        );
    }

    /** Raw slam damage before armor and other normal damage reductions. */
    public static float getMountedSlamDamage(EntitySeaSerpent serpent) {
        int stage = getGrowthStage(serpent);
        float progress = getProgressWithinStage(serpent, stage);
        return switch (stage) {
            case 3 -> Mth.lerp(progress, 35.0F, 50.0F);
            case 4 -> Mth.lerp(progress, 55.0F, 80.0F);
            case 5 -> Mth.lerp(progress, 90.0F, 120.0F);
            default -> 35.0F;
        };
    }

    /** Larger stages trade their stronger slam for a deliberately long wait. */
    public static int getMountedSlamCooldownTicks(
            EntitySeaSerpent serpent
    ) {
        int stage = getGrowthStage(serpent);
        float progress = getProgressWithinStage(serpent, stage);
        float seconds = switch (stage) {
            case 3 -> Mth.lerp(progress, 25.0F, 30.0F);
            case 4 -> Mth.lerp(progress, 35.0F, 45.0F);
            case 5 -> Mth.lerp(progress, 50.0F, 60.0F);
            default -> 25.0F;
        };
        return Math.round(seconds * 20.0F);
    }

    public static long getMountedSlamCooldownRemainingTicks(
            EntitySeaSerpent serpent
    ) {
        CompoundTag data = serpent.getPersistentData();
        long gameTime = serpent.level().getGameTime();
        long remaining = Math.max(
                0L,
                data.getLong(SLAM_COOLDOWN_END) - gameTime
        );
        if (remaining == 0L) {
            data.remove(SLAM_COOLDOWN_END);
        }
        return remaining;
    }

    /**
     * Starts a mounted breach from any sensible environment. At the water
     * surface it launches immediately, deep underwater it first performs a
     * server-controlled ascent, and on land it launches into a ground slam.
     */
    public static boolean tryStartMountedBreach(
            EntitySeaSerpent serpent,
            Player rider
    ) {
        if (serpent.level().isClientSide
                || serpent.getFirstPassenger() != rider
                || !isOwner(serpent, rider.getUUID())
                || !canBeRidden(serpent)) {
            return false;
        }

        if (MOUNTED_BREACH_ACTIVE.contains(serpent)
                || MOUNTED_BREACH_ASCENT.containsKey(serpent)
                || serpent.isJumpingOutOfWater()) {
            return false;
        }

        long remaining = getMountedSlamCooldownRemainingTicks(serpent);
        if (remaining > 0L) {
            rider.displayClientMessage(Component.translatable(
                    "message.iceandfire_bond_beyond.sea_serpent.ability_cooldown",
                    (remaining + 19L) / 20L
            ), true);
            return false;
        }

        LockedBreachTrajectory trajectory =
                getLockedBreachTrajectory(rider);
        if (canBreachFromWaterSurface(serpent)) {
            launchMountedBreach(serpent, trajectory);
            return true;
        }

        if (serpent.isInWater()) {
            Double surfaceY = findOpenWaterSurfaceY(serpent);
            if (surfaceY == null) {
                return false;
            }
            beginUnderwaterBreachAscent(
                    serpent,
                    trajectory,
                    surfaceY
            );
            return true;
        }

        if (!serpent.onGround()) {
            return false;
        }

        launchMountedBreach(serpent, trajectory);
        return true;
    }

    /**
     * Reads the rider's look ray once. A ray aimed more vertically than
     * horizontally requests a straight-up underwater ascent; its yaw still
     * supplies the forward heading for the automatic post-surface lunge.
     */
    private static LockedBreachTrajectory getLockedBreachTrajectory(
            Player rider
    ) {
        Vec3 lookRay = rider.getLookAngle().normalize();
        Vec3 horizontalRay = new Vec3(lookRay.x, 0.0D, lookRay.z);
        double horizontalLengthSqr = horizontalRay.lengthSqr();
        boolean verticalAscent = horizontalLengthSqr <= 0.5D;

        Vec3 forward;
        if (horizontalLengthSqr >= 1.0E-7D) {
            forward = horizontalRay.normalize();
        } else {
            double yaw = Math.toRadians(rider.getYRot());
            forward = new Vec3(
                    -Math.sin(yaw),
                    0.0D,
                    Math.cos(yaw)
            );
        }
        return new LockedBreachTrajectory(forward, verticalAscent);
    }

    private static void beginUnderwaterBreachAscent(
            EntitySeaSerpent serpent,
            LockedBreachTrajectory trajectory,
            double surfaceY
    ) {
        MOUNTED_BREACH_ASCENT.put(
                serpent,
                new UnderwaterBreachAscent(
                        trajectory,
                        surfaceY,
                        serpent.level().getGameTime()
                )
        );
        setMountedBreachAiLocked(serpent, true);
        serpent.getNavigation().stop();
        serpent.setBreathing(false);
        LAST_MOTION_CACHE.remove(serpent);
        applyLockedBreachYaw(serpent, trajectory);
        applyUnderwaterAscentMotion(serpent, trajectory);
    }

    private static void launchMountedBreach(
            EntitySeaSerpent serpent,
            LockedBreachTrajectory trajectory
    ) {
        MOUNTED_BREACH_ASCENT.remove(serpent);
        MOUNTED_BREACH_AIRBORNE.remove(serpent);
        MOUNTED_BREACH_TRAJECTORY.put(serpent, trajectory);

        applyLockedBreachYaw(serpent, trajectory);

        double forwardSpeed = getMountedBreachForwardSpeed(serpent);
        double upwardSpeed = getMountedBreachUpwardSpeed(serpent);
        Vec3 launchMotion = trajectory.forward().scale(forwardSpeed).add(
                0.0D,
                upwardSpeed,
                0.0D
        );
        serpent.setDeltaMovement(launchMotion);
        // Ice and Fire owns the jump pose: its aiStep derives XRot from this
        // motion and advances jumpRot while JumpingOutOfWater is true.
        serpent.setJumpingOutOfWater(true);
        int cooldownTicks = getMountedSlamCooldownTicks(serpent);
        serpent.jumpCooldown = cooldownTicks;
        serpent.getNavigation().stop();
        LAST_MOTION_CACHE.remove(serpent);
        MOUNTED_BREACH_ACTIVE.add(serpent);
        setMountedBreachAiLocked(serpent, true);
        long gameTime = serpent.level().getGameTime();
        MOUNTED_BREACH_STARTED.put(serpent, gameTime);
        serpent.getPersistentData().putLong(
                SLAM_COOLDOWN_END,
                gameTime + cooldownTicks
        );
    }

    private static boolean canBreachFromWaterSurface(
            EntitySeaSerpent serpent
    ) {
        BlockPos pos = serpent.blockPosition();
        // Swimming over a wave/block edge can lift the feet just out of water
        // for a tick. Accept the water immediately below instead of dropping
        // G until the rider brakes and sinks back in. No mid-air relaunches
        // away from water; the existing active-breach/cooldown guards still run.
        if (!serpent.level().getFluidState(pos).is(FluidTags.WATER)
                && serpent.level().getBlockState(pos).isAir()) {
            pos = pos.below();
        }
        if (!serpent.level().getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }
        return hasVerticalAirClearance(serpent, pos.above());
    }

    @Nullable
    private static Double findOpenWaterSurfaceY(EntitySeaSerpent serpent) {
        BlockPos.MutableBlockPos cursor = serpent.blockPosition().mutable();
        if (!serpent.level().getFluidState(cursor).is(FluidTags.WATER)) {
            return null;
        }

        int highestSearchY = serpent.level().getMaxBuildHeight() - 1;
        while (cursor.getY() < highestSearchY
                && serpent.level().getFluidState(cursor).is(FluidTags.WATER)) {
            cursor.move(Direction.UP);
        }

        if (cursor.getY() >= highestSearchY
                || !hasVerticalAirClearance(serpent, cursor)) {
            return null;
        }
        return (double) cursor.getY() - 1.0D;
    }

    private static boolean hasVerticalAirClearance(
            EntitySeaSerpent serpent,
            BlockPos firstAirBlock
    ) {
        int clearance = Math.max(2, Mth.ceil(serpent.getBbHeight()) + 1);
        for (int i = 0; i < clearance; i++) {
            if (!serpent.level().getBlockState(firstAirBlock.above(i)).isAir()) {
                return false;
            }
        }
        return true;
    }

    private static void applyUnderwaterAscentMotion(
            EntitySeaSerpent serpent,
            LockedBreachTrajectory trajectory
    ) {
        serpent.setDeltaMovement(getUnderwaterAscentMotion(
                serpent,
                trajectory
        ));
    }

    private static Vec3 getUnderwaterAscentMotion(
            EntitySeaSerpent serpent,
            LockedBreachTrajectory trajectory
    ) {
        double ascentSpeed = switch (getGrowthStage(serpent)) {
            case 3 -> 0.42D;
            case 4 -> 0.49D;
            default -> 0.56D;
        };
        double horizontalSpeed = trajectory.verticalAscent()
                ? 0.0D
                : switch (getGrowthStage(serpent)) {
                    case 3 -> 0.28D;
                    case 4 -> 0.34D;
                    default -> 0.40D;
                };
        return new Vec3(
                trajectory.forward().x * horizontalSpeed,
                ascentSpeed,
                trajectory.forward().z * horizontalSpeed
        );
    }

    private static double getMountedBreachForwardSpeed(
            EntitySeaSerpent serpent
    ) {
        // Air drag reduces this rapidly; these initial speeds produce a
        // roughly 14-20 block forward commitment before the landing arc.
        return switch (getGrowthStage(serpent)) {
            case 3 -> 1.35D;
            case 4 -> 1.60D;
            default -> 1.85D;
        };
    }

    private static double getMountedBreachUpwardSpeed(
            EntitySeaSerpent serpent
    ) {
        return switch (getGrowthStage(serpent)) {
            case 3 -> 1.00D;
            case 4 -> 1.10D;
            default -> 1.20D;
        };
    }

    private static void applyLockedBreachYaw(
            EntitySeaSerpent serpent,
            LockedBreachTrajectory trajectory
    ) {
        Vec3 forward = trajectory.forward();
        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        serpent.setYRot(yaw);
        serpent.yBodyRot = yaw;
        serpent.yHeadRot = yaw;
    }

    public static void tickMountedBreach(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide) {
            return;
        }

        UnderwaterBreachAscent ascent = MOUNTED_BREACH_ASCENT.get(serpent);
        if (ascent != null) {
            tickUnderwaterBreachAscent(serpent, ascent);
            return;
        }

        if (!MOUNTED_BREACH_ACTIVE.contains(serpent)) {
            return;
        }

        if (!(serpent.getFirstPassenger() instanceof Player rider)
                || !isOwner(serpent, rider.getUUID())
                || !canBeRidden(serpent)) {
            serpent.setJumpingOutOfWater(false);
            clearMountedBreach(serpent);
            return;
        }

        LockedBreachTrajectory trajectory =
                MOUNTED_BREACH_TRAJECTORY.get(serpent);
        if (trajectory == null) {
            serpent.setJumpingOutOfWater(false);
            clearMountedBreach(serpent);
            return;
        }
        setMountedBreachAiLocked(serpent, true);
        serpent.getNavigation().stop();
        suppressCombat(serpent);
        applyLockedBreachYaw(serpent, trajectory);

        long gameTime = serpent.level().getGameTime();
        long started = MOUNTED_BREACH_STARTED.getOrDefault(
                serpent,
                gameTime
        );
        if (gameTime - started > 10L * 20L) {
            serpent.setJumpingOutOfWater(false);
            clearMountedBreach(serpent);
            return;
        }

        if (gameTime > started && !serpent.onGround()) {
            MOUNTED_BREACH_AIRBORNE.add(serpent);
        }
        if (MOUNTED_BREACH_AIRBORNE.contains(serpent)
                && serpent.onGround()) {
            serpent.setJumpingOutOfWater(false);
            performGroundSlamImpact(serpent);
            clearMountedBreach(serpent);
            return;
        }

        if (gameTime > started && !serpent.isJumpingOutOfWater()) {
            clearMountedBreach(serpent);
        }
    }

    private static void tickUnderwaterBreachAscent(
            EntitySeaSerpent serpent,
            UnderwaterBreachAscent ascent
    ) {
        long gameTime = serpent.level().getGameTime();
        if (!(serpent.getFirstPassenger() instanceof Player rider)
                || !isOwner(serpent, rider.getUUID())
                || !canBeRidden(serpent)
                || gameTime - ascent.startedAt()
                > UNDERWATER_BREACH_TIMEOUT_TICKS) {
            clearMountedBreach(serpent);
            return;
        }

        if (serpent.getY() >= ascent.surfaceY() - 0.25D
                || canBreachFromWaterSurface(serpent)) {
            launchMountedBreach(serpent, ascent.trajectory());
            return;
        }

        if (!serpent.isInWater()) {
            clearMountedBreach(serpent);
            return;
        }

        setMountedBreachAiLocked(serpent, true);
        serpent.getNavigation().stop();
        serpent.setBreathing(false);
        suppressCombat(serpent);
        applyLockedBreachYaw(serpent, ascent.trajectory());
        applyUnderwaterAscentMotion(serpent, ascent.trajectory());
        LAST_MOTION_CACHE.remove(serpent);
    }

    /**
     * Reasserts only the ray-locked heading at level END. Ice and Fire keeps
     * complete ownership of XRot, jumpRot and the native jump animation.
     */
    public static void finishMountedBreachHeading(
            EntitySeaSerpent serpent,
            Player rider
    ) {
        UnderwaterBreachAscent ascent = MOUNTED_BREACH_ASCENT.get(serpent);
        if (serpent.level().isClientSide
                || serpent.getFirstPassenger() != rider) {
            return;
        }
        if (ascent != null) {
            applyLockedBreachYaw(serpent, ascent.trajectory());
            return;
        }
        LockedBreachTrajectory trajectory =
                MOUNTED_BREACH_TRAJECTORY.get(serpent);
        if (trajectory != null) {
            applyLockedBreachYaw(serpent, trajectory);
        }
    }

    /**
     * Gives naturally spawned Sea Serpents a real land impact after either
     * of Ice and Fire's two water-jump goals. Re-entering water is deliberately
     * ignored so the original private splash routine remains authoritative.
     */
    public static void tickWildGroundSlam(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide || wasHatched(serpent)) {
            return;
        }

        long gameTime = serpent.level().getGameTime();
        boolean inWater = serpent.isInWater();
        boolean wasInWater = WILD_WAS_IN_WATER.getOrDefault(
                serpent,
                inWater
        );
        boolean tracked = WILD_BREACH_AIRBORNE.contains(serpent);

        // SeaSerpentAIJump sets JumpingOutOfWater, but the melee jump goal
        // does not. The water-to-air transition catches that second path
        // without turning ordinary falls from land into free slam attacks.
        boolean leftWaterInJump = !inWater
                && !serpent.onGround()
                && serpent.getDeltaMovement().y > 0.05D
                && (serpent.isJumpingOutOfWater()
                || (wasInWater
                && serpent.getTarget() != null
                && serpent.jumpCooldown > 0));
        if (!tracked && leftWaterInJump) {
            WILD_BREACH_AIRBORNE.add(serpent);
            WILD_BREACH_STARTED.put(serpent, gameTime);
            tracked = true;
        }

        if (tracked) {
            long started = WILD_BREACH_STARTED.getOrDefault(
                    serpent,
                    gameTime
            );
            if (!inWater && serpent.onGround()) {
                performWildGroundSlamImpact(serpent);
                clearWildGroundSlam(serpent);
            } else if (inWater
                    || gameTime - started > WILD_BREACH_TIMEOUT_TICKS) {
                clearWildGroundSlam(serpent);
            }
        }

        WILD_WAS_IN_WATER.put(serpent, inWater);
    }

    private static void performGroundSlamImpact(EntitySeaSerpent serpent) {
        performGroundSlamImpact(
                serpent,
                getMountedSlamDamage(serpent),
                true
        );
    }

    private static void performWildGroundSlamImpact(
            EntitySeaSerpent serpent
    ) {
        performGroundSlamImpact(
                serpent,
                WILD_GROUND_SLAM_DAMAGE,
                false
        );
    }

    private static void performGroundSlamImpact(
            EntitySeaSerpent serpent,
            float damage,
            boolean mounted
    ) {
        double radius = 2.0D * serpent.getSeaSerpentScale();
        double knockback = 0.3D * serpent.getSeaSerpentScale();

        for (LivingEntity target : serpent.level().getEntitiesOfClass(
                LivingEntity.class,
                serpent.getBoundingBox().inflate(
                        radius,
                        radius * 0.5D,
                        radius
                ),
                target -> target != serpent && target.isAlive()
        )) {
            if ((mounted && isOwnedAlly(serpent, target))
                    || (!mounted && target instanceof EntitySeaSerpent)) {
                continue;
            }

            boolean hurt = mounted
                    ? hurtWithMountedAbility(serpent, target, damage)
                    : target.hurt(
                            serpent.level().damageSources().mobAttack(serpent),
                            damage
                    );
            if (!hurt) {
                continue;
            }

            Vec3 away = target.position().subtract(serpent.position());
            double horizontalLength = Math.sqrt(
                    away.x * away.x + away.z * away.z
            );
            if (horizontalLength < 1.0E-4D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
                horizontalLength = 1.0D;
            }
            target.setDeltaMovement(
                    target.getDeltaMovement().multiply(0.5D, 1.0D, 0.5D)
                            .add(
                                    away.x / horizontalLength * knockback,
                                    knockback,
                                    away.z / horizontalLength * knockback
                            )
            );
            target.hasImpulse = true;
        }

        serpent.playSound(
                IafSoundRegistry.SEA_SERPENT_SPLASH,
                5.0F,
                0.75F
        );
        if (serpent.level() instanceof ServerLevel serverLevel) {
            int particles = Mth.clamp(
                    Mth.ceil(serpent.getSeaSerpentScale() * 12.0F),
                    24,
                    160
            );
            serverLevel.sendParticles(
                    ParticleTypes.CLOUD,
                    serpent.getX(),
                    serpent.getY() + 0.2D,
                    serpent.getZ(),
                    particles,
                    radius * 0.35D,
                    0.25D,
                    radius * 0.35D,
                    0.08D
            );
        }
    }

    private static void clearWildGroundSlam(EntitySeaSerpent serpent) {
        WILD_BREACH_AIRBORNE.remove(serpent);
        WILD_BREACH_STARTED.remove(serpent);
    }

    private static void clearMountedBreach(EntitySeaSerpent serpent) {
        MOUNTED_BREACH_ACTIVE.remove(serpent);
        MOUNTED_BREACH_STARTED.remove(serpent);
        MOUNTED_BREACH_ASCENT.remove(serpent);
        MOUNTED_BREACH_TRAJECTORY.remove(serpent);
        MOUNTED_BREACH_AIRBORNE.remove(serpent);
        setMountedBreachAiLocked(serpent, false);
    }

    public static boolean isMountedBreachInProgress(
            EntitySeaSerpent serpent
    ) {
        return serpent.level().isClientSide
                ? serpent.getPersistentData().getBoolean(CLIENT_BREACH)
                : MOUNTED_BREACH_ACTIVE.contains(serpent) || MOUNTED_BREACH_ASCENT.containsKey(serpent);
    }

    private static void setMountedBreachAiLocked(
            EntitySeaSerpent serpent,
            boolean locked
    ) {
        if (locked) {
            if (MOUNTED_BREACH_AI_LOCK.add(serpent)) {
                serpent.goalSelector.disableControlFlag(Goal.Flag.MOVE);
                serpent.goalSelector.disableControlFlag(Goal.Flag.LOOK);
                serpent.goalSelector.disableControlFlag(Goal.Flag.JUMP);
                ModNetwork.syncEquipment(serpent);
            }
            return;
        }
        if (MOUNTED_BREACH_AI_LOCK.remove(serpent)) {
            serpent.goalSelector.enableControlFlag(Goal.Flag.MOVE);
            serpent.goalSelector.enableControlFlag(Goal.Flag.LOOK);
            serpent.goalSelector.enableControlFlag(Goal.Flag.JUMP);
            ModNetwork.syncEquipment(serpent);
        }
    }

    public static boolean isMountedBreachActive(EntitySeaSerpent serpent) {
        return MOUNTED_BREACH_ACTIVE.contains(serpent);
    }

    public static boolean isMountedSlamImpact(EntitySeaSerpent serpent) {
        return isMountedBreachActive(serpent)
                && !serpent.isJumpingOutOfWater()
                && !isMountedAbilityAttack(serpent);
    }

    public static boolean isProtectedOwnedAlly(
            EntitySeaSerpent serpent,
            LivingEntity target
    ) {
        return isOwnedAlly(serpent, target);
    }

    private static void clearMountedAbility(EntitySeaSerpent serpent) {
        boolean wasActive = MOUNTED_ABILITY_ACTIVE.remove(serpent);
        NEXT_MOUNTED_SHOT.remove(serpent);
        if (wasActive) {
            serpent.setBreathing(false);
        }
    }

    public static boolean hurtWithMountedAbility(
            EntitySeaSerpent serpent,
            LivingEntity target,
            float damage
    ) {
        if (isOwnedAlly(serpent, target)) {
            return false;
        }

        MOUNTED_ABILITY_ATTACK.set(serpent);
        try {
            return target.hurt(
                    serpent.level().damageSources().mobAttack(serpent),
                    damage
            );
        } finally {
            MOUNTED_ABILITY_ATTACK.remove();
        }
    }

    public static boolean isMountedAbilityAttack(EntitySeaSerpent serpent) {
        return MOUNTED_ABILITY_ATTACK.get() == serpent;
    }

    public static void tickArmorModifier(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide) {
            return;
        }

        SeaSerpentArmorItem.ArmorTier tier = getArmorTierFromServerData(serpent);
        int tierId = tier == null ? 0 : tier.ordinal() + 1;
        AttributeInstance armorAttribute = serpent.getAttribute(Attributes.ARMOR);
        if (armorAttribute == null) {
            return;
        }

        AttributeModifier current = armorAttribute.getModifier(ARMOR_MODIFIER_UUID);
        Integer cached = LAST_ARMOR_CACHE.get(serpent);
        if (cached != null && cached == tierId
                && ((tier == null && current == null)
                || (tier != null && current != null
                && current.getAmount() == tier.getArmorBonus()))) {
            return;
        }

        armorAttribute.removeModifier(ARMOR_MODIFIER_UUID);
        if (tier != null) {
            armorAttribute.addTransientModifier(new AttributeModifier(
                    ARMOR_MODIFIER_UUID,
                    "Bond Beyond Sea Serpent armor",
                    tier.getArmorBonus(),
                    AttributeModifier.Operation.ADDITION
            ));
        }
        LAST_ARMOR_CACHE.put(serpent, tierId);
    }

    private record LockedBreachTrajectory(
            Vec3 forward,
            boolean verticalAscent
    ) {
    }

    private record UnderwaterBreachAscent(
            LockedBreachTrajectory trajectory,
            double surfaceY,
            long startedAt
    ) {
    }

    public record MovementLimits(double maxSpeed, double maxAcceleration) {
    }

    public record RiderInput(@Nullable UUID riderId, float forward, float strafe,
            boolean up, boolean down, boolean breath, boolean bite, long receivedAt) {
        private static final RiderInput IDLE = new RiderInput(null, 0, 0,
                false, false, false, false, 0L);
    }
}
