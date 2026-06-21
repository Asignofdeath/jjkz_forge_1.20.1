package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueLearnLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BarrierTechniqueLearnEvents
{
    private BarrierTechniqueLearnEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        BarrierTechniqueLearnLogic.tryUnlockOnWakeUp(player);
    }
}
