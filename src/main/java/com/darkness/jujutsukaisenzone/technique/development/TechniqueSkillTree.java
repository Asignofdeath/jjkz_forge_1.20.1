package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 某生得术式的完整技能解锁图（静态定义）。
 */
public final class TechniqueSkillTree
{
    private final String techniqueId;
    private final Map<String, SkillTreeNodeDef> nodesById;
    private final Map<String, SkillTreeNodeDef> skillNodesBySkillId;

    private TechniqueSkillTree(String techniqueId, Map<String, SkillTreeNodeDef> nodesById)
    {
        this.techniqueId = techniqueId;
        this.nodesById = Collections.unmodifiableMap(nodesById);
        Map<String, SkillTreeNodeDef> skillIndex = new LinkedHashMap<>();
        for (SkillTreeNodeDef node : nodesById.values())
        {
            if (node.isSkillNode())
            {
                skillIndex.put(node.skillId(), node);
            }
        }
        this.skillNodesBySkillId = Collections.unmodifiableMap(skillIndex);
        validate();
    }

    public String techniqueId()
    {
        return techniqueId;
    }

    public Collection<SkillTreeNodeDef> nodes()
    {
        return nodesById.values();
    }

    public Optional<SkillTreeNodeDef> findNode(String nodeId)
    {
        return Optional.ofNullable(nodesById.get(nodeId));
    }

    public Optional<SkillTreeNodeDef> findSkillNode(String skillId)
    {
        return Optional.ofNullable(skillNodesBySkillId.get(skillId));
    }

    public List<SkillTreeNodeDef> rootSkillNodes()
    {
        List<SkillTreeNodeDef> roots = new ArrayList<>();
        for (SkillTreeNodeDef node : nodesById.values())
        {
            if (node.isSkillNode() && node.prerequisiteNodeIds().isEmpty() && node.unlockConditions().isEmpty())
            {
                roots.add(node);
            }
        }
        return roots;
    }

    /**
     * 校验节点 id 唯一、前置存在、skillId 与 {@link TechniqueSkill} 列表一致，并检测环。
     */
    public void validateAgainstSkills(Collection<TechniqueSkill> skills)
    {
        for (TechniqueSkill skill : skills)
        {
            if (!skillNodesBySkillId.containsKey(skill.skillId()))
            {
                throw new IllegalStateException("术式 " + techniqueId + " 的技能树缺少节点：" + skill.skillId());
            }
        }
    }

    private void validate()
    {
        for (SkillTreeNodeDef node : nodesById.values())
        {
            for (String prereqId : node.prerequisiteNodeIds())
            {
                if (!nodesById.containsKey(prereqId))
                {
                    throw new IllegalStateException("术式 " + techniqueId + " 节点 " + node.nodeId()
                            + " 引用了未知前置：" + prereqId);
                }
            }
            if (node.isSkillNode() && (node.skillId() == null || node.skillId().isBlank()))
            {
                throw new IllegalStateException("术式 " + techniqueId + " 技能节点缺少 skillId：" + node.nodeId());
            }
            if (node.isReferenceNode()
                    && (node.referenceKind() == null || node.referenceId() == null || node.referenceId().isBlank()))
            {
                throw new IllegalStateException("术式 " + techniqueId + " 引用节点配置不完整：" + node.nodeId());
            }
            if (node.isInfoNode())
            {
                if (node.infoTitleKey() == null || node.infoTitleKey().isBlank())
                {
                    throw new IllegalStateException("术式 " + techniqueId + " 说明节点缺少 titleKey：" + node.nodeId());
                }
                if (node.infoDescriptionKey() == null || node.infoDescriptionKey().isBlank())
                {
                    throw new IllegalStateException("术式 " + techniqueId + " 说明节点缺少 descriptionKey：" + node.nodeId());
                }
                if (node.visibilityConditions().isEmpty())
                {
                    throw new IllegalStateException("术式 " + techniqueId + " 说明节点缺少 visibilityConditions：" + node.nodeId());
                }
            }
        }
        detectCycle();
    }

    private void detectCycle()
    {
        Map<String, Integer> state = new LinkedHashMap<>();
        for (String nodeId : nodesById.keySet())
        {
            if (hasCycle(nodeId, state))
            {
                throw new IllegalStateException("术式 " + techniqueId + " 技能树存在环，节点：" + nodeId);
            }
        }
    }

    /** 0=未访问, 1=访问中, 2=已完成 */
    private boolean hasCycle(String nodeId, Map<String, Integer> state)
    {
        Integer current = state.get(nodeId);
        if (current != null && current == 1)
        {
            return true;
        }
        if (current != null && current == 2)
        {
            return false;
        }
        state.put(nodeId, 1);
        SkillTreeNodeDef node = nodesById.get(nodeId);
        if (node != null)
        {
            for (String prereqId : node.prerequisiteNodeIds())
            {
                if (hasCycle(prereqId, state))
                {
                    return true;
                }
            }
        }
        state.put(nodeId, 2);
        return false;
    }

    public static Builder builder(String techniqueId)
    {
        return new Builder(techniqueId);
    }

    public static final class Builder
    {
        private final String techniqueId;
        private final Map<String, SkillTreeNodeDef> nodes = new LinkedHashMap<>();

        private Builder(String techniqueId)
        {
            this.techniqueId = techniqueId;
        }

        public Builder node(SkillTreeNodeDef node)
        {
            if (nodes.containsKey(node.nodeId()))
            {
                throw new IllegalArgumentException("重复节点 id：" + node.nodeId());
            }
            nodes.put(node.nodeId(), node);
            return this;
        }

        public TechniqueSkillTree build()
        {
            return new TechniqueSkillTree(techniqueId, nodes);
        }
    }
}
