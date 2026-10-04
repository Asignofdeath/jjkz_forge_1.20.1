package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.quest.MainQuest11Intro;
import com.darkness.jujutsukaisenzone.quest.MainQuest11VillageClear;
import com.darkness.jujutsukaisenzone.quest.MainQuestLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestEvents
{
    private QuestEvents()
    {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        MainQuestLogic.bootstrapIfNeeded(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            MainQuest11Intro.abort(player);
        }
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
        MainQuest11Intro.tick(serverPlayer);
        if (serverPlayer.tickCount % 100 != 0)
        {
            return;
        }
        MainQuestLogic.ensureLocateForTracked(serverPlayer, false);
        MainQuest11VillageClear.tick(serverPlayer);
    }
}
