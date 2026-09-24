package com.iceandfirebondbeyond.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** The inlet is usable directly; decorative shell blocks require a complete forge. */
public final class SeaSerpentForgePartBlock extends Block {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTIVE =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("active");
    public SeaSerpentForgePartBlock() {
        super(Properties.copy(Blocks.STONE_BRICKS).strength(5.0F, 30.0F).requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        for (BlockPos core : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.hasChunkAt(core) && level.getBlockEntity(core) instanceof SeaSerpentForgeBlockEntity forge) {
                boolean inlet = state.is(com.iceandfirebondbeyond.registry.SeaSteelContent.FORGE_INPUT.get())
                        && pos.getY() == core.getY() && pos.distManhattan(core) == 1;
                if (!inlet && forge.inspectStructure() == null) continue;
                if (player instanceof ServerPlayer serverPlayer) NetworkHooks.openScreen(serverPlayer, forge, core);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }
}
