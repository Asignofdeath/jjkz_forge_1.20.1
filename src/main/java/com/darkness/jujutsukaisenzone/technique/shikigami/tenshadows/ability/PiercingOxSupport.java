package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** 贯牛查询。 */
public final class PiercingOxSupport
{
    private PiercingOxSupport()
    {
    }

    public static boolean isPiercingOx(Mob mob)
    {
        return mob != null
                && TenShadowsSummonLogic.isFriendlyOwned(mob)
                && mob.getPersistentData().getInt(TenShadowsSummonLogic.TAG_INDEX)
                == TenShadowsShikigami.PIERCING_OX.index();
    }

    public static Mob first(ServerPlayer owner)
    {
        var list = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.PIERCING_OX);
        return list.isEmpty() ? null : list.get(0);
    }
}
