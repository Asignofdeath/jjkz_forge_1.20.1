package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.faction.FactionStance;
import com.darkness.jujutsukaisenzone.faction.FactionTempCoop;
import com.darkness.jujutsukaisenzone.faction.JjkzFactions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FactionStanceEvents
{
    private FactionStanceEvents()
    {
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event)
    {
        LivingEntity attacker = event.getEntity();
        if (attacker.level().isClientSide || !JjkzFactions.isThree(JjkzFactions.assigned(attacker)))
        {
            return;
        }
        LivingEntity target = event.getNewTarget();
        if (target == null)
        {
            return;
        }
        if (!FactionStance.canAggress(attacker, target))
        {
            event.setCanceled(true);
            return;
        }
        FactionTempCoop.tryRoll(attacker, target);
    }
}
