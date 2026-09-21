package com.darkness.jujutsukaisenzone.technique.commonskill;

import com.darkness.jujutsukaisenzone.player.PlayerStrengthenTechniqueData;
import com.darkness.jujutsukaisenzone.technique.StrengthenTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通用术式技能：习得后可拖入任意生得术式的技能快捷栏。
 */
public final class CommonTechniqueSkillRegistry
{
    public static final String STRENGTHEN_TECHNIQUE_ID = "strengthen_technique";

    private static final TechniqueSkill STRENGTHEN_SKILL = new TechniqueSkill(
            STRENGTHEN_TECHNIQUE_ID,
            0,
            Component.translatable("skill.jujutsukaisenzone.strengthen_technique"),
            "textures/gui/technique/strengthen.png",
            0,
            0,
            0,
            "",
            "",
            "");

    private static final Map<String, TechniqueSkill> SKILLS = new LinkedHashMap<>();

    static
    {
        register(STRENGTHEN_SKILL);
    }

    private CommonTechniqueSkillRegistry()
    {
    }

    private static void register(TechniqueSkill skill)
    {
        SKILLS.put(skill.skillId(), skill);
    }

    public static List<TechniqueSkill> getConfigurableSkills()
    {
        return List.copyOf(SKILLS.values());
    }

    public static boolean isRegistered(String skillId)
    {
        return SKILLS.containsKey(skillId);
    }

    public static Optional<TechniqueSkill> find(String skillId)
    {
        return Optional.ofNullable(SKILLS.get(skillId));
    }

    public static boolean isUnlocked(Player player, String skillId)
    {
        if (STRENGTHEN_TECHNIQUE_ID.equals(skillId))
        {
            return PlayerStrengthenTechniqueData.hasLearned(player);
        }
        return false;
    }

    public static void useSkill(ServerPlayer player, String skillId)
    {
        if (STRENGTHEN_TECHNIQUE_ID.equals(skillId))
        {
            StrengthenTechniqueServerLogic.cast(player);
        }
    }
}
