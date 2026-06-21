package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.TechniqueMeltdownEffect;
import com.darkness.jujutsukaisenzone.compat.ShaderedCompat;
import com.darkness.jujutsukaisenzone.player.BarrierRulesData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainExpansionData;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModBlocks;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.barrier.AbstractBarrierTechnique;
import com.darkness.jujutsukaisenzone.technique.barrier.ExpansionAnimationType;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 领域展开抽象基类 - 提供领域展开的共享实现。
 * <p>
 * 继承自 {@link AbstractBarrierTechnique}，定义了领域展开的骨架实现。
 * 子类只需实现领域专属逻辑（领域方块、特殊效果等）。
 * <p>
 * 采用模板方法模式：
 * - 基类定义领域展开的标准流程
 * - 子类通过覆盖钩子方法实现特殊行为
 * <p>
 * 类层次：
 * <pre>
 *   AbstractBarrierTechnique
 *       └── AbstractDomainExpansion（本类）
 *               ├── DomainExpansionServerLogic（基础领域，无术式时）
 *               └── AbstractTechniqueDomain（术式领域抽象基类）
 *                       ├── XxxDomainExpansion（具体术式领域）
 *                       └── ...
 * </pre>
 */
public abstract class AbstractDomainExpansion extends AbstractBarrierTechnique
{
    // ==================== 配置参数 ====================

    /**
     * 领域天空盒的默认纹理路径（黑色天空盒）。
     * <p>
     * 所有领域展开统一使用 jjkz_skyblock 方块，默认使用黑色纹理。
     */
    private static final ResourceLocation DEFAULT_SKYBOX_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/environment/black");

    /** 领域展开的固定半径 */
    protected static final int DOMAIN_RADIUS = 24;

    /** 领域同时存在数量上限 */
    private static final int MAX_ACTIVE_BARRIER_COUNT = 1;

    /** 维持领域每秒消耗的咒力基数 */
    private static final double MAINTAIN_COST_BASE_PER_SECOND = 2.0;

    /** 维持领域每秒消耗的咒力系数 */
    private static final double MAINTAIN_COST_PER_RADIUS_PER_SECOND = 1.0;

    // ==================== 状态管理 ====================

    /**
     * 活跃领域：按施术者 UUID 记录（每个领域实现各自维护）。
     * <p>
     * 业务意图：避免不同领域实现共享同一状态容器，导致 actionbar 文案与领域类型串台。
     */
    protected final ConcurrentMap<UUID, Deque<BarrierInstance>> ACTIVE_DOMAINS = new ConcurrentHashMap<>();

    // ==================== 派生字段 ====================

    /** 领域名称 */
    private String domainName = "";

    /** 领域展开消耗咒力 */
    private int domainExpansionCost = 200;

    /** 维持领域消耗咒力（每秒） */
    private int domainMaintainCost = 30;

    // ==================== 派生字段的 Getter/Setter ====================

    public String getDomainName()
    {
        return domainName;
    }

    public void setDomainName(String domainName)
    {
        this.domainName = domainName;
    }

    public boolean hasLearnedDomainExpansion()
    {
        /* 无参版本保留向后兼容，但无法访问持久化数据，始终返回 false。
         * 有 ServerPlayer 的调用方应使用 hasLearnedDomainExpansion(ServerPlayer) */
        return false;
    }

    /** 检查指定玩家是否已习得领域展开（从持久化数据读取） */
    public boolean hasLearnedDomainExpansion(ServerPlayer player)
    {
        return PlayerDomainExpansionData.hasLearned(player);
    }

    /** 设置指定玩家的领域展开习得状态（写入持久化数据） */
    public void setLearnedDomainExpansion(ServerPlayer player, boolean learned)
    {
        PlayerDomainExpansionData.setLearned(player, learned);
    }

    public int getDomainExpansionCost()
    {
        return domainExpansionCost;
    }

    public void setDomainExpansionCost(int cost)
    {
        this.domainExpansionCost = cost;
    }

