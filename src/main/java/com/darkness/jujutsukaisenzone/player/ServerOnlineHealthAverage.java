package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.heaven.scale.OnlinePowerProbe;
import net.minecraft.server.MinecraftServer;

/**
 * @deprecated 迁至 {@link OnlinePowerProbe}；保留转发以免旧调用方断裂。
 */
@Deprecated
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
        return OnlinePowerProbe.averageMaxHealth(server);
    }
}
