package com.darkness.jujutsukaisenzone.technique.development;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 术式开发树自动分层布局引擎（Sugiyama 简化版）。
 * <p>
 * 输入 {@link TechniqueSkillTree}，输出 nodeId → 左上角坐标 (x, y) 的映射。
 * 算法：
 * <ol>
 *   <li>计算每个节点的深度（最长前置路径 + 1），根节点 depth=0</li>
 *   <li>按深度分层，层内用 barycenter 启发式排序以减少连线交叉</li>
 *   <li>按层分配 x 坐标，同层居中分配 y 坐标</li>
 * </ol>
 */
public final class SkillTreeLayoutEngine
{
    /** 列间距（相邻 depth 的节点列左边缘距离）。 */
    public static final int HORIZONTAL_SPACING = 200;
    /** 同行节点垂直间距。 */
    public static final int VERTICAL_SPACING = 140;

    private SkillTreeLayoutEngine()
    {
    }

    /**
     * 对技能树执行自动布局。
     *
     * @param tree 技能树定义
     * @return nodeId → 左上角坐标 (x, y)，保证调用顺序一致（LinkedHashMap）
     */
    public static Map<String, int[]> layout(TechniqueSkillTree tree)
    {
        List<SkillTreeNodeDef> nodes = new ArrayList<>(tree.nodes());
        if (nodes.isEmpty())
        {
            return Collections.emptyMap();
        }

        // 1. 计算深度
        Map<String, Integer> depths = computeDepths(tree, nodes);

        // 2. 按深度分组
        int maxDepth = 0;
        Map<Integer, List<SkillTreeNodeDef>> layers = new LinkedHashMap<>();
        for (SkillTreeNodeDef node : nodes)
        {
            int d = depths.getOrDefault(node.nodeId(), 0);
            layers.computeIfAbsent(d, k -> new ArrayList<>()).add(node);
            if (d > maxDepth)
            {
                maxDepth = d;
            }
        }

        // 3. 层内排序（barycenter）
        orderLayers(tree, layers, maxDepth);

        // 4. 分配坐标
        int maxLayerSize = 0;
        for (List<SkillTreeNodeDef> layer : layers.values())
        {
            if (layer.size() > maxLayerSize)
            {
                maxLayerSize = layer.size();
            }
        }

        Map<String, int[]> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<SkillTreeNodeDef>> entry : layers.entrySet())
        {
            int depth = entry.getKey();
            List<SkillTreeNodeDef> layer = entry.getValue();
            int x = depth * HORIZONTAL_SPACING;
            // 垂直居中：以最大层尺寸为基准
            int totalHeight = maxLayerSize * VERTICAL_SPACING;
            int layerHeight = layer.size() * VERTICAL_SPACING;
            int yOffset = (totalHeight - layerHeight) / 2;

            for (int i = 0; i < layer.size(); i++)
            {
                int y = yOffset + i * VERTICAL_SPACING;
                result.put(layer.get(i).nodeId(), new int[] { x, y });
            }
        }

        return result;
    }

    /**
     * 计算每个节点的深度 = max(前置节点深度) + 1；无前置的根节点深度 = 0。
     * 使用拓扑顺序避免重复计算，同时处理多路径取 max 的场景。
     */
    private static Map<String, Integer> computeDepths(TechniqueSkillTree tree, List<SkillTreeNodeDef> nodes)
    {
        Map<String, Integer> depths = new LinkedHashMap<>();

        // 初始化：所有节点深度为 0
        for (SkillTreeNodeDef node : nodes)
        {
            depths.put(node.nodeId(), 0);
        }

        // 多次迭代直到稳定（DAG 最长路径深度）
        boolean changed = true;
        while (changed)
        {
            changed = false;
            for (SkillTreeNodeDef node : nodes)
            {
                int maxPredDepth = -1;
                for (String prereqId : node.prerequisiteNodeIds())
                {
                    Integer predDepth = depths.get(prereqId);
                    if (predDepth != null && predDepth > maxPredDepth)
                    {
                        maxPredDepth = predDepth;
                    }
                }
                int newDepth = maxPredDepth + 1;
                if (newDepth > depths.get(node.nodeId()))
                {
                    depths.put(node.nodeId(), newDepth);
                    changed = true;
                }
            }
        }

        return depths;
    }

    /**
     * 对每个深度层的节点按 barycenter 排序：节点在当前层的"重心" = 其所有前置节点在上层中的索引平均值。
     * 深度 0 无需排序（无前置）。
     */
    private static void orderLayers(
            TechniqueSkillTree tree,
            Map<Integer, List<SkillTreeNodeDef>> layers,
            int maxDepth)
    {
        for (int d = 1; d <= maxDepth; d++)
        {
            List<SkillTreeNodeDef> layer = layers.get(d);
            if (layer == null || layer.size() <= 1)
            {
                continue;
            }

            List<SkillTreeNodeDef> prevLayer = layers.get(d - 1);
            if (prevLayer == null)
            {
                continue;
            }

            // 建立上层 nodeId → index 映射
            Map<String, Integer> prevIndex = new HashMap<>();
            for (int i = 0; i < prevLayer.size(); i++)
            {
                prevIndex.put(prevLayer.get(i).nodeId(), i);
            }

            // 计算每个节点的 barycenter
            Map<SkillTreeNodeDef, Double> barycenters = new LinkedHashMap<>();
            for (SkillTreeNodeDef node : layer)
            {
                double sum = 0;
                int count = 0;
                for (String prereqId : node.prerequisiteNodeIds())
                {
                    Integer idx = prevIndex.get(prereqId);
                    if (idx != null)
                    {
                        sum += idx;
                        count++;
                    }
                }
                barycenters.put(node, count > 0 ? sum / count : Double.MAX_VALUE);
            }

            // 按 barycenter 排序
            layer.sort(Comparator.comparingDouble(barycenters::get));
        }
    }
}
