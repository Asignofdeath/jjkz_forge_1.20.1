package com.darkness.jujutsukaisenzone.technique.development;

/**
 * 机制型技能「术式解放」定义：关联主动满级 + 消耗 SP。
 */
public record MechanicLiberationDef(String mechanicSkillId, String requiredActiveSkillId, int spCost)
{
    public MechanicLiberationDef
    {
        if (mechanicSkillId == null || mechanicSkillId.isBlank())
        {
            throw new IllegalArgumentException("mechanicSkillId");
        }
        if (requiredActiveSkillId == null || requiredActiveSkillId.isBlank())
        {
            throw new IllegalArgumentException("requiredActiveSkillId");
        }
        spCost = Math.max(0, spCost);
    }
}
