package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** 反转术式习得状态与理论书研读进度。 */
public final class PlayerReverseTechniqueData
{
    private static final String TAG = "jujutsukaisenzone_reverse_technique";

    private PlayerReverseTechniqueData()
    {
    }

    public static boolean hasReverseTechnique(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).reverseTechniqueLearned;
        }
        return player.getPersistentData().getBoolean(TAG);
    }

    public static int getStudybookBoostUses(ServerPlayer player)
    {
        return Math.max(0, JjkzPlayerDataStorage.get(player).getOrCreate(player).reverseStudybookBoostUses);
    }

    public static void setStudybookBoostUses(ServerPlayer player, int uses)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        data.reverseStudybookBoostUses = Math.max(0, uses);
        JjkzPlayerDataStorage.get(player).setDirty();
    }

    public static void setLearned(ServerPlayer player, boolean learned)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        data.reverseTechniqueLearned = learned;
        JjkzPlayerDataStorage.get(player).setDirty();
        ModNetwork.sendReverseTechniqueSync(player, learned);
        // 反转术式是术式强度的来源之一，面板必须跟着刷新
        PlayerCombatPowerData.syncToClient(player);
    }

    public static void applyClientLearned(Player player, boolean learned)
    {
        player.getPersistentData().putBoolean(TAG, learned);
    }
}
