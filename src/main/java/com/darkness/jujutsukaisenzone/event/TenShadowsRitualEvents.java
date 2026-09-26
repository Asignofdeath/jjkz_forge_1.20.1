package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsRitualLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TenShadowsRitualEvents
{
    private TenShadowsRitualEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget == null || !TenShadowsRitualLogic.isFalseDead(newTarget.getUUID()))
        {
            return;
        }
        event.setCanceled(true);
        /* 仅当当前目标已是假死者才清；勿清掉正转向假死者的其它仇恨 */
        if (event.getEntity() instanceof Mob mob)
        {
            LivingEntity current = mob.getTarget();
            if (current != null && TenShadowsRitualLogic.isFalseDead(current.getUUID()))
            {
                TenShadowsRitualLogic.clearMobTarget(mob);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        LivingEntity living = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity livingAttacker && TenShadowsRitualLogic.isRitualEntity(livingAttacker))
        {
            TenShadowsRitualLogic.onRitualEntityDealtDamage(livingAttacker);
        }
        if (TenShadowsRitualLogic.isRitualEntity(living))
        {
            if (attacker instanceof LivingEntity livingAttacker)
            {
                TenShadowsRitualLogic.onRitualEntityAttacked(living, livingAttacker);
            }
            return;
        }
        if (!TenShadowsRitualLogic.isInRitual(living.getUUID()))
        {
            return;
        }
        var session = TenShadowsRitualLogic.findForParticipant(living.getUUID());
        if (session == null || session.isClosed())
        {
            return;
        }
        if (session.isFalseDead(living.getUUID()))
        {
            event.setCanceled(true);
            return;
        }
        if (!session.isMulti())
        {
            return;
        }
        if (living.getHealth() - event.getAmount() > 0.0F)
        {
            return;
        }
        event.setCanceled(true);
        TenShadowsRitualLogic.onParticipantLethal(living);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (TenShadowsRitualLogic.isRitualEntity(entity))
        {
            TenShadowsRitualLogic.onRitualEntityDied(entity);
            return;
        }
        if (!TenShadowsRitualLogic.isInRitual(entity.getUUID()))
        {
            return;
        }
        var session = TenShadowsRitualLogic.findForParticipant(entity.getUUID());
        if (session != null && session.isMulti() && !session.isClosed())
        {
            event.setCanceled(true);
            TenShadowsRitualLogic.onParticipantLethal(entity);
            return;
        }
        TenShadowsRitualLogic.onParticipantTrueDeathOutsideFalseDeath(entity);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTravelToDimension(EntityTravelToDimensionEvent event)
    {
        if (TenShadowsRitualLogic.tryBlockDimensionTravel(event.getEntity(), event.getDimension()))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            TenShadowsRitualLogic.onLogout(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide || !TenShadowsRitualLogic.isRitualEntity(living))
        {
            return;
        }
        /* 会话已结束的残留式神：延后丢弃，避免在本 tick 中途移除当前实体 */
        if (living.tickCount % 20 != 0)
        {
            return;
        }
        var server = living.getServer();
        if (server == null)
        {
            return;
        }
        server.execute(() -> TenShadowsRitualLogic.discardOrphanRitualEntity(living));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }
        TenShadowsRitualLogic.tickFalseDeadParticipants();
        /* 每 5 tick：仇恨重选 / 传送 / chunk ticket / 卸载假死 */
        if (event.getServer().getTickCount() % 5 == 0)
        {
            TenShadowsRitualLogic.tickSessions();
        }
    }
}
