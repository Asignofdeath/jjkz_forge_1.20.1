package com.darkness.jujutsukaisenzone.entity.util;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link SpecialEffectEntity} 超远程飞行时的临时区块强制加载。
 * <p>
 * 行为类似玩家周围区块：进入飞行路径则加载，远离则卸载。通过 {@link ForgeChunkManager} 以特效实体 UUID
 * 持有 ticket；飞行结束或 {@link SpecialEffectEntity#remove} 前必须 {@link Tracker#releaseAll}。
 * <p>
 * 典型用法：
 * <pre>{@code
 * Tracker tracker = SpecialEffectChunkForce.create(SpecialEffectChunkForce.Options.defaults());
 * // 每 tick（位移前）：
 * tracker.tickAlongPath(serverLevel, entity, velocity);
 * entity.advanceVelocityFlightUnbounded(velocity);
 * // 结束：
 * tracker.releaseAll(serverLevel, entity);
 * }</pre>
 */
public final class SpecialEffectChunkForce
{
    /** 单区块边长（格），用于沿飞行方向预加载前方区块。 */
    public static final double BLOCKS_PER_CHUNK = 16.0D;

    private SpecialEffectChunkForce()
    {
    }

    /** 滑动窗口参数：前方预加载层数 + 身后保留层数。 */
    public static final class Options
    {
        private final int aheadChunks;
        private final int trailChunks;

        private Options(int aheadChunks, int trailChunks)
        {
            this.aheadChunks = Math.max(0, aheadChunks);
            this.trailChunks = Math.max(0, trailChunks);
        }

        public static Options of(int aheadChunks, int trailChunks)
        {
            return new Options(aheadChunks, trailChunks);
        }

        /** 默认：前方 2 区块预加载，身后 1 区块保留。 */
        public static Options defaults()
        {
            return of(2, 1);
        }

        public int aheadChunks()
        {
            return this.aheadChunks;
        }

        public int trailChunks()
        {
            return this.trailChunks;
        }

        /** 当前位置与仍强制加载区块的最大切比雪夫距离。 */
        public int retainRadius()
        {
            return this.aheadChunks + this.trailChunks;
        }
    }

    /**
     * 一次飞行会话持有的强制加载 ticket；由 Behavior 创建并在结束时释放。
     */
    public static final class Tracker
    {
        private final Options options;
        private final Set<Long> forcedChunkKeys = new LinkedHashSet<>();

        private Tracker(Options options)
        {
            this.options = options;
        }

        /**
         * 每 tick 在位移前调用：强制加载当前及前方区块，释放身后过远的区块。
         */
        public void tickAlongPath(ServerLevel level, SpecialEffectEntity entity, Vec3 flightDirection)
        {
            SpecialEffectChunkForce.tickAlongPath(this, level, entity, flightDirection);
        }

        /** 释放本 tracker 持有的全部 ticket。 */
        public void releaseAll(ServerLevel level, SpecialEffectEntity entity)
        {
            SpecialEffectChunkForce.releaseAll(this, level, entity);
        }

        public boolean isActive()
        {
            return !this.forcedChunkKeys.isEmpty();
        }

        public Options options()
        {
            return this.options;
        }
    }

    public static Tracker create(Options options)
    {
        return new Tracker(options);
    }

    public static Tracker create(int aheadChunks, int trailChunks)
    {
        return create(Options.of(aheadChunks, trailChunks));
    }

    /**
     * 从 {@code origin} 起算，{@code current} 是否已达最大飞行距离（格）。
     */
    public static boolean hasExceededDistance(Vec3 origin, Vec3 current, double maxDistanceBlocks)
    {
        return origin.distanceTo(current) >= maxDistanceBlocks;
    }

    /**
     * 将区块数换算为格数距离上限。
     */
    public static double blocksFromChunks(int chunkCount)
    {
        return Math.max(0, chunkCount) * BLOCKS_PER_CHUNK;
    }

    static void tickAlongPath(Tracker tracker, ServerLevel level, SpecialEffectEntity entity, Vec3 flightDirection)
    {
        ChunkPos current = entity.chunkPosition();
        Vec3 dir = normalizeDirection(flightDirection);

        force(tracker, level, entity, current);
        Vec3 sample = entity.position();
        for (int i = 1; i <= tracker.options.aheadChunks(); i++)
        {
            sample = sample.add(dir.scale(BLOCKS_PER_CHUNK));
            force(tracker, level, entity, new ChunkPos(BlockPos.containing(sample)));
        }

        int retainRadius = tracker.options.retainRadius();
        Iterator<Long> iterator = tracker.forcedChunkKeys.iterator();
        while (iterator.hasNext())
        {
            ChunkPos forced = new ChunkPos(iterator.next());
            int dx = Math.abs(forced.x - current.x);
            int dz = Math.abs(forced.z - current.z);
            if (Math.max(dx, dz) > retainRadius)
            {
                unforce(level, entity, forced);
                iterator.remove();
            }
        }
    }

    static void releaseAll(Tracker tracker, ServerLevel level, SpecialEffectEntity entity)
    {
        for (Long key : tracker.forcedChunkKeys)
        {
            unforce(level, entity, new ChunkPos(key));
        }
        tracker.forcedChunkKeys.clear();
    }

    private static void force(Tracker tracker, ServerLevel level, SpecialEffectEntity entity, ChunkPos pos)
    {
        long key = pos.toLong();
        if (!tracker.forcedChunkKeys.add(key))
        {
            return;
        }
        ForgeChunkManager.forceChunk(level, JuJutsuKaisenZone.MODID, entity, pos.x, pos.z, true, true);
    }

    private static void unforce(ServerLevel level, SpecialEffectEntity entity, ChunkPos pos)
    {
        ForgeChunkManager.forceChunk(level, JuJutsuKaisenZone.MODID, entity, pos.x, pos.z, false, true);
    }

    private static Vec3 normalizeDirection(Vec3 direction)
    {
        if (direction.lengthSqr() < 1.0E-8D)
        {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return direction.normalize();
    }
}
