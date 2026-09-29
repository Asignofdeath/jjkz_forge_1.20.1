package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.effect.ConfiscationEffect;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.entity.JudgeEntity;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.JudgmentTrialTargetData;
import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga.MahoragaAdaptation;
import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 「审判」技能：Epic Fight 锁定目标 2 秒后发动裁决；施法者受击则打断。
 */
public final class JudgmentTrialSkillLogic
{
    /** 锁定蓄力时长：2 秒。 */
    public static final int LOCK_CHARGE_TICKS = 20 * 2;
    /** 「审判」/领域多人审理完成后施加的缓慢时长：15 秒。 */
    public static final int TRIAL_COMPLETE_SLOWNESS_TICKS = 20 * 15;
    /** 基础有罪判定概率（%）。 */
    public static final int BASE_GUILTY_CHANCE_PERCENT = 10;
    /** 有罪时额外获得【死刑】的概率（%）。 */
    public static final int DEATH_SENTENCE_CHANCE_PERCENT = 20;
    /** 有罪处罚标题：淡入 / 停留 / 淡出（tick）。 */
    private static final int VERDICT_TITLE_FADE_IN_TICKS = 10;
    private static final int VERDICT_TITLE_STAY_TICKS = 60;
    private static final int VERDICT_TITLE_FADE_OUT_TICKS = 20;
    /** 【死刑】/【附加了没收的死刑】大标题演出延迟（tick），3 秒 = 60 tick。 */
    public static final int DEATH_VERDICT_TITLE_DELAY_TICKS = 60;

    private record PendingVerdictTitle(JudgmentVerdictType verdictType, int remainingTicks, boolean grantExecutionersSword)
    {
    }

    private static final ConcurrentMap<UUID, PendingVerdictTitle> pendingVerdictTitleByRecipient =
            new ConcurrentHashMap<>();
    /** 目标每个 debuff 提升的【死刑】概率（%）。 */
    public static final double DEATH_SENTENCE_BONUS_PER_DEBUFF_PERCENT = 2.0D;
    /** 目标 debuff 对【死刑】概率的最大加成（%）。 */
    public static final double MAX_DEATH_SENTENCE_DEBUFF_BONUS_PERCENT = 12.0D;
    /** 触发【死刑】时，额外附加【没收】的基础概率（%）。 */
    public static final int DEATH_SENTENCE_WITH_CONFISCATION_CHANCE_PERCENT = 5;
    /** 审判者跟随施法者时长：出场动画长度。 */
    private static final int JUDGE_FOLLOW_TICKS = JudgeEntity.entranceAnimationDurationTicks();
    /** 审判者相对施法者身后的水平距离（格）。 */
    private static final double JUDGE_BEHIND_OFFSET = 2.0D;
    /** 锁定目标最大水平距离（格）。 */
    private static final double LOCK_TARGET_MAX_DISTANCE = 32.0D;
    /** 未锁定目标时，行动栏提醒间隔（2 秒）。 */
    private static final int NO_LOCK_REMINDER_INTERVAL_TICKS = 20 * 2;
    /** 未锁定目标时，审判 session 最长持续时间（tick），超时后关闭进度条并需重新按键。 */
    public static final int SESSION_AWAIT_LOCK_TICKS = 20 * 10;

    private static final ConcurrentMap<UUID, TrialSession> sessionByCaster = new ConcurrentHashMap<>();

    private static final class TrialSession
    {
        private boolean locking;
        private UUID reportedTargetUuid;
        private int lockChargeTicks;
        private UUID lockChargeTargetUuid;
        private UUID judgeUuid;
        private int judgeFollowTicksRemaining;
        private int noLockReminderCooldown;
        private int awaitLockTicksRemaining;
    }

    private JudgmentTrialSkillLogic()
    {
    }

    public static boolean isLocking(ServerPlayer player)
    {
        return sessionByCaster.containsKey(player.getUUID());
    }

