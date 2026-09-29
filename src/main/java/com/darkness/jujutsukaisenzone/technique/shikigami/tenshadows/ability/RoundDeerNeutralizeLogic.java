package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

/**
 * 圆鹿 · 中和（自动）：随机清除自己 / 主人 / 其他式神身上的一个有害效果。
 */
public final class RoundDeerNeutralizeLogic
{
    private RoundDeerNeutralizeLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob deer = RoundDeerPositiveEnergyLogic.firstDeer(owner);
        if (deer == null)
        {
            return false;
        }
        List<LivingEntity> allies = RoundDeerPositiveEnergyLogic.collectAllies(owner, deer);
        List<DebuffEntry> debuffs = new ArrayList<>();
        for (LivingEntity ally : allies)
        {
            for (MobEffectInstance instance : ally.getActiveEffects())
            {
                MobEffect effect = instance.getEffect();
                if (effect.getCategory() == MobEffectCategory.HARMFUL)
                {
                    debuffs.add(new DebuffEntry(ally, effect));
                }
            }
        }
        if (debuffs.isEmpty())
        {
            return false;
        }

        DebuffEntry pick = debuffs.get(owner.getRandom().nextInt(debuffs.size()));
        pick.entity().removeEffect(pick.effect());
        if (owner.level() instanceof ServerLevel level)
        {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    pick.entity().getX(),
                    pick.entity().getY() + pick.entity().getBbHeight() * 0.7D,
                    pick.entity().getZ(),
                    8, 0.3D, 0.35D, 0.3D, 0.02D);
        }
        return true;
    }

    private record DebuffEntry(LivingEntity entity, MobEffect effect)
    {
    }
}
