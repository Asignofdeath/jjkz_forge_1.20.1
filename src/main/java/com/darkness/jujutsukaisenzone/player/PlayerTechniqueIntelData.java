package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 玩家「术式情报」：按生得术式 id 独立累计，作为术式开发界面解锁招式的门槛（不消耗）。
 */
public final class PlayerTechniqueIntelData
{
    private static final String CLIENT_TAG_ROOT = "jujutsukaisenzone_technique_intel";

    private PlayerTechniqueIntelData()
    {
    }

    public static int getIntel(Player player, String techniqueId)
    {
        if (techniqueId == null || techniqueId.isBlank())
        {
            return 0;
        }
        if (player instanceof ServerPlayer serverPlayer)
        {
            return Math.max(0, JjkzPlayerDataStorage.get(serverPlayer)
                    .getOrCreate(serverPlayer)
                    .techniqueIntel
                    .getOrDefault(techniqueId, 0));
        }
        CompoundTag root = player.getPersistentData().getCompound(CLIENT_TAG_ROOT);
        return root.contains(techniqueId) ? Math.max(0, root.getInt(techniqueId)) : 0;
    }

    public static void addIntel(ServerPlayer player, String techniqueId, int amount)
    {
        if (techniqueId == null || techniqueId.isBlank() || amount <= 0)
        {
            return;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        int next = Math.max(0, data.techniqueIntel.getOrDefault(techniqueId, 0) + amount);
        data.techniqueIntel.put(techniqueId, next);
        JjkzPlayerDataStorage.get(player).setDirty();
        syncToClient(player);
    }

    public static boolean tryConsume(ServerPlayer player, String techniqueId, int amount)
    {
        if (techniqueId == null || techniqueId.isBlank() || amount <= 0)
        {
            return true;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        int current = Math.max(0, data.techniqueIntel.getOrDefault(techniqueId, 0));
        if (current < amount)
        {
            return false;
        }
        data.techniqueIntel.put(techniqueId, current - amount);
        JjkzPlayerDataStorage.get(player).setDirty();
        syncToClient(player);
        return true;
    }

    public static Map<String, Integer> snapshot(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        return Collections.unmodifiableMap(new LinkedHashMap<>(data.techniqueIntel));
    }

    public static void applyClientSnapshot(Player player, Map<String, Integer> values)
    {
        CompoundTag root = new CompoundTag();
        for (Map.Entry<String, Integer> entry : values.entrySet())
        {
            if (entry.getKey() == null || entry.getKey().isBlank())
            {
                continue;
            }
            root.putInt(entry.getKey(), Math.max(0, entry.getValue()));
        }
        player.getPersistentData().put(CLIENT_TAG_ROOT, root);
    }

    public static void syncToClient(ServerPlayer player)
    {
        ModNetwork.sendTechniqueIntelSync(player, snapshot(player));
    }
}
