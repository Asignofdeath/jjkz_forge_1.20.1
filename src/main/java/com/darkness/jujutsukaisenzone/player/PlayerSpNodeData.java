package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeShelfRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SP 节点运行时：货架在售 / 已点亮 / 质量派生。
 */
public final class PlayerSpNodeData
{
    public static final int DEFAULT_QUALITY = SpNodeShelfRules.BASE_QUALITY;

    private static final String CLIENT_TAG_ROOT = "jujutsukaisenzone_sp_shelf";
    private static final String CLIENT_TAG_LIT = "lit";
    private static final String CLIENT_TAG_OFFERS = "offers";
    private static final String CLIENT_TAG_QUALITY = "quality";
    private static final String CLIENT_TAG_ALLOWANCE = "allowance";
    private static final String CLIENT_TAG_TIER = "tier";
    private static final String CLIENT_TAG_XP = "xp";

    private PlayerSpNodeData()
    {
    }

    public static int getQuality(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            int xp = PlayerCursedGradeData.getExperience(serverPlayer);
            return SpNodeShelfRules.qualityCap(xp);
        }
        return Math.max(1, player.getPersistentData().getCompound(CLIENT_TAG_ROOT).getInt(CLIENT_TAG_QUALITY));
    }

    public static int getAllowance(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return SpNodeShelfRules.allowance(PlayerCursedGradeData.getExperience(serverPlayer));
        }
        return Math.max(0, player.getPersistentData().getCompound(CLIENT_TAG_ROOT).getInt(CLIENT_TAG_ALLOWANCE));
    }

    public static int getTier(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return SpNodeShelfRules.tier(PlayerCursedGradeData.getExperience(serverPlayer));
        }
        return Math.max(0, player.getPersistentData().getCompound(CLIENT_TAG_ROOT).getInt(CLIENT_TAG_TIER));
    }

    /** @deprecated 质量由经验派生；保留空操作以免旧命令编译失败。 */
    @Deprecated
    public static void setQuality(ServerPlayer player, int quality)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        data.spNodeQuality = SpNodeShelfRules.qualityCap(data.curseGradeExperience);
        JjkzPlayerDataStorage.get(player).setDirty();
        syncToClient(player);
        PlayerCombatPowerData.syncToClient(player);
    }

    public static List<SpNodeInstance> getLit(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return Collections.unmodifiableList(new ArrayList<>(
                    JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).spLitNodes));
        }
        return readClientList(player, CLIENT_TAG_LIT);
    }

    public static List<SpNodeInstance> getOffers(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return Collections.unmodifiableList(new ArrayList<>(
                    JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).spOffers));
        }
        return readClientList(player, CLIENT_TAG_OFFERS);
    }

    public static boolean isLit(Player player, String instanceId)
    {
        for (SpNodeInstance node : getLit(player))
        {
            if (node.instanceId().equals(instanceId))
            {
                return true;
            }
        }
        return false;
    }

    public static int unlockedCount(Player player)
    {
        return getLit(player).size();
    }

    public static int sumPhysicalBonus(Player player)
    {
        int sum = 0;
        for (SpNodeInstance node : getLit(player))
        {
            sum += node.physicalBonus();
        }
        return sum;
    }

    public static int sumTechniqueBonus(Player player)
    {
        int sum = 0;
        for (SpNodeInstance node : getLit(player))
        {
            sum += node.techniqueBonus();
        }
        return sum;
    }

    public static SpNodeUnlockResult tryUnlock(ServerPlayer player, String instanceId)
    {
        if (instanceId == null || instanceId.isBlank())
        {
            return SpNodeUnlockResult.UNKNOWN_NODE;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        SpNodeInstance offer = null;
        for (SpNodeInstance node : data.spOffers)
        {
            if (node.instanceId().equals(instanceId))
            {
                offer = node;
                break;
            }
        }
        if (offer == null)
        {
            return SpNodeUnlockResult.UNKNOWN_NODE;
        }
        for (SpNodeInstance lit : data.spLitNodes)
        {
            if (lit.instanceId().equals(instanceId))
            {
                return SpNodeUnlockResult.ALREADY_UNLOCKED;
            }
        }
        if (PlayerQualityPoolData.wouldExceedHardLimit(player, offer.spCost()))
        {
            return SpNodeUnlockResult.QUALITY_LIMIT;
        }
        if (PlayerSpData.computeAvailable(data) < offer.spCost())
        {
            return SpNodeUnlockResult.INSUFFICIENT_SP;
        }
        data.spOffers.remove(offer);
        data.spLitNodes.add(offer);
        data.spNodeQuality = SpNodeShelfRules.qualityCap(data.curseGradeExperience);
        JjkzPlayerDataStorage.get(player).setDirty();
        PlayerSpData.syncToClient(player);
        PlayerSpNodeAttributeApplier.recompute(player);
        syncToClient(player);
        PlayerCombatPowerData.syncToClient(player);
        return SpNodeUnlockResult.SUCCESS;
    }

    public static void resetAll(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        List<SpNodeInstance> previous = new ArrayList<>(data.spLitNodes);
        data.spLitNodes.clear();
        JjkzPlayerDataStorage.get(player).setDirty();
        PlayerSpNodeAttributeApplier.clearInstances(player, previous);
        PlayerSpData.syncToClient(player);
        syncToClient(player);
        PlayerCombatPowerData.syncToClient(player);
    }

    public static void applyClientSnapshot(
            Player player,
            List<SpNodeInstance> lit,
            List<SpNodeInstance> offers,
            int quality,
            int allowance,
            int tier,
            int xp)
    {
        CompoundTag root = new CompoundTag();
        root.put(CLIENT_TAG_LIT, writeList(lit));
        root.put(CLIENT_TAG_OFFERS, writeList(offers));
        root.putInt(CLIENT_TAG_QUALITY, Math.max(1, quality));
        root.putInt(CLIENT_TAG_ALLOWANCE, Math.max(0, allowance));
        root.putInt(CLIENT_TAG_TIER, Math.max(0, tier));
        root.putInt(CLIENT_TAG_XP, Math.max(0, xp));
        player.getPersistentData().put(CLIENT_TAG_ROOT, root);
    }

    public static void syncToClient(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        int xp = Math.max(0, data.curseGradeExperience);
        ModNetwork.sendSpNodesSync(
                player,
                List.copyOf(data.spLitNodes),
                List.copyOf(data.spOffers),
                SpNodeShelfRules.qualityCap(xp),
                SpNodeShelfRules.allowance(xp),
                SpNodeShelfRules.tier(xp),
                xp);
    }

    private static ListTag writeList(List<SpNodeInstance> nodes)
    {
        ListTag list = new ListTag();
        if (nodes == null)
        {
            return list;
        }
        for (SpNodeInstance node : nodes)
        {
            if (node != null)
            {
                list.add(node.save());
            }
        }
        return list;
    }

    private static List<SpNodeInstance> readClientList(Player player, String key)
    {
        CompoundTag root = player.getPersistentData().getCompound(CLIENT_TAG_ROOT);
        ListTag list = root.getList(key, Tag.TAG_COMPOUND);
        List<SpNodeInstance> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++)
        {
            SpNodeInstance node = SpNodeInstance.load(list.getCompound(i));
            if (node != null)
            {
                result.add(node);
            }
        }
        return result;
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
