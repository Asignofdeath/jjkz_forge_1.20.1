package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerSpData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 击杀获取 SP：BOSS 稳定发放，普通怪物小概率。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpGainEvents
{
    private static final int WITHER_SP = 5;
    private static final int ENDER_DRAGON_SP = 10;
    private static final float MOB_CHANCE = 0.02F;
    private static final int MOB_SP = 1;

    private SpGainEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }

        ServerPlayer killer = resolveKiller(event);
        if (killer == null)
        {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (victim instanceof Player)
        {
            return;
        }

        int award = resolveAward(victim, killer);
        if (award <= 0)
        {
            return;
        }

        PlayerSpData.add(killer, award);
        killer.sendSystemMessage(Component.translatable(
                "message.jujutsukaisenzone.sp.gained", award, PlayerSpData.getAvailable(killer)));
    }

    private static int resolveAward(LivingEntity victim, ServerPlayer killer)
    {
        if (victim instanceof WitherBoss)
        {
            return WITHER_SP;
        }
        if (victim instanceof EnderDragon)
        {
            return ENDER_DRAGON_SP;
        }
        if (!(victim instanceof Mob))
        {
            return 0;
        }
        return killer.getRandom().nextFloat() < MOB_CHANCE ? MOB_SP : 0;
    }

    private static ServerPlayer resolveKiller(LivingDeathEvent event)
    {
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer player)
        {
            return player;
        }
        if (source.getDirectEntity() instanceof ServerPlayer player)
        {
            return player;
        }
        if (event.getEntity().getLastHurtByMob() instanceof ServerPlayer player)
        {
            return player;
        }
        return null;
    }
}