    public int getDomainMaintainCost()
    {
        return domainMaintainCost;
    }

    public void setDomainMaintainCost(int cost)
    {
        this.domainMaintainCost = cost;
    }

    // ==================== 实现抽象方法 ====================

    @Override
    protected int[] getRadiusValues()
    {
        return new int[]{DOMAIN_RADIUS};
    }

    @Override
    protected int getDefaultRadiusTier()
    {
        return 0;
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
        return ExpansionAnimationType.BACK_TO_FRONT;
    }

    @Override
    public boolean canCast(ServerPlayer player)
    {
        return isInBattleMode(player)
                && hasLearnedDomainExpansion(player)
                && PlayerBarrierTechniqueData.hasLearned(player);
    }

    @Override
    protected int calculateExpansionCost(int tier)
    {
        // 领域展开固定消耗
        return domainExpansionCost;
    }

    @Override
    protected double getMaintainCostReduction(ServerPlayer player)
    {
        // 领域展开不享受结界术等级减免
        return 0.0;
    }

    // ==================== 子类必须实现的抽象方法 ====================

    /**
     * 应用领域专属效果。
     * <p>
     * 基础领域无特殊效果，术式领域有专属效果（如必中、伤害加成等）。
     *
     * @param player   施术者
     * @param instance 领域实例
     */
    protected abstract void applyDomainSpecificEffect(ServerPlayer player, BarrierInstance instance);

    @Override
    protected void onBarrierClosed(ServerPlayer player, BarrierInstance instance)
    {
        TechniqueMeltdownEffect.applyOnDomainClosed(player);
    }

    // ==================== 领域方块 ====================

    /**
     * 所有领域共用同一个领域方块，不需要为不同术式创建额外方块。
     * <p>
     * 子类一般无需覆盖此方法。如果未来确有需要，可以覆盖。
     */
    @Override
    protected BlockState getBarrierBlockState()
    {
        return ModBlocks.DOMAIN_BLOCK.get().defaultBlockState();
    }

    // ==================== 核心逻辑 ====================

