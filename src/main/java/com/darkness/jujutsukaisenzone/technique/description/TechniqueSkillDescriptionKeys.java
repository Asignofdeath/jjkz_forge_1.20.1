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
    public static final String COOLDOWN = "gui." + JuJutsuKaisenZone.MODID + ".skill.cooldown";

    private TechniqueSkillDescriptionKeys()
    {
    }

    public static String description(String skillId)
    {
        return SKILL_PREFIX + skillId + ".desc";
    }
}
