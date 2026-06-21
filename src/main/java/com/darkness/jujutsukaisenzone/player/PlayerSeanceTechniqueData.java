package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 降灵术等级数据：控制拟态技能可用性与维持咒力减免。
 */
public final class PlayerSeanceTechniqueData
{
    private static final String CLIENT_TAG_LEARNED = "jujutsukaisenzone_seance_technique_learned";
    private static final String CLIENT_TAG_TIER = "jujutsukaisenzone_seance_technique_tier";

    private PlayerSeanceTechniqueData()
    {
    }

    public static SeanceTechniqueLevel getLevel(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        return SeanceTechniqueLevel.fromString(data.seanceTechniqueLevel);
    }

    /** 客户端/服务端均可读取降灵术等级（客户端依赖 S2C 镜像 tier）。 */
    public static SeanceTechniqueLevel getLevel(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return getLevel(serverPlayer);
        }
        return SeanceTechniqueLevel.fromTier(player.getPersistentData().getInt(CLIENT_TAG_TIER));
    }

    public static void setLevel(ServerPlayer player, SeanceTechniqueLevel level)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        data.seanceTechniqueLevel = level.name();
        markDirty(player);
        if (level == SeanceTechniqueLevel.UNLEARNED)
        {
            PlayerMimicryData.forceDisable(player);
        }
        ModNetwork.sendSeanceTechniqueLearnedSync(player, hasLearned(player), getLevel(player).getTier());
    }

    public static boolean hasLearned(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return getLevel(serverPlayer) != SeanceTechniqueLevel.UNLEARNED;
        }
        return player.getPersistentData().getBoolean(CLIENT_TAG_LEARNED);
    }

    public static void applyClientLearned(Player player, boolean learned, int tier)
    {
        player.getPersistentData().putBoolean(CLIENT_TAG_LEARNED, learned);
        player.getPersistentData().putInt(CLIENT_TAG_TIER, Math.max(0, tier));
    }

    public static double getMaintainCostReduction(ServerPlayer player)
    {
        return getLevel(player).getMaintainCostReduction();
    }

    public static int getStudybookReadCount(ServerPlayer player)
    {
        return Math.max(0, getData(player).soulStudybookReadCount);
    }

    public static void setStudybookReadCount(ServerPlayer player, int count)
    {
        getData(player).soulStudybookReadCount = Math.max(0, count);
        markDirty(player);
    }

    public static int getUpgradeChancePercent(ServerPlayer player)
    {
        return Math.max(0, Math.min(8, getData(player).seanceTechniqueUpgradeChance));
    }

    public static void setUpgradeChancePercent(ServerPlayer player, int percent)
    {
        getData(player).seanceTechniqueUpgradeChance = Math.max(0, Math.min(8, percent));
        markDirty(player);
    }

    private static JjkzPlayerDataStorage.PlayerDataEntry getData(ServerPlayer player)
    {
        PlayerCursedEnergyData.initializeIfAbsent(player);
        return JjkzPlayerDataStorage.get(player).getOrCreate(player);
    }

    private static void markDirty(ServerPlayer player)
    {
        JjkzPlayerDataStorage.get(player).setDirty();
    }
}