    @Override
    public void cast(ServerPlayer player)
    {
        if (!player.isAlive() || !isInBattleMode(player))
        {
            return;
        }

        // 如果玩家已在自己的领域内，解除领域（Alt+结界术键 = 解除领域）
        if (closeDomainIfPlayerInside(player))
        {
            return;
        }

        if (TechniqueMeltdownEffect.blockDomainExpansion(player))
        {
            return;
        }

        // 检查玩家是否在任何领域内（包括别人的领域），领域内无法展开领域
        if (isPlayerInsideAnyDomain(player))
        {
            player.displayClientMessage(Component.literal("无法展开领域，因为领域对抗还没做").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 检查玩家是否在普通结界内，结界内无法展开领域
        if (BarrierTechniqueServerLogic.getInstance().isPositionInBarrier(player.serverLevel(), player.blockPosition()))
        {
            player.displayClientMessage(Component.literal("结界内无法展开领域").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 检查是否习得领域展开
        if (!hasLearnedDomainExpansion(player))
        {
            player.displayClientMessage(Component.literal("尚未习得领域展开").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 检查是否习得结界术（前置条件）
        if (!PlayerBarrierTechniqueData.hasLearned(player))
        {
            player.displayClientMessage(Component.literal("需要先习得结界术").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 领域展开固定半径
        int radius = DOMAIN_RADIUS;

        // 检查并消耗展开领域所需的咒力
        int expansionCost = calculateExpansionCost(0);
        if (!PlayerCursedEnergyData.tryConsume(player, expansionCost))
        {
            player.displayClientMessage(
                    Component.literal("咒力不足，无法展开领域（需要 " + expansionCost + "）")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int playerFootY = player.getBlockY() - 1;
        Map<BlockPos, BlockState> removedBlocks = new HashMap<>();

        // 计算球壳所有位置
        List<BlockPos> shellBlocks = buildHollowSpherePositions(center, radius);
        if (shellBlocks.isEmpty())
        {
            return;
        }

        // 领域展开始终使用延迟展开动画（从背后向前方包围）
        boolean animatedExpand = radius > ANIMATED_EXPAND_THRESHOLD;

        // 排序（从背后向前方包围）
        shellBlocks = sortShellBlocksForExpansion(shellBlocks, center, player);

        // 读取玩家的规则设置
        boolean blockMobSpawn = BarrierRulesData.isBlockMobSpawn(player.getUUID());

        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.computeIfAbsent(player.getUUID(), key -> new ArrayDeque<>());

        BarrierInstance instance = new BarrierInstance(
                level.dimension(),
                shellBlocks,
                removedBlocks,
                center,
                radius,
                blockMobSpawn,
                10,
                animatedExpand ? 0 : shellBlocks.size(),
                animatedExpand,
                0,
                expansionCost,
                playerFootY
        );

        queue.addLast(instance);

        // 领域只能同时存在一个
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
            ACTIVE_DOMAINS.remove(player.getUUID());
        }

        // 领域展开固定清除内部方块
        if (!animatedExpand)
        {
            // 即时展开：放置所有方块、生成地板、填充地板以下、清空地板以上
            placeAllDomainBlocks(level, shellBlocks, removedBlocks, center, radius, playerFootY);
            // 放置天空盒（替换地板层 + 壳内层，如果玩家配置了天空盒）
            placeDomainSkybox(level, shellBlocks, center, radius, playerFootY, removedBlocks, instance);
            // 即时展开也视为“领域完成”，在此触发领域专属完成效果。
            applyDomainSpecificEffect(player, instance);
            String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
            player.displayClientMessage(
                    Component.literal(nameDisplay + "领域展开完成（半径 " + radius + "）")
                            .withStyle(ChatFormatting.GOLD), true);
        }
        else
        {
            String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
            player.displayClientMessage(
                    Component.literal(nameDisplay + "领域展开中（半径 " + radius + "）")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    @Override
    public void tickMaintain(ServerPlayer player)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
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
                tickDomainExpand(player, level, instance);
                kept.addLast(instance);
                continue;
            }

            // 玩家离开领域 → 领域立刻崩解
            if (!isPlayerInsideBarrier(player, instance))
            {
                String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                player.displayClientMessage(
                        Component.literal(nameDisplay + "离开领域范围，领域崩解")
                                .withStyle(ChatFormatting.RED), true);
                clearBarrierInstance(player, instance);
                continue;
            }

            // 消耗维持领域所需的咒力
            double costPerTick = (MAINTAIN_COST_BASE_PER_SECOND + instance.getRadius() * MAINTAIN_COST_PER_RADIUS_PER_SECOND) / TICKS_PER_SECOND;
            double reduction = getMaintainCostReduction(player);
            costPerTick = costPerTick * (1.0 - reduction);
            int costThisTick = (int) Math.ceil(costPerTick);
            if (costThisTick > 0)
            {
                if (!PlayerCursedEnergyData.tryConsume(player, costThisTick))
                {
                    if (!collapsedNotified)
                    {
                        String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                        player.displayClientMessage(
                                Component.literal(nameDisplay + "咒力耗尽，领域崩解")
                                        .withStyle(ChatFormatting.RED), true);
                        collapsedNotified = true;
                    }
                    clearBarrierInstance(player, instance);
                    continue;
                }
            }

            // 领域生命值追踪
            double remainingPercent = calculateRemainingPercent(level, instance);
            if (remainingPercent < MIN_MAINTAIN_PERCENT)
            {
                if (!collapsedNotified && isPlayerInsideBarrier(player, instance))
                {
                    String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                    player.displayClientMessage(
                            Component.literal(nameDisplay + "已崩解")
                                    .withStyle(ChatFormatting.RED), true);
                    collapsedNotified = true;
                }
                clearBarrierInstance(player, instance);
                continue;
            }

            notifyDomainHealthIfNeeded(player, instance, remainingPercent);
            kept.addLast(instance);
        }

        if (kept.isEmpty())
        {
            ACTIVE_DOMAINS.remove(player.getUUID());
            return;
        }
        ACTIVE_DOMAINS.put(player.getUUID(), kept);
    }

    @Override
    public boolean isPositionInBarrier(ServerLevel level, BlockPos pos)
    {
        return isPositionInAnyDomain(level, pos);
    }

    @Override
    public boolean isPositionBlockingMobSpawn(ServerLevel level, BlockPos pos)
    {
        ResourceKey<Level> dimension = level.dimension();
        double px = pos.getX() + 0.5D;
        double py = pos.getY() + 0.5D;
        double pz = pos.getZ() + 0.5D;

        for (Deque<BarrierInstance> queue : ACTIVE_DOMAINS.values())
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
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }

        boolean newBlockMobSpawn = BarrierRulesData.isBlockMobSpawn(player.getUUID());

        for (BarrierInstance instance : queue)
        {
            if (isPlayerInsideBarrier(player, instance))
            {
                instance.setBlockMobSpawn(newBlockMobSpawn);
                String status = newBlockMobSpawn ? "已启用" : "已禁用";
                player.displayClientMessage(
                        Component.literal("领域阻止生物生成规则：" + status).withStyle(ChatFormatting.GREEN),
                        true
                );
                return;
            }
        }
    }

    @Override
    public void repairBarrierGaps(ServerPlayer player)
    {
        if (!PlayerReverseTechniqueData.hasReverseTechnique(player))
        {
            return;
        }

        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        if (!isPlayerInsideBarrier(player, instance))
        {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (level == null)
        {
            return;
        }

        if (!PlayerCursedEnergyData.tryConsume(player, REPAIR_COST_PER_TICK))
        {
            return;
        }

        /* 修补领域方块和天空盒方块（优先修补领域方块） */
        repairBarrierAndSkybox(level, instance);
    }

    /**
     * 领域展开使用固定半径，不支持档位调整。
     * 此方法仅为满足接口契约，调用时无实际效果。
     */
    @Override
    public int adjustBarrierRadiusTier(ServerPlayer player, int delta)
    {
        return 0;
    }

    // ==================== 辅助方法 ====================

    /**
     * 放置领域天空盒：替换地板层为 jjkz_skyblock，并在球壳内层放置 jjkz_skyblock。
     * <p>
     * 领域展开必须使用天空盒方块，所有天空盒方块使用 jjkz_skyblock，
     * 通过 BlockEntity 设置纹理路径实现不同领域环境的视觉效果。
     * 默认使用黑色纹理。
     * <p>
     * 天空盒效果分为两部分：
     * <ol>
     *   <li>地板层：将玩家脚下的领域方块地板替换为天空盒方块</li>
     *   <li>壳内层：在球壳内侧 1 格处放置天空盒方块，形成完整的封闭天空盒空间</li>
     * </ol>
     *
     * @param level         服务端世界
     * @param shellBlocks   球壳方块位置列表
     * @param center        球心位置
     * @param radius        球壳半径
     * @param playerFootY   地板 Y 坐标
     * @param removedBlocks 被替换方块的记录
     * @param instance      领域实例
     */
    private void placeDomainSkybox(ServerLevel level, List<BlockPos> shellBlocks,
                                    BlockPos center, int radius, int playerFootY,
                                    Map<BlockPos, BlockState> removedBlocks,
                                    BarrierInstance instance)
    {
        placeDomainSkyboxWithTexture(level, shellBlocks, center, radius, playerFootY, removedBlocks, instance,
                getDomainSkyboxTexture());
    }

    private void placeDomainSkyboxWithTexture(
            ServerLevel level,
            List<BlockPos> shellBlocks,
            BlockPos center,
            int radius,
            int playerFootY,
            Map<BlockPos, BlockState> removedBlocks,
            BarrierInstance instance,
            ResourceLocation skyboxTexture)
    {
        /* 使用 jjkz_skyblock 替换地板层 */
        List<BlockPos> floorSkybox = ShaderedCompat.placeCustomSkyboxFloor(
                level, collectFloorPositions(center, radius, playerFootY),
                getBarrierBlockState().getBlock(), skyboxTexture, removedBlocks);

        /* 使用 jjkz_skyblock 替换壳内层 */
        List<BlockPos> shellInnerSkybox = ShaderedCompat.placeCustomSkyboxLayer(
                level, shellBlocks, center, radius, skyboxTexture, removedBlocks);

        /* 合并所有天空盒位置到实例 */
        List<BlockPos> allSkybox = new java.util.ArrayList<>(floorSkybox.size() + shellInnerSkybox.size());
        allSkybox.addAll(floorSkybox);
        allSkybox.addAll(shellInnerSkybox);
        instance.setSkyboxBlocks(allSkybox);
        instance.setCustomSkyboxTexture(skyboxTexture);
    }

    /**
     * 领域结界完成时用于 {@link #placeDomainSkybox} 的 jjkz_skyblock 纹理（内壁与地板天空盒层）。
     * 术式领域子类可覆盖；默认黑色天空盒。
     */
    protected ResourceLocation getDomainSkyboxTexture()
    {
        return DEFAULT_SKYBOX_TEXTURE;
    }

    /**
     * 收集领域地板（playerFootY 层）在球体内部的所有方块位置。
     */
    private List<BlockPos> collectFloorPositions(BlockPos center, int radius, int playerFootY)
    {
        int innerRadius = radius - 1;
        int innerRS = innerRadius * innerRadius;
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int dy = playerFootY - cy;

        List<BlockPos> positions = new java.util.ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                int distXZSq = dx * dx + dz * dz;
                if (distXZSq + dy * dy >= innerRS)
                {
                    continue;
                }
                positions.add(new BlockPos(cx + dx, playerFootY, cz + dz));
            }
        }
        return positions;
    }

    /**
     * 放置所有领域方块，同时生成地板、填充地板以下空间、清空地板以上空间。
     * <p>
     * 与普通结界不同，领域展开：
     * - 在玩家脚下 Y 层铺设领域方块地板
     * - 从地板往下到球壳底部用领域方块填充
     * - 只清空地板以上的球体内部空间
     * - 所有被替换的原方块都会记录到 removedBlocks 以便恢复
     */
    private void placeAllDomainBlocks(ServerLevel level, List<BlockPos> shellBlocks,
                                       Map<BlockPos, BlockState> removedBlocks, BlockPos center, int radius, int playerFootY)
    {
        int innerRadius = radius - 1;
        int innerRS = innerRadius * innerRadius;
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // 1. 放置球壳方块
        for (BlockPos pos : shellBlocks)
        {
            if (!level.getBlockState(pos).isAir())
            {
                rememberRemovedBlockForPos(level, pos, removedBlocks);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
            }
            level.setBlock(pos, getBarrierBlockState(), BLOCK_UPDATE_FLAGS);
        }

        // 2. 遍历球内所有 (dx, dz) 列，处理地板/填充/清空
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                // 先找到球内在该 (dx, dz) 列的 Y 范围
                int minDistXZ = dx * dx + dz * dz;
                if (minDistXZ >= innerRS)
                {
                    continue;
                }
                int yRange = (int) Math.sqrt(innerRS - minDistXZ);
                int minY = cy - yRange;
                int maxY = cy + yRange;

                // 2a. 清空地板以上空间（非领域方块 → 空气）
                for (int y = playerFootY + 1; y <= maxY; y++)
                {
                    int dy = y - cy;
                    int distSq = minDistXZ + dy * dy;
                    if (distSq >= innerRS)
                    {
                        break;
                    }
                    mutablePos.set(cx + dx, y, cz + dz);
                    if (!level.getBlockState(mutablePos).isAir())
                    {
                        rememberRemovedBlock(level, mutablePos, removedBlocks);
                        level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                    }
                }

                // 2a-2. 清除该列地板以上球内范围内的掉落物
                clearItemsInColumn(level, cx, cz, dx, dz, playerFootY, maxY);

                // 2b. 放置地板（玩家脚下 Y 层）
                mutablePos.set(cx + dx, playerFootY, cz + dz);
                BlockState existingFloor = level.getBlockState(mutablePos);
                if (!existingFloor.is(getBarrierBlockState().getBlock()))
                {
                    removedBlocks.putIfAbsent(mutablePos.immutable(), existingFloor);
                    level.setBlock(mutablePos, getBarrierBlockState(), BLOCK_UPDATE_FLAGS);
                }

                // 2c. 从地板往下填充到球壳底部
                for (int y = playerFootY - 1; y >= minY; y--)
                {
                    int dy = y - cy;
                    int distSq = minDistXZ + dy * dy;
                    if (distSq >= innerRS)
                    {
                        break;
                    }
                    mutablePos.set(cx + dx, y, cz + dz);
                    BlockState existing = level.getBlockState(mutablePos);
                    if (!existing.is(getBarrierBlockState().getBlock()))
                    {
                        removedBlocks.putIfAbsent(mutablePos.immutable(), existing);
                        level.setBlock(mutablePos, getBarrierBlockState(), BLOCK_UPDATE_FLAGS);
                    }
                }
            }
        }
    }

    /**
     * 清除指定 (dx, dz) 列在地板上方球内范围内的掉落物实体。
     * <p>
     * 随着领域展开动画同步调用，实现掉落物跟随领域展开动态消失的效果。
     * 查找该列范围内所有 ItemEntity 并移除。
     *
     * @param level       服务端世界
     * @param cx          球心 X
     * @param cz          球心 Z
     * @param dx          相对球心的 X 偏移
     * @param dz          相对球心的 Z 偏移
     * @param playerFootY 地板 Y 坐标
     * @param maxY        该列球内的最大 Y
     */
    private void clearItemsInColumn(ServerLevel level, int cx, int cz, int dx, int dz, int playerFootY, int maxY)
    {
        // 构建该列从地板到球内顶部的 AABB
        double minX = cx + dx;
        double maxX = cx + dx + 1.0;
        double minY = playerFootY;
        double maxYBound = maxY + 1.0;
        double minZ = cz + dz;
        double maxZ = cz + dz + 1.0;
        AABB columnBox = new AABB(minX, minY, minZ, maxX, maxYBound, maxZ);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, columnBox);
        for (ItemEntity item : items)
        {
            item.discard();
        }
    }

    /**
     * 对球壳上一个方块位置，同步处理该 (dx, dz) 列的地板/填充/清空。
     * <p>
     * 随着球壳展开动画逐步放置，每个球壳方块放置时调用此方法，
     * 实现地板、填充和清空跟随球壳同步展开的效果。
     * 使用一个 Set 避免同一 (dx, dz) 列被重复处理。
     */
    private void processColumnForShellBlock(ServerLevel level, BlockPos shellPos, BlockPos center,
                                             int radius, int playerFootY, Map<BlockPos, BlockState> removedBlocks,
                                             Set<Long> processedColumns)
    {
        int dx = shellPos.getX() - center.getX();
        int dz = shellPos.getZ() - center.getZ();

        // 用 dx, dz 组合成 long 作为列的唯一标识
        long columnKey = ((long) dx << 32) | (dz & 0xFFFFFFFFL);
        if (!processedColumns.add(columnKey))
        {
            return; // 该列已处理过
        }

        // 该列到球心的水平距离平方
        int distXZSq = dx * dx + dz * dz;

        // 检查球内范围
        int innerRadius = radius - 1;
        int innerRS = innerRadius * innerRadius;
        if (distXZSq >= innerRS)
        {
            return; // 该列不在球内
        }

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int yRange = (int) Math.sqrt(innerRS - distXZSq);
        int minY = cy - yRange;
        int maxY = cy + yRange;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // 清空地板以上空间
        for (int y = playerFootY + 1; y <= maxY; y++)
        {
            int dy = y - cy;
            int distSq = distXZSq + dy * dy;
            if (distSq >= innerRS)
            {
                break;
            }
            mutablePos.set(cx + dx, y, cz + dz);
            if (!level.getBlockState(mutablePos).isAir())
            {
                rememberRemovedBlock(level, mutablePos, removedBlocks);
                level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
            }
        }

        // 清除该列地板以上球内范围内的掉落物
        clearItemsInColumn(level, cx, cz, dx, dz, playerFootY, maxY);

        // 放置地板
        mutablePos.set(cx + dx, playerFootY, cz + dz);
        BlockState existingFloor = level.getBlockState(mutablePos);
        if (!existingFloor.is(getBarrierBlockState().getBlock()))
        {
            removedBlocks.putIfAbsent(mutablePos.immutable(), existingFloor);
            level.setBlock(mutablePos, getBarrierBlockState(), BLOCK_UPDATE_FLAGS);
        }

        // 从地板往下填充到球壳底部
        for (int y = playerFootY - 1; y >= minY; y--)
        {
            int dy = y - cy;
            int distSq = distXZSq + dy * dy;
            if (distSq >= innerRS)
            {
                break;
            }
            mutablePos.set(cx + dx, y, cz + dz);
            BlockState existing = level.getBlockState(mutablePos);
            if (!existing.is(getBarrierBlockState().getBlock()))
            {
                removedBlocks.putIfAbsent(mutablePos.immutable(), existing);
                level.setBlock(mutablePos, getBarrierBlockState(), BLOCK_UPDATE_FLAGS);
            }
        }
    }

    /**
     * 处理领域展开的延迟展开动画（从背后向前方包围）。
     * <p>
     * 按排序后的顺序逐批放置球壳方块，每放一个球壳方块时，
     * 同步处理该列的地板铺设、地板以下填充和地板以上清空，
     * 实现地板和清空跟随球壳一同展开的动画效果。
     */
    private void tickDomainExpand(ServerPlayer player, ServerLevel level, BarrierInstance instance)
    {
        List<BlockPos> allBlocks = instance.getShellBlocks();
        int currentProgress = instance.getExpansionProgress();

        if (currentProgress >= allBlocks.size())
        {
            instance.setExpanding(false);
            instance.setExpansionProgress(allBlocks.size());
            return;
        }

        // 获取或初始化已处理的列集合
        Set<Long> processedColumns = instance.getProcessedColumns();
        if (processedColumns == null)
        {
            processedColumns = new HashSet<>();
            instance.setProcessedColumns(processedColumns);
        }

        int playerFootY = instance.getPlayerFootY();
        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();

        // 每tick放置一批方块
        int placedThisTick = 0;
        int newProgress = currentProgress;

        while (newProgress < allBlocks.size() && placedThisTick < BLOCKS_PER_BATCH)
        {
            BlockPos pos = allBlocks.get(newProgress);

            // 放置球壳方块
            if (!level.getBlockState(pos).isAir())
            {
                rememberRemovedBlockForPos(level, pos, instance.getRemovedBlocks());
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
            }
            level.setBlock(pos, getBarrierBlockState(), BLOCK_UPDATE_FLAGS);

            // 同步处理该列的地板/填充/清空
            processColumnForShellBlock(level, pos, center, radius, playerFootY,
                                       instance.getRemovedBlocks(), processedColumns);

            placedThisTick++;
            newProgress++;
        }

        instance.setExpansionProgress(newProgress);

        // 显示展开进度
        int totalBlocks = allBlocks.size();
        int percent = (int) Math.round(100.0 * newProgress / totalBlocks);
        String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
        player.displayClientMessage(
                Component.literal(nameDisplay + "构建中: " + percent + "%")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        // 检查展开是否完成
        if (newProgress >= allBlocks.size())
        {
            instance.setExpanding(false);
            // 展开完成后放置天空盒（替换地板层 + 壳内层）
            placeDomainSkybox(level, allBlocks, center, radius, instance.getPlayerFootY(),
                    instance.getRemovedBlocks(), instance);
            // 动画展开完成后触发领域专属完成效果。
            applyDomainSpecificEffect(player, instance);
            nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
            player.displayClientMessage(
                    Component.literal(nameDisplay + "领域构建完成")
                            .withStyle(ChatFormatting.GOLD), true);
        }
    }

    /**
     * 玩家位于自己领域内部时再次触发领域展开，视为解除领域。
     */
    private boolean closeDomainIfPlayerInside(ServerPlayer player)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
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
                String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                player.displayClientMessage(
                        Component.literal(nameDisplay + "已解除")
                                .withStyle(ChatFormatting.YELLOW), true);
                continue;
            }
            kept.addLast(instance);
        }

        if (kept.isEmpty())
        {
            ACTIVE_DOMAINS.remove(player.getUUID());
        }
        else
        {
            ACTIVE_DOMAINS.put(player.getUUID(), kept);
        }
        return closed;
    }

    /**
     * 检查玩家是否在任何领域内（包括自己和别人的领域）。
     */
    private boolean isPlayerInsideAnyDomain(ServerPlayer player)
    {
        return isPositionInAnyDomain(player.serverLevel(), player.blockPosition());
    }

    /**
     * 检查指定位置是否在任何活跃领域范围内。
     */
    private boolean isPositionInAnyDomain(ServerLevel level, BlockPos pos)
    {
        ResourceKey<Level> dimension = level.dimension();
        double px = pos.getX() + 0.5D;
        double py = pos.getY() + 0.5D;
        double pz = pos.getZ() + 0.5D;

        for (Deque<BarrierInstance> queue : ACTIVE_DOMAINS.values())
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
     * 领域生命值按 10% 阶段通知。
     */
    private void notifyDomainHealthIfNeeded(ServerPlayer player, BarrierInstance instance, double remainingPercent)
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
                String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                Component message = Component.literal(nameDisplay + "完整度降至 " + percent + "%");
                if (percent <= 70)
                {
                    message = message.copy().withStyle(ChatFormatting.RED);
                }
                player.displayClientMessage(message, true);
            }
        }
        instance.setLastNotifiedTenPercentStep(currentStep);
    }

