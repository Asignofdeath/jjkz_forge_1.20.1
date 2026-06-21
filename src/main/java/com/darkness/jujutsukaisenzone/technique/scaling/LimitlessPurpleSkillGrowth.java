package com.darkness.jujutsukaisenzone.technique.scaling;

import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 「茈」随咒术等级成长的数值表。
 * <p>
 * 槽位基准冷却 14 秒（四级）；每高于四级一级：小幅度减少冷却。
 * 超远程虚式茈不在此成长范围内。
 */
public final class LimitlessPurpleSkillGrowth
{
    /** 四级基准冷却（tick），与 {@code LimitlessTechniqueSkillSet} 茈槽位一致。 */
    private static final int BASELINE_COOLDOWN_TICKS = 20 * 14;
    /** 相对四级基准，每提升一级咒术等级的冷却减少量（tick）。 */
    private static final int COOLDOWN_REDUCTION_PER_RANK = 20;

    private LimitlessPurpleSkillGrowth()
    {
    }

    public static int cooldownTicks(@Nullable Player player)
    {
        int steps = SkillGrowthSteps.stepsAboveBaseline(player);
        return BASELINE_COOLDOWN_TICKS - COOLDOWN_REDUCTION_PER_RANK * steps;
    }
}