    public static void startSession(ServerPlayer player)
    {
        TrialSession session = new TrialSession();
        session.awaitLockTicksRemaining = SESSION_AWAIT_LOCK_TICKS;
        sessionByCaster.put(player.getUUID(), session);
        ModNetwork.sendJudgmentTrialActiveSync(player, true);
        syncChargeHud(player, true, 0.0F);
        remindNoLockTarget(player);
        session.noLockReminderCooldown = NO_LOCK_REMINDER_INTERVAL_TICKS;
    }

    public static void updateLockOn(ServerPlayer caster, boolean locking, UUID targetUuid)
    {
        TrialSession session = sessionByCaster.get(caster.getUUID());
        if (session == null)
        {
            return;
        }

        session.locking = locking;
        session.reportedTargetUuid = locking ? targetUuid : null;
    }

    public static void tickMaintain(ServerPlayer caster)
    {
        TrialSession session = sessionByCaster.get(caster.getUUID());
        if (session == null)
        {
            return;
        }

        if (!caster.isAlive() || !EpicFightCompat.isBattleMode(caster) || !PlayerTechniqueData.isJudgment(caster))
        {
            cancel(caster, false);
            return;
        }

        if (session.judgeFollowTicksRemaining > 0)
        {
            tickJudgeFollow(caster, session);
            return;
        }

        if (!session.locking || session.reportedTargetUuid == null)
        {
            session.awaitLockTicksRemaining--;
            if (session.awaitLockTicksRemaining <= 0)
            {
                cancel(caster, false);
                return;
            }
            remindNoLockTargetIfDue(caster, session);
            resetLockCharge(caster, session);
            return;
        }

        LivingEntity target = resolveLockTarget(caster, session.reportedTargetUuid);
        if (target == null)
        {
            remindNoLockTargetIfDue(caster, session);
            resetLockCharge(caster, session);
            return;
        }

        if (session.lockChargeTargetUuid == null || !session.lockChargeTargetUuid.equals(target.getUUID()))
        {
            session.lockChargeTicks = 0;
            session.lockChargeTargetUuid = target.getUUID();
        }

        session.lockChargeTicks++;
        syncChargeHud(caster, true, (float) session.lockChargeTicks / (float) LOCK_CHARGE_TICKS);

        if (session.lockChargeTicks >= LOCK_CHARGE_TICKS)
        {
            executeTrial(caster, session, target);
        }
    }

    public static void cancelOnHurt(ServerPlayer caster)
    {
        TrialSession session = sessionByCaster.get(caster.getUUID());
        if (session == null || session.judgeFollowTicksRemaining > 0)
        {
            return;
        }
        cancel(caster, true);
    }

    public static void onLogout(ServerPlayer player)
    {
        cancel(player, false);
    }

    private static void executeTrial(ServerPlayer caster, TrialSession session, LivingEntity target)
    {
        if (!JudgmentTechniqueSkillSet.INSTANCE.finalizeTrialEconomy(caster))
        {
            caster.sendSystemMessage(Component.literal("咒力不足，审判未能完成。").withStyle(ChatFormatting.RED));
            cancel(caster, false);
            return;
        }

        spawnJudgeBehindCaster(caster, session);
        rollVerdict(caster, target);

        session.lockChargeTicks = 0;
        session.lockChargeTargetUuid = null;
        session.locking = false;
        session.reportedTargetUuid = null;
        syncChargeHud(caster, false, 0.0F);
        ModNetwork.sendJudgmentTrialActiveSync(caster, false);
    }

    public record DomainTrialVerdictResult(
            boolean guilty,
            JudgmentCrimeLibrary.CrimeEntry crime,
            JudgmentVerdictType verdictType)
    {
    }

