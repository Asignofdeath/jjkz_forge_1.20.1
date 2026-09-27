package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 修复术式熔断习得状态（通用技能，无等级；与反转术式 / 领域展开同型）。
 */
public final class PlayerMeltdownRepairData
{
    private static final String CLIENT_TAG_LEARNED = "jujutsukaisenzone_meltdown_repair_learned";

    private PlayerMeltdownRepairData()
    {
    }

    public static boolean hasLearned(ServerPlayer player)
    {
        return getData(player).meltdownRepairLearned;
    }

    public static boolean hasLearned(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return hasLearned(serverPlayer);
        }
        return player.getPersistentData().getBoolean(CLIENT_TAG_LEARNED);
    }

    public static void setLearned(ServerPlayer player, boolean learned)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        data.meltdownRepairLearned = learned;
        markDirty(player);
        ModNetwork.sendMeltdownRepairLearnedSync(player, learned);
    }

    public static void applyClientLearned(Player player, boolean learned)
    {
        player.getPersistentData().putBoolean(CLIENT_TAG_LEARNED, learned);
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
