package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;

/** Each impact advances every eligible block in the breath footprint by one step. */
public final class SeaSerpentErosion {
    public static BlockState next(BlockState state) {
        if (state.is(Blocks.GRAVEL)) return Blocks.SAND.defaultBlockState();
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND)) return Blocks.GRAVEL.defaultBlockState();
        return state;
    }
    public static int erodeArea(Level level, BlockPos center, EntitySeaSerpent source) {
        if (level.isClientSide || !source.isAlive() || !level.hasChunkAt(center)
                || !ForgeEventFactory.getMobGriefingEvent(level, source)) return 0;
        int stage = SeaSerpentBondData.getKnockbackStage(source);
        int radius = SeaSerpentBreathArea.radius(stage);
        int changed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (!SeaSerpentBreathArea.contains(stage, pos.getX() - center.getX(),
                    pos.getY() - center.getY(), pos.getZ() - center.getZ())) continue;
            // Never load neighboring chunks or reprocess a block within one impact.
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) continue;
            if (erodeBlock(level, pos, source, changed < 12)) changed++;
        }
        return changed;
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
