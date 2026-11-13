package com.darkness.jujutsukaisenzone.heaven;

import com.darkness.jujutsukaisenzone.faction.JjkzFactions;
import com.darkness.jujutsukaisenzone.gameplay.CursedSpiritGrade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 天道咒灵判定：非玩家且阵营为咒灵。
 */
public final class CurseSpiritEntities
{
    private CurseSpiritEntities()
    {
    }

    public static boolean isCurseSpirit(LivingEntity entity)
    {
        if (entity == null || entity instanceof Player)
        {
            return false;
        }
        return JjkzFactions.CURSE_SPIRIT.equals(JjkzFactions.resolve(entity));
    }

    /**
     * 可读等级时返回；否则 empty。
     * 实现 {@link CurseSpiritGradeSource} 的实体优先。
     */
    public static CursedSpiritGrade gradeOrNull(LivingEntity entity)
    {
        if (entity instanceof CurseSpiritGradeSource source)
        {
            return source.cursedSpiritGrade();
        }
        return null;
    }
}
