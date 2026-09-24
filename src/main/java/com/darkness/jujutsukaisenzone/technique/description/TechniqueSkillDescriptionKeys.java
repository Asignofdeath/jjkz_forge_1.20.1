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
     * 技能简介（简洁页）正文键，与完整描述 {@link #description(String)} 平行。
     * <p>
     * 简洁页不显示任何数值，所以按<b>无参</b>翻译解析。这个键是「有没有简洁页」的唯一依据：
     * 键不存在（翻译结果落回键名本身）时，悬停框直接回退完整页，不再需要维护一份技能名单。
     */
    public static String brief(String skillId)
    {
        return SKILL_PREFIX + skillId + ".brief";
    }
}
