package com.darkness.jujutsukaisenzone.heaven.scale;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 天道用「在线术师强度」探测。当前提供均最大生命；以后可加均等级等。
 */
public final class OnlinePowerProbe
{
    private OnlinePowerProbe()
    {
    }

    /**
     * @return 在线非旁观者玩家 {@code getMaxHealth()} 的算术平均；无人则 0
     */
    public static double averageMaxHealth(MinecraftServer server)
    {
        if (server == null)
        {
            return 0.0D;
        }
        double sum = 0.0D;
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (player == null || player.isSpectator())
            {
                continue;
            }
            float max = player.getMaxHealth();
            if (max <= 0.0F)
            {
                continue;
            }
            sum += max;
            count++;
        }
        if (count <= 0)
        {
            return 0.0D;
        }
        return sum / (double) count;
    }
}
