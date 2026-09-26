package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skill.RatioSpotLogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 十划光点：存活粒子。弱点击破与强制暴击已并入 {@code TechniqueDamageZoneEvents} 暴击乘区。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RatioSpotEvents
{
    private RatioSpotEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide)
        {
            return;
        }
        CompoundTag data = living.getPersistentData();
        if (!data.contains("jujutsukaisenzone_ratio_spot"))
        {
            return;
        }
        RatioSpotLogic.tickSpotParticles(living);
    }
}
