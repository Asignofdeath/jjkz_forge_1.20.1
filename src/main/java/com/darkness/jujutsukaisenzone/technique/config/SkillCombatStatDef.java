package com.darkness.jujutsukaisenzone.technique.config;

import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.technique.scaling.SkillGrowthSteps;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 单项战斗数值：以 {@link #baselineGrade} 为基准，每高一级咒术等级增加 {@link #perRank}。
 */
public record SkillCombatStatDef(String key, double baseline, double perRank, int baselineGrade)
{
    public SkillCombatStatDef(String key, double baseline, double perRank)
    {
        this(key, baseline, perRank, PlayerCursedGradeData.RANK_GRADE_4);
    }

    public float resolveFloat(@Nullable LivingEntity owner)
    {
        return (float) resolveDouble(owner);
    }

    public double resolveDouble(@Nullable LivingEntity owner)
    {
        int steps = SkillGrowthSteps.stepsAboveBaseline(owner, baselineGrade);
        return baseline + perRank * steps;
    }

    public int resolveInt(@Nullable LivingEntity owner)
    {
        return (int) Math.round(resolveDouble(owner));
    }
}
