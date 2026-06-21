package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.MimicryClientState;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 拟态状态的多人同步：登录时互相同步、进入视野时补发。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MimicryEvents
{
    private MimicryEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer joining))
        {
            return;
        }

        for (ServerPlayer online : joining.server.getPlayerList().getPlayers())
        {
            ModNetwork.sendMimicrySyncTo(joining, online);
            if (online != joining)
            {
                ModNetwork.sendMimicrySyncTo(online, joining);
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event)
    {
        Entity target = event.getTarget();
        if (!(target instanceof ServerPlayer tracked))
        {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer tracker))
        {
            return;
        }
        ModNetwork.sendMimicrySyncTo(tracker, tracked);
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Client
    {
        private Client()
        {
        }

        @SubscribeEvent
        public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event)
        {
            MimicryClientState.remove(event.getEntity().getUUID());
        }
    }
}
