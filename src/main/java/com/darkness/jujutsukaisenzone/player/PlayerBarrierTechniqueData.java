package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 结界术等级数据管理类。
 * <p>
 * 提供获取、设置玩家结界术等级的方法。
 */
public final class PlayerBarrierTechniqueData
{
    private static final String CLIENT_TAG_LEARNED = "jujutsukaisenzone_barrier_technique_learned";

    private PlayerBarrierTechniqueData()
    {
    }

    /**
     * 获取玩家的结界术等级。
     * @param player 玩家
     * @return 结界术等级枚举
     */
    public static BarrierTechniqueLevel getLevel(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        return BarrierTechniqueLevel.fromString(data.barrierTechniqueLevel);
    }

    /**
     * 设置玩家的结界术等级。
     * @param player 玩家
     * @param level 结界术等级
     */
    public static void setLevel(ServerPlayer player, BarrierTechniqueLevel level)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        data.barrierTechniqueLevel = level.name();
        markDirty(player);
        ModNetwork.sendBarrierTechniqueLearnedSync(player, hasLearned(player));
    }

    /**
     * 检查玩家是否习得了结界术（等级不为"未习得"）。
     */
    public static boolean hasLearned(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return getLevel(serverPlayer) != BarrierTechniqueLevel.UNLEARNED;
        }
        return player.getPersistentData().getBoolean(CLIENT_TAG_LEARNED);
    }

    public static void applyClientLearned(Player player, boolean learned)
    {
        player.getPersistentData().putBoolean(CLIENT_TAG_LEARNED, learned);
    }

    public static int getStudybookReadCount(ServerPlayer player)
    {
        return Math.max(0, getData(player).barrierStudybookReadCount);
    }

    public static void setStudybookReadCount(ServerPlayer player, int count)
    {
        getData(player).barrierStudybookReadCount = Math.max(0, count);
        markDirty(player);
    }

    public static boolean isPendingWakeUnlock(ServerPlayer player)
    {
        return getData(player).barrierTechniquePendingWakeUnlock;
    }

    public static void setPendingWakeUnlock(ServerPlayer player, boolean pending)
    {
        getData(player).barrierTechniquePendingWakeUnlock = pending;
        markDirty(player);
    }

    /**
     * 获取玩家的结界术维持消耗减免百分比（0.0 ~ 1.0）。
     * @param player 玩家
     * @return 减免比例
     */
    public static double getMaintainCostReduction(ServerPlayer player)
    {
        return getLevel(player).getMaintainCostReduction();
    }

    public static int getUpgradeChancePercent(ServerPlayer player)
    {
        return Math.max(0, Math.min(8, getData(player).barrierTechniqueUpgradeChance));
    }

    public static void setUpgradeChancePercent(ServerPlayer player, int percent)
    {
        getData(player).barrierTechniqueUpgradeChance = Math.max(0, Math.min(8, percent));
        markDirty(player);
    }

    public static long getLastProcessedGameDay(ServerPlayer player)
    {
        return getData(player).lastProcessedGameDay;
    }

    public static void setLastProcessedGameDay(ServerPlayer player, long day)
    {
        getData(player).lastProcessedGameDay = day;
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
