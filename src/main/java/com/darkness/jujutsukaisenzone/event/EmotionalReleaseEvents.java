package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.EmotionalReleaseClientState;
import com.darkness.jujutsukaisenzone.client.EmotionalReleaseChargeHud;
import com.darkness.jujutsukaisenzone.player.PlayerEmotionalReleaseData;
import com.darkness.jujutsukaisenzone.technique.EmotionalReleaseServerLogic;
import com.darkness.jujutsukaisenzone.technique.commonskill.EmotionalReleaseCommonKeySkill;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EmotionalReleaseEvents
{
    private EmotionalReleaseEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }
        if (event.player instanceof ServerPlayer serverPlayer)
        {
            EmotionalReleaseServerLogic.tickMaintain(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getAmount() <= 0.0F || !(event.getEntity() instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        EmotionalReleaseServerLogic.interruptFromDamage(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            PlayerEmotionalReleaseData.clearAll(serverPlayer);
        }
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Client
    {
        private Client()
        {
        }

        @SubscribeEvent
        public static void onClientLivingHurt(LivingHurtEvent event)
        {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || event.getEntity() != minecraft.player || event.getAmount() <= 0.0F)
            {
                return;
            }
            if (EmotionalReleaseChargeHud.isVisible() || EmotionalReleaseClientState.isCharging())
            {
                EmotionalReleaseClientState.forceInterruptLocal();
                EmotionalReleaseCommonKeySkill.INSTANCE.resetBeginSent();
            }
        }
    }
}
