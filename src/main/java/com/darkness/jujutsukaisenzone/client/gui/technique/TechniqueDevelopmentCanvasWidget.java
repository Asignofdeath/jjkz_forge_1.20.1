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
import java.util.Map;
import java.util.Optional;

/**
 * 术式开发全屏画布：渲染当前术式技能树的节点与连线。
 */
@OnlyIn(Dist.CLIENT)
public final class TechniqueDevelopmentCanvasWidget extends FreeGraphView
{
    private String treeId = "";

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
        clearAllWidgets();

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
        TechniqueSkillTree layoutTree = layoutBuilder.build();
        Map<String, int[]> layout = SkillTreeLayoutEngine.layout(layoutTree);

        Map<String, SkillTreeNodeWidget> nodeWidgets = new LinkedHashMap<>();
        for (SkillTreeNodeDef nodeDef : visibleNodes)
        {
            SkillTreeNodeWidget widget = new SkillTreeNodeWidget(treeId, nodeDef);
            int[] pos = layout.getOrDefault(nodeDef.nodeId(), new int[] { 0, 0 });
            widget.setSelfPosition(pos[0], pos[1]);
            nodeWidgets.put(nodeDef.nodeId(), widget);
        }

        addWidget(new SkillTreeConnectionsWidget(this, layoutTree, nodeWidgets));
        for (SkillTreeNodeWidget widget : nodeWidgets.values())
        {
            addWidget(widget);
        }

        resetFitScaleByWidgets();
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
