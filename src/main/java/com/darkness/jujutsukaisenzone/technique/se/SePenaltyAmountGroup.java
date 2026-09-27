package com.darkness.jujutsukaisenzone.technique.se;

import com.darkness.jujutsukaisenzone.item.CursedObjectGrade;

/**
 * SE 代价 {@code amount} 的数值分组：三档咒物等级 × 组内闭区间，产出时均匀随机。
 * <p>
 * 单位随落点：强度/资源/阈值是「点数」，抗性/穿透/效率/暴击是「百分比点数」。
 */
public enum SePenaltyAmountGroup
{
    /** 体术 / 术式强度。 */
    POWER(3, 6, 6, 10, 10, 16),
    /** 抗性 / 穿透 / 效率 / 暴击。 */
    PERCENT(2, 4, 4, 7, 7, 11),
    /** 咒力总量 / 最大生命 / 领域量表满条↑ / 脑损上限↓。 */
    RESOURCE(4, 8, 8, 14, 14, 22);

    private final int commonMin;
    private final int commonMax;
    private final int rareMin;
    private final int rareMax;
    private final int specialMin;
    private final int specialMax;

    SePenaltyAmountGroup(
            int commonMin, int commonMax,
            int rareMin, int rareMax,
            int specialMin, int specialMax)
    {
        this.commonMin = commonMin;
        this.commonMax = commonMax;
        this.rareMin = rareMin;
        this.rareMax = rareMax;
        this.specialMin = specialMin;
        this.specialMax = specialMax;
    }

    public int minInclusive(CursedObjectGrade grade)
    {
        return switch (grade == null ? CursedObjectGrade.COMMON : grade)
        {
            case COMMON -> commonMin;
            case RARE -> rareMin;
            case SPECIAL -> specialMin;
        };
    }

    public int maxInclusive(CursedObjectGrade grade)
    {
        return switch (grade == null ? CursedObjectGrade.COMMON : grade)
        {
            case COMMON -> commonMax;
            case RARE -> rareMax;
            case SPECIAL -> specialMax;
        };
    }
}
