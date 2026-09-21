package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.JudgmentTrialTargetData;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseEvidence;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseResponse;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseVerdictLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentCrimeLibrary;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentJudgeEntityLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentSoundLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentWeaponLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「诛伏赐死」领域审判：单人指控式 / 多人「审判」技能裁决。
 * 惩罚 Buff 在领域关闭瞬间结算，审判过程中仅播报与标题演出。
 */
public final class JudgmentDomainTrialLogic
{
    /** 与「指控」一致：辩护选择窗口 10 秒。 */
    public static final int CHOICE_WINDOW_TICKS = 20 * 10;
    /** 聊天栏每条消息间隔 1 秒。 */
    private static final int CHAT_LINE_INTERVAL_TICKS = 20;
    /** 标题「审理...」点动画间隔（缓慢循环）。 */
    private static final int TITLE_DOT_CYCLE_TICKS = 15;
    /** 非玩家 AI 决策延迟：1～5 秒。 */
    private static final int MOB_RESPONSE_DELAY_MIN_TICKS = 20;
    private static final int MOB_RESPONSE_DELAY_MAX_TICKS = 20 * 5;
    /** 证据「罪行确认」基础概率（%）。 */
    private static final double BASE_EVIDENCE_CONFIRMED_CHANCE_PERCENT = 30.0D;
    /** 开庭演出总时长（tick），6 秒 = 120 tick */
    private static final int OPENING_PERFORMANCE_TOTAL_TICKS = 20 * 6;
    /** 开庭演出：主手替换为常规法槌（tick），1 秒 = 20 tick */
    private static final int OPENING_PERFORMANCE_GAVEL_SWAP_TICK = 20;
    /** 开庭演出：播放开庭音效（tick），5 秒 = 100 tick */
    private static final int OPENING_PERFORMANCE_COURT_SOUND_TICK = 20 * 5;

    private static final ConcurrentHashMap<UUID, DomainTrialSession> sessionByCaster = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, List<PendingDomainPunishment>> pendingPunishmentsByCaster =
            new ConcurrentHashMap<>();
    /** 单人审理死刑：愤怒演出结束后再自动关领域（剩余 tick）。 */
    private static final ConcurrentHashMap<UUID, Integer> pendingDomainCloseAfterAngryPerformanceByCaster =
            new ConcurrentHashMap<>();
    /** 单人审理死刑：裁决已提前结算惩罚，处刑人之剑待领域关闭时授予。 */
    private static final ConcurrentHashMap<UUID, Boolean> pendingExecutionersSwordGrantByCaster =
            new ConcurrentHashMap<>();

    private sealed interface PendingDomainPunishment permits TrialSkillPunishment, AccusePunishment
    {
        UUID targetUuid();

        void apply(ServerPlayer caster, LivingEntity target);
    }

