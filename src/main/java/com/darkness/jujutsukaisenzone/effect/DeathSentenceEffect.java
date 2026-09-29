package com.darkness.jujutsukaisenzone.effect;

import com.darkness.jujutsukaisenzone.photon.PhotonFxCommands;
import com.darkness.jujutsukaisenzone.registry.ModEffects;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga.MahoragaAdaptation;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentJudgeEntityLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentSoundLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 死刑：挂载 spotlight Photon 特效；处刑人之剑可处决该状态下的生物。
 */
public final class DeathSentenceEffect extends MobEffect
{
    /** 持续时间：5 分钟。 */
    public static final int DURATION_TICKS = 20 * 60 * 5;

    /**
     * 判处死刑时的附带演出与奖励时机。
     * <ul>
     *   <li>{@link #SKILL} — 「审判」「指控」等技能</li>
     *   <li>{@link #DOMAIN_SINGLE} — 诛伏赐死领域单人审理（指控式）</li>
     *   <li>{@link #DOMAIN_MULTI} — 诛伏赐死领域多人审理</li>
     * </ul>
     */
    public enum ApplyMode
    {
        /** 即刻运镜、延迟愤怒动画；屏幕大字与同期音效/处刑人之剑（领域外）延迟 3 秒。 */
        SKILL,
        /** 与 {@link #SKILL} 演出相同；处刑人之剑由领域关闭时统一授予。 */
        DOMAIN_SINGLE,
        /** 不播放运镜与愤怒动画；屏幕大字已在审理过程中播报；处刑人之剑由领域关闭时统一授予。 */
        DOMAIN_MULTI
    }

    public DeathSentenceEffect()
    {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    public static boolean isActive(LivingEntity entity)
    {
        return entity.hasEffect(ModEffects.DEATH_SENTENCE.get());
    }

    public static void apply(LivingEntity target)
    {
        apply(target, null);
    }

    public static void apply(LivingEntity target, ServerPlayer caster)
    {
        apply(target, caster, ApplyMode.SKILL);
    }

    public static void apply(LivingEntity target, ServerPlayer caster, ApplyMode mode)
    {
        if (MahoragaAdaptation.tryEngage(target, "judgment_death_sentence"))
        {
            return;
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.DEATH_SENTENCE.get(),
                DURATION_TICKS,
                0,
                false,
                false,
                true));
        if (caster == null)
        {
            return;
        }

        switch (mode)
        {
            case SKILL -> {
                JudgmentSoundLogic.scheduleDeathPenalty(caster, target);
                JudgmentJudgeEntityLogic.scheduleAngryAnimationForCaster(caster);
            }
            case DOMAIN_SINGLE -> {
                JudgmentSoundLogic.scheduleDeathPenalty(caster, target);
                JudgmentJudgeEntityLogic.scheduleAngryAnimationForCaster(caster);
            }
            case DOMAIN_MULTI -> JudgmentSoundLogic.playDeathPenalty(caster, target);
        }
    }

    public static void remove(LivingEntity target)
    {
        target.removeEffect(ModEffects.DEATH_SENTENCE.get());
    }

    public static void mountSpotlight(LivingEntity entity)
    {
        PhotonFxCommands.perform(entity, PhotonFxCommands.SPOTLIGHT_MOUNT);
    }

    public static void removeSpotlight(LivingEntity entity)
    {
        PhotonFxCommands.perform(entity, PhotonFxCommands.SPOTLIGHT_REMOVE);
    }
}
