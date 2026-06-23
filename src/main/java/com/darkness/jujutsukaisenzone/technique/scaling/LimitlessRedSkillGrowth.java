package com.darkness.jujutsukaisenzone.technique.scaling;



import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;

import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;



/**

 * 「赫」随咒术等级成长的数值表（数值来自 {@code data/jujutsukaisenzone/skill_combat/limitless_red.json}）。

 */

public final class LimitlessRedSkillGrowth

{

    private LimitlessRedSkillGrowth()

    {

    }



    public static float explosionPower(@Nullable LivingEntity owner)

    {

        return SkillCombatConfigRegistry.resolveFloat(

                SkillCombatConfigRegistry.SKILL_LIMITLESS_RED, "explosion_power", owner);

    }



    public static double knockbackRadius(@Nullable LivingEntity owner)

    {

        return SkillCombatConfigRegistry.resolveDouble(

                SkillCombatConfigRegistry.SKILL_LIMITLESS_RED, "knockback_radius", owner);

    }

}


