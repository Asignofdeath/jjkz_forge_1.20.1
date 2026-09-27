package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import net.minecraft.world.entity.player.Player;

/**
 * 统一「质量」池：SP 节点与 SE 共用一套<b>占位</b>账。
 * <p>
 * 语义是<b>权重和</b>，不是「节点个数」：
 * <pre>占用 = Σ(已点亮 SP 节点的 spCost) + Σ(已装载 SE 的 quality)</pre>
 * 三档行为：
 * <ul>
 *   <li>占用 ≤ 上限：正常。</li>
 *   <li>上限 &lt; 占用 ≤ 上限 + 宽限：允许装载，但按超出量吃「质量超载」罚。</li>
 *   <li>占用 &gt; 上限 + 宽限：拒绝装载。</li>
 * </ul>
 * ⚠️ 质量上限与宽限由咒术经验派生（{@code SpNodeShelfRules}）；超载罚在 {@link Config}。
 * ⚠️ SP 点数（购买力，可退）与质量（占位）是<b>两套账</b>：SE 不花 SP，只靠背包里的咒物占位。
 */
public final class PlayerQualityPoolData
{
    private PlayerQualityPoolData()
    {
    }

    /** 质量上限（由咒术经验派生）。 */
    public static int cap(Player player)
    {
        return PlayerSpNodeData.getQuality(player);
    }

    /**
     * 宽限：上限之外还允许多占多少（超出部分才吃超载罚）。
     * 由咒术经验派生；{@link Config#seQualityAllowance} 仅作旧配置兜底，不再单独使用。
     */
    public static int allowance(Player player)
    {
        return PlayerSpNodeData.getAllowance(player);
    }

    /** @deprecated 用 {@link #allowance(Player)}。 */
    @Deprecated
    public static int allowance()
    {
        return Math.max(0, Config.seQualityAllowance);
    }

    /** 当前总占用。 */
    public static int occupied(Player player)
    {
        return occupiedBySpNodes(player) + occupiedBySe(player);
    }

    public static int occupiedBySpNodes(Player player)
    {
        int sum = 0;
        for (SpNodeInstance node : PlayerSpNodeData.getLit(player))
        {
            sum += node.spCost();
        }
        return Math.max(0, sum);
    }

    public static int occupiedBySe(Player player)
    {
        int sum = 0;
        for (var def : PlayerSeLoadoutData.getAllEquipped(player))
        {
            sum += def.quality();
        }
        return Math.max(0, sum);
    }

    /** 超出<b>上限</b>的部分（宽限内的超出也算，用于计算超载罚）。 */
    public static int overload(Player player)
    {
        return Math.max(0, occupied(player) - cap(player));
    }

    /** 还能占多少（不含宽限）。 */
    public static int remaining(Player player)
    {
        return Math.max(0, cap(player) - occupied(player));
    }

    /**
     * 再装入 {@code additional} 点质量，是否越过「上限 + 宽限」的硬顶。
     */
    public static boolean wouldExceedHardLimit(Player player, int additional)
    {
        if (additional <= 0)
        {
            return false;
        }
        return occupied(player) + additional > cap(player) + allowance(player);
    }
}
