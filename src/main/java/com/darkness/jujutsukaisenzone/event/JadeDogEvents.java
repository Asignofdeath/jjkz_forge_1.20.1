package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.JadeDogEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玉犬击败敌人且周围无威胁时，原地进食并回复生命。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JadeDogEvents
{
    private JadeDogEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide)
        {
            return;
        }
        JadeDogEntity dog = resolveKillerDog(event.getSource().getEntity(), victim);
        if (dog != null)
        {
            dog.tryStartEatingAfterKill(victim);
        }
    }

    private static JadeDogEntity resolveKillerDog(Entity source, LivingEntity victim)
    {
        if (source instanceof JadeDogEntity dog)
        {
            return dog;
        }
        LivingEntity last = victim.getLastHurtByMob();
        if (last instanceof JadeDogEntity dog)
        {
            return dog;
        }
        return null;
    }
}
