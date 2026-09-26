package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 调伏仪式式神的强制区块加载（类似玩家加载圈）。
 * <p>
 * 用 {@link ForgeChunkManager} 以式神 UUID 持有 ticket；仪式结束必须 {@link Tracker#releaseAll}。
 */
public final class TenShadowsRitualChunkForce
{
    /** 以式神为中心的切比雪夫半径（区块）。 */
    public static final int RADIUS = 2;

    private TenShadowsRitualChunkForce()
    {
    }

    public static Tracker create()
    {
        return new Tracker();
    }

    public static final class Tracker
    {
        private final Map<UUID, Set<Long>> forcedByEntity = new HashMap<>();

        private Tracker()
        {
        }

        /** 刷新某只式神周围强制加载圈。 */
        public void tick(ServerLevel level, Entity entity)
        {
            if (level == null || entity == null)
            {
                return;
            }
            UUID id = entity.getUUID();
            Set<Long> forced = forcedByEntity.computeIfAbsent(id, ignored -> new LinkedHashSet<>());
            ChunkPos center = entity.chunkPosition();
            Set<Long> needed = new LinkedHashSet<>();
            for (int dx = -RADIUS; dx <= RADIUS; dx++)
            {
                for (int dz = -RADIUS; dz <= RADIUS; dz++)
                {
                    ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                    long key = pos.toLong();
                    needed.add(key);
                    if (forced.add(key))
                    {
                        ForgeChunkManager.forceChunk(level, JuJutsuKaisenZone.MODID, id, pos.x, pos.z, true, true);
                    }
                }
            }
            Iterator<Long> iterator = forced.iterator();
            while (iterator.hasNext())
            {
                long key = iterator.next();
                if (needed.contains(key))
                {
                    continue;
                }
                ChunkPos pos = new ChunkPos(key);
                ForgeChunkManager.forceChunk(level, JuJutsuKaisenZone.MODID, id, pos.x, pos.z, false, true);
                iterator.remove();
            }
        }

        public void releaseEntity(ServerLevel level, UUID entityId)
        {
            Set<Long> forced = forcedByEntity.remove(entityId);
            if (level == null || forced == null)
            {
                return;
            }
            for (long key : forced)
            {
                ChunkPos pos = new ChunkPos(key);
                ForgeChunkManager.forceChunk(level, JuJutsuKaisenZone.MODID, entityId, pos.x, pos.z, false, true);
            }
        }

        public void releaseAll(ServerLevel level)
        {
            for (UUID id : Set.copyOf(forcedByEntity.keySet()))
            {
                releaseEntity(level, id);
            }
            forcedByEntity.clear();
        }
    }
}
