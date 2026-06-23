package com.darkness.jujutsukaisenzone.technique.scaling;



import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;

import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;



/**

 * 「苍」随咒术等级成长的数值表（数值来自 {@code data/jujutsukaisenzone/skill_combat/limitless_blue.json}）。

 */

public final class LimitlessBlueSkillGrowth

{

    private LimitlessBlueSkillGrowth()

    {

    }



    public static float damage(@Nullable LivingEntity owner)

    {

        return SkillCombatConfigRegistry.resolveFloat(

                SkillCombatConfigRegistry.SKILL_LIMITLESS_BLUE, "damage", owner);

    }



    public static double pullRadius(@Nullable LivingEntity owner)

    {

        return SkillCombatConfigRegistry.resolveDouble(

                SkillCombatConfigRegistry.SKILL_LIMITLESS_BLUE, "pull_radius", owner);

    }

}