    private record TrialSkillPunishment(UUID targetUuid, JudgmentTrialSkillLogic.DomainTrialVerdictResult result)
            implements PendingDomainPunishment
    {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target)
        {
            if (result.guilty())
            {
                JudgmentDomainAppealLogic.deferAppealEligibility(
                        caster,
                        target,
                        result.crime() != null ? result.crime().chinese() : null);
            }
            JudgmentTrialSkillLogic.applyDomainTrialVerdictResult(caster, target, result);
        }
    }

    private record AccusePunishment(
            UUID targetUuid,
            JudgmentAccuseResponse response,
            JudgmentAccuseEvidence evidence,
            String judgmentCrimeDescription,
            boolean skipAppealDefer)
            implements PendingDomainPunishment
    {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target)
        {
            if (!skipAppealDefer && JudgmentAccuseVerdictLogic.isGuiltyVerdict(response, evidence))
            {
                JudgmentDomainAppealLogic.deferAppealEligibility(caster, target, judgmentCrimeDescription);
            }
            JudgmentAccuseVerdictLogic.applyVerdictEffects(
                    caster,
                    target,
                    response,
                    evidence,
                    judgmentCrimeDescription,
                    true);
        }
    }

    private enum TrialMode
    {
        SINGLE,
        MULTI
    }

    private enum Phase
    {
        AWAITING_START,
        OPENING_PERFORMANCE,
        CHOICE,
        RESOLVING,
        MULTI_TRIAL
    }

    private enum ResolveChatStep
    {
        EVIDENCE,
        VERDICT
    }

    private enum MultiStep
    {
        CRIME_LINE,
        VERDICT_LINE
    }

    private static final class DomainTrialSession
    {
        private TrialMode mode = TrialMode.SINGLE;
        private UUID defendantUuid;
        private List<UUID> defendantUuids = List.of();
        private int currentDefendantIndex;
        private Phase phase = Phase.AWAITING_START;
        private JudgmentCrimeLibrary.CrimeEntry crime;
        private int choiceTicksRemaining = CHOICE_WINDOW_TICKS;
        private int mobResponseDelayTicks;
        private JudgmentAccuseResponse response;
        private boolean responseLocked;
        private boolean resolving;
        private JudgmentAccuseEvidence pendingEvidence;
        private ResolveChatStep resolveChatStep;
        private int resolveChatCooldownTicks;
        private int titleDotPhase;
        private int titleDotCooldown;
        private MultiStep multiStep;
        private int multiChatCooldownTicks;
        private JudgmentTrialSkillLogic.DomainTrialVerdictResult pendingMultiVerdict;
        private int openingPerformanceTicksElapsed;
        private ItemStack openingPerformanceSavedMainHand;
        private boolean appealTrial;
    }

    private JudgmentDomainTrialLogic()
    {
    }

    /** 领域关闭时结算已裁决、尚未施加的惩罚。 */
    public static void applyPendingPunishmentsOnDomainClose(ServerPlayer caster)
    {
        applyPendingPunishmentsOnDomainClose(caster, true);
    }

    /**
     * 结算待施加惩罚。
     *
     * @param grantExecutionersSword 是否在结算后授予处刑人之剑（单人审理提前结算时应为 {@code false}，改在领域关闭时授予）
     */
    public static void applyPendingPunishmentsOnDomainClose(ServerPlayer caster, boolean grantExecutionersSword)
    {
        List<PendingDomainPunishment> pending = pendingPunishmentsByCaster.remove(caster.getUUID());
        if (pending == null || pending.isEmpty())
        {
            return;
        }

        for (PendingDomainPunishment punishment : pending)
        {
            Entity entity = caster.serverLevel().getEntity(punishment.targetUuid());
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved())
            {
                continue;
            }
            punishment.apply(caster, living);
        }

        if (grantExecutionersSword)
        {
            grantExecutionersSwordForDeathSentences(caster, pending);
        }
    }

    /** 单人审理判死刑后，处刑人之剑延迟至领域关闭时授予。 */
    public static void grantPendingExecutionersSwordOnDomainClose(ServerPlayer caster)
    {
        if (!Boolean.TRUE.equals(pendingExecutionersSwordGrantByCaster.remove(caster.getUUID())))
        {
            return;
        }
        JudgmentWeaponLogic.grantExecutionersSwordOnDeathSentence(caster);
    }

    public static void clearPendingExecutionersSwordGrant(UUID casterUuid)
    {
        pendingExecutionersSwordGrantByCaster.remove(casterUuid);
    }

    private static void grantExecutionersSwordForDeathSentences(
            ServerPlayer caster,
            List<PendingDomainPunishment> punishments)
    {
        for (PendingDomainPunishment punishment : punishments)
        {
            Entity entity = caster.serverLevel().getEntity(punishment.targetUuid());
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved())
            {
                continue;
            }
            if (DeathSentenceEffect.isActive(living))
            {
                JudgmentWeaponLogic.grantExecutionersSwordOnDeathSentence(caster);
                return;
            }
        }
    }

    public static boolean isAppealRetrialSession(ServerPlayer caster)
    {
        DomainTrialSession session = sessionByCaster.get(caster.getUUID());
        return session != null && session.appealTrial;
    }

    public static void beginAppealSingleTrial(
            ServerPlayer caster,
            LivingEntity defendant,
            JudgmentCrimeLibrary.CrimeEntry crime)
    {
        DomainTrialSession session = new DomainTrialSession();
        session.mode = TrialMode.SINGLE;
        session.appealTrial = true;
        session.defendantUuid = defendant.getUUID();
        session.crime = crime != null
                ? crime
                : JudgmentCrimeLibrary.pickForDefendant(defendant, caster.getRandom());
        session.phase = Phase.CHOICE;
        session.choiceTicksRemaining = CHOICE_WINDOW_TICKS;
        sessionByCaster.put(caster.getUUID(), session);

        announceAccusation(caster, defendant, session.crime);
        if (defendant instanceof ServerPlayer targetPlayer)
        {
            syncChoiceHud(targetPlayer, true, session);
        }
        else
        {
            session.response = rollMobResponse(caster.getRandom());
            session.responseLocked = true;
            session.mobResponseDelayTicks = rollMobResponseDelayTicks(caster.getRandom());
        }
    }

    public static void onSortComplete(ServerPlayer caster, List<LivingEntity> seatedDefendants)
    {
        List<LivingEntity> validDefendants = new ArrayList<>();
        for (LivingEntity defendant : seatedDefendants)
        {
            if (defendant.isAlive() && !defendant.getUUID().equals(caster.getUUID()))
            {
                validDefendants.add(defendant);
            }
        }
        if (validDefendants.isEmpty())
        {
            return;
        }

        DomainTrialSession session = new DomainTrialSession();
        if (validDefendants.size() == 1)
        {
            session.mode = TrialMode.SINGLE;
            session.defendantUuid = validDefendants.get(0).getUUID();
        }
        else
        {
            session.mode = TrialMode.MULTI;
            session.defendantUuids = validDefendants.stream().map(LivingEntity::getUUID).toList();
        }
        sessionByCaster.put(caster.getUUID(), session);
        sendStartTrialPrompt(caster);
    }

    public static boolean tryStartTrial(ServerPlayer caster)
    {
        DomainTrialSession session = sessionByCaster.get(caster.getUUID());
        if (session == null || session.phase != Phase.AWAITING_START)
        {
            return false;
        }
        if (!JudgmentDomainExpansion.getInstance().hasActiveCompletedDomain(caster))
        {
            endSession(caster);
            return false;
        }

        beginOpeningPerformance(caster, session);
        return true;
    }

    private static void beginOpeningPerformance(ServerPlayer caster, DomainTrialSession session)
    {
        session.phase = Phase.OPENING_PERFORMANCE;
        session.openingPerformanceTicksElapsed = 0;
        ModNetwork.sendJudgmentTrialOpeningPerformance(caster, caster.getYRot());
        EpicFightCompat.playSwingTheGavelAnimation(caster);
    }

    private static void tickOpeningPerformance(ServerPlayer caster, DomainTrialSession session)
    {
        session.openingPerformanceTicksElapsed++;
        int elapsed = session.openingPerformanceTicksElapsed;

        if (elapsed == OPENING_PERFORMANCE_GAVEL_SWAP_TICK)
        {
            applyOpeningPerformanceGavel(caster, session);
        }
        else if (elapsed == OPENING_PERFORMANCE_COURT_SOUND_TICK)
        {
            BlockPos domainCenter = JudgmentDomainExpansion.getInstance().getActiveDomainCenter(caster);
            if (domainCenter != null)
            {
                JudgmentSoundLogic.playHoldACourtSessionAtDomainCenter(caster.serverLevel(), domainCenter);
            }
        }
        else if (elapsed >= OPENING_PERFORMANCE_TOTAL_TICKS)
        {
            restoreOpeningPerformanceMainHand(caster, session);
            beginTrialAfterOpeningPerformance(caster, session);
        }
    }

    private static void applyOpeningPerformanceGavel(ServerPlayer caster, DomainTrialSession session)
    {
        if (session.openingPerformanceSavedMainHand != null)
        {
            return;
        }
        session.openingPerformanceSavedMainHand = caster.getMainHandItem().copy();
        ItemStack gavel = new ItemStack(ModItems.SM_GAVEL_WEAPON.get());
        JudgmentWeaponLogic.setGavelForm(gavel, JudgmentWeaponLogic.GAVEL_FORM_STANDARD);
        ItemStack oldMainHand = caster.getMainHandItem();
        caster.setItemInHand(InteractionHand.MAIN_HAND, gavel);
        EpicFightCompat.refreshGavelWeaponHeldItem(caster, InteractionHand.MAIN_HAND, oldMainHand, gavel);
        caster.inventoryMenu.broadcastChanges();
    }

    private static void restoreOpeningPerformanceMainHand(ServerPlayer caster, DomainTrialSession session)
    {
        if (session.openingPerformanceSavedMainHand == null)
        {
            return;
        }
        ItemStack currentMainHand = caster.getMainHandItem();
        ItemStack restore = session.openingPerformanceSavedMainHand;
        session.openingPerformanceSavedMainHand = null;
        caster.setItemInHand(InteractionHand.MAIN_HAND, restore);
        EpicFightCompat.refreshGavelWeaponHeldItem(caster, InteractionHand.MAIN_HAND, currentMainHand, restore);
        caster.inventoryMenu.broadcastChanges();
    }

    private static void beginTrialAfterOpeningPerformance(ServerPlayer caster, DomainTrialSession session)
    {
        if (session.mode == TrialMode.MULTI)
        {
            session.phase = Phase.MULTI_TRIAL;
            session.titleDotPhase = 0;
            session.titleDotCooldown = 0;
            session.currentDefendantIndex = 0;
            syncProcessingTitle(caster, session);
            if (!beginMultiDefendant(caster, session))
            {
                finishMultiTrial(caster, session);
            }
            return;
        }

        LivingEntity defendant = resolveDefendant(caster, session);
        if (defendant == null)
        {
            endSession(caster, session);
            return;
        }

        session.phase = Phase.CHOICE;
        session.crime = JudgmentCrimeLibrary.pickForDefendant(defendant, caster.getRandom());
        announceAccusation(caster, defendant, session.crime);

        if (defendant instanceof ServerPlayer targetPlayer)
        {
            syncChoiceHud(targetPlayer, true, session);
        }
        else
        {
            session.response = rollMobResponse(caster.getRandom());
            session.responseLocked = true;
            session.mobResponseDelayTicks = rollMobResponseDelayTicks(caster.getRandom());
        }
    }

    public static void tickMaintain(ServerPlayer caster)
    {
        tickPendingDomainCloseAfterAngryPerformance(caster);

        DomainTrialSession session = sessionByCaster.get(caster.getUUID());
        if (session == null)
        {
            return;
        }

        if (!JudgmentDomainExpansion.getInstance().hasActiveCompletedDomain(caster))
        {
            endSession(caster, session);
            return;
        }

        if (session.phase == Phase.OPENING_PERFORMANCE)
        {
            tickOpeningPerformance(caster, session);
            return;
        }

        if (session.mode == TrialMode.MULTI)
        {
            if (session.phase == Phase.MULTI_TRIAL)
            {
                tickMultiTrial(caster, session);
            }
            return;
        }

        LivingEntity defendant = resolveDefendant(caster, session);
        if (defendant == null)
        {
            endSession(caster, session);
            return;
        }

        if (session.phase == Phase.AWAITING_START)
        {
            return;
        }

        if (session.resolving)
        {
            tickResolveChat(caster, session, defendant);
            return;
        }

        if (session.phase != Phase.CHOICE)
        {
            return;
        }

        if (session.responseLocked)
        {
            if (session.mobResponseDelayTicks > 0)
            {
                session.mobResponseDelayTicks--;
                return;
            }
            beginResolve(caster, session, defendant);
            return;
        }

        session.choiceTicksRemaining--;
        if (defendant instanceof ServerPlayer targetPlayer)
        {
            syncChoiceHud(targetPlayer, true, session);
        }

        if (session.choiceTicksRemaining <= 0)
        {
            session.response = JudgmentAccuseResponse.NONE;
            session.responseLocked = true;
            beginResolve(caster, session, defendant);
        }
    }

    public static boolean submitPlayerResponse(ServerPlayer defendant, int responseId)
    {
        JudgmentAccuseResponse chosen = JudgmentAccuseResponse.fromId(responseId);
        if (chosen == JudgmentAccuseResponse.NONE)
        {
            return false;
        }

        for (Map.Entry<UUID, DomainTrialSession> entry : sessionByCaster.entrySet())
        {
            DomainTrialSession session = entry.getValue();
            if (session.mode != TrialMode.SINGLE
                    || !defendant.getUUID().equals(session.defendantUuid)
                    || session.phase != Phase.CHOICE
                    || session.responseLocked)
            {
                continue;
            }

            ServerPlayer caster = defendant.serverLevel().getServer().getPlayerList().getPlayer(entry.getKey());
            if (caster == null)
            {
                continue;
            }

            LivingEntity target = resolveDefendant(caster, session);
            if (target == null)
            {
                endSession(caster, session);
                continue;
            }

            session.response = chosen;
            session.responseLocked = true;
            beginResolve(caster, session, target);
            return true;
        }
        return false;
    }

    public static void onDomainClosed(ServerPlayer caster)
    {
        clearPendingDomainCloseAfterAngryPerformance(caster.getUUID());
        clearPendingExecutionersSwordGrant(caster.getUUID());
        DomainTrialSession session = sessionByCaster.remove(caster.getUUID());
        if (session != null)
        {
            restoreOpeningPerformanceMainHand(caster, session);
            clearProcessingTitle(caster);
            cleanupDefendantHud(caster, session);
        }
    }

    public static void onLogout(ServerPlayer player)
    {
        clearPendingDomainCloseAfterAngryPerformance(player.getUUID());
        clearPendingExecutionersSwordGrant(player.getUUID());
        JudgmentDomainAppealLogic.clearForCaster(player.getUUID());
        JudgmentDomainAppealLogic.clearForDefendant(player.getUUID());
        DomainTrialSession casterSession = sessionByCaster.remove(player.getUUID());
        if (casterSession != null)
        {
            restoreOpeningPerformanceMainHand(player, casterSession);
            clearProcessingTitle(player);
            cleanupDefendantHud(player, casterSession);
            pendingPunishmentsByCaster.remove(player.getUUID());
            return;
        }

        for (Map.Entry<UUID, DomainTrialSession> entry : sessionByCaster.entrySet())
        {
            DomainTrialSession session = entry.getValue();
            if (session.mode == TrialMode.SINGLE && player.getUUID().equals(session.defendantUuid))
            {
                ServerPlayer caster = player.serverLevel().getServer().getPlayerList().getPlayer(entry.getKey());
                if (caster != null)
                {
                    endSession(caster, session);
                }
                return;
            }
        }
    }

    private static void queuePunishment(ServerPlayer caster, PendingDomainPunishment punishment)
    {
        pendingPunishmentsByCaster.computeIfAbsent(caster.getUUID(), uuid -> new ArrayList<>()).add(punishment);
    }

    private static void tickMultiTrial(ServerPlayer caster, DomainTrialSession session)
    {
        tickProcessingTitle(caster, session);

        if (session.multiChatCooldownTicks > 0)
        {
            session.multiChatCooldownTicks--;
            return;
        }

        LivingEntity defendant = resolveMultiDefendant(caster, session);
        if (defendant == null)
        {
            if (!advanceMultiDefendant(caster, session))
            {
                finishMultiTrial(caster, session);
            }
            return;
        }

        int seatNumber = session.currentDefendantIndex + 1;
        JudgmentTrialSkillLogic.DomainTrialVerdictResult verdict = session.pendingMultiVerdict;

        if (session.multiStep == MultiStep.CRIME_LINE)
        {
            if (verdict.guilty() && verdict.crime() != null)
            {
                broadcastToDomain(caster, Component.literal("座位" + seatNumber + "，罪行：" + verdict.crime().chinese())
                        .withStyle(ChatFormatting.RED));
            }
            else
            {
                broadcastToDomain(caster, Component.literal("座位" + seatNumber + "，无罪。")
                        .withStyle(ChatFormatting.GRAY));
            }
            session.multiStep = MultiStep.VERDICT_LINE;
            session.multiChatCooldownTicks = CHAT_LINE_INTERVAL_TICKS;
            return;
        }

        if (session.multiStep == MultiStep.VERDICT_LINE)
        {
            if (verdict.guilty() && verdict.verdictType() != null)
            {
                JudgmentTrialTargetData.clearTrialCount(caster, defendant.getUUID());
                broadcastToDomain(caster, Component.literal("裁决：" + verdict.verdictType().chineseLabel())
                        .withStyle(ChatFormatting.DARK_RED));
                queuePunishment(caster, new TrialSkillPunishment(defendant.getUUID(), verdict));
                JudgmentTrialSkillLogic.showVerdictTitleToDefendant(defendant, verdict.verdictType());
            }
            else
            {
                broadcastToDomain(caster, Component.literal("裁决：释放")
                        .withStyle(ChatFormatting.GREEN));
            }

            JudgmentTrialSkillLogic.applyTrialCompleteSlowness(defendant);

            if (!advanceMultiDefendant(caster, session))
            {
                finishMultiTrial(caster, session);
            }
        }
    }

    private static boolean beginMultiDefendant(ServerPlayer caster, DomainTrialSession session)
    {
        while (session.currentDefendantIndex < session.defendantUuids.size())
        {
            LivingEntity defendant = resolveMultiDefendant(caster, session);
            if (defendant == null)
            {
                session.currentDefendantIndex++;
                continue;
            }

            session.pendingMultiVerdict = JudgmentTrialSkillLogic.rollDomainTrialVerdict(caster, defendant);
            session.multiStep = MultiStep.CRIME_LINE;
            session.multiChatCooldownTicks = 0;
            return true;
        }
        return false;
    }

    private static boolean advanceMultiDefendant(ServerPlayer caster, DomainTrialSession session)
    {
        session.currentDefendantIndex++;
        return beginMultiDefendant(caster, session);
    }

    private static void finishMultiTrial(ServerPlayer caster, DomainTrialSession session)
    {
        clearProcessingTitle(caster);
        endSession(caster, session);
        JudgmentDomainExpansion.getInstance().closeActiveDomainAfterVerdict(caster);
    }

    private static void tickProcessingTitle(ServerPlayer caster, DomainTrialSession session)
    {
        if (session.titleDotCooldown > 0)
        {
            session.titleDotCooldown--;
            return;
        }

        session.titleDotPhase = (session.titleDotPhase + 1) % 3;
        session.titleDotCooldown = TITLE_DOT_CYCLE_TICKS;
        syncProcessingTitle(caster, session);
    }

    private static void syncProcessingTitle(ServerPlayer caster, DomainTrialSession session)
    {
        String dots = ".".repeat(session.titleDotPhase + 1);
        Component title = Component.literal("审判者正在审理" + dots).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer player : JudgmentDomainExpansion.getInstance().getPlayersInsideActiveDomain(caster))
        {
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    private static void clearProcessingTitle(ServerPlayer caster)
    {
        for (ServerPlayer player : JudgmentDomainExpansion.getInstance().getPlayersInsideActiveDomain(caster))
        {
            player.connection.send(new ClientboundClearTitlesPacket(true));
        }
    }

    private static void broadcastToDomain(ServerPlayer caster, Component message)
    {
        for (ServerPlayer player : JudgmentDomainExpansion.getInstance().getPlayersInsideActiveDomain(caster))
        {
            player.sendSystemMessage(message);
        }
    }

    private static LivingEntity resolveMultiDefendant(ServerPlayer caster, DomainTrialSession session)
    {
        if (session.currentDefendantIndex < 0 || session.currentDefendantIndex >= session.defendantUuids.size())
        {
            return null;
        }
        UUID defendantUuid = session.defendantUuids.get(session.currentDefendantIndex);
        Entity entity = caster.serverLevel().getEntity(defendantUuid);
        if (!(entity instanceof LivingEntity living))
        {
            return null;
        }
        if (!living.isAlive() || living.isRemoved())
        {
            return null;
        }
        return living;
    }

    private static void sendStartTrialPrompt(ServerPlayer caster)
    {
        caster.sendSystemMessage(Component.literal("被告已就位，证据已获取。是否开始审判？")
                .withStyle(ChatFormatting.GOLD));
        caster.sendSystemMessage(clickableStartTrialButton());
    }

    private static MutableComponent clickableStartTrialButton()
    {
        Style style = Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jjkz domainTrial start"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击开始审判")));
        return Component.literal("【开始审判】").withStyle(style);
    }

    private static void beginResolve(ServerPlayer caster, DomainTrialSession session, LivingEntity defendant)
    {
        if (session.resolving)
        {
            return;
        }
        session.resolving = true;
        session.phase = Phase.RESOLVING;

        if (defendant instanceof ServerPlayer targetPlayer)
        {
            syncChoiceHud(targetPlayer, false, session);
        }

        JudgmentAccuseResponse response = session.response != null ? session.response : JudgmentAccuseResponse.NONE;
        session.pendingEvidence = rollEvidence(caster, defendant, caster.getRandom());

        announceResponse(caster, defendant, response);
        session.resolveChatStep = ResolveChatStep.EVIDENCE;
        session.resolveChatCooldownTicks = CHAT_LINE_INTERVAL_TICKS;
    }

    private static void tickResolveChat(ServerPlayer caster, DomainTrialSession session, LivingEntity defendant)
    {
        if (session.resolveChatCooldownTicks > 0)
        {
            session.resolveChatCooldownTicks--;
            return;
        }

        JudgmentAccuseResponse response = session.response != null ? session.response : JudgmentAccuseResponse.NONE;

        if (session.resolveChatStep == ResolveChatStep.EVIDENCE)
        {
            announceEvidence(caster, defendant, session.pendingEvidence);
            session.resolveChatStep = ResolveChatStep.VERDICT;
            session.resolveChatCooldownTicks = CHAT_LINE_INTERVAL_TICKS;
            return;
        }

        if (session.resolveChatStep == ResolveChatStep.VERDICT)
        {
            UUID defendantUuid = defendant.getUUID();

            if (session.appealTrial && !JudgmentAccuseVerdictLogic.isGuiltyVerdict(response, session.pendingEvidence))
            {
                JudgmentAccuseVerdictLogic.sendVerdictMessage(
                        caster,
                        defendant,
                        JudgmentAccuseVerdictLogic.buildVerdictMessage(response, session.pendingEvidence, "上诉"));
                JudgmentDomainAppealLogic.revokeDomainPunishments(defendant);
                JudgmentDomainAppealLogic.clearForDefendant(defendantUuid);
                JudgmentTrialTargetData.clearTrialCount(caster, defendantUuid);
                endSession(caster, session);
                JudgmentDomainExpansion.getInstance().closeActiveDomainAfterVerdict(caster);
                return;
            }

            JudgmentAccuseVerdictLogic.sendVerdictMessage(
                    caster,
                    defendant,
                    JudgmentAccuseVerdictLogic.buildVerdictMessage(response, session.pendingEvidence, "审判"));
            queuePunishment(caster, new AccusePunishment(
                    defendantUuid,
                    response,
                    session.pendingEvidence,
                    session.crime != null ? session.crime.chinese() : null,
                    session.appealTrial));
            JudgmentTrialTargetData.incrementTrialCount(caster, defendantUuid);
            endSession(caster, session);
            applyPendingPunishmentsOnDomainClose(caster, false);
            if (hasActiveDeathSentence(caster, defendantUuid))
            {
                pendingExecutionersSwordGrantByCaster.put(caster.getUUID(), true);
                scheduleDomainCloseAfterAngryPerformance(caster);
            }
            else
            {
                JudgmentDomainExpansion.getInstance().closeActiveDomainAfterVerdict(caster);
            }
        }
    }

    private static boolean hasActiveDeathSentence(ServerPlayer caster, UUID defendantUuid)
    {
        Entity entity = caster.serverLevel().getEntity(defendantUuid);
        return entity instanceof LivingEntity living && DeathSentenceEffect.isActive(living);
    }

    private static void scheduleDomainCloseAfterAngryPerformance(ServerPlayer caster)
    {
        pendingDomainCloseAfterAngryPerformanceByCaster.put(
                caster.getUUID(),
                Math.max(
                        JudgmentTrialSkillLogic.DEATH_VERDICT_TITLE_DELAY_TICKS,
                        JudgmentJudgeEntityLogic.angryPerformanceRetentionTicks()));
    }

    public static boolean isPendingDomainCloseAfterAngryPerformance(UUID casterUuid)
    {
        return pendingDomainCloseAfterAngryPerformanceByCaster.containsKey(casterUuid);
    }

    private static void tickPendingDomainCloseAfterAngryPerformance(ServerPlayer caster)
    {
        Integer remaining = pendingDomainCloseAfterAngryPerformanceByCaster.get(caster.getUUID());
        if (remaining == null)
        {
            return;
        }

        if (remaining <= 1)
        {
            pendingDomainCloseAfterAngryPerformanceByCaster.remove(caster.getUUID());
            if (JudgmentDomainExpansion.getInstance().hasActiveCompletedDomain(caster))
            {
                JudgmentDomainExpansion.getInstance().closeActiveDomainAfterVerdict(caster);
            }
            return;
        }

        pendingDomainCloseAfterAngryPerformanceByCaster.put(caster.getUUID(), remaining - 1);
    }

    public static void clearPendingDomainCloseAfterAngryPerformance(UUID casterUuid)
    {
        pendingDomainCloseAfterAngryPerformanceByCaster.remove(casterUuid);
    }

    private static JudgmentAccuseEvidence rollEvidence(ServerPlayer caster, LivingEntity target, RandomSource random)
    {
        if (SinfulOneEffect.isActive(target))
        {
            return JudgmentAccuseEvidence.CONFIRMED;
        }
        double confirmedChance = BASE_EVIDENCE_CONFIRMED_CHANCE_PERCENT
                + JudgmentTrialTargetData.getGuiltyBonusPercent(caster, target.getUUID());
        confirmedChance = Math.min(100.0D, confirmedChance);
        return random.nextDouble() * 100.0D < confirmedChance
                ? JudgmentAccuseEvidence.CONFIRMED
                : JudgmentAccuseEvidence.CANNOT_CONFIRM;
    }

    private static void endSession(ServerPlayer caster)
    {
        DomainTrialSession session = sessionByCaster.remove(caster.getUUID());
        if (session != null)
        {
            restoreOpeningPerformanceMainHand(caster, session);
            cleanupDefendantHud(caster, session);
        }
    }

    private static void endSession(ServerPlayer caster, DomainTrialSession session)
    {
        restoreOpeningPerformanceMainHand(caster, session);
        sessionByCaster.remove(caster.getUUID());
        cleanupDefendantHud(caster, session);
    }

    private static void cleanupDefendantHud(ServerPlayer caster, DomainTrialSession session)
    {
        if (session.mode != TrialMode.SINGLE || session.defendantUuid == null)
        {
            return;
        }
        Entity entity = caster.serverLevel().getEntity(session.defendantUuid);
        if (entity instanceof ServerPlayer targetPlayer)
        {
            syncChoiceHud(targetPlayer, false, session);
        }
    }

    private static void syncChoiceHud(ServerPlayer targetPlayer, boolean active, DomainTrialSession session)
    {
        if (!active || session.crime == null)
        {
            ModNetwork.sendJudgmentAccuseChoiceSync(targetPlayer, false, 0, "", "");
            return;
        }
        ModNetwork.sendJudgmentAccuseChoiceSync(
                targetPlayer,
                true,
                session.choiceTicksRemaining,
                session.crime.chinese(),
                session.crime.english());
    }

    private static LivingEntity resolveDefendant(ServerPlayer caster, DomainTrialSession session)
    {
        if (session.defendantUuid == null)
        {
            return null;
        }
        Entity entity = caster.serverLevel().getEntity(session.defendantUuid);
        if (!(entity instanceof LivingEntity living))
        {
            return null;
        }
        if (!living.isAlive() || living.isRemoved())
        {
            return null;
        }
        return living;
    }

    private static int rollMobResponseDelayTicks(RandomSource random)
    {
        return MOB_RESPONSE_DELAY_MIN_TICKS + random.nextInt(MOB_RESPONSE_DELAY_MAX_TICKS - MOB_RESPONSE_DELAY_MIN_TICKS + 1);
    }

    private static JudgmentAccuseResponse rollMobResponse(RandomSource random)
    {
        return switch (random.nextInt(4))
        {
            case 0 -> JudgmentAccuseResponse.DENY;
            case 1 -> JudgmentAccuseResponse.LIE;
            case 2 -> JudgmentAccuseResponse.CONFESS;
            default -> JudgmentAccuseResponse.NONE;
        };
    }

    private static void announceAccusation(ServerPlayer caster, LivingEntity target, JudgmentCrimeLibrary.CrimeEntry crime)
    {
        Component message = Component.literal("【审判】审判者指控：")
                .withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(crime.chinese()).withStyle(ChatFormatting.RED))
                .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(crime.english()).withStyle(ChatFormatting.WHITE));
        caster.sendSystemMessage(message);
        if (target instanceof ServerPlayer targetPlayer && !targetPlayer.getUUID().equals(caster.getUUID()))
        {
            targetPlayer.sendSystemMessage(message);
        }
    }

    private static void announceResponse(ServerPlayer caster, LivingEntity target, JudgmentAccuseResponse response)
    {
        Component message = Component.literal("【审判】辩护：")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(response.chinese()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" / " + response.english()).withStyle(ChatFormatting.GRAY));
        caster.sendSystemMessage(message);
        if (target instanceof ServerPlayer targetPlayer && !targetPlayer.getUUID().equals(caster.getUUID()))
        {
            targetPlayer.sendSystemMessage(message);
        }
    }

    private static void announceEvidence(ServerPlayer caster, LivingEntity target, JudgmentAccuseEvidence evidence)
    {
        Component message = Component.literal("【审判】证据：")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(evidence.chinese()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" / " + evidence.english()).withStyle(ChatFormatting.GRAY));
        caster.sendSystemMessage(message);
        if (target instanceof ServerPlayer targetPlayer && !targetPlayer.getUUID().equals(caster.getUUID()))
        {
            targetPlayer.sendSystemMessage(message);
        }
    }
}
