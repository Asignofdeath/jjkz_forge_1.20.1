package com.darkness.jujutsukaisenzone.technique.development;

/**
 * 主动技能等级规则：满级门槛与倍率曲线。
 * <p>
 * 等级写在各技能的 {@code levelTag} 上。出伤时应先得到
 * {@code 有效技能倍率 = 技能倍率 × powerMultiplier(等级)}，再交给
 * {@link com.darkness.jujutsukaisenzone.technique.SkillDamageResolver}
 *（{@code 强度 × 有效技能倍率}），不要在 Resolver 之后再乘本倍率。
 */
public final class SkillLevelRules
{
    /** 主动技能最高等级。 */
    public static final int MAX_LEVEL = 5;

    /** 每升一级相对 1 级增加的伤害/效果倍率（L1=1.0，L5=1.4）。 */
    public static final float POWER_BONUS_PER_LEVEL = 0.10F;

    private SkillLevelRules()
    {
    }

    public static int clampLevel(int level)
    {
        return Math.max(1, Math.min(MAX_LEVEL, level));
    }

    public static boolean isMaxLevel(int level)
    {
        return clampLevel(level) >= MAX_LEVEL;
    }

    /**
     * 技能等级 → 自身基值倍率。缺省 / 未写入 tag 时按 1 级计。
     */
    public static float powerMultiplier(int level)
    {
        int clamped = clampLevel(level);
        return 1.0F + POWER_BONUS_PER_LEVEL * (clamped - 1);
    }
}
