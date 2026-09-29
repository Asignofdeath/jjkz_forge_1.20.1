package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.MaxElephantMeteorLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.MaxElephantWaterFloodLogic;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 满象：临时水回收、陨石落地结算。 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MaxElephantEvents
{
    private MaxElephantEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide)
        {
            return;
        }
        MaxElephantWaterFloodLogic.tickElephant(mob);
        MaxElephantMeteorLogic.tickElephant(mob);
    }
}
