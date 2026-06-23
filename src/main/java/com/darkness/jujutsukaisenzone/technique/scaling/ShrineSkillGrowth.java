package com.darkness.jujutsukaisenzone.technique.scaling;



import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;

import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;



/**

 * 御厨子术式技能随咒术等级成长的数值表。

 */

public final class ShrineSkillGrowth

{

    private ShrineSkillGrowth()

    {

    }



    /** 「解」挥砍柱形每 tick 伤害。 */

    public static float kaiColumnDamage(@Nullable LivingEntity owner)

    {

        return SkillCombatConfigRegistry.resolveFloat(

                SkillCombatConfigRegistry.SKILL_SHRINE_KAI, "damage", owner);

    }



    /** 「解」切割柱形每 tick 伤害。 */

    public static float kaiFlyobjColumnDamage(@Nullable LivingEntity owner)

    {

        return SkillCombatConfigRegistry.resolveFloat(

                SkillCombatConfigRegistry.SKILL_SHRINE_KAI_FLYOBJ, "damage", owner);

    }



    /** 「捌」每间隔伤害。 */

    public static float hachiDamage(@Nullable LivingEntity owner)

    {

        return SkillCombatConfigRegistry.resolveFloat(

                SkillCombatConfigRegistry.SKILL_SHRINE_HACHI, "damage", owner);

    }

}


