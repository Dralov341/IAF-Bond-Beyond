package com.iceandfirebondbeyond.review;

import com.github.alexthe666.iceandfire.entity.*;
import com.iceandfirebondbeyond.item.SeaSerpentHornItem;
import com.iceandfirebondbeyond.registry.ModItems;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("iceandfire_bond_beyond")
@PrefixGameTestTemplate(false)
public final class WaterBlockHornGameTests {
    private static void click(GameTestHelper h, Direction face, boolean flowing, boolean ancient) {
        ServerPlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "water-block-click"));
        Vec3 at = h.absoluteVec(new Vec3(29.5, 5, 32.5));
        // Simulate a block-targeted input whose hit result is authoritative: a
        // fresh camera ray is different (e.g. a touch click or a later rotation).
        p.moveTo(at.x, at.y, at.z, 90, 0);
        p.setNoGravity(true);
        h.getLevel().addNewPlayer(p);
        p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        EntitySeaSerpent s = IafEntityRegistry.SEA_SERPENT.get().create(h.getLevel());
        SeaSerpentBondData.markAsHatchedBaby(s, true);
        SeaSerpentBondData.addGrowthDays(s, ancient ? 125 : 10);
        SeaSerpentBondData.tame(s, p.getUUID());
        s.setNoAi(true);
        Vec3 away = h.absoluteVec(new Vec3(12, 3, 12));
        s.moveTo(away.x, away.y, away.z, 0, 0);
        h.getLevel().addFreshEntity(s);
        s.setHealth(s.getMaxHealth() * .4F);
        UUID id = s.getUUID();
        int age = SeaSerpentBondData.getGrowthTicks(s);
        float scale = s.getSeaSerpentScale(), health = s.getHealth();
        ItemStack horn = new ItemStack(ModItems.SEA_SERPENT_HORN.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, horn);
        h.assertTrue(horn.getItem().interactLivingEntity(horn, p, s, InteractionHand.MAIN_HAND).consumesAction(), "capture fixture");
        h.runAfterDelay(3, () -> {
            BlockPos target = h.absolutePos(new BlockPos(32, 5, 32));
            h.getLevel().setBlockAndUpdate(target, Blocks.WATER.defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, flowing ? 4 : 0));
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target)
                    .add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)), face, target, false);
            InteractionResult result = p.gameMode.useItemOn(p, h.getLevel(), horn, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(result.consumesAction(), "clicking WATER itself must release, including a side/bottom face");
            EntitySeaSerpent restored = (EntitySeaSerpent) h.getLevel().getEntity(id);
            h.assertTrue(restored != null && !SeaSerpentHornItem.isFilled(horn), "one serpent released and storage cleared");
            h.assertTrue(restored.blockPosition().equals(target), "use actual clicked WATER cell, not a new ray or block above");
            h.assertTrue(SeaSerpentBondData.isOwner(restored, p.getUUID())
                    && SeaSerpentBondData.getGrowthTicks(restored) == age
                    && restored.getSeaSerpentScale() == scale && restored.getHealth() == health,
                    "full identity, age, scale, health and owner survive");
            h.assertTrue(p.gameMode.useItemOn(p, h.getLevel(), horn, InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS,
                    "second click on empty horn cannot duplicate serpent");
            h.succeed();
        });
    }
    @GameTest(template="combat_empty", timeoutTicks=35)
    public static void water_source_side(GameTestHelper h) { click(h, Direction.WEST, false, false); }
    @GameTest(template="combat_empty", timeoutTicks=35)
    public static void water_source_top_ancient(GameTestHelper h) { click(h, Direction.UP, false, true); }
    @GameTest(template="combat_empty", timeoutTicks=35)
    public static void water_flowing_bottom(GameTestHelper h) { click(h, Direction.DOWN, true, false); }
}
