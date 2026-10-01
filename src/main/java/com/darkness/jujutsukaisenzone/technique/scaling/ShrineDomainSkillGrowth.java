package com.darkness.jujutsukaisenzone.technique.scaling;



import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;

import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;



/**

 * 「伏魔神龛」领域随咒术等级成长的数值表（数值来自 {@code config/jujutsukaisenzone-common.toml}）。

 */

public final class ShrineDomainSkillGrowth

{

    /** 【配置项·默认值】四级基准：普通斩击每次伤害（1.5 颗心）。 */

    public static final float BASELINE_SLASH_DAMAGE = 0.15F;

    /** 【配置项·默认值】普通斩击每升一级咒术等级的增量。 */

    public static final float BASELINE_SLASH_DAMAGE_PER_RANK = 0.25F;

    /** 【配置项·默认值】四级基准：强力斩击每次伤害（3 颗心）。 */

    public static final float BASELINE_POWER_SLASH_DAMAGE = 0.30F;

    /** 【配置项·默认值】强力斩击每升一级咒术等级的增量。 */

    public static final float BASELINE_POWER_SLASH_DAMAGE_PER_RANK = 0.50F;



    private ShrineDomainSkillGrowth()

    {

    }



    public static float slashDamage(@Nullable Player player)

    {

        return SkillCombatConfigRegistry.resolveFloat(

                SkillCombatConfigRegistry.SKILL_SHRINE_DOMAIN, "slash_damage", player);

    }



    public static float powerSlashDamage(@Nullable Player player)

    {

        return SkillCombatConfigRegistry.resolveFloat(

                SkillCombatConfigRegistry.SKILL_SHRINE_DOMAIN, "power_slash_damage", player);

    }

}


