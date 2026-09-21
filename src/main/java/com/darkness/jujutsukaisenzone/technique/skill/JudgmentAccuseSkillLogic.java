package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.entity.JudgeEntity;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.JudgmentTrialTargetData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「指控」技能：准星选目标 → 审判者跟随 → 随机指控 → 辩护选择 → 证据 → 裁决。
 */
public final class JudgmentAccuseSkillLogic
{
    /** 辩护选择窗口：10 秒。 */
    public static final int CHOICE_WINDOW_TICKS = 20 * 10;
    /** 准星选目标最大距离（格）。 */
    private static final double TARGET_MAX_DISTANCE = 32.0D;
    /** 审判者相对施法者身后的水平距离（格）。 */
    private static final double JUDGE_BEHIND_OFFSET = 2.0D;
    /** 非玩家目标 AI 决策延迟（1 秒，便于阅读指控）。 */
    private static final int MOB_RESPONSE_DELAY_TICKS = 20;
    /** 聊天栏每条消息间隔：1 秒。 */
    private static final int CHAT_LINE_INTERVAL_TICKS = 20;
    /** 证据「罪行确认」概率（%），其余为「无法确认」。 */
    public static final double EVIDENCE_CONFIRMED_CHANCE_PERCENT = 30.0D;

    private static final ConcurrentHashMap<UUID, AccuseSession> sessionByCaster = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingAccuseJudgeDismiss> pendingJudgeDismissByCaster =
            new ConcurrentHashMap<>();

    private record PendingAccuseJudgeDismiss(UUID judgeUuid, int ticksRemaining)
    {
    }

    private enum Phase
    {
        CHOICE,
        RESOLVING
    }

    private enum ResolveChatStep
    {
        EVIDENCE,
        VERDICT
    }

    private static final class AccuseSession
    {
        private UUID targetUuid;
        private UUID judgeUuid;
        private JudgmentCrimeLibrary.CrimeEntry crime;
        private Phase phase = Phase.CHOICE;
        private int choiceTicksRemaining = CHOICE_WINDOW_TICKS;
        private int mobResponseDelayTicks;
        private JudgmentAccuseResponse response;
        private boolean responseLocked;
        private boolean resolving;
        private JudgmentAccuseEvidence pendingEvidence;
        private ResolveChatStep resolveChatStep;
        private int resolveChatCooldownTicks;
    }

    private JudgmentAccuseSkillLogic()
    {
    }

    public static boolean isActive(ServerPlayer player)
    {
        return sessionByCaster.containsKey(player.getUUID());
    }

    public static boolean hasCrosshairTarget(ServerPlayer caster)
    {
        return resolveCrosshairTarget(caster, TARGET_MAX_DISTANCE) != null;
    }

    /** 施法者当前「指控」技能随行的审判者；不存在或已失效时返回 null。 */
    public static JudgeEntity findJudgeForCaster(ServerPlayer caster)
    {
        AccuseSession session = sessionByCaster.get(caster.getUUID());
        if (session != null && session.judgeUuid != null)
        {
            JudgeEntity judge = resolveJudgeEntity(caster, session.judgeUuid);
            if (judge != null)
            {
                return judge;
            }
            session.judgeUuid = null;
        }

        PendingAccuseJudgeDismiss pending = pendingJudgeDismissByCaster.get(caster.getUUID());
        if (pending == null)
        {
            return null;
        }

        JudgeEntity judge = resolveJudgeEntity(caster, pending.judgeUuid());
        if (judge != null)
        {
            return judge;
        }

        pendingJudgeDismissByCaster.remove(caster.getUUID());
        return null;
    }

    public static void tickPendingJudgeDismiss(ServerPlayer caster)
    {
        PendingAccuseJudgeDismiss pending = pendingJudgeDismissByCaster.get(caster.getUUID());
        if (pending == null)
        {
            return;
        }

        if (pending.ticksRemaining() <= 1)
        {
            pendingJudgeDismissByCaster.remove(caster.getUUID());
            discardJudgeEntity(caster, pending.judgeUuid());
            return;
        }

        pendingJudgeDismissByCaster.put(
                caster.getUUID(),
                new PendingAccuseJudgeDismiss(pending.judgeUuid(), pending.ticksRemaining() - 1));
    }

