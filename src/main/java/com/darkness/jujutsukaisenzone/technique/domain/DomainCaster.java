package com.darkness.jujutsukaisenzone.technique.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 领域施法者抽象：把「领域的坐标来源 / 预算来源」从 {@code ServerPlayer} 解耦出来，
 * 使 <b>AI 实体</b>也能作为领域的正主展开真实领域（含蓄力、白幕、必中、搬点、维持消耗、关域还原全流程）。
 * <p>
 * <b>两种实现</b>：
 * <ul>
 *   <li>{@link PlayerCaster} —— 玩家施法者。咒力 / 战斗模式 / 模式选择（常规·瞬发·开放式）/ 各类面板数据
 *       全部来自玩家存档（{@code PlayerCursedEnergyData} 等）。</li>
 *   <li>{@link EntityCaster} —— AI 实体施法者。咒力走内存池，战斗模式恒 true，模式恒「常规」，
 *       强度面板走 {@code AiTechniqueStats}。不写任何玩家存档。</li>
 * </ul>
 * <p>
 * <b>关键约束</b>：本抽象刻意<b>不</b>定义「如何消费咒力 / 如何逐 tick 扣费」，因为领域的
 * 消耗结算已经集中在 {@link AbstractDomainExpansion#tickMaintain} 一处，只需在那里对
 * {@code caster} 做一次类型分派即可。这里的 {@link #uid()} 是唯一的身份键，
 * 领域内部所有 {@code Map<UUID, ...>} 容器继续以它为主键，无需改动。
 * <p>
 * <b>不含玩家专属演出</b>：领域内的「输入锁」（{@code ModNetwork.sendPlayerInputLockSync}）、
 * 「脑损」「领域量表」等仅适用于玩家，由各实现通过 {@link #player()} 是否为空自行短路。
 */
public interface DomainCaster
{
    /** 身份键（玩家 = 玩家 UUID；AI 实体 = 实体 UUID）。领域内部所有容器的主键。 */
    java.util.UUID uid();

    /** 施法者所处服务端世界；不在服务端 / 实体已移除时返回 {@code null}。 */
    @Nullable ServerLevel level();

    /** 施法者当前世界维度键。 */
    ResourceKey<Level> dimension();

    /** 施法者脚底参考坐标（搬点锚点 / 球心水平基准用）。 */
    Vec3 position();

    /** 施法者脚下方块坐标（领域球心水平基准）。 */
    BlockPos blockPosition();

    /** 施法者那个方块层「地面」的 Y（领域地板层）。 */
    int footBlockY();

    /** 施法者是否存活。 */
    boolean alive();

    /** 是否处于「可展开领域」的战斗状态；AI 实体恒 true。 */
    boolean inBattleMode();

    /**
     * 消费咒力。返回 {@code false} 表示不足以支付（调用方据此中断 / 崩解）。
     * <p>
     * 玩家：写 {@code PlayerCursedEnergyData}；AI 实体：内存池。
     */
    boolean consumeCursedEnergy(int amount);

    /**
     * 当前剩余咒力（可不为 0；供 AI 内存池的维持逻辑与调试使用）。
     */
    int currentCursedEnergy();

    /** 领域消耗减免（玩家 = 结界术等级减免；AI = 0）。 */
    double maintainCostReduction();

    /**
     * 供「必须拿到玩家」的既有代码路径使用；AI 实体返回 {@code null}。
     * <p>
     * 仅用于<b>可空</b>分支（输入锁、脑损、领域量表、成就、提示文案等），
     * 不再是领域的必要前提。
     */
    @Nullable net.minecraft.server.level.ServerPlayer player();

    /**
     * 供「必须拿到实体」的 AI 专用分支使用；玩家施法者返回 {@code null}。
     * <p>
     * 与 {@link #player()} 互斥：二者恰有一个非空。
     */
    @Nullable net.minecraft.world.entity.LivingEntity entity();

    /** 当前 {@code MinecraftServer}；世界不可用时返回 {@code null}。 */
    @Nullable
    default net.minecraft.server.MinecraftServer server()
    {
        ServerLevel level = level();
        return level != null ? level.getServer() : null;
    }

    // ==================== 实现 ====================

    /** 玩家施法者：一切面板 / 存档数据都取自该 {@code ServerPlayer} 自己。 */
    record PlayerCaster(net.minecraft.server.level.ServerPlayer serverPlayer) implements DomainCaster
    {
        @Override
        public java.util.UUID uid()
        {
            return serverPlayer.getUUID();
        }

        @Override
        public ServerLevel level()
        {
            return serverPlayer.serverLevel();
        }

        @Override
        public ResourceKey<Level> dimension()
        {
            return serverPlayer.serverLevel().dimension();
        }

        @Override
        public Vec3 position()
        {
            return serverPlayer.position();
        }

        @Override
        public BlockPos blockPosition()
        {
            return serverPlayer.blockPosition();
        }

        @Override
        public int footBlockY()
        {
            return serverPlayer.getBlockY() - 1;
        }

        @Override
        public boolean alive()
        {
            return serverPlayer.isAlive();
        }

        @Override
        public boolean inBattleMode()
        {
            return serverPlayer.isAlive()
                    && com.darkness.jujutsukaisenzone.compat.EpicFightCompat.isBattleMode(serverPlayer);
        }

        @Override
        public boolean consumeCursedEnergy(int amount)
        {
            return com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData.tryConsume(serverPlayer, amount);
        }

        @Override
        public int currentCursedEnergy()
        {
            return com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData.getCurrent(serverPlayer);
        }

        @Override
        public double maintainCostReduction()
        {
            return 0.0D;
        }

        @Override
        public net.minecraft.server.level.ServerPlayer player()
        {
            return serverPlayer;
        }

        @Override
        @Nullable
        public net.minecraft.world.entity.LivingEntity entity()
        {
            return null;
        }
    }

    /**
     * AI 实体施法者：咒力走 {@code AiTechniqueStats} 内存池，战斗模式恒 true、模式恒「常规」。
     * <p>
     * 生命体征、位置全部实时读实体；实体被移除（{@link net.minecraft.world.entity.LivingEntity#isRemoved()}）
     * 时 {@link #alive()} 返回 false，交由 {@link AbstractDomainExpansion} 走崩解路径。
     */
    record EntityCaster(net.minecraft.world.entity.LivingEntity entity) implements DomainCaster
    {
        @Override
        public java.util.UUID uid()
        {
            return entity.getUUID();
        }

        @Override
        @Nullable
        public ServerLevel level()
        {
            return entity.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        }

        @Override
        public ResourceKey<Level> dimension()
        {
            return entity.level().dimension();
        }

        @Override
        public Vec3 position()
        {
            return entity.position();
        }

        @Override
        public BlockPos blockPosition()
        {
            return entity.blockPosition();
        }

        @Override
        public int footBlockY()
        {
            return entity.getBlockY() - 1;
        }

        @Override
        public boolean alive()
        {
            return entity.isAlive() && !entity.isRemoved();
        }

        @Override
        public boolean inBattleMode()
        {
            /* AI 术师无「战斗模式」概念，恒可展开。 */
            return true;
        }

        @Override
        public boolean consumeCursedEnergy(int amount)
        {
            return com.darkness.jujutsukaisenzone.entity.combat.AiTechniqueStats.tryConsume(entity, amount);
        }

        @Override
        public int currentCursedEnergy()
        {
            return com.darkness.jujutsukaisenzone.entity.combat.AiTechniqueStats.cursedEnergyCurrent(entity);
        }

        @Override
        public double maintainCostReduction()
        {
            return 0.0D;
        }

        @Override
        @Nullable
        public net.minecraft.server.level.ServerPlayer player()
        {
            return null;
        }

        @Override
        public net.minecraft.world.entity.LivingEntity entity()
        {
            return entity;
        }
    }

    /** 便捷构造：玩家施法者。 */
    static DomainCaster of(net.minecraft.server.level.ServerPlayer player)
    {
        return new PlayerCaster(player);
    }

    /** 便捷构造：AI 实体施法者。 */
    static DomainCaster of(net.minecraft.world.entity.LivingEntity entity)
    {
        return new EntityCaster(entity);
    }
}
