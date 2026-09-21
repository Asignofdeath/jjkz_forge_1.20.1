package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.DomainExpansionServerLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 领域展开路由器 - 根据玩家术式类型路由到对应的领域实现。
 * <p>
 * 统一管理所有领域展开实例的查询与遍历，避免调用方需要知道具体领域类型。
 * <p>
 * 当前注册的领域：
 * <ul>
 *   <li>{@link DomainExpansionServerLogic} - 基础领域（无术式时）</li>
 *   <li>{@link LimitlessDomainExpansion} - 无下限术式领域「无量空处」</li>
 *   <li>{@link ShrineDomainExpansion} - 御厨子术式领域「伏魔神龛」</li>
 *   <li>{@link JudgmentDomainExpansion} - 审判术式领域「诛伏赐死」</li>
 * </ul>
 */
public final class DomainExpansionRouter
{
    private DomainExpansionRouter()
    {
    }

    /** 所有已注册的领域实例 */
    private static final List<AbstractDomainExpansion> ALL_DOMAINS = new ArrayList<>();

    static
    {
        ALL_DOMAINS.add(DomainExpansionServerLogic.getInstance());
        ALL_DOMAINS.add(LimitlessDomainExpansion.getInstance());
        ALL_DOMAINS.add(ShrineDomainExpansion.getInstance());
        ALL_DOMAINS.add(JudgmentDomainExpansion.getInstance());
    }

    /**
     * 根据玩家术式类型获取对应的领域实现。
     * <p>
     * 如果玩家有术式专属领域且满足条件，返回术式领域；
     * 否则回退到基础领域。
     *
     * @param player 玩家
     * @return 对应的领域实现
     */
    public static AbstractDomainExpansion getDomainForPlayer(ServerPlayer player)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            if (domain instanceof AbstractTechniqueDomain techniqueDomain)
            {
                if (techniqueDomain.hasLearnedTechnique(player))
                {
                    return domain;
                }
            }
        }
        return DomainExpansionServerLogic.getInstance();
    }

    /**
     * 检查指定位置是否在任何活跃领域范围内（遍历所有领域实例）。
     *
     * @param level 服务端世界
     * @param pos   方块位置
     * @return 是否在领域范围内
     */
    public static boolean isPositionInAnyDomain(ServerLevel level, BlockPos pos)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            if (domain.isPositionInBarrier(level, pos))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查指定位置是否在启用了"阻止生物生成"的领域范围内（遍历所有领域实例）。
     *
     * @param level 服务端世界
     * @param pos   方块位置
     * @return 是否在阻止生物生成的领域范围内
     */
    public static boolean isPositionBlockingMobSpawn(ServerLevel level, BlockPos pos)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            if (domain.isPositionBlockingMobSpawn(level, pos))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 对所有领域实例执行 tickMaintain。
     *
     * @param player 玩家
     */
    public static void tickMaintainAll(ServerPlayer player)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            domain.tickMaintain(player);
        }
    }

    /**
     * 对所有领域实例尝试修补结界空缺（仅持有活跃领域的实例会实际执行）。
     *
     * @param player 玩家
     */
    public static void repairBarrierGapsAll(ServerPlayer player)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            domain.repairBarrierGaps(player);
        }
    }

    /**
     * 对所有领域实例应用结界规则变更（立即生效项）。
     *
     * @param player 玩家
     */
    public static void updateBarrierRulesAll(ServerPlayer player)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            domain.updateBarrierRules(player);
        }
    }
}
