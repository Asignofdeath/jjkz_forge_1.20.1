package com.darkness.jujutsukaisenzone.player;

/**
 * 结界术等级枚举。
 * <p>
 * - UNLEARNED: 未习得，无法使用结界术
 * - BEGINNER: 初习，维持结界消耗咒力减免 0%
 * - MASTERED: 掌握，维持结界消耗咒力减免 60%
 * - COMPLETE: 完全掌握，维持结界消耗咒力减免 100%（免费维持）
 */
public enum BarrierTechniqueLevel
{
    UNLEARNED(0, 0.0),
    BEGINNER(1, 0.0),
    MASTERED(2, 0.6),
    COMPLETE(3, 1.0);

    private final int tier;
    private final double maintainCostReduction;

    BarrierTechniqueLevel(int tier, double maintainCostReduction)
    {
        this.tier = tier;
        this.maintainCostReduction = maintainCostReduction;
    }

    public int getTier()
    {
        return tier;
    }

    public double getMaintainCostReduction()
    {
        return maintainCostReduction;
    }

    public static BarrierTechniqueLevel fromTier(int tier)
    {
        for (BarrierTechniqueLevel level : values())
        {
            if (level.tier == tier)
            {
                return level;
            }
        }
        return UNLEARNED;
    }

    public static BarrierTechniqueLevel fromString(String name)
    {
        try
        {
            return valueOf(name);
        }
        catch (IllegalArgumentException e)
        {
            return UNLEARNED;
        }
    }
}
