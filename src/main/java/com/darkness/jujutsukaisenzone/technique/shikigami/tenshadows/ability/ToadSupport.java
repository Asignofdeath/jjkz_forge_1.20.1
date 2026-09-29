package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * 蝦蟇工具：无常规攻击判定等。
 */
public final class ToadSupport
{
    private ToadSupport()
    {
    }

    public static boolean isToad(Mob mob)
    {
        return mob != null
                && TenShadowsSummonLogic.isFriendlyOwned(mob)
                && mob.getPersistentData().getInt(TenShadowsSummonLogic.TAG_INDEX)
                == TenShadowsShikigami.TOAD.index();
    }

    public static Mob firstToad(ServerPlayer owner)
    {
        var list = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.TOAD);
        return list.isEmpty() ? null : list.get(0);
    }
}
