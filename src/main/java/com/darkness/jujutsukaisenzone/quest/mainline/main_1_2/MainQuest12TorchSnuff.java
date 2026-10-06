package com.darkness.jujutsukaisenzone.quest.mainline.main_1_2;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * 主线 1.2：ghost_ambient 开播后，玩家靠近村内火把时将其破坏（无掉落）。
 */
public final class MainQuest12TorchSnuff
{
    /** 水平 + 垂直检测半径（格）。 */
    private static final double SNUFF_RADIUS = 6.5D;
    private static final int CHECK_INTERVAL = 8;

    private MainQuest12TorchSnuff()
    {
    }

    public static void tick(ServerPlayer player, @Nullable BoundingBox villageBounds)
    {
        if (villageBounds == null || player.tickCount % CHECK_INTERVAL != 0)
        {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int radius = Mth.ceil(SNUFF_RADIUS);
        double radiusSq = SNUFF_RADIUS * SNUFF_RADIUS;
        int minY = Math.max(villageBounds.minY(), center.getY() - radius);
        int maxY = Math.min(villageBounds.maxY(), center.getY() + radius);
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!villageBounds.isInside(pos))
                    {
                        continue;
                    }
                    double dx = (x + 0.5D) - player.getX();
                    double dy = (y + 0.5D) - player.getY();
                    double dz = (z + 0.5D) - player.getZ();
                    if (dx * dx + dy * dy + dz * dz > radiusSq)
                    {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (isTorch(state))
                    {
                        level.destroyBlock(pos, false);
                    }
                }
            }
        }
    }

    private static boolean isTorch(BlockState state)
    {
        return state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH)
                || state.is(Blocks.SOUL_WALL_TORCH);
    }
}
