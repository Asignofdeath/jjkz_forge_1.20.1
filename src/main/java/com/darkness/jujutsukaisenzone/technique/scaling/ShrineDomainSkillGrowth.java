package com.darkness.jujutsukaisenzone.technique.scaling;

import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 「伏魔神龛」领域随咒术等级成长的数值表。
 * <p>
 * 四级（{@link PlayerCursedGradeData#RANK_GRADE_4}）为基准伤害；
 * 每高于四级一级：大幅提升普通斩击与强力斩击伤害。
 */
public final class ShrineDomainSkillGrowth
{
    /** 四级基准：普通斩击每次伤害（1.5 颗心）。 */
    public static final float BASELINE_SLASH_DAMAGE = 3.0F;
    /** 四级基准：强力斩击每次伤害（3 颗心）。 */
    public static final float BASELINE_POWER_SLASH_DAMAGE = 6.0F;
    /** 相对四级基准，每提升一级咒术等级的普通斩击伤害增量。 */
    private static final float SLASH_DAMAGE_PER_RANK = 5.0F;
    /** 相对四级基准，每提升一级咒术等级的强力斩击伤害增量。 */
    private static final float POWER_SLASH_DAMAGE_PER_RANK = 10.0F;

    private ShrineDomainSkillGrowth()
    {
    }

    public static float slashDamage(@Nullable Player player)
    {
        return BASELINE_SLASH_DAMAGE + SLASH_DAMAGE_PER_RANK * SkillGrowthSteps.stepsAboveBaseline(player);
    }

    public static float powerSlashDamage(@Nullable Player player)
    {
        return BASELINE_POWER_SLASH_DAMAGE + POWER_SLASH_DAMAGE_PER_RANK * SkillGrowthSteps.stepsAboveBaseline(player);
    }
}
