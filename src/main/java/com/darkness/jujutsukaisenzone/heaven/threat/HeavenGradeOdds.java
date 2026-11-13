package com.darkness.jujutsukaisenzone.heaven.threat;

import com.darkness.jujutsukaisenzone.gameplay.CursedSpiritGrade;

/**
 * 咒灵等级 → 天道对策：至少持有一条的概率、SE 槽位上限（队列长度）。
 */
public final class HeavenGradeOdds
{
    private HeavenGradeOdds()
    {
    }

    /** 0～1；未列入的等级视为 0。 */
    public static float chanceAtLeastOne(CursedSpiritGrade grade)
    {
        if (grade == null)
        {
            return 0.0F;
        }
        return switch (grade)
        {
            case GRADE_4 -> 0.08F;
            case GRADE_3 -> 0.20F;
            case GRADE_2 -> 0.40F;
            case GRADE_1 -> 0.65F;
            case SPECIAL -> 0.90F;
        };
    }

    /**
     * SE 槽位上限（FIFO 队列长度）。溢出时丢掉最早入队的一条。
     */
    public static int slotCap(CursedSpiritGrade grade)
    {
        if (grade == null)
        {
            return 0;
        }
        return switch (grade)
        {
            case GRADE_4, GRADE_3 -> 1;
            case GRADE_2, GRADE_1 -> 2;
            case SPECIAL -> 3;
        };
    }

    /** @deprecated 改用 {@link #slotCap} */
    @Deprecated
    public static int maxCount(CursedSpiritGrade grade)
    {
        return slotCap(grade);
    }
}
