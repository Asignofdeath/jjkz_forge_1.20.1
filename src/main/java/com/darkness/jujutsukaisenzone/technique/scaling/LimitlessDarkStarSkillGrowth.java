package com.darkness.jujutsukaisenzone.technique.scaling;

import com.darkness.jujutsukaisenzone.entity.data.BlackHoleSkillConfig;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 「极之番——暗星」随咒术等级成长的数值表。
 * <p>
 * 真值来自 {@code Config} 中 {@code [skillCombat.limitlessDarkStar]} 各项的默认值，
 * 经 {@link SkillCombatConfigRegistry} 汇总；未汇总时退回 {@link BlackHoleSkillConfig}
 * 上标注【配置项·默认值】的常量。伤害再乘主动技能等级倍率（{@link SkillUpgradeLogic#scaleRatio}）。
 */
public final class LimitlessDarkStarSkillGrowth
{
    private LimitlessDarkStarSkillGrowth()
    {
    }

    /** 飞行擦伤倍率 */
    public static float scratchDamage(@Nullable LivingEntity owner)
    {
        float base = SkillCombatConfigRegistry.resolveFloat(
                SkillCombatConfigRegistry.SKILL_LIMITLESS_DARK_STAR, "scratch_damage", owner);
        return SkillUpgradeLogic.scaleRatio(
                owner, PlayerTechniqueData.TECHNIQUE_LIMITLESS, LimitlessTechniqueSkillSet.SKILL_DARK_STAR, base);
    }

    /** 驻留坍缩每跳伤害倍率 */
    public static float zoneDamage(@Nullable LivingEntity owner)
    {
        float base = SkillCombatConfigRegistry.resolveFloat(
                SkillCombatConfigRegistry.SKILL_LIMITLESS_DARK_STAR, "damage", owner);
        return SkillUpgradeLogic.scaleRatio(
                owner, PlayerTechniqueData.TECHNIQUE_LIMITLESS, LimitlessTechniqueSkillSet.SKILL_DARK_STAR, base);
    }
}
