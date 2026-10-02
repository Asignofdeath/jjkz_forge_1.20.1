package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.locate.PlayerLocateGuidance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LocateGuidanceEvents
{
    private LocateGuidanceEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        if (serverPlayer.tickCount % 10 != 0)
        {
            return;
        }
        PlayerLocateGuidance.tickPlayer(serverPlayer);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            PlayerLocateGuidance.clearSilent(serverPlayer);
        }
    }
}
