package com.darkness.jujutsukaisenzone.technique.development;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按术式 id 索引静态技能树定义。
 */
public final class TechniqueSkillTreeRegistry
{
    private static final Map<String, TechniqueSkillTree> TREES = new ConcurrentHashMap<>();

    private TechniqueSkillTreeRegistry()
    {
    }

    public static void register(TechniqueSkillTree tree)
    {
        TREES.put(tree.techniqueId(), tree);
    }

    public static Optional<TechniqueSkillTree> get(String techniqueId)
    {
        if (techniqueId == null || techniqueId.isBlank())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(TREES.get(techniqueId));
    }
}
