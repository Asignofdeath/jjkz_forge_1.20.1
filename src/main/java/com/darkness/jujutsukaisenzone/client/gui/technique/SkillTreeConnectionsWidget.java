package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.lowdragmc.lowdraglib.gui.widget.FreeGraphView;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeLayoutEngine;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

/**
 * 绘制技能树节点之间的前置连线（轴对齐折线）。
 * <p>
 * 相邻列：单弯折 L 形线，垂直段在列间隙中心。
 * 跨列（depth 差 ≥ 2）：双弯折 └─┐ 形线，横向主干避开中间列节点。
 */
@OnlyIn(Dist.CLIENT)
public final class SkillTreeConnectionsWidget extends com.lowdragmc.lowdraglib.gui.widget.Widget
{
    private static final int LINE_COLOR = 0xFFBBBBBB;
    private static final float SCREEN_LINE_WIDTH = 3.0F;

    private final FreeGraphView graphView;
    private final TechniqueSkillTree tree;
    private final Map<String, SkillTreeNodeWidget> nodeWidgets;

    public SkillTreeConnectionsWidget(FreeGraphView graphView, TechniqueSkillTree tree, Map<String, SkillTreeNodeWidget> nodeWidgets)
    {
        super(new Position(0, 0), new Size(1, 1));
        this.graphView = graphView;
        this.tree = tree;
        this.nodeWidgets = nodeWidgets;
        setClientSideWidget();
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        float scale = Math.max(0.05F, graphView.getScale());
        float lineWidth = SCREEN_LINE_WIDTH / scale;

        for (SkillTreeNodeDef node : tree.nodes())
        {
            SkillTreeNodeWidget targetWidget = nodeWidgets.get(node.nodeId());
            if (targetWidget == null)
            {
                continue;
            }

            for (String prereqId : node.prerequisiteNodeIds())
            {
                SkillTreeNodeWidget sourceWidget = nodeWidgets.get(prereqId);
                if (sourceWidget == null)
                {
                    continue;
                }
                drawConnection(graphics, sourceWidget, targetWidget, lineWidth);
            }
        }
    }

    private void drawConnection(
            GuiGraphics graphics,
            SkillTreeNodeWidget from,
            SkillTreeNodeWidget to,
            float lineWidth)
    {
        float[] start = edgeAnchor(from, to);
        float[] end = edgeAnchor(to, from);
        float x1 = start[0];
        float y1 = start[1];
        float x2 = end[0];
        float y2 = end[1];

        if (Math.abs(x1 - x2) < 0.5F)
        {
            drawVerticalSegment(graphics, x1, y1, y2, lineWidth, LINE_COLOR);
            return;
        }
        if (Math.abs(y1 - y2) < 0.5F)
        {
            drawHorizontalSegment(graphics, x1, x2, y1, lineWidth, LINE_COLOR);
            return;
        }

        int sourceDepth = columnDepth(Math.min(x1, x2));
        int targetDepth = columnDepth(Math.max(x1, x2));
        int skip = targetDepth - sourceDepth;

        if (skip == 1)
        {
            // 相邻列：单弯折 L 形，垂直段在列间隙中
            float bendX = columnGapCenter(sourceDepth);
            drawHorizontalSegment(graphics, x1, bendX, y1, lineWidth, LINE_COLOR);
            drawVerticalSegment(graphics, bendX, y1, y2, lineWidth, LINE_COLOR);
            drawHorizontalSegment(graphics, bendX, x2, y2, lineWidth, LINE_COLOR);
        }
        else
        {
            // 跨列：双弯折 └─┐ 形，横向主干避开中间列节点
            float bendX1 = columnGapCenter(sourceDepth);
            float bendX2 = columnGapCenter(targetDepth - 1);
            float yTrunk = findSafeHorizontalY(sourceDepth, targetDepth, y1, y2);

            drawHorizontalSegment(graphics, x1, bendX1, y1, lineWidth, LINE_COLOR);
            drawVerticalSegment(graphics, bendX1, y1, yTrunk, lineWidth, LINE_COLOR);
            drawHorizontalSegment(graphics, bendX1, bendX2, yTrunk, lineWidth, LINE_COLOR);
            drawVerticalSegment(graphics, bendX2, yTrunk, y2, lineWidth, LINE_COLOR);
            drawHorizontalSegment(graphics, bendX2, x2, y2, lineWidth, LINE_COLOR);
        }
    }

