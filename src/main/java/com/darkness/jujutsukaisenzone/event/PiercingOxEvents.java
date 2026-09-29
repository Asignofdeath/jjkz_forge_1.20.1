package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.PiercingOxChargeLogic;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 贯牛冲锋位移与路径打击。 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PiercingOxEvents
{
    private PiercingOxEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide)
        {
            return;
        }
        PiercingOxChargeLogic.tickOx(mob);
    }
}
