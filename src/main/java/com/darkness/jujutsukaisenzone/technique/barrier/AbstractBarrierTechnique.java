package com.darkness.jujutsukaisenzone.technique.barrier;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.compat.ShaderedCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 结界术抽象基类 - 提供结界术的共享实现。
 * <p>
 * 实现了 {@link IBarrierTechnique} 接口，定义了结界术的骨架实现。
 * 子类只需实现差异化逻辑（消耗计算、条件检查等）。
 * <p>
 * 采用模板方法模式：
 * - 基类定义结界术的标准流程
 * - 子类通过覆盖钩子方法实现特殊行为
 * <p>
 * 共享能力：
 * - 球壳位置计算
 * - 方块放置与移除
 * - 玩家位置检测
 * - 结界实例管理
 * - 延迟展开动画
 */
public abstract class AbstractBarrierTechnique implements IBarrierTechnique
{
    // ==================== 共享常量 ====================

    /** 常规方块更新标记，保证客户端可见并触发必要邻接更新。 */
    protected static final int BLOCK_UPDATE_FLAGS = 3;
    /** 壳层厚度判定阈值，数值越小壳越薄。 */
    protected static final double SHELL_THICKNESS_EPSILON = 0.5D;
    /** 大半径结界(>8)展开时每tick扩展的层数 */
    protected static final int EXPAND_LAYERS_PER_TICK = 1;
    /** 大半径结界(>8)展开时每层最多放置的方块数（分批放置防卡顿） */
    protected static final int BLOCKS_PER_BATCH = 256;
    /**
     * 瞬发领域每 tick 放置天空盒对应的壳格进度（大于 {@link #BLOCKS_PER_BATCH}，只求比常规快）。
     */
    protected static final int INSTANT_BLOCKS_PER_BATCH = 640;
    /** 大半径阈值：超过此值使用延迟展开动画 */
    protected static final int ANIMATED_EXPAND_THRESHOLD = 8;
    /** 结界修补每tick消耗的咒力 */
    protected static final int REPAIR_COST_PER_TICK = 2;
    /** 结界修补每tick最多修补的方块数 */
    protected static final int BLOCKS_REPAIR_PER_TICK = 5;
    /** 每秒对应的tick数（用于换算） */
    protected static final double TICKS_PER_SECOND = 20.0;
    /** 低于该剩余百分比时，判定结界无法维持并立即清除。 */
    protected static final double MIN_MAINTAIN_PERCENT = 0.5D;

    // ==================== 子类可配置参数 ====================

    /** 可用结界半径档位，index 即 tier（0→8, 1→16, 2→32, 3→64）。 */
    protected abstract int[] getRadiusValues();

    /** 默认半径档位。 */
    protected abstract int getDefaultRadiusTier();

    /** 结界同时存在数量上限；超出时移除最早创建的结界。 */
    protected abstract int getMaxActiveBarrierCount();

    /** 维持结界每秒消耗的咒力基数。 */
    protected abstract double getMaintainCostBasePerSecond();

    /** 维持结界每秒消耗的咒力系数（半径每增加1格，每秒额外消耗此系数）。 */
    protected abstract double getMaintainCostPerRadiusPerSecond();

    // ==================== 展开动画相关 ====================

    /**
     * 获取展开动画类型。
     * <p>
     * 不同结界类型有不同的展开动画：
     * - 普通结界：从下向上包围（像从地面升起）
     * - 领域展开：从施术者背后向前方包围
     *
     * @return 展开动画类型
     */
    protected abstract ExpansionAnimationType getExpansionAnimationType();

