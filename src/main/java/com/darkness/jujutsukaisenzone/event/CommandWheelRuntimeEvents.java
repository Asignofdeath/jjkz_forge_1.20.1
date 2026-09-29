package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsStanceData;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilityRuntime;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigamiAiLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 十影式神姿态会话与跟随 / 协同 / 护卫 AI，以及式神自动招 tick。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommandWheelRuntimeEvents
{
    private CommandWheelRuntimeEvents()
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
        TenShadowsShikigamiAiLogic.tickOwner(player);
        ShikigamiAbilityRuntime.tickAuto(player);
    }

    @SubscribeEvent
    public static void onOwnerHurt(LivingHurtEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer owner) || owner.level().isClientSide)
        {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (source instanceof LivingEntity attacker)
        {
            TenShadowsShikigamiAiLogic.onOwnerHurt(owner, attacker);
        }
    }

    @SubscribeEvent
    public static void onShikigamiHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob))
        {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (source instanceof LivingEntity attacker)
        {
            TenShadowsShikigamiAiLogic.onShikigamiHurt(mob, attacker);
        }
    }

    @SubscribeEvent
    public static void onOwnerAttack(AttackEntityEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer owner) || owner.level().isClientSide)
        {
            return;
        }
        if (event.getTarget() instanceof LivingEntity victim)
        {
            TenShadowsShikigamiAiLogic.onOwnerAttack(owner, victim);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            PlayerTenShadowsStanceData.onLogout(player);
        }
    }
}
