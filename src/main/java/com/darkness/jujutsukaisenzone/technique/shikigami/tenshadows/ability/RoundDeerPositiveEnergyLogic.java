package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

/**
 * 圆鹿 · 正向能量（自动）：治疗自己、主人与其他友方式神。
 */
public final class RoundDeerPositiveEnergyLogic
{
    private static final float HEAL_AMOUNT = 6.0F;
    private static final float MISSING_HP_THRESHOLD = 0.5F;

    private RoundDeerPositiveEnergyLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob deer = firstDeer(owner);
        if (deer == null)
        {
            return false;
        }
        List<LivingEntity> allies = collectAllies(owner, deer);
        boolean anyHurt = false;
        for (LivingEntity ally : allies)
        {
            if (ally.getHealth() + MISSING_HP_THRESHOLD < ally.getMaxHealth())
            {
                anyHurt = true;
                break;
            }
        }
        if (!anyHurt)
        {
            return false;
        }

        ServerLevel level = owner.serverLevel();
        for (LivingEntity ally : allies)
        {
            float before = ally.getHealth();
            ally.heal(HEAL_AMOUNT);
            if (ally.getHealth() > before)
            {
                level.sendParticles(
                        ParticleTypes.HEART,
                        ally.getX(), ally.getY() + ally.getBbHeight() * 0.6D, ally.getZ(),
                        3, 0.25D, 0.2D, 0.25D, 0.0D);
            }
        }
        return true;
    }

    static Mob firstDeer(ServerPlayer owner)
    {
        List<Mob> list = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.ROUND_DEER);
        return list.isEmpty() ? null : list.get(0);
    }

    static List<LivingEntity> collectAllies(ServerPlayer owner, Mob deer)
    {
        List<LivingEntity> allies = new ArrayList<>();
        allies.add(deer);
        if (owner.isAlive())
        {
            allies.add(owner);
        }
        for (Mob mob : TenShadowsFieldQuery.allOwnedMobs(owner))
        {
            if (mob != deer && mob.isAlive())
            {
                allies.add(mob);
            }
        }
        return allies;
    }
}
