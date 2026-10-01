package com.darkness.jujutsukaisenzone.block;

import com.darkness.jujutsukaisenzone.blockentity.JJKZSkyblockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

/**
 * 自定义天空盒方块。
 * <p>
 * 作为 Shadered 模组的附属，使用自定义纹理渲染天空盒效果。
 * 纹理路径存储在 BlockEntity 中，支持不同术式使用不同天空盒纹理。
 * 不阻挡光线，避免领域内因壳层/内壁过暗。
 */
public class JJKZSkyblock extends Block implements EntityBlock
{
    public JJKZSkyblock()
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
    public RenderShape getRenderShape(BlockState pState)
    {
        /* 使用 ENTITYBLOCK_ANIMATED 渲染形状，由 BlockEntityRenderer 负责渲染 */
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity)
    {
        /* 禁用方块上的粒子效果 */
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState)
    {
        return new JJKZSkyblockEntity(pPos, pState);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos)
    {
        /* 不可挖掘 */
        return 0.0F;
    }
}
