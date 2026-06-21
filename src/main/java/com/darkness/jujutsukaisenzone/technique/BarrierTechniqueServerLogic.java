package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.player.BarrierRulesData;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.registry.ModBlocks;
import com.darkness.jujutsukaisenzone.technique.barrier.AbstractBarrierTechnique;
import com.darkness.jujutsukaisenzone.technique.barrier.ExpansionAnimationType;
import com.darkness.jujutsukaisenzone.technique.TechniqueDailyUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 普通结界术服务端逻辑。
 * <p>
 * 继承自 {@link AbstractBarrierTechnique}，实现普通结界术的具体逻辑。
 * <p>
 * 特点：
 * - 从下向上展开动画
 * - 根据结界术等级减免维持消耗
 */
public final class BarrierTechniqueServerLogic extends AbstractBarrierTechnique
{
    // ==================== 单例实例（向后兼容） ====================

    /** 单例实例，用于静态方法委托。 */
    private static final BarrierTechniqueServerLogic INSTANCE = new BarrierTechniqueServerLogic();

    /**
     * 获取单例实例。
     * @return 单例实例
     */
    public static BarrierTechniqueServerLogic getInstance()
    {
        return INSTANCE;
    }

    // ==================== 静态委托方法（向后兼容） ====================

    /** 静态委托方法，保持向后兼容。 */
    public static void staticCast(ServerPlayer player) { INSTANCE.cast(player); }

    /** 静态委托方法，保持向后兼容。 */
    public static void staticTickMaintain(ServerPlayer player) { INSTANCE.tickMaintain(player); }

    /** 静态委托方法，保持向后兼容。 */
    public static boolean staticIsPositionBlockingMobSpawn(ServerLevel level, BlockPos pos) { return INSTANCE.isPositionBlockingMobSpawn(level, pos); }

    /** 静态委托方法，保持向后兼容。 */
    public static int staticAdjustBarrierRadiusTier(ServerPlayer player, int delta) { return INSTANCE.adjustBarrierRadiusTier(player, delta); }

    /** 静态委托方法，保持向后兼容。 */
    public static void staticRepairBarrierGaps(ServerPlayer player) { INSTANCE.repairBarrierGaps(player); }

    /** 静态委托方法，保持向后兼容。 */
    public static void staticUpdateBarrierRules(ServerPlayer player) { INSTANCE.updateBarrierRules(player); }

    // ==================== 配置参数 ====================

    /** 可用结界半径档位，index 即 tier（0→8, 1→16, 2→32, 3→64）。 */
    private static final int[] RADIUS_VALUES = {8, 16, 32, 64};
    private static final int DEFAULT_RADIUS_TIER = 0;
    private static final int MIN_RADIUS_TIER = 0;
    private static final int MAX_RADIUS_TIER = RADIUS_VALUES.length - 1;
    /** 结界同时存在数量上限；超出时移除最早创建的结界。 */
    private static final int MAX_ACTIVE_BARRIER_COUNT = 1;
    /** 展开结界消耗的咒力（按半径档位：tier 0→8格消耗60，tier 1→16格消耗70，tier 2→32格消耗80，tier 3→64格消耗100） */
    private static final int[] EXPANSION_COSTS = {60, 70, 80, 100};
    /** 维持结界每秒消耗的咒力基数（与半径成正比） */
    private static final double MAINTAIN_COST_BASE_PER_SECOND = 1.0;
    /** 维持结界每秒消耗的咒力系数（半径每增加1格，每秒额外消耗此系数） */
    private static final double MAINTAIN_COST_PER_RADIUS_PER_SECOND = 0.5;

    // ==================== 状态管理 ====================

    /** 活跃结界：按施术者 UUID 记录队列，支持"超上限淘汰最早结界"。 */
    private static final ConcurrentMap<UUID, Deque<BarrierInstance>> ACTIVE_BARRIERS = new ConcurrentHashMap<>();
    /** 玩家结界半径档位（tier 0~3）。 */
    private static final ConcurrentMap<UUID, Integer> BARRIER_RADIUS_TIER = new ConcurrentHashMap<>();

    // ==================== 实现抽象方法 ====================

    @Override
    protected int[] getRadiusValues()
    {
        return RADIUS_VALUES;
    }

