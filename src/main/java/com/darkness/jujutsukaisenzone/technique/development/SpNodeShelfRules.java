package com.darkness.jujutsukaisenzone.technique.development;

/**
 * SP 货架定案常数与派生公式（咒术经验驱动）。
 * <pre>
 * tier = floor(xp / 500)
 * 质量上限 = min(18, 6 + tier)
 * 宽限     = min(5, 2 + floor(tier / 3))
 * </pre>
 */
public final class SpNodeShelfRules
{
    public static final int XP_PER_TIER = 500;
    /** 特级经验门槛（与 {@code PlayerCursedGradeData#XP_SPECIAL} 对齐）。 */
    public static final int SPECIAL_XP = 4000;
    /** {@code floor(SPECIAL_XP / XP_PER_TIER)} */
    public static final int SPECIAL_TIER = SPECIAL_XP / XP_PER_TIER;

    public static final int BASE_QUALITY = 6;
    public static final int MAX_QUALITY = 18;
    public static final int BASE_ALLOWANCE = 2;
    public static final int MAX_ALLOWANCE = 5;

    public static final int OFFERS_PER_TIER_PRE_SPECIAL = 6;
    public static final int OFFERS_PER_TIER_POST_SPECIAL = 1;
    public static final int MAX_UNLIT_OFFERS = 12;

    public static final int REFRESH_COST_PRE_SPECIAL = 2;
    public static final int REFRESH_COST_POST_SPECIAL = 3;

    private SpNodeShelfRules()
    {
    }

    public static int tier(int curseGradeExperience)
    {
        return Math.max(0, curseGradeExperience) / XP_PER_TIER;
    }

    public static boolean isPostSpecial(int curseGradeExperience)
    {
        return Math.max(0, curseGradeExperience) >= SPECIAL_XP;
    }

    public static boolean isPostSpecialTier(int tier)
    {
        return tier >= SPECIAL_TIER;
    }

    public static int qualityCap(int curseGradeExperience)
    {
        return Math.min(MAX_QUALITY, BASE_QUALITY + tier(curseGradeExperience));
    }

    public static int allowance(int curseGradeExperience)
    {
        int t = tier(curseGradeExperience);
        return Math.min(MAX_ALLOWANCE, BASE_ALLOWANCE + t / 3);
    }

    public static int offersGrantedForTier(int tierJustReached)
    {
        if (tierJustReached <= 0)
        {
            return 0;
        }
        return isPostSpecialTier(tierJustReached)
                ? OFFERS_PER_TIER_POST_SPECIAL
                : OFFERS_PER_TIER_PRE_SPECIAL;
    }

    /** 特级前日终重 roll 后的在售数量目标。 */
    public static int dailyShelfSize(int tier)
    {
        if (tier <= 0)
        {
            return 0;
        }
        return Math.min(MAX_UNLIT_OFFERS, OFFERS_PER_TIER_PRE_SPECIAL * Math.min(tier, 2));
    }

    public static int refreshCost(int curseGradeExperience)
    {
        return isPostSpecial(curseGradeExperience)
                ? REFRESH_COST_POST_SPECIAL
                : REFRESH_COST_PRE_SPECIAL;
    }

    /**
     * 异向节点权重：{@code clamp(0.10 + 0.04 * min(tier, 8), 0.10, 0.40)}。
     */
    public static double offAxisChance(int tier)
    {
        double raw = 0.10D + 0.04D * Math.min(Math.max(0, tier), SPECIAL_TIER);
        return Math.max(0.10D, Math.min(0.40D, raw));
    }
}
