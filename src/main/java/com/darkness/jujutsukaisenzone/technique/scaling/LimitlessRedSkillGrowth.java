package com.darkness.jujutsukaisenzone.technique.scaling;



import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;

import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;



/**

 * 「赫」随咒术等级成长的数值表。

 * <p>

 * 真值来自 {@code config/jujutsukaisenzone-common.toml} 的 {@code [skillCombat.limitlessRed]} 段，

 * 经 {@link SkillCombatConfigRegistry} 汇总；配置未加载时退回 {@code LimitlessRedConfig} 上标注

 * 【配置项·默认值】的常量。

 * <p>

 * ⛔ 本注释此前写的是 {@code data/jujutsukaisenzone/skill_combat/limitless_red.json}——那个文件

 * 从来不存在。别再照抄那个路径。

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


