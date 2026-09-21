package com.darkness.jujutsukaisenzone.player;

/**
 * 强化术等级枚举。
 * <p>
 * 维持消耗减免上限低于结界术/降灵术，完全掌握时仍须支付咒力。
 */
public enum StrengthenTechniqueLevel
{
    UNLEARNED(0, 0.0),
    BEGINNER(1, 0.0),
    MASTERED(2, 0.35),
    COMPLETE(3, 0.65);

    private final int tier;
    private final double maintainCostReduction;

    StrengthenTechniqueLevel(int tier, double maintainCostReduction)
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

    /** 习得后每升一级额外 +50% 最大生命（初习为 1 档）。 */
    public double getActiveHealthBonusMultiplier()
    {
        if (this == UNLEARNED)
        {
            return 0.0D;
        }
        return 0.5D * tier;
    }

    /** 相对初习，每升一级跳跃初速度 +10%（初习为 1.0）。 */
    public double getJumpVelocityMultiplier()
    {
        if (this == UNLEARNED)
        {
            return 1.0D;
        }
        return 1.0D + (tier - 1) * 0.10D;
    }

    public static StrengthenTechniqueLevel fromTier(int tier)
    {
        for (StrengthenTechniqueLevel level : values())
        {
            if (level.tier == tier)
            {
                return level;
            }
        }
        return UNLEARNED;
    }

    public static StrengthenTechniqueLevel fromString(String name)
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
