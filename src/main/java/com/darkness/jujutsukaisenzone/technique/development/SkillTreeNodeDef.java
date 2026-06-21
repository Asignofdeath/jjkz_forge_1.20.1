package com.darkness.jujutsukaisenzone.technique.development;

import java.util.Collections;
import java.util.List;

/**
 * 术式开发树中的单个节点定义。
 * <p>
 * 节点坐标由 {@link SkillTreeLayoutEngine} 自动计算，不再手动指定。
 */
public final class SkillTreeNodeDef
{
    private final String nodeId;
    private final SkillTreeNodeKind kind;
    private final String skillId;
    private final SkillTreeReferenceKind referenceKind;
    private final String referenceId;
    private final List<String> prerequisiteNodeIds;
    private final List<UnlockCondition> unlockConditions;
    private final int intelCost;
    private final boolean autoGranted;

    SkillTreeNodeDef(
            String nodeId,
            SkillTreeNodeKind kind,
            String skillId,
            SkillTreeReferenceKind referenceKind,
            String referenceId,
            List<String> prerequisiteNodeIds,
            List<UnlockCondition> unlockConditions,
            int intelCost,
            boolean autoGranted)
    {
        this.nodeId = nodeId;
        this.kind = kind;
        this.skillId = skillId;
        this.referenceKind = referenceKind;
        this.referenceId = referenceId;
        this.prerequisiteNodeIds = List.copyOf(prerequisiteNodeIds);
        this.unlockConditions = List.copyOf(unlockConditions);
        this.intelCost = Math.max(0, intelCost);
        this.autoGranted = autoGranted;
    }

    public String nodeId()
    {
        return nodeId;
    }

    public SkillTreeNodeKind kind()
    {
        return kind;
    }

    public String skillId()
    {
        return skillId;
    }

    public SkillTreeReferenceKind referenceKind()
    {
        return referenceKind;
    }

    public String referenceId()
    {
        return referenceId;
    }

    public List<String> prerequisiteNodeIds()
    {
        return prerequisiteNodeIds;
    }

    public List<UnlockCondition> unlockConditions()
    {
        return unlockConditions;
    }

    public int intelCost()
    {
        return intelCost;
    }

    public boolean autoGranted()
    {
        return autoGranted;
    }

    public boolean isSkillNode()
    {
        return kind == SkillTreeNodeKind.SKILL;
    }

    public boolean isReferenceNode()
    {
        return kind == SkillTreeNodeKind.REFERENCE;
    }

    public static Builder skill(String nodeId, String skillId)
    {
        return new Builder(nodeId, SkillTreeNodeKind.SKILL).skillId(skillId);
    }

    public static Builder reference(String nodeId, SkillTreeReferenceKind referenceKind, String referenceId)
    {
        return new Builder(nodeId, SkillTreeNodeKind.REFERENCE)
                .reference(referenceKind, referenceId);
    }

    public static final class Builder
    {
        private final String nodeId;
        private final SkillTreeNodeKind kind;
        private String skillId = "";
        private SkillTreeReferenceKind referenceKind;
        private String referenceId = "";
        private List<String> prerequisiteNodeIds = Collections.emptyList();
        private List<UnlockCondition> unlockConditions = Collections.emptyList();
        private int intelCost;
        private boolean autoGranted;

        private Builder(String nodeId, SkillTreeNodeKind kind)
        {
            this.nodeId = nodeId;
            this.kind = kind;
        }

        public Builder skillId(String skillId)
        {
            this.skillId = skillId;
            return this;
        }

        public Builder reference(SkillTreeReferenceKind referenceKind, String referenceId)
        {
            this.referenceKind = referenceKind;
            this.referenceId = referenceId;
            return this;
        }

        public Builder requires(String... prerequisiteNodeIds)
        {
            this.prerequisiteNodeIds = List.of(prerequisiteNodeIds);
            return this;
        }

        public Builder conditions(UnlockCondition... unlockConditions)
        {
            this.unlockConditions = List.of(unlockConditions);
            return this;
        }

        public Builder intelCost(int intelCost)
        {
            this.intelCost = intelCost;
            return this;
        }

        /** 获得术式后视为已解锁，不消耗术式情报。 */
        public Builder autoGranted()
        {
            this.autoGranted = true;
            this.intelCost = 0;
            return this;
        }

        public SkillTreeNodeDef build()
        {
            return new SkillTreeNodeDef(
                    nodeId,
                    kind,
                    skillId,
                    referenceKind,
                    referenceId,
                    prerequisiteNodeIds,
                    unlockConditions,
                    intelCost,
                    autoGranted
            );
        }
    }
}
