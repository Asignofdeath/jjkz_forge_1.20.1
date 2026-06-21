package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.ReverseTechniqueLearnLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ReverseTechniqueLearnEvents
{
    private ReverseTechniqueLearnEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        ReverseTechniqueLearnLogic.tryLearnFromDamage(player, event.getAmount());
    }
}
