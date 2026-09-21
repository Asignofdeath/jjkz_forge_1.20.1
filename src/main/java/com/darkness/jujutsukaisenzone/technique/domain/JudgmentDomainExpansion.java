package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.compat.ShaderedCompat;
import com.darkness.jujutsukaisenzone.effect.ConfiscationEffect;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.effect.TechniqueMeltdownEffect;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyTrialSpotlightFx;
import com.darkness.jujutsukaisenzone.entity.ChubukushishiEntity;
import com.darkness.jujutsukaisenzone.entity.CourtSeatEntity;
import com.darkness.jujutsukaisenzone.entity.JudgeEntity;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerJudgmentDomainData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentCrimeLibrary;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentJudgeEntityLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentSoundLogic;
import com.darkness.jujutsukaisenzone.technique.barrier.AbstractBarrierTechnique;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 审判术式领域展开 — 诛伏赐死。
 * <p>
 * 结界构建完成后：地板天空盒按圈自中心向外扩散，每圈石砖自上方落下；并在领域中心生成处刑架圈景观。
 */
public final class JudgmentDomainExpansion extends AbstractTechniqueDomain
{
    private static final JudgmentDomainExpansion INSTANCE = new JudgmentDomainExpansion();

    private static final String DOMAIN_NAME = "诛伏赐死";
    private static final ResourceLocation JUDGMENT_DOMAIN_SKYBOX_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/environment/chubokushishi_blacksky");
    /** 领域展开蓄力时间（tick），3 秒 = 60 tick */
    private static final int CAST_DELAY_TICKS = 60;
    /** 地板石砖圆形区域半径（格），不铺满整个领域地板 */
    private static final int FLOOR_STONE_RADIUS = 18;
    /** 最内层若干圈直接铺砖（1=最中心圈），超出后改用落下动画 */
    private static final int FLOOR_STONE_INSTANT_RING_COUNT = 6;
    /** 地板石砖每批生成的落下方块数 */
    private static final int FLOOR_STONE_BATCH = 20;
    /** 每批之间的间隔（tick） */
    private static final int FLOOR_STONE_SPREAD_INTERVAL_TICKS = 1;
    /** 落下方块生成高度（相对目标地板格） */
    private static final int FLOOR_STONE_FALL_SPAWN_HEIGHT = 2;
    /** 落下方块超时后强制落位（tick） */
    private static final int FLOOR_STONE_FALL_TIMEOUT_TICKS = 80;
    /** 结界完成后延迟生成处刑架圈景观（tick），3 秒 = 60 tick */
    private static final int CHUBUKUSHI_SPAWN_DELAY_TICKS = 60;
    /** 结界完成后延迟播放红黄青循环闪现世界层滤镜（tick），4 秒 = 80 tick */
    private static final int COLOR_CYCLE_OVERLAY_DELAY_TICKS = 80;
    /** 滤镜动画总时长（tick），与 {@link com.darkness.jujutsukaisenzone.client.renderer.JudgmentDomainColorCycleOverlayRenderer} 一致 */
    private static final int COLOR_CYCLE_OVERLAY_DURATION_TICKS = 80;
    /** 审判位置排序半径（格）：仅用于生成站位圆盘，不限制参与排序的领域内目标。 */
    private static final int TRIAL_POSITION_SORT_RADIUS = 8;
    /** 审判站位网格间隔（格）：相邻站位中心至少相隔该格数，避免实体重叠。 */
    private static final int TRIAL_SEAT_GRID_SPACING = 2;
    /** 施法者落点周围排除其他站位的水平半径（格）。 */
    private static final int CASTER_SEAT_EXCLUSION_RADIUS = 4;
    /** 未分配到座位的目标随机传送：水平距离须大于该值（格）。 */
    private static final double AUDIENCE_RING_INNER = 10.0D;
    /** 未分配到座位的目标随机传送：水平距离须小于该值（格）。 */
    private static final double AUDIENCE_RING_OUTER = 23.0D;
    /** 已分配落点的玩家允许偏离的最大水平距离（格），超出则立刻传送回落点。 */
    private static final double TRIAL_ASSIGNED_POSITION_MAX_DRIFT_SQ = 1.0D;
    /** 结界完成后延迟执行位置排序（tick），8 秒 = 160 tick */
    private static final int POSITION_SORT_DELAY_TICKS = 160;
    /** 「全员回来重审」：结界构建完成时领域内人员须超过该值。 */
    private static final int PERFECT_RETRIAL_OCCUPANT_THRESHOLD = 7;
    private static final float PERFECT_RETRIAL_IRON_SOUND_VOLUME = 5.0F;
    /** 结界完成后延迟在施法者头顶生成审判者（tick），9 秒 = 180 tick */
    private static final int JUDGE_SPAWN_DELAY_TICKS = 180;
    /** 审判者生成高度：相对施法者眼部位置的向上偏移（格）。 */
    private static final double JUDGE_ABOVE_HEAD_OFFSET = 1.0D;
    private static final int TRIAL_SEAT_LIGHT_BLOCK_LEVEL = 10;
    private static final BlockState TRIAL_SEAT_LIGHT_BLOCK_STATE =
            Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, TRIAL_SEAT_LIGHT_BLOCK_LEVEL);
    private static final int TRIAL_SEAT_LIGHT_BLOCK_COUNT = 2;
    private static final int LIGHT_BLOCK_UPDATE_FLAGS = 3;
    /** 领域中心向上偏移（格），各放置一块满级光明方块。 */
    private static final int[] DOMAIN_CENTER_LIGHT_OFFSETS = {7, 13};
    private static final BlockState DOMAIN_CENTER_LIGHT_BLOCK_STATE =
            Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LightBlock.MAX_LEVEL);

    private final ConcurrentMap<UUID, Integer> castingDelayMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, UUID> chubukushishiEntityByCaster = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> chubukushishiSpawnDelayByCaster = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> colorCycleOverlayDelayByCaster = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> positionSortDelayByCaster = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> judgeSpawnDelayByCaster = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, UUID> judgeEntityByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 本次领域生成的法庭座位实体 UUID */
    private final ConcurrentMap<UUID, Set<UUID>> courtSeatEntityUuidsByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 已挂载 spotlight 的实体 UUID */
    private final ConcurrentMap<UUID, Set<UUID>> trialSpotlightEntityUuidsByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 审判站位处放置的光明方块及其原方块状态 */
    private final ConcurrentMap<UUID, Map<BlockPos, BlockState>> trialSeatLightBlocksByCaster = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, FloorSpreadState> floorSpreadByCaster = new ConcurrentHashMap<>();
    /** 结界构建完成后：领域内玩家移动锁定（玩家 UUID → 锁定状态）。 */
    private final ConcurrentMap<UUID, TrialPlayerLock> trialPlayerLockByPlayer = new ConcurrentHashMap<>();
    /** 结界构建完成后：领域内生物 AI 暂停（实体 UUID → 状态）。 */
    private final ConcurrentMap<UUID, TrialMobPause> trialMobPauseByEntity = new ConcurrentHashMap<>();

    private record TrialPlayerLock(UUID ownerUuid, Vec3 lockPos, boolean wasInvulnerable, boolean assignedPosition)
    {
    }

    private record TrialMobPause(UUID ownerUuid, boolean aiPausedByThisLock, boolean originNoAi, boolean wasInvulnerable)
    {
    }

    private record PendingFall(BlockPos targetPos, UUID entityUuid, int ageTicks)
    {
        private PendingFall ageOneTick()
        {
            return new PendingFall(targetPos, entityUuid, ageTicks + 1);
        }
    }

    private record FloorSpreadState(
            ResourceKey<Level> dimension,
            List<List<BlockPos>> rings,
            int currentRingIndex,
            int nextIndexInRing,
            int ticksUntilNextBatch,
            List<PendingFall> pendingFalls)
    {
    }

    /** 裁决完成后自动关闭领域时，跳过一次术式熔断。 */
    private final ConcurrentMap<UUID, Boolean> skipMeltdownOnAutoCloseByCaster = new ConcurrentHashMap<>();
    /** 上诉重审领域关闭时不计入正常领域冷却。 */
    private final ConcurrentMap<UUID, Boolean> appealDomainCloseByCaster = new ConcurrentHashMap<>();
    /** 上诉即时展开：待审理被告 UUID。 */
    private final ConcurrentMap<UUID, UUID> appealDefendantByCaster = new ConcurrentHashMap<>();
    /** 上诉即时展开：沿用原指控罪名。 */
    private final ConcurrentMap<UUID, JudgmentCrimeLibrary.CrimeEntry> appealCrimeByCaster = new ConcurrentHashMap<>();
    /** 上诉即时展开期间强制立刻放置天空盒（覆盖 {@link #deferDomainSkyboxPlacement()}）。 */
    private volatile UUID instantSkyboxCasterUuid;

    private JudgmentDomainExpansion()
    {
        setDomainName(DOMAIN_NAME);
    }

    public static JudgmentDomainExpansion getInstance()
    {
        return INSTANCE;
    }

    /** 诛伏赐死审判冻结期间禁止使用该玩家的术式技能。 */
    public static boolean blockTechniqueSkillUse(ServerPlayer player)
    {
        return INSTANCE.trialPlayerLockByPlayer.containsKey(player.getUUID());
    }

    /**
     * 诛伏赐死领域审判冻结期间：该生物不可受到任何伤害（直至 {@link #endTrialFreeze} / 领域关闭）。
     */
    public static boolean isEntityUnderTrialViolenceProtection(LivingEntity entity)
    {
        return INSTANCE.isEntityUnderTrialViolenceProtectionInternal(entity);
    }

    private boolean isEntityUnderTrialViolenceProtectionInternal(LivingEntity entity)
    {
        if (entity instanceof ServerPlayer player && trialPlayerLockByPlayer.containsKey(player.getUUID()))
        {
            return true;
        }
        if (entity instanceof Mob mob && trialMobPauseByEntity.containsKey(mob.getUUID()))
        {
            return true;
        }
        return findActiveDomainCasterForEntity(entity) != null;
    }

    @Override
    protected void applyDomainSpecificEffect(ServerPlayer player, BarrierInstance instance)
    {
        UUID appealDefendantUuid = appealDefendantByCaster.remove(player.getUUID());
        if (appealDefendantUuid != null)
        {
            applyAppealDomainSetup(player, instance, appealDefendantUuid);
            return;
        }

        ServerLevel level = player.serverLevel();
        JudgmentSoundLogic.playLayingStoneTilesAtDomainCenter(level, instance.getCenter());
        teleportCasterToDomainCenter(player, instance);
        EpicFightCompat.playJudgmentDomainLookdownAnimation(player);
        int occupantCount = collectTrialSortTargets(level, player.getUUID(), instance).size();
        maybeAnnouncePerfectRetrial(player, instance, occupantCount);
        ModNetwork.sendJudgmentDomainCmdCamSetup(player, instance.getCenter());
        beginTrialFreeze(player, instance);
        scheduleChubukushishiSpawn(player);
        scheduleColorCycleOverlay(player);
        scheduleTrialPositionSort(player);
        scheduleJudgeSpawn(player);
        beginFloorStoneSpread(player, instance);
    }

    @Override
    protected void onInnateDomainArenaBuilt(
            ServerPlayer player,
            ServerLevel level,
            InnateArenaHandle handle)
    {
        BarrierInstance instance = handle.getBarrierInstance();
        if (instance == null)
        {
            return;
        }
        placeInnateFloorStone(level, instance);
        spawnInnateChubukushishi(player, level, instance);
    }

    @Override
    protected boolean hasLearnedTechnique(ServerPlayer player)
    {
        return PlayerTechniqueData.isJudgment(player);
    }

    @Override
    public String getTechniqueType()
    {
        return "judgment_domain";
    }

    @Override
    protected ResourceLocation getDomainSkyboxTexture()
    {
        return JUDGMENT_DOMAIN_SKYBOX_TEXTURE;
    }

    @Override
    protected boolean deferDomainSkyboxPlacement()
    {
        return instantSkyboxCasterUuid == null;
    }

    @Override
    protected int getDeferredDomainSkyboxPlacementDelayTicks()
    {
        return COLOR_CYCLE_OVERLAY_DELAY_TICKS + COLOR_CYCLE_OVERLAY_DURATION_TICKS;
    }

    @Override
    protected boolean shouldApplyMeltdownOnBarrierClosed(ServerPlayer player, BarrierInstance instance)
    {
        return !Boolean.TRUE.equals(skipMeltdownOnAutoCloseByCaster.remove(player.getUUID()));
    }

    @Override
    protected void onBarrierClosed(ServerPlayer player, BarrierInstance instance)
    {
        removeTrialSpotlightForCaster(player, instance);
        JudgmentDomainTrialLogic.applyPendingPunishmentsOnDomainClose(player);
        JudgmentDomainTrialLogic.grantPendingExecutionersSwordOnDomainClose(player);
        endTrialFreeze(player);
        JudgmentDomainTrialLogic.onDomainClosed(player);
        JudgmentDomainAppealLogic.activateDeferredAppeals(player);
        removeChubukushishiForCaster(player);
        removeCourtSeatsForCaster(player);
        removeTrialSeatLightBlocksForCaster(player);
        removeJudgeForCaster(player);
        chubukushishiSpawnDelayByCaster.remove(player.getUUID());
        colorCycleOverlayDelayByCaster.remove(player.getUUID());
        positionSortDelayByCaster.remove(player.getUUID());
        judgeSpawnDelayByCaster.remove(player.getUUID());
        clearFloorSpreadState(player);
        appealDefendantByCaster.remove(player.getUUID());
        appealCrimeByCaster.remove(player.getUUID());
        if (!Boolean.TRUE.equals(appealDomainCloseByCaster.remove(player.getUUID())))
        {
            PlayerJudgmentDomainData.applyCooldownOnDomainClosed(player);
        }
        super.onBarrierClosed(player, instance);
    }

    /**
     * 上诉：瞬间展开诛伏赐死并进入单人审理（无蓄力、展开动画、滤镜、音效与地板扩散演出）。
     */
    public boolean castAppealDomain(
            ServerPlayer caster,
            LivingEntity defendant,
            JudgmentCrimeLibrary.CrimeEntry crime)
    {
        if (caster == null || defendant == null || !caster.isAlive() || !defendant.isAlive() || defendant.isRemoved())
        {
            return false;
        }
        if (hasActiveCompletedDomain(caster) || isCasting(caster))
        {
            return false;
        }
        if (DomainExpansionRouter.isPositionInAnyDomain(caster.serverLevel(), caster.blockPosition()))
        {
            return false;
        }

        appealDefendantByCaster.put(caster.getUUID(), defendant.getUUID());
        if (crime != null)
        {
            appealCrimeByCaster.put(caster.getUUID(), crime);
        }
        appealDomainCloseByCaster.put(caster.getUUID(), true);
        skipMeltdownOnAutoCloseByCaster.put(caster.getUUID(), true);

        instantSkyboxCasterUuid = caster.getUUID();
        boolean opened;
        try
        {
            opened = castAndCompleteDomainInstantly(caster, false);
        }
        finally
        {
            instantSkyboxCasterUuid = null;
        }

        if (!opened)
        {
            appealDefendantByCaster.remove(caster.getUUID());
            appealCrimeByCaster.remove(caster.getUUID());
            appealDomainCloseByCaster.remove(caster.getUUID());
            skipMeltdownOnAutoCloseByCaster.remove(caster.getUUID());
            return false;
        }

        caster.displayClientMessage(
                Component.literal("「" + DOMAIN_NAME + "」上诉重审领域已展开")
                        .withStyle(ChatFormatting.GOLD),
                true);
        return true;
    }

    private void applyAppealDomainSetup(ServerPlayer caster, BarrierInstance instance, UUID defendantUuid)
    {
        ServerLevel level = caster.serverLevel();
        Entity entity = level.getEntity(defendantUuid);
        if (!(entity instanceof LivingEntity defendant) || !defendant.isAlive() || defendant.isRemoved())
        {
            closeActiveDomainAfterVerdict(caster);
            return;
        }

        placeInnateFloorStone(level, instance);
        spawnInnateChubukushishi(caster, level, instance);
        teleportCasterToDomainCenter(caster, instance);
        beginTrialFreeze(caster, instance);
        applySingleTrialFaceToFacePlacement(
                level,
                caster,
                defendant,
                instance.getCenter(),
                instance.getPlayerFootY(),
                caster.getUUID());
        placeDomainCenterLightBlocks(caster, instance);
        spawnJudgeAboveCaster(caster, true);
        mountAppealTrialSpotlights(caster, defendant);

        JudgmentCrimeLibrary.CrimeEntry crime = appealCrimeByCaster.remove(caster.getUUID());
        if (crime == null)
        {
            crime = JudgmentCrimeLibrary.pickForDefendant(defendant, caster.getRandom());
        }
        JudgmentDomainTrialLogic.beginAppealSingleTrial(caster, defendant, crime);
    }

    /** 上诉领域：对审判者/被告显式挂载聚光灯（不依赖球内玩家扫描时机）。 */
    private void mountAppealTrialSpotlights(ServerPlayer caster, LivingEntity defendant)
    {
        List<ServerPlayer> viewers = new ArrayList<>(2);
        viewers.add(caster);
        if (defendant instanceof ServerPlayer defendantPlayer
                && !defendantPlayer.getUUID().equals(caster.getUUID()))
        {
            viewers.add(defendantPlayer);
        }

        OwnerOnlyTrialSpotlightFx.mount(caster, viewers);
        trialSpotlightEntityUuidsByCaster
                .computeIfAbsent(caster.getUUID(), ignored -> ConcurrentHashMap.newKeySet())
                .add(caster.getUUID());

        OwnerOnlyTrialSpotlightFx.mount(defendant, viewers);
        trialSpotlightEntityUuidsByCaster
                .computeIfAbsent(caster.getUUID(), ignored -> ConcurrentHashMap.newKeySet())
                .add(defendant.getUUID());
    }

    @Override
    public void cast(ServerPlayer player)
    {
        if (!player.isAlive() || !isInBattleMode(player))
        {
            return;
        }

        if (isPlayerInsideOwnDomain(player))
        {
            super.cast(player);
            return;
        }

        UUID uuid = player.getUUID();

        if (TechniqueMeltdownEffect.blockDomainExpansion(player))
        {
            return;
        }

        if (ConfiscationEffect.blockDomainExpansion(player))
        {
            return;
        }

        if (castingDelayMap.containsKey(uuid))
        {
            castingDelayMap.remove(uuid);
            PlayerJudgmentDomainData.clearPendingCast(player);
            player.displayClientMessage(
                    Component.literal("「" + DOMAIN_NAME + "」已取消")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }

        if (DomainExpansionRouter.isPositionInAnyDomain(player.serverLevel(), player.blockPosition()))
        {
            player.displayClientMessage(Component.literal("无法展开领域，因为领域对抗还没做").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!hasLearnedDomainExpansion(player))
        {
            player.displayClientMessage(Component.literal("尚未习得领域展开").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!hasLearnedTechnique(player))
        {
            return;
        }

        if (PlayerJudgmentDomainData.isOnCooldown(player))
        {
            PlayerJudgmentDomainData.notifyCooldownBlocked(player);
            return;
        }

        PlayerJudgmentDomainData.prepareCast(player);
        castingDelayMap.put(uuid, CAST_DELAY_TICKS);
        player.displayClientMessage(
                Component.literal("「" + DOMAIN_NAME + "」展开蓄力中...")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                true);
    }

    @Override
    public void tickMaintain(ServerPlayer player)
    {
        tickCastingDelay(player);
        tickTrialFreeze(player);
        tickChubukushishiSpawnDelay(player);
        tickColorCycleOverlayDelay(player);
        tickTrialPositionSortDelay(player);
        tickJudgeSpawnDelay(player);
        tickJudgeFollowCaster(player);
        tickFloorStoneSpread(player);
        JudgmentDomainTrialLogic.tickMaintain(player);
        JudgmentDomainAppealLogic.tick(player);
        super.tickMaintain(player);
    }

    public boolean isCasting(ServerPlayer player)
    {
        return castingDelayMap.containsKey(player.getUUID());
    }

    public void clearCastingState(UUID uuid)
    {
        castingDelayMap.remove(uuid);
    }

    private static int domainQueueSize(UUID casterUuid)
    {
        Deque<BarrierInstance> queue = INSTANCE.ACTIVE_DOMAINS.get(casterUuid);
        return queue == null ? 0 : queue.size();
    }

    private void tickCastingDelay(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        Integer remaining = castingDelayMap.get(uuid);
        if (remaining == null)
        {
            return;
        }

        if (!player.isAlive() || !isInBattleMode(player))
        {
            castingDelayMap.remove(uuid);
            PlayerJudgmentDomainData.clearPendingCast(player);
            player.displayClientMessage(
                    Component.literal("「" + DOMAIN_NAME + "」展开蓄力中断")
                            .withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        int newRemaining = remaining - 1;
        if (newRemaining <= 0)
        {
            castingDelayMap.remove(uuid);
            if (PlayerJudgmentDomainData.isOnCooldown(player))
            {
                PlayerJudgmentDomainData.clearPendingCast(player);
                PlayerJudgmentDomainData.notifyCooldownBlocked(player);
                return;
            }
            int queueSizeBefore = domainQueueSize(uuid);
            super.cast(player);
            if (domainQueueSize(uuid) <= queueSizeBefore)
            {
                PlayerJudgmentDomainData.clearPendingCast(player);
            }
        }
        else
        {
            castingDelayMap.put(uuid, newRemaining);
        }
    }

    private boolean isPlayerInsideOwnDomain(ServerPlayer player)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return false;
        }
        for (BarrierInstance instance : queue)
        {
            if (isPlayerInsideBarrier(player, instance))
            {
                return true;
            }
        }
        return false;
    }

    private void scheduleChubukushishiSpawn(ServerPlayer player)
    {
        chubukushishiSpawnDelayByCaster.put(player.getUUID(), CHUBUKUSHI_SPAWN_DELAY_TICKS);
    }

    private void scheduleColorCycleOverlay(ServerPlayer player)
    {
        colorCycleOverlayDelayByCaster.put(player.getUUID(), COLOR_CYCLE_OVERLAY_DELAY_TICKS);
    }

    private void tickColorCycleOverlayDelay(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        Integer remaining = colorCycleOverlayDelayByCaster.get(uuid);
        if (remaining == null)
        {
            return;
        }

        if (remaining <= 1)
        {
            colorCycleOverlayDelayByCaster.remove(uuid);
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
            broadcastColorCycleOverlayToPlayersInDomain(player);
            return;
        }

        colorCycleOverlayDelayByCaster.put(uuid, remaining - 1);
    }

    private void broadcastColorCycleOverlayToPlayersInDomain(ServerPlayer caster)
    {
        for (ServerPlayer nearby : getPlayersInsideActiveDomain(caster))
        {
            ModNetwork.sendJudgmentDomainColorCycleOverlaySync(nearby);
        }
    }

    private void tickChubukushishiSpawnDelay(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        Integer remaining = chubukushishiSpawnDelayByCaster.get(uuid);
        if (remaining == null)
        {
            return;
        }

        if (remaining <= 1)
        {
            chubukushishiSpawnDelayByCaster.remove(uuid);
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
            spawnChubukushishiAtDomainCenter(player, instance);
            return;
        }

        chubukushishiSpawnDelayByCaster.put(uuid, remaining - 1);
    }

    private void teleportCasterToDomainCenter(ServerPlayer player, BarrierInstance instance)
    {
        BlockPos center = instance.getCenter();
        double x = center.getX() + 0.5D;
        double y = instance.getPlayerFootY() + 1.0D;
        double z = center.getZ() + 0.5D;
        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    private void spawnChubukushishiAtDomainCenter(ServerPlayer player, BarrierInstance instance)
    {
        ServerLevel level = player.server.getLevel(instance.getDimension());
        if (level == null)
        {
            return;
        }

        removeChubukushishiForCaster(player);

        ChubukushishiEntity entity = ModEntities.EXECUTION_SCAFFOLD_CIRCLE.get().create(level);
        if (entity == null)
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int footY = instance.getPlayerFootY();
        entity.moveTo(center.getX() + 0.5D, footY, center.getZ() + 0.5D, 0.0F, 0.0F);
        entity.prepareEntranceAnimation();
        if (!level.addFreshEntity(entity))
        {
            return;
        }

        chubukushishiEntityByCaster.put(player.getUUID(), entity.getUUID());
    }

    private void placeInnateFloorStone(ServerLevel level, BarrierInstance instance)
    {
        BlockPos center = instance.getCenter();
        int footY = instance.getPlayerFootY();
        int radiusSq = FLOOR_STONE_RADIUS * FLOOR_STONE_RADIUS;
        List<BlockPos> floorPositions = new ArrayList<>();
        for (BlockPos pos : instance.getSkyboxBlocks())
        {
            if (pos.getY() != footY)
            {
                continue;
            }
            int dx = pos.getX() - center.getX();
            int dz = pos.getZ() - center.getZ();
            if (dx * dx + dz * dz > radiusSq)
            {
                continue;
            }
            floorPositions.add(pos);
        }
        for (BlockPos pos : floorPositions)
        {
            placeStoneBrickInstant(level, instance, pos);
        }
    }

    private void spawnInnateChubukushishi(ServerPlayer player, ServerLevel level, BarrierInstance instance)
    {
        removeChubukushishiForCaster(player);

        ChubukushishiEntity entity = ModEntities.EXECUTION_SCAFFOLD_CIRCLE.get().create(level);
        if (entity == null)
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int footY = instance.getPlayerFootY();
        entity.moveTo(center.getX() + 0.5D, footY, center.getZ() + 0.5D, 0.0F, 0.0F);
        entity.prepareStaticDisplay();
        if (!level.addFreshEntity(entity))
        {
            return;
        }

        chubukushishiEntityByCaster.put(player.getUUID(), entity.getUUID());
    }

    private void scheduleJudgeSpawn(ServerPlayer player)
    {
        judgeSpawnDelayByCaster.put(player.getUUID(), JUDGE_SPAWN_DELAY_TICKS);
    }

    private void tickJudgeSpawnDelay(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        Integer remaining = judgeSpawnDelayByCaster.get(uuid);
        if (remaining == null)
        {
            return;
        }

        if (remaining <= 1)
        {
            judgeSpawnDelayByCaster.remove(uuid);
            spawnJudgeAboveCaster(player);
            return;
        }

        judgeSpawnDelayByCaster.put(uuid, remaining - 1);
    }

    private void spawnJudgeAboveCaster(ServerPlayer caster)
    {
        spawnJudgeAboveCaster(caster, false);
    }

    /** 若领域内尚无审判者则立刻在施法者头顶生成（用于单人审理死刑愤怒演出）。 */
    public void ensureJudgeForAngryPerformance(ServerPlayer caster)
    {
        if (findJudgeForCaster(caster) != null)
        {
            return;
        }
        spawnJudgeAboveCaster(caster, true);
    }

    private void spawnJudgeAboveCaster(ServerPlayer caster, boolean staticDisplay)
    {
        removeJudgeForCaster(caster);

        ServerLevel level = caster.serverLevel();
        JudgeEntity judge = ModEntities.JUDGE.get().create(level);
        if (judge == null)
        {
            return;
        }

        Vec3 spawnPos = judgePositionAboveCaster(caster);
        float yaw = caster.getYRot();
        judge.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, yaw, 0.0F);
        if (staticDisplay)
        {
            judge.prepareStaticDisplay();
        }
        else
        {
            judge.prepareEntranceAnimation();
        }
        if (!level.addFreshEntity(judge))
        {
            return;
        }

        judgeEntityByCaster.put(caster.getUUID(), judge.getUUID());
    }

    private void tickJudgeFollowCaster(ServerPlayer caster)
    {
        if (JudgmentJudgeEntityLogic.hasPendingAngryPerformance(caster.getUUID())
                || JudgmentDomainTrialLogic.isPendingDomainCloseAfterAngryPerformance(caster.getUUID()))
        {
            return;
        }

        UUID judgeUuid = judgeEntityByCaster.get(caster.getUUID());
        if (judgeUuid == null)
        {
            return;
        }

        ServerLevel level = caster.serverLevel();
        Entity entity = level.getEntity(judgeUuid);
        if (!(entity instanceof JudgeEntity judge) || !judge.isAlive())
        {
            judgeEntityByCaster.remove(caster.getUUID());
            return;
        }

        Vec3 targetPos = judgePositionAboveCaster(caster);
        float yaw = caster.getYRot();
        if (judge.tickCount <= JudgeEntity.entranceAnimationDurationTicks())
        {
            judge.moveTo(judge.getX(), judge.getY(), judge.getZ(), yaw, 0.0F);
            return;
        }

        judge.moveTo(targetPos.x, targetPos.y, targetPos.z, yaw, 0.0F);
    }

    private static Vec3 judgePositionAboveCaster(ServerPlayer caster)
    {
        return new Vec3(caster.getX(), caster.getEyeY() + JUDGE_ABOVE_HEAD_OFFSET, caster.getZ());
    }

    private void removeJudgeForCaster(ServerPlayer player)
    {
        UUID entityUuid = judgeEntityByCaster.remove(player.getUUID());
        if (entityUuid == null)
        {
            return;
        }

        Entity entity = player.serverLevel().getEntity(entityUuid);
        if (entity != null)
        {
            entity.discard();
        }
    }

    /** 施法者当前领域内随行的审判者；不存在或已失效时返回 null。 */
    public JudgeEntity findJudgeForCaster(ServerPlayer caster)
    {
        UUID judgeUuid = judgeEntityByCaster.get(caster.getUUID());
        if (judgeUuid == null)
        {
            return null;
        }

        Entity entity = caster.serverLevel().getEntity(judgeUuid);
        if (entity instanceof JudgeEntity judge && judge.isAlive())
        {
            return judge;
        }

        judgeEntityByCaster.remove(caster.getUUID());
        return null;
    }

    private void scheduleTrialPositionSort(ServerPlayer player)
    {
        positionSortDelayByCaster.put(player.getUUID(), POSITION_SORT_DELAY_TICKS);
    }

    private void tickTrialPositionSortDelay(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        Integer remaining = positionSortDelayByCaster.get(uuid);
        if (remaining == null)
        {
            return;
        }

        if (remaining <= 1)
        {
            positionSortDelayByCaster.remove(uuid);
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
            applyTrialPositionSort(player, instance);
            return;
        }

        positionSortDelayByCaster.put(uuid, remaining - 1);
    }

    private void applyTrialPositionSort(ServerPlayer caster, BarrierInstance instance)
    {
        ServerLevel level = caster.server.getLevel(instance.getDimension());
        if (level == null)
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int footY = instance.getPlayerFootY();
        UUID casterUuid = caster.getUUID();
        List<LivingEntity> targets = collectTrialSortTargets(level, casterUuid, instance);
        List<LivingEntity> seatedDefendants = new ArrayList<>();
        if (!targets.isEmpty())
        {
            List<LivingEntity> seatCandidates = new ArrayList<>();
            for (LivingEntity target : targets)
            {
                if (!target.getUUID().equals(casterUuid))
                {
                    seatCandidates.add(target);
                }
            }

            if (seatCandidates.size() == 1)
            {
                LivingEntity defendant = seatCandidates.get(0);
                applySingleTrialFaceToFacePlacement(level, caster, defendant, center, footY, casterUuid);
                seatedDefendants.add(defendant);
            }
            else
            {
                sortTrialTargetsForSeats(targets, center, casterUuid);
                List<Vec3> seats = generateTrialSeatPositionsOuterFirst(center, footY);
                if (!seats.isEmpty())
                {
                    LivingEntity casterTarget = null;
                    List<LivingEntity> multiSeatCandidates = new ArrayList<>();
                    for (LivingEntity target : targets)
                    {
                        if (target.getUUID().equals(casterUuid))
                        {
                            casterTarget = target;
                        }
                        else
                        {
                            multiSeatCandidates.add(target);
                        }
                    }

                    List<Vec3> assignSeats = seats;
                    if (casterTarget != null)
                    {
                        Vec3 casterSeat = seats.get(0);
                        placeTrialSortTarget(level, casterTarget, casterSeat, center, casterUuid);
                        assignSeats = filterTrialSeatsOutsideCasterExclusion(seats, casterSeat);
                    }

                    int assignCount = Math.min(multiSeatCandidates.size(), assignSeats.size());
                    for (int i = 0; i < assignCount; i++)
                    {
                        LivingEntity defendant = multiSeatCandidates.get(i);
                        placeTrialSortTarget(level, defendant, assignSeats.get(i), center, casterUuid);
                        seatedDefendants.add(defendant);
                    }

                    for (int i = assignCount; i < multiSeatCandidates.size(); i++)
                    {
                        placeTrialOverflowTarget(level, multiSeatCandidates.get(i), center, footY, casterUuid);
                    }
                }
            }
        }

        placeDomainCenterLightBlocks(caster, instance);
        JudgmentDomainTrialLogic.onSortComplete(caster, seatedDefendants);
    }

    private static void maybeAnnouncePerfectRetrial(ServerPlayer caster, BarrierInstance instance, int occupantCount)
    {
        if (occupantCount <= PERFECT_RETRIAL_OCCUPANT_THRESHOLD)
        {
            return;
        }
        ServerLevel level = caster.serverLevel();
        BlockPos center = instance.getCenter();
        double cx = center.getX() + 0.5D;
        double cy = center.getY() + 0.5D;
        double cz = center.getZ() + 0.5D;
        level.playSound(null, cx, cy, cz, ModSounds.JUDGMENT_DOMAIN_PERFECT.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), ModSounds.SOUND_EFFECTS_IRON_SOUND.get(), SoundSource.PLAYERS, PERFECT_RETRIAL_IRON_SOUND_VOLUME, 1.0F);
        caster.displayClientMessage(Component.literal("全员回来重审").withStyle(ChatFormatting.GOLD), false);
        grantAdvancement(caster, "perfect_retrial");
    }

    private static void grantAdvancement(ServerPlayer player, String name)
    {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, name);
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null)
        {
            return;
        }
        var progress = player.getAdvancements();
        for (String criterion : advancement.getCriteria().keySet())
        {
            if (!progress.getOrStartProgress(advancement).isDone())
            {
                progress.award(advancement, criterion);
            }
        }
    }

    private List<LivingEntity> collectTrialSortTargets(ServerLevel level, UUID casterUuid, BarrierInstance instance)
    {
        BlockPos center = instance.getCenter();
        int domainRadius = instance.getRadius();
        double domainRadiusSq = (double) domainRadius * domainRadius;
        List<LivingEntity> targets = new ArrayList<>();

        for (ServerPlayer player : findPlayersInsideDomain(level, center, domainRadius, domainRadiusSq))
        {
            if (!player.isAlive())
            {
                continue;
            }
            targets.add(player);
        }

        for (Mob mob : findTrialSortMobTargets(level, casterUuid, center, domainRadius, domainRadiusSq))
        {
            targets.add(mob);
        }

        return targets;
    }

    /** 以领域中心方块中心为圆心的水平距离平方（XZ）。 */
    private static double horizontalDistSqFromCenter(Vec3 position, BlockPos center)
    {
        double dx = position.x - (center.getX() + 0.5D);
        double dz = position.z - (center.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static double horizontalDistSqBetween(Vec3 a, Vec3 b)
    {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static List<Vec3> filterTrialSeatsOutsideCasterExclusion(List<Vec3> seats, Vec3 casterSeat)
    {
        double exclusionRadiusSq = (double) CASTER_SEAT_EXCLUSION_RADIUS * CASTER_SEAT_EXCLUSION_RADIUS;
        List<Vec3> filtered = new ArrayList<>();
        for (Vec3 seat : seats)
        {
            if (horizontalDistSqBetween(seat, casterSeat) <= exclusionRadiusSq)
            {
                continue;
            }
            filtered.add(seat);
        }
        return filtered;
    }

    private static List<Mob> findTrialSortMobTargets(
            ServerLevel level, UUID casterUuid, BlockPos center, int domainRadius, double domainRadiusSq)
    {
        AABB queryBox = new AABB(
                center.getX() - domainRadius, center.getY() - domainRadius, center.getZ() - domainRadius,
                center.getX() + domainRadius + 1.0D, center.getY() + domainRadius + 1.0D, center.getZ() + domainRadius + 1.0D);
        return level.getEntitiesOfClass(Mob.class, queryBox, entity ->
        {
            if (!entity.isAlive() || entity.getUUID().equals(casterUuid))
            {
                return false;
            }
            if (entity instanceof SpecialEffectEntity)
            {
                return false;
            }
            return isInsideDomainSphere(entity.position(), center, domainRadiusSq);
        });
    }

    /** 座位分配顺序：施法者固定第一，其余按距领域中心由近到远（仅决定谁先拿座，站位点仍外圈优先生成）。 */
    private static void sortTrialTargetsForSeats(List<LivingEntity> targets, BlockPos center, UUID casterUuid)
    {
        targets.sort(Comparator
                .comparingInt((LivingEntity entity) -> entity.getUUID().equals(casterUuid) ? 0 : 1)
                .thenComparingDouble((LivingEntity entity) -> horizontalDistSqFromCenter(entity.position(), center))
                .thenComparingDouble(entity -> trialSortAngle(entity, center)));
    }

    private static int computeSeatRingFromDistSq(double distSq)
    {
        if (distSq <= 1.0E-6D)
        {
            return 1;
        }
        return Mth.clamp((int) Math.ceil(Math.sqrt(distSq)), 1, TRIAL_POSITION_SORT_RADIUS);
    }

    private static double trialSortAngle(LivingEntity entity, BlockPos center)
    {
        double dx = entity.getX() - (center.getX() + 0.5D);
        double dz = entity.getZ() - (center.getZ() + 0.5D);
        return Math.atan2(dz, dx);
    }

    private static int computeHorizontalRing(int dx, int dz, int maxRing)
    {
        int distSq = dx * dx + dz * dz;
        if (distSq == 0)
        {
            return 1;
        }
        return Mth.clamp((int) Math.ceil(Math.sqrt(distSq)), 1, maxRing);
    }

    private static List<Vec3> generateTrialSeatPositionsOuterFirst(BlockPos center, int footY)
    {
        List<Vec3> seats = new ArrayList<>();
        double centerX = center.getX() + 0.5D;
        double centerZ = center.getZ() + 0.5D;
        double standY = footY + 1.0D;
        double sortRadiusSq = (double) TRIAL_POSITION_SORT_RADIUS * TRIAL_POSITION_SORT_RADIUS;

        for (int ring = TRIAL_POSITION_SORT_RADIUS; ring >= 1; ring--)
        {
            List<Vec3> ringSeats = new ArrayList<>();
            for (int dx = -TRIAL_POSITION_SORT_RADIUS; dx <= TRIAL_POSITION_SORT_RADIUS; dx += TRIAL_SEAT_GRID_SPACING)
            {
                for (int dz = -TRIAL_POSITION_SORT_RADIUS; dz <= TRIAL_POSITION_SORT_RADIUS; dz += TRIAL_SEAT_GRID_SPACING)
                {
                    double offsetDistSq = (double) dx * dx + (double) dz * dz;
                    if (offsetDistSq > sortRadiusSq)
                    {
                        continue;
                    }
                    if (computeSeatRingFromDistSq(offsetDistSq) != ring)
                    {
                        continue;
                    }
                    ringSeats.add(new Vec3(center.getX() + dx + 0.5D, standY, center.getZ() + dz + 0.5D));
                }
            }
            ringSeats.sort(Comparator.comparingDouble(pos -> Math.atan2(pos.z - centerZ, pos.x - centerX)));
            seats.addAll(ringSeats);
        }

        return seats;
    }

    /**
     * 单人审理：施法者与被告距领域中心各 {@link #TRIAL_POSITION_SORT_RADIUS} 格，
     * 同一直线、朝向中心、面对面。
     */
    private void applySingleTrialFaceToFacePlacement(
            ServerLevel level,
            ServerPlayer caster,
            LivingEntity defendant,
            BlockPos center,
            int footY,
            UUID casterUuid)
    {
        double centerX = center.getX() + 0.5D;
        double centerZ = center.getZ() + 0.5D;
        Vec3 axis = horizontalUnitFromCenter(defendant.position(), centerX, centerZ);
        double seatDistance = TRIAL_POSITION_SORT_RADIUS;

        Vec3 defendantSeat = seatOnCenterLine(center, footY, axis, seatDistance);
        Vec3 casterSeat = seatOnCenterLine(center, footY, axis, -seatDistance);

        placeTrialSortTarget(level, defendant, defendantSeat, center, casterUuid);
        placeTrialSortTarget(level, caster, casterSeat, center, casterUuid);
    }

    private static Vec3 horizontalUnitFromCenter(Vec3 from, double centerX, double centerZ)
    {
        double dx = from.x - centerX;
        double dz = from.z - centerZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4D)
        {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return new Vec3(dx / length, 0.0D, dz / length);
    }

    private static Vec3 seatOnCenterLine(BlockPos center, int footY, Vec3 axis, double signedDistance)
    {
        double centerX = center.getX() + 0.5D;
        double centerZ = center.getZ() + 0.5D;
        double standY = footY + 1.0D;
        return new Vec3(
                centerX + axis.x * signedDistance,
                standY,
                centerZ + axis.z * signedDistance);
    }

    private void placeTrialSortTarget(ServerLevel level, LivingEntity target, Vec3 seat, BlockPos center, UUID ownerUuid)
    {
        float yaw = computeLookYawTowardCenter(seat, center);
        float pitch = computeLookPitchTowardCenter(seat, center, target.getEyeHeight());

        if (target instanceof ServerPlayer player)
        {
            player.connection.teleport(seat.x, seat.y, seat.z, yaw, pitch);
            updateTrialPlayerAssignedPosition(player, ownerUuid, seat);
        }
        else
        {
            target.moveTo(seat.x, seat.y, seat.z, yaw, pitch);
            target.setYHeadRot(yaw);
            target.setDeltaMovement(Vec3.ZERO);
        }

        spawnCourtSeatInFrontOf(level, seat, center, ownerUuid, yaw);
        placeTrialSeatLightBlocks(level, seat, ownerUuid);
        mountTrialSpotlightFx(level, target, ownerUuid);
    }

    private void placeTrialOverflowTarget(
            ServerLevel level, LivingEntity target, BlockPos center, int footY, UUID ownerUuid)
    {
        Vec3 pos = randomAudienceRingPosition(level, center, footY);
        float yaw = computeLookYawTowardCenter(pos, center);
        float pitch = computeLookPitchTowardCenter(pos, center, target.getEyeHeight());

        if (target instanceof ServerPlayer player)
        {
            player.connection.teleport(pos.x, pos.y, pos.z, yaw, pitch);
            updateTrialPlayerAssignedPosition(player, ownerUuid, pos);
        }
        else
        {
            target.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
            target.setYHeadRot(yaw);
            target.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static Vec3 randomAudienceRingPosition(ServerLevel level, BlockPos center, int footY)
    {
        double centerX = center.getX() + 0.5D;
        double centerZ = center.getZ() + 0.5D;
        double standY = footY + 1.0D;
        double span = AUDIENCE_RING_OUTER - AUDIENCE_RING_INNER;
        double radius = AUDIENCE_RING_INNER + (0.001D + level.random.nextDouble() * (span - 0.002D));
        double angle = level.random.nextDouble() * (Math.PI * 2.0D);
        return new Vec3(centerX + radius * Math.cos(angle), standY, centerZ + radius * Math.sin(angle));
    }

    private void spawnCourtSeatInFrontOf(ServerLevel level, Vec3 occupantSeat, BlockPos center, UUID casterUuid, float occupantYaw)
    {
        double yawRad = Math.toRadians(occupantYaw);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        Vec3 courtSeatPos = new Vec3(
                occupantSeat.x + forwardX,
                occupantSeat.y,
                occupantSeat.z + forwardZ);
        // 椅背朝向领域中心（实体前向背对中心）；渲染朝向由 CourtSeatRenderer 读取 yRot
        float courtYaw = Mth.wrapDegrees(computeLookYawTowardCenter(courtSeatPos, center) + 180.0F);

        CourtSeatEntity courtSeat = ModEntities.COURT_SEAT.get().create(level);
        if (courtSeat == null)
        {
            return;
        }

        courtSeat.moveTo(courtSeatPos.x, courtSeatPos.y, courtSeatPos.z, courtYaw, 0.0F);
        if (!level.addFreshEntity(courtSeat))
        {
            return;
        }

        courtSeatEntityUuidsByCaster
                .computeIfAbsent(casterUuid, ignored -> ConcurrentHashMap.newKeySet())
                .add(courtSeat.getUUID());
    }

    private void removeCourtSeatsForCaster(ServerPlayer player)
    {
        Set<UUID> seatUuids = courtSeatEntityUuidsByCaster.remove(player.getUUID());
        if (seatUuids == null || seatUuids.isEmpty())
        {
            return;
        }

        ServerLevel level = player.serverLevel();
        for (UUID seatUuid : seatUuids)
        {
            Entity entity = level.getEntity(seatUuid);
            if (entity != null)
            {
                entity.discard();
            }
        }
    }

    private void mountTrialSpotlightFx(ServerLevel level, LivingEntity target, UUID casterUuid)
    {
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterUuid);
        if (caster == null)
        {
            return;
        }

        OwnerOnlyTrialSpotlightFx.mount(target, getPlayersInsideActiveDomain(caster));
        trialSpotlightEntityUuidsByCaster
                .computeIfAbsent(casterUuid, ignored -> ConcurrentHashMap.newKeySet())
                .add(target.getUUID());
    }

    private void removeTrialSpotlightForCaster(ServerPlayer player, BarrierInstance instance)
    {
        Set<UUID> entityUuids = trialSpotlightEntityUuidsByCaster.remove(player.getUUID());
        if (entityUuids == null || entityUuids.isEmpty())
        {
            return;
        }

        ServerLevel level = player.server.getLevel(instance.getDimension());
        if (level == null)
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        List<ServerPlayer> viewers = findPlayersInsideDomain(level, center, radius, radiusSqr);

        for (UUID entityUuid : entityUuids)
        {
            Entity entity = level.getEntity(entityUuid);
            if (entity == null)
            {
                continue;
            }

            OwnerOnlyTrialSpotlightFx.remove(entity, viewers);
            if (entity instanceof LivingEntity living && DeathSentenceEffect.isActive(living))
            {
                DeathSentenceEffect.mountSpotlight(living);
            }
        }
    }

    private void placeDomainCenterLightBlocks(ServerPlayer player, BarrierInstance instance)
    {
        ServerLevel level = player.server.getLevel(instance.getDimension());
        if (level == null)
        {
            return;
        }

        BlockPos center = instance.getCenter();
        Map<BlockPos, BlockState> placed = trialSeatLightBlocksByCaster
                .computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());

        for (int offsetY : DOMAIN_CENTER_LIGHT_OFFSETS)
        {
            BlockPos pos = center.offset(0, offsetY, 0);
            if (placed.containsKey(pos))
            {
                continue;
            }
            BlockState existing = level.getBlockState(pos);
            if (!existing.isAir())
            {
                continue;
            }
            placed.put(pos.immutable(), existing);
            level.setBlock(pos, DOMAIN_CENTER_LIGHT_BLOCK_STATE, LIGHT_BLOCK_UPDATE_FLAGS);
        }
    }

    private void placeTrialSeatLightBlocks(ServerLevel level, Vec3 seat, UUID casterUuid)
    {
        int x = Mth.floor(seat.x);
        int z = Mth.floor(seat.z);
        // 从脚所在格起向上放，不覆盖其下方石砖地板（floor(seat.y) - 1）
        int baseY = Mth.floor(seat.y);
        Map<BlockPos, BlockState> placed = trialSeatLightBlocksByCaster
                .computeIfAbsent(casterUuid, ignored -> new HashMap<>());

        for (int i = 0; i < TRIAL_SEAT_LIGHT_BLOCK_COUNT; i++)
        {
            BlockPos pos = new BlockPos(x, baseY + i, z);
            if (placed.containsKey(pos))
            {
                continue;
            }
            BlockState existing = level.getBlockState(pos);
            if (!existing.isAir())
            {
                continue;
            }
            placed.put(pos.immutable(), existing);
            level.setBlock(pos, TRIAL_SEAT_LIGHT_BLOCK_STATE, LIGHT_BLOCK_UPDATE_FLAGS);
        }
    }

    private void removeTrialSeatLightBlocksForCaster(ServerPlayer player)
    {
        Map<BlockPos, BlockState> placed = trialSeatLightBlocksByCaster.remove(player.getUUID());
        if (placed == null || placed.isEmpty())
        {
            return;
        }

        ServerLevel level = player.serverLevel();
        for (Map.Entry<BlockPos, BlockState> entry : placed.entrySet())
        {
            BlockPos pos = entry.getKey();
            if (level.getBlockState(pos).is(Blocks.LIGHT))
            {
                level.setBlock(pos, entry.getValue(), LIGHT_BLOCK_UPDATE_FLAGS);
            }
        }
    }

    private static float computeLookYawTowardCenter(Vec3 seat, BlockPos center)
    {
        double targetX = center.getX() + 0.5D;
        double targetZ = center.getZ() + 0.5D;
        double dx = targetX - seat.x;
        double dz = targetZ - seat.z;
        return Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F);
    }

    private static float computeLookPitchTowardCenter(Vec3 seat, BlockPos center, float eyeHeight)
    {
        double targetX = center.getX() + 0.5D;
        double targetY = center.getY() + 0.5D;
        double targetZ = center.getZ() + 0.5D;
        double dx = targetX - seat.x;
        double dy = targetY - (seat.y + eyeHeight);
        double dz = targetZ - seat.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        return (float) (-(Mth.atan2(dy, horizDist) * (180.0D / Math.PI)));
    }

    private void removeChubukushishiForCaster(ServerPlayer player)
    {
        UUID entityUuid = chubukushishiEntityByCaster.remove(player.getUUID());
        if (entityUuid == null)
        {
            return;
        }

        Entity entity = player.serverLevel().getEntity(entityUuid);
        if (entity != null)
        {
            entity.discard();
        }
    }

    private void beginFloorStoneSpread(ServerPlayer player, BarrierInstance instance)
    {
        BlockPos center = instance.getCenter();
        int footY = instance.getPlayerFootY();
        List<List<BlockPos>> rings = new ArrayList<>(FLOOR_STONE_RADIUS);
        for (int i = 0; i < FLOOR_STONE_RADIUS; i++)
        {
            rings.add(new ArrayList<>());
        }

        int radiusSq = FLOOR_STONE_RADIUS * FLOOR_STONE_RADIUS;
        for (int dx = -FLOOR_STONE_RADIUS; dx <= FLOOR_STONE_RADIUS; dx++)
        {
            for (int dz = -FLOOR_STONE_RADIUS; dz <= FLOOR_STONE_RADIUS; dz++)
            {
                if (dx * dx + dz * dz > radiusSq)
                {
                    continue;
                }
                int ring = computeHorizontalRing(dx, dz, FLOOR_STONE_RADIUS);
                rings.get(ring - 1).add(new BlockPos(center.getX() + dx, footY, center.getZ() + dz));
            }
        }

        int firstRing = findNextNonEmptyRing(rings, 0);
        if (firstRing < 0)
        {
            return;
        }

        floorSpreadByCaster.put(
                player.getUUID(),
                new FloorSpreadState(instance.getDimension(), rings, firstRing, 0, 0, new ArrayList<>()));
    }

    private static int findNextNonEmptyRing(List<List<BlockPos>> rings, int startIndex)
    {
        for (int i = startIndex; i < FLOOR_STONE_RADIUS; i++)
        {
            if (!rings.get(i).isEmpty())
            {
                return i;
            }
        }
        return -1;
    }

    private void clearFloorSpreadState(ServerPlayer player)
    {
        FloorSpreadState state = floorSpreadByCaster.remove(player.getUUID());
        if (state == null)
        {
            return;
        }

        ServerLevel level = player.server.getLevel(state.dimension());
        if (level == null)
        {
            return;
        }

        for (PendingFall pending : state.pendingFalls())
        {
            Entity entity = level.getEntity(pending.entityUuid());
            if (entity != null)
            {
                entity.discard();
            }
        }
    }

    private void tickFloorStoneSpread(ServerPlayer player)
    {
        FloorSpreadState state = floorSpreadByCaster.get(player.getUUID());
        if (state == null)
        {
            return;
        }

        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            clearFloorSpreadState(player);
            return;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        ServerLevel level = player.server.getLevel(state.dimension());
        if (level == null)
        {
            clearFloorSpreadState(player);
            return;
        }

        List<PendingFall> pendingFalls = tickPendingFalls(level, state.pendingFalls());
        int currentRingIndex = state.currentRingIndex();

        if (currentRingIndex < 0 || currentRingIndex >= FLOOR_STONE_RADIUS)
        {
            if (pendingFalls.isEmpty())
            {
                floorSpreadByCaster.remove(player.getUUID());
            }
            else
            {
                floorSpreadByCaster.put(player.getUUID(), copyState(state, currentRingIndex, state.nextIndexInRing(), 0, pendingFalls));
            }
            return;
        }

        List<BlockPos> currentRing = state.rings().get(currentRingIndex);
        int nextIndexInRing = state.nextIndexInRing();
        boolean ringSpawnComplete = nextIndexInRing >= currentRing.size();

        if (ringSpawnComplete)
        {
            if (pendingFalls.isEmpty())
            {
                int nextRing = findNextNonEmptyRing(state.rings(), currentRingIndex + 1);
                if (nextRing < 0)
                {
                    floorSpreadByCaster.remove(player.getUUID());
                }
                else
                {
                    floorSpreadByCaster.put(
                            player.getUUID(),
                            copyState(state, nextRing, 0, 0, pendingFalls));
                }
            }
            else
            {
                floorSpreadByCaster.put(
                        player.getUUID(),
                        copyState(state, currentRingIndex, nextIndexInRing, 0, pendingFalls));
            }
            return;
        }

        if (state.ticksUntilNextBatch() > 0)
        {
            floorSpreadByCaster.put(
                    player.getUUID(),
                    copyState(state, currentRingIndex, nextIndexInRing, state.ticksUntilNextBatch() - 1, pendingFalls));
            return;
        }

        int ticksAfterBatch = 0;
        if (usesFallAnimation(currentRingIndex))
        {
            int spawned = 0;
            while (nextIndexInRing < currentRing.size() && spawned < FLOOR_STONE_BATCH)
            {
                spawnFallingStoneBrick(level, instance, currentRing.get(nextIndexInRing), pendingFalls);
                nextIndexInRing++;
                spawned++;
            }
            ticksAfterBatch = FLOOR_STONE_SPREAD_INTERVAL_TICKS;
        }
        else
        {
            while (nextIndexInRing < currentRing.size())
            {
                placeStoneBrickInstant(level, instance, currentRing.get(nextIndexInRing));
                nextIndexInRing++;
            }
        }

        floorSpreadByCaster.put(
                player.getUUID(),
                copyState(
                        state,
                        currentRingIndex,
                        nextIndexInRing,
                        ticksAfterBatch,
                        pendingFalls));
    }

    private static boolean usesFallAnimation(int ringIndex)
    {
        return ringIndex + 1 > FLOOR_STONE_INSTANT_RING_COUNT;
    }

    private void placeStoneBrickInstant(ServerLevel level, BarrierInstance instance, BlockPos targetPos)
    {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState current = level.getBlockState(targetPos);
        if (ShaderedCompat.isSkyboxBlock(current) || current.is(getBarrierBlockState().getBlock()))
        {
            level.setBlock(targetPos, stoneBricks, AbstractBarrierTechnique.BLOCK_UPDATE_FLAGS);
            instance.getSkyboxBlocks().remove(targetPos);
            return;
        }
        if (!current.is(Blocks.STONE_BRICKS))
        {
            instance.getSkyboxBlocks().remove(targetPos);
        }
    }

    private static FloorSpreadState copyState(
            FloorSpreadState state,
            int currentRingIndex,
            int nextIndexInRing,
            int ticksUntilNextBatch,
            List<PendingFall> pendingFalls)
    {
        return new FloorSpreadState(
                state.dimension(),
                state.rings(),
                currentRingIndex,
                nextIndexInRing,
                ticksUntilNextBatch,
                pendingFalls);
    }

    private List<PendingFall> tickPendingFalls(ServerLevel level, List<PendingFall> pendingFalls)
    {
        if (pendingFalls.isEmpty())
        {
            return pendingFalls;
        }

        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        List<PendingFall> next = new ArrayList<>(pendingFalls.size());

        for (PendingFall pending : pendingFalls)
        {
            PendingFall aged = pending.ageOneTick();
            BlockPos targetPos = aged.targetPos();

            if (level.getBlockState(targetPos).is(Blocks.STONE_BRICKS))
            {
                continue;
            }

            if (aged.ageTicks() >= FLOOR_STONE_FALL_TIMEOUT_TICKS)
            {
                Entity entity = level.getEntity(aged.entityUuid());
                if (entity != null)
                {
                    entity.discard();
                }
                level.setBlock(targetPos, stoneBricks, AbstractBarrierTechnique.BLOCK_UPDATE_FLAGS);
                continue;
            }

            Entity entity = level.getEntity(aged.entityUuid());
            if (entity == null || !entity.isAlive())
            {
                level.setBlock(targetPos, stoneBricks, AbstractBarrierTechnique.BLOCK_UPDATE_FLAGS);
                continue;
            }

            next.add(aged);
        }

        return next;
    }

    private void spawnFallingStoneBrick(
            ServerLevel level,
            BarrierInstance instance,
            BlockPos targetPos,
            List<PendingFall> pendingFalls)
    {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState current = level.getBlockState(targetPos);
        if (!ShaderedCompat.isSkyboxBlock(current) && !current.is(getBarrierBlockState().getBlock()))
        {
            if (!current.is(Blocks.STONE_BRICKS))
            {
                level.setBlock(targetPos, stoneBricks, AbstractBarrierTechnique.BLOCK_UPDATE_FLAGS);
            }
            instance.getSkyboxBlocks().remove(targetPos);
            return;
        }

        instance.getSkyboxBlocks().remove(targetPos);
        level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), AbstractBarrierTechnique.BLOCK_UPDATE_FLAGS);

        BlockPos spawnPos = targetPos.above(FLOOR_STONE_FALL_SPAWN_HEIGHT);
        if (!level.getBlockState(spawnPos).isAir())
        {
            level.setBlock(targetPos, stoneBricks, AbstractBarrierTechnique.BLOCK_UPDATE_FLAGS);
            return;
        }

        FallingBlockEntity falling = FallingBlockEntity.fall(level, spawnPos, stoneBricks);
        if (falling == null)
        {
            level.setBlock(targetPos, stoneBricks, AbstractBarrierTechnique.BLOCK_UPDATE_FLAGS);
            return;
        }

        falling.dropItem = false;
        falling.setHurtsEntities(0, 0);
        pendingFalls.add(new PendingFall(targetPos.immutable(), falling.getUUID(), 0));
    }

    private void beginTrialFreeze(ServerPlayer caster, BarrierInstance instance)
    {
        captureTrialPlayersInDomain(caster, instance);
        captureTrialMobsInDomain(caster, instance);
    }

    private void tickTrialFreeze(ServerPlayer owner)
    {
        BarrierInstance instance = getActiveBarrierInstance(owner);
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        captureTrialPlayersInDomain(owner, instance);
        tickTrialPlayerLocksForOwner(owner);
        captureTrialMobsInDomain(owner, instance);
    }

    private void captureTrialPlayersInDomain(ServerPlayer owner, BarrierInstance instance)
    {
        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        UUID ownerUuid = owner.getUUID();

        for (ServerPlayer player : findPlayersInsideDomain(level, center, radius, radiusSqr))
        {
            UUID playerUuid = player.getUUID();
            if (trialPlayerLockByPlayer.containsKey(playerUuid))
            {
                continue;
            }

            boolean wasInvulnerable = player.isInvulnerable();
            if (!wasInvulnerable)
            {
                player.setInvulnerable(true);
            }
            trialPlayerLockByPlayer.put(playerUuid, new TrialPlayerLock(
                    ownerUuid,
                    player.position(),
                    wasInvulnerable,
                    false));
            ModNetwork.sendPlayerInputLockSync(player, true);
        }
    }

    private void tickTrialPlayerLocksForOwner(ServerPlayer owner)
    {
        UUID ownerUuid = owner.getUUID();
        List<UUID> lockedPlayerUuids = new ArrayList<>();
        for (Map.Entry<UUID, TrialPlayerLock> entry : trialPlayerLockByPlayer.entrySet())
        {
            if (entry.getValue().ownerUuid().equals(ownerUuid))
            {
                lockedPlayerUuids.add(entry.getKey());
            }
        }

        MinecraftServer server = owner.getServer();
        if (server == null)
        {
            return;
        }

        for (UUID playerUuid : lockedPlayerUuids)
        {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null)
            {
                tickTrialPlayerLock(player);
            }
        }
    }

    private void tickTrialPlayerLock(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        TrialPlayerLock lockState = trialPlayerLockByPlayer.get(uuid);
        if (lockState == null)
        {
            return;
        }

        if (!player.isAlive())
        {
            releaseTrialPlayerLock(player, lockState);
            trialPlayerLockByPlayer.remove(uuid);
            return;
        }

        if (!player.isInvulnerable())
        {
            player.setInvulnerable(true);
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;

        Vec3 lockPos = lockState.lockPos();
        double maxDriftSq = lockState.assignedPosition()
                ? TRIAL_ASSIGNED_POSITION_MAX_DRIFT_SQ
                : 1.0E-6D;
        if (player.position().distanceToSqr(lockPos) > maxDriftSq)
        {
            player.connection.teleport(lockPos.x, lockPos.y, lockPos.z, player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        }
    }

    private void updateTrialPlayerAssignedPosition(ServerPlayer player, UUID ownerUuid, Vec3 position)
    {
        TrialPlayerLock existing = trialPlayerLockByPlayer.get(player.getUUID());
        boolean wasInvulnerable = existing != null ? existing.wasInvulnerable() : player.isInvulnerable();
        trialPlayerLockByPlayer.put(
                player.getUUID(),
                new TrialPlayerLock(ownerUuid, position, wasInvulnerable, true));
    }

    private void captureTrialMobsInDomain(ServerPlayer owner, BarrierInstance instance)
    {
        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        UUID ownerUuid = owner.getUUID();

        for (Mob mob : findTrialPauseTargets(level, ownerUuid, center, radius, radiusSqr))
        {
            pauseTrialMob(ownerUuid, mob);
        }
    }

    private static List<Mob> findTrialPauseTargets(
            ServerLevel level, UUID casterUuid, BlockPos center, int radius, double radiusSqr)
    {
        AABB queryBox = new AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius + 1.0D, center.getY() + radius + 1.0D, center.getZ() + radius + 1.0D);
        return level.getEntitiesOfClass(Mob.class, queryBox, entity ->
        {
            if (!entity.isAlive() || entity.getUUID().equals(casterUuid))
            {
                return false;
            }
            if (entity instanceof SpecialEffectEntity)
            {
                return false;
            }
            return isInsideDomainSphere(entity.position(), center, radiusSqr);
        });
    }

    private void pauseTrialMob(UUID ownerUuid, Mob mob)
    {
        UUID entityUuid = mob.getUUID();
        if (trialMobPauseByEntity.containsKey(entityUuid))
        {
            if (!mob.isInvulnerable())
            {
                mob.setInvulnerable(true);
            }
            if (trialMobPauseByEntity.get(entityUuid).aiPausedByThisLock())
            {
                mob.setNoAi(true);
            }
            mob.setDeltaMovement(Vec3.ZERO);
            return;
        }

        boolean originNoAi = mob.isNoAi();
        boolean aiPausedByThisLock = false;
        if (!originNoAi)
        {
            mob.setNoAi(true);
            aiPausedByThisLock = true;
        }
        boolean wasInvulnerable = mob.isInvulnerable();
        if (!wasInvulnerable)
        {
            mob.setInvulnerable(true);
        }
        mob.setDeltaMovement(Vec3.ZERO);
        trialMobPauseByEntity.put(entityUuid, new TrialMobPause(ownerUuid, aiPausedByThisLock, originNoAi, wasInvulnerable));
    }

    private void endTrialFreeze(ServerPlayer caster)
    {
        UUID casterUuid = caster.getUUID();
        MinecraftServer server = caster.getServer();

        Iterator<Map.Entry<UUID, TrialPlayerLock>> playerLockIterator = trialPlayerLockByPlayer.entrySet().iterator();
        while (playerLockIterator.hasNext())
        {
            Map.Entry<UUID, TrialPlayerLock> entry = playerLockIterator.next();
            if (!entry.getValue().ownerUuid().equals(casterUuid))
            {
                continue;
            }

            if (server != null)
            {
                ServerPlayer lockedPlayer = server.getPlayerList().getPlayer(entry.getKey());
                if (lockedPlayer != null)
                {
                    releaseTrialPlayerLock(lockedPlayer, entry.getValue());
                }
            }
            playerLockIterator.remove();
        }

        ServerLevel level = caster.serverLevel();
        Iterator<Map.Entry<UUID, TrialMobPause>> mobIterator = trialMobPauseByEntity.entrySet().iterator();
        while (mobIterator.hasNext())
        {
            Map.Entry<UUID, TrialMobPause> entry = mobIterator.next();
            if (!entry.getValue().ownerUuid().equals(casterUuid))
            {
                continue;
            }

            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof Mob mob)
            {
                releaseTrialMob(mob, entry.getValue());
            }
            mobIterator.remove();
        }
    }

    private static void releaseTrialPlayerLock(ServerPlayer player, TrialPlayerLock lockState)
    {
        player.setInvulnerable(lockState.wasInvulnerable());
        ModNetwork.sendPlayerInputLockSync(player, false);
    }

    private static void releaseTrialMob(Mob mob, TrialMobPause pauseState)
    {
        if (pauseState.aiPausedByThisLock())
        {
            mob.setNoAi(pauseState.originNoAi());
        }
        mob.setInvulnerable(pauseState.wasInvulnerable());
    }

    private BarrierInstance getActiveBarrierInstance(ServerPlayer owner)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return null;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return null;
        }

        return instance;
    }

    /** 领域结界已构建完成且仍在维持中。 */
    public boolean hasActiveCompletedDomain(ServerPlayer owner)
    {
        return getActiveBarrierInstance(owner) != null;
    }

    /** 当前活跃领域的中心方块坐标；无活跃领域时返回 {@code null}。 */
    public BlockPos getActiveDomainCenter(ServerPlayer owner)
    {
        BarrierInstance instance = getActiveBarrierInstance(owner);
        return instance != null ? instance.getCenter() : null;
    }

    /**
     * 若实体位于某施法者已完成的诛伏赐死领域内，返回该施法者；否则 {@code null}。
     */
    public ServerPlayer findActiveDomainCasterForEntity(LivingEntity entity)
    {
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level))
        {
            return null;
        }

        UUID entityUuid = entity.getUUID();
        for (Map.Entry<UUID, Set<UUID>> entry : trialSpotlightEntityUuidsByCaster.entrySet())
        {
            if (!entry.getValue().contains(entityUuid))
            {
                continue;
            }
            ServerPlayer caster = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (caster != null && hasActiveCompletedDomain(caster))
            {
                return caster;
            }
        }

        for (UUID casterUuid : ACTIVE_DOMAINS.keySet())
        {
            ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterUuid);
            if (caster == null || !hasActiveCompletedDomain(caster))
            {
                continue;
            }
            BarrierInstance instance = getActiveBarrierInstance(caster);
            if (instance == null)
            {
                continue;
            }
            double radiusSqr = (double) instance.getRadius() * instance.getRadius();
            if (isInsideDomainSphere(entity.position(), instance.getCenter(), radiusSqr))
            {
                return caster;
            }
        }

        return null;
    }

    /** 记录领域内聚光灯目标，便于关闭领域时统一卸载。 */
    public void trackTrialSpotlightEntity(UUID casterUuid, UUID entityUuid)
    {
        trialSpotlightEntityUuidsByCaster
                .computeIfAbsent(casterUuid, ignored -> ConcurrentHashMap.newKeySet())
                .add(entityUuid);
    }

    /** 当前已完成领域结界球内的所有玩家（用于审判广播）。 */
    public List<ServerPlayer> getPlayersInsideActiveDomain(ServerPlayer owner)
    {
        BarrierInstance instance = getActiveBarrierInstance(owner);
        if (instance == null)
        {
            return List.of();
        }

        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return List.of();
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        return findPlayersInsideDomain(level, center, radius, radiusSqr);
    }

    /** 裁决完成后自动解除当前领域。 */
    public void closeActiveDomainAfterVerdict(ServerPlayer caster)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(caster.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        queue.removeLast();
        if (queue.isEmpty())
        {
            ACTIVE_DOMAINS.remove(caster.getUUID());
        }

        skipMeltdownOnAutoCloseByCaster.put(caster.getUUID(), true);
        clearBarrierInstance(caster, instance);
        String domainName = getDomainName();
        String nameDisplay = domainName.isEmpty() ? "「" + DOMAIN_NAME + "」" : "「" + domainName + "」";
        caster.displayClientMessage(
                Component.literal(nameDisplay + "已解除")
                        .withStyle(ChatFormatting.YELLOW),
                true);
    }

    private static List<ServerPlayer> findPlayersInsideDomain(ServerLevel level, BlockPos center, int radius, double radiusSqr)
    {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : level.players())
        {
            if (isInsideDomainSphere(player.position(), center, radiusSqr))
            {
                result.add(player);
            }
        }
        return result;
    }

    private static boolean isInsideDomainSphere(Vec3 entityPos, BlockPos center, double radiusSqr)
    {
        double dx = entityPos.x - (center.getX() + 0.5D);
        double dy = entityPos.y - (center.getY() + 0.5D);
        double dz = entityPos.z - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz < radiusSqr;
    }
}
