package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiSkillRatios;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 大蛇突袭：钻地计时结束后在目标脚下破土。 */
public final class GreatSerpentAmbushTicker
{
    private static final int CONTROL_TICKS = 20 * 3;

    private GreatSerpentAmbushTicker()
    {
    }

    public static void tickMob(Mob mob)
    {
        if (mob.level().isClientSide || !(mob.level() instanceof ServerLevel level))
        {
            return;
        }
        if (!GreatSerpentAmbushLogic.isGreatSerpent(mob))
        {
            return;
        }
        int remain = mob.getPersistentData().getInt(GreatSerpentAmbushLogic.TAG_AMBUSH_TICKS);
        if (remain <= 0)
        {
            return;
        }
        remain--;
        mob.getPersistentData().putInt(GreatSerpentAmbushLogic.TAG_AMBUSH_TICKS, remain);
        mob.setInvisible(true);
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);

        if (remain > 0)
        {
            return;
        }

        if (!mob.getPersistentData().hasUUID(GreatSerpentAmbushLogic.TAG_AMBUSH_TARGET))
        {
            finishBurrowState(mob);
            return;
        }
        UUID targetId = mob.getPersistentData().getUUID(GreatSerpentAmbushLogic.TAG_AMBUSH_TARGET);
        mob.getPersistentData().remove(GreatSerpentAmbushLogic.TAG_AMBUSH_TARGET);
        Entity entity = level.getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive())
        {
            finishBurrowState(mob);
            return;
        }

        Vec3 emerge = findEmergePos(level, target);
        emergeFx(level, emerge);
        mob.teleportTo(emerge.x, emerge.y, emerge.z);
        finishBurrowState(mob);

        mob.getPersistentData().putBoolean(GreatSerpentAmbushLogic.TAG_ALLOW_STRIKE, true);
        try
        {
            TechniqueDamageSources.deal(target, mob, TenShadowsShikigamiSkillRatios.GREAT_SERPENT_AMBUSH,
                    SkillDamageType.MARTIAL);
            /* 控制占位：强减速 + 缓慢坠落感（挖掘疲劳作「缚住」观感） */
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CONTROL_TICKS, 4, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, CONTROL_TICKS, 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CONTROL_TICKS, 0, false, true, true));
        }
        finally
        {
            mob.getPersistentData().remove(GreatSerpentAmbushLogic.TAG_ALLOW_STRIKE);
        }
        mob.setTarget(target);
    }

    private static Vec3 findEmergePos(ServerLevel level, LivingEntity target)
    {
        BlockPos feet = target.blockPosition();
        double x = target.getX();
        double z = target.getZ();
        double y = feet.getY();
        BlockPos ground = feet.below();
        if (!level.getBlockState(ground).isAir())
        {
            y = ground.getY() + 1.0D;
        }
        return new Vec3(x, y, z);
    }

    private static void emergeFx(ServerLevel level, Vec3 pos)
    {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                pos.x, pos.y + 0.2D, pos.z,
                40, 0.55D, 0.35D, 0.55D, 0.12D);
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.4D, pos.z, 12, 0.4D, 0.2D, 0.4D, 0.03D);
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 0.6D, pos.z, 16, 0.35D, 0.4D, 0.35D, 0.15D);
    }

    private static void finishBurrowState(Mob mob)
    {
        mob.getPersistentData().remove(GreatSerpentAmbushLogic.TAG_AMBUSH_TICKS);
        mob.setInvisible(false);
        mob.setNoGravity(false);
        mob.noPhysics = false;
        /* 召出体若仍挂主人标记，保持可见名 */
        if (TenShadowsSummonLogic.isFriendlyOwned(mob))
        {
            mob.setCustomNameVisible(true);
        }
    }
}
