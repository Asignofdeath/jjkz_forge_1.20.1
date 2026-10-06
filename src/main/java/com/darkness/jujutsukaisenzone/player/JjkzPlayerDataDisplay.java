package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import com.darkness.jujutsukaisenzone.technique.DomainExpansionServerLogic;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 在聊天栏展示 JJKZ 玩家核心数据（与 {@code /jjkz data} 一致）。
 */
public final class JjkzPlayerDataDisplay
{
    private JjkzPlayerDataDisplay()
    {
    }

    public static void sendToViewer(ServerPlayer viewer, ServerPlayer subject)
    {
        int current = PlayerCursedEnergyData.getCurrent(subject);
        int max = PlayerCursedEnergyData.getMax(subject);
        int experience = PlayerCursedGradeData.getExperience(subject);
        int rank = PlayerCursedGradeData.getRank(subject);
        String factionId = PlayerFactionData.getFactionId(subject);
        String factionDisplay = factionId.isEmpty()
                ? "未选择"
                : PlayerFactionData.getDisplayName(factionId) + " (" + factionId + ")";
        String innateTechnique = PlayerTechniqueData.getInnateTechnique(subject);
        boolean reverseTechniqueLearned = PlayerReverseTechniqueData.hasReverseTechnique(subject);
        BarrierTechniqueLevel barrierLevel = PlayerBarrierTechniqueData.getLevel(subject);
        SeanceTechniqueLevel seanceLevel = PlayerSeanceTechniqueData.getLevel(subject);
        StrengthenTechniqueLevel strengthenLevel = PlayerStrengthenTechniqueData.getLevel(subject);

        viewer.sendSystemMessage(Component.literal(
                "========== JJKZ 玩家数据（" + subject.getGameProfile().getName() + "）=========="));
        viewer.sendSystemMessage(Component.literal("阵营: " + factionDisplay));
        viewer.sendSystemMessage(Component.literal("生得术式: " + innateTechnique));
        viewer.sendSystemMessage(Component.literal("反转术式: " + (reverseTechniqueLearned ? "已习得" : "未习得")
                + String.format("（效率 %.0f%%）",
                subject.getAttribute(ModAttributes.REVERSE_TECHNIQUE_EFFICIENCY.get()) == null
                        ? 100.0D
                        : subject.getAttribute(ModAttributes.REVERSE_TECHNIQUE_EFFICIENCY.get()).getValue())));
        viewer.sendSystemMessage(Component.literal("结界术等级: " + getBarrierLevelDisplayName(barrierLevel)
                + "（维持减免：" + (int) (barrierLevel.getMaintainCostReduction() * 100) + "%）"));
        viewer.sendSystemMessage(Component.literal("降灵术等级: " + getSeanceLevelDisplayName(seanceLevel)
                + "（维持减免：" + (int) (seanceLevel.getMaintainCostReduction() * 100) + "%）"));
        viewer.sendSystemMessage(Component.literal("强化术等级: " + getStrengthenLevelDisplayName(strengthenLevel)
                + "（维持减免：" + (int) (strengthenLevel.getMaintainCostReduction() * 100) + "%"
                + (PlayerStrengthenTechniqueData.isActive(subject) ? "，已开启" : "，未开启") + "）"));
        viewer.sendSystemMessage(Component.literal("咒术经验: " + experience
                + "（等级: " + PlayerCursedGradeData.getRankDisplayName(rank) + "）"));
        viewer.sendSystemMessage(Component.literal("咒力: " + current + "/" + max));
        float health = subject.getHealth();
        float healthMax = PlayerHealthData.getMax(subject);
        double healthBonus = PlayerHealthData.getBonus(subject);
        viewer.sendSystemMessage(Component.literal(String.format(
                "生命值: %.1f/%.1f（基础%.0f + 加成%.0f）",
                health,
                healthMax,
                CursedGradeBonuses.BASE_MAX_HEALTH,
                healthBonus)));
        var talent = com.darkness.jujutsukaisenzone.talent.InnateTalentData.get(subject);
        if (talent != null)
        {
            var source = com.darkness.jujutsukaisenzone.talent.InnateTalentData.getSource(subject);
            String prefix = source == com.darkness.jujutsukaisenzone.talent.InnateTalentSource.ACQUIRED
                    ? "后天天赋"
                    : "生得天赋";
            viewer.sendSystemMessage(Component.literal(prefix + ": " + talent.displayName()));
        }
        CombatPowerStats power = PlayerCombatPowerData.resolve(subject);
        viewer.sendSystemMessage(Component.literal("体术强度: " + power.physicalPower()
                + "　术式强度: " + power.techniquePower()));
        for (CombatPowerStats.Contribution contribution : power.physicalBreakdown())
        {
            viewer.sendSystemMessage(Component.literal("  · 体术 " + contribution.label() + ": " + contribution.amount()));
        }
        for (CombatPowerStats.Contribution contribution : power.techniqueBreakdown())
        {
            viewer.sendSystemMessage(Component.literal("  · 术式 " + contribution.label() + ": " + contribution.amount()));
        }
        // 防御侧平行三层 + 攻击侧穿透（穿甲穿护甲%与体抗，术穿只穿术抗）。
        var physicalResistance = subject.getAttribute(ModAttributes.PHYSICAL_RESISTANCE.get());
        var techniqueResistance = subject.getAttribute(ModAttributes.TECHNIQUE_RESISTANCE.get());
        var physicalPenetration = subject.getAttribute(ModAttributes.PHYSICAL_PENETRATION.get());
        var techniquePenetration = subject.getAttribute(ModAttributes.TECHNIQUE_PENETRATION.get());
        viewer.sendSystemMessage(Component.literal(String.format(
                "护甲（防御力）: %s　体术抗性: %.1f%%　术式抗性: %.1f%%",
                subject.getArmorValue(),
                physicalResistance == null ? 0.0D : physicalResistance.getValue(),
                techniqueResistance == null ? 0.0D : techniqueResistance.getValue())));
        viewer.sendSystemMessage(Component.literal(String.format(
                "穿甲: %.1f%%　术式穿透: %.1f%%",
                physicalPenetration == null ? 0.0D : physicalPenetration.getValue(),
                techniquePenetration == null ? 0.0D : techniquePenetration.getValue())));
        // 破韧效力 + 咒力特质：两者共同决定怪物韧性条的破韧速度（特质对付特攻怪）。
        var breakPotency = subject.getAttribute(ModAttributes.BREAK_POTENCY.get());
        viewer.sendSystemMessage(Component.literal(String.format(
                "破韧效力: %.1f%%　咒力特质: %s",
                breakPotency == null ? 0.0D : breakPotency.getValue(),
                PlayerCursedEnergyTraitData.getTrait(subject).id())));
        DomainExpansionServerLogic domainLogic = DomainExpansionServerLogic.getInstance();
        boolean hasDomain = domainLogic.hasLearnedDomainExpansion(subject);
        String domainName = domainLogic.getDomainName();
        viewer.sendSystemMessage(Component.literal("领域展开: " + (hasDomain ? "已习得" : "未习得")
                + (domainName.isEmpty() ? "" : "（" + domainName + "）")));
        viewer.sendSystemMessage(Component.literal("修复术式熔断: "
                + (PlayerMeltdownRepairData.hasLearned(subject) ? "已习得" : "未习得")));
        int brain = PlayerBrainDamageData.getBrainDamage(subject);
        int expands = PlayerBrainDamageData.getExpandsToday(subject);
        int free = PlayerBrainDamageData.getFreeDailyQuota();
        int maxBrain = PlayerBrainDamageData.getBrainMax(subject);
        viewer.sendSystemMessage(Component.literal("脑损: " + brain + "/" + maxBrain
                + "　今日领域: " + expands + "（免费额度 " + free + "）"));
        viewer.sendSystemMessage(Component.literal("领域量表: " + PlayerDomainGaugeData.getGauge(subject)
                + "/" + PlayerDomainGaugeData.getMax(subject)
                + String.format("（充能效率 %.0f%%）",
                subject.getAttribute(ModAttributes.DOMAIN_GAUGE_CHARGE_EFFICIENCY.get()) == null
                        ? 100.0D
                        : subject.getAttribute(ModAttributes.DOMAIN_GAUGE_CHARGE_EFFICIENCY.get()).getValue())));
        viewer.sendSystemMessage(Component.literal("今日熔断修复: "
                + PlayerBrainDamageData.getMeltdownRepairsToday(subject)));

        List<Component> crimeRecordLines = new ArrayList<>();
        PlayerCrimeRecordData.appendDisplayLines(subject, crimeRecordLines);
        for (Component line : crimeRecordLines)
        {
            viewer.sendSystemMessage(line);
        }
    }

    private static String getBarrierLevelDisplayName(BarrierTechniqueLevel level)
    {
        return switch (level)
        {
            case UNLEARNED -> "未习得";
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
        };
    }

    private static String getSeanceLevelDisplayName(SeanceTechniqueLevel level)
    {
        return switch (level)
        {
            case UNLEARNED -> "未习得";
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
        };
    }

    private static String getStrengthenLevelDisplayName(StrengthenTechniqueLevel level)
    {
        return switch (level)
        {
            case UNLEARNED -> "未习得";
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
        };
    }
}
