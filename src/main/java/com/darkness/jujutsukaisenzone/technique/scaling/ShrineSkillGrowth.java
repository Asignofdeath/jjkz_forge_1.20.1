package com.darkness.jujutsukaisenzone.technique.scaling;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 御厨子术式技能随咒术等级成长的数值表。
 * <p>
 * 配置基值再乘主动技能等级倍率（{@link SkillUpgradeLogic#scaleRatio}）。
 */
public final class ShrineSkillGrowth
{
    private ShrineSkillGrowth()
    {
    }

    /** 「解」挥砍柱形每 tick 伤害。 */
    public static float kaiColumnDamage(@Nullable LivingEntity owner)
    {
        float base = SkillCombatConfigRegistry.resolveFloat(
                SkillCombatConfigRegistry.SKILL_SHRINE_KAI, "damage", owner);
        return SkillUpgradeLogic.scaleRatio(
                owner, PlayerTechniqueData.TECHNIQUE_SHRINE, ShrineTechniqueSkillSet.SKILL_KAI, base);
    }

    /** 「解」切割柱形每 tick 伤害。 */
    public static float kaiFlyobjColumnDamage(@Nullable LivingEntity owner)
    {
        float base = SkillCombatConfigRegistry.resolveFloat(
                SkillCombatConfigRegistry.SKILL_SHRINE_KAI_FLYOBJ, "damage", owner);
        return SkillUpgradeLogic.scaleRatio(
                owner, PlayerTechniqueData.TECHNIQUE_SHRINE, ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ, base);
    }

    /** 「捌」每间隔伤害。 */
    public static float hachiDamage(@Nullable LivingEntity owner)
    {
        float base = SkillCombatConfigRegistry.resolveFloat(
                SkillCombatConfigRegistry.SKILL_SHRINE_HACHI, "damage", owner);
        return SkillUpgradeLogic.scaleRatio(
                owner, PlayerTechniqueData.TECHNIQUE_SHRINE, ShrineTechniqueSkillSet.SKILL_HACHI, base);
    }
}
