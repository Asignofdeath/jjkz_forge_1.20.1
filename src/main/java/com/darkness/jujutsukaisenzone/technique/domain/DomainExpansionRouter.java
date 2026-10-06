package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.DomainExpansionServerLogic;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
 *   <li>{@link LoveRendezvousDomainExpansion} - 星间飞行术式领域「爱与幽会」</li>
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
        ALL_DOMAINS.add(LoveRendezvousDomainExpansion.getInstance());
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
     * 玩家是否作为主人持有任意活跃领域（含展开动画中）。
     * <p>
     * 用于领域量表等「领域维持期间」门控；遍历所有领域实现的活跃表。
     */
    public static boolean hasActiveDomain(ServerPlayer player)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            if (domain.hasActiveDomain(player))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 按 UUID 查询该施法者是否持有任意活跃领域（玩家 / AI 实体通用）。
     */
    public static boolean hasActiveDomain(UUID casterUuid)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            if (domain.hasActiveDomain(casterUuid))
            {
                return true;
            }
        }
        return false;
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
     * 对所有领域实例执行 tickMaintain（AI 实体施法者版）。
     * <p>
     * 由 {@code CursedEnergyEvents} 在遍历「已登记的 AI 领域施法者」时调用。
     * 与玩家路径共用同一套维持逻辑，只是咒力 / 战斗模式 / 模式来自
     * {@link DomainCaster} 的实体实现。
     *
     * @param caster AI 实体施法者
     */
    public static void tickMaintainAll(DomainCaster caster)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            domain.tickMaintain(caster);
        }
    }

    /**
     * 任一领域实现下，该施法者是否持有活跃领域（含展开动画 / 蓄力中）。
     */
    public static boolean hasAnyActiveDomain(DomainCaster caster)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            if (domain.hasActiveDomain(caster.uid()))
            {
                return true;
            }
        }
        return false;
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

    /**
     * 清空所有领域实例的会话级运行态（活跃领域队列、延迟天空盒倒计时等）。
     * <p>
     * 领域实现是进程级静态单例，退出世界 / 切换存档不会重新加载类，故必须在服务器停机时显式清理；
     * 否则残留的 {@code BarrierInstance} 会在新会话中以「幽灵领域」形式匹配同维度同坐标并继续生效，
     * 且方块快照永久驻留内存。由 {@code SessionStateCleanupEvents} 在服务器停机时调用。
     */
    public static void resetAllSessionState()
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            domain.resetSessionState();
        }
    }

    /**
     * 关闭该玩家在所有领域实现中的活跃领域（含回调与方块还原）。用于玩家登出。
     */
    public static void closeAllForPlayer(ServerPlayer player)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            domain.closeAllForPlayer(player);
        }
    }

    /**
     * 对仍残留在各领域实现活跃表中的实例做世界侧方块还原（服务器停机兜底）。
     */
    public static void restoreAllWorldAll(MinecraftServer server)
    {
        for (AbstractDomainExpansion domain : ALL_DOMAINS)
        {
            domain.restoreAllWorld(server);
        }
    }
}
