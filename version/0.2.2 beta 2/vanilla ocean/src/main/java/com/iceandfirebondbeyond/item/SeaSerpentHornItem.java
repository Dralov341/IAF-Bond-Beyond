package com.iceandfirebondbeyond.item;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.iceandfirebondbeyond.block.SeaSerpentForgeBlockEntity;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Dragon-Horn-style storage, using the serpent's complete save rather than recreating a tame. */
public final class SeaSerpentHornItem extends Item {
    private static final String ENTITY = "SeaSerpentEntity";
    private static final String SUMMARY = "SeaSerpentSummary";

    public SeaSerpentHornItem() { super(new Properties().stacksTo(1)); }

    public static boolean isFilled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(ENTITY, Tag.TAG_COMPOUND);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
            LivingEntity target, InteractionHand hand) {
        if (!(target instanceof EntitySeaSerpent serpent)) return InteractionResult.PASS;
        // Also called by CommonEvents before IAF's multipart/empty-hand handling.
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) return InteractionResult.FAIL;
        if (isFilled(stack)) return refuse(player, "full");
        // Client owner state may still be in flight; mutation/ownership checks belong to the server.
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!serpent.isAlive() || !SeaSerpentBondData.isOwner(serpent, player.getUUID()))
            return refuse(player, "not_owner");
        if (serpent.isVehicle() || serpent.isPassenger()) return refuse(player, "occupied");

        CompoundTag entity = new CompoundTag();
        if (!serpent.save(entity)) return InteractionResult.FAIL;
        // A forge claim is a short-lived world position, not equipment. Do not restore an old claim.
        CompoundTag forgeData = entity.getCompound("ForgeData");
        forgeData.remove(SeaSerpentForgeBlockEntity.FUEL_UNTIL);
        forgeData.remove(SeaSerpentForgeBlockEntity.FUEL_POS);
        CompoundTag summary = new CompoundTag();
        summary.putString("Name", Component.Serializer.toJson(serpent.getName()));
        summary.putInt("Stage", SeaSerpentBondData.getGrowthStage(serpent));
        summary.putInt("Days", SeaSerpentBondData.getAgeInDays(serpent));
        summary.putBoolean("Male", SeaSerpentBondData.isMale(serpent));
        // Keep the horn's custom name and other unrelated item tags.
        stack.getOrCreateTag().put(ENTITY, entity);
        stack.getOrCreateTag().put(SUMMARY, summary);
        SeaSerpentForgeBlockEntity.release(serpent);
        serpent.discard(); // No death, corpse, equipment drop, or taming reset.
        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
                SoundSource.NEUTRAL, 3F, .75F);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!isFilled(stack)) return InteractionResult.PASS;
        Player player = context.getPlayer();
        if (player == null || player.isSpectator()) return InteractionResult.FAIL;
        Level level = context.getLevel();
        // A block-targeted click already identifies the water cell. Do not
        // replace that hit with a second camera ray (touch input / rotation
        // updates can differ), or apply the solid-block UP-face restriction.
        if (isOpenWater(level, context.getClickedPos())) {
            return release(level, player, stack, context.getClickedPos(), context.getClickedFace());
        }
        // The ordinary block ray ignores fluids and may select the seabed behind
        // the water. Prefer the nearer water hit before handling that block.
        BlockHitResult water = waterHit(level, player);
        if (water != null) return release(level, player, stack, water.getBlockPos(), water.getDirection());
        if (context.getClickedFace() != Direction.UP) return InteractionResult.FAIL;
        return release(level, player, stack, context.getClickedPos().above(), Direction.UP);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isFilled(stack)) return InteractionResultHolder.pass(stack);
        if (player.isSpectator()) return InteractionResultHolder.fail(stack);
        BlockHitResult water = waterHit(level, player);
        if (water == null) return InteractionResultHolder.pass(stack);
        return new InteractionResultHolder<>(release(level, player, stack,
                water.getBlockPos(), water.getDirection()), stack);
    }

    @Nullable
    private static BlockHitResult waterHit(Level level, Player player) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        return hit.getType() == HitResult.Type.BLOCK && isOpenWater(level, hit.getBlockPos()) ? hit : null;
    }

    private static boolean isOpenWater(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static InteractionResult release(Level level, Player player, ItemStack stack,
            BlockPos pos, Direction face) {
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack))
            return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        CompoundTag saved = stack.getTag().getCompound(ENTITY);
        float health = saved.getFloat("Health");
        if (!saved.hasUUID("UUID") || !"iceandfire:sea_serpent".equals(saved.getString("id"))
                || !Float.isFinite(health) || health <= 0F)
            return refuse(player, "invalid");
        // Preserve identity across dimensions; never clear storage on a failed spawn.
        for (ServerLevel other : server.getServer().getAllLevels()) {
            if (other.getEntity(saved.getUUID("UUID")) != null) return refuse(player, "release_failed");
        }
        EntitySeaSerpent serpent = IafEntityRegistry.SEA_SERPENT.get().create(server);
        if (serpent == null) return refuse(player, "release_failed");
        serpent.load(saved.copy());
        // beta-5's readAdditionalSaveData -> updateAttributes heals 30 * scale.
        // Reapply transient Heart upgrades before restoring health, so storage cannot heal
        // wounds or clamp upgraded health to the unmodified native maximum.
        SeaSerpentBondData.tickHeartHealthModifier(serpent);
        serpent.setHealth(health);
        if (!serpent.isAlive()) return refuse(player, "invalid");
        SeaSerpentForgeBlockEntity.release(serpent);
        serpent.setTarget(null);
        serpent.setBreathing(false);
        serpent.attackDecision = true;
        serpent.setJumpingOutOfWater(false);
        serpent.setDeltaMovement(Vec3.ZERO);
        serpent.fallDistance = 0;
        float yaw = 180F + player.getDirection().toYRot();
        serpent.absMoveTo(pos.getX() + .5D, pos.getY(), pos.getZ() + .5D, yaw, 0F);
        serpent.yBodyRot = serpent.yBodyRotO = serpent.yHeadRot = serpent.yHeadRotO = yaw;
        serpent.refreshDimensions();
        if (!server.getWorldBorder().isWithinBounds(serpent.getBoundingBox())
                || serpent.getBoundingBox().maxY > server.getMaxBuildHeight()
                || pos.getY() < server.getMinBuildHeight()
                || !server.noCollision(serpent, serpent.getBoundingBox()))
            return refuse(player, "no_space");
        // Forge's EntityJoinLevelEvent can veto this (e.g. another mod). Keep the full horn then.
        if (!server.addFreshEntity(serpent)) return refuse(player, "release_failed");
        stack.removeTagKey(ENTITY);
        stack.removeTagKey(SUMMARY);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult refuse(Player player, String reason) {
        if (!player.level().isClientSide) player.displayClientMessage(Component.translatable(
                "message.iceandfire_bond_beyond.horn." + reason), true);
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> lines, TooltipFlag flag) {
        if (!isFilled(stack)) {
            lines.add(Component.translatable("tooltip.iceandfire_bond_beyond.horn.empty")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable("entity.iceandfire.sea_serpent").withStyle(ChatFormatting.DARK_BLUE));
        CompoundTag info = stack.getTag().getCompound(SUMMARY);
        if (info.contains("Name", Tag.TAG_STRING)) {
            try {
                Component name = Component.Serializer.fromJson(info.getString("Name"));
                if (name != null) lines.add(name.copy().withStyle(ChatFormatting.GRAY));
            } catch (com.google.gson.JsonParseException ignored) { /* Still allow release of old/custom data. */ }
        }
        lines.add(Component.translatable("tooltip.iceandfire_bond_beyond.horn.age",
                info.getInt("Stage"), info.getInt("Days")).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.iceandfire_bond_beyond.horn."
                + (info.getBoolean("Male") ? "male" : "female")).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.iceandfire_bond_beyond.horn.release")
                .withStyle(ChatFormatting.GRAY));
    }
}