    public static void applyTrialCompleteSlowness(LivingEntity target)
    {
        if (MahoragaAdaptation.tryEngage(target, JudgmentTechniqueSkillSet.SKILL_TRIAL))
        {
            return;
        }
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                TRIAL_COMPLETE_SLOWNESS_TICKS,
                0,
                false,
                false,
                true));
    }

    public static DomainTrialVerdictResult rollDomainTrialVerdict(ServerPlayer caster, LivingEntity target)
    {
        UUID targetUuid = target.getUUID();
        double guiltyBonus = JudgmentTrialTargetData.getGuiltyBonusPercent(caster, targetUuid);
        double guiltyChance = BASE_GUILTY_CHANCE_PERCENT + guiltyBonus;
        boolean guilty = SinfulOneEffect.isActive(target)
                || caster.getRandom().nextDouble() * 100.0D < guiltyChance;

        JudgmentTrialTargetData.incrementTrialCount(caster, targetUuid);

        if (!guilty)
        {
            return new DomainTrialVerdictResult(false, null, null);
        }

        JudgmentCrimeLibrary.CrimeEntry crime = JudgmentCrimeLibrary.pickForDefendant(target, caster.getRandom());
        JudgmentVerdictType verdictType = rollGuiltyVerdictType(caster, target, crime);
        return new DomainTrialVerdictResult(true, crime, verdictType);
    }

    public static void applyDomainTrialVerdictResult(
            ServerPlayer caster,
            LivingEntity target,
            DomainTrialVerdictResult result)
    {
        applyDomainTrialVerdictResult(caster, target, result, DeathSentenceEffect.ApplyMode.DOMAIN_MULTI);
    }

    public static void applyDomainTrialVerdictResult(
            ServerPlayer caster,
            LivingEntity target,
            DomainTrialVerdictResult result,
            DeathSentenceEffect.ApplyMode deathApplyMode)
    {
        if (!result.guilty() || result.crime() == null || result.verdictType() == null)
        {
            return;
        }

        SinfulOneEffect.apply(target, PlayerCrimeRecordData.Category.JUDGMENT_TECHNIQUE, result.crime().chinese());
        switch (result.verdictType())
        {
            case DEATH_SENTENCE -> DeathSentenceEffect.apply(target, caster, deathApplyMode);
            case DEATH_SENTENCE_WITH_CONFISCATION ->
            {
                DeathSentenceEffect.apply(target, caster, deathApplyMode);
                ConfiscationEffect.applyRandom(caster, target);
            }
            case CONFISCATION -> ConfiscationEffect.applyRandom(caster, target);
        }
    }

    private static void rollVerdict(ServerPlayer caster, LivingEntity target)
    {
        DomainTrialVerdictResult result = rollDomainTrialVerdict(caster, target);
        applyTrialCompleteSlowness(target);

        if (!result.guilty())
        {
            caster.displayClientMessage(Component.literal("裁定：无罪").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        JudgmentTrialTargetData.clearTrialCount(caster, target.getUUID());
        caster.displayClientMessage(Component.literal("裁定：有罪").withStyle(ChatFormatting.DARK_RED), true);
        caster.sendSystemMessage(result.crime().toChatMessage());
        applyDomainTrialVerdictResult(caster, target, result, DeathSentenceEffect.ApplyMode.SKILL);
        showVerdictTitle(caster, target, result.verdictType(), true);
    }

    public static void showVerdictTitle(ServerPlayer caster, LivingEntity target, JudgmentVerdictType verdictType)
    {
        showVerdictTitle(caster, target, verdictType, false);
    }

    public static void showVerdictTitle(
            ServerPlayer caster,
            LivingEntity target,
            JudgmentVerdictType verdictType,
            boolean grantExecutionersSwordOnCasterTitle)
    {
        if (isDeathVerdict(verdictType))
        {
            scheduleVerdictTitle(caster, verdictType, grantExecutionersSwordOnCasterTitle);
            if (target instanceof ServerPlayer targetPlayer && !targetPlayer.getUUID().equals(caster.getUUID()))
            {
                scheduleVerdictTitle(targetPlayer, verdictType, false);
            }
            return;
        }

        sendVerdictTitleToPlayer(caster, verdictType);
        if (target instanceof ServerPlayer targetPlayer && !targetPlayer.getUUID().equals(caster.getUUID()))
        {
            sendVerdictTitleToPlayer(targetPlayer, verdictType);
        }
    }

    public static void showVerdictTitleToDefendant(LivingEntity target, JudgmentVerdictType verdictType)
    {
        if (target instanceof ServerPlayer targetPlayer)
        {
            sendVerdictTitleToPlayer(targetPlayer, verdictType);
        }
    }

    public static void tickPendingVerdictTitles(ServerPlayer player)
    {
        PendingVerdictTitle pending = pendingVerdictTitleByRecipient.get(player.getUUID());
        if (pending == null)
        {
            return;
        }

        if (pending.remainingTicks() <= 1)
        {
            pendingVerdictTitleByRecipient.remove(player.getUUID());
            sendVerdictTitleToPlayer(player, pending.verdictType());
            if (pending.grantExecutionersSword())
            {
                JudgmentWeaponLogic.grantExecutionersSwordOnDeathSentence(player);
            }
            return;
        }

        pendingVerdictTitleByRecipient.put(
                player.getUUID(),
                new PendingVerdictTitle(
                        pending.verdictType(),
                        pending.remainingTicks() - 1,
                        pending.grantExecutionersSword()));
    }

    public static void clearPendingVerdictTitle(UUID recipientUuid)
    {
        pendingVerdictTitleByRecipient.remove(recipientUuid);
    }

    private static void scheduleVerdictTitle(
            ServerPlayer recipient,
            JudgmentVerdictType verdictType,
            boolean grantExecutionersSword)
    {
        pendingVerdictTitleByRecipient.put(
                recipient.getUUID(),
                new PendingVerdictTitle(verdictType, DEATH_VERDICT_TITLE_DELAY_TICKS, grantExecutionersSword));
    }

    private static boolean isDeathVerdict(JudgmentVerdictType verdictType)
    {
        return verdictType == JudgmentVerdictType.DEATH_SENTENCE
                || verdictType == JudgmentVerdictType.DEATH_SENTENCE_WITH_CONFISCATION;
    }

    private static void sendVerdictTitleToPlayer(ServerPlayer player, JudgmentVerdictType verdictType)
    {
        JudgmentSoundLogic.playBigSubtitleShow(player);
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(verdictType.mainTitle())));
        player.connection.send(new ClientboundSetSubtitleTextPacket(verdictType.subTitleComponent()));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                VERDICT_TITLE_FADE_IN_TICKS,
                VERDICT_TITLE_STAY_TICKS,
                VERDICT_TITLE_FADE_OUT_TICKS));
    }

    public static int countHarmfulDebuffs(LivingEntity target)
    {
        int count = 0;
        for (MobEffectInstance instance : target.getActiveEffects())
        {
            if (instance.getEffect().getCategory() == MobEffectCategory.HARMFUL)
            {
                count++;
            }
        }
        return count;
    }

    public static double computeDebuffDeathSentenceBonusPercent(LivingEntity target)
    {
        return Math.min(
                MAX_DEATH_SENTENCE_DEBUFF_BONUS_PERCENT,
                countHarmfulDebuffs(target) * DEATH_SENTENCE_BONUS_PER_DEBUFF_PERCENT);
    }

    /**
     * 计算【死刑】掷骰成功率（%）：基础 + debuff + 重罪加成，上限 100。
     */
    public static double computeDeathSentenceChancePercent(LivingEntity target, JudgmentCrimeLibrary.CrimeEntry crime)
    {
        return computeDeathSentenceChancePercent(target, JudgmentCrimeLibrary.getDeathSentenceBonusPercent(crime));
    }

    public static double computeDeathSentenceChancePercent(LivingEntity target, String accusationChinese)
    {
        return computeDeathSentenceChancePercent(
                target,
                JudgmentCrimeLibrary.getDeathSentenceBonusPercent(accusationChinese));
    }

    private static double computeDeathSentenceChancePercent(LivingEntity target, double crimeBonusPercent)
    {
        return Math.min(
                100.0D,
                DEATH_SENTENCE_CHANCE_PERCENT
                        + computeDebuffDeathSentenceBonusPercent(target)
                        + crimeBonusPercent);
    }

    /**
     * 在有罪前提下，根据概率掷出【死刑】或【没收】裁决类型。
     */
    public static JudgmentVerdictType rollGuiltyVerdictType(
            ServerPlayer caster,
            LivingEntity target,
            JudgmentCrimeLibrary.CrimeEntry crime)
    {
        return rollGuiltyVerdictType(caster, target, JudgmentCrimeLibrary.getDeathSentenceBonusPercent(crime));
    }

    public static JudgmentVerdictType rollGuiltyVerdictType(
            ServerPlayer caster,
            LivingEntity target,
            String accusationChinese)
    {
        return rollGuiltyVerdictType(
                caster,
                target,
                JudgmentCrimeLibrary.getDeathSentenceBonusPercent(accusationChinese));
    }

    /**
     * 掷骰的实际实现，也是<b>两个机制型技能唯一的门控点</b>（见
     * {@link JudgmentTechniqueSkillSet#SKILL_SENTENCING} / {@link JudgmentTechniqueSkillSet#SKILL_AGGRAVATION}）。
     * <p>
     * 「审判」「指控」「领域审理」三条路径都经由本方法掷骰，因此门控收在这里即三路全覆盖，
     * 不必在每个调用点各写一遍。
     * <ul>
     *   <li><b>未解锁「量刑」</b>：有罪一律判【没收】，不掷【死刑】——先让玩家把「有罪 = 没收」
     *       这一步立住，第一次见到【死刑】才是件有分量的事。</li>
     *   <li><b>未解锁「从重论处」</b>：【死刑】只吃 {@link #DEATH_SENTENCE_CHANCE_PERCENT} 基础概率，
     *       罪状库的重罪加成与目标身上每个 harmful debuff 的加成一律不计（含「附加了没收的死刑」
     *       那一档的 debuff 加成）。</li>
     * </ul>
     */
    private static JudgmentVerdictType rollGuiltyVerdictType(
            ServerPlayer caster,
            LivingEntity target,
            double crimeBonusPercent)
    {
        if (!isSentencingUnlocked(caster))
        {
            return JudgmentVerdictType.CONFISCATION;
        }

        boolean aggravating = isAggravationUnlocked(caster);
        double deathSentenceChance = aggravating
                ? computeDeathSentenceChancePercent(target, crimeBonusPercent)
                : DEATH_SENTENCE_CHANCE_PERCENT;
        if (caster.getRandom().nextDouble() * 100.0D >= deathSentenceChance)
        {
            return JudgmentVerdictType.CONFISCATION;
        }

        double deathWithConfiscationChance = DEATH_SENTENCE_WITH_CONFISCATION_CHANCE_PERCENT
                + (aggravating ? computeDebuffDeathSentenceBonusPercent(target) : 0.0D);
        return caster.getRandom().nextDouble() * 100.0D < deathWithConfiscationChance
                ? JudgmentVerdictType.DEATH_SENTENCE_WITH_CONFISCATION
                : JudgmentVerdictType.DEATH_SENTENCE;
    }

    /** 机制型技能「量刑」是否已解锁；未解锁时有罪一律只判【没收】。 */
    private static boolean isSentencingUnlocked(ServerPlayer caster)
    {
        return SkillUnlockLogic.isSkillUnlockedById(caster, PlayerTechniqueData.TECHNIQUE_JUDGMENT,
                JudgmentTechniqueSkillSet.SKILL_SENTENCING);
    }

    /** 机制型技能「从重论处」是否已解锁；未解锁时【死刑】只吃基础概率。 */
    private static boolean isAggravationUnlocked(ServerPlayer caster)
    {
        return SkillUnlockLogic.isSkillUnlockedById(caster, PlayerTechniqueData.TECHNIQUE_JUDGMENT,
                JudgmentTechniqueSkillSet.SKILL_AGGRAVATION);
    }

    /** 施法者当前「审判」技能随行的审判者；不存在或已失效时返回 null。 */
    public static JudgeEntity findJudgeForCaster(ServerPlayer caster)
    {
        TrialSession session = sessionByCaster.get(caster.getUUID());
        if (session == null || session.judgeUuid == null)
        {
            return null;
        }

        Entity entity = caster.serverLevel().getEntity(session.judgeUuid);
        if (entity instanceof JudgeEntity judge && judge.isAlive())
        {
            return judge;
        }

        session.judgeUuid = null;
        session.judgeFollowTicksRemaining = 0;
        return null;
    }

    private static void spawnJudgeBehindCaster(ServerPlayer caster, TrialSession session)
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
        session.judgeFollowTicksRemaining = JUDGE_FOLLOW_TICKS;
    }

    private static void tickJudgeFollow(ServerPlayer caster, TrialSession session)
    {
        ServerLevel level = caster.serverLevel();
        Entity entity = session.judgeUuid != null ? level.getEntity(session.judgeUuid) : null;
        if (!(entity instanceof JudgeEntity judge) || !judge.isAlive())
        {
            finishJudgeFollow(caster, session);
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

        session.judgeFollowTicksRemaining--;
        if (session.judgeFollowTicksRemaining <= 0)
        {
            judge.discard();
            finishJudgeFollow(caster, session);
        }
    }

    private static void finishJudgeFollow(ServerPlayer caster, TrialSession session)
    {
        session.judgeUuid = null;
        session.judgeFollowTicksRemaining = 0;
        sessionByCaster.remove(caster.getUUID());
    }

    private static void removeJudge(ServerPlayer caster, TrialSession session)
    {
        if (session.judgeUuid == null)
        {
            return;
        }
        Entity entity = caster.serverLevel().getEntity(session.judgeUuid);
        if (entity != null)
        {
            entity.discard();
        }
        session.judgeUuid = null;
        session.judgeFollowTicksRemaining = 0;
    }

    private static void cancel(ServerPlayer caster, boolean interrupted)
    {
        TrialSession session = sessionByCaster.remove(caster.getUUID());
        if (session == null)
        {
            return;
        }

        removeJudge(caster, session);
        ModNetwork.sendJudgmentTrialActiveSync(caster, false);
        syncChargeHud(caster, false, 0.0F);
        if (interrupted)
        {
            caster.displayClientMessage(Component.literal("审判被打断").withStyle(ChatFormatting.RED), true);
        }
    }

    private static void resetLockCharge(ServerPlayer caster, TrialSession session)
    {
        session.lockChargeTicks = 0;
        session.lockChargeTargetUuid = null;
        syncChargeHud(caster, true, 0.0F);
    }

    private static LivingEntity resolveLockTarget(ServerPlayer caster, UUID targetUuid)
    {
        Entity entity = caster.serverLevel().getEntity(targetUuid);
        if (!(entity instanceof LivingEntity living))
        {
            return null;
        }
        if (!living.isAlive() || living.isRemoved() || living.getUUID().equals(caster.getUUID()))
        {
            return null;
        }
        if (!living.isAttackable())
        {
            return null;
        }
        double maxDistSq = LOCK_TARGET_MAX_DISTANCE * LOCK_TARGET_MAX_DISTANCE;
        if (caster.distanceToSqr(living) > maxDistSq)
        {
            return null;
        }
        return living;
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

    private static void syncChargeHud(ServerPlayer caster, boolean visible, float progress)
    {
        ModNetwork.sendJudgmentTrialChargeSync(caster, visible, progress);
    }

    private static void remindNoLockTarget(ServerPlayer caster)
    {
        caster.displayClientMessage(Component.literal("请锁定审判目标").withStyle(ChatFormatting.YELLOW), true);
    }

    private static void remindNoLockTargetIfDue(ServerPlayer caster, TrialSession session)
    {
        if (session.noLockReminderCooldown > 0)
        {
            session.noLockReminderCooldown--;
            return;
        }
        remindNoLockTarget(caster);
        session.noLockReminderCooldown = NO_LOCK_REMINDER_INTERVAL_TICKS;
    }
}
