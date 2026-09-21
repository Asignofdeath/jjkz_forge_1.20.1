package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.ConfiscationEffect;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyTrialSpotlightFx;
import com.darkness.jujutsukaisenzone.registry.ModEffects;
import com.darkness.jujutsukaisenzone.technique.domain.JudgmentDomainExpansion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JudgmentEffectEvents
{
    private JudgmentEffectEvents()
    {
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        LivingEntity living = event.getEntity();
        MobEffect effect = event.getEffectInstance().getEffect();
        if (effect == ModEffects.DEATH_SENTENCE.get())
        {
            mountDeathSentenceSpotlight(living);
        }
    }

    private static void mountDeathSentenceSpotlight(LivingEntity living)
    {
        JudgmentDomainExpansion domain = JudgmentDomainExpansion.getInstance();
        ServerPlayer domainCaster = domain.findActiveDomainCasterForEntity(living);
        if (domainCaster != null)
        {
            domain.trackTrialSpotlightEntity(domainCaster.getUUID(), living.getUUID());
            OwnerOnlyTrialSpotlightFx.mount(living, domain.getPlayersInsideActiveDomain(domainCaster));
            return;
        }
        DeathSentenceEffect.mountSpotlight(living);
    }

    private static void removeDeathSentenceSpotlight(LivingEntity living)
    {
        JudgmentDomainExpansion domain = JudgmentDomainExpansion.getInstance();
        ServerPlayer domainCaster = domain.findActiveDomainCasterForEntity(living);
        if (domainCaster != null)
        {
            OwnerOnlyTrialSpotlightFx.remove(living, domain.getPlayersInsideActiveDomain(domainCaster));
            return;
        }
        DeathSentenceEffect.removeSpotlight(living);
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        LivingEntity living = event.getEntity();
        if (event.getEffect() == ModEffects.DEATH_SENTENCE.get())
        {
            removeDeathSentenceSpotlight(living);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        LivingEntity entity = event.getEntity();
        MobEffectInstance confiscation = entity.getEffect(ModEffects.CONFISCATION.get());
        if (confiscation != null)
        {
            ConfiscationEffect.tickParticles(entity, confiscation);
        }
    }
}
