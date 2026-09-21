package com.darkness.jujutsukaisenzone.technique.aerial;

import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTreeRegistry;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 空战能力技能定义与开发树注册。
 */
public final class AerialCombatSkillRegistry
{
    private static final TechniqueSkill CURSE_LEVITATION = new TechniqueSkill(
            AerialCombatSkills.SKILL_CURSE_LEVITATION,
            0,
            Component.translatable("skill.jujutsukaisenzone.curse_levitation"),
            "",
            0,
            0,
            0,
            "",
            AerialCombatSkills.TAG_CURSE_LEVITATION_UNLOCK,
            "");

    private static final TechniqueSkill AERIAL_DASH = new TechniqueSkill(
            AerialCombatSkills.SKILL_AERIAL_DASH,
            0,
            Component.translatable("skill.jujutsukaisenzone.aerial_dash"),
            "",
            0,
            0,
            0,
            "",
            AerialCombatSkills.TAG_AERIAL_DASH_UNLOCK,
            "");

    private static final TechniqueSkill AERIAL_MULTI_JUMP = new TechniqueSkill(
            AerialCombatSkills.SKILL_AERIAL_MULTI_JUMP,
            0,
            Component.translatable("skill.jujutsukaisenzone.aerial_multi_jump"),
            "",
            0,
            0,
            0,
            "",
            AerialCombatSkills.TAG_AERIAL_MULTI_JUMP_UNLOCK,
            "");

    private static final TechniqueSkillTree SKILL_TREE = TechniqueSkillTree.builder(AerialCombatSkills.TREE_ID)
            .node(SkillTreeNodeDef.skill("curse_levitation", AerialCombatSkills.SKILL_CURSE_LEVITATION)
                    .vanillaXpLevelCost(AerialCombatSkills.CURSE_LEVITATION_XP_LEVEL_COST)
                    .build())
            .node(SkillTreeNodeDef.skill("aerial_dash", AerialCombatSkills.SKILL_AERIAL_DASH)
                    .vanillaXpLevelCost(AerialCombatSkills.AERIAL_DASH_XP_LEVEL_COST)
                    .build())
            .node(SkillTreeNodeDef.skill("aerial_multi_jump", AerialCombatSkills.SKILL_AERIAL_MULTI_JUMP)
                    .vanillaXpLevelCost(AerialCombatSkills.AERIAL_MULTI_JUMP_XP_LEVEL_COST)
                    .build())
            .build();

    private static final Map<String, TechniqueSkill> SKILLS = new LinkedHashMap<>();

    static
    {
        register(CURSE_LEVITATION);
        register(AERIAL_DASH);
        register(AERIAL_MULTI_JUMP);
    }

    private AerialCombatSkillRegistry()
    {
    }

    public static void bootstrap()
    {
        TechniqueSkillTreeRegistry.register(SKILL_TREE);
    }

    private static void register(TechniqueSkill skill)
    {
        SKILLS.put(skill.skillId(), skill);
    }

    public static Optional<TechniqueSkill> find(String skillId)
    {
        return Optional.ofNullable(SKILLS.get(skillId));
    }

    public static boolean isUnlocked(Player player, String skillId)
    {
        return find(skillId)
                .map(skill -> player.getPersistentData().getBoolean(skill.unlockTag()))
                .orElse(false);
    }
}
