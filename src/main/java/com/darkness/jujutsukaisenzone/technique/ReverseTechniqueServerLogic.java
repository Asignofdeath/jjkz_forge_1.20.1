package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.joml.Vector3f;

/**
 * 反转术式：战斗模式 + 按住 G 时，每刻消耗咒力、回复生命，并产生白色粒子。
 */
public final class ReverseTechniqueServerLogic
{
    /** 每刻消耗的咒力 */
    public static final int COST_PER_TICK = 1;
    /** 每刻回复的生命值（2.0 = 1 颗心） */
    public static final float HEAL_PER_TICK = 0.12f;
    /** 周身粒子数量（每刻） */
    public static final int PARTICLE_COUNT = 10;
    /** 白色尘屑缩放 */
    private static final float DUST_SCALE = 1.2f;

    private ReverseTechniqueServerLogic()
    {
    }

    public static void applyTick(ServerPlayer player)
    {
        if (!EpicFightCompat.isBattleMode(player))
        {
            return;
        }
        if (!PlayerReverseTechniqueData.hasReverseTechnique(player))
        {
            return;
        }
        if (!PlayerCursedEnergyData.tryConsume(player, COST_PER_TICK))
        {
            return;
        }

        float maxHealth = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
        if (player.getHealth() < maxHealth)
        {
            float heal = Math.min(HEAL_PER_TICK, maxHealth - player.getHealth());
            if (heal > 0)
            {
                player.heal(heal);
            }
        }

        if (player.level() instanceof ServerLevel serverLevel)
        {
            spawnWhiteParticles(serverLevel, player);
        }
    }

    private static void spawnWhiteParticles(ServerLevel level, ServerPlayer player)
    {
        Vector3f color = new Vector3f(1f, 1f, 1f);
        DustParticleOptions dust = new DustParticleOptions(color, DUST_SCALE);
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.55;
        double z = player.getZ();
        level.sendParticles(
                dust,
                x,
                y,
                z,
                PARTICLE_COUNT,
                0.42,
                0.55,
                0.42,
                0.02
        );
    }
}