    /** 找到横向主干的安全 y 坐标：避开中间列所有节点的 y 范围。 */
    private float findSafeHorizontalY(int sourceDepth, int targetDepth, float y1, float y2)
    {
        float top = Float.MAX_VALUE;
        float bottom = Float.MIN_VALUE;

        for (SkillTreeNodeWidget w : nodeWidgets.values())
        {
            int d = columnDepth(w.getSelfPosition().x);
            if (d > sourceDepth && d < targetDepth)
            {
                float wTop = w.getSelfPosition().y;
                float wBottom = wTop + SkillTreeNodeWidget.HEIGHT;
                top = Math.min(top, wTop);
                bottom = Math.max(bottom, wBottom);
            }
        }

        if (top == Float.MAX_VALUE)
        {
            return (y1 + y2) * 0.5F;
        }

        // 源→目标的 y 区间不与中间节点相交 → 无需绕路
        float lineTop = Math.min(y1, y2);
        float lineBot = Math.max(y1, y2);
        if (lineBot <= top + 2F || lineTop >= bottom - 2F)
        {
            return (y1 + y2) * 0.5F;
        }

        // 上面 or 下面，选离 (y1+y2)/2 更近的
        float above = top - 12F;
        float below = bottom + 12F;
        float midY = (y1 + y2) * 0.5F;
        return Math.abs(midY - above) < Math.abs(midY - below) ? above : below;
    }

    /** 节点 x 坐标 → 列深度。 */
    private static int columnDepth(float x)
    {
        return (int)(Math.floor(x / SkillTreeLayoutEngine.HORIZONTAL_SPACING));
    }

    /** 指定列的右侧间隙中心 x。 */
    private static float columnGapCenter(int depth)
    {
        float col = depth * SkillTreeLayoutEngine.HORIZONTAL_SPACING;
        return col + SkillTreeNodeWidget.WIDTH + (SkillTreeLayoutEngine.HORIZONTAL_SPACING - SkillTreeNodeWidget.WIDTH) * 0.5F;
    }

    /** 从节点中心指向目标节点的矩形边缘交点。 */
    private static float[] edgeAnchor(SkillTreeNodeWidget node, SkillTreeNodeWidget toward)
    {
        float cx = nodeCenterX(node);
        float cy = nodeCenterY(node);
        float dx = nodeCenterX(toward) - cx;
        float dy = nodeCenterY(toward) - cy;
        if (Math.abs(dx) < 0.001F && Math.abs(dy) < 0.001F)
        {
            return new float[] { cx, cy };
        }

        float halfW = SkillTreeNodeWidget.WIDTH * 0.5F;
        float halfH = SkillTreeNodeWidget.HEIGHT * 0.5F;
        float scale = Math.max(Math.abs(dx) / halfW, Math.abs(dy) / halfH);
        return new float[] { cx + dx / scale, cy + dy / scale };
    }

    private static float nodeCenterX(SkillTreeNodeWidget node)
    {
        return node.getSelfPosition().x + SkillTreeNodeWidget.WIDTH * 0.5F;
    }

    private static float nodeCenterY(SkillTreeNodeWidget node)
    {
        return node.getSelfPosition().y + SkillTreeNodeWidget.HEIGHT * 0.5F;
    }

    private static void drawHorizontalSegment(GuiGraphics graphics, float x1, float x2, float y, float thickness, int color)
    {
        float half = thickness * 0.5F;
        fillRect(graphics, Math.min(x1, x2), y - half, Math.max(x1, x2), y + half, color);
    }

    private static void drawVerticalSegment(GuiGraphics graphics, float x, float y1, float y2, float thickness, int color)
    {
        float half = thickness * 0.5F;
        fillRect(graphics, x - half, Math.min(y1, y2), x + half, Math.max(y1, y2), color);
    }

    private static void fillRect(GuiGraphics graphics, float left, float top, float right, float bottom, int color)
    {
        int l = (int) Math.floor(Math.min(left, right));
        int r = (int) Math.ceil(Math.max(left, right));
        int t = (int) Math.floor(Math.min(top, bottom));
        int b = (int) Math.ceil(Math.max(top, bottom));
        if (r <= l)
        {
            r = l + 1;
        }
        if (b <= t)
        {
            b = t + 1;
        }
        graphics.fill(l, t, r, b, color);
    }
}
