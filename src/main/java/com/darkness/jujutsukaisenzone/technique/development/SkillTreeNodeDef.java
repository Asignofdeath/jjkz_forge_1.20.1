package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSpecialMove;

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
    private final TechniqueSpecialMove specialMove;
    private final SkillTreeReferenceKind referenceKind;
    private final String referenceId;
    private final String infoTitleKey;
    private final String infoIconPath;
    private final String infoDescriptionKey;
    private final List<String> prerequisiteNodeIds;
    private final List<UnlockCondition> unlockConditions;
    private final List<UnlockCondition> visibilityConditions;
    private final int intelCost;
    private final int vanillaXpLevelCost;
    private final boolean autoGranted;

    SkillTreeNodeDef(
            String nodeId,
            SkillTreeNodeKind kind,
            String skillId,
            TechniqueSpecialMove specialMove,
            SkillTreeReferenceKind referenceKind,
            String referenceId,
            String infoTitleKey,
            String infoIconPath,
            String infoDescriptionKey,
            List<String> prerequisiteNodeIds,
            List<UnlockCondition> unlockConditions,
            List<UnlockCondition> visibilityConditions,
            int intelCost,
            int vanillaXpLevelCost,
            boolean autoGranted)
    {
        this.nodeId = nodeId;
        this.kind = kind;
        this.skillId = skillId;
        this.specialMove = specialMove;
        this.referenceKind = referenceKind;
        this.referenceId = referenceId;
        this.infoTitleKey = infoTitleKey;
        this.infoIconPath = infoIconPath;
        this.infoDescriptionKey = infoDescriptionKey;
        this.prerequisiteNodeIds = List.copyOf(prerequisiteNodeIds);
        this.unlockConditions = List.copyOf(unlockConditions);
        this.visibilityConditions = List.copyOf(visibilityConditions);
        this.intelCost = Math.max(0, intelCost);
        this.vanillaXpLevelCost = Math.max(0, vanillaXpLevelCost);
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

    /** 特别招式声明；仅 {@link SkillTreeNodeKind#SPECIAL_MOVE} 节点非空。 */
    public TechniqueSpecialMove specialMove()
    {
        return specialMove;
    }

    /** 特别招式依附的原技能 skillId；非特别招式节点返回 {@code null}。 */
    public String baseSkillId()
    {
        return specialMove == null ? null : specialMove.baseSkillId();
    }

    public SkillTreeReferenceKind referenceKind()
    {
        return referenceKind;
    }

    public String referenceId()
    {
        return referenceId;
    }

    public String infoTitleKey()
    {
        return infoTitleKey;
    }

    public String infoIconPath()
    {
        return infoIconPath;
    }

    public String infoDescriptionKey()
    {
        return infoDescriptionKey;
    }

    public List<String> prerequisiteNodeIds()
    {
        return prerequisiteNodeIds;
    }

    public List<UnlockCondition> unlockConditions()
    {
        return unlockConditions;
    }

    public List<UnlockCondition> visibilityConditions()
    {
        return visibilityConditions;
    }

    public int intelCost()
    {
        return intelCost;
    }

    public int vanillaXpLevelCost()
    {
        return vanillaXpLevelCost;
    }

    public boolean autoGranted()
    {
        return autoGranted;
    }

    public boolean isSkillNode()
    {
        return kind == SkillTreeNodeKind.SKILL;
    }

    public boolean isSpecialMoveNode()
    {
        return kind == SkillTreeNodeKind.SPECIAL_MOVE;
    }

    public boolean isReferenceNode()
    {
        return kind == SkillTreeNodeKind.REFERENCE;
    }

    public boolean isInfoNode()
    {
        return kind == SkillTreeNodeKind.INFO;
    }

    /**
     * 两个节点之间是否算一条「连线」——决定要不要画前置线、要不要参与点选高亮的连通关系。
     * <p>
     * <b>说明节点不参与任何连线。</b>它在 {@link #prerequisiteNodeIds()} 里声明的节点只被
     * {@link SkillTreeLayoutEngine} 当作<b>布局锚点</b>（决定它排在哪一列、行序上贴近谁），
     * 与解锁前置无关：没有任何技能以说明节点为前置，说明节点自身也不参与解锁判定
     * （{@code SkillUnlockLogic#isNodeRequirementMet} 对说明节点只查可见性条件）。
     * 因此连线与高亮都必须把说明节点排除，否则界面上会出现「不代表任何前置关系的线」。
     */
    public static boolean isConnectableEdge(SkillTreeNodeDef from, SkillTreeNodeDef to)
    {
        if (from == null || to == null)
        {
            return false;
        }
        return !from.isInfoNode() && !to.isInfoNode();
    }

    public static Builder skill(String nodeId, String skillId)
    {
        return new Builder(nodeId, SkillTreeNodeKind.SKILL).skillId(skillId);
    }

    /**
     * 特别招式节点：{@code nodeId} 由 {@code TechniqueSpecialMoveAttacher} 生成，
     * 通常 {@code requires(原技能节点)}，{@code skillId} 取变体自身 id。
     */
    public static Builder specialMove(String nodeId, TechniqueSpecialMove specialMove)
    {
        return new Builder(nodeId, SkillTreeNodeKind.SPECIAL_MOVE).specialMove(specialMove);
    }

    public static Builder reference(String nodeId, SkillTreeReferenceKind referenceKind, String referenceId)
    {
        return new Builder(nodeId, SkillTreeNodeKind.REFERENCE)
                .reference(referenceKind, referenceId);
    }

    public static Builder info(String nodeId)
    {
        return new Builder(nodeId, SkillTreeNodeKind.INFO);
    }

    public static final class Builder
    {
        private final String nodeId;
        private final SkillTreeNodeKind kind;
        private String skillId = "";
        private TechniqueSpecialMove specialMove;
        private SkillTreeReferenceKind referenceKind;
        private String referenceId = "";
        private String infoTitleKey = "";
        private String infoIconPath = "";
        private String infoDescriptionKey = "";
        private List<String> prerequisiteNodeIds = Collections.emptyList();
        private List<UnlockCondition> unlockConditions = Collections.emptyList();
        private List<UnlockCondition> visibilityConditions = Collections.emptyList();
        private int intelCost;
        private int vanillaXpLevelCost;
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

        /** 特别招式节点：skillId 自动取变体自身 id，展示信息全部来自 {@code specialMove}。 */
        public Builder specialMove(TechniqueSpecialMove specialMove)
        {
            this.specialMove = specialMove;
            this.skillId = specialMove.variantId();
            return this;
        }

        public Builder reference(SkillTreeReferenceKind referenceKind, String referenceId)
        {
            this.referenceKind = referenceKind;
            this.referenceId = referenceId;
            return this;
        }

        public Builder titleKey(String infoTitleKey)
        {
            this.infoTitleKey = infoTitleKey;
            return this;
        }

        public Builder iconPath(String infoIconPath)
        {
            this.infoIconPath = infoIconPath;
            return this;
        }

        public Builder descriptionKey(String infoDescriptionKey)
        {
            this.infoDescriptionKey = infoDescriptionKey;
            return this;
        }

        /**
         * 声明前置节点。
         * <p>
         * 技能 / 特别招式节点：表示解锁前置，会画连线。
         * <br>
         * 说明节点（{@link SkillTreeNodeDef#info}）：只表示<b>布局锚点</b>，用于自动布局定位，
         * <b>不画连线</b>、也不参与解锁判定（见 {@link SkillTreeNodeDef#isConnectableEdge}）。
         */
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

        public Builder visibleWhen(UnlockCondition... visibilityConditions)
        {
            this.visibilityConditions = List.of(visibilityConditions);
            return this;
        }

        public Builder intelCost(int intelCost)
        {
            this.intelCost = intelCost;
            return this;
        }

        /** 解锁时消耗的原版经验等级。 */
        public Builder vanillaXpLevelCost(int vanillaXpLevelCost)
        {
            this.vanillaXpLevelCost = vanillaXpLevelCost;
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
                    specialMove,
                    referenceKind,
                    referenceId,
                    infoTitleKey,
                    infoIconPath,
                    infoDescriptionKey,
                    prerequisiteNodeIds,
                    unlockConditions,
                    visibilityConditions,
                    intelCost,
                    vanillaXpLevelCost,
                    autoGranted
            );
        }
    }
}
