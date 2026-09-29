package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 虎葬 · 咆哮（BOTH，Shift+9）：秒杀低级怪，概率吓退其它实体。
 */
public final class TigerFuneralRoarLogic
{
    private static final double RADIUS = 10.0D;
    private static final float LOW_TIER_MAX_HEALTH = 20.0F;
    private static final float FEAR_CHANCE = 0.55F;
    private static final int FEAR_TICKS = 20 * 3;

    private TigerFuneralRoarLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        List<Mob> tigers = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.TIGER_FUNERAL);
        if (tigers.isEmpty())
        {
            return false;
        }
        Mob tiger = tigers.get(0);
        ServerLevel level = owner.serverLevel();
        AABB box = tiger.getBoundingBox().inflate(RADIUS);

        List<LivingEntity> victims = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive()
                        && e != owner
                        && e != tiger
                        && !TenShadowsSummonLogic.isOwnedBy(e, owner.getUUID()));

        if (source == ShikigamiAbilitySource.AUTO && victims.isEmpty())
        {
            return false;
        }
        /* 自动：附近至少有一个敌对或低级怪才吼 */
        if (source == ShikigamiAbilitySource.AUTO)
        {
            boolean hasThreat = false;
            for (LivingEntity v : victims)
            {
                if (v instanceof Enemy || v instanceof Monster || isLowTier(v))
                {
                    hasThreat = true;
                    break;
                }
            }
            if (!hasThreat)
            {
                return false;
            }
        }

        level.playSound(null, tiger.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL, 1.2F, 0.85F);
        level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                tiger.getX(), tiger.getY() + 1.0D, tiger.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(
                ParticleTypes.CLOUD,
                tiger.getX(), tiger.getY() + 0.8D, tiger.getZ(),
                28, 2.5D, 0.6D, 2.5D, 0.05D);

        int killed = 0;
        int feared = 0;
        for (LivingEntity victim : victims)
        {
            if (isLowTier(victim))
            {
                victim.kill();
                killed++;
                continue;
            }
            if (owner.getRandom().nextFloat() < FEAR_CHANCE)
            {
                applyFear(tiger, victim);
                feared++;
            }
        }

        if (source == ShikigamiAbilitySource.PLAYER)
        {
            owner.displayClientMessage(
                    Component.literal("虎葬 · 咆哮（歼 " + killed + " / 惧 " + feared + "）")
                            .withStyle(ChatFormatting.GOLD),
                    true);
        }
        return killed > 0 || feared > 0 || source == ShikigamiAbilitySource.PLAYER;
    }

    /** 低级：敌对且最大生命 ≤ 阈值（僵尸级）。 */
    private static boolean isLowTier(LivingEntity entity)
    {
        if (!(entity instanceof Enemy) && !(entity instanceof Monster))
        {
            return false;
        }
        return entity.getMaxHealth() <= LOW_TIER_MAX_HEALTH + 0.01F;
    }

    private static void applyFear(Mob tiger, LivingEntity victim)
    {
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FEAR_TICKS, 1, false, true, true));
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FEAR_TICKS, 0, false, true, true));
        if (victim instanceof Mob mob)
        {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
        }
        Vec3 away = victim.position().subtract(tiger.position());
        if (away.lengthSqr() < 1.0E-4D)
        {
            away = tiger.getLookAngle();
        }
        victim.setDeltaMovement(away.normalize().scale(1.1D).add(0.0D, 0.25D, 0.0D));
        victim.hurtMarked = true;
    }
}
