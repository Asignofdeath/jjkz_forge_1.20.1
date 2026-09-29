package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * 满象 · 顶击（自动）：近距离顶飞敌人。
 */
public final class MaxElephantRamLogic
{
    private static final double RANGE = 3.5D;
    private static final float DAMAGE = 8.0F;

    private MaxElephantRamLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob elephant = MaxElephantSupport.first(owner);
        if (elephant == null)
        {
            return false;
        }
        LivingEntity target = elephant.getTarget();
        if (target == null || !target.isAlive() || TenShadowsSummonLogic.isOwnedBy(target, owner.getUUID()))
        {
            return false;
        }
        if (elephant.distanceToSqr(target) > RANGE * RANGE)
        {
            return false;
        }

        target.hurt(elephant.damageSources().mobAttack(elephant), DAMAGE);
        Vec3 away = target.position().subtract(elephant.position());
        if (away.lengthSqr() < 1.0E-4D)
        {
            away = elephant.getLookAngle();
        }
        Vec3 knock = away.normalize().scale(0.85D).add(0.0D, 0.75D, 0.0D);
        target.setDeltaMovement(target.getDeltaMovement().add(knock));
        target.hurtMarked = true;
        return true;
    }
}
