package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.Nullable;

/**
 * 防御侧的换算：把目标身上的护甲 / 抗性，减去攻击者穿透后，换成一道减伤比例。
 * 与 {@link SkillDamageResolver} 对称，由 {@code TechniqueDamageZoneEvents} 的区 2 统一调用。
 * <p>
 * <b>三层防御（平行，抗性不是护甲子集）</b>
 * <ol>
 *   <li><b>护甲</b>——原版已对绝大多数通道算过；仅本模组术式通道因 {@code BYPASSES_ARMOR}
 *       在此补自研线性公式。可被<strong>穿甲</strong>削减。</li>
 *   <li><b>体术抗性</b>——只吃贴身格斗 / {@code MARTIAL}。可被<strong>穿甲</strong>削减。</li>
 *   <li><b>术式抗性</b>——只吃 {@code TECHNIQUE}。可被<strong>术穿</strong>削减。</li>
 * </ol>
 * 复合（{@code COMPOSITE}）两层各按 {@code combatDefense.compositeResistanceShare} 计入：
 * 体侧吃穿甲、术侧吃术穿。
 * <p>
 * 穿透公式（百分比点数同单位）：正抗性部分先减穿透；默认不 overpen（最多减到 0，
 * 不把正抗性打成易伤）。已有易伤（负抗性）原样保留。第一期不对「原版护甲层」回补穿甲，
 * 只穿自研护甲%与模组抗性。
 * <p>
 * ⚠️ 单位是<b>百分比点数</b>：属性值 {@code 25} = 25%。
 */
public final class SkillDefenseResolver
{
    private SkillDefenseResolver()
    {
    }

    public static boolean enabled()
    {
        return Config.combatDefenseEnabled;
    }

    /**
     * 护甲层减伤（0.0~1.0）。只应由区 2 对<b>本模组术式通道</b>调用——其它通道原版已经算过了，
     * 再补一道就是双计。{@code attacker} 为 null 时穿透按 0。
     */
    public static double armorReduction(LivingEntity target, @Nullable LivingEntity attacker)
    {
        if (!enabled())
        {
            return 0.0D;
        }
        double raw = target.getArmorValue() * Config.combatDefenseArmorFactorPerPoint / 100.0D;
        raw = clamp(raw, 0.0D, Config.combatDefenseArmorMaxReduction / 100.0D);
        return applyPenetration(raw, physicalPenetration(attacker));
    }

    /**
     * 抗性层减伤（0.0~1.0）。{@code type} 为 null 时返回 0。{@code attacker} 为 null 时穿透按 0。
     */
    public static double resistanceReduction(LivingEntity target, @Nullable SkillDamageType type,
                                            @Nullable LivingEntity attacker)
    {
        if (!enabled() || type == null)
        {
            return 0.0D;
        }
        return switch (type)
        {
            case MARTIAL -> physicalResistance(target, attacker);
            case TECHNIQUE -> techniqueResistance(target, attacker);
            case COMPOSITE ->
            {
                double share = Config.combatDefenseCompositeResistanceShare;
                double physical = physicalResistance(target, attacker) * share;
                double technique = techniqueResistance(target, attacker) * share;
                yield 1.0D - (1.0D - physical) * (1.0D - technique);
            }
        };
    }

    /**
     * 总减伤封顶换算成的「乘数下限」（cap = 85% ⇒ 0.15）。
     */
    public static double multiplierFloor()
    {
        return clamp(1.0D - Config.combatDefenseDefenseReductionCap / 100.0D, 0.0D, 1.0D);
    }

    private static double physicalResistance(LivingEntity target, @Nullable LivingEntity attacker)
    {
        double raw = rawResistance(target, ModAttributes.PHYSICAL_RESISTANCE.get(),
                Config.combatDefensePhysicalResistanceCap);
        return applyPenetration(raw, physicalPenetration(attacker));
    }

    private static double techniqueResistance(LivingEntity target, @Nullable LivingEntity attacker)
    {
        double raw = rawResistance(target, ModAttributes.TECHNIQUE_RESISTANCE.get(),
                Config.combatDefenseTechniqueResistanceCap);
        return applyPenetration(raw, techniquePenetration(attacker));
    }

    /**
     * 读抗性并夹到 [-100%, cap]。允许负值表示易伤。
     */
    private static double rawResistance(LivingEntity target, Attribute attribute, double capPercent)
    {
        var instance = target.getAttribute(attribute);
        if (instance == null)
        {
            return 0.0D;
        }
        return clamp(instance.getValue() / 100.0D, -1.0D, capPercent / 100.0D);
    }

    /**
     * 正抗性部分减去穿透；默认不 overpen（正部分最多到 0）。负抗性（易伤）原样相加保留。
     */
    private static double applyPenetration(double rawMitigation, double penetration)
    {
        if (penetration <= 0.0D)
        {
            return rawMitigation;
        }
        double vulnerability = Math.min(0.0D, rawMitigation);
        double positive = Math.max(0.0D, rawMitigation) - penetration;
        if (!Config.combatDefensePenetrationCanOverpen)
        {
            positive = Math.max(0.0D, positive);
        }
        return clamp(vulnerability + positive, -1.0D, 1.0D);
    }

    private static double physicalPenetration(@Nullable LivingEntity attacker)
    {
        return readPenetration(attacker, ModAttributes.PHYSICAL_PENETRATION.get());
    }

    private static double techniquePenetration(@Nullable LivingEntity attacker)
    {
        return readPenetration(attacker, ModAttributes.TECHNIQUE_PENETRATION.get());
    }

    private static double readPenetration(@Nullable LivingEntity attacker, Attribute attribute)
    {
        if (!Config.combatDefensePenetrationEnabled || attacker == null)
        {
            return 0.0D;
        }
        var instance = attacker.getAttribute(attribute);
        if (instance == null)
        {
            return 0.0D;
        }
        return clamp(instance.getValue() / 100.0D, 0.0D, 1.0D);
    }

    private static double clamp(double value, double min, double max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
