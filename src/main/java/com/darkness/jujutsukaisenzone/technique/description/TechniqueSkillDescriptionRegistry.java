package com.darkness.jujutsukaisenzone.technique.description;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 {@code skillId} 注册描述参数解析器。
 */
public final class TechniqueSkillDescriptionRegistry
{
    private static final Map<String, TechniqueSkillDescriptionArgsResolver> RESOLVERS = new ConcurrentHashMap<>();

    private TechniqueSkillDescriptionRegistry()
    {
    }

    public static void register(String skillId, TechniqueSkillDescriptionArgsResolver resolver)
    {
        if (skillId == null || skillId.isBlank() || resolver == null)
        {
            return;
        }
        RESOLVERS.put(skillId, resolver);
    }

    public static TechniqueSkillDescriptionArgsResolver get(String skillId)
    {
        return skillId == null ? null : RESOLVERS.get(skillId);
    }

    public static boolean has(String skillId)
    {
        return skillId != null && RESOLVERS.containsKey(skillId);
    }
}
