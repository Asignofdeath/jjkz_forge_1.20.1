package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.effect.ConfiscationEffect;
import com.darkness.jujutsukaisenzone.effect.TechniqueMeltdownEffect;
import com.darkness.jujutsukaisenzone.compat.ShaderedCompat;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.BarrierRulesData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainExpansionData;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerBrainDamageData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainGaugeData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
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

    /** 领域展开球体半径（常规）。 */
    protected static final int DOMAIN_RADIUS = 24;
    /** 瞬发领域球体半径。 */
    protected static final int INSTANT_DOMAIN_RADIUS = 32;
    /** 开放式领域球体半径（开放式最小标准）。 */
    protected static final int OPEN_DOMAIN_RADIUS = 64;

    /** 领域同时存在数量上限 */
    private static final int MAX_ACTIVE_BARRIER_COUNT = 1;

    /** 瞬发领域构建完成后的完成态保持时长（2 秒 = 40 tick）。 */
    private static final int INSTANT_AUTO_CLOSE_HOLD_TICKS = 40;
    /** 瞬发关域后十字残留时长（约 3.5 秒）。 */
    private static final int INSTANT_CROSS_RESIDUAL_DURATION_TICKS = 70;
    /** 十字平面内自转：约 3 秒一圈。 */
    private static final float INSTANT_CROSS_RESIDUAL_SECONDS_PER_REV = 3.0F;
    /** 由时长与转速推出的总圈数。 */
    private static final float INSTANT_CROSS_RESIDUAL_REVOLUTIONS =
            (INSTANT_CROSS_RESIDUAL_DURATION_TICKS * 0.05F) / INSTANT_CROSS_RESIDUAL_SECONDS_PER_REV;
    /** 十字残留同步半径（格）。 */
    private static final double INSTANT_CROSS_RESIDUAL_SYNC_RADIUS = 64.0D;
    /** 十字锚点相对头顶再上抬（格）。 */
    private static final double INSTANT_CROSS_RESIDUAL_HEAD_LIFT = 0.35D;
    /** 十字锚点沿水平朝向后方的偏移（格）。 */
    private static final double INSTANT_CROSS_RESIDUAL_BACK_OFFSET = 0.65D;
    /** 瞬发关域瞬间屏幕强晃同步半径（格）。 */
    private static final double INSTANT_CLOSE_SCREEN_SHAKE_RADIUS = 32.0D;
    /** 瞬发关域瞬间屏幕单次冲击时长。 */
    private static final int INSTANT_CLOSE_SCREEN_SHAKE_TICKS = 10;
    /** 瞬发关域瞬间屏幕单次冲击最大角度。 */
    private static final float INSTANT_CLOSE_SCREEN_SHAKE_MAX_DEG = 10.0F;

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

    /** 延迟放置天空盒的倒计时（施法者 UUID → 剩余 tick）。 */
    private final ConcurrentMap<UUID, Integer> deferredSkyboxPlacementDelayByCaster = new ConcurrentHashMap<>();

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
     * 应用领域专属效果（玩家版）。
     * <p>
     * 基础领域无特殊效果，术式领域有专属效果（如必中、伤害加成等）。
     *
     * @param player   施术者（玩家）
     * @param instance 领域实例
     */
    protected abstract void applyDomainSpecificEffect(ServerPlayer player, BarrierInstance instance);

    /**
     * 应用领域专属效果（施法者无关版）：玩家与 AI 实体共用。
     * <p>
     * 默认实现：有玩家 → 转调 {@link #applyDomainSpecificEffect(ServerPlayer, BarrierInstance)}；
     * AI 实体（无玩家）→ 什么也不做。术式领域若要支持 AI 实体施法，覆写本方法并在
     * {@code caster.player() == null} 分支里做实体版演出 / 必中判定。
     */
    protected void applyDomainSpecificEffect(DomainCaster caster, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            applyDomainSpecificEffect(player, instance);
        }
    }

    /**
     * 关闭回调（施法者无关版）。默认实现转调玩家版（若有玩家），否则什么也不做。
     */
    protected void onBarrierClosed(DomainCaster caster, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            onBarrierClosed(player, instance);
        }
    }

    @Override
    protected void onBarrierClosed(ServerPlayer player, BarrierInstance instance)
    {
        deferredSkyboxPlacementDelayByCaster.remove(player.getUUID());
        ServerLevel level = player.server.getLevel(instance.getDimension());
        if (instance.isAutoCloseOnComplete() || instance.isSkyboxOnlyExpand())
        {
            /* 瞬发：关域后按停留时长 ×6 延续控制（施术者立刻解除）。 */
            InstantDomainSlowMo.beginAftermath(level, instance.getInstantSlowMoTargets(), player.getUUID());
        }
        else
        {
            InstantDomainSlowMo.clearActive(level, instance.getInstantSlowMoTargets());
        }
        if (shouldApplyMeltdownOnBarrierClosed(player, instance))
        {
            TechniqueMeltdownEffect.applyOnDomainClosed(player);
        }
    }

    /**
     * 是否在结界壳构建完成后延迟放置天空盒；默认即时放置。
     * 子类可覆盖以配合客户端演出（如世界层滤镜结束后再出现内壁天空盒）。
     */
    protected boolean deferDomainSkyboxPlacement()
    {
        return false;
    }

    /**
     * 结界构建完成到放置天空盒的延迟（tick）；仅当 {@link #deferDomainSkyboxPlacement()} 为 true 时生效。
     */
    protected int getDeferredDomainSkyboxPlacementDelayTicks()
    {
        return 0;
    }

    /** 领域关闭时是否对施术者施加术式熔断；默认 true。 */
    protected boolean shouldApplyMeltdownOnBarrierClosed(ServerPlayer player, BarrierInstance instance)
    {
        return true;
    }

    /**
     * 领域实例入队后的钩子（玩家版）：子类可标记瞬发共展天空盒 / 构建完即关等。
     */
    protected void configureDomainInstance(ServerPlayer player, BarrierInstance instance)
    {
    }

    /**
     * 领域实例入队后的钩子（施法者无关版）。默认：有玩家 → 转调玩家版；AI 实体 → 无操作。
     */
    protected void configureDomainInstance(DomainCaster caster, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            configureDomainInstance(player, instance);
        }
    }

    /**
     * 瞬发领域（天空盒-only / 构建完即关）展开进度<strong>首次过半</strong>时的钩子（玩家版）：
     * 子类可在此挂演出（如球心 Photon）。比等到 100% 构建完成更早，给短命瞬发领域留出展示时间。
     * <p>
     * 瞬发路径不经过 {@link #applyDomainSpecificEffect(ServerPlayer, BarrierInstance)}，故演出需挂此处。
     * 同一次展开只触发一次。
     */
    protected void onInstantDomainHalfProgress(ServerPlayer player, ServerLevel level, BarrierInstance instance)
    {
    }

    /**
     * 瞬发过半钩子（施法者无关版）。默认：有玩家 → 转调玩家版；AI 实体 → 无操作。
     */
    protected void onInstantDomainHalfProgress(DomainCaster caster, ServerLevel level, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            onInstantDomainHalfProgress(player, level, instance);
        }
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
        cast(DomainCaster.of(player));
    }

    /**
     * 领域展开（施法者无关版）：玩家与 AI 实体共用同一条真实展开路径。
     * <p>
     * 与旧 {@code cast(ServerPlayer)} 的唯一差别是「数值来源」与「玩家专属演出」都走
     * {@link DomainCaster}：咒力 / 战斗模式 / 模式选择由实现决定，输入锁、脑损、
     * 领域量表、提示文案等只在 {@code caster.player() != null} 时执行。
     */
    public void cast(DomainCaster caster)
    {
        ServerPlayer player = caster.player();
        if (!caster.alive() || !caster.inBattleMode())
        {
            return;
        }

        // 如果玩家已在自己的领域内，解除领域（Alt+结界术键 = 解除领域）
        if (closeDomainIfCasterInside(caster))
        {
            return;
        }

        if (player != null)
        {
            if (TechniqueMeltdownEffect.blockDomainExpansion(player))
            {
                return;
            }

            if (ConfiscationEffect.blockDomainExpansion(player))
            {
                return;
            }
        }

        // 检查施法者是否在任何领域内（包括别人的领域），领域内无法展开领域
        if (isPositionInAnyDomain(caster.level(), caster.blockPosition()))
        {
            if (player != null)
            {
                player.displayClientMessage(Component.literal("无法展开领域，因为领域对抗还没做").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        // 检查施法者是否在普通结界内，结界内无法展开领域
        if (caster.level() != null
                && BarrierTechniqueServerLogic.getInstance().isPositionInBarrier(caster.level(), caster.blockPosition()))
        {
            if (player != null)
            {
                player.displayClientMessage(Component.literal("结界内无法展开领域").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        // 检查是否习得领域展开
        if (!hasLearnedDomainExpansion(caster))
        {
            if (player != null)
            {
                player.displayClientMessage(Component.literal("尚未习得领域展开").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        // 检查是否习得结界术（前置条件）
        if (!hasLearnedBarrierTechnique(caster))
        {
            if (player != null)
            {
                player.displayClientMessage(Component.literal("需要先习得结界术").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        // 领域展开半径：开放式 64 / 瞬发 32 / 常规 DOMAIN_RADIUS
        int radius;
        if (isOpenDomainMode(caster))
        {
            radius = OPEN_DOMAIN_RADIUS;
        }
        else if (isInstantDomainMode(caster))
        {
            radius = INSTANT_DOMAIN_RADIUS;
        }
        else
        {
            radius = DOMAIN_RADIUS;
        }

        // 脑损满则禁止展开（强开受伤）；仅玩家
        if (player != null && !PlayerBrainDamageData.tryAllowDomainExpansion(player))
        {
            return;
        }
        // 领域量表未满则禁止展开；仅玩家
        if (player != null && !PlayerDomainGaugeData.tryAllowDomainExpansion(player))
        {
            return;
        }


        // 检查并消耗展开领域所需的咒力
        int expansionCost = calculateExpansionCost(0);
        if (!caster.consumeCursedEnergy(expansionCost))
        {
            if (player != null)
            {
                player.displayClientMessage(
                        Component.literal("咒力不足，无法展开领域（需要 " + expansionCost + "）")
                                .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        ServerLevel level = caster.level();
        if (level == null)
        {
            return;
        }
        BlockPos center = caster.blockPosition();
        int playerFootY = caster.footBlockY();
        Map<BlockPos, BlockState> removedBlocks = new HashMap<>();

        boolean openDomain = isOpenDomainMode(caster);
        List<BlockPos> shellBlocks = openDomain
                ? List.of()
                : buildHollowSpherePositions(center, radius);
        if (!openDomain && shellBlocks.isEmpty())
        {
            return;
        }

        // 领域展开始终使用延迟展开动画（从背后向前方包围）；开放式无壳，直接完成。
        boolean animatedExpand = !openDomain && radius > ANIMATED_EXPAND_THRESHOLD;

        // 排序（从背后向前方包围）
        if (!openDomain)
        {
            shellBlocks = sortShellBlocksForExpansion(shellBlocks, center, caster);
        }

        // 读取施法者的规则设置
        boolean blockMobSpawn = player != null
                ? BarrierRulesData.isBlockMobSpawn(player.getUUID())
                : false;

        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.computeIfAbsent(caster.uid(), key -> new ArrayDeque<>());

        BarrierInstance instance = new BarrierInstance(
                level.dimension(),
                shellBlocks,
                removedBlocks,
                center,
                radius,
                blockMobSpawn,
                10,
                animatedExpand ? 0 : Math.max(shellBlocks.size(), openDomain ? 1 : 0),
                animatedExpand,
                0,
                expansionCost,
                playerFootY
        );

        queue.addLast(instance);

        // 今日次数 / 超额脑损（关域不算，仅成功展开）；仅玩家
        if (player != null)
        {
            PlayerBrainDamageData.onDomainExpanded(player);
            PlayerDomainGaugeData.consumeOnExpand(player);
        }

        configureDomainInstance(caster, instance);
        if (instance.isOpenDomain())
        {
            instance.setExpanding(false);
            applyDomainSpecificEffect(caster, instance);
            if (player != null)
            {
                String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                player.displayClientMessage(
                        Component.literal(nameDisplay + "开放式领域展开（半径 " + radius + "）")
                                .withStyle(ChatFormatting.GOLD), true);
            }
        }
        else if (instance.isAutoCloseOnComplete())
        {
            InstantDomainSlowMo.refresh(level, instance.getCenter(), instance.getInstantSlowMoTargets());
        }

        // 领域只能同时存在一个
        while (queue.size() > MAX_ACTIVE_BARRIER_COUNT)
        {
            BarrierInstance oldest = queue.pollFirst();
            if (oldest != null)
            {
                clearBarrierInstance(caster, oldest);
            }
        }

        if (queue.isEmpty())
        {
            ACTIVE_DOMAINS.remove(caster.uid());
        }

        if (instance.isOpenDomain())
        {
            return;
        }

        // 领域展开固定清除内部方块
        if (!animatedExpand)
        {
            // 即时展开：放置所有方块、生成地板、填充地板以下、清空地板以上
            placeAllDomainBlocks(level, shellBlocks, removedBlocks, center, radius, playerFootY);
            completeDomainShellExpansion(caster, level, instance, shellBlocks, center, radius, playerFootY, removedBlocks);
            if (instance.isDiscardAfterTick())
            {
                return;
            }
            if (player != null)
            {
                String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                player.displayClientMessage(
                        Component.literal(nameDisplay + "领域展开完成（半径 " + radius + "）")
                                .withStyle(ChatFormatting.GOLD), true);
            }
        }
        else if (player != null)
        {
            String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
            player.displayClientMessage(
                    Component.literal(nameDisplay + "领域展开中（半径 " + radius + "）")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    // ==================== 施法者抽象适配（玩家 / AI 实体通用） ====================

    /**
     * 该施法者是否已习得「领域展开」。默认走玩家存档；{@link com.darkness.jujutsukaisenzone.technique.domain.DomainCaster.EntityCaster}
     * 由术式子类覆盖（AI 术师默认视为已习得，前提是它已被赋予该术式）。
     */
    protected boolean hasLearnedDomainExpansion(DomainCaster caster)
    {
        ServerPlayer player = caster.player();
        return player != null && hasLearnedDomainExpansion(player);
    }

    /**
     * 该施法者是否已习得「结界术」前置。默认走玩家存档；AI 实体默认 true。
     */
    protected boolean hasLearnedBarrierTechnique(DomainCaster caster)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            return PlayerBarrierTechniqueData.hasLearned(player);
        }
        return false;
    }

    /**
     * 是否「开放式」模式。玩家读圆盘；AI 术师读 {@link AiDomainModeData}。
     */
    protected boolean isOpenDomainMode(DomainCaster caster)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            return PlayerDomainModeData.isOpen(player);
        }
        return AiDomainModeData.isOpen(caster.entity());
    }

    /**
     * 是否「瞬发」模式。玩家读圆盘；AI 术师读 {@link AiDomainModeData}。
     */
    protected boolean isInstantDomainMode(DomainCaster caster)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            return PlayerDomainModeData.isInstant(player);
        }
        return AiDomainModeData.isInstant(caster.entity());
    }

    /**
     * 瞬间构建领域结界与天空盒并触发 {@link #applyDomainSpecificEffect}（无展开动画）。
     *
     * @param consumeCursedEnergy 是否消耗展开咒力
     * @return 是否成功创建领域实例
     */
    protected final boolean castAndCompleteDomainInstantly(ServerPlayer player, boolean consumeCursedEnergy)
    {
        if (!player.isAlive())
        {
            return false;
        }

        if (isPlayerInsideAnyDomain(player))
        {
            return false;
        }

        if (BarrierTechniqueServerLogic.getInstance().isPositionInBarrier(player.serverLevel(), player.blockPosition()))
        {
            return false;
        }

        if (!hasLearnedDomainExpansion(player))
        {
            return false;
        }

        if (!PlayerBarrierTechniqueData.hasLearned(player))
        {
            return false;
        }

        int radius = DOMAIN_RADIUS;
        int expansionCost = calculateExpansionCost(0);
        if (consumeCursedEnergy && !PlayerCursedEnergyData.tryConsume(player, expansionCost))
        {
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int playerFootY = player.getBlockY() - 1;
        Map<BlockPos, BlockState> removedBlocks = new HashMap<>();

        List<BlockPos> shellBlocks = buildHollowSpherePositions(center, radius);
        if (shellBlocks.isEmpty())
        {
            return false;
        }

        shellBlocks = sortShellBlocksForExpansion(shellBlocks, center, player);
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
                shellBlocks.size(),
                false,
                0,
                expansionCost,
                playerFootY);

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
            ACTIVE_DOMAINS.remove(player.getUUID());
            return false;
        }

        placeAllDomainBlocks(level, shellBlocks, removedBlocks, center, radius, playerFootY);
        completeDomainShellExpansion(DomainCaster.of(player), level, instance, shellBlocks, center, radius, playerFootY, removedBlocks);
        return true;
    }

    /**
     * 领域维持（施法者入口，{@link IBarrierTechnique} 契约）。
     * <p>
     * 默认实现<b>转发</b>到 {@link #tickMaintain(DomainCaster)}，使玩家与 AI 实体走同一套维持逻辑。
     * <p>
     * ⛔⛔ <b>子类覆写本方法时必须注意「虚调用环」</b>：本方法与 {@link #tickMaintain(DomainCaster)}
     * 互相可达（本方法 → DC 版；而子类若覆写了 DC 版、其玩家分支又回调本方法，则 DC 版 → 本方法）。
     * 一旦两侧同时被覆写且互相转发，就会形成「SP 版 → DC 版 → SP 版」的无限递归（{@code StackOverflowError}），
     * 且<b>编译期完全无法发现</b>。
     * <p>
     * <b>正确做法</b>：共用逻辑抽成<b>私有非虚</b>方法（如 {@code LimitlessDomainExpansion#tickMaintainPlayerSide}），
     * 两条入口都直达该私有方法；子类覆写 DC 版的玩家分支<b>严禁</b>写
     * {@code tickMaintain(player)} / {@code super.tickMaintain(player)}。
     * <p>
     * 参考实现：{@code LimitlessDomainExpansion}（唯一覆写 DC 版的领域，已按此规矩处理）。
     * 其余领域（{@code ShrineDomainExpansion} / {@code LoveRendezvousDomainExpansion}/
     * {@code JudgmentDomainExpansion}）只覆写 SP 版，故不涉及此陷阱。
     */
    @Override
    public void tickMaintain(ServerPlayer player)
    {
        tickMaintain(DomainCaster.of(player));
    }

    /**
     * 领域维持（施法者无关版）：玩家与 AI 实体共用。
     * <p>
     * 所有玩家专属副作用（提示文案、瞬发慢镜头标记、延迟天空盒）在 {@code caster.player() == null}
     * 时自动跳过；咒力消耗走 {@link DomainCaster#consumeCursedEnergy(int)}。
     * <p>
     * ⛔ <b>覆写本方法的子类请先阅读 {@link #tickMaintain(ServerPlayer)} 的「虚调用环」警告</b>：
     * 本方法与 SP 版互相可达，双方同时覆写并互相转发会导致无限递归。
     * <p>
     * 基类本实现是<b>纯实现</b>（不回调 SP 版），因此「只覆写 SP 版」的子类（Shrine / Love / Judgment）
     * 由基类转发进入本方法后即终止，不会成环；只有「同时覆写 DC 版」的子类才有风险。
     */
    public void tickMaintain(DomainCaster caster)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            tickDeferredDomainSkyboxPlacement(player);
        }
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(caster.uid());
        if (queue == null || queue.isEmpty())
        {
            return;
        }

        Deque<BarrierInstance> kept = new ArrayDeque<>();
        boolean collapsedNotified = false;

        for (BarrierInstance instance : queue)
        {
            ServerLevel level = caster.level();
            if (level == null || !level.dimension().equals(instance.getDimension()))
            {
                ServerLevel byKey = level != null && level.getServer() != null
                        ? level.getServer().getLevel(instance.getDimension())
                        : null;
                if (byKey == null)
                {
                    continue;
                }
                level = byKey;
            }

            // 处理延迟展开动画
            if (instance.isExpanding())
            {
                tickDomainExpand(caster, level, instance);
                if (instance.isAutoCloseOnComplete() && !instance.isDiscardAfterTick())
                {
                    InstantDomainSlowMo.refresh(level, instance.getCenter(), instance.getInstantSlowMoTargets());
                }
                if (instance.isDiscardAfterTick())
                {
                    continue;
                }
                kept.addLast(instance);
                continue;
            }

            // 瞬发：构建完成后保持完成态若干 tick，再自动关闭（期间不跑完整度/咒力维持，避免天空盒-only 误崩）
            if (instance.getAutoCloseHoldTicksRemaining() > 0)
            {
                InstantDomainSlowMo.refresh(level, instance.getCenter(), instance.getInstantSlowMoTargets());
                int remaining = instance.getAutoCloseHoldTicksRemaining() - 1;
                instance.setAutoCloseHoldTicksRemaining(remaining);
                if (remaining <= 0)
                {
                    finishInstantAutoClose(caster, instance);
                    continue;
                }
                kept.addLast(instance);
                continue;
            }

            // 施法者离开领域 → 领域立刻崩解
            if (!isCasterInsideBarrier(caster, instance))
            {
                if (player != null)
                {
                    String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                    player.displayClientMessage(
                            Component.literal(nameDisplay + "离开领域范围，领域崩解")
                                    .withStyle(ChatFormatting.RED), true);
                }
                clearBarrierInstance(caster, instance);
                continue;
            }

            // 消耗维持领域所需的咒力
            double costPerTick = (MAINTAIN_COST_BASE_PER_SECOND + instance.getRadius() * MAINTAIN_COST_PER_RADIUS_PER_SECOND) / TICKS_PER_SECOND;
            double reduction = caster.maintainCostReduction();
            costPerTick = costPerTick * (1.0 - reduction);
            int costThisTick = (int) Math.ceil(costPerTick);
            if (costThisTick > 0)
            {
                if (!caster.consumeCursedEnergy(costThisTick))
                {
                    if (player != null && !collapsedNotified)
                    {
                        String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                        player.displayClientMessage(
                                Component.literal(nameDisplay + "咒力耗尽，领域崩解")
                                        .withStyle(ChatFormatting.RED), true);
                        collapsedNotified = true;
                    }
                    clearBarrierInstance(caster, instance);
                    continue;
                }
            }

            // 领域生命值追踪（开放式无壳，跳过）
            if (!instance.isOpenDomain())
            {
                double remainingPercent = calculateRemainingPercent(level, instance);
                if (remainingPercent < MIN_MAINTAIN_PERCENT)
                {
                    if (player != null && !collapsedNotified && isCasterInsideBarrier(caster, instance))
                    {
                        String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                        player.displayClientMessage(
                                Component.literal(nameDisplay + "已崩解")
                                        .withStyle(ChatFormatting.RED), true);
                        collapsedNotified = true;
                    }
                    clearBarrierInstance(caster, instance);
                    continue;
                }

                if (player != null)
                {
                    notifyDomainHealthIfNeeded(player, instance, remainingPercent);
                }
            }
            else
            {
                tickOpenDomainMaintenance(caster, instance);
            }
            kept.addLast(instance);
        }

        if (kept.isEmpty())
        {
            ACTIVE_DOMAINS.remove(caster.uid());
            return;
        }
        ACTIVE_DOMAINS.put(caster.uid(), kept);
    }

    /**
     * 施法者是否位于领域球内（玩家 / AI 实体通用）。
     */
    protected final boolean isCasterInsideBarrier(DomainCaster caster, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            return isPlayerInsideBarrier(player, instance);
        }
        if (!instance.getDimension().equals(caster.dimension()))
        {
            return false;
        }
        Vec3 pos = caster.position();
        double cx = instance.getCenter().getX() + 0.5D;
        double cy = instance.getCenter().getY() + 0.5D;
        double cz = instance.getCenter().getZ() + 0.5D;
        double dx = pos.x - cx;
        double dy = pos.y - cy;
        double dz = pos.z - cz;
        double radiusSquared = instance.getRadius() * instance.getRadius();
        return dx * dx + dy * dy + dz * dz < radiusSquared;
    }

    /**
     * 开放式领域每 tick 钩子（穿出结算等）；默认空。
     */
    protected void tickOpenDomainMaintenance(ServerPlayer player, BarrierInstance instance)
    {
    }

    /**
     * 开放式领域每 tick 钩子（施法者无关版）。默认转调玩家版（若有玩家）。
     */
    protected void tickOpenDomainMaintenance(DomainCaster caster, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            tickOpenDomainMaintenance(player, instance);
        }
    }

    /**
     * 该玩家是否仍持有本实现下的活跃领域（含展开动画中）。
     * <p>
     * 领域主人在维持领域期间应停掉领域量表灌充；展开动画尚未完成时同样算「在领域中」。
     */
    public boolean hasActiveDomain(ServerPlayer owner)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        return queue != null && !queue.isEmpty();
    }

    /**
     * 该 UUID 是否仍持有本实现下的活跃领域（玩家 / AI 实体通用）。
     */
    public boolean hasActiveDomain(UUID ownerUuid)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(ownerUuid);
        return queue != null && !queue.isEmpty();
    }

    /**
     * 释放本领域实现持有的全部会话级运行态（活跃领域队列 + 延迟天空盒倒计时）。
     * <p>
     * 领域实现是进程级静态单例（见 {@link DomainExpansionRouter}），退出世界 / 切换存档不会重新加载类；
     * 世界退出时若领域仍活跃，其自然关闭路径 {@code clearBarrierInstance} 不会执行，条目会跨会话残留。
     * 故由 {@link DomainExpansionRouter#resetAllSessionState()} 在服务器停机时统一调用此方法。
     */
    public void resetSessionState()
    {
        ACTIVE_DOMAINS.clear();
        deferredSkyboxPlacementDelayByCaster.clear();
        InstantDomainSlowMo.clearAll(null);
    }

    /**
     * 关闭该施术者当前持有的全部领域（含 {@link #onBarrierClosed} 回调与方块还原），并从活跃表移除。
     * <p>
     * 用于玩家登出等「会话结束」场合：既还原被领域替换的方块，又触发子类回调清理按施术者
     * UUID 记录的各种状态，避免「没有领域却仍然必中 / 仍然连锁」的幽灵状态。
     */
    public void closeAllForPlayer(ServerPlayer player)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.remove(player.getUUID());
        if (queue == null)
        {
            return;
        }
        for (BarrierInstance instance : queue)
        {
            clearBarrierInstance(player, instance);
        }
    }

    /**
     * 关闭该施法者当前持有的全部领域（施法者无关版）：玩家与 AI 实体共用。
     * <p>
     * 与 {@link #closeAllForPlayer(ServerPlayer)} 的唯一差别是关域回调与方块还原都走 {@code caster}，
     * 使 AI 实体的领域也能正常撤除并<b>实时复原地形</b>（含天空盒、球壳、被替换方块）。
     */
    public void closeAllForCaster(DomainCaster caster)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.remove(caster.uid());
        if (queue == null)
        {
            return;
        }
        for (BarrierInstance instance : queue)
        {
            clearBarrierInstance(caster, instance);
        }
    }

    /**
     * 对仍残留在活跃表中的全部领域实例做世界侧方块还原（不触发回调、不移除条目）。
     * <p>
     * 供服务器停机兜底：所有者已离线、来不及走正常关闭路径的实例仍能被还原。
     * 调用后应由 {@link #resetSessionState()} 清空活跃表。
     */
    public void restoreAllWorld(MinecraftServer server)
    {
        for (Deque<BarrierInstance> queue : ACTIVE_DOMAINS.values())
        {
            for (BarrierInstance instance : queue)
            {
                ServerLevel level = server.getLevel(instance.getDimension());
                InstantDomainSlowMo.clearActive(level, instance.getInstantSlowMoTargets());
                restoreBarrierWorld(level, instance);
            }
        }
        InstantDomainSlowMo.clearAll(server);
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
     * 生得领域演出用的天空盒纹理；默认与 {@link #getDomainSkyboxTexture()} 相同。
     * 正式领域若使用阶段性天空盒切换，子类可在此指定演出专用纹理。
     */
    public ResourceLocation resolveInnateDomainSkyboxTexture(ServerPlayer player)
    {
        return getDomainSkyboxTexture();
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
                    rememberRemovedBlock(level, mutablePos, removedBlocks);
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
                        rememberRemovedBlock(level, mutablePos, removedBlocks);
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
            rememberRemovedBlock(level, mutablePos, removedBlocks);
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
                rememberRemovedBlock(level, mutablePos, removedBlocks);
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
    private void tickDomainExpand(DomainCaster caster, ServerLevel level, BarrierInstance instance)
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

        // 每 tick 放置一批
        int batchLimit = resolveExpandBatchLimit(instance);
        int placedThisTick = 0;
        int newProgress = currentProgress;

        if (instance.isSkyboxOnlyExpand())
        {
            tickSkyboxOnlyExpand(caster, level, instance, allBlocks, processedColumns,
                    center, radius, playerFootY, batchLimit, currentProgress);
            return;
        }

        while (newProgress < allBlocks.size() && placedThisTick < batchLimit)
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
        ServerPlayer player = caster.player();
        if (player != null)
        {
            int percent = (int) Math.round(100.0 * newProgress / totalBlocks);
            String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
            player.displayClientMessage(
                    Component.literal(nameDisplay + "构建中: " + percent + "%")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }

        // 检查展开是否完成
        if (newProgress >= allBlocks.size())
        {
            instance.setExpanding(false);
            completeDomainShellExpansion(
                    caster,
                    level,
                    instance,
                    allBlocks,
                    center,
                    radius,
                    instance.getPlayerFootY(),
                    instance.getRemovedBlocks());
            if (player != null && !instance.isDiscardAfterTick())
            {
                String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                player.displayClientMessage(
                        Component.literal(nameDisplay + "领域构建完成")
                                .withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    /**
     * 瞬发：按球壳顺序只放天空盒（内侧 + 地板），不放结界/领域壳与填充。
     */
    private void tickSkyboxOnlyExpand(
            DomainCaster caster,
            ServerLevel level,
            BarrierInstance instance,
            List<BlockPos> allBlocks,
            Set<Long> processedColumns,
            BlockPos center,
            int radius,
            int playerFootY,
            int batchLimit,
            int currentProgress)
    {
        ResourceLocation texture = instance.getCustomSkyboxTexture() != null
                ? instance.getCustomSkyboxTexture()
                : getDomainSkyboxTexture();
        instance.setCustomSkyboxTexture(texture);
        Set<BlockPos> shellSet = instance.getOrCreateShellBlockSet();

        int placedThisTick = 0;
        int newProgress = currentProgress;
        while (newProgress < allBlocks.size() && placedThisTick < batchLimit)
        {
            BlockPos shellPos = allBlocks.get(newProgress);
            placeSkyboxOnlyForShellStep(level, shellPos, center, radius, playerFootY, instance, shellSet, processedColumns, texture);
            placedThisTick++;
            newProgress++;
        }

        instance.setExpansionProgress(newProgress);

        int totalBlocks = allBlocks.size();
        maybeNotifyInstantHalfProgress(caster, level, instance, newProgress, totalBlocks);
        ServerPlayer player = caster.player();
        if (player != null)
        {
            int percent = (int) Math.round(100.0 * newProgress / totalBlocks);
            String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
            player.displayClientMessage(
                    Component.literal(nameDisplay + "构建中: " + percent + "%")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }

        if (newProgress >= allBlocks.size())
        {
            instance.setExpanding(false);
            completeDomainShellExpansion(
                    caster,
                    level,
                    instance,
                    allBlocks,
                    center,
                    radius,
                    playerFootY,
                    instance.getRemovedBlocks());
        }
    }

    /**
     * 瞬发展开进度首次过半时触发一次 {@link #onInstantDomainHalfProgress}。
     */
    private void maybeNotifyInstantHalfProgress(DomainCaster caster, ServerLevel level, BarrierInstance instance,
                                                int progress, int totalBlocks)
    {
        if (instance.isInstantHalfProgressNotified() || totalBlocks <= 0 || progress * 2 < totalBlocks)
        {
            return;
        }
        instance.setInstantHalfProgressNotified(true);
        onInstantDomainHalfProgress(caster, level, instance);
    }

    /**
     * 瞬发领域（天空盒-only / 构建完即关）用 {@link #INSTANT_BLOCKS_PER_BATCH}；
     * 常规领域仍用 {@link #BLOCKS_PER_BATCH}。
     */
    private static int resolveExpandBatchLimit(BarrierInstance instance)
    {
        if (instance.isSkyboxOnlyExpand() || instance.isAutoCloseOnComplete())
        {
            return INSTANT_BLOCKS_PER_BATCH;
        }
        return BLOCKS_PER_BATCH;
    }

    /**
     * 结界壳构建完成：放置天空盒（或登记延迟放置）并触发领域专属完成效果。
     * 瞬发模式进入完成态保持，倒计时结束后自动关闭。
     */
    private void completeDomainShellExpansion(
            DomainCaster caster,
            ServerLevel level,
            BarrierInstance instance,
            List<BlockPos> shellBlocks,
            BlockPos center,
            int radius,
            int playerFootY,
            Map<BlockPos, BlockState> removedBlocks)
    {
        ServerPlayer player = caster.player();
        if (!instance.isSkyboxOnlyExpand())
        {
            if (deferDomainSkyboxPlacement() && player != null)
            {
                scheduleDeferredDomainSkyboxPlacement(player);
            }
            else
            {
                placeDomainSkybox(level, shellBlocks, center, radius, playerFootY, removedBlocks, instance);
            }
        }
        else
        {
            /* 天空盒-only 路径已在展开过程中放置完毕。 */
            if (instance.getCustomSkyboxTexture() == null)
            {
                instance.setCustomSkyboxTexture(getDomainSkyboxTexture());
            }
        }

        if (instance.isAutoCloseOnComplete())
        {
            /* 进入完成态保持，慢镜头继续；倒计时结束后再关。 */
            instance.setAutoCloseHoldTicksRemaining(INSTANT_AUTO_CLOSE_HOLD_TICKS);
            if (player != null)
            {
                String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                player.displayClientMessage(
                        Component.literal(nameDisplay + "领域构建完成")
                                .withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }

        applyDomainSpecificEffect(caster, instance);
    }

    /** 瞬发完成态倒计时结束：关域、熔断路径走 {@link #clearBarrierInstance}。 */
    private void finishInstantAutoClose(DomainCaster caster, BarrierInstance instance)
    {
        String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
        ResourceLocation residualTexture = instance.getCustomSkyboxTexture() != null
                ? instance.getCustomSkyboxTexture()
                : DEFAULT_SKYBOX_TEXTURE;
        clearBarrierInstance(caster, instance);
        instance.setDiscardAfterTick(true);
        ServerPlayer player = caster.player();
        if (player != null)
        {
            broadcastInstantCloseScreenShake(player);
            broadcastInstantDomainCrossResidual(player, residualTexture);
            player.displayClientMessage(
                    Component.literal(nameDisplay + "瞬发领域已关闭")
                            .withStyle(ChatFormatting.AQUA), true);
        }
    }

    /**
     * 瞬发关域瞬间：向半径 32 内玩家发送单次冲击晃动（非连震）。
     */
    private void broadcastInstantCloseScreenShake(ServerPlayer caster)
    {
        ServerLevel level = caster.serverLevel();
        double cx = caster.getX();
        double cy = caster.getY();
        double cz = caster.getZ();
        double radiusSqr = INSTANT_CLOSE_SCREEN_SHAKE_RADIUS * INSTANT_CLOSE_SCREEN_SHAKE_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            double dx = nearby.getX() - cx;
            double dy = nearby.getY() - cy;
            double dz = nearby.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendScreenShake(
                    nearby,
                    ScreenShakeKind.SINGLE,
                    INSTANT_CLOSE_SCREEN_SHAKE_TICKS,
                    INSTANT_CLOSE_SCREEN_SHAKE_MAX_DEG
            );
        }
    }

    /**
     * 瞬发关域后：向附近玩家同步十字窗口残留（窗内为领域立方体贴图）。
     * 残留锚点为施术者头顶略后方；熔断等关域代价已由 {@link #clearBarrierInstance} 处理。
     */
    private void broadcastInstantDomainCrossResidual(
            ServerPlayer caster,
            ResourceLocation textureFolder)
    {
        ServerLevel level = caster.serverLevel();
        /* 水平朝向的后方单位向量（忽略俯仰，避免低头时锚点扎进身体）。 */
        float yawRad = caster.getYRot() * ((float) Math.PI / 180.0F);
        double backX = Math.sin(yawRad);
        double backZ = -Math.cos(yawRad);
        double cx = caster.getX() + backX * INSTANT_CROSS_RESIDUAL_BACK_OFFSET;
        double cy = caster.getY() + caster.getBbHeight() + INSTANT_CROSS_RESIDUAL_HEAD_LIFT;
        double cz = caster.getZ() + backZ * INSTANT_CROSS_RESIDUAL_BACK_OFFSET;
        double radiusSqr = INSTANT_CROSS_RESIDUAL_SYNC_RADIUS * INSTANT_CROSS_RESIDUAL_SYNC_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            double dx = nearby.getX() - cx;
            double dy = nearby.getY() - cy;
            double dz = nearby.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendInstantDomainCrossResidualSync(
                    nearby,
                    cx,
                    cy,
                    cz,
                    INSTANT_CROSS_RESIDUAL_DURATION_TICKS,
                    INSTANT_CROSS_RESIDUAL_REVOLUTIONS,
                    textureFolder
            );
        }
    }

    private void removeDomainInstanceFromActive(UUID ownerUuid, BarrierInstance instance)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(ownerUuid);
        if (queue == null)
        {
            return;
        }
        queue.remove(instance);
        if (queue.isEmpty())
        {
            ACTIVE_DOMAINS.remove(ownerUuid);
        }
    }

    /**
     * 瞬发单步：为当前壳格放置内侧天空盒；该列首次处理时再铺地板天空盒。
     * 不放置任何结界/领域壳或地下填充。
     */
    private void placeSkyboxOnlyForShellStep(
            ServerLevel level,
            BlockPos shellPos,
            BlockPos center,
            int radius,
            int playerFootY,
            BarrierInstance instance,
            Set<BlockPos> shellSet,
            Set<Long> processedColumns,
            ResourceLocation texture)
    {
        BlockPos innerPos = resolveShellInnerSkyboxPos(shellPos, center);
        if (innerPos != null)
        {
            tryPlaceSkyboxAt(level, innerPos, texture, instance, shellSet);
        }

        int dx = shellPos.getX() - center.getX();
        int dz = shellPos.getZ() - center.getZ();
        long columnKey = ((long) dx << 32) | (dz & 0xFFFFFFFFL);
        if (!processedColumns.add(columnKey))
        {
            return;
        }

        int innerRadius = radius - 1;
        int distXZSq = dx * dx + dz * dz;
        int dyFloor = playerFootY - center.getY();
        if (distXZSq + dyFloor * dyFloor >= innerRadius * innerRadius)
        {
            return;
        }

        BlockPos floorPos = new BlockPos(shellPos.getX(), playerFootY, shellPos.getZ());
        tryPlaceSkyboxAt(level, floorPos, texture, instance, shellSet);
    }

    /**
     * 在指定位置放置天空盒（跳过壳格几何点与已有天空盒）；原地形记入还原表。
     */
    private void tryPlaceSkyboxAt(
            ServerLevel level,
            BlockPos pos,
            ResourceLocation texture,
            BarrierInstance instance,
            Set<BlockPos> shellSet)
    {
        BlockPos immutable = pos.immutable();
        if (shellSet.contains(immutable))
        {
            return;
        }
        BlockState existing = level.getBlockState(immutable);
        if (ShaderedCompat.isSkyboxBlock(existing))
        {
            instance.trackSkyboxBlock(immutable);
            return;
        }

        var jjkzSkyblock = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "jjkz_skyblock"));
        if (jjkzSkyblock == null)
        {
            return;
        }

        rememberRemovedBlockForPos(level, immutable, instance.getRemovedBlocks());
        level.setBlock(immutable, jjkzSkyblock.defaultBlockState(), BLOCK_UPDATE_FLAGS);
        if (level.getBlockEntity(immutable) instanceof com.darkness.jujutsukaisenzone.blockentity.JJKZSkyblockEntity sky)
        {
            sky.setTextureFolder(texture);
        }
        instance.trackSkyboxBlock(immutable);
    }

    @Nullable
    private static BlockPos resolveShellInnerSkyboxPos(BlockPos shellPos, BlockPos center)
    {
        int dx = shellPos.getX() - center.getX();
        int dy = shellPos.getY() - center.getY();
        int dz = shellPos.getZ() - center.getZ();
        double length = Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
        if (length < 0.001D)
        {
            return null;
        }
        int innerX = shellPos.getX() - (int) Math.round(dx / length);
        int innerY = shellPos.getY() - (int) Math.round(dy / length);
        int innerZ = shellPos.getZ() - (int) Math.round(dz / length);
        return new BlockPos(innerX, innerY, innerZ);
    }

    private void scheduleDeferredDomainSkyboxPlacement(ServerPlayer player)
    {
        int delayTicks = Math.max(getDeferredDomainSkyboxPlacementDelayTicks(), 1);
        deferredSkyboxPlacementDelayByCaster.put(player.getUUID(), delayTicks);
    }

    private void tickDeferredDomainSkyboxPlacement(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        Integer remaining = deferredSkyboxPlacementDelayByCaster.get(uuid);
        if (remaining == null)
        {
            return;
        }

        if (remaining <= 1)
        {
            deferredSkyboxPlacementDelayByCaster.remove(uuid);
            Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(uuid);
            if (queue == null || queue.isEmpty())
            {
                return;
            }
            BarrierInstance instance = queue.peekLast();
            if (instance == null || instance.isExpanding())
            {
                return;
            }
            ServerLevel level = player.server.getLevel(instance.getDimension());
            if (level == null)
            {
                return;
            }
            placeDomainSkybox(
                    level,
                    instance.getShellBlocks(),
                    instance.getCenter(),
                    instance.getRadius(),
                    instance.getPlayerFootY(),
                    instance.getRemovedBlocks(),
                    instance);
            return;
        }

        deferredSkyboxPlacementDelayByCaster.put(uuid, remaining - 1);
    }

    /**
     * 施法者位于自己领域内部时再次触发领域展开，视为解除领域。
     */
    private boolean closeDomainIfCasterInside(DomainCaster caster)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(caster.uid());
        if (queue == null || queue.isEmpty())
        {
            return false;
        }

        Deque<BarrierInstance> kept = new ArrayDeque<>();
        boolean closed = false;
        ServerPlayer player = caster.player();

        for (BarrierInstance instance : queue)
        {
            if (!closed && isCasterInsideBarrier(caster, instance))
            {
                clearBarrierInstance(caster, instance);
                closed = true;
                if (player != null)
                {
                    String nameDisplay = domainName.isEmpty() ? "" : "「" + domainName + "」";
                    player.displayClientMessage(
                            Component.literal(nameDisplay + "已解除")
                                    .withStyle(ChatFormatting.YELLOW), true);
                }
                continue;
            }
            kept.addLast(instance);
        }

        if (kept.isEmpty())
        {
            ACTIVE_DOMAINS.remove(caster.uid());
        }
        else
        {
            ACTIVE_DOMAINS.put(caster.uid(), kept);
        }
        return closed;
    }

    /**
     * 玩家位于自己领域内部时再次触发领域展开，视为解除领域。
     */
    private boolean closeDomainIfPlayerInside(ServerPlayer player)
    {
        return closeDomainIfCasterInside(DomainCaster.of(player));
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

        public BarrierInstance getBarrierInstance()
        {
            return instance;
        }
    }

    /**
     * 生得领域结界与天空盒建造完成后的装饰钩子（如审判术式地板、处刑架圈景观）。
     */
    protected void onInnateDomainArenaBuilt(ServerPlayer player, ServerLevel level, InnateArenaHandle handle)
    {
    }

    public final InnateArenaHandle buildInnateDomainArena(
            ServerPlayer player,
            ServerLevel level,
            BlockPos center,
            int radius,
            ResourceLocation skyboxTexture)
    {
        return buildInnateDomainArena(player, level, center, radius, center.getY() - radius + 1, skyboxTexture);
    }

    public final InnateArenaHandle buildInnateDomainArena(
            ServerPlayer player,
            ServerLevel level,
            BlockPos center,
            int radius,
            int playerFootY,
            ResourceLocation skyboxTexture)
    {
        Map<BlockPos, BlockState> removedBlocks = new HashMap<>();
        List<BlockPos> shellBlocks = buildHollowSpherePositions(center, radius);
        if (shellBlocks.isEmpty())
        {
            return null;
        }
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
        InnateArenaHandle handle = new InnateArenaHandle(instance);
        onInnateDomainArenaBuilt(player, level, handle);
        return handle;
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
