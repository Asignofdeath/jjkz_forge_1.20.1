package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.entity.JadeDogEntity;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiSkillRatios;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 玉犬 · 扑击（自动）：中距冲向仇恨目标并造成冲击伤。
 */
public final class DivineDogsPounceLogic
{
    private static final double MIN_RANGE = 3.0D;
    private static final double MAX_RANGE = 8.0D;

    private DivineDogsPounceLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        List<Mob> dogs = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.DIVINE_DOGS);
        boolean any = false;
        for (Mob dog : dogs)
        {
            LivingEntity target = dog.getTarget();
            if (target == null || !target.isAlive())
            {
                continue;
            }
            if (dog instanceof JadeDogEntity eating && eating.isEating())
            {
                continue;
            }
            double distSq = dog.distanceToSqr(target);
            if (distSq < MIN_RANGE * MIN_RANGE || distSq > MAX_RANGE * MAX_RANGE)
            {
                continue;
            }
            Vec3 delta = target.position().subtract(dog.position()).normalize().scale(1.15D);
            dog.setDeltaMovement(dog.getDeltaMovement().add(delta.x, 0.35D, delta.z));
            dog.hurtMarked = true;
            if (dog instanceof JadeDogEntity jadeDog)
            {
                jadeDog.playPounceAnimation();
            }
            if (dog.distanceToSqr(target) <= (MIN_RANGE + 1.5D) * (MIN_RANGE + 1.5D)
                    || distSq <= 16.0D)
            {
                TechniqueDamageSources.deal(target, dog, TenShadowsShikigamiSkillRatios.DIVINE_DOGS_POUNCE,
                        SkillDamageType.MARTIAL);
            }
            any = true;
        }
        return any;
    }
}
