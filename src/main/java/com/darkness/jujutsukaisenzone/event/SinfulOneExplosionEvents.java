package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.PublicSafetyExplosionRules;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import com.darkness.jujutsukaisenzone.player.PlayerEndCrystalAttackerData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家引爆 TNT 或末地水晶时，记录「危害公共安全」罪行。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SinfulOneExplosionEvents
{
    private SinfulOneExplosionEvents()
    {
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event)
    {
        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }

        ServerPlayer player = PublicSafetyExplosionRules.resolvePlayerCauser(event.getExplosion(), level);
        if (player == null || !isEligiblePlayer(player))
        {
            return;
        }

        BlockPos pos = BlockPos.containing(event.getExplosion().getPosition());
        SinfulOneEffect.apply(
                player,
                PlayerCrimeRecordData.Category.PUBLIC_SAFETY,
                PublicSafetyExplosionRules.CRIME_DESCRIPTION,
                pos);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event)
    {
        if (event.getEntity().level().isClientSide())
        {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!isEligiblePlayer(player))
        {
            return;
        }
        if (event.getTarget() instanceof EndCrystal crystal)
        {
            PlayerEndCrystalAttackerData.markAttacker(crystal, player);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event)
    {
        if (event.getProjectile().level().isClientSide())
        {
            return;
        }
        if (!(event.getProjectile().getOwner() instanceof ServerPlayer player))
        {
            return;
        }
        if (!isEligiblePlayer(player))
        {
            return;
        }
        if (event.getRayTraceResult() instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof EndCrystal crystal)
        {
            PlayerEndCrystalAttackerData.markAttacker(crystal, player);
        }
    }

    private static boolean isEligiblePlayer(ServerPlayer player)
    {
        return !player.isCreative() && !player.isSpectator();
    }
}