    /**
     * 对球壳位置进行排序，以实现特定的展开动画效果。
     * <p>
     * 不同展开动画类型需要不同的排序方式：
     * - BOTTOM_UP：按 Y 坐标升序（从下向上）
     * - BACK_TO_FRONT：按到玩家背后的距离升序（从后向前）
     * <p>
     * 返回的新列表已按展开顺序排序，用于分批放置实现动画效果。
     *
     * @param shellBlocks 原始球壳位置列表
     * @param center      球心位置
     * @param player      施术者（用于获取朝向等信息）
     * @return 排序后的球壳位置列表
     */
    protected List<BlockPos> sortShellBlocksForExpansion(List<BlockPos> shellBlocks, BlockPos center, ServerPlayer player)
    {
        switch (getExpansionAnimationType())
        {
            case BOTTOM_UP:
                return sortBottomUp(shellBlocks, center);
            case BACK_TO_FRONT:
                return sortBackToFront(shellBlocks, center, player);
            default:
                return new ArrayList<>(shellBlocks);
        }
    }

    /**
     * 排序球壳展开顺序（施法者无关版）：AI 实体没有玩家朝向，退化为「由下向上」排序。
     */
    protected List<BlockPos> sortShellBlocksForExpansion(
            List<BlockPos> shellBlocks, BlockPos center,
            com.darkness.jujutsukaisenzone.technique.domain.DomainCaster caster)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            return sortShellBlocksForExpansion(shellBlocks, center, player);
        }
        return sortBottomUp(shellBlocks, center);
    }

    /**
     * 从下向上排序（普通结界术）。
     * <p>
     * 按 Y 坐标升序排序，Y 坐标相同的按到中心的水平距离升序。
     */
    private List<BlockPos> sortBottomUp(List<BlockPos> shellBlocks, BlockPos center)
    {
        List<BlockPos> sorted = new ArrayList<>(shellBlocks);
        sorted.sort((a, b) ->
        {
            // 先按 Y 坐标升序
            int yCompare = Integer.compare(a.getY(), b.getY());
            if (yCompare != 0)
            {
                return yCompare;
            }
            // Y 坐标相同，按到中心的水平距离升序
            int distA = (a.getX() - center.getX()) * (a.getX() - center.getX())
                     + (a.getZ() - center.getZ()) * (a.getZ() - center.getZ());
            int distB = (b.getX() - center.getX()) * (b.getX() - center.getX())
                     + (b.getZ() - center.getZ()) * (b.getZ() - center.getZ());
            return Integer.compare(distA, distB);
        });
        return sorted;
    }

    /**
     * 从背后向前排序（领域展开）。
     * <p>
     * 使用玩家背后方向向量的投影排序：将每个球壳位置投影到玩家背后方向轴上，
     * 投影值越小（越在背后）的先放置，投影值越大（越在前方）的后放置。
     * 这确保球壳从玩家背后开始生成，逐步向前方包围，形成闭合效果。
     */
    private List<BlockPos> sortBackToFront(List<BlockPos> shellBlocks, BlockPos center, ServerPlayer player)
    {
        // 计算玩家的前方朝向单位向量
        float yaw = player.getYRot();
        double dirX = -Math.sin(Math.toRadians(yaw));
        double dirZ = Math.cos(Math.toRadians(yaw));

        // 背后方向 = 朝向的反方向
        double backX = -dirX;
        double backZ = -dirZ;

        // 用玩家位置作为投影参考原点
        double playerX = player.getX();
        double playerZ = player.getZ();

        List<BlockPos> sorted = new ArrayList<>(shellBlocks);
        sorted.sort((a, b) ->
        {
            // 计算每个球壳点在背后方向轴上的投影
            // 投影 = (点 - 玩家位置) · 背后方向向量
            // 投影值为正 = 在背后，投影值为负 = 在前方
            double projA = (a.getX() + 0.5 - playerX) * backX + (a.getZ() + 0.5 - playerZ) * backZ;
            double projB = (b.getX() + 0.5 - playerX) * backX + (b.getZ() + 0.5 - playerZ) * backZ;
            // 从大到小排序：背后（投影大）先放置，前方（投影小）后放置
            int cmp = Double.compare(projB, projA);
            if (cmp != 0)
            {
                return cmp;
            }
            // 投影相同则按Y坐标升序（先下后上）
            return Integer.compare(a.getY(), b.getY());
        });
        return sorted;
    }

    // ==================== 子类必须实现的抽象方法 ====================

    /**
     * 检查施放结界的前置条件。
     * <p>
     * 不同结界类型有不同的条件：
     * - 普通结界：习得结界术
     * - 领域展开：习得领域展开 + 结界术等级达到要求
     *
     * @param player 玩家
     * @return 是否满足施放条件
     */
    @Override
    public abstract boolean canCast(ServerPlayer player);

    /**
     * 计算展开结界所需的咒力消耗。
     *
     * @param tier 半径档位
     * @return 消耗的咒力
     */
    protected abstract int calculateExpansionCost(int tier);

    /**
     * 获取结界方块的状态。
     * <p>
     * 不同结界类型可能使用不同的方块：
     * - 普通结界：黑色羊毛
     * - 领域展开：特殊领域方块
     *
     * @return 结界方块的 BlockState
     */
    protected abstract BlockState getBarrierBlockState();

    /**
     * 获取维持结界的咒力消耗减免比例。
     * <p>
     * 不同结界类型有不同的减免机制：
     * - 普通结界：根据结界术等级
     *
     * @param player 玩家
     * @return 减免比例（0.0 = 无减免，1.0 = 完全免费）
     */
    protected abstract double getMaintainCostReduction(ServerPlayer player);

    /**
     * 当结界创建时的回调。
     * <p>
     * 子类可覆盖此方法实现特殊逻辑：
     * - 领域展开：激活必中效果
     *
     * @param player   施术者
     * @param instance 结界实例
     */
    protected void onBarrierCreated(ServerPlayer player, BarrierInstance instance)
    {
        // 默认无操作，子类可覆盖
    }

    /**
     * 当结界崩解时的回调。
     *
     * @param player   施术者
     * @param instance 结界实例
     */
    protected void onBarrierCollapsed(ServerPlayer player, BarrierInstance instance)
    {
        // 默认无操作，子类可覆盖
    }

    /**
     * 当结界关闭时的回调。
     *
     * @param player   施术者
     * @param instance 结界实例
     */
    protected void onBarrierClosed(ServerPlayer player, BarrierInstance instance)
    {
        // 默认无操作，子类可覆盖
    }

    /**
     * 当结界关闭时的回调（施法者无关版）：玩家与 AI 实体共用。
     * <p>
     * 默认：有玩家 → 转调 {@link #onBarrierClosed(ServerPlayer, BarrierInstance)}；AI 实体 → 无操作。
     * 术式领域（{@code AbstractDomainExpansion}）覆写本方法以让 AI 施法也能执行领域专属清理。
     */
    protected void onBarrierClosed(com.darkness.jujutsukaisenzone.technique.domain.DomainCaster caster, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            onBarrierClosed(player, instance);
        }
    }

    // ==================== 共享数据结构 ====================

    /**
     * 结界实例 - 记录单个结界的所有状态。
     */
    protected static final class BarrierInstance
    {
        private final ResourceKey<Level> dimension;
        private final List<BlockPos> shellBlocks;  // 已排序：按到中心距离升序
        private final Map<BlockPos, BlockState> removedBlocks;
        private final BlockPos center;
        private final int radius;
        private boolean blockMobSpawn;  // 是否阻止生物生成（非final以支持实时更新）
        private int lastNotifiedTenPercentStep;
        /** 当前展开进度（已放置的方块数量） */
        private int expansionProgress;
        /** 是否正在延迟展开中 */
        private boolean isExpanding;
        /** 待处理的批次索引（用于分批放置优化） */
        private int pendingBatchIndex;
        /** 展开结界时消耗的咒力（用于信息显示） */
        private final int expansionCost;
        /** 领域展开时玩家脚下的 Y 坐标，用于生成地板；-1 表示非领域（普通结界不使用地板） */
        private final int playerFootY;
        /** 动画展开期间已处理过的 (dx,dz) 列集合，避免重复处理；展开完成后置为 null */
        private Set<Long> processedColumns;
        /** 天空盒方块位置列表（领域展开时 jjkz_skyblock 方块的位置）；空列表表示未启用天空盒 */
        private List<BlockPos> skyboxBlocks = new ArrayList<>();
        /** 与 {@link #skyboxBlocks} 同步的查重集合。 */
        private Set<BlockPos> skyboxBlockSet = new HashSet<>();
        /** {@link #shellBlocks} 的查重缓存（瞬发天空盒展开用，避免每格重建）。 */
        @Nullable
        private Set<BlockPos> shellBlockSet;
        /** 自定义天空盒纹理路径；非 null 时表示领域使用了 jjkz_skyblock，修补时需要此信息 */
        @Nullable
        private net.minecraft.resources.ResourceLocation customSkyboxTexture = null;
        /**
         * 瞬发：只按球壳顺序展开天空盒，不放置结界/领域壳方块。
         */
        private boolean skyboxOnlyExpand;
        /** 结界构建完成后立刻关闭（瞬发领域模式）。 */
        private boolean autoCloseOnComplete;
        /**
         * 开放式：无领域壳 / 无天空盒方块，仅半径逻辑 + 客户端范围立方体贴图。
         */
        private boolean openDomain;
        /**
         * 瞬发：构建完成后的「完成态」剩余 tick；&gt;0 时保持实例、刷新慢镜头，倒计时结束再关。
         * 0 表示未进入完成态保持。
         */
        private int autoCloseHoldTicksRemaining;
        /** 本场瞬发慢镜头曾施加过的实体（关域时统一移除效果）。 */
        private final Set<UUID> instantSlowMoTargets = new HashSet<>();
        /** 本 tick 内已执行 auto-close 清理，维持循环应丢弃该实例。 */
        private boolean discardAfterTick;
        /** 瞬发展开过程中「进度过半」钩子是否已触发过（同一次展开只触发一次）。 */
        private boolean instantHalfProgressNotified;

        public BarrierInstance(
                ResourceKey<Level> dimension,
                List<BlockPos> shellBlocks,
                Map<BlockPos, BlockState> removedBlocks,
                BlockPos center,
                int radius,
                boolean blockMobSpawn,
                int lastNotifiedTenPercentStep,
                int expansionProgress,
                boolean isExpanding,
                int pendingBatchIndex,
                int expansionCost
        )
        {
            this(dimension, shellBlocks, removedBlocks, center, radius, blockMobSpawn,
                 lastNotifiedTenPercentStep, expansionProgress, isExpanding, pendingBatchIndex, expansionCost, -1);
        }

        public BarrierInstance(
                ResourceKey<Level> dimension,
                List<BlockPos> shellBlocks,
                Map<BlockPos, BlockState> removedBlocks,
                BlockPos center,
                int radius,
                boolean blockMobSpawn,
                int lastNotifiedTenPercentStep,
                int expansionProgress,
                boolean isExpanding,
                int pendingBatchIndex,
                int expansionCost,
                int playerFootY
        )
        {
            this.dimension = dimension;
            this.shellBlocks = shellBlocks;
            this.removedBlocks = removedBlocks;
            this.center = center;
            this.radius = radius;
            this.blockMobSpawn = blockMobSpawn;
            this.lastNotifiedTenPercentStep = lastNotifiedTenPercentStep;
            this.expansionProgress = expansionProgress;
            this.isExpanding = isExpanding;
            this.pendingBatchIndex = pendingBatchIndex;
            this.expansionCost = expansionCost;
            this.playerFootY = playerFootY;
            this.processedColumns = null;
        }

        // Getters
        public ResourceKey<Level> getDimension() { return dimension; }
        public List<BlockPos> getShellBlocks() { return shellBlocks; }
        public Map<BlockPos, BlockState> getRemovedBlocks() { return removedBlocks; }
        public BlockPos getCenter() { return center; }
        public int getRadius() { return radius; }
        public boolean isBlockMobSpawn() { return blockMobSpawn; }
        public int getLastNotifiedTenPercentStep() { return lastNotifiedTenPercentStep; }
        public int getExpansionProgress() { return expansionProgress; }
        public boolean isExpanding() { return isExpanding; }
        public int getPendingBatchIndex() { return pendingBatchIndex; }
        public int getExpansionCost() { return expansionCost; }
        public int getPlayerFootY() { return playerFootY; }
        public Set<Long> getProcessedColumns() { return processedColumns; }
        public List<BlockPos> getSkyboxBlocks() { return skyboxBlocks; }
        public Set<BlockPos> getSkyboxBlockSet() { return skyboxBlockSet; }
        public Set<BlockPos> getOrCreateShellBlockSet()
        {
            if (shellBlockSet == null)
            {
                shellBlockSet = new HashSet<>(shellBlocks);
            }
            return shellBlockSet;
        }
        @Nullable
        public net.minecraft.resources.ResourceLocation getCustomSkyboxTexture() { return customSkyboxTexture; }
        public boolean isSkyboxOnlyExpand() { return skyboxOnlyExpand; }
        public boolean isAutoCloseOnComplete() { return autoCloseOnComplete; }
        public boolean isOpenDomain() { return openDomain; }
        public boolean isDiscardAfterTick() { return discardAfterTick; }
        public int getAutoCloseHoldTicksRemaining() { return autoCloseHoldTicksRemaining; }
        public Set<UUID> getInstantSlowMoTargets() { return instantSlowMoTargets; }
        public boolean isInstantHalfProgressNotified() { return instantHalfProgressNotified; }
        public void setInstantHalfProgressNotified(boolean instantHalfProgressNotified) { this.instantHalfProgressNotified = instantHalfProgressNotified; }

        // Setters
        public void setBlockMobSpawn(boolean blockMobSpawn) { this.blockMobSpawn = blockMobSpawn; }
        public void setLastNotifiedTenPercentStep(int step) { this.lastNotifiedTenPercentStep = step; }
        public void setExpansionProgress(int progress) { this.expansionProgress = progress; }
        public void setExpanding(boolean expanding) { this.isExpanding = expanding; }
        public void setPendingBatchIndex(int index) { this.pendingBatchIndex = index; }
        public void setProcessedColumns(Set<Long> processedColumns) { this.processedColumns = processedColumns; }
        public void setSkyboxBlocks(List<BlockPos> skyboxBlocks)
        {
            this.skyboxBlocks = skyboxBlocks == null ? new ArrayList<>() : skyboxBlocks;
            this.skyboxBlockSet = new HashSet<>(this.skyboxBlocks);
        }
        public void setCustomSkyboxTexture(@Nullable net.minecraft.resources.ResourceLocation texture) { this.customSkyboxTexture = texture; }
        public void setSkyboxOnlyExpand(boolean skyboxOnlyExpand) { this.skyboxOnlyExpand = skyboxOnlyExpand; }
        public void setAutoCloseOnComplete(boolean autoCloseOnComplete) { this.autoCloseOnComplete = autoCloseOnComplete; }
        public void setOpenDomain(boolean openDomain) { this.openDomain = openDomain; }
        public void setDiscardAfterTick(boolean discardAfterTick) { this.discardAfterTick = discardAfterTick; }
        public void setAutoCloseHoldTicksRemaining(int ticks) { this.autoCloseHoldTicksRemaining = Math.max(0, ticks); }

        public void trackInstantSlowMoTarget(UUID id)
        {
            if (id != null)
            {
                instantSlowMoTargets.add(id);
            }
        }

        public void clearInstantSlowMoTargets()
        {
            instantSlowMoTargets.clear();
        }

        /** 登记一处天空盒位置（列表 + 查重集）。 */
        public void trackSkyboxBlock(BlockPos pos)
        {
            BlockPos immutable = pos.immutable();
            if (skyboxBlockSet.add(immutable))
            {
                skyboxBlocks.add(immutable);
            }
        }
    }

    // ==================== 共享实现方法 ====================

    /**
     * 构建空心球壳的所有位置（不放置方块）。
     * <p>
     * 返回的列表已按到中心的距离排序。
     *
     * @param center 球心位置
     * @param radius 半径
     * @return 球壳位置列表
     */
    protected final List<BlockPos> buildHollowSpherePositions(BlockPos center, int radius)
    {
        List<BlockPos> shellBlocks = new ArrayList<>();
        int radiusSquared = radius * radius;
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dy = -radius; dy <= radius; dy++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    int distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (!isOnShell(distanceSquared, radiusSquared))
                    {
                        continue;
                    }
                    shellBlocks.add(new BlockPos(center.getX() + dx, center.getY() + dy, center.getZ() + dz));
                }
            }
        }
        return shellBlocks;
    }

    /**
     * 判断指定距离是否在球壳上。
     * <p>
     * 使用整数运算避免浮点精度问题：外半径以内且内半径以外的位置均视为球壳。
     * 这确保球壳厚度至少为1格，不会出现漏风。
     */
    protected final boolean isOnShell(int distanceSquared, int radiusSquared)
    {
        int outerRadius = (int) Math.ceil(Math.sqrt(radiusSquared));
        int innerRadius = outerRadius - 1;
        int outerRS = outerRadius * outerRadius;
        int innerRS = innerRadius * innerRadius;
        return distanceSquared <= outerRS && distanceSquared >= innerRS;
    }

    /**
     * 检查玩家是否在结界内部。
     */
    protected final boolean isPlayerInsideBarrier(ServerPlayer player, BarrierInstance instance)
    {
        if (!instance.getDimension().equals(player.serverLevel().dimension()))
        {
            return false;
        }
        double cx = instance.getCenter().getX() + 0.5D;
        double cy = instance.getCenter().getY() + 0.5D;
        double cz = instance.getCenter().getZ() + 0.5D;
        double dx = player.getX() - cx;
        double dy = player.getY() - cy;
        double dz = player.getZ() - cz;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double radiusSquared = instance.getRadius() * instance.getRadius();
        return distanceSquared < radiusSquared;
    }

    /**
     * 计算结界剩余生命值百分比。
     */
    protected final double calculateRemainingPercent(ServerLevel level, BarrierInstance instance)
    {
        int total = instance.getShellBlocks().size();
        if (total <= 0)
        {
            return 0.0D;
        }
        int remaining = 0;
        for (BlockPos pos : instance.getShellBlocks())
        {
            if (level.getBlockState(pos).is(getBarrierBlockState().getBlock()))
            {
                remaining++;
            }
        }
        return remaining / (double) total;
    }

    /**
     * 清空球体内部空间。
     * <p>
     * 只清除球壳内部的方块，不会触碰球壳上的方块。
     * 使用与 {@link #isOnShell} 一致的内径定义，确保球壳完整。
     */
    protected final void clearSphereInterior(ServerLevel level, BlockPos center, int radius, Map<BlockPos, BlockState> removedBlocks)
    {
        int radiusSquared = radius * radius;
        // 计算球壳内径，与 isOnShell 保持一致
        int outerRadius = (int) Math.ceil(Math.sqrt(radiusSquared));
        int innerRadius = outerRadius - 1;
        int innerRS = innerRadius * innerRadius;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dy = -radius; dy <= radius; dy++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    int distanceSquared = dx * dx + dy * dy + dz * dz;
                    // 跳过球壳及其外部：d² >= innerRadius² 的位置都是球壳或外部
                    if (distanceSquared >= innerRS)
                    {
                        continue;
                    }
                    mutablePos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.getBlockState(mutablePos).isAir())
                    {
                        rememberRemovedBlock(level, mutablePos, removedBlocks);
                        level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                    }
                }
            }
        }
    }

    /**
     * 记录被删除方块的原状态。
     * <p>
     * 本模组结界 / 领域 / 天空盒属于施工临时方块，不得记入「原地形」——否则关域还原时会把它们摆回去。
     */
    protected final void rememberRemovedBlock(ServerLevel level, BlockPos.MutableBlockPos mutablePos, Map<BlockPos, BlockState> removedBlocks)
    {
        BlockPos immutablePos = mutablePos.immutable();
        removedBlocks.putIfAbsent(immutablePos, originalStateForRestore(level.getBlockState(immutablePos)));
    }

    /**
     * 记录被删除方块的原状态（BlockPos 版本）。
     */
    protected final void rememberRemovedBlockForPos(ServerLevel level, BlockPos pos, Map<BlockPos, BlockState> removedBlocks)
    {
        removedBlocks.putIfAbsent(pos.immutable(), originalStateForRestore(level.getBlockState(pos)));
    }

    /** 施工临时方块（结界壳 / 天空盒）按空气记入还原表。 */
    private BlockState originalStateForRestore(BlockState existing)
    {
        if (existing.is(getBarrierBlockState().getBlock()) || ShaderedCompat.isSkyboxBlock(existing))
        {
            return Blocks.AIR.defaultBlockState();
        }
        return existing;
    }

    /**
     * 恢复被删除的方块。
     */
    protected final void restoreRemovedBlocks(ServerLevel level, BarrierInstance instance)
    {
        for (Map.Entry<BlockPos, BlockState> entry : instance.getRemovedBlocks().entrySet())
        {
            level.setBlock(entry.getKey(), entry.getValue(), BLOCK_UPDATE_FLAGS);
        }
    }

    /**
     * 计算从指定位置到"脚下最近非空气方块顶面"的距离。
     */
    protected final double getDistanceToFootBlockFromPos(ServerLevel level, int x, int y, int z)
    {
        int minY = level.getMinBuildHeight();
        int startY = y - 1;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int posY = startY; posY >= minY; posY--)
        {
            mutablePos.set(x, posY, z);
            if (!level.getBlockState(mutablePos).isAir())
            {
                double blockTopY = posY + 1.0D;
                return y - blockTopY;
            }
        }
        return Double.MAX_VALUE;
    }

    /**
     * 从中心计算到脚下方块的距离。
     */
    protected final double getDistanceToFootBlockFromCenter(ServerLevel level, BlockPos center)
    {
        return getDistanceToFootBlockFromPos(level, center.getX(), center.getY(), center.getZ());
    }

    /**
     * 即时放置所有结界方块（用于小半径结界或展开完成时）。
     */
    protected final void placeAllBarrierBlocks(ServerLevel level, List<BlockPos> shellBlocks,
                                               Map<BlockPos, BlockState> removedBlocks, BlockPos center, int radius)
    {
        // 清空球体内部（如果玩家不在球体范围内）
        double distanceToFootBlock = getDistanceToFootBlockFromCenter(level, center);
        if (distanceToFootBlock >= radius)
        {
            clearSphereInterior(level, center, radius, removedBlocks);
        }

        for (BlockPos pos : shellBlocks)
        {
            if (!level.getBlockState(pos).isAir())
            {
                rememberRemovedBlockForPos(level, pos, removedBlocks);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
            }
            level.setBlock(pos, getBarrierBlockState(), BLOCK_UPDATE_FLAGS);
        }
    }

    /**
     * 清理单个结界实例。
     * <p>
     * 先触发 {@link #onBarrierClosed} 回调，再执行世界侧还原
     * （先移除天空盒方块，再清理球壳方块，最后恢复被替换的原方块）。
     */
    protected final void clearBarrierInstance(ServerPlayer player, BarrierInstance instance)
    {
        onBarrierClosed(player, instance);
        restoreBarrierWorld(player.server.getLevel(instance.getDimension()), instance);
    }

    /**
     * 清理单个结界实例（施法者无关版）：玩家与 AI 实体共用。
     * <p>
     * 与 {@link #clearBarrierInstance(ServerPlayer, BarrierInstance)} 的唯一差别是回调走
     * {@code caster}，世界侧还原仍旧按维度执行。
     */
    protected final void clearBarrierInstance(
            com.darkness.jujutsukaisenzone.technique.domain.DomainCaster caster, BarrierInstance instance)
    {
        onBarrierClosed(caster, instance);
        restoreBarrierWorld(caster.level(), instance);
    }

    /**
     * 仅执行世界侧还原：天空盒方块 → 球壳方块 → 被替换的原方块。
     * <p>
     * 不触发 {@link #onBarrierClosed} 回调，供拿不到在线施术者对象的场合使用
     * （如服务器停机时对残留实例做兜底还原）。{@code level} 为 null（维度已不可用）时静默跳过。
     */
    protected final void restoreBarrierWorld(@Nullable ServerLevel level, BarrierInstance instance)
    {
        if (level == null)
        {
            return;
        }
        /* 先移除天空盒方块（避免天空盒位置与 removedBlocks 冲突） */
        ShaderedCompat.removeSkyboxLayer(level, instance.getSkyboxBlocks(), instance.getRemovedBlocks());

        for (BlockPos pos : instance.getShellBlocks())
        {
            if (level.getBlockState(pos).is(getBarrierBlockState().getBlock()))
            {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
            }
        }
        restoreRemovedBlocks(level, instance);

        /*
         * 兜底：还原表若曾误记天空盒 / 结界为「原地形」，restore 会把它们摆回球壳。
         * 球壳位与已登记天空盒位必须最终为空或真正原地形，再清一遍施工方块。
         */
        ShaderedCompat.removeSkyboxLayer(level, instance.getSkyboxBlocks(), null);
        for (BlockPos pos : instance.getShellBlocks())
        {
            BlockState state = level.getBlockState(pos);
            if (state.is(getBarrierBlockState().getBlock()) || ShaderedCompat.isSkyboxBlock(state))
            {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 修补结界/领域方块及天空盒方块的破损处。
     * <p>
     * 修补优先级：先修补结界/领域方块（shell），再修补天空盒方块（skybox）。
     * 天空盒修补需要 instance 中记录了 customSkyboxTexture 才能确定纹理。
     *
     * @param level    服务端世界
     * @param instance 结界实例
     * @return 本次修补的方块总数
     */
    protected final int repairBarrierAndSkybox(ServerLevel level, BarrierInstance instance)
    {
        int repaired = 0;

        /* 第一优先级：修补结界/领域方块 */
        for (BlockPos pos : instance.getShellBlocks())
        {
            if (repaired >= BLOCKS_REPAIR_PER_TICK)
            {
                break;
            }
            if (!level.getBlockState(pos).is(getBarrierBlockState().getBlock()))
            {
                level.setBlock(pos, getBarrierBlockState(), BLOCK_UPDATE_FLAGS);
                repaired++;
            }
        }

        /* 第二优先级：修补天空盒方块（jjkz_skyblock） */
        if (repaired >= BLOCKS_REPAIR_PER_TICK || instance.getSkyboxBlocks().isEmpty())
        {
            return repaired;
        }

        ResourceLocation customTexture = instance.getCustomSkyboxTexture();
        if (customTexture == null)
        {
            return repaired;
        }

        /* 修补自定义天空盒方块 */
        var jjkzSkyblock = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation(JuJutsuKaisenZone.MODID, "jjkz_skyblock")
        );
        if (jjkzSkyblock == null)
        {
            return repaired;
        }

        for (BlockPos pos : instance.getSkyboxBlocks())
        {
            if (repaired >= BLOCKS_REPAIR_PER_TICK)
            {
                break;
            }
            BlockState current = level.getBlockState(pos);
            /* 仅当该位置不再是天空盒方块时才修补 */
            if (!ShaderedCompat.isSkyboxBlock(current) && !current.is(getBarrierBlockState().getBlock()))
            {
                level.setBlock(pos, jjkzSkyblock.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                /* 设置 BlockEntity 的纹理路径 */
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof com.darkness.jujutsukaisenzone.blockentity.JJKZSkyblockEntity jjkzBe)
                {
                    jjkzBe.setTextureFolder(customTexture);
                }
                repaired++;
            }
        }

        return repaired;
    }

    /**
     * 检查玩家是否在战斗模式。
     * <p>
     * 对 {@code DomainCaster.PlayerCaster}（跨包）开放，故为 {@code public}。
     */
    public final boolean isInBattleMode(ServerPlayer player)
    {
        return player.isAlive() && EpicFightCompat.isBattleMode(player);
    }

    /**
     * 获取半径值。
     */
    protected final int getRadius(int tier)
    {
        int[] values = getRadiusValues();
        int clampedTier = Math.max(0, Math.min(tier, values.length - 1));
        return values[clampedTier];
    }
}
