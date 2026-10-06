package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.entity.combat.AiTechniqueStats;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 领域施法者登记表 + 每 tick 驱动。
 * <p>
 * 领域的维持逻辑（{@link AbstractDomainExpansion#tickMaintain}）原本挂在
 * {@code CursedEnergyEvents#onPlayerTick} 里、逐个在线玩家驱动。AI 实体没有 PlayerTickEvent，
 * 所以这里登记「谁在用实体身份开领域」，再由同一处 driver 每 tick 遍历驱动。
 * <p>
 * <b>登记时机</b>：AI 实体真正展开领域成功后调用 {@link #track(LivingEntity)}；
 * 领域关闭 / 实体死亡 / 实体被移除时自动 {@link #untrack(UUID) 注销}。
 * <p>
 * <b>为什么按 UUID 而不是直接持有实体引用</b>：跨 tick 持有实体强引用会阻止区块卸载时的 GC，
 * 且实体被移除后引用悬空。UUID 查询走 {@code level.getEntity(uuid)}，代价可忽略。
 * <p>
 * <b>停机兜底</b>：本表是进程级静态容器，由 {@code SessionStateCleanupEvents} 在服务器停机时
 * 调用 {@link #clearAll()} 清空 —— 与领域 / 结界单例同一套会话清理契约。
 */
public final class AiDomainCasterRegistry
{
    /** 已登记的 AI 施法者 UUID（按维度分组，便于 O(1) 取层级）。 */
    private static final Map<UUID, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> TRACKED =
            new ConcurrentHashMap<>();

    /**
     * 上次执行 {@link #tickAll} 的 server tick。
     * <p>
     * {@code tickAll} 的调用方是 {@code CursedEnergyEvents#onPlayerTick}，而该事件<b>每个在线玩家各触发一次</b>，
     * 即同一 server tick 内 {@code tickAll} 会被调用 N 次（N = 在线玩家数）。驱动链上的
     * {@code tickCastingDelay}（蓄力倒计时，每次递减 1）与 {@code tickAiCasterPoseLock}（姿态锁倒计时）
     * 都是「每调用一次扣 1」，若不去重，多人联机时这些倒计时会被加速 N 倍。
     * <p>
     * 本字段是去重闸门：同一 server tick 只真正驱动一次。范式同
     * {@code LimitlessDomainExpansion#tickPostDomainLocksOncePerServerTick}。
     */
    private static int lastTickedServerTick = -1;

    private AiDomainCasterRegistry()
    {
    }

    /** 登记一个正在（或即将）维持领域的 AI 实体施法者。 */
    public static void track(LivingEntity entity)
    {
        if (entity == null || entity.level().isClientSide)
        {
            return;
        }
        TRACKED.put(entity.getUUID(), entity.level().dimension());
    }

    /** 注销；领域已关闭 / 实体消失时调用。 */
    public static void untrack(UUID uuid)
    {
        if (uuid != null)
        {
            TRACKED.remove(uuid);
        }
    }

    public static boolean isTracked(UUID uuid)
    {
        return uuid != null && TRACKED.containsKey(uuid);
    }

    /**
     * 每服务器 tick 驱动全部已登记的 AI 施法者。
     * <p>
     * <b>去重</b>：调用方每个在线玩家各触发一次，本方法用 {@link #lastTickedServerTick} 保证
     * 每 server tick 至多真正驱动一次（否则蓄力 / 姿态锁倒计时会被加速 N 倍）。
     * <p>
     * 对每个登记项：
     * <ol>
     *   <li>解析实体；实体已不存在 / 已死亡 → 立即注销（领域崩解交给丢弃前的最后一帧维持逻辑，
     *       残留方块由停机兜底或 {@code restoreAllWorld} 处理）；</li>
     *   <li>驱动 {@link DomainExpansionRouter#tickMaintainAll(DomainCaster)}；</li>
     *   <li>既不持有活跃领域、也不处于蓄力流程 → 注销（蓄力期必须留驻，见 {@code isCastingPending}）。</li>
     * </ol>
     *
     * @param server 服务器
     * @return 本次驱动过的施法者数量（调试 / 日志用；被去重跳过时返回 0）
     */
    public static int tickAll(MinecraftServer server)
    {
        if (TRACKED.isEmpty() || server == null)
        {
            return 0;
        }
        /*
         * 去重闸门：调用方每个在线玩家各触发一次 ⇒ 同一 server tick 内本方法会被调用 N 次。
         * 驱动链上的蓄力 / 姿态锁倒计时均「每调用一次扣 1」，不去重会被加速 N 倍（多人联机）。
         * 保证每 server tick 至多真正驱动一次。
         */
        int serverTick = server.getTickCount();
        if (serverTick == lastTickedServerTick)
        {
            return 0;
        }
        lastTickedServerTick = serverTick;
        List<UUID> stale = new ArrayList<>();
        int driven = 0;
        for (Map.Entry<UUID, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> entry : TRACKED.entrySet())
        {
            ServerLevel level = server.getLevel(entry.getValue());
            LivingEntity entity = level == null ? null : resolve(level, entry.getKey());
            if (entity == null || !entity.isAlive() || entity.isRemoved())
            {
                stale.add(entry.getKey());
                continue;
            }
            DomainCaster caster = DomainCaster.of(entity);
            DomainExpansionRouter.tickMaintainAll(caster);
            driven++;
            /*
             * 注销判据：既不持有活跃领域、也不处于蓄力流程 → 才算失效。
             * 无量空处是「先蓄力 60 刻、之后才入 ACTIVE_DOMAINS」，蓄力期间 hasActiveDomain 恒 false；
             * 若只据此注销，蓄力第 1 tick 就会把施法者踢出 TRACKED，tickMaintain 断供 ⇒ 领域永不展开。
             * isCastingPending 与 {@link #castLimitlessDomain} 的判据同源，两处必须一致。
             */
            if (!DomainExpansionRouter.hasActiveDomain(entity.getUUID())
                    && !LimitlessDomainExpansion.getInstance().isCastingPending(entity.getUUID()))
            {
                stale.add(entry.getKey());
            }
        }
        for (UUID uuid : stale)
        {
            TRACKED.remove(uuid);
        }
        return driven;
    }

    /**
     * 服务器停机 / 会话结束：清空登记表与去重闸门。
     * <p>
     * 只清本表，不负责方块还原 —— 方块还原由 {@link DomainExpansionRouter#restoreAllWorldAll} 统一处理，
     * 它以 {@code ACTIVE_DOMAINS} 为准，与这里是否登记无关。
     * <p>
     * {@link #lastTickedServerTick} 一并复位：本表是进程级静态容器，跨会话残留的 tick 值会让
     * 新会话首个 tick 误判为「本 tick 已驱动」而被跳过。
     */
    public static void clearAll()
    {
        TRACKED.clear();
        lastTickedServerTick = -1;
    }

    @Nullable
    private static LivingEntity resolve(ServerLevel level, UUID uuid)
    {
        Entity entity = level.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    /**
     * 便捷方法：让 AI 实体以「真实无量空处」流程开一次领域（含蓄力）。
     * <p>
     * 成功后自动 {@link #track}，使维持逻辑进入每 tick 驱动。
     *
     * @param entity 施法 AI 实体
     * @return 是否成功进入展开流程
     */
    public static boolean castLimitlessDomain(LivingEntity entity)
    {
        if (entity == null || entity.level().isClientSide || !entity.isAlive())
        {
            return false;
        }
        if (!AiTechniqueStats.has(entity))
        {
            return false;
        }
        if (DomainExpansionRouter.hasActiveDomain(entity.getUUID()))
        {
            return false;
        }
        LimitlessDomainExpansion.getInstance().cast(DomainCaster.of(entity));
        /*
         * 判据用 {@link #isTracked} 而非 {@link DomainExpansionRouter#hasActiveDomain}：
         * 无量空处是「先蓄力、60 刻后才真正入 ACTIVE_DOMAINS」，进入蓄力时 {@code cast} 内部已 track，
         * 此刻尚无活跃领域。若仍按 hasActiveDomain 判，会误报失败并漏掉 track（实为已成功）。
         * 这与 {@link #tickAll} 的注销判据同源：只要「持有活跃领域 or 处于蓄力中」就保持在册。
         */
        return isTracked(entity.getUUID());
    }

    /**
     * 便捷方法：撤除 AI 实体当前持有的全部领域（含 {@code onBarrierClosed} 回调与地形还原），并注销登记。
     * <p>
     * 供演出脚本收尾 / 强制中断使用。幂等：无活跃领域时静默返回。
     *
     * @param entity 施法 AI 实体
     * @return 是否确实撤除了至少一个领域
     */
    public static boolean closeDomain(LivingEntity entity)
    {
        if (entity == null)
        {
            return false;
        }
        boolean had = DomainExpansionRouter.hasActiveDomain(entity.getUUID());
        if (had)
        {
            LimitlessDomainExpansion.getInstance().closeAllForCaster(DomainCaster.of(entity));
        }
        untrack(entity.getUUID());
        return had;
    }
}
