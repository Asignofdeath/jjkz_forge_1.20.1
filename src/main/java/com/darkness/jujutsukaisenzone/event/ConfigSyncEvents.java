package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 联机时「以房主 / 服主的配置文件为唯一真相」的服务端一半。
 * <p>
 * 玩家登录时把服务端<b>当前生效</b>的数值推给他；客户端收到后整个盖掉本地
 * （见 {@code SyncModConfigS2CPacket}）。单机与局域网房主是同一个 JVM、静态字段本来就是同一份，
 * 客户端会自行跳过套用，所以这里无条件推是安全的。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ConfigSyncEvents
{
    private ConfigSyncEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
        {
            return;
        }

        ModNetwork.sendModConfigSync(serverPlayer);

        // 单机 / 进世界时客户端侧可能尚未就绪，下一 tick 再补一次（沿用 CursedEnergyEvents 的做法）。
        // 客户端套用是幂等的：同一份表重复套没有副作用。
        serverPlayer.server.execute(() -> ModNetwork.sendModConfigSync(serverPlayer));
    }
}
