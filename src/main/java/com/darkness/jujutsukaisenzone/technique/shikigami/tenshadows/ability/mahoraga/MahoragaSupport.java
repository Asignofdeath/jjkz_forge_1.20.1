package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga;

import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/** 魔虚罗共用查询。 */
public final class MahoragaSupport
{
    private MahoragaSupport()
    {
    }

    public static boolean isMahoraga(Mob mob)
    {
        return mob != null
                && TenShadowsSummonLogic.isFriendlyOwned(mob)
                && mob.getPersistentData().getInt(TenShadowsSummonLogic.TAG_INDEX)
                == TenShadowsShikigami.MAHORAGA.index();
    }

    public static Mob first(ServerPlayer owner)
    {
        List<Mob> list = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.MAHORAGA);
        return list.isEmpty() ? null : list.get(0);
    }

    public static LivingEntity resolveCombatTarget(ServerPlayer owner, Mob mahoraga)
    {
        LivingEntity target = mahoraga.getTarget();
        if (target != null && target.isAlive() && !TenShadowsSummonLogic.isOwnedBy(target, owner.getUUID()))
        {
            return target;
        }
        return null;
    }
}
