package com.darkness.jujutsukaisenzone.entity.util;

import com.darkness.jujutsukaisenzone.entity.data.LimitlessBlueConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 苍：将范围内方块转为 {@link FallingBlockEntity} 吸向中心，并在中心堆成球体。
 */
public final class LimitlessBlueBlockPull
{
    public static final String TAG_BLUE_PULLED_BLOCK = "jujutsukaisenzone_blue_pulled_block";

    private LimitlessBlueBlockPull()
    {
    }

    public static void tryConvertWave(
            ServerLevel level,
            Vec3 center,
            Set<Long> convertedPositions,
            int maxCount)
    {
        tryConvertWave(level, center, convertedPositions, maxCount, LimitlessBlueConfig.BLOCK_PULL_RADIUS);
    }

    public static void tryConvertWave(
            ServerLevel level,
            Vec3 center,
            Set<Long> convertedPositions,
            int maxCount,
            double blockPullRadius)
    {
        BlockPos centerPos = BlockPos.containing(center);
        double radius = blockPullRadius;
        int attempts = LimitlessBlueConfig.BLOCK_PULL_SAMPLE_ATTEMPTS;
        int converted = 0;

        for (int i = 0; i < attempts && converted < maxCount; i++)
        {
            BlockPos pos = randomPosInSphere(level, centerPos, radius);
            if (!level.isLoaded(pos))
            {
                continue;
            }

            long key = pos.asLong();
            if (convertedPositions.contains(key))
            {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!canPullBlock(state))
            {
                continue;
            }

            FallingBlockEntity flying = FallingBlockEntity.fall(level, pos, state);
            if (flying == null)
            {
                continue;
            }

            Vec3 spawn = Vec3.atCenterOf(pos);
            flying.setPos(spawn.x, spawn.y, spawn.z);
            flying.xo = spawn.x;
            flying.yo = spawn.y;
            flying.zo = spawn.z;
            flying.getPersistentData().putBoolean(TAG_BLUE_PULLED_BLOCK, true);
            convertedPositions.add(key);
            converted++;
        }
    }

    public static void pullFlyingBlocks(ServerLevel level, Vec3 center, double radius)
    {
        AABB area = new AABB(center, center).inflate(radius);
        for (FallingBlockEntity block : level.getEntitiesOfClass(FallingBlockEntity.class, area))
        {
            if (!block.getPersistentData().getBoolean(TAG_BLUE_PULLED_BLOCK))
            {
                continue;
            }

            pullBlockToward(center, radius, block);
        }
    }

    public static boolean tryAbsorbIntoBall(ServerLevel level, FallingBlockEntity block, BallStack ballStack)
    {
        if (!block.getPersistentData().getBoolean(TAG_BLUE_PULLED_BLOCK))
        {
            return false;
        }

        Vec3 center = Vec3.atCenterOf(ballStack.origin());
        if (block.position().distanceTo(center) > LimitlessBlueConfig.BLOCK_BALL_ABSORB_DISTANCE)
        {
            return false;
        }

        BlockState state = block.getBlockState();
        if (ballStack.tryPlace(level, state))
        {
            block.discard();
            return true;
        }

        block.discard();
        return true;
    }

    private static void pullBlockToward(Vec3 center, double radius, FallingBlockEntity block)
    {
        Vec3 blockCenter = block.position();
        Vec3 offset = center.subtract(blockCenter);
        double distance = offset.length();
        if (distance < 1.0E-4D)
        {
            return;
        }

        double strength = LimitlessBlueConfig.BLOCK_PULL_STRENGTH
                + Math.max(0.0D, radius - distance) * LimitlessBlueConfig.BLOCK_PULL_DISTANCE_BONUS;
        Vec3 pull = offset.normalize().scale(strength);
        block.setDeltaMovement(block.getDeltaMovement().scale(0.45D).add(pull));
    }

    public static boolean canPullBlock(BlockState state)
    {
        if (state.isAir() || !state.getFluidState().isEmpty())
        {
            return false;
        }

        return state.getBlock() != Blocks.BEDROCK && state.getBlock() != Blocks.OBSIDIAN;
    }

    private static BlockPos randomPosInSphere(ServerLevel level, BlockPos center, double radius)
    {
        double u = level.random.nextDouble();
        double v = level.random.nextDouble();
        double w = level.random.nextDouble();

        double theta = Math.PI * 2.0D * u;
        double phi = Math.acos(2.0D * v - 1.0D);
        double dist = radius * Math.cbrt(w);

        int dx = Mth.floor(dist * Math.sin(phi) * Math.cos(theta));
        int dy = Mth.floor(dist * Math.sin(phi) * Math.sin(theta));
        int dz = Mth.floor(dist * Math.cos(phi));
        return center.offset(dx, dy, dz);
    }

    /** 按距中心由近到远预生成球体槽位，用于在苍中心堆球。 */
    public static final class BallStack
    {
        private final BlockPos origin;
        private final List<BlockPos> slotOffsets;
        private int nextSlotIndex;

        public BallStack(Vec3 center)
        {
            this.origin = BlockPos.containing(center);
            this.slotOffsets = buildSphereOffsets(LimitlessBlueConfig.BLOCK_BALL_MAX_RADIUS);
            this.nextSlotIndex = 0;
        }

        public BlockPos origin()
        {
            return this.origin;
        }

        public boolean tryPlace(ServerLevel level, BlockState state)
        {
            while (this.nextSlotIndex < this.slotOffsets.size())
            {
                BlockPos target = this.origin.offset(this.slotOffsets.get(this.nextSlotIndex++));
                if (!level.isLoaded(target))
                {
                    continue;
                }

                BlockState existing = level.getBlockState(target);
                if (existing.isAir())
                {
                    level.setBlockAndUpdate(target, state);
                    return true;
                }

                if (existing.canBeReplaced() && state.canSurvive(level, target))
                {
                    level.setBlockAndUpdate(target, state);
                    return true;
                }
            }

            return false;
        }

        private static List<BlockPos> buildSphereOffsets(int maxRadius)
        {
            List<BlockPos> offsets = new ArrayList<>();
            for (int dx = -maxRadius; dx <= maxRadius; dx++)
            {
                for (int dy = -maxRadius; dy <= maxRadius; dy++)
                {
                    for (int dz = -maxRadius; dz <= maxRadius; dz++)
                    {
                        if (dx * dx + dy * dy + dz * dz <= maxRadius * maxRadius)
                        {
                            offsets.add(new BlockPos(dx, dy, dz));
                        }
                    }
                }
            }

            offsets.sort(Comparator.comparingDouble(pos -> pos.distSqr(BlockPos.ZERO)));
            return offsets;
        }
    }
}
