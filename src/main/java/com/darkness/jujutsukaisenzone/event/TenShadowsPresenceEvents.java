package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsPresenceBonuses;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 十影在场增益：周期性刷新属性；脱兔概率分担仇恨。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TenShadowsPresenceEvents
{
    private TenShadowsPresenceEvents()
    {
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
        if (player.tickCount % 20 != 0)
        {
            return;
        }
        TenShadowsPresenceBonuses.refresh(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            TenShadowsPresenceBonuses.clear(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        LivingEntity newTarget = event.getNewTarget();
        if (!(newTarget instanceof ServerPlayer owner))
        {
            return;
        }
        if (!TenShadowsPresenceBonuses.hasRabbits(owner))
        {
            return;
        }
        if (!(event.getEntity() instanceof Mob))
        {
            return;
        }
        if (owner.getRandom().nextDouble() >= TenShadowsPresenceBonuses.RABBIT_AGGRO_CHANCE)
        {
            return;
        }
        LivingEntity rabbit = TenShadowsPresenceBonuses.pickRandomRabbit(owner);
        if (rabbit != null && rabbit != event.getEntity())
        {
            event.setNewTarget(rabbit);
        }
    }
}
