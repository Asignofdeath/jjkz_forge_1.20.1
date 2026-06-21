package com.darkness.jujutsukaisenzone.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 天元之楔结界内的咒灵刷新点（内存存储，与结界记录同生命周期）。
 */
public final class TengenWedgeCurseSpiritSpawns
{
    public static final int MIN_REQUIRED = 6;
    private static final int PARTICLE_INTERVAL_TICKS = 4;
    private static final int PARTICLE_COUNT = 10;
    private static final float PARTICLE_SPREAD = 0.35F;

    private static final ConcurrentMap<BarrierKey, CopyOnWriteArrayList<SpawnPoint>> SPAWN_POINTS =
            new ConcurrentHashMap<>();

    private TengenWedgeCurseSpiritSpawns()
    {
    }

    public static boolean addSpawnPoint(
            TengenWedgeBarrierLogic.TengenWedgeBarrier barrier,
            Vec3 position,
            CurseSpiritSpawnGrade grade)
    {
        if (!isInsideBarrier(barrier, position))
        {
            return false;
        }

        BarrierKey key = BarrierKey.from(barrier);
        SpawnPoint point = new SpawnPoint(position.x, position.y, position.z, grade);
        SPAWN_POINTS.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(point);
        return true;
    }

    public static int countForBarrier(TengenWedgeBarrierLogic.TengenWedgeBarrier barrier)
    {
        CopyOnWriteArrayList<SpawnPoint> points = SPAWN_POINTS.get(BarrierKey.from(barrier));
        return points == null ? 0 : points.size();
    }

    public static void removeForBarrier(TengenWedgeBarrierLogic.TengenWedgeBarrier barrier)
    {
        SPAWN_POINTS.remove(BarrierKey.from(barrier));
    }

    public static void tickLevel(ServerLevel level)
    {
        ResourceKey<Level> dimension = level.dimension();
        if (level.getGameTime() % PARTICLE_INTERVAL_TICKS != 0)
        {
            return;
        }

        for (var entry : SPAWN_POINTS.entrySet())
        {
            if (!entry.getKey().dimension.equals(dimension))
            {
                continue;
            }
            for (SpawnPoint point : entry.getValue())
            {
                level.sendParticles(
                        point.grade.dustParticle(),
                        point.x,
                        point.y + 0.15D,
                        point.z,
                        PARTICLE_COUNT,
                        PARTICLE_SPREAD,
                        0.55F,
                        PARTICLE_SPREAD,
                        0.025D);
            }
        }
    }

    private static boolean isInsideBarrier(TengenWedgeBarrierLogic.TengenWedgeBarrier barrier, Vec3 pos)
    {
        BlockPos center = barrier.getCenter();
        double cx = center.getX() + 0.5D;
        double cy = center.getY() + 0.5D;
        double cz = center.getZ() + 0.5D;
        double radiusSquared = (double) barrier.getRadius() * barrier.getRadius();
        return TengenWedgeBarrierLogic.isInsideSphere(pos, cx, cy, cz, radiusSquared);
    }

    private record BarrierKey(UUID ownerUuid, ResourceKey<Level> dimension, BlockPos center)
    {
        static BarrierKey from(TengenWedgeBarrierLogic.TengenWedgeBarrier barrier)
        {
            return new BarrierKey(barrier.getOwnerUuid(), barrier.getDimension(), barrier.getCenter());
        }

        BarrierKey
        {
            Objects.requireNonNull(ownerUuid);
            Objects.requireNonNull(dimension);
            Objects.requireNonNull(center);
        }
    }

    private record SpawnPoint(double x, double y, double z, CurseSpiritSpawnGrade grade)
    {
    }
}
