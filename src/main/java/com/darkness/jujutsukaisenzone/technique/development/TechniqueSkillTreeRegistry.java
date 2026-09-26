package com.darkness.jujutsukaisenzone.technique.development;

import java.util.Collection;
import java.util.List;
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

    /**
     * 全部已注册技能树。
     * <p>
     * 供「静态声明表 vs 技能表」的启动自检用（见 {@code CritSkillPolicy#validate}）：
     * 那份自检需要「所有技能 id / 所有节点 id」的完整视图，逐个 {@link #get} 拿不到。
     */
    public static Collection<TechniqueSkillTree> all()
    {
        return List.copyOf(TREES.values());
    }
}
