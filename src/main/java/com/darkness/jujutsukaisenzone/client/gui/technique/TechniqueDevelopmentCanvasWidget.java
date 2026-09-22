package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.lowdragmc.lowdraglib.gui.widget.FreeGraphView;
import com.lowdragmc.lowdraglib.utils.Size;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeLayoutEngine;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTreeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 术式开发全屏画布：渲染当前术式技能树的节点与连线。
 */
@OnlyIn(Dist.CLIENT)
public final class TechniqueDevelopmentCanvasWidget extends FreeGraphView
{
    private String treeId = "";
    /** 当前点选高亮；{@link SkillTreeHighlight#NONE} 表示没有选中任何节点。 */
    private SkillTreeHighlight highlight = SkillTreeHighlight.NONE;
    /** 最近一次构建出的可见树，点击节点时用来反查前置 / 后继。 */
    private TechniqueSkillTree layoutTree;
    /** 点击回调里不能直接重建（父容器正在遍历子组件列表），推迟到本帧 updateScreen 执行。 */
    private String pendingSelectedNodeId;
    private boolean hasPendingSelection;

    public TechniqueDevelopmentCanvasWidget()
    {
        this(resolveDefaultTreeId());
    }

    public TechniqueDevelopmentCanvasWidget(String treeId)
    {
        super(0, 0, 1, 1);
        this.treeId = treeId == null ? "" : treeId;
        setDrawGrid(true);
        setGridWidth(20);
    }

    public String treeId()
    {
        return treeId;
    }

    public void setTreeId(String treeId)
    {
        this.treeId = treeId == null ? "" : treeId;
    }

    public void rebuildGraph()
    {
        rebuildGraph(true);
    }

    /**
     * 重建整张图。
     *
     * @param refitView 是否重新自适应缩放。点选高亮触发的重建传 false，以保留玩家当前的平移 / 缩放。
     */
    private void rebuildGraph(boolean refitView)
    {
        clearAllWidgets();
        this.layoutTree = null;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || treeId.isBlank())
        {
            return;
        }

        Optional<TechniqueSkillTree> treeOpt = TechniqueSkillTreeRegistry.get(treeId);
        if (treeOpt.isEmpty())
        {
            return;
        }

        TechniqueSkillTree tree = treeOpt.get();
        java.util.List<SkillTreeNodeDef> visibleNodes = tree.nodes().stream()
                .filter(node -> !node.isInfoNode() || SkillUnlockLogic.isInfoNodeVisible(minecraft.player, node))
                .toList();
        TechniqueSkillTree.Builder layoutBuilder = TechniqueSkillTree.builder(tree.techniqueId());
        for (SkillTreeNodeDef node : visibleNodes)
        {
            layoutBuilder.node(node);
        }
        this.layoutTree = layoutBuilder.build();
        Map<String, int[]> layout = SkillTreeLayoutEngine.layout(this.layoutTree);

        Map<String, SkillTreeNodeWidget> nodeWidgets = new LinkedHashMap<>();
        for (SkillTreeNodeDef nodeDef : visibleNodes)
        {
            SkillTreeNodeWidget widget = new SkillTreeNodeWidget(treeId, nodeDef);
            int[] pos = layout.getOrDefault(nodeDef.nodeId(), new int[] { 0, 0 });
            widget.setSelfPosition(pos[0], pos[1]);
            widget.setHighlight(highlight);
            widget.setOnNodeClicked(this::selectNode);
            nodeWidgets.put(nodeDef.nodeId(), widget);
        }

        SkillTreeConnectionsWidget connections = new SkillTreeConnectionsWidget(this, this.layoutTree, nodeWidgets);
        connections.setHighlight(highlight);
        addWidget(connections);

        // LDLib 按子组件加入顺序绘制，后加入者在上层。
        // 依次加入「普通节点 → 直连节点 → 选中节点」，使高亮子图盖在其余节点之上（连线始终垫底）。
        for (SkillTreeNodeWidget widget : nodeWidgets.values())
        {
            String nodeId = widget.nodeDef().nodeId();
            if (!highlight.isSelected(nodeId) && !highlight.isConnected(nodeId))
            {
                addWidget(widget);
            }
        }
        for (SkillTreeNodeWidget widget : nodeWidgets.values())
        {
            String nodeId = widget.nodeDef().nodeId();
            if (highlight.isConnected(nodeId) && !highlight.isSelected(nodeId))
            {
                addWidget(widget);
            }
        }
        for (SkillTreeNodeWidget widget : nodeWidgets.values())
        {
            if (highlight.isSelected(widget.nodeDef().nodeId()))
            {
                addWidget(widget);
            }
        }