    @Override
    protected int getDefaultRadiusTier()
    {
        return DEFAULT_RADIUS_TIER;
    }

    @Override
    protected int getMaxActiveBarrierCount()
    {
        return MAX_ACTIVE_BARRIER_COUNT;
    }

    @Override
    protected double getMaintainCostBasePerSecond()
    {
        return MAINTAIN_COST_BASE_PER_SECOND;
    }

    @Override
    protected double getMaintainCostPerRadiusPerSecond()
    {
        return MAINTAIN_COST_PER_RADIUS_PER_SECOND;
    }

    @Override
    protected ExpansionAnimationType getExpansionAnimationType()
    {
        return ExpansionAnimationType.BOTTOM_UP;  // 普通结界：从下向上展开
    }

    @Override
    public boolean canCast(ServerPlayer player)
    {
        return isInBattleMode(player) && PlayerBarrierTechniqueData.hasLearned(player);
    }

    @Override
    protected int calculateExpansionCost(int tier)
    {
        int clampedTier = Math.max(0, Math.min(tier, EXPANSION_COSTS.length - 1));
        return EXPANSION_COSTS[clampedTier];
    }

    @Override
    protected BlockState getBarrierBlockState()
    {
        return ModBlocks.BARRIER_BLOCK.get().defaultBlockState();
    }

    @Override
    protected double getMaintainCostReduction(ServerPlayer player)
    {
        return PlayerBarrierTechniqueData.getMaintainCostReduction(player);
    }

    @Override
    public String getTechniqueType()
    {
        return "barrier_technique";
    }

    // ==================== 实现接口方法 ====================

    // ==================== 接口方法实现 ====================

