package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.domain.JudgmentDomainExpansion;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 诛伏赐死领域：审判冻结期间禁止暴力（含弓箭等投射物）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JudgmentDomainEvents
{
    private JudgmentDomainEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide())
        {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (JudgmentDomainExpansion.isEntityUnderTrialViolenceProtection(victim))
        {
            event.setCanceled(true);
        }
    }
}
