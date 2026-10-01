package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.entity.data.LimitlessBlueConfig;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessPurpleConfig;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRemoteMurasakiConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineFugaConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineHachiConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.technique.SkillDamageResolver;
import com.darkness.jujutsukaisenzone.technique.description.TechniqueSkillStatFormat;
import com.darkness.jujutsukaisenzone.technique.skill.GavelFormStats;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioAbsolutionLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioCollapseLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioIchimonjiLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioImpactLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioOvertimeLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioSpotLogic;
import com.darkness.jujutsukaisenzone.technique.skill.StarFlightSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkDetonation;
import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TenShadowsTechniqueSkillSet;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

/**
 * 技能升级前后「哪些数值变了」的展示目录。仅列出已接入等级成长的字段。
 */
public final class SkillUpgradeStatCatalog
{
    public record Line(Component label, String from, String to)
    {
    }

    private SkillUpgradeStatCatalog()
    {
    }

    public static List<Line> linesFor(String skillId, int oldLevel, int newLevel)
    {
        int fromLv = SkillLevelRules.clampLevel(oldLevel);
        int toLv = SkillLevelRules.clampLevel(newLevel);
        List<Line> lines = new ArrayList<>();
        if (skillId == null || fromLv == toLv)
        {
            return lines;
        }

        switch (skillId)
        {
            case LimitlessTechniqueSkillSet.SKILL_BLUE -> lines.add(ratioLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.damage",
                    LimitlessBlueConfig.DAMAGE, fromLv, toLv));
            case LimitlessTechniqueSkillSet.SKILL_PURPLE -> lines.add(ratioLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.tick_damage",
                    LimitlessPurpleConfig.ARROW_SPHERE_DAMAGE_PER_TICK, fromLv, toLv));
            case LimitlessTechniqueSkillSet.SKILL_REMOTE_PURPLE -> lines.add(ratioLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.tick_damage",
                    LimitlessRemoteMurasakiConfig.ARROW_SPHERE_DAMAGE_PER_TICK, fromLv, toLv));

            case ShrineTechniqueSkillSet.SKILL_KAI, ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ -> lines.add(ratioLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.damage",
                    ShrineKaiConfig.COLUMN_DAMAGE, fromLv, toLv));
            case ShrineTechniqueSkillSet.SKILL_HACHI -> lines.add(ratioLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.damage",
                    ShrineHachiConfig.DAMAGE, fromLv, toLv));
            case ShrineTechniqueSkillSet.SKILL_FUGA -> lines.add(ratioLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.tick_damage",
                    ShrineFugaConfig.BOOM_DAMAGE_PER_TICK, fromLv, toLv));

            case JudgmentTechniqueSkillSet.SKILL_GAVEL ->
            {
                lines.add(ratioLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.gavel_projectile",
                        GavelFormStats.STANDARD_PROJECTILE_DAMAGE, fromLv, toLv));
                lines.add(powerLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.gavel_explosion",
                        GavelFormStats.STANDARD_EXPLOSION_POWER, fromLv, toLv));
            }
            case JudgmentTechniqueSkillSet.SKILL_TRIAL ->
            {
                lines.add(flatLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.guilty_chance",
                        JudgmentTrialSkillLogic.BASE_GUILTY_CHANCE_PERCENT, 2, fromLv, toLv,
                        SkillUpgradeStatCatalog::percentText));
                lines.add(flatLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.death_chance",
                        JudgmentTrialSkillLogic.DEATH_SENTENCE_CHANCE_PERCENT, 2, fromLv, toLv,
                        SkillUpgradeStatCatalog::percentText));
                lines.add(flatLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.lock_range",
                        32, 2, fromLv, toLv, SkillUpgradeStatCatalog::blocksText));
            }
            case JudgmentTechniqueSkillSet.SKILL_ACCUSE ->
            {
                lines.add(flatLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.evidence_chance",
                        (int) JudgmentAccuseSkillLogic.EVIDENCE_CONFIRMED_CHANCE_PERCENT, 3, fromLv, toLv,
                        SkillUpgradeStatCatalog::percentText));
                lines.add(flatLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.target_range",
                        32, 2, fromLv, toLv, SkillUpgradeStatCatalog::blocksText));
            }
            case JudgmentTechniqueSkillSet.SKILL_VIEW_FILE -> lines.add(flatLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.target_range",
                    32, 2, fromLv, toLv, SkillUpgradeStatCatalog::blocksText));

            case LoveRendezvousTechniqueSkillSet.SKILL_STAR_MARK -> lines.add(ratioLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.detonation",
                    StarMarkDetonation.DETONATION_DAMAGE, fromLv, toLv));
            case LoveRendezvousTechniqueSkillSet.SKILL_STAR_FLIGHT ->
            {
                lines.add(ratioLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.ram_damage",
                        StarFlightSkillLogic.RAM_DAMAGE, fromLv, toLv));
                lines.add(ratioLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.path_damage",
                        StarFlightSkillLogic.PATH_DAMAGE, fromLv, toLv));
            }

            case RatioTechniqueSkillSet.SKILL_RATIO_MARK ->
            {
                lines.add(flatLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.spot_duration",
                        RatioSpotLogic.SPOT_DURATION_TICKS / 20, 1, fromLv, toLv,
                        SkillUpgradeStatCatalog::secondsText));
                lines.add(multLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.spot_crit",
                        RatioSpotLogic.FORCED_CRIT_MULT, 0.05F, fromLv, toLv));
            }
            case RatioTechniqueSkillSet.SKILL_ICHIMONJI -> lines.add(ratioLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.hit_damage",
                    RatioIchimonjiLogic.DAMAGE_PER_HIT, fromLv, toLv));
            case RatioTechniqueSkillSet.SKILL_IMPACT -> lines.add(ratioLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.damage",
                    RatioImpactLogic.DAMAGE, fromLv, toLv));
            case RatioTechniqueSkillSet.SKILL_COLLAPSE -> lines.add(ratioLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.debris_damage",
                    RatioCollapseLogic.HURT_DAMAGE, fromLv, toLv));
            case RatioTechniqueSkillSet.SKILL_OVERTIME -> lines.add(multLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.overtime_damage",
                    RatioOvertimeLogic.OVERTIME_DAMAGE_MULT, 0.02F, fromLv, toLv));
            case RatioTechniqueSkillSet.SKILL_ABSOLUTION ->
            {
                lines.add(ratioLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.damage",
                        RatioAbsolutionLogic.DAMAGE, fromLv, toLv));
                lines.add(ratioLine(
                        "gui.jujutsukaisenzone.skill_upgrade.stat.impact_damage",
                        RatioAbsolutionLogic.IMPACT_DAMAGE, fromLv, toLv));
            }

            case TenShadowsTechniqueSkillSet.SKILL_HAND_SHADOW -> lines.add(flatLine(
                    "gui.jujutsukaisenzone.skill_upgrade.stat.invis_duration",
                    TenShadowsTechniqueSkillSet.HAND_SHADOW_INVIS_TICKS / 20, 1, fromLv, toLv,
                    SkillUpgradeStatCatalog::secondsText));
            default ->
            {
            }
        }
        return lines;
    }

    private static Line ratioLine(String labelKey, float baseRatio, int fromLv, int toLv)
    {
        return new Line(
                Component.translatable(labelKey),
                SkillDamageResolver.ratioText(baseRatio * SkillLevelRules.powerMultiplier(fromLv)),
                SkillDamageResolver.ratioText(baseRatio * SkillLevelRules.powerMultiplier(toLv)));
    }

    private static Line powerLine(String labelKey, float base, int fromLv, int toLv)
    {
        return new Line(
                Component.translatable(labelKey),
                TechniqueSkillStatFormat.formatNumber(base * SkillLevelRules.powerMultiplier(fromLv)),
                TechniqueSkillStatFormat.formatNumber(base * SkillLevelRules.powerMultiplier(toLv)));
    }

    private static Line multLine(String labelKey, float base, float perStep, int fromLv, int toLv)
    {
        float from = base + perStep * (fromLv - 1);
        float to = base + perStep * (toLv - 1);
        return new Line(
                Component.translatable(labelKey),
                "×" + String.format(Locale.ROOT, "%.2f", from),
                "×" + String.format(Locale.ROOT, "%.2f", to));
    }

    private static Line flatLine(
            String labelKey,
            int base,
            int perStep,
            int fromLv,
            int toLv,
            IntFunction<String> format)
    {
        return new Line(
                Component.translatable(labelKey),
                format.apply(base + perStep * (fromLv - 1)),
                format.apply(base + perStep * (toLv - 1)));
    }

    private static String percentText(int value)
    {
        return value + "%";
    }

    private static String blocksText(int value)
    {
        return value + Component.translatable("gui.jujutsukaisenzone.skill_upgrade.unit.blocks").getString();
    }

    private static String secondsText(int value)
    {
        return value + Component.translatable("gui.jujutsukaisenzone.skill_upgrade.unit.seconds").getString();
    }
}