    @Override
    public void cast(ServerPlayer player)
    {
        if (!player.isAlive() || !isInBattleMode(player))
        {
            return;
        }
        if (closeBarrierIfPlayerInside(player))
        {
            return;
        }

        // 检查玩家是否在任何领域内（含术式领域如无量空处），领域内无法展开结界
        if (DomainExpansionRouter.isPositionInAnyDomain(player.serverLevel(), player.blockPosition()))
        {
            player.displayClientMessage(Component.literal("领域内无法展开结界").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 检查玩家是否习得结界术
        if (!PlayerBarrierTechniqueData.hasLearned(player))
        {
            player.displayClientMessage(Component.literal("尚未习得结界术").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 获取玩家当前选择的半径档位
        int tier = BARRIER_RADIUS_TIER.getOrDefault(player.getUUID(), DEFAULT_RADIUS_TIER);

        // 检查并消耗展开结界所需的咒力
        int expansionCost = calculateExpansionCost(tier);
        if (!PlayerCursedEnergyData.tryConsume(player, expansionCost))
        {
            player.displayClientMessage(Component.literal("咒力不足，无法展开结界（需要 " + expansionCost + "）").withStyle(ChatFormatting.RED), true);
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int radius = getRadius(tier);
        Map<BlockPos, BlockState> removedBlocks = new HashMap<>();

        // 计算球壳所有位置（但暂不放置）
        List<BlockPos> shellBlocks = buildHollowSpherePositions(center, radius);
        if (shellBlocks.isEmpty())
        {
            return;
        }

        // 判断是否需要延迟展开动画
        boolean animatedExpand = radius > ANIMATED_EXPAND_THRESHOLD;

        // 排序（实现展开动画）
        shellBlocks = sortShellBlocksForExpansion(shellBlocks, center, player);

        // 读取玩家的规则设置
        boolean blockMobSpawn = BarrierRulesData.isBlockMobSpawn(player.getUUID());

        Deque<BarrierInstance> queue = ACTIVE_BARRIERS.computeIfAbsent(player.getUUID(), key -> new ArrayDeque<>());

        BarrierInstance instance = new BarrierInstance(
                level.dimension(),
                shellBlocks,
                removedBlocks,
                center,
                radius,
                blockMobSpawn,
                10,
                animatedExpand ? 0 : shellBlocks.size(),  // expansionProgress
                animatedExpand,                              // isExpanding
                0,                                          // pendingBatchIndex
                expansionCost                               // expansionCost
        );

        queue.addLast(instance);

        while (queue.size() > MAX_ACTIVE_BARRIER_COUNT)
        {
            BarrierInstance oldest = queue.pollFirst();
            if (oldest != null)
            {
                clearBarrierInstance(player, oldest);
            }
        }

        if (queue.isEmpty())
        {
            ACTIVE_BARRIERS.remove(player.getUUID());
        }

        // 如果是常规结界（即时展开），立即放置所有方块
        if (!animatedExpand)
        {
            placeAllBarrierBlocks(level, shellBlocks, removedBlocks, center, radius);
            player.displayClientMessage(Component.literal("结界已展开（半径 " + radius + "）").withStyle(ChatFormatting.GREEN), true);
        }
        else
        {
            player.displayClientMessage(Component.literal("结界展开中（半径 " + radius + "）").withStyle(ChatFormatting.AQUA), true);
        }
        TechniqueDailyUpgradeLogic.onBarrierTechniqueUsed(player);
    }

    @Override
    public void tickMaintain(ServerPlayer player)
    {
        Deque<BarrierInstance> queue = ACTIVE_BARRIERS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }
        Deque<BarrierInstance> kept = new ArrayDeque<>();
        boolean collapsedNotified = false;
        for (BarrierInstance instance : queue)
        {
            ServerLevel level = player.server.getLevel(instance.getDimension());
            if (level == null)
            {
                continue;
            }

            // 处理延迟展开动画
            if (instance.isExpanding())
            {
                tickBarrierExpand(player, level, instance);
                // 展开期间仍然需要追踪
                kept.addLast(instance);
                continue;
            }

            // 消耗维持结界所需的咒力（每秒消耗 base + radius * coefficient，并根据结界术等级减免）
            double costPerTick = (MAINTAIN_COST_BASE_PER_SECOND + instance.getRadius() * MAINTAIN_COST_PER_RADIUS_PER_SECOND) / TICKS_PER_SECOND;
            double reduction = getMaintainCostReduction(player);
            costPerTick = costPerTick * (1.0 - reduction);
            int costThisTick = (int) Math.ceil(costPerTick);
            if (costThisTick > 0)
            {
                if (!PlayerCursedEnergyData.tryConsume(player, costThisTick))
                {
                    // 咒力不足，结界自动崩解
                    if (!collapsedNotified)
                    {
                        player.displayClientMessage(Component.literal("咒力耗尽，结界崩解").withStyle(ChatFormatting.RED), true);
                        collapsedNotified = true;
                    }
                    clearBarrierInstance(player, instance);
                    continue;
                }
            }

            // 结界稳定后的生命值追踪
            double remainingPercent = calculateRemainingPercent(level, instance);
            if (remainingPercent < MIN_MAINTAIN_PERCENT)
            {
                if (!collapsedNotified && isPlayerInsideBarrier(player, instance))
                {
                    player.displayClientMessage(Component.literal("结界已崩解").withStyle(ChatFormatting.RED), true);
                    collapsedNotified = true;
                }
                clearBarrierInstance(player, instance);
                continue;
            }
            notifyBarrierHealthIfNeeded(player, instance, remainingPercent);
            kept.addLast(instance);
        }
        if (kept.isEmpty())
        {
            ACTIVE_BARRIERS.remove(player.getUUID());
            return;
        }
        ACTIVE_BARRIERS.put(player.getUUID(), kept);
    }

    @Override
    public boolean isPositionInBarrier(ServerLevel level, BlockPos pos)
    {
        return isPositionInAnyBarrier(level, pos);
    }

    @Override
    public boolean isPositionBlockingMobSpawn(ServerLevel level, BlockPos pos)
    {
        ResourceKey<Level> dimension = level.dimension();
        double px = pos.getX() + 0.5D;
        double py = pos.getY() + 0.5D;
        double pz = pos.getZ() + 0.5D;

        for (Deque<BarrierInstance> queue : ACTIVE_BARRIERS.values())
        {
            for (BarrierInstance instance : queue)
            {
                if (!instance.getDimension().equals(dimension))
                {
                    continue;
                }
                if (!instance.isBlockMobSpawn())
                {
                    continue;
                }
                if (instance.isExpanding())
                {
                    continue;
                }
                double cx = instance.getCenter().getX() + 0.5D;
                double cy = instance.getCenter().getY() + 0.5D;
                double cz = instance.getCenter().getZ() + 0.5D;
                double dx = px - cx;
                double dy = py - cy;
                double dz = pz - cz;
                double distanceSquared = dx * dx + dy * dy + dz * dz;
                double radiusSquared = instance.getRadius() * instance.getRadius();
                if (distanceSquared < radiusSquared)
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void updateBarrierRules(ServerPlayer player)
    {
        Deque<BarrierInstance> queue = ACTIVE_BARRIERS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }

        boolean newBlockMobSpawn = BarrierRulesData.isBlockMobSpawn(player.getUUID());

        for (BarrierInstance instance : queue)
        {
            if (isPlayerInsideBarrier(player, instance))
            {
                // 直接更新规则，无需重建结界
                instance.setBlockMobSpawn(newBlockMobSpawn);
                
                // 发送反馈消息
                String status = newBlockMobSpawn ? "已启用" : "已禁用";
                player.displayClientMessage(
                    Component.literal("阻止生物生成规则：" + status).withStyle(ChatFormatting.GREEN), 
                    true
                );
                return;
            }
        }
    }

    @Override
    public void repairBarrierGaps(ServerPlayer player)
    {
        // 检查反转术式
        if (!com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData.hasReverseTechnique(player))
        {
            return;
        }

        Deque<BarrierInstance> queue = ACTIVE_BARRIERS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        // 检查玩家是否在结界内
        if (!isPlayerInsideBarrier(player, instance))
        {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (level == null)
        {
            return;
        }

        // 消耗咒力
        if (!PlayerCursedEnergyData.tryConsume(player, REPAIR_COST_PER_TICK))
        {
            return;
        }

        // 修补结界方块和天空盒方块（优先修补结界方块）
        int repaired = repairBarrierAndSkybox(level, instance);

        if (repaired > 0)
        {
            // 生成修补粒子效果
            spawnRepairParticles(level, player, repaired);
        }
    }

    @Override
    public int adjustBarrierRadiusTier(ServerPlayer player, int delta)
    {
        int currentTier = BARRIER_RADIUS_TIER.getOrDefault(player.getUUID(), DEFAULT_RADIUS_TIER);
        int newTier = Math.max(MIN_RADIUS_TIER, Math.min(MAX_RADIUS_TIER, currentTier + delta));
        BARRIER_RADIUS_TIER.put(player.getUUID(), newTier);
        return newTier;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 处理大半径结界的延迟展开动画。
     * 每tick放置一批方块，从下往上逐层展开，实现包围式生成效果。
     */
    private void tickBarrierExpand(ServerPlayer player, ServerLevel level, BarrierInstance instance)
    {
        List<BlockPos> allBlocks = instance.getShellBlocks();
        int currentProgress = instance.getExpansionProgress();

        if (currentProgress >= allBlocks.size())
        {
            // 展开完成
            instance.setExpanding(false);
            instance.setExpansionProgress(allBlocks.size());
            return;
        }

        // 计算当前最低的Y层（展开起点）
        int currentBottomY = allBlocks.get(0).getY();
        // 计算当前已展开到的最高Y层
        int currentTopY = (currentProgress > 0) ? allBlocks.get(currentProgress - 1).getY() : currentBottomY - 1;

        // 每tick展开一层
        int targetY = currentTopY + 1;

        // 放置本tick的方块批次
        int placedThisTick = 0;
        int newProgress = currentProgress;

        while (newProgress < allBlocks.size() && placedThisTick < BLOCKS_PER_BATCH)
        {
            BlockPos pos = allBlocks.get(newProgress);

            // 只放置当前目标层及其以下（但不超过已展开层+1）的方块
            if (pos.getY() > targetY)
            {
                // 已经到了还没展开到的层，暂停
                break;
            }

            // 放置方块
            if (!level.getBlockState(pos).isAir())
            {
                rememberRemovedBlockForPos(level, pos, instance.getRemovedBlocks());
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
            }
            level.setBlock(pos, getBarrierBlockState(), BLOCK_UPDATE_FLAGS);

            placedThisTick++;
            newProgress++;
        }

        instance.setExpansionProgress(newProgress);

        // 计算并显示展开进度
        int totalBlocks = allBlocks.size();
        int placedBlocks = newProgress;
        int percent = (int) Math.round(100.0 * placedBlocks / totalBlocks);
        player.displayClientMessage(Component.literal("结界构建中: " + percent + "%").withStyle(ChatFormatting.AQUA), true);

        // 检查是否展开完成
        if (instance.getExpansionProgress() >= allBlocks.size())
        {
            instance.setExpanding(false);
            player.displayClientMessage(Component.literal("结界构建完成").withStyle(ChatFormatting.GREEN), true);
            // 清空内部空间（如果需要）
            clearSphereInteriorIfNeeded(player, level, instance);
        }
    }

    /**
     * 展开完成后，如果玩家位置高于球体，清空球体内部。
     */
    private void clearSphereInteriorIfNeeded(ServerPlayer player, ServerLevel level, BarrierInstance instance)
    {
        double distanceToFootBlock = getDistanceToFootBlockFromCenter(level, instance.getCenter());
        if (distanceToFootBlock >= instance.getRadius())
        {
            clearSphereInterior(level, instance.getCenter(), instance.getRadius(), instance.getRemovedBlocks());
        }
    }

    /**
     * 检查指定位置是否在任何活跃结界范围内。
     */
    private boolean isPositionInAnyBarrier(ServerLevel level, BlockPos pos)
    {
        ResourceKey<Level> dimension = level.dimension();
        double px = pos.getX() + 0.5D;
        double py = pos.getY() + 0.5D;
        double pz = pos.getZ() + 0.5D;

        for (Deque<BarrierInstance> queue : ACTIVE_BARRIERS.values())
        {
            for (BarrierInstance instance : queue)
            {
                if (!instance.getDimension().equals(dimension))
                {
                    continue;
                }
                if (instance.isExpanding())
                {
                    continue;
                }
                double cx = instance.getCenter().getX() + 0.5D;
                double cy = instance.getCenter().getY() + 0.5D;
                double cz = instance.getCenter().getZ() + 0.5D;
                double dx = px - cx;
                double dy = py - cy;
                double dz = pz - cz;
                double distanceSquared = dx * dx + dy * dy + dz * dz;
                double radiusSquared = instance.getRadius() * instance.getRadius();
                if (distanceSquared < radiusSquared)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 玩家位于自己结界内部时再次按下结界术，视为"关闭结界"。
     */
    private boolean closeBarrierIfPlayerInside(ServerPlayer player)
    {
        Deque<BarrierInstance> queue = ACTIVE_BARRIERS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return false;
        }
        Deque<BarrierInstance> kept = new ArrayDeque<>();
        boolean closed = false;
        for (BarrierInstance instance : queue)
        {
            if (!closed && isPlayerInsideBarrier(player, instance))
            {
                clearBarrierInstance(player, instance);
                closed = true;
                continue;
            }
            kept.addLast(instance);
        }
        if (kept.isEmpty())
        {
            ACTIVE_BARRIERS.remove(player.getUUID());
        }
        else
        {
            ACTIVE_BARRIERS.put(player.getUUID(), kept);
        }
        return closed;
    }

    /**
     * 结界生命值按 10% 阶段通知。
     */
    private void notifyBarrierHealthIfNeeded(ServerPlayer player, BarrierInstance instance, double remainingPercent)
    {
        int currentStep = Math.max(0, Math.min(10, (int) Math.floor(remainingPercent * 10.0D)));
        if (currentStep >= instance.getLastNotifiedTenPercentStep())
        {
            return;
        }
        if (isPlayerInsideBarrier(player, instance))
        {
            for (int step = instance.getLastNotifiedTenPercentStep() - 1; step >= currentStep; step--)
            {
                int percent = step * 10;
                Component message = Component.literal("结界完整度降至 " + percent + "%");
                if (percent == 70 || percent == 60 || percent == 50)
                {
                    message = message.copy().withStyle(ChatFormatting.RED);
                }
                player.displayClientMessage(message, true);
            }
        }
        instance.setLastNotifiedTenPercentStep(currentStep);
    }

    /**
     * 生成修补粒子效果。
     */
    private void spawnRepairParticles(ServerLevel level, ServerPlayer player, int count)
    {
        if (count <= 0) return;
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f(1.0f, 1.0f, 1.0f), 1.2f
        );
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.5;
        double z = player.getZ();
        level.sendParticles(dust, x, y, z, Math.min(count * 3, 30), 0.5, 0.5, 0.5, 0.02);
    }

}
