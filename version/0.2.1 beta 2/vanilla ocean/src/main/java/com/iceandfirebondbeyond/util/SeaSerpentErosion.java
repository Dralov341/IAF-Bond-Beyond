package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Irregular, surface-first erosion; each selected block advances only once per impact. */
public final class SeaSerpentErosion {
    public static BlockState next(BlockState state) {
        if (state.is(Blocks.GRAVEL)) return Blocks.SAND.defaultBlockState();
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND)) return Blocks.GRAVEL.defaultBlockState();
        return state;
    }
    public static int erodeArea(Level level, BlockPos center, EntitySeaSerpent source) {
        return erodeImpact(level, center, source, null);
    }

    public static int erodeImpact(Level level, BlockPos center, EntitySeaSerpent source, @Nullable BlockPos forgeCore) {
        if (level.isClientSide || !source.isAlive() || !level.hasChunkAt(center)
                || !ForgeEventFactory.getMobGriefingEvent(level, source)) return 0;
        int stage = SeaSerpentBondData.getKnockbackStage(source);
        int radius = SeaSerpentBreathArea.radius(stage);
        double driftX = (source.getRandom().nextDouble() - 0.5D) * 0.9D;
        double driftY = (source.getRandom().nextDouble() - 0.5D) * 0.9D;
        double driftZ = (source.getRandom().nextDouble() - 0.5D) * 0.9D;
        Map<BlockPos, Double> candidates = new LinkedHashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            double chance = SeaSerpentBreathArea.erosionChance(stage, pos.getX() - center.getX(),
                    pos.getY() - center.getY(), pos.getZ() - center.getZ(), driftX, driftY, driftZ);
            if (chance > 0) candidates.put(pos.immutable(), chance);
        }
        if (forgeCore != null) {
            BlockPos bed = forgeCore.below(2);
            int spread = radius + 1;
            for (BlockPos pos : BlockPos.betweenClosed(bed.offset(-spread, -1, -spread), bed.offset(spread, 1, spread))) {
                double chance = SeaSerpentBreathArea.forgeErosionChance(stage, pos.getX() - bed.getX(),
                        pos.getY() - bed.getY(), pos.getZ() - bed.getZ(), driftX, driftZ);
                if (chance > 0) candidates.merge(pos.immutable(), chance, Math::max);
            }
        }
        // Select before mutating: freshly changed neighbors cannot expose a
        // second underground layer in the same impact. Never load edge chunks.
        List<BlockPos> selected = new ArrayList<>();
        for (Map.Entry<BlockPos, Double> candidate : candidates.entrySet()) {
            BlockPos pos = candidate.getKey();
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                    || source.getRandom().nextDouble() >= candidate.getValue()) continue;
            BlockState before = level.getBlockState(pos);
            if (next(before) == before || level.getBlockEntity(pos) != null) continue;
            boolean underFoundation = forgeCore != null && pos.getY() == forgeCore.getY() - 2
                    && Math.abs(pos.getX() - forgeCore.getX()) <= 1 && Math.abs(pos.getZ() - forgeCore.getZ()) <= 1;
            if (underFoundation || exposed(level, pos)) selected.add(pos);
        }
        // Randomize debris positions instead of always drawing the scan's first rows.
        for (int i = selected.size() - 1; i > 0; i--) {
            int j = source.getRandom().nextInt(i + 1);
            BlockPos swap = selected.get(i); selected.set(i, selected.get(j)); selected.set(j, swap);
        }
        int changed = 0;
        for (BlockPos pos : selected) if (erodeBlock(level, pos, source, changed < 12)) changed++;
        return changed;
    }

    private static boolean exposed(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (!level.hasChunkAt(adjacent) || level.isOutsideBuildHeight(adjacent)) continue;
            BlockState state = level.getBlockState(adjacent);
            if (state.isAir() || !state.getFluidState().isEmpty() || state.getCollisionShape(level, adjacent).isEmpty()) return true;
        }
        return false;
    }

    private static boolean erodeBlock(Level level, BlockPos pos, EntitySeaSerpent source, boolean particles) {
        BlockState before = level.getBlockState(pos), after = next(before);
        if (after == before || level.getBlockEntity(pos) != null
                || !ForgeEventFactory.onEntityDestroyBlock(source, pos, before)) return false;
        if (!level.setBlock(pos, after, Block.UPDATE_ALL)) return false;
        // Bound debris/sound traffic independently of the full terrain footprint.
        if (particles) level.levelEvent(2001, pos, Block.getId(before));
        return true;
    }
    private SeaSerpentErosion() {}
}