    /**
     * 生得领域演出：即时建造领域球与天空盒，不注册 {@link #ACTIVE_DOMAINS}、不消耗咒力。
     */
    public static final class InnateArenaHandle
    {
        private final BarrierInstance instance;

        private InnateArenaHandle(BarrierInstance instance)
        {
            this.instance = instance;
        }
    }

    public final InnateArenaHandle buildInnateDomainArena(
            ServerPlayer player,
            ServerLevel level,
            BlockPos center,
            int radius,
            ResourceLocation skyboxTexture)
    {
        Map<BlockPos, BlockState> removedBlocks = new HashMap<>();
        List<BlockPos> shellBlocks = buildHollowSpherePositions(center, radius);
        if (shellBlocks.isEmpty())
        {
            return null;
        }
        int playerFootY = center.getY() - radius + 1;
        BarrierInstance instance = new BarrierInstance(
                level.dimension(),
                shellBlocks,
                removedBlocks,
                center,
                radius,
                false,
                10,
                shellBlocks.size(),
                false,
                0,
                0,
                playerFootY);
        placeAllDomainBlocks(level, shellBlocks, removedBlocks, center, radius, playerFootY);
        placeDomainSkyboxWithTexture(level, shellBlocks, center, radius, playerFootY, removedBlocks, instance,
                skyboxTexture);
        return new InnateArenaHandle(instance);
    }

    /** 拆除生得领域演出结界并恢复方块。 */
    public final void dismantleInnateDomainArena(ServerPlayer player, InnateArenaHandle handle)
    {
        if (handle != null && handle.instance != null)
        {
            clearBarrierInstance(player, handle.instance);
        }
    }
}
