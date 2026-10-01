package com.darkness.jujutsukaisenzone.technique.scaling;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 「苍」随咒术等级成长的数值表。
 * <p>
 * 真值来自 {@code config/jujutsukaisenzone-common.toml} 的 {@code [skillCombat.limitlessBlue]} 段，
 * 经 {@link SkillCombatConfigRegistry} 汇总；配置未加载时退回 {@code LimitlessBlueConfig} 上标注
 * 【配置项·默认值】的常量。伤害再乘主动技能等级倍率（{@link SkillUpgradeLogic#scaleRatio}）。
 * <p>
 * ⛔ 本注释此前写的是 {@code data/jujutsukaisenzone/skill_combat/limitless_blue.json}——那个文件
 * 从来不存在。数值方案中途从数据包改成了 Forge 配置，注释没跟着改；别再照抄那个路径。
 */
public final class LimitlessBlueSkillGrowth
{
    private LimitlessBlueSkillGrowth()
    {
    }

    public static float damage(@Nullable LivingEntity owner)
    {
        float base = SkillCombatConfigRegistry.resolveFloat(
                SkillCombatConfigRegistry.SKILL_LIMITLESS_BLUE, "damage", owner);
        return SkillUpgradeLogic.scaleRatio(
                owner, PlayerTechniqueData.TECHNIQUE_LIMITLESS, LimitlessTechniqueSkillSet.SKILL_BLUE, base);
    }

    public static double pullRadius(@Nullable LivingEntity owner)
    {
        return SkillCombatConfigRegistry.resolveDouble(
                SkillCombatConfigRegistry.SKILL_LIMITLESS_BLUE, "pull_radius", owner);
    }
}
