package com.darkness.jujutsukaisenzone.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * 领域展开结界方块。
 * <p>
 * 与普通结界方块类似，但用于领域展开场景。
 * 不可挖掘、可被爆破、不可推动、阻止生物生成；不阻挡光线。
 */
public class DomainBlock extends Block
{
    public DomainBlock()
    {
        super(Properties.of()
                .strength(-1.0F, 0.5F)
                .noOcclusion()
                .isValidSpawn((state, level, pos, entityType) -> false)
                .pushReaction(PushReaction.BLOCK)
        );
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
    {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos)
    {
        return 0;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state)
    {
        return false;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos)
    {
        return 0.0F;
    }

    @Override
    public boolean isSignalSource(BlockState state)
    {
        return false;
    }
}
