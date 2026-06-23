package com.darkness.jujutsukaisenzone.technique.scaling;

import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 咒术等级成长共用：以四级为基准，计算高于基准的档位数。
 */
public final class SkillGrowthSteps
{
    private SkillGrowthSteps()
    {
    }

    /** 高于指定基准咒术等级的档位数（基准档 = 0 档加成）。 */
    public static int stepsAboveBaseline(@Nullable LivingEntity owner, int baselineGrade)
    {
        if (!(owner instanceof Player player))
        {
            return 0;
        }
        return Math.max(0, PlayerCursedGradeData.getRank(player) - baselineGrade);
    }

    /** 高于四级基准的咒术等级档位数（四级 = 0 档加成）。 */
    public static int stepsAboveBaseline(@Nullable LivingEntity owner)
    {
        return stepsAboveBaseline(owner, PlayerCursedGradeData.RANK_GRADE_4);
    }
}
