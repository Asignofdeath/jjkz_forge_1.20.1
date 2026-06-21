package com.darkness.jujutsukaisenzone.technique.description;

import net.minecraft.network.chat.Component;

/**
 * 阶段 1 产出的技能描述快照：名称、咒力、冷却与正文（已填入伤害等变量）。
 */
public record TechniqueSkillDescriptionView(
        Component displayName,
        int cursedEnergyCost,
        int cooldownSeconds,
        Component description)
{
}
