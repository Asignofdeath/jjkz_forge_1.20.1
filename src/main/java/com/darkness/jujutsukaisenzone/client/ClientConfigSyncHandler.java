package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 联机时「以房主 / 服主的配置文件为唯一真相」的客户端收尾。
 * <p>
 * ⛔ 断开连接时必须把被服务端盖掉的数值还原成本地 {@code settings.toml} 的值。
 * {@code Config} 的静态初始化一个 JVM <b>只跑一次</b>，不还原的话「先连服务器、再进单人存档」
 * 会继续沿用服务器的数值 —— 单人手感被静默改掉，而且极难查。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientConfigSyncHandler
{
    private ClientConfigSyncHandler()
    {
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        // 不区分单机与联机：统一按「断开就回到本地文件」处理。
        // 单机与局域网房主本来就没被服务端盖过，重读只是拿到同样的值，没有副作用；
        // 留特例反而容易漏。
        Config.reloadFromLocalFile();
    }
}
