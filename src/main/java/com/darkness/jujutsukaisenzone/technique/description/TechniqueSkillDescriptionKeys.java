package com.darkness.jujutsukaisenzone.technique.description;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;

/**
 * 术式技能描述相关的语言键。
 * <p>
 * 描述正文键约定为 {@code skill.jujutsukaisenzone.<skillId>.desc}，参数由
 * {@link TechniqueSkillDescriptionRegistry} 中注册的解析器提供。
 */
public final class TechniqueSkillDescriptionKeys
{
    private static final String SKILL_PREFIX = "skill." + JuJutsuKaisenZone.MODID + ".";

    public static final String COST = "gui." + JuJutsuKaisenZone.MODID + ".skill.cost";
    public static final String MAINTAIN_COST = "gui." + JuJutsuKaisenZone.MODID + ".skill.maintain_cost";
    public static final String TOGGLE = "gui." + JuJutsuKaisenZone.MODID + ".skill.toggle";
    public static final String COOLDOWN = "gui." + JuJutsuKaisenZone.MODID + ".skill.cooldown";

    /** 机制型技能的悬停标注行，与特别招式的 {@code special_move_tag} 同一用法。 */
    public static final String MECHANIC_SKILL_TAG =
            "gui." + JuJutsuKaisenZone.MODID + ".skill_tree.mechanic_skill_tag";

    /** 领域说明节点的悬停标注行，红色，同样与 {@code special_move_tag} 同一形态。 */
    public static final String DOMAIN_EXPANSION_TAG =
            "gui." + JuJutsuKaisenZone.MODID + ".skill_tree.domain_expansion_tag";

    /** 「本技能不参与暴击」的悬停标注行，与 {@code mechanic_skill_tag} 同一形态。 */
    public static final String NO_CRIT_TAG =
            "gui." + JuJutsuKaisenZone.MODID + ".skill_tree.no_crit_tag";

    /** 简洁页底部的「按住 Shift」提示行。 */
    public static final String SHIFT_FOR_DETAILS =
            "gui." + JuJutsuKaisenZone.MODID + ".skill.shift_for_details";

    private TechniqueSkillDescriptionKeys()
    {
    }

    public static String description(String skillId)
    {
        return SKILL_PREFIX + skillId + ".desc";
    }

    /**
     * 带变体后缀的描述键（{@code skill.<modid>.<skillId>.desc_<suffix>}），供按解锁状态换文案的技能使用。
     *
     * @param variantSuffix 变体后缀；为 null / 空串时等同 {@link #description(String)}。
     */
    public static String description(String skillId, String variantSuffix)
    {
        return withSuffix(description(skillId), variantSuffix);
    }

    /**
     * 技能简介（简洁页）正文键，与完整描述 {@link #description(String)} 平行。
     * <p>
     * 简洁页不显示任何数值，所以按<b>无参</b>翻译解析。这个键是「有没有简洁页」的唯一依据：
     * 键不存在（翻译结果落回键名本身）时，悬停框直接回退完整页，不再需要维护一份技能名单。
     */
    public static String brief(String skillId)
    {
        return SKILL_PREFIX + skillId + ".brief";
    }

    /** 带变体后缀的简洁页键，与 {@link #description(String, String)} 同一约定。 */
    public static String brief(String skillId, String variantSuffix)
    {
        return withSuffix(brief(skillId), variantSuffix);
    }

    private static String withSuffix(String baseKey, String variantSuffix)
    {
        return variantSuffix == null || variantSuffix.isBlank() ? baseKey : baseKey + "_" + variantSuffix;
    }
}
