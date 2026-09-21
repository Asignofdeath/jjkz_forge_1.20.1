package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.effect.ConfiscationEffect;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.player.JudgmentTrialTargetData;
import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 「指控」裁决：辩护选项 × 证据结论。
 */
public final class JudgmentAccuseVerdictLogic
{
    private static final int TIMEOUT_IMMOBILIZE_TICKS = 20 * 3;

    private JudgmentAccuseVerdictLogic()
    {
    }

    public static Component buildVerdictMessage(JudgmentAccuseResponse response, JudgmentAccuseEvidence evidence)
    {
        return buildVerdictMessage(response, evidence, "指控");
    }

    public static Component buildVerdictMessage(
            JudgmentAccuseResponse response,
            JudgmentAccuseEvidence evidence,
            String tag)
    {
        String chinese;
        String english;
        switch (response)
        {
            case NONE -> {
                chinese = "裁决：未作回应";
                english = "Verdict: No response";
            }
            case DENY -> {
                if (evidence == JudgmentAccuseEvidence.CONFIRMED)
                {
                    chinese = "裁决：否认失败，罪行确认";
                    english = "Verdict: Denial failed, crime confirmed";
                }
                else
                {
                    chinese = "裁决：无罪释放";
                    english = "Verdict: Acquitted";
                }
            }
            case LIE -> {
                if (evidence == JudgmentAccuseEvidence.CONFIRMED)
                {
                    chinese = "裁决：撒谎败露";
                    english = "Verdict: Lie exposed";
                }
                else
                {
                    chinese = "裁决：没收";
                    english = "Verdict: Confiscation";
                }
            }
            case CONFESS -> {
                if (evidence == JudgmentAccuseEvidence.CONFIRMED)
                {
                    chinese = "裁决：坦白从宽，没收";
                    english = "Verdict: Confession, confiscation";
                }
                else
                {
                    chinese = "裁决：坦白，证据不足";
                    english = "Verdict: Confession, insufficient evidence";
                }
            }
            default -> {
                chinese = "裁决";
                english = "Verdict";
            }
        }
        return Component.literal("【" + tag + "】" + chinese)
                .withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(" / " + english).withStyle(ChatFormatting.GRAY));
    }

    public static void sendVerdictMessage(ServerPlayer caster, LivingEntity target, Component message)
    {
        caster.sendSystemMessage(message);
        if (target instanceof ServerPlayer targetPlayer && !targetPlayer.getUUID().equals(caster.getUUID()))
        {
            targetPlayer.sendSystemMessage(message);
        }
    }

    public static void applyVerdictEffects(
            ServerPlayer caster,
            LivingEntity target,
            JudgmentAccuseResponse response,
            JudgmentAccuseEvidence evidence)
    {
        applyVerdictEffects(caster, target, response, evidence, null);
    }

    public static void applyVerdictEffects(
            ServerPlayer caster,
            LivingEntity target,
            JudgmentAccuseResponse response,
            JudgmentAccuseEvidence evidence,
            String judgmentCrimeDescription)
    {
        applyVerdictEffects(caster, target, response, evidence, judgmentCrimeDescription, false);
    }

    public static void applyVerdictEffects(
            ServerPlayer caster,
            LivingEntity target,
            JudgmentAccuseResponse response,
            JudgmentAccuseEvidence evidence,
            String judgmentCrimeDescription,
            boolean domainSingleTrial)
    {
        if (isGuiltyVerdict(response, evidence))
        {
            JudgmentTrialTargetData.clearTrialCount(caster, target.getUUID());
        }

        switch (response)
        {
            case NONE -> applyTimeoutEffects(caster, target, judgmentCrimeDescription, domainSingleTrial);
            case DENY -> applyDenyEffects(caster, target, evidence, judgmentCrimeDescription, domainSingleTrial);
            case LIE -> applyLieEffects(caster, target, evidence, judgmentCrimeDescription, domainSingleTrial);
            case CONFESS -> applyConfessEffects(caster, target, evidence, judgmentCrimeDescription, domainSingleTrial);
        }
    }

    /** 「指控」/领域单人审理：除「否认 + 无法确认（无罪释放）」外均视为【有罪】。 */
    public static boolean isGuiltyVerdict(JudgmentAccuseResponse response, JudgmentAccuseEvidence evidence)
    {
        return !(response == JudgmentAccuseResponse.DENY && evidence == JudgmentAccuseEvidence.CANNOT_CONFIRM);
    }

    private static void applyDenyEffects(
            ServerPlayer caster,
            LivingEntity target,
            JudgmentAccuseEvidence evidence,
            String judgmentCrimeDescription,
            boolean domainSingleTrial)
    {
        if (evidence == JudgmentAccuseEvidence.CONFIRMED)
        {
            applyDeathSentenceVerdict(caster, target, domainSingleTrial, judgmentCrimeDescription);
        }
    }

    private static void applyLieEffects(
            ServerPlayer caster,
            LivingEntity target,
            JudgmentAccuseEvidence evidence,
            String judgmentCrimeDescription,
            boolean domainSingleTrial)
    {
        if (evidence == JudgmentAccuseEvidence.CONFIRMED)
        {
            applyConfiscationVerdict(caster, target, domainSingleTrial);
            applyJudgmentSinfulOne(target, judgmentCrimeDescription);
            return;
        }
        applyConfiscationVerdict(caster, target, domainSingleTrial);
    }

    private static void applyConfessEffects(
            ServerPlayer caster,
            LivingEntity target,
            JudgmentAccuseEvidence evidence,
            String judgmentCrimeDescription,
            boolean domainSingleTrial)
    {
        if (evidence == JudgmentAccuseEvidence.CONFIRMED)
        {
            applyConfiscationVerdict(caster, target, domainSingleTrial);
            return;
        }
        applyJudgmentSinfulOne(target, judgmentCrimeDescription);
    }

    private static void applyTimeoutEffects(
            ServerPlayer caster,
            LivingEntity target,
            String judgmentCrimeDescription,
            boolean domainSingleTrial)
    {
        applyConfiscationVerdict(caster, target, domainSingleTrial);
        applyJudgmentSinfulOne(target, judgmentCrimeDescription);
        applyImmobilize(target);
    }

    private static void applyJudgmentSinfulOne(LivingEntity target, String judgmentCrimeDescription)
    {
        String description = judgmentCrimeDescription != null && !judgmentCrimeDescription.isBlank()
                ? judgmentCrimeDescription
                : "被审判术式判定有罪";
        SinfulOneEffect.apply(target, PlayerCrimeRecordData.Category.JUDGMENT_TECHNIQUE, description);
    }

    private static void applyConfiscationVerdict(ServerPlayer caster, LivingEntity target, boolean domainSingleTrial)
    {
        ConfiscationEffect.applyRandom(caster, target);
        JudgmentTrialSkillLogic.showVerdictTitle(caster, target, JudgmentVerdictType.CONFISCATION);
    }

    private static void applyDeathSentenceVerdict(
            ServerPlayer caster,
            LivingEntity target,
            boolean domainSingleTrial,
            String judgmentCrimeDescription)
    {
        JudgmentVerdictType verdictType = JudgmentTrialSkillLogic.rollGuiltyVerdictType(
                caster,
                target,
                judgmentCrimeDescription);
        DeathSentenceEffect.ApplyMode applyMode = domainSingleTrial
                ? DeathSentenceEffect.ApplyMode.DOMAIN_SINGLE
                : DeathSentenceEffect.ApplyMode.SKILL;
        switch (verdictType)
        {
            case DEATH_SENTENCE -> DeathSentenceEffect.apply(target, caster, applyMode);
            case DEATH_SENTENCE_WITH_CONFISCATION ->
            {
                DeathSentenceEffect.apply(target, caster, applyMode);
                ConfiscationEffect.applyRandom(caster, target);
            }
            case CONFISCATION -> ConfiscationEffect.applyRandom(caster, target);
        }
        JudgmentTrialSkillLogic.showVerdictTitle(caster, target, verdictType, !domainSingleTrial);
    }

    private static void applyImmobilize(LivingEntity target)
    {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TIMEOUT_IMMOBILIZE_TICKS, 255, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, TIMEOUT_IMMOBILIZE_TICKS, 128, false, false, true));
    }
}
