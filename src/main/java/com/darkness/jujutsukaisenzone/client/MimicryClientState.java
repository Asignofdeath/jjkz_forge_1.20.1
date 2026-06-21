package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.player.mimicry.MimicryCharacterRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端拟态状态缓存：按玩家 UUID 记录开关与角色 ID，供渲染层（Phase 3 Mixin）查询。
 * <p>
 * 所有玩家的拟态状态均由服务端 {@code SyncMimicryS2CPacket} 下发，本地玩家额外镜像到 persistentData。
 */
public final class MimicryClientState
{
    public record Entry(boolean active, String characterId)
    {
    }

    private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();

    private MimicryClientState()
    {
    }

    public static void apply(UUID playerUuid, boolean active, String characterId)
    {
        if (!active)
        {
            CACHE.put(playerUuid, new Entry(false, ""));
            return;
        }
        CACHE.put(playerUuid, new Entry(true, characterId == null ? "" : characterId));
    }

    public static Optional<MimicryCharacterRegistry.MimicryProfile> getProfile(UUID playerUuid)
    {
        Entry entry = CACHE.get(playerUuid);
        if (entry == null || !entry.active || entry.characterId.isBlank())
        {
            return Optional.empty();
        }
        return MimicryCharacterRegistry.getProfile(entry.characterId);
    }

    public static void remove(UUID playerUuid)
    {
        CACHE.remove(playerUuid);
    }
}
