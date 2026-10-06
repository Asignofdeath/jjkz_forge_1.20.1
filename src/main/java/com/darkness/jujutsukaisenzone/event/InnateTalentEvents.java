package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.talent.InnateTalentData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InnateTalentEvents
{
    private InnateTalentEvents()
    {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity living))
        {
            return;
        }
        InnateTalentData.refreshModifiers(living);
    }
}
