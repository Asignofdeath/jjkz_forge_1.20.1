package com.darkness.jujutsukaisenzone.technique.development;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SP 节点：可重置的基础面板加点。质量上限限制同时点亮数量。
 */
public final class SpNodeRegistry
{
    private static final Map<String, SpNodeDef> NODES = new LinkedHashMap<>();

    private SpNodeRegistry()
    {
    }

    public static void bootstrap()
    {
        NODES.clear();
        register(new SpNodeDef("phys_small", 1, 5, 0));
        register(new SpNodeDef("phys_medium", 2, 12, 0));
        register(new SpNodeDef("tech_small", 1, 0, 5));
        register(new SpNodeDef("tech_medium", 2, 0, 12));
        register(new SpNodeDef("balanced", 2, 4, 4));
    }

    private static void register(SpNodeDef def)
    {
        NODES.put(def.nodeId(), def);
    }

    public static Optional<SpNodeDef> find(String nodeId)
    {
        if (nodeId == null || nodeId.isBlank())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(NODES.get(nodeId));
    }

    public static Collection<SpNodeDef> all()
    {
        return Collections.unmodifiableCollection(NODES.values());
    }

    /**
     * @param spCost            点亮消耗的 SP（重置退回）
     * @param physicalBonus     体术强度
     * @param techniqueBonus    术式强度
     */
    public record SpNodeDef(String nodeId, int spCost, int physicalBonus, int techniqueBonus)
    {
        public SpNodeDef
        {
            if (nodeId == null || nodeId.isBlank())
            {
                throw new IllegalArgumentException("nodeId");
            }
            spCost = Math.max(0, spCost);
            physicalBonus = Math.max(0, physicalBonus);
            techniqueBonus = Math.max(0, techniqueBonus);
        }
    }
}
