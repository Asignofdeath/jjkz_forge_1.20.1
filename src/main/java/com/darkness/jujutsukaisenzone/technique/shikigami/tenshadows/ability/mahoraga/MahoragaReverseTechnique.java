package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * 魔虚罗反转术式：高效率自愈（占位——固定间隔大量回血）。
 */
public final class MahoragaReverseTechnique
{
    private static final int INTERVAL = 20;
    private static final float HEAL = 10.0F;

    private MahoragaReverseTechnique()
    {
    }

    public static void tick(Mob mahoraga)
    {
        if (!MahoragaSupport.isMahoraga(mahoraga) || mahoraga.level().isClientSide)
        {
            return;
        }
        if (mahoraga.tickCount % INTERVAL != 0)
        {
            return;
        }
        if (mahoraga.getHealth() >= mahoraga.getMaxHealth())
        {
            return;
        }
        float before = mahoraga.getHealth();
        mahoraga.heal(HEAL);
        if (mahoraga.getHealth() > before && mahoraga.level() instanceof ServerLevel level)
        {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    mahoraga.getX(), mahoraga.getY() + 1.5D, mahoraga.getZ(),
                    4, 0.3D, 0.4D, 0.3D, 0.02D);
        }
    }
}
