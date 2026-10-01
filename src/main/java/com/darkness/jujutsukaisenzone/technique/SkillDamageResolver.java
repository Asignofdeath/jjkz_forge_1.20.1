package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.entity.combat.LivingCombatStats;
import com.darkness.jujutsukaisenzone.player.PlayerCombatPowerData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 强度面板 → 实际伤害的唯一换算点。所有术式 / 体术<b>技能</b>出伤都必须经过这里。
 * <p>
 * <b>技能出伤公式</b>：{@code 伤害 = 术/体强度 × 技能倍率}（再经 {@code TechniqueDamageZoneEvents} 等乘区）。
 * 技能常量应为显式倍率（如 {@code 0.40F} = 40%），不要再写旧的「绝对伤害点数」。
 * <p>
 * <b>近战绝对伤</b>（EpicFight / 原版挥击）走 {@link #scaleMeleeAbsolute}：已是 HP 量，再按
 * {@code × 体术强度 / physicalBaseline} 缩放；与技能倍率不是同一条 API。
 * <p>
 * 攻击者优先级：玩家 {@link PlayerCombatPowerData} → 实体 {@link LivingCombatStats}
 * （十影式神等）→ 无面板则按「参考强度」把倍率换成绝对伤（见 {@link Config#combatPowerTechniqueBaseline}）。
 */
public final class SkillDamageResolver
{
    /** 体术强度放大前的原始伤害，供黑闪乘幂读取（避免 pow 把强度倍率一起放大）。 */
    private static final ThreadLocal<Float> RAW_BEFORE_POWER = new ThreadLocal<>();

    private SkillDamageResolver()
    {
    }

    public static boolean enabled()
    {
        return Config.combatPowerScalingEnabled;
    }

    /**
     * 术式技能：{@code 术式强度 × skillRatio}。
     *
     * @param skillRatio 技能倍率（1.0 = 100% 术式强度）；不是旧「点数」
     */
    public static float technique(@Nullable LivingEntity owner, float skillRatio)
    {
        int power = resolveTechniquePower(owner);
        if (!enabled() || power < 0)
        {
            return skillRatio * (float) Config.combatPowerTechniqueBaseline;
        }
        return skillRatio * power;
    }

    /**
     * 体术技能（经 {@code deal(..., MARTIAL)}）：{@code 体术强度 × skillRatio}。
     *
     * @param skillRatio 技能倍率（1.0 = 100% 体术强度）
     */
    public static float physical(@Nullable LivingEntity attacker, float skillRatio)
    {
        int power = resolvePhysicalPower(attacker);
        if (!enabled() || power < 0)
        {
            return skillRatio * (float) Config.combatPowerPhysicalBaseline;
        }
        return skillRatio * power;
    }

    /**
     * EpicFight / 原版近战：对已是绝对伤害的挥击量施加体术强度缩放。
     * {@code 绝对伤 × 体术强度 / physicalBaseline}；勿用于技能倍率路径。
     */
    public static float scaleMeleeAbsolute(@Nullable LivingEntity attacker, float rawAbsoluteDamage)
    {
        if (!enabled())
        {
            return rawAbsoluteDamage;
        }
        int power = resolvePhysicalPower(attacker);
        if (power < 0)
        {
            return rawAbsoluteDamage;
        }
        return rawAbsoluteDamage * power / (float) Config.combatPowerPhysicalBaseline;
    }

    private static int resolvePhysicalPower(@Nullable LivingEntity attacker)
    {
        if (attacker instanceof Player player)
        {
            return PlayerCombatPowerData.physicalPower(player);
        }
        return LivingCombatStats.physicalPower(attacker);
    }

    private static int resolveTechniquePower(@Nullable LivingEntity owner)
    {
        if (owner instanceof Player player)
        {
            return PlayerCombatPowerData.techniquePower(player);
        }
        return LivingCombatStats.techniquePower(owner);
    }

    /** 技能倍率 → 描述用百分数（0.40 → 40）。 */
    public static int ratioPercent(float skillRatio)
    {
        return Math.round(skillRatio * 100.0F);
    }

    /** 技能描述文本（如 {@code "40%"}）。 */
    public static String ratioText(float skillRatio)
    {
        return ratioPercent(skillRatio) + "%";
    }

    /** 同 {@link #ratioText}（体术技能倍率）。 */
    public static String martialRatioText(float skillRatio)
    {
        return ratioPercent(skillRatio) + "%";
    }

    /** 体术伤害被强度放大前，记下原始值。 */
    public static void markRawBeforePower(float rawAmount)
    {
        RAW_BEFORE_POWER.set(rawAmount);
    }

    /** 无条件清掉标记（出伤结束后调用，避免残留值污染下一次结算）。 */
    public static void clearRawBeforePower()
    {
        RAW_BEFORE_POWER.remove();
    }

    /** 取回原始值（取后即清）；无记录时返回传入值。 */
    public static float consumeRawBeforePower(float fallback)
    {
        Float raw = RAW_BEFORE_POWER.get();
        RAW_BEFORE_POWER.remove();
        return raw == null ? fallback : raw;
    }
}
