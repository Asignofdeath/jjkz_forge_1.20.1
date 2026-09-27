package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SP 节点运行时：点亮 / 重置 / 质量槽位。
 */
public final class PlayerSpNodeData
{
    /** 质量<b>上限</b>的默认值。语义是权重和（Σ spCost），不再是「可点节点个数」。 */
    public static final int DEFAULT_QUALITY = 6;
    private static final String CLIENT_TAG_NODES = "jujutsukaisenzone_sp_nodes";
    private static final String CLIENT_TAG_QUALITY = "jujutsukaisenzone_sp_node_quality";

    private PlayerSpNodeData()
    {
    }

    public static int getQuality(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return Math.max(1, JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).spNodeQuality);
        }
        return Math.max(1, player.getPersistentData().getInt(CLIENT_TAG_QUALITY));
    }

    public static void setQuality(ServerPlayer player, int quality)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        data.spNodeQuality = Math.max(1, quality);
        JjkzPlayerDataStorage.get(player).setDirty();
        syncToClient(player);
        PlayerCombatPowerData.syncToClient(player);
    }

    public static Set<String> getUnlocked(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return Collections.unmodifiableSet(new LinkedHashSet<>(
                    JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).spUnlockedNodes));
        }
        CompoundTag root = player.getPersistentData().getCompound(CLIENT_TAG_NODES);
        Set<String> result = new LinkedHashSet<>();
        ListTag list = root.getList("ids", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++)
        {
            result.add(list.getString(i));
        }
        return result;
    }

    public static boolean isUnlocked(Player player, String nodeId)
    {
        return getUnlocked(player).contains(nodeId);
    }

    public static int unlockedCount(Player player)
    {
        return getUnlocked(player).size();
    }

    public static int sumPhysicalBonus(Player player)
    {
        int sum = 0;
        for (String id : getUnlocked(player))
        {
            sum += SpNodeRegistry.find(id).map(SpNodeRegistry.SpNodeDef::physicalBonus).orElse(0);
        }
        return sum;
    }

    public static int sumTechniqueBonus(Player player)
    {
        int sum = 0;
        for (String id : getUnlocked(player))
        {
            sum += SpNodeRegistry.find(id).map(SpNodeRegistry.SpNodeDef::techniqueBonus).orElse(0);
        }
        return sum;
    }

    public static SpNodeUnlockResult tryUnlock(ServerPlayer player, String nodeId)
    {
        OptionalDef def = resolve(nodeId);
        if (def.missing())
        {
            return SpNodeUnlockResult.UNKNOWN_NODE;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        if (data.spUnlockedNodes.contains(nodeId))
        {
            return SpNodeUnlockResult.ALREADY_UNLOCKED;
        }
        // 质量是「权重和」：SP 节点与 SE 共用一个池，这里问池子是否还放得下这个节点的 spCost。
        if (PlayerQualityPoolData.wouldExceedHardLimit(player, def.cost()))
        {
            return SpNodeUnlockResult.QUALITY_LIMIT;
        }
        if (PlayerSpData.computeAvailable(data) < def.cost())
        {
            return SpNodeUnlockResult.INSUFFICIENT_SP;
        }
        data.spUnlockedNodes.add(nodeId);
        JjkzPlayerDataStorage.get(player).setDirty();
        PlayerSpData.syncToClient(player);
        syncToClient(player);
        PlayerCombatPowerData.syncToClient(player);
        return SpNodeUnlockResult.SUCCESS;
    }

    public static void resetAll(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        data.spUnlockedNodes.clear();
        JjkzPlayerDataStorage.get(player).setDirty();
        PlayerSpData.syncToClient(player);
        syncToClient(player);
        PlayerCombatPowerData.syncToClient(player);
    }

    public static void applyClientSnapshot(Player player, Set<String> nodeIds, int quality)
    {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (String id : nodeIds)
        {
            if (id != null && !id.isBlank())
            {
                list.add(StringTag.valueOf(id));
            }
        }
        root.put("ids", list);
        player.getPersistentData().put(CLIENT_TAG_NODES, root);
        player.getPersistentData().putInt(CLIENT_TAG_QUALITY, Math.max(1, quality));
    }

    public static void syncToClient(ServerPlayer player)
    {
        ModNetwork.sendSpNodesSync(player, getUnlocked(player), getQuality(player));
    }

    private static OptionalDef resolve(String nodeId)
    {
        return SpNodeRegistry.find(nodeId)
                .map(d -> new OptionalDef(false, d.spCost()))
                .orElse(new OptionalDef(true, 0));
    }

    private record OptionalDef(boolean missing, int cost)
    {
    }

    public enum SpNodeUnlockResult
    {
        SUCCESS,
        ALREADY_UNLOCKED,
        UNKNOWN_NODE,
        QUALITY_LIMIT,
        INSUFFICIENT_SP
    }
}
