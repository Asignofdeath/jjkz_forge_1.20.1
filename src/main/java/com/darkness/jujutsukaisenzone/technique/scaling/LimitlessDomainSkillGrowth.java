package com.darkness.jujutsukaisenzone.technique.scaling;

import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 「无量空处」随咒术等级成长的数值表。
 * <p>
 * 维持领域咒力消耗以四级（{@link PlayerCursedGradeData#RANK_GRADE_4}）为基准；
 * 每高于四级一级：小幅度降低维持消耗。
 */
public final class LimitlessDomainSkillGrowth
{
    /** 相对四级基准，每提升一级咒术等级的维持咒力消耗减免比例。 */
    private static final double MAINTAIN_COST_REDUCTION_PER_RANK = 0.05D;

    private LimitlessDomainSkillGrowth()
    {
    }

    /**
     * 维持领域咒力消耗减免比例（0.0 = 无减免，0.2 = 减免 20%）。
     */
    public static double maintainCostReduction(@Nullable Player player)
    {
        return MAINTAIN_COST_REDUCTION_PER_RANK * SkillGrowthSteps.stepsAboveBaseline(player);
    }
}
