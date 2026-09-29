package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.GreatSerpentAmbushLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.GreatSerpentAmbushTicker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 大蛇：无常规攻击（封锁普攻伤害）；突袭钻地 tick。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GreatSerpentEvents
{
    private GreatSerpentEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Mob mob) || !GreatSerpentAmbushLogic.isGreatSerpent(mob))
        {
            return;
        }
        if (!GreatSerpentAmbushLogic.isStrikeAllowed(mob))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        if (event.getEntity() instanceof Mob mob)
        {
            GreatSerpentAmbushTicker.tickMob(mob);
        }
    }
}
