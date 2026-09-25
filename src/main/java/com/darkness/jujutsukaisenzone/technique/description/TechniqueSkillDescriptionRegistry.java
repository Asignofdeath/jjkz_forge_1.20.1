package com.darkness.jujutsukaisenzone.technique.description;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 {@code skillId} 注册描述参数解析器。
 */
public final class TechniqueSkillDescriptionRegistry
{
    private static final Map<String, TechniqueSkillDescriptionArgsResolver> RESOLVERS = new ConcurrentHashMap<>();
    private static final Map<String, TechniqueSkillDescriptionVariantResolver> VARIANTS = new ConcurrentHashMap<>();

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

    /**
     * 注册「按解锁状态换文案」的解析器（见 {@link TechniqueSkillDescriptionVariant}）。
     * <p>
     * 它与 {@link #register} 可以并存：有变体时优先用变体（键与参数都由变体一次给全，两者不会走偏），
     * 变体返回 {@code null} 时才回退到这里的参数解析器 + 默认键。
     */
    public static void registerVariant(String skillId, TechniqueSkillDescriptionVariantResolver resolver)
    {
        if (skillId == null || skillId.isBlank() || resolver == null)
        {
            return;
        }
        VARIANTS.put(skillId, resolver);
    }

    public static TechniqueSkillDescriptionVariantResolver getVariant(String skillId)
    {
        return skillId == null ? null : VARIANTS.get(skillId);
    }

    public static boolean has(String skillId)
    {
        return skillId != null && RESOLVERS.containsKey(skillId);
    }
}
