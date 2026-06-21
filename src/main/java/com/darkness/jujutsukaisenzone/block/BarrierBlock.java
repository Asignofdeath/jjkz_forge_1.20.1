package com.darkness.jujutsukaisenzone.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class BarrierBlock extends Block {
    public BarrierBlock() {
        super(Properties.of()
                .strength(-1.0F, 0.5F)
                .noOcclusion()
                .isValidSpawn((state, level, pos, entityType) -> false)
                .pushReaction(PushReaction.BLOCK)
        );
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return 0.0F;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }
}
