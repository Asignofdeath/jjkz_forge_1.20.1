package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 术师的领域模式（常规 / 瞬发 / 开放式）。会话态，默认常规。
 * <p>
 * 与 {@link PlayerDomainModeData} 同一套枚举；不写存档。停机由
 * {@code SessionStateCleanupEvents} 清表。
 */
public final class AiDomainModeData
{
    private static final Map<UUID, PlayerDomainModeData.Mode> MODE = new ConcurrentHashMap<>();

    private AiDomainModeData()
    {
    }

    public static void set(@Nullable LivingEntity entity, PlayerDomainModeData.Mode mode)
    {
        if (entity == null)
        {
            return;
        }
        MODE.put(entity.getUUID(), mode == null ? PlayerDomainModeData.Mode.NORMAL : mode);
    }

    public static PlayerDomainModeData.Mode get(@Nullable LivingEntity entity)
    {
        if (entity == null)
        {
            return PlayerDomainModeData.Mode.NORMAL;
        }
        return MODE.getOrDefault(entity.getUUID(), PlayerDomainModeData.Mode.NORMAL);
    }

    public static boolean isOpen(@Nullable LivingEntity entity)
    {
        return get(entity) == PlayerDomainModeData.Mode.OPEN;
    }

    public static boolean isInstant(@Nullable LivingEntity entity)
    {
        return get(entity) == PlayerDomainModeData.Mode.INSTANT;
    }

    public static void clear(@Nullable UUID uuid)
    {
        if (uuid != null)
        {
            MODE.remove(uuid);
        }
    }

    public static void clearAll()
    {
        MODE.clear();
    }
}
