package com.darkness.jujutsukaisenzone.technique.scaling;

import com.darkness.jujutsukaisenzone.entity.data.ShrineHachiConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 御厨子术式技能随咒术等级成长的数值表。
 * <p>
 * 配置类为四级（{@link PlayerCursedGradeData#RANK_GRADE_4}）基准；
 * 每高于四级一级：中幅度提升伤害。「灶」开不在此成长范围内。
 */
public final class ShrineSkillGrowth
{
    /** 相对四级基准，每提升一级咒术等级的伤害增量。 */
    private static final float DAMAGE_PER_RANK = 2.0F;

    private ShrineSkillGrowth()
    {
    }

    /** 「解」挥砍 / 「解」切割柱形每 tick 伤害。 */
    public static float kaiColumnDamage(@Nullable LivingEntity owner)
    {
        return ShrineKaiConfig.COLUMN_DAMAGE + DAMAGE_PER_RANK * SkillGrowthSteps.stepsAboveBaseline(owner);
    }

    /** 「捌」每间隔伤害。 */
    public static float hachiDamage(@Nullable LivingEntity owner)
    {
        return ShrineHachiConfig.DAMAGE + DAMAGE_PER_RANK * SkillGrowthSteps.stepsAboveBaseline(owner);
    }
}
