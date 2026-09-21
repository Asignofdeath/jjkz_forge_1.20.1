package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.domain.JudgmentDomainAppealLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JudgmentAppealEvents
{
    private JudgmentAppealEvents()
    {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event)
    {
        ServerPlayer player = event.getPlayer();
        JudgmentDomainAppealLogic.tryPlayerAppeal(player, event.getRawText());
    }
}
