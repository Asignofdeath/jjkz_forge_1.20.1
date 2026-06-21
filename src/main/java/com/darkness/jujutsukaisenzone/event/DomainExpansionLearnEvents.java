package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.DomainExpansionLearnLogic;
import com.darkness.jujutsukaisenzone.technique.domain.InnateDomainExperienceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DomainExpansionLearnEvents
{
    private DomainExpansionLearnEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerSleepInBed(PlayerSleepInBedEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        DomainExpansionLearnLogic.tryEnterOnSleep(player);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer hurtPlayer
                && InnateDomainExperienceManager.isSessionActive(hurtPlayer.getUUID()))
        {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player)
        {
            DomainExpansionLearnLogic.tryEnterOnNearDeathDamage(player, event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }
        if (!(event.player instanceof ServerPlayer player))
        {
            return;
        }
        if (InnateDomainExperienceManager.isSessionActive(player.getUUID()))
        {
            InnateDomainExperienceManager.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            InnateDomainExperienceManager.abortIfActive(player);
        }
    }
}
