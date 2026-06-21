package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.lowdragmc.lowdraglib.gui.widget.FreeGraphView;
import com.lowdragmc.lowdraglib.utils.Size;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeLayoutEngine;
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
    public TechniqueDevelopmentCanvasWidget()
    {
        super(0, 0, 1, 1);
        setDrawGrid(true);
        setGridWidth(20);
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

    private void rebuildGraph()
    {
        clearAllWidgets();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
        {
            return;
        }

        String techniqueId = PlayerTechniqueData.getInnateTechnique(minecraft.player);
        Optional<TechniqueSkillTree> treeOpt = TechniqueSkillTreeRegistry.get(techniqueId);
        if (treeOpt.isEmpty())
        {
            return;
        }

        TechniqueSkillTree tree = treeOpt.get();
        Map<String, int[]> layout = SkillTreeLayoutEngine.layout(tree);

        Map<String, SkillTreeNodeWidget> nodeWidgets = new LinkedHashMap<>();
        for (SkillTreeNodeDef nodeDef : tree.nodes())
        {
            SkillTreeNodeWidget widget = new SkillTreeNodeWidget(techniqueId, nodeDef);
            int[] pos = layout.getOrDefault(nodeDef.nodeId(), new int[] { 0, 0 });
            widget.setSelfPosition(pos[0], pos[1]);
            nodeWidgets.put(nodeDef.nodeId(), widget);
        }

        addWidget(new SkillTreeConnectionsWidget(this, tree, nodeWidgets));
        for (SkillTreeNodeWidget widget : nodeWidgets.values())
        {
            addWidget(widget);
        }

        resetFitScaleByWidgets();
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
