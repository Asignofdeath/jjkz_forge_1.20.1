package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.technique.skillset.IInnateTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSpecialMove;

import java.util.ArrayList;
import java.util.List;

/**
 * 把技能集声明的特别招式挂到技能树上。
 * <p>
 * 技能集只在 {@link IInnateTechniqueSkillSet#getSpecialMoves(String)} 里按原技能声明变体，
 * 树定义本身不需要（也不应该）手写这些节点；注册时由本类统一派生：
 * <ul>
 *   <li>节点 id = {@code 原技能节点 id + "$" + variantId}，保证唯一且可反查；</li>
 *   <li>前置 = 原技能节点 → 开发界面自动连出一条「原技能 → 特别招式」的边；</li>
 *   <li>图标 / 数值 / 解锁状态默认取原技能（见 {@code TechniqueSpecialMove}）。</li>
 * </ul>
 */
public final class TechniqueSpecialMoveAttacher
{
    /** 特别招式节点 id 与普通节点 id 的分隔符。 */
    private static final String NODE_ID_SEPARATOR = "$";

    private TechniqueSpecialMoveAttacher()
    {
    }

    /**
     * 返回一棵附加了特别招式节点的新树；没有可附加内容时原样返回。
     */
    public static TechniqueSkillTree attach(TechniqueSkillTree tree, IInnateTechniqueSkillSet skillSet)
    {
        if (tree == null || skillSet == null || !tree.techniqueId().equals(skillSet.techniqueId()))
        {
            return tree;
        }

        List<SkillTreeNodeDef> skillNodes = new ArrayList<>();
        for (SkillTreeNodeDef node : tree.nodes())
        {
            if (node.isSkillNode())
            {
                skillNodes.add(node);
            }
        }
        if (skillNodes.isEmpty())
        {
            return tree;
        }

        TechniqueSkillTree.Builder builder = TechniqueSkillTree.builder(tree.techniqueId());
        for (SkillTreeNodeDef node : tree.nodes())
        {
            builder.node(node);
        }

        boolean appended = false;
        for (SkillTreeNodeDef skillNode : skillNodes)
        {
            for (TechniqueSpecialMove move : skillSet.getSpecialMoves(skillNode.skillId()))
            {
                builder.node(SkillTreeNodeDef.specialMove(nodeIdOf(skillNode, move), move)
                        .requires(skillNode.nodeId())
                        .build());
                appended = true;
            }
        }
        return appended ? builder.build() : tree;
    }

    /** 特别招式节点 id：{@code <原技能节点 id>$<variantId>}。 */
    public static String nodeIdOf(SkillTreeNodeDef baseNode, TechniqueSpecialMove move)
    {
        return baseNode.nodeId() + NODE_ID_SEPARATOR + move.variantId();
    }
}
