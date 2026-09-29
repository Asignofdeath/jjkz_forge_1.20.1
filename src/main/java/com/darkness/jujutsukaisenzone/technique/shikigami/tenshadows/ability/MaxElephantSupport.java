package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** 满象共用查询。 */
public final class MaxElephantSupport
{
    private MaxElephantSupport()
    {
    }

    public static boolean isMaxElephant(Mob mob)
    {
        return mob != null
                && TenShadowsSummonLogic.isFriendlyOwned(mob)
                && mob.getPersistentData().getInt(TenShadowsSummonLogic.TAG_INDEX)
                == TenShadowsShikigami.MAX_ELEPHANT.index();
    }

    public static Mob first(ServerPlayer owner)
    {
        var list = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.MAX_ELEPHANT);
        return list.isEmpty() ? null : list.get(0);
    }
}
