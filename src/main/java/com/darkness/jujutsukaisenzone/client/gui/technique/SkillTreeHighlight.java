package com.darkness.jujutsukaisenzone.client.gui.technique;

import java.util.Set;

/**
 * 术式开发树的「选中高亮」状态：一个被点选的节点，以及它的全部直连节点与连线。
 * <p>
 * 由 {@link TechniqueDevelopmentCanvasWidget} 在节点被点击时算出，交给
 * {@link SkillTreeNodeWidget}（描边 / 光晕）与 {@link SkillTreeConnectionsWidget}（连线配色）。
 * 只描述「谁和谁相关」，不含任何渲染逻辑。
 */
public record SkillTreeHighlight(String selectedNodeId, Set<String> connectedNodeIds, Set<String> connectedEdges)
{
    public static final SkillTreeHighlight NONE = new SkillTreeHighlight("", Set.of(), Set.of());

    /** 连线键：前置节点 → 后继节点。 */
    public static String edgeKey(String fromNodeId, String toNodeId)
    {
        return fromNodeId + '\u0001' + toNodeId;
    }

    public boolean isActive()
    {
        return selectedNodeId != null && !selectedNodeId.isBlank();
    }

    public boolean isSelected(String nodeId)
    {
        return nodeId != null && nodeId.equals(selectedNodeId);
    }

    /** 是否为选中节点的直连节点（前置或后继）。 */
    public boolean isConnected(String nodeId)
    {
        return nodeId != null && connectedNodeIds.contains(nodeId);
    }

    /** 该连线（前置 → 后继）是否与选中节点相连。 */
    public boolean isEdgeConnected(String fromNodeId, String toNodeId)
    {
        return connectedEdges.contains(edgeKey(fromNodeId, toNodeId));
    }
}
