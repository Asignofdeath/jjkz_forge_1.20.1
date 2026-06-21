package com.darkness.jujutsukaisenzone.player;

/**
 * 降灵术等级枚举（拟态维持消耗减免，规则对齐结界术）。
 * <p>
 * - UNLEARNED: 未习得，无法使用降灵术
 * - BEGINNER: 初习，维持拟态消耗咒力减免 0%
 * - MASTERED: 掌握，维持拟态消耗咒力减免 60%
 * - COMPLETE: 完全掌握，维持拟态消耗咒力减免 100%（免费维持）
 */
public enum SeanceTechniqueLevel
{
    UNLEARNED(0, 0.0),
    BEGINNER(1, 0.0),
    MASTERED(2, 0.6),
    COMPLETE(3, 1.0);

    private final int tier;
    private final double maintainCostReduction;

    SeanceTechniqueLevel(int tier, double maintainCostReduction)
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

    public static SeanceTechniqueLevel fromTier(int tier)
    {
        for (SeanceTechniqueLevel level : values())
        {
            if (level.tier == tier)
            {
                return level;
            }
        }
        return UNLEARNED;
    }

    public static SeanceTechniqueLevel fromString(String name)
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
