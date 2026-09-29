package com.iceandfirebondbeyond.event;

import com.github.alexthe666.iceandfire.api.FoodUtils;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.EntityMutlipartPart;
import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.client.ClientPartResolver;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import com.iceandfirebondbeyond.entity.SeaSerpentEggEntity;
import com.iceandfirebondbeyond.entity.ai.SeaSerpentBondGoals;
import com.iceandfirebondbeyond.inventory.SeaSerpentMenu;
import com.iceandfirebondbeyond.network.ModNetwork;
import com.iceandfirebondbeyond.registry.ModEntities;
import com.iceandfirebondbeyond.registry.ModItems;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(
        modid = IceAndFireBondBeyond.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CommonEvents {
    private static final float SEA_SERPENT_TAME_CHANCE = 0.15F;
    private static final double BREEDING_SEARCH_RADIUS = 32.0D;

    private CommonEvents() {
    }

    @SubscribeEvent
    public static void remapLegacySeaSerpentEgg(MissingMappingsEvent event) {
        // Preserve eggs in old inventories/chests after the darkblue rename.
        for (MissingMappingsEvent.Mapping<Item> mapping :
                event.getMappings(ForgeRegistries.Keys.ITEMS, IceAndFireBondBeyond.MOD_ID)) {
            if (mapping.getKey().getPath().equals("sea_serpent_egg_deepblue")) {
                mapping.remap(ModItems.SEA_SERPENT_EGG_DARKBLUE.get());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void restrictHatchedSerpentTargets(
            LivingChangeTargetEvent event
    ) {
        if (!(event.getEntity() instanceof EntitySeaSerpent serpent)
                || !SeaSerpentBondData.hasRestrictedCombat(serpent)) {
            return;
        }

        LivingEntity newTarget = event.getNewTarget();
        if (newTarget != null
                && !SeaSerpentBondData.isAllowedCombatTarget(
                serpent,
                newTarget
        )) {
            event.setNewTarget(null);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void restrictHatchedSerpentDamage(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof EntitySeaSerpent serpent)
                || !SeaSerpentBondData.hasRestrictedCombat(serpent)) {
            return;
        }

        // This check precedes EVERY ability bypass, including the native
        // bubble/slam and a projectile fired before taming this serpent.
        if (SeaSerpentBondData.isProtectedOwnedAlly(serpent, event.getEntity())) {
            event.setCanceled(true);
            return;
        }

        if (SeaSerpentBondData.isMountedAbilityAttack(serpent)) {
            return;
        }

        // A rider deliberately triggered the original breach/slam. Let the
        // splash hit nearby creatures, but never the owner or their own pets.
        if (SeaSerpentBondData.isMountedBreachActive(serpent)) {
            return;
        }

        if (!SeaSerpentBondData.isAllowedCombatTarget(
                serpent,
                event.getEntity()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void initializeSeaSerpentData(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof EntitySeaSerpent serpent
                && !event.getLevel().isClientSide) {
            SeaSerpentBondData.initializeWildAge(serpent, event.loadedFromDisk());
            SeaSerpentBondData.ensureDefaults(serpent);
            SeaSerpentBondGoals.install(serpent);
            SeaSerpentBondData.tickArmorModifier(serpent);
            SeaSerpentBondData.tickGrowthKnockbackModifier(serpent);
            SeaSerpentBondData.tickHeartHealthModifier(serpent);
        }
    }

    @SubscribeEvent
    public static void syncEquipmentWhenTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTarget() instanceof EntitySeaSerpent serpent) {
            ModNetwork.syncEquipmentTo(serpent, player);
        }
    }

    @SubscribeEvent
    public static void tickSeaSerpent(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof EntitySeaSerpent serpent)) {
            return;
        }

        SeaSerpentBondData.tickWildGroundSlam(serpent);
        SeaSerpentBondData.tickCombatRules(serpent);
        SeaSerpentBondData.tickGrowthKnockbackModifier(serpent);
        SeaSerpentBondData.tickGrowthAndHunger(serpent);
        // Command NBT ownership also works for wild-born/adopted serpents.
        SeaSerpentBondData.tickCommandAndFollow(serpent);
        if (!serpent.level().isClientSide && !SeaSerpentBondData.wasHatched(serpent)
                && (serpent.tickCount + serpent.getId()) % 100 == 0) ModNetwork.syncEquipment(serpent);
        if (!SeaSerpentBondData.wasHatched(serpent)) {
            return;
        }

        SeaSerpentBondData.tickRiderControl(serpent);
        SeaSerpentBondData.tickMountedBite(serpent);
        SeaSerpentBondData.tickMountedAbility(serpent);
        SeaSerpentBondData.tickMountedBreach(serpent);
        SeaSerpentBondData.tickArmorModifier(serpent);
        SeaSerpentBondData.tickHeartHealthModifier(serpent);
        if (!serpent.level().isClientSide && (serpent.tickCount + serpent.getId()) % 100 == 0) {
            ModNetwork.syncEquipment(serpent);
        }
    }

    @SubscribeEvent
    public static void positionPlayersOnSaddles(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // Level END runs after vehicle.positionRider, so vanilla's generic
        // Animal offset cannot overwrite the custom saddle position.
        for (Player player : event.level.players()) {
            if (player.getVehicle() instanceof EntitySeaSerpent serpent) {
                SeaSerpentBondData.positionRiderOnSaddle(player, serpent);
                SeaSerpentBondData.finishMountedBreachHeading(
                        serpent,
                        player
                );
            }
        }
    }

    /** Includes head/tail parts: their native interact() otherwise bypasses Forge's parent event. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interactWithHatchedSeaSerpent(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        for (int i = 0; i < 10 && target instanceof EntityMutlipartPart part; i++) {
            target = part.level().isClientSide
                    ? DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ClientPartResolver.findParent(part))
                    : part.getParent();
        }
        if (target instanceof EntitySeaSerpent serpent) {
            InteractionResult result = interact(serpent, event.getEntity(), event.getHand());
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        }
    }

    /** Shared by normal clicks, multipart clicks, and IAF's direct parent calls. */
    public static InteractionResult interact(EntitySeaSerpent serpent, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof com.iceandfirebondbeyond.item.SeaSerpentHornItem horn) {
            return horn.interactLivingEntity(held, player, serpent, hand);
        }
        if (!SeaSerpentBondData.wasHatched(serpent)) {
            return InteractionResult.PASS;
        }
        InteractionContext context = new InteractionContext(player, hand);
        handleInteraction(context, serpent);
        if (context.result != InteractionResult.PASS && !serpent.level().isClientSide) {
            ModNetwork.syncEquipment(serpent);
        }
        return context.result;
    }

    private static void handleInteraction(InteractionContext event, EntitySeaSerpent serpent) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        boolean owner = SeaSerpentBondData.isOwner(serpent, player.getUUID());

        if (!SeaSerpentBondData.isTamed(serpent)) {
            if (stack.getItem() == IafItemRegistry.CREATIVE_DRAGON_MEAL.get()) {
                handleCreativeMealTaming(event, serpent, player, stack);
            } else if (isValidTamingFish(stack)) {
                handleFishTaming(event, serpent, player, stack);
            }
            return;
        }

        if (!owner) {
            return;
        }

        if (stack.getItem() == IafItemRegistry.DRAGON_STAFF.get()) {
            handleDragonCommandStaff(event, serpent, player);
            return;
        }

        if (stack.is(Items.HEART_OF_THE_SEA)) {
            handleHeartOfTheSeaUpgrade(event, serpent, player, stack);
            return;
        }

        if (handleDragonMeal(event, serpent, player, stack)) {
            return;
        }

        int foodPoints = stack.is(Items.PUFFERFISH)
                ? 0
                : FoodUtils.getFoodPoints(stack, true, true);
        if (foodPoints > 0 || stack.is(Items.PUFFERFISH)) {
            // A full stomach still consumes the CLICK, never the item. Prevent
            // Minecraft retrying an empty offhand and mounting instead.
            consumeInteraction(event);
            if (!player.level().isClientSide) {
                if (foodPoints > 0 && (SeaSerpentBondData.getHunger(serpent)
                        < SeaSerpentBondData.getMaxHunger(serpent)
                        || serpent.getHealth() < serpent.getMaxHealth())) {
                    SeaSerpentBondData.addHunger(serpent, foodPoints);
                    serpent.heal(Math.max(1.0F, foodPoints / 10.0F));
                    playEatingEffects(serpent, player, stack);
                    consumeOne(player, stack);
                } else {
                    player.displayClientMessage(Component.translatable(
                            "message.iceandfire_bond_beyond.sea_serpent." +
                                    (stack.is(Items.PUFFERFISH) ? "reject_pufferfish" : "full")), true);
                }
            }
            return;
        }

        if (!stack.isEmpty()) {
            return; // Name tags and other vanilla/mod items keep their own action.
        }
        if (event.hand != InteractionHand.MAIN_HAND
                || !player.getOffhandItem().isEmpty()) {
            return;
        }

        consumeInteraction(event);
        if (player.level().isClientSide) {
            return;
        }

        if (player.isShiftKeyDown()) {
            openInventory(serpent, player);
            return;
        }

        if (SeaSerpentBondData.getGrowthStage(serpent) < 3) {
            player.displayClientMessage(Component.translatable(
                    "message.iceandfire_bond_beyond.sea_serpent.too_young"
            ), true);
            return;
        }
        if (SeaSerpentBondData.getArmorStack(serpent).isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.iceandfire_bond_beyond.sea_serpent.needs_armor"
            ), true);
            return;
        }
        if (!player.isPassenger()) {
            serpent.getNavigation().stop();
            SeaSerpentBondData.suppressCombat(serpent);
            player.setYRot(serpent.getYRot());
            player.startRiding(serpent, true);
        }
    }

    private static void handleFishTaming(
            InteractionContext event,
            EntitySeaSerpent serpent,
            Player player,
            ItemStack stack
    ) {
        consumeInteraction(event);
        if (player.level().isClientSide) {
            return;
        }

        boolean success = serpent.getRandom().nextFloat()
                < SEA_SERPENT_TAME_CHANCE;
        serpent.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
        if (success) {
            SeaSerpentBondData.tame(serpent, player.getUUID());
        }
        ((ServerLevel) player.level()).sendParticles(
                success ? ParticleTypes.HEART : ParticleTypes.SMOKE,
                serpent.getX(),
                serpent.getY() + serpent.getBbHeight() * 0.75D,
                serpent.getZ(),
                success ? 8 : 6,
                0.35D,
                0.35D,
                0.35D,
                0.02D
        );
        consumeOne(player, stack);
    }

    private static void handleCreativeMealTaming(
            InteractionContext event,
            EntitySeaSerpent serpent,
            Player player,
            ItemStack stack
    ) {
        consumeInteraction(event);
        if (player.level().isClientSide) {
            return;
        }
        SeaSerpentBondData.tame(serpent, player.getUUID());
        SeaSerpentBondData.addGrowthDays(serpent, SeaSerpentBondData.MAX_GROWTH_DAYS);
        SeaSerpentBondData.addHunger(serpent, 20);
        serpent.heal(serpent.getMaxHealth());
        playEatingEffects(serpent, player, stack);
        ((ServerLevel) player.level()).sendParticles(
                ParticleTypes.HEART,
                serpent.getX(),
                serpent.getY() + serpent.getBbHeight() * 0.75D,
                serpent.getZ(),
                12,
                0.4D,
                0.4D,
                0.4D,
                0.03D
        );
        consumeOne(player, stack);
    }

    private static void handleDragonCommandStaff(
            InteractionContext event,
            EntitySeaSerpent serpent,
            Player player
    ) {
        consumeInteraction(event);
        if (player.level().isClientSide) {
            return;
        }

        if (player.isShiftKeyDown()) {
            if (SeaSerpentBondData.hasHome(serpent)) {
                SeaSerpentBondData.clearHome(serpent);
                player.displayClientMessage(Component.translatable(
                        "dragon.command.remove_home"
                ), true);
            } else {
                SeaSerpentBondData.setHomeHere(serpent);
                BlockPos pos = SeaSerpentBondData.getHomePos(serpent);
                player.displayClientMessage(Component.translatable(
                        "dragon.command.new_home",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        SeaSerpentBondData.getHomeDimension(serpent)
                ), true);
            }
            return;
        }

        int command = SeaSerpentBondData.cycleCommand(serpent);
        serpent.playSound(SoundEvents.ZOMBIE_INFECT, 1.0F, 1.0F);
        String commandName = switch (command) {
            case SeaSerpentBondData.COMMAND_SIT -> "stay";
            case SeaSerpentBondData.COMMAND_ESCORT -> "follow";
            default -> "wander";
        };
        player.displayClientMessage(Component.translatable(
                "message.iceandfire_bond_beyond.sea_serpent.command_" + commandName
        ), true);
    }

    private static boolean handleDragonMeal(
            InteractionContext event,
            EntitySeaSerpent serpent,
            Player player,
            ItemStack stack
    ) {
        Item item = stack.getItem();
        boolean normal = item == IafItemRegistry.DRAGON_MEAL.get();
        boolean sickly = item == IafItemRegistry.SICKLY_DRAGON_MEAL.get();
        boolean creative = item == IafItemRegistry.CREATIVE_DRAGON_MEAL.get();
        if (!normal && !sickly && !creative) {
            return false;
        }
        if (SeaSerpentBondData.isAgingDisabled(serpent)) {
            consumeInteraction(event);
            if (!player.level().isClientSide && !sickly) {
                player.displayClientMessage(Component.translatable(
                        "message.iceandfire_bond_beyond.sea_serpent.growth_locked"), true);
            }
            return true;
        }

        if (normal && SeaSerpentBondData.isFullyGrown(serpent)) {
            consumeInteraction(event);
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "message.iceandfire_bond_beyond.sea_serpent.max_growth_meal"
                ), true);
            }
            return true;
        }

        consumeInteraction(event);
        if (player.level().isClientSide) {
            return true;
        }

        if (normal) {
            SeaSerpentBondData.addGrowthDays(serpent, 1);
            SeaSerpentBondData.addHunger(serpent, 20);
            serpent.heal(serpent.getMaxHealth() / 2.0F);
            serpent.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
        } else if (sickly) {
            SeaSerpentBondData.setAgingDisabled(serpent, true);
            SeaSerpentBondData.addHunger(serpent, 20);
            serpent.heal(serpent.getMaxHealth());
            serpent.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F);
        } else {
            SeaSerpentBondData.addGrowthDays(serpent, SeaSerpentBondData.MAX_GROWTH_DAYS);
            SeaSerpentBondData.addHunger(serpent, 20);
            serpent.heal(serpent.getMaxHealth());
            serpent.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
        }
        spawnItemParticles(serpent, stack);
        consumeOne(player, stack);
        return true;
    }

    private static void handleHeartOfTheSeaUpgrade(
            InteractionContext event,
            EntitySeaSerpent serpent,
            Player player,
            ItemStack stack
    ) {
        consumeInteraction(event);
        if (player.level().isClientSide) {
            return;
        }

        float added = SeaSerpentBondData.increaseMaxHealthWithHeart(serpent);
        if (added > 0.0F) {
            playHeartOfTheSeaEffects(serpent, stack);
            consumeOne(player, stack);
            player.displayClientMessage(Component.translatable(
                    "message.iceandfire_bond_beyond.sea_serpent.health_increased",
                    Math.round(added),
                    Math.round(serpent.getMaxHealth()),
                    Math.round(SeaSerpentBondData.getStageFiveDragonHealthCap())
            ), true);

            if (SeaSerpentBondData.hasBreedingStats(serpent)
                    && !SeaSerpentBondData.wasBreedingTutorialShown(serpent)) {
                SeaSerpentBondData.markBreedingTutorialShown(serpent);
                showBreedingUnlockedMessage(player);
            }
            return;
        }

        if (!SeaSerpentBondData.hasReachedHeartHealthCap(serpent)) {
            player.displayClientMessage(Component.translatable(
                    "message.iceandfire_bond_beyond.sea_serpent.health_cap",
                    Math.round(SeaSerpentBondData.getStageFiveDragonHealthCap())
            ), true);
            return;
        }

        if (SeaSerpentBondData.getGrowthStage(serpent) < 5) {
            player.displayClientMessage(Component.translatable(
                    "message.iceandfire_bond_beyond.sea_serpent.breeding_wait_for_stage"
            ), false);
            return;
        }

        if (!SeaSerpentBondData.wasBreedingTutorialShown(serpent)) {
            SeaSerpentBondData.markBreedingTutorialShown(serpent);
            showBreedingUnlockedMessage(player);
            return;
        }

        if (!SeaSerpentBondData.isReadyForBreeding(serpent)) {
            showBreedingRequirementsMessage(player);
            return;
        }

        EntitySeaSerpent partner = findBreedingPartner(serpent, player);
        if (partner == null) {
            showBreedingRequirementsMessage(player);
            return;
        }

        SeaSerpentEggEntity egg = layBreedingEgg(
                (ServerLevel) player.level(),
                serpent,
                partner,
                player
        );
        if (egg == null) {
            player.displayClientMessage(Component.translatable(
                    "message.iceandfire_bond_beyond.sea_serpent.breeding_no_space"
            ), true);
            return;
        }

        serpent.setAge(SeaSerpentBondData.BREEDING_COOLDOWN_TICKS);
        partner.setAge(SeaSerpentBondData.BREEDING_COOLDOWN_TICKS);
        playBreedingEffects((ServerLevel) player.level(), serpent);
        playBreedingEffects((ServerLevel) player.level(), partner);
        consumeOne(player, stack);
        player.displayClientMessage(Component.translatable(
                "message.iceandfire_bond_beyond.sea_serpent.breeding_success",
                ModItems.getSeaSerpentEgg(egg.getVariant()).getHoverName()
        ), false);
    }

    private static void playHeartOfTheSeaEffects(
            EntitySeaSerpent serpent,
            ItemStack stack
    ) {
        spawnItemParticles(serpent, stack);
        serpent.playSound(SoundEvents.CONDUIT_ACTIVATE, 1.5F, 1.0F);
        if (serpent.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    serpent.getX(),
                    serpent.getY() + serpent.getBbHeight() * 0.75D,
                    serpent.getZ(),
                    12,
                    0.45D,
                    0.45D,
                    0.45D,
                    0.03D
            );
        }
    }

    private static void showBreedingUnlockedMessage(Player player) {
        player.displayClientMessage(Component.translatable(
                "message.iceandfire_bond_beyond.sea_serpent.breeding_unlocked"
        ), false);
    }

    private static void showBreedingRequirementsMessage(Player player) {
        player.displayClientMessage(Component.translatable(
                "message.iceandfire_bond_beyond.sea_serpent.breeding_requirements"
        ), false);
    }

    private static EntitySeaSerpent findBreedingPartner(
            EntitySeaSerpent serpent,
            Player player
    ) {
        EntitySeaSerpent nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (EntitySeaSerpent candidate : serpent.level().getEntitiesOfClass(
                EntitySeaSerpent.class,
                serpent.getBoundingBox().inflate(BREEDING_SEARCH_RADIUS)
        )) {
            if (candidate == serpent
                    || !SeaSerpentBondData.isOwner(
                    candidate,
                    player.getUUID()
            )
                    || SeaSerpentBondData.isMale(candidate)
                    == SeaSerpentBondData.isMale(serpent)
                    || !SeaSerpentBondData.isReadyForBreeding(candidate)) {
                continue;
            }

            double distance = serpent.distanceToSqr(candidate);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static SeaSerpentEggEntity layBreedingEgg(
            ServerLevel level,
            EntitySeaSerpent first,
            EntitySeaSerpent second,
            Player owner
    ) {
        EntitySeaSerpent mother = SeaSerpentBondData.isFemale(first)
                ? first
                : second;
        EnumSeaSerpent variant = mother.getRandom().nextBoolean()
                ? first.getEnum()
                : second.getEnum();
        SeaSerpentEggEntity egg = ModEntities.SEA_SERPENT_EGG.get().create(level);
        if (egg == null) {
            return null;
        }

        egg.setVariant(variant);
        egg.setOwnerId(owner.getUUID());
        double distance = Math.max(
                1.5D,
                mother.getBbWidth() * 0.55D + egg.getBbWidth()
        );
        double rearAngle = Math.toRadians(mother.getYRot());
        boolean foundSpace = false;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = rearAngle + attempt * Math.PI / 4.0D;
            double x = mother.getX() + Math.sin(angle) * distance;
            double z = mother.getZ() - Math.cos(angle) * distance;
            egg.moveTo(x, mother.getY() + 0.1D, z, mother.getYRot(), 0.0F);
            if (level.noCollision(egg, egg.getBoundingBox())) {
                foundSpace = true;
                break;
            }
        }

        if (!foundSpace || !level.addFreshEntity(egg)) {
            egg.discard();
            return null;
        }
        return egg;
    }

    private static void playBreedingEffects(
            ServerLevel level,
            EntitySeaSerpent serpent
    ) {
        level.sendParticles(
                ParticleTypes.HEART,
                serpent.getX(),
                serpent.getY() + serpent.getBbHeight() * 0.65D,
                serpent.getZ(),
                14,
                0.55D,
                0.55D,
                0.55D,
                0.04D
        );
        serpent.playSound(SoundEvents.CONDUIT_ACTIVATE, 1.2F, 1.15F);
    }

    private static void openInventory(EntitySeaSerpent serpent, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        String ownerName = player.getName().getString();
        NetworkHooks.openScreen(
                serverPlayer,
                new SimpleMenuProvider(
                        (containerId, inventory, menuPlayer) ->
                                new SeaSerpentMenu(
                                        containerId,
                                        inventory,
                                        serpent,
                                        ownerName
                                ),
                        Component.translatable(
                                "container.iceandfire_bond_beyond.sea_serpent"
                        )
                ),
                buffer -> {
                    buffer.writeVarInt(serpent.getId());
                    buffer.writeUtf(ownerName, 128);
                }
        );
    }

    private static boolean isValidTamingFish(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.PUFFERFISH)) {
            return false;
        }
        return stack.is(ItemTags.FISHES)
                || stack.is(Items.COOKED_COD)
                || stack.is(Items.COOKED_SALMON);
    }

    private static void consumeInteraction(
            InteractionContext event
    ) {
        event.result = InteractionResult.sidedSuccess(event.player.level().isClientSide);
    }

    private static final class InteractionContext {
        private final Player player;
        private final InteractionHand hand;
        private InteractionResult result = InteractionResult.PASS;
        private InteractionContext(Player player, InteractionHand hand) {
            this.player = player;
            this.hand = hand;
        }
        private Player getEntity() { return player; }
        private ItemStack getItemStack() { return player.getItemInHand(hand); }
    }

    private static void playEatingEffects(
            EntitySeaSerpent serpent,
            Player player,
            ItemStack stack
    ) {
        serpent.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
        spawnItemParticles(serpent, stack);
    }

    private static void spawnItemParticles(
            EntitySeaSerpent serpent,
            ItemStack stack
    ) {
        if (!(serpent.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(
                new ItemParticleOption(ParticleTypes.ITEM, stack.copy()),
                serpent.getX(),
                serpent.getY() + serpent.getBbHeight() * 0.6D,
                serpent.getZ(),
                15,
                0.35D,
                0.35D,
                0.35D,
                0.08D
        );
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @SubscribeEvent
    public static void dropEquipmentAndEgg(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof EntitySeaSerpent serpent)
                || serpent.level().isClientSide) {
            return;
        }

        if (SeaSerpentBondData.wasHatched(serpent)) {
            addDrop(event, serpent, SeaSerpentBondData.getArmorStack(serpent));
            SeaSerpentBondData.setArmorStack(serpent, ItemStack.EMPTY);
        }

        if (com.iceandfirebondbeyond.util.SeaSerpentEggLoot.shouldDrop(serpent)) {
            addDrop(
                    event,
                    serpent,
                    ModItems.getSeaSerpentEgg(serpent.getEnum())
            );
        }
    }

    private static void addDrop(
            LivingDropsEvent event,
            EntitySeaSerpent serpent,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return;
        }
        event.getDrops().add(new ItemEntity(
                serpent.level(),
                serpent.getX(),
                serpent.getY(),
                serpent.getZ(),
                stack.copy()
        ));
    }
}
