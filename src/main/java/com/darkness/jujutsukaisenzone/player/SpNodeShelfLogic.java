package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeRoller;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeShelfRules;
import net.minecraft.server.level.ServerPlayer;

/**
 * SP 货架编排：升档发货、日终刷新、付费刷新、未点亮软顶。
 * <p>
 * 放在 {@code player} 包内，才能读写 {@link JjkzPlayerDataStorage.PlayerDataEntry} 的包可见字段。
 */
public final class SpNodeShelfLogic
{
    private SpNodeShelfLogic()
    {
    }

    /** 经验变更 / 登录时调用：同步质量派生并补发缺失档位。 */
    public static void syncWithExperience(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        int xp = Math.max(0, data.curseGradeExperience);
        data.spNodeQuality = SpNodeShelfRules.qualityCap(xp);
        grantMissingTiers(player, data, xp);
        trimUnlitOffers(data);
        JjkzPlayerDataStorage.get(player).setDirty();
        afterMutation(player);
    }

    /** 特级前：日终重 roll 全部未点亮。 */
    public static void onDayEnd(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        int xp = Math.max(0, data.curseGradeExperience);
        if (SpNodeShelfRules.isPostSpecial(xp))
        {
            return;
        }
        int tier = SpNodeShelfRules.tier(xp);
        int size = SpNodeShelfRules.dailyShelfSize(tier);
        data.spOffers.clear();
        if (size > 0)
        {
            data.spOffers.addAll(SpNodeRoller.rollMany(player.getRandom(), size, Math.max(1, tier)));
        }
        JjkzPlayerDataStorage.get(player).setDirty();
        afterMutation(player);
    }

    public static RefreshResult tryPaidRefresh(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        int xp = Math.max(0, data.curseGradeExperience);
        if (data.spOffers.isEmpty())
        {
            return RefreshResult.NO_OFFERS;
        }
        int cost = SpNodeShelfRules.refreshCost(xp);
        if (PlayerSpData.getAvailable(player) < cost)
        {
            return RefreshResult.INSUFFICIENT_SP;
        }
        data.spSpentRefresh = Math.max(0, data.spSpentRefresh) + cost;
        if (SpNodeShelfRules.isPostSpecial(xp))
        {
            int last = data.spOffers.size() - 1;
            SpNodeInstance old = data.spOffers.get(last);
            data.spOffers.set(last, SpNodeRoller.rollOne(player.getRandom(), Math.max(1, old.createdTier())));
        }
        else
        {
            int count = data.spOffers.size();
            int tier = Math.max(1, SpNodeShelfRules.tier(xp));
            data.spOffers.clear();
            data.spOffers.addAll(SpNodeRoller.rollMany(player.getRandom(), count, tier));
        }
        JjkzPlayerDataStorage.get(player).setDirty();
        PlayerSpData.syncToClient(player);
        afterMutation(player);
        return RefreshResult.SUCCESS;
    }

    private static void grantMissingTiers(
            ServerPlayer player,
            JjkzPlayerDataStorage.PlayerDataEntry data,
            int xp)
    {
        int targetTier = SpNodeShelfRules.tier(xp);
        int last = Math.max(0, data.spLastGrantedTier);
        if (targetTier <= last)
        {
            return;
        }
        for (int t = last + 1; t <= targetTier; t++)
        {
            int count = SpNodeShelfRules.offersGrantedForTier(t);
            if (count <= 0)
            {
                continue;
            }
            data.spOffers.addAll(SpNodeRoller.rollMany(player.getRandom(), count, t));
        }
        data.spLastGrantedTier = targetTier;
    }

    /** 未点亮超过软顶时丢掉最旧的。 */
    public static void trimUnlitOffers(JjkzPlayerDataStorage.PlayerDataEntry data)
    {
        while (data.spOffers.size() > SpNodeShelfRules.MAX_UNLIT_OFFERS)
        {
            data.spOffers.remove(0);
        }
    }

    private static void afterMutation(ServerPlayer player)
    {
        PlayerSpNodeAttributeApplier.recompute(player);
        PlayerSpNodeData.syncToClient(player);
        PlayerCombatPowerData.syncToClient(player);
    }

    public enum RefreshResult
    {
        SUCCESS,
        NO_OFFERS,
        INSUFFICIENT_SP
    }
}
