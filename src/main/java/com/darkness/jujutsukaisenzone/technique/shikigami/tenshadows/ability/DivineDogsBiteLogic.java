package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * 玉犬 · 撕咬（自动）：占位——近战距离内对当前仇恨目标造成一次额外轻伤。
 */
public final class DivineDogsBiteLogic
{
    private static final double RANGE = 2.75D;
    private static final float DAMAGE = 3.0F;

    private DivineDogsBiteLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        List<Mob> dogs = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.DIVINE_DOGS);
        boolean any = false;
        for (Mob dog : dogs)
        {
            LivingEntity target = dog.getTarget();
            if (target == null || !target.isAlive() || dog.distanceToSqr(target) > RANGE * RANGE)
            {
                continue;
            }
            target.hurt(dog.damageSources().mobAttack(dog), DAMAGE);
            any = true;
        }
        return any;
    }
}
