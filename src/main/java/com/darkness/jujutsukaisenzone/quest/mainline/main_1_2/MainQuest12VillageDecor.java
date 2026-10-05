package com.darkness.jujutsukaisenzone.quest.mainline.main_1_2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * 主线 1.2：在目标村地表撒红石粉（干枯血迹）与骷髅头（尸骨），加强空村异样感。
 */
public final class MainQuest12VillageDecor
{
    private static final int MIN_REDSTONE = 28;
    private static final int MAX_REDSTONE = 90;
    private static final int MIN_SKULLS = 8;
    private static final int MAX_SKULLS = 22;
    private static final int REDSTONE_CLUSTER = 4;
    private static final int ATTEMPT_MULTIPLIER = 4;

    private MainQuest12VillageDecor()
    {
    }

    public static void place(ServerLevel level, BoundingBox box)
    {
        int minX = box.minX();
        int maxX = box.maxX();
        int minZ = box.minZ();
        int maxZ = box.maxZ();
        if (maxX < minX || maxZ < minZ)
        {
            return;
        }

        forceChunks(level, box);

        int area = Math.max(1, (maxX - minX + 1) * (maxZ - minZ + 1));
        int redstoneTarget = Mth.clamp(area / 48, MIN_REDSTONE, MAX_REDSTONE);
        int skullTarget = Mth.clamp(area / 220, MIN_SKULLS, MAX_SKULLS);
        RandomSource random = level.getRandom();

        int redstonePlaced = 0;
        int redstoneAttempts = redstoneTarget * ATTEMPT_MULTIPLIER;
        for (int i = 0; i < redstoneAttempts && redstonePlaced < redstoneTarget; i++)
        {
            int cx = Mth.randomBetweenInclusive(random, minX, maxX);
            int cz = Mth.randomBetweenInclusive(random, minZ, maxZ);
            BlockPos surface = surfaceAir(level, cx, cz, box);
            if (surface == null)
            {
                continue;
            }
            // 血迹团：中心点周围再抹几格
            for (int n = 0; n < REDSTONE_CLUSTER && redstonePlaced < redstoneTarget; n++)
            {
                int ox = cx + random.nextInt(3) - 1;
                int oz = cz + random.nextInt(3) - 1;
                if (ox < minX || ox > maxX || oz < minZ || oz > maxZ)
                {
                    continue;
                }
                BlockPos pos = surfaceAir(level, ox, oz, box);
                if (pos != null && tryPlaceRedstone(level, pos))
                {
                    redstonePlaced++;
                }
            }
        }

        int skullsPlaced = 0;
        int skullAttempts = skullTarget * ATTEMPT_MULTIPLIER;
        for (int i = 0; i < skullAttempts && skullsPlaced < skullTarget; i++)
        {
            int x = Mth.randomBetweenInclusive(random, minX, maxX);
            int z = Mth.randomBetweenInclusive(random, minZ, maxZ);
            BlockPos pos = surfaceAir(level, x, z, box);
            if (pos == null)
            {
                continue;
            }
            if (tryPlaceSkull(level, pos, random))
            {
                skullsPlaced++;
            }
        }
    }

    private static void forceChunks(ServerLevel level, BoundingBox box)
    {
        int minCx = box.minX() >> 4;
        int maxCx = box.maxX() >> 4;
        int minCz = box.minZ() >> 4;
        int maxCz = box.maxZ() >> 4;
        for (int cx = minCx; cx <= maxCx; cx++)
        {
            for (int cz = minCz; cz <= maxCz; cz++)
            {
                level.getChunk(cx, cz, ChunkStatus.FULL, true);
            }
        }
    }

    private static BlockPos surfaceAir(ServerLevel level, int x, int z, BoundingBox box)
    {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y < box.minY() || y > box.maxY() + 1)
        {
            return null;
        }
        BlockPos air = new BlockPos(x, y, z);
        BlockPos below = air.below();
        if (!level.getBlockState(air).isAir())
        {
            return null;
        }
        BlockState ground = level.getBlockState(below);
        if (ground.isAir() || !ground.isFaceSturdy(level, below, Direction.UP))
        {
            return null;
        }
        // 水面 / 岩浆上不铺
        if (!level.getFluidState(air).isEmpty() || !level.getFluidState(below).isEmpty())
        {
            return null;
        }
        return air;
    }

    private static boolean tryPlaceRedstone(ServerLevel level, BlockPos pos)
    {
        if (!level.getBlockState(pos).isAir())
        {
            return false;
        }
        BlockState state = Blocks.REDSTONE_WIRE.defaultBlockState();
        if (!state.canSurvive(level, pos))
        {
            return false;
        }
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private static boolean tryPlaceSkull(ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (!level.getBlockState(pos).isAir())
        {
            return false;
        }
        BlockState state = Blocks.SKELETON_SKULL.defaultBlockState()
                .setValue(SkullBlock.ROTATION, random.nextInt(16));
        if (!state.canSurvive(level, pos))
        {
            return false;
        }
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
