package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga.MahoragaAdaptation;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga.MahoragaCounterPlaceholder;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga.MahoragaReverseTechnique;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga.MahoragaSupport;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 魔虚罗：适应、反转术式、对策占位 tick。 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MahoragaEvents
{
    private MahoragaEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || !MahoragaSupport.isMahoraga(mob))
        {
            return;
        }
        if (MahoragaAdaptation.tryAdaptAndMaybeImmune(mob, event.getSource(), event.getAmount()))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide)
        {
            return;
        }
        if (!MahoragaSupport.isMahoraga(mob))
        {
            return;
        }
        MahoragaReverseTechnique.tick(mob);
        MahoragaCounterPlaceholder.tickPlaceholder(mob);
    }
}
