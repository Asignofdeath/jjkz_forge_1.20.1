package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.entity.combat.LivingCombatStats;
import com.darkness.jujutsukaisenzone.player.PlayerCombatPowerData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 强度面板 → 实际伤害的<b>唯一换算点</b>。所有术式 / 体术出伤都必须经过这里，
 * 否则强度面板就是个空数字。
 * <ul>
 *   <li>术式：实际伤害 = 四级基准点数 × 术式强度 / 术式基准强度（8 点 ⇒ 40% × 强度）</li>
 *   <li>体术：实际伤害 = 原始伤害 × 体术强度 / 体术基准强度</li>
 * </ul>
 * 这样现有 {@code *Config} 里的常量（8.0 / 10.0 / 5.0 / 3.0 / 400.0 …）语义保持不变，
 * 只是从「四级时的固定伤害」变成「四级基准点数」，之后随强度线性放大。
 * <p>
 * 攻击者优先级：玩家 {@link PlayerCombatPowerData} → 实体 {@link LivingCombatStats}
 * （十影式神等）→ 无面板则原样返回。
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

    /** 术式：把「四级基准点数」换算成施术者当前强度下的实际伤害。 */
    public static float technique(@Nullable LivingEntity owner, float baselinePoints)
    {
        if (!enabled())
        {
            return baselinePoints;
        }
        int power = resolveTechniquePower(owner);
        if (power < 0)
        {
            return baselinePoints;
        }
        return baselinePoints * power / (float) Config.combatPowerTechniqueBaseline;
    }

    /** 体术：对 EpicFight / 原版近战的原始伤害施加体术强度倍率。 */
    public static float physical(@Nullable LivingEntity attacker, float rawAmount)
    {
        if (!enabled())
        {
            return rawAmount;
        }
        int power = resolvePhysicalPower(attacker);
        if (power < 0)
        {
            return rawAmount;
        }
        return rawAmount * power / (float) Config.combatPowerPhysicalBaseline;
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

    /** 技能描述用的百分比：四级基准点数 → 倍率百分数（8 点 = 40%）。 */
    public static int ratioPercent(float baselinePoints)
    {
        return Math.round(baselinePoints * 100.0F / (float) Config.combatPowerTechniqueBaseline);
    }

    /** 技能描述直接用的文本（如 {@code "40%"}）；把 % 放在参数里，lang 就不必写 {@code %%}。 */
    public static String ratioText(float baselinePoints)
    {
        return ratioPercent(baselinePoints) + "%";
    }

    /** 同 {@link #ratioText}，用于体术通道（除以体术基准）。 */
    public static String martialRatioText(float baselinePoints)
    {
        return Math.round(baselinePoints * 100.0F / (float) Config.combatPowerPhysicalBaseline) + "%";
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
