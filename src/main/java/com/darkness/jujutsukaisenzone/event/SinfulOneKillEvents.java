package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.effect.SinfulOneKillRules;
import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 击杀中立 / 友好生物时，令击杀者获得【罪恶者】。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SinfulOneKillEvents
{
    private SinfulOneKillEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (event.getEntity().level().isClientSide())
        {
            return;
        }

        LivingEntity victim = event.getEntity();
        LivingEntity killer = resolveKiller(event);
        if (!isEligibleKiller(killer))
        {
            return;
        }
        if (!SinfulOneKillRules.isNeutralOrFriendlyVictim(victim, killer))
        {
            return;
        }

        SinfulOneEffect.apply(killer, PlayerCrimeRecordData.Category.NEUTRAL_KILL, "击杀中立及好生物");
    }

    private static LivingEntity resolveKiller(LivingDeathEvent event)
    {
        if (event.getSource().getEntity() instanceof LivingEntity directKiller)
        {
            return directKiller;
        }
        return event.getEntity().getLastHurtByMob();
    }

    private static boolean isEligibleKiller(LivingEntity killer)
    {
        return killer instanceof Player || killer instanceof Mob;
    }
}
