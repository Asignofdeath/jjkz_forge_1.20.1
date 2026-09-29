package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.NueDiveImpactTicker;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.NueRideLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 鵺骑乘占位操控：乘客看向哪，鵺飞向哪。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NueRideEvents
{
    private static final double FLY_SPEED = 0.95D;

    private NueRideEvents()
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
        if (!(player.getVehicle() instanceof Mob mob) || !NueRideLogic.isNueRideVehicle(player, mob))
        {
            return;
        }
        Vec3 look = player.getLookAngle();
        mob.setDeltaMovement(look.scale(FLY_SPEED));
        mob.setYRot(player.getYRot());
        mob.setXRot(player.getXRot() * 0.5F);
        mob.hurtMarked = true;
        /* 抬升一点，减少贴地卡死 */
        if (mob.onGround())
        {
            mob.setDeltaMovement(mob.getDeltaMovement().add(0.0D, 0.35D, 0.0D));
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        if (event.getEntity() instanceof Mob mob)
        {
            NueDiveImpactTicker.tickMob(mob);
        }
    }
}
