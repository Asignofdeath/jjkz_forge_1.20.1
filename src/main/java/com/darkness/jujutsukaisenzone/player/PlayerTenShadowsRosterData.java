package com.darkness.jujutsukaisenzone.player;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 当前场上式神 UUID 名册（会话态）。键为 {@link com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami#index()}。
 */
public final class PlayerTenShadowsRosterData
{
    private static final Map<UUID, Map<Integer, List<UUID>>> ROSTER = new ConcurrentHashMap<>();

    private PlayerTenShadowsRosterData()
    {
    }

    public static List<UUID> get(ServerPlayer player, int shikigamiIndex)
    {
        Map<Integer, List<UUID>> byIndex = ROSTER.get(player.getUUID());
        if (byIndex == null)
        {
            return List.of();
        }
        List<UUID> list = byIndex.get(shikigamiIndex);
        return list == null ? List.of() : List.copyOf(list);
    }

    public static boolean hasAliveEntry(ServerPlayer player, int shikigamiIndex)
    {
        return !get(player, shikigamiIndex).isEmpty();
    }

    public static void put(ServerPlayer player, int shikigamiIndex, List<UUID> entityIds)
    {
        ROSTER.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>())
                .put(shikigamiIndex, new ArrayList<>(entityIds));
    }

    public static void clearSlot(ServerPlayer player, int shikigamiIndex)
    {
        Map<Integer, List<UUID>> byIndex = ROSTER.get(player.getUUID());
        if (byIndex != null)
        {
            byIndex.remove(shikigamiIndex);
            if (byIndex.isEmpty())
            {
                ROSTER.remove(player.getUUID());
            }
        }
    }

    public static Map<Integer, List<UUID>> snapshot(ServerPlayer player)
    {
        Map<Integer, List<UUID>> byIndex = ROSTER.get(player.getUUID());
        if (byIndex == null || byIndex.isEmpty())
        {
            return Map.of();
        }
        Map<Integer, List<UUID>> copy = new ConcurrentHashMap<>();
        byIndex.forEach((index, list) -> copy.put(index, List.copyOf(list)));
        return Collections.unmodifiableMap(copy);
    }

    public static void clearAll(ServerPlayer player)
    {
        ROSTER.remove(player.getUUID());
    }
}
