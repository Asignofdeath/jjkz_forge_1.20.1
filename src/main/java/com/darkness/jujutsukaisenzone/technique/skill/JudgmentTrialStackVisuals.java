package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.JudgmentTrialTargetData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.List;

/**
 * 「审判」叠层可视化：施法者可见的目标脚底黑金粒子，叠层越高粒子越多。
 */
public final class JudgmentTrialStackVisuals
{
    private static final int PARTICLE_INTERVAL_TICKS = 5;
    private static final double VISUAL_RANGE = 32.0D;
    private static final int PARTICLES_PER_STACK = 2;
    private static final int MAX_PARTICLES_PER_COLOR = 24;
    private static final DustParticleOptions BLACK_DUST =
            new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.0F);
    private static final DustParticleOptions GOLD_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.08F), 1.15F);

    private JudgmentTrialStackVisuals()
    {
    }

    public static void tickForCaster(ServerPlayer caster)
    {
        if (caster.level().isClientSide()
                || caster.tickCount % PARTICLE_INTERVAL_TICKS != 0
                || !PlayerTechniqueData.isJudgment(caster)
                || !EpicFightCompat.isBattleMode(caster))
        {
            return;
        }

        ServerLevel level = caster.serverLevel();
        AABB searchBox = caster.getBoundingBox().inflate(VISUAL_RANGE);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity.isAlive()
                        && !entity.isRemoved()
                        && !entity.getUUID().equals(caster.getUUID())
                        && entity.isAttackable()
                        && !entity.isSpectator());

        for (LivingEntity target : targets)
        {
            int trialCount = JudgmentTrialTargetData.getTrialCount(caster, target.getUUID());
            if (trialCount <= 0)
            {
                continue;
            }
            spawnFootParticles(caster, level, target, trialCount);
        }
    }

    private static void spawnFootParticles(
            ServerPlayer viewer,
            ServerLevel level,
            LivingEntity target,
            int trialCount)
    {
        int count = Math.min(MAX_PARTICLES_PER_COLOR, trialCount * PARTICLES_PER_STACK);
        double spread = 0.18D + trialCount * 0.025D;
        double x = target.getX();
        double y = target.getY() + 0.05D;
        double z = target.getZ();

        level.sendParticles(viewer, BLACK_DUST, false, x, y, z, count, spread, 0.04D, spread, 0.0D);
        level.sendParticles(viewer, GOLD_DUST, false, x, y, z, count, spread, 0.04D, spread, 0.0D);
    }
}
