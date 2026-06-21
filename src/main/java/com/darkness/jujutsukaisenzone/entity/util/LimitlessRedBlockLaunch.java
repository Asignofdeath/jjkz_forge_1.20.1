package com.darkness.jujutsukaisenzone.entity.util;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.behavior.LimitlessRedArrowBehavior;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRedConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 赫爆炸：将地表方块转为 {@link FallingBlockEntity} 并向外弹飞。
 */
public final class LimitlessRedBlockLaunch
{
    private static final double HORIZONTAL_DIR_EPSILON = 0.25D;

    private LimitlessRedBlockLaunch()
    {
    }

    public static boolean isRedExplosion(Explosion explosion)
    {
        if (!(explosion.getDirectSourceEntity() instanceof SpecialEffectEntity entity))
        {
            return false;
        }
        return entity.getEffectBehavior() instanceof LimitlessRedArrowBehavior;
    }

    public static void launchFromExplosion(ServerLevel level, Explosion explosion, List<BlockPos> affectedBlocks)
    {
        Vec3 center = explosion.getPosition();
        float radius = LimitlessRedConfig.ARROW_EXPLOSION_POWER;
        if (explosion.getDirectSourceEntity() instanceof SpecialEffectEntity entity
                && entity.getEffectBehavior() instanceof LimitlessRedArrowBehavior arrowBehavior)
        {
            radius = arrowBehavior.explosionPower();
        }

        List<BlockPos> candidates = collectSurfaceCandidates(level, center, radius, affectedBlocks);
        candidates.sort(Comparator.comparingDouble(pos -> -horizontalDistance(center, pos)));

        List<BlockPos> launched = new ArrayList<>();
        int count = 0;

        for (BlockPos pos : candidates)
        {
            if (count >= LimitlessRedConfig.BLOCK_LAUNCH_MAX_COUNT)
            {
                break;
            }

            BlockState state = level.getBlockState(pos);
            if (!isLaunchableState(level, pos, state))
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
            flying.setHurtsEntities(
                    LimitlessRedConfig.BLOCK_LAUNCH_HURT_DAMAGE_PER_DISTANCE,
                    LimitlessRedConfig.BLOCK_LAUNCH_HURT_MAX);
            flying.setDeltaMovement(computeLaunchVelocity(level, center, spawn, radius));
            launched.add(pos);
            count++;
        }

        affectedBlocks.removeAll(launched);
    }

    /**
     * 每个 (x,z) 列只取最高且露天的一层，避免从爆炸球体内部“凭空”生成方块。
     */
    private static List<BlockPos> collectSurfaceCandidates(
            ServerLevel level,
            Vec3 center,
            float radius,
            List<BlockPos> affectedBlocks)
    {
        Map<Long, BlockPos> topByColumn = new HashMap<>();

        for (BlockPos pos : affectedBlocks)
        {
            double horizontal = horizontalDistance(center, pos);
            if (horizontal < LimitlessRedConfig.BLOCK_LAUNCH_MIN_DISTANCE || horizontal > radius)
            {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!isLaunchableState(level, pos, state) || !hasOpenSkyAbove(level, pos))
            {
                continue;
            }

            long columnKey = columnKey(pos);
            BlockPos current = topByColumn.get(columnKey);
            if (current == null || pos.getY() > current.getY())
            {
                topByColumn.put(columnKey, pos.immutable());
            }
        }

        return new ArrayList<>(topByColumn.values());
    }

    private static Vec3 computeLaunchVelocity(ServerLevel level, Vec3 center, Vec3 blockCenter, float radius)
    {
        Vec3 offset = blockCenter.subtract(center);
        double dist = offset.horizontalDistance();
        if (dist < HORIZONTAL_DIR_EPSILON)
        {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            offset = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            dist = 1.0D;
        }

        Vec3 horizontalDir = new Vec3(offset.x / dist, 0.0D, offset.z / dist);
        double falloff = 1.0D - Math.min(blockCenter.distanceTo(center) / radius, 1.0D);
        double speed = LimitlessRedConfig.BLOCK_LAUNCH_SPEED * (0.55D + falloff * 0.45D);
        double up = LimitlessRedConfig.BLOCK_LAUNCH_UP_SPEED * (0.5D + falloff * 0.5D);

        double spread = 0.18D;
        double vx = horizontalDir.x * speed + Mth.nextDouble(level.random, -spread, spread);
        double vy = up + Mth.nextDouble(level.random, 0.0D, spread);
        double vz = horizontalDir.z * speed + Mth.nextDouble(level.random, -spread, spread);
        return new Vec3(vx, vy, vz);
    }

    private static boolean isLaunchableState(ServerLevel level, BlockPos pos, BlockState state)
    {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    /** 上方直到世界高度均为空气，确保是真实地表而非爆炸空腔内部。 */
    private static boolean hasOpenSkyAbove(ServerLevel level, BlockPos pos)
    {
        for (BlockPos scan = pos.above(); scan.getY() < level.getMaxBuildHeight(); scan = scan.above())
        {
            if (!level.getBlockState(scan).isAir())
            {
                return false;
            }
        }
        return true;
    }

    private static long columnKey(BlockPos pos)
    {
        return BlockPos.asLong(pos.getX(), 0, pos.getZ());
    }

    private static double horizontalDistance(Vec3 center, BlockPos pos)
    {
        return center.subtract(Vec3.atCenterOf(pos)).horizontalDistance();
    }
}
