package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 机制型技能术式解放表：关联主动满级 + SP 消耗。
 */
public final class MechanicLiberationRegistry
{
    private static final Map<String, MechanicLiberationDef> BY_MECHANIC = new HashMap<>();

    private MechanicLiberationRegistry()
    {
    }

    public static void bootstrap()
    {
        BY_MECHANIC.clear();
        register(new MechanicLiberationDef(
                JudgmentTechniqueSkillSet.SKILL_GAVEL_FORMS,
                JudgmentTechniqueSkillSet.SKILL_GAVEL,
                3));
        register(new MechanicLiberationDef(
                JudgmentTechniqueSkillSet.SKILL_SENTENCING,
                JudgmentTechniqueSkillSet.SKILL_TRIAL,
                5));
        register(new MechanicLiberationDef(
                JudgmentTechniqueSkillSet.SKILL_AGGRAVATION,
                JudgmentTechniqueSkillSet.SKILL_ACCUSE,
                8));
        register(new MechanicLiberationDef(
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_SLING,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_GATE,
                5));
        register(new MechanicLiberationDef(
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_CHAIN,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_FLIGHT,
                8));
        register(new MechanicLiberationDef(
                RatioTechniqueSkillSet.SKILL_OVERTIME_VOW,
                RatioTechniqueSkillSet.SKILL_RATIO_MARK,
                5));
    }

    private static void register(MechanicLiberationDef def)
    {
        BY_MECHANIC.put(def.mechanicSkillId(), def);
    }

    public static Optional<MechanicLiberationDef> find(String mechanicSkillId)
    {
        if (mechanicSkillId == null || mechanicSkillId.isBlank())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_MECHANIC.get(mechanicSkillId));
    }

    public static boolean isLiberationMechanic(String skillId)
    {
        return find(skillId).isPresent();
    }
}