    public static boolean tryBeginAccuse(ServerPlayer caster)
    {
        if (isActive(caster) || JudgmentTrialSkillLogic.isLocking(caster))
        {
            return false;
        }

        LivingEntity target = resolveCrosshairTarget(caster, TARGET_MAX_DISTANCE);
        if (target == null)
        {
            caster.displayClientMessage(Component.literal("请对准指控目标").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }

        AccuseSession session = new AccuseSession();
        session.targetUuid = target.getUUID();
        session.crime = JudgmentCrimeLibrary.pickForDefendant(target, caster.getRandom());
        sessionByCaster.put(caster.getUUID(), session);

        spawnJudgeBehindCaster(caster, session);
        JudgmentCrimeLibrary.announceAccusation(caster, target, session.crime);

        if (target instanceof ServerPlayer targetPlayer)
        {
            syncChoiceHud(targetPlayer, true, session);
        }
        else
        {
            session.response = rollMobResponse(caster.getRandom());
            session.responseLocked = true;
            session.mobResponseDelayTicks = MOB_RESPONSE_DELAY_TICKS;
        }

        return true;
    }

    public static void tickMaintain(ServerPlayer caster)
    {
        AccuseSession session = sessionByCaster.get(caster.getUUID());
        if (session == null)
        {
            return;
        }

        if (!caster.isAlive() || !EpicFightCompat.isBattleMode(caster) || !PlayerTechniqueData.isJudgment(caster))
        {
            endSession(caster, session);
            return;
        }

        LivingEntity target = resolveTarget(caster, session.targetUuid);
        if (target == null)
        {
            endSession(caster, session);
            return;
        }

        tickJudgeFollow(caster, session);

        if (session.resolving)
        {
            tickResolveChat(caster, session, target);
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
            beginResolve(caster, session, target);
            return;
        }

        session.choiceTicksRemaining--;
        if (target instanceof ServerPlayer targetPlayer)
        {
            syncChoiceHud(targetPlayer, true, session);
        }

        if (session.choiceTicksRemaining <= 0)
        {
            session.response = JudgmentAccuseResponse.NONE;
            session.responseLocked = true;
            beginResolve(caster, session, target);
        }
    }

    public static void submitPlayerResponse(ServerPlayer defendant, int responseId)
    {
        JudgmentAccuseResponse chosen = JudgmentAccuseResponse.fromId(responseId);
        if (chosen == JudgmentAccuseResponse.NONE)
        {
            return;
        }

        for (Map.Entry<UUID, AccuseSession> entry : sessionByCaster.entrySet())
        {
            AccuseSession session = entry.getValue();
            if (!defendant.getUUID().equals(session.targetUuid)
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

            LivingEntity target = resolveTarget(caster, session.targetUuid);
            if (target == null)
            {
                endSession(caster, session);
                continue;
            }

            session.response = chosen;
            session.responseLocked = true;
            beginResolve(caster, session, target);
            return;
        }
    }

    public static void onLogout(ServerPlayer player)
    {
        PendingAccuseJudgeDismiss pending = pendingJudgeDismissByCaster.remove(player.getUUID());
        if (pending != null)
        {
            discardJudgeEntity(player, pending.judgeUuid());
        }
        AccuseSession casterSession = sessionByCaster.remove(player.getUUID());
        if (casterSession != null)
        {
            cleanupTargetHud(player, casterSession);
            removeJudge(player, casterSession);
            return;
        }

        for (Map.Entry<UUID, AccuseSession> entry : sessionByCaster.entrySet())
        {
            if (!player.getUUID().equals(entry.getValue().targetUuid))
            {
                continue;
            }
            ServerPlayer caster = player.serverLevel().getServer().getPlayerList().getPlayer(entry.getKey());
            if (caster != null)
            {
                endSession(caster, entry.getValue());
            }
            return;
        }
    }

    private static void beginResolve(ServerPlayer caster, AccuseSession session, LivingEntity target)
    {
        if (session.resolving)
        {
            return;
        }
        session.resolving = true;
        session.phase = Phase.RESOLVING;

        if (target instanceof ServerPlayer targetPlayer)
        {
            syncChoiceHud(targetPlayer, false, session);
        }

        JudgmentAccuseResponse response = session.response != null ? session.response : JudgmentAccuseResponse.NONE;
        session.pendingEvidence = rollEvidence(caster, target, caster.getRandom());

        announceResponse(caster, target, response);
        session.resolveChatStep = ResolveChatStep.EVIDENCE;
        session.resolveChatCooldownTicks = CHAT_LINE_INTERVAL_TICKS;
    }

    private static void tickResolveChat(ServerPlayer caster, AccuseSession session, LivingEntity target)
    {
        if (session.resolveChatCooldownTicks > 0)
        {
            session.resolveChatCooldownTicks--;
            return;
        }

        JudgmentAccuseResponse response = session.response != null ? session.response : JudgmentAccuseResponse.NONE;

        if (session.resolveChatStep == ResolveChatStep.EVIDENCE)
        {
            announceEvidence(caster, target, session.pendingEvidence);
            session.resolveChatStep = ResolveChatStep.VERDICT;
            session.resolveChatCooldownTicks = CHAT_LINE_INTERVAL_TICKS;
            return;
        }

        if (session.resolveChatStep == ResolveChatStep.VERDICT)
        {
            JudgmentAccuseVerdictLogic.sendVerdictMessage(
                    caster,
                    target,
                    JudgmentAccuseVerdictLogic.buildVerdictMessage(response, session.pendingEvidence));
            JudgmentAccuseVerdictLogic.applyVerdictEffects(
                    caster,
                    target,
                    response,
                    session.pendingEvidence,
                    session.crime != null ? session.crime.chinese() : null);
            if (DeathSentenceEffect.isActive(target) && session.judgeUuid != null)
            {
                scheduleJudgeDismissAfterAngryPerformance(caster, session.judgeUuid);
                endSession(caster, session, false);
            }
            else
            {
                endSession(caster, session, true);
            }
        }
    }

    private static JudgmentAccuseEvidence rollEvidence(ServerPlayer caster, LivingEntity target, RandomSource random)
    {
        double confirmedChance = EVIDENCE_CONFIRMED_CHANCE_PERCENT
                + JudgmentTrialTargetData.getGuiltyBonusPercent(caster, target.getUUID());
        confirmedChance = Math.min(100.0D, confirmedChance);
        return random.nextDouble() * 100.0D < confirmedChance
                ? JudgmentAccuseEvidence.CONFIRMED
                : JudgmentAccuseEvidence.CANNOT_CONFIRM;
    }

    private static void endSession(ServerPlayer caster, AccuseSession session)
    {
        endSession(caster, session, true);
    }

    private static void endSession(ServerPlayer caster, AccuseSession session, boolean removeJudgeNow)
    {
        sessionByCaster.remove(caster.getUUID());
        cleanupTargetHud(caster, session);
        if (removeJudgeNow)
        {
            removeJudge(caster, session);
        }
    }

    private static void scheduleJudgeDismissAfterAngryPerformance(ServerPlayer caster, UUID judgeUuid)
    {
        int retentionTicks = JudgmentJudgeEntityLogic.ANGRY_ANIMATION_DELAY_TICKS
                + JudgmentJudgeEntityLogic.ANGRY_PERFORMANCE_TOTAL_TICKS
                + JudgeEntity.angryAnimationDurationTicks();
        pendingJudgeDismissByCaster.put(
                caster.getUUID(),
                new PendingAccuseJudgeDismiss(judgeUuid, retentionTicks));
    }

    private static void cleanupTargetHud(ServerPlayer caster, AccuseSession session)
    {
        if (session.targetUuid == null)
        {
            return;
        }
        Entity entity = caster.serverLevel().getEntity(session.targetUuid);
        if (entity instanceof ServerPlayer targetPlayer)
        {
            syncChoiceHud(targetPlayer, false, session);
        }
    }

    private static void syncChoiceHud(ServerPlayer targetPlayer, boolean active, AccuseSession session)
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

    private static void announceResponse(ServerPlayer caster, LivingEntity target, JudgmentAccuseResponse response)
    {
        Component message = Component.literal("【指控】辩护：")
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
        Component message = Component.literal("【指控】证据：")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(evidence.chinese()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" / " + evidence.english()).withStyle(ChatFormatting.GRAY));
        caster.sendSystemMessage(message);
        if (target instanceof ServerPlayer targetPlayer && !targetPlayer.getUUID().equals(caster.getUUID()))
        {
            targetPlayer.sendSystemMessage(message);
        }
    }

    private static LivingEntity resolveCrosshairTarget(ServerPlayer player, double maxDistance)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDistance));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                searchBox,
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && !living.isRemoved()
                        && !living.getUUID().equals(player.getUUID())
                        && living.isAttackable()
                        && !living.isSpectator()
                        && entity.isPickable(),
                maxDistance * maxDistance);
        if (hit != null && hit.getEntity() instanceof LivingEntity living)
        {
            return living;
        }
        return null;
    }

    private static LivingEntity resolveTarget(ServerPlayer caster, UUID targetUuid)
    {
        Entity entity = caster.serverLevel().getEntity(targetUuid);
        if (!(entity instanceof LivingEntity living))
        {
            return null;
        }
        if (!living.isAlive() || living.isRemoved())
        {
            return null;
        }
        double maxDistSq = TARGET_MAX_DISTANCE * TARGET_MAX_DISTANCE;
        if (caster.distanceToSqr(living) > maxDistSq)
        {
            return null;
        }
        return living;
    }

    private static void spawnJudgeBehindCaster(ServerPlayer caster, AccuseSession session)
    {
        removeJudge(caster, session);

        ServerLevel level = caster.serverLevel();
        JudgeEntity judge = ModEntities.JUDGE.get().create(level);
        if (judge == null)
        {
            return;
        }

        Vec3 spawnPos = judgePositionBehindCaster(caster);
        float yaw = caster.getYRot();
        judge.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, yaw, 0.0F);
        judge.prepareEntranceAnimation();
        if (!level.addFreshEntity(judge))
        {
            return;
        }

        session.judgeUuid = judge.getUUID();
    }

    private static void tickJudgeFollow(ServerPlayer caster, AccuseSession session)
    {
        if (session.judgeUuid == null || pendingJudgeDismissByCaster.containsKey(caster.getUUID()))
        {
            return;
        }
        Entity entity = caster.serverLevel().getEntity(session.judgeUuid);
        if (!(entity instanceof JudgeEntity judge) || !judge.isAlive())
        {
            session.judgeUuid = null;
            return;
        }

        Vec3 targetPos = judgePositionBehindCaster(caster);
        float yaw = caster.getYRot();
        if (judge.tickCount <= JudgeEntity.entranceAnimationDurationTicks())
        {
            judge.moveTo(judge.getX(), judge.getY(), judge.getZ(), yaw, 0.0F);
        }
        else
        {
            judge.moveTo(targetPos.x, targetPos.y, targetPos.z, yaw, 0.0F);
        }
    }

    private static void removeJudge(ServerPlayer caster, AccuseSession session)
    {
        if (session.judgeUuid == null)
        {
            return;
        }
        discardJudgeEntity(caster, session.judgeUuid);
        session.judgeUuid = null;
    }

    private static JudgeEntity resolveJudgeEntity(ServerPlayer caster, UUID judgeUuid)
    {
        Entity entity = caster.serverLevel().getEntity(judgeUuid);
        if (entity instanceof JudgeEntity judge && judge.isAlive())
        {
            return judge;
        }
        return null;
    }

    private static void discardJudgeEntity(ServerPlayer caster, UUID judgeUuid)
    {
        Entity entity = caster.serverLevel().getEntity(judgeUuid);
        if (entity != null)
        {
            entity.discard();
        }
    }

    private static Vec3 judgePositionBehindCaster(ServerPlayer caster)
    {
        float yawRad = (float) Math.toRadians(caster.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        return new Vec3(
                caster.getX() - forwardX * JUDGE_BEHIND_OFFSET,
                caster.getY(),
                caster.getZ() - forwardZ * JUDGE_BEHIND_OFFSET);
    }
}
