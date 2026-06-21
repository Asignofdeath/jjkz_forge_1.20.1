package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerHoldReleaseChargeData;
import com.darkness.jujutsukaisenzone.player.PlayerMaxOutputChargeData;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 术式蓄力：受击打断、登录时清理残留状态。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TechniqueMaxOutputServerEvents
{
    private TechniqueMaxOutputServerEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getAmount() <= 0.0F || !(event.getEntity() instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        if (PlayerHoldReleaseChargeData.isCharging(serverPlayer))
        {
            TechniqueSkillSetRegistry.interruptHoldReleaseFromDamage(serverPlayer);
            return;
        }
        if (!PlayerMaxOutputChargeData.isCharging(serverPlayer))
        {
            return;
        }
        TechniqueSkillSetRegistry.interruptMaxOutputFromDamage(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            if (PlayerHoldReleaseChargeData.isCharging(serverPlayer))
            {
                TechniqueSkillSetRegistry.cancelHoldReleaseCharge(
                        serverPlayer,
                        PlayerHoldReleaseChargeData.getChargingSlot(serverPlayer));
            }
            PlayerMaxOutputChargeData.clear(serverPlayer);
            PlayerHoldReleaseChargeData.clear(serverPlayer);
        }
    }
}
