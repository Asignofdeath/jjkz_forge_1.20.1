package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 御厨子术式开发：解锁「解」切割后累计的黑闪次数（用于「捌」前置里程碑）。
 */
public final class PlayerShrineBlackFlashMilestoneData
{
    private static final String TAG = "jujutsukaisenzone_shrine_bf_milestone_count";

    private PlayerShrineBlackFlashMilestoneData()
    {
    }

    public static int getCount(Player player)
    {
        return Math.max(0, player.getPersistentData().getInt(TAG));
    }

    public static boolean isMilestoneMet(Player player, int required)
    {
        return required <= 0 || getCount(player) >= required;
    }

    public static void increment(ServerPlayer player)
    {
        int next = getCount(player) + 1;
        player.getPersistentData().putInt(TAG, next);
        ModNetwork.sendShrineBlackFlashMilestoneSync(player, next);
    }

    public static void applyClientCount(Player player, int count)
    {
        player.getPersistentData().putInt(TAG, Math.max(0, count));
    }
}
