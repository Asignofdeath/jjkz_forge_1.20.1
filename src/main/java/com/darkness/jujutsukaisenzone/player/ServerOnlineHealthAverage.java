package com.darkness.jujutsukaisenzone.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 当前服务器在线玩家最大生命的平均值（「术师越强，怪越强」的参照）。
 */
public final class ServerOnlineHealthAverage
{
    private ServerOnlineHealthAverage()
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
