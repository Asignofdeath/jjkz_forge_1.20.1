package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows;

import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsRosterData;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 十种影能力共用的场上式神查询。 */
public final class TenShadowsFieldQuery
{
    private TenShadowsFieldQuery()
    {
    }

    public static List<Mob> aliveMobs(ServerPlayer owner, TenShadowsShikigami shikigami)
    {
        List<Mob> result = new ArrayList<>();
        if (owner == null || shikigami == null)
        {
            return result;
        }
        ServerLevel level = owner.serverLevel();
        for (UUID id : PlayerTenShadowsRosterData.get(owner, shikigami.index()))
        {
            Entity entity = level.getEntity(id);
            if (entity instanceof Mob mob
                    && mob.isAlive()
                    && TenShadowsSummonLogic.isOwnedBy(mob, owner.getUUID()))
            {
                result.add(mob);
            }
        }
        return result;
    }

    /** 主人场上全部友方式神。 */
    public static List<Mob> allOwnedMobs(ServerPlayer owner)
    {
        List<Mob> result = new ArrayList<>();
        if (owner == null)
        {
            return result;
        }
        for (TenShadowsShikigami shikigami : TenShadowsShikigami.values())
        {
            result.addAll(aliveMobs(owner, shikigami));
        }
        return result;
    }
}
