package com.darkness.jujutsukaisenzone.technique.scaling;

import com.darkness.jujutsukaisenzone.entity.data.LimitlessRedConfig;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 「赫」随咒术等级成长的数值表。
 * <p>
 * {@link LimitlessRedConfig} 为四级（{@link PlayerCursedGradeData#RANK_GRADE_4}）基准；
 * 每高于四级一级：中幅度提升爆炸伤害。
 */
public final class LimitlessRedSkillGrowth
{
    /** 相对四级基准，每提升一级咒术等级的爆炸威力增量。 */
    private static final float EXPLOSION_POWER_PER_RANK = 2.0F;

    private LimitlessRedSkillGrowth()
    {
    }

    public static float explosionPower(@Nullable LivingEntity owner)
    {
        return LimitlessRedConfig.ARROW_EXPLOSION_POWER
                + EXPLOSION_POWER_PER_RANK * SkillGrowthSteps.stepsAboveBaseline(owner);
    }
}
