package com.darkness.jujutsukaisenzone.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 「帐」方块，属性与结界方块一致。
 * 相邻同方块共享面不渲染，避免球壳上出现方块网格接缝。
 * 不遮挡天光与方块光，外观半透明但允许阳光穿透。
 */
public class ShroudBlock extends BarrierBlock
{
    public ShroudBlock()
    {
        super();
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side)
    {
        return adjacentState.is(this) ? true : super.skipRendering(state, adjacentState, side);
    }

    @Override
    public boolean supportsExternalFaceHiding(BlockState state)
    {
        return true;
    }

    @Override
    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir)
    {
        return state.is(this);
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
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos)
    {
        return 1.0F;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return Shapes.empty();
    }
}
