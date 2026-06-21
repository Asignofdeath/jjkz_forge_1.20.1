package com.darkness.jujutsukaisenzone.technique.barrier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 结界术接口 - 定义结界术的核心契约。
 * <p>
 * 所有结界术类型（普通结界、领域展开等）都应实现此接口。
 * 接口定义了结界术的对外行为，不关心具体实现细节。
 * <p>
 * 主要能力：
 * - 施放结界（展开）
 * - 维持结界（每tick消耗咒力、检测生命值）
 * - 关闭结界（崩解/主动关闭）
 * - 查询结界状态（位置检测、规则查询）
 */
public interface IBarrierTechnique
{
    /**
     * 施放结界术。
     * <p>
     * 业务流程：
     * 1. 检查施放条件（习得等级、战斗模式等）
     * 2. 检查并消耗咒力
     * 3. 构建结界形状（球壳位置）
     * 4. 放置结界方块
     * 5. 记录结界实例到活跃列表
     * <p>
     * 如果玩家已在结界内，则关闭结界。
     *
     * @param player 施术者
     */
    void cast(ServerPlayer player);

    /**
     * 每 tick 维持结界。
     * <p>
     * 业务流程：
     * 1. 消耗维持咒力（根据半径和等级）
     * 2. 检测结界生命值（剩余方块百分比）
     * 3. 处理延迟展开动画（大半径结界）
     * 4. 崩解判定（咒力不足或生命值过低）
     *
     * @param player 施术者
     */
    void tickMaintain(ServerPlayer player);

    /**
     * 检查指定位置是否在任何活跃结界范围内。
     *
     * @param level 世界
     * @param pos   检查的位置
     * @return 是否在结界内
     */
    boolean isPositionInBarrier(ServerLevel level, BlockPos pos);

    /**
     * 检查指定位置是否在阻止生物生成的结界范围内。
     *
     * @param level 世界
     * @param pos   检查的位置
     * @return 是否在阻止生物生成的结界内
     */
    boolean isPositionBlockingMobSpawn(ServerLevel level, BlockPos pos);

    /**
     * 更新玩家当前结界的规则。
     * <p>
     * 如果玩家在自己的结界中，规则立即生效。
     *
     * @param player 玩家
     */
    void updateBarrierRules(ServerPlayer player);

    /**
     * 修补结界空缺。
     * <p>
     * 使用反转术式修补结界的破损处。
     * 条件：习得反转术式 + 处于自己的结界内。
     *
     * @param player 施术者
     */
    void repairBarrierGaps(ServerPlayer player);

    /**
     * 调整结界半径档位。
     *
     * @param player 玩家
     * @param delta  档位变化量（+1 或 -1）
     * @return 调整后的档位
     */
    int adjustBarrierRadiusTier(ServerPlayer player, int delta);

    /**
     * 获取结界术类型标识。
     * <p>
     * 用于区分不同结界术类型（普通结界、领域展开等）。
     *
     * @return 类型标识（如 "barrier_technique", "domain_expansion"）
     */
    String getTechniqueType();

    /**
     * 检查玩家是否可以施放此结界术。
     * <p>
     * 检查条件：
     * - 是否习得
     * - 是否在战斗模式
     * - 是否满足其他前置条件
     *
     * @param player 玩家
     * @return 是否可以施放
     */
    boolean canCast(ServerPlayer player);
}
