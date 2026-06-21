package com.darkness.jujutsukaisenzone.technique.scaling;

import com.darkness.jujutsukaisenzone.entity.data.LimitlessBlueConfig;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 「苍」随咒术等级成长的数值表。
 * <p>
 * {@link LimitlessBlueConfig} 为四级（{@link PlayerCursedGradeData#RANK_GRADE_4}）基准；
 * 每高于四级一级：中幅度提升伤害、小幅度提升吸附范围。
 */
public final class LimitlessBlueSkillGrowth
{
    /** 相对四级基准，每提升一级咒术等级的伤害增量。 */
    private static final float DAMAGE_PER_RANK = 2.0F;
    /** 相对四级基准，每提升一级咒术等级的吸附半径增量（格）。 */
    private static final double PULL_RADIUS_PER_RANK = 1.0D;

    private LimitlessBlueSkillGrowth()
    {
    }

    public static float damage(@Nullable LivingEntity owner)
    {
        return LimitlessBlueConfig.DAMAGE + DAMAGE_PER_RANK * SkillGrowthSteps.stepsAboveBaseline(owner);
    }

    public static double pullRadius(@Nullable LivingEntity owner)
    {
        return LimitlessBlueConfig.PULL_RADIUS + PULL_RADIUS_PER_RANK * SkillGrowthSteps.stepsAboveBaseline(owner);
    }
}