        if (refitView)
        {
            resetFitScaleByWidgets();
        }
    }

    /**
     * 点选节点：高亮它的全部连线与直连节点，并把它们提到最上层；再次点击同一节点则取消。
     * <p>
     * 这里只登记意图——本方法是在父容器的子组件遍历中（mouseClicked）被调用的，
     * 当场 {@code clearAllWidgets()} 会触发并发修改，因此推迟到本帧 {@link #updateScreen()}。
     */
    public void selectNode(String nodeId)
    {
        this.pendingSelectedNodeId = highlight.isSelected(nodeId) ? "" : (nodeId == null ? "" : nodeId);
        this.hasPendingSelection = true;
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if (hasPendingSelection)
        {
            hasPendingSelection = false;
            this.highlight = computeHighlight(layoutTree, pendingSelectedNodeId);
            rebuildGraph(false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        // 点在空白画布（未被任何节点消费）→ 取消选中。返回值原样透传，不影响拖拽平移。
        if (!handled && button == 0 && highlight.isActive())
        {
            selectNode(highlight.selectedNodeId());
        }
        return handled;
    }

    /**
     * 依据可见树算出「选中节点 + 它的全部前置 / 后继 + 相应连线」。
     * <p>
     * 说明节点不参与连线（{@link SkillTreeNodeDef#isConnectableEdge}），因此也不参与连通关系：
     * 选中说明节点时只高亮它自己，选中技能时也不会把说明节点拉进来。
     */
    private static SkillTreeHighlight computeHighlight(TechniqueSkillTree tree, String selectedNodeId)
    {
        if (tree == null || selectedNodeId == null || selectedNodeId.isBlank())
        {
            return SkillTreeHighlight.NONE;
        }

        SkillTreeNodeDef selected = tree.findNode(selectedNodeId).orElse(null);
        boolean selectedIsInfoNode = selected != null && selected.isInfoNode();

        Set<String> nodes = new LinkedHashSet<>();
        Set<String> edges = new LinkedHashSet<>();
        nodes.add(selectedNodeId);

        for (SkillTreeNodeDef node : tree.nodes())
        {
            if (node.isInfoNode())
            {
                continue;
            }
            if (node.nodeId().equals(selectedNodeId))
            {
                // 选中节点的前置：连线 前置 → 选中
                for (String prereqId : node.prerequisiteNodeIds())
                {
                    if (!SkillTreeNodeDef.isConnectableEdge(tree.findNode(prereqId).orElse(null), node))
                    {
                        continue;
                    }
                    nodes.add(prereqId);
                    edges.add(SkillTreeHighlight.edgeKey(prereqId, node.nodeId()));
                }
            }
            if (!selectedIsInfoNode && node.prerequisiteNodeIds().contains(selectedNodeId))
            {
                // 选中节点的后继：连线 选中 → 后继
                nodes.add(node.nodeId());
                edges.add(SkillTreeHighlight.edgeKey(selectedNodeId, node.nodeId()));
            }
        }

        return new SkillTreeHighlight(selectedNodeId, Set.copyOf(nodes), Set.copyOf(edges));
    }

    @Override
    public void initWidget()
    {
        super.initWidget();
        rebuildGraph();
    }

    @Override
    public void onScreenSizeUpdate(int screenWidth, int screenHeight)
    {
        setSelfPosition(0, 0);
        setSize(new Size(screenWidth, screenHeight));
        super.onScreenSizeUpdate(screenWidth, screenHeight);
        rebuildGraph();
    }

    private static String resolveDefaultTreeId()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
        {
            return "";
        }
        return PlayerTechniqueData.getInnateTechnique(minecraft.player);
    }

    public static void refreshIfOpen()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer container))
        {
            return;
        }
        for (com.lowdragmc.lowdraglib.gui.widget.Widget widget : container.modularUI.getFlatWidgetCollection())
        {
            if (widget instanceof TechniqueDevelopmentCanvasWidget canvas)
            {
                canvas.rebuildGraph();
                return;
            }
        }
    }
}
