package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.entity.JudgeEntity;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.domain.JudgmentDomainExpansion;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 查找施法者当前随行的审判者实体并触发演出。
 */
public final class JudgmentJudgeEntityLogic
{
    /** 判处死刑后延迟播放审判者愤怒动画（tick），1 秒 = 20 tick。 */
    public static final int ANGRY_ANIMATION_DELAY_TICKS = 20;
    /** 愤怒演出总时长（tick），与客户端 CMDCam {@code cam start 2s} 一致。 */
    public static final int ANGRY_PERFORMANCE_TOTAL_TICKS = 20 * 2;

    private static final ConcurrentMap<UUID, Integer> pendingAngryAnimationDelayByCaster = new ConcurrentHashMap<>();

    private JudgmentJudgeEntityLogic()
    {
    }

    /** 愤怒演出期间审判者需保留的最短 tick（延迟 + CMDCam + 愤怒动画）。 */
    public static int angryPerformanceRetentionTicks()
    {
        return ANGRY_ANIMATION_DELAY_TICKS
                + ANGRY_PERFORMANCE_TOTAL_TICKS
                + JudgeEntity.angryAnimationDurationTicks();
    }

    public static boolean hasPendingAngryPerformance(UUID casterUuid)
    {
        return pendingAngryAnimationDelayByCaster.containsKey(casterUuid);
    }

    /**
     * 判处死刑：即刻 CMDCam 正脸运镜，{@link #ANGRY_ANIMATION_DELAY_TICKS} 后再播放愤怒动画。
     */
    public static void scheduleAngryAnimationForCaster(ServerPlayer caster)
    {
        playAngryCameraForCaster(caster);
        pendingAngryAnimationDelayByCaster.put(caster.getUUID(), ANGRY_ANIMATION_DELAY_TICKS);
    }

    public static void tickPendingAngryAnimation(ServerPlayer caster)
    {
        UUID uuid = caster.getUUID();
        Integer remaining = pendingAngryAnimationDelayByCaster.get(uuid);
        if (remaining == null)
        {
            return;
        }

        if (remaining <= 1)
        {
            pendingAngryAnimationDelayByCaster.remove(uuid);
            playAngryAnimationOnlyForCaster(caster);
            return;
        }

        pendingAngryAnimationDelayByCaster.put(uuid, remaining - 1);
    }

    public static void clearPendingAngryAnimation(UUID casterUuid)
    {
        pendingAngryAnimationDelayByCaster.remove(casterUuid);
    }

    private static void playAngryCameraForCaster(ServerPlayer caster)
    {
        JudgeEntity judge = resolveJudgeForAngryPerformance(caster);
        if (judge == null)
        {
            return;
        }
        ModNetwork.sendJudgmentJudgeAngryPerformance(
                caster,
                judge.getX(),
                judge.getY(),
                judge.getZ(),
                judge.getYRot());
    }

    private static void playAngryAnimationOnlyForCaster(ServerPlayer caster)
    {
        JudgeEntity judge = resolveJudgeForAngryPerformance(caster);
        if (judge != null)
        {
            judge.prepareAngryAnimation();
        }
    }

    private static JudgeEntity resolveJudgeForAngryPerformance(ServerPlayer caster)
    {
        JudgmentDomainExpansion domain = JudgmentDomainExpansion.getInstance();
        if (domain.hasActiveCompletedDomain(caster))
        {
            domain.ensureJudgeForAngryPerformance(caster);
        }
        return findFollowingJudge(caster);
    }

    private static JudgeEntity findFollowingJudge(ServerPlayer caster)
    {
        JudgeEntity domainJudge = JudgmentDomainExpansion.getInstance().findJudgeForCaster(caster);
        if (domainJudge != null)
        {
            return domainJudge;
        }

        JudgeEntity trialJudge = JudgmentTrialSkillLogic.findJudgeForCaster(caster);
        if (trialJudge != null)
        {
            return trialJudge;
        }

        return JudgmentAccuseSkillLogic.findJudgeForCaster(caster);
    }
}
