package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.registry.ModItems;
import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 主动技能升级消耗表：key = skillId，value = 从 1→2 … (MAX-1)→MAX 的四级消耗。
 */
public final class SkillUpgradeCostRegistry
{
    private static final Map<String, List<SkillUpgradeCost>> COSTS = new HashMap<>();

    private SkillUpgradeCostRegistry()
    {
    }

    public static void bootstrap()
    {
        COSTS.clear();
        // 样板：审判三主动各吃一类通用咒物
        registerFlat(JudgmentTechniqueSkillSet.SKILL_GAVEL, 5, ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 1);
        registerFlat(JudgmentTechniqueSkillSet.SKILL_TRIAL, 8, ModItems.CURSED_OBJECT_BROKEN_GETA, 1);
        registerFlat(JudgmentTechniqueSkillSet.SKILL_ACCUSE, 8, ModItems.CURSED_OBJECT_TORN_FAN, 1);
    }

    private static void registerFlat(
            String skillId,
            int vanillaXpLevels,
            net.minecraftforge.registries.RegistryObject<? extends net.minecraft.world.item.Item> item,
            int count)
    {
        SkillUpgradeCost step = SkillUpgradeCost.of(vanillaXpLevels, item, count);
        COSTS.put(skillId, Collections.nCopies(SkillLevelRules.MAX_LEVEL - 1, step));
    }

    public static boolean isUpgradeable(String skillId)
    {
        return COSTS.containsKey(skillId);
    }

    /**
     * 当前等级升到下一级的消耗；已满级或不在表中返回 empty。
     */
    public static Optional<SkillUpgradeCost> costForNextLevel(String skillId, int currentLevel)
    {
        List<SkillUpgradeCost> steps = COSTS.get(skillId);
        if (steps == null || steps.isEmpty())
        {
            return Optional.empty();
        }
        int clamped = SkillLevelRules.clampLevel(currentLevel);
        if (clamped >= SkillLevelRules.MAX_LEVEL)
        {
            return Optional.empty();
        }
        int index = clamped - 1;
        if (index < 0 || index >= steps.size())
        {
            return Optional.empty();
        }
        return Optional.of(steps.get(index));
    }
}
