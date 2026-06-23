package com.darkness.jujutsukaisenzone.technique.description;

import com.darkness.jujutsukaisenzone.entity.data.LimitlessBlueConfig;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessPurpleConfig;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRedConfig;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRemoteMurasakiConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineFugaConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineHachiConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessBlueSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessPurpleSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessRedSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * 术式技能描述入口：注册各技能数值解析，并组装完整描述视图。
 */
public final class TechniqueSkillDescriptions
{
    private TechniqueSkillDescriptions()
    {
    }

    public static void bootstrap()
    {
        registerLimitlessSkills();
        registerShrineSkills();
    }

    /**
     * 根据技能定义与玩家上下文生成描述快照（阶段 2 悬停框将直接消费此结构）。
     */
    public static TechniqueSkillDescriptionView resolve(Player player, String techniqueId, TechniqueSkill skill)
    {
        if (skill == null)
        {
            return new TechniqueSkillDescriptionView(
                    Component.empty(),
                    0,
                    0,
                    Component.empty());
        }

        String skillId = skill.skillId();
        Object[] args = resolveDescriptionArgs(player, techniqueId, skill);
        Component description = Component.translatable(TechniqueSkillDescriptionKeys.description(skillId), args);
        return new TechniqueSkillDescriptionView(
                skill.displayName(),
                skill.cost(),
                TechniqueSkillStatFormat.ticksToSeconds(resolveCooldownTicks(player, techniqueId, skill)),
                description);
    }

    public static Component resolveDescriptionComponent(Player player, String techniqueId, TechniqueSkill skill)
    {
        return resolve(player, techniqueId, skill).description();
    }

    private static int resolveCooldownTicks(Player player, String techniqueId, TechniqueSkill skill)
    {
        if (PlayerTechniqueData.TECHNIQUE_LIMITLESS.equals(techniqueId)
                && LimitlessTechniqueSkillSet.SKILL_PURPLE.equals(skill.skillId()))
        {
            return LimitlessPurpleSkillGrowth.cooldownTicks(player);
        }
        return skill.cooldownTicks();
    }

    private static Object[] resolveDescriptionArgs(Player player, String techniqueId, TechniqueSkill skill)
    {
        TechniqueSkillDescriptionArgsResolver resolver = TechniqueSkillDescriptionRegistry.get(skill.skillId());
        if (resolver == null)
        {
            return new Object[0];
        }
        return resolver.resolve(player, techniqueId, skill);
    }

    private static void registerLimitlessSkills()
    {
        TechniqueSkillDescriptionRegistry.register(
                LimitlessTechniqueSkillSet.SKILL_BLUE,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(LimitlessBlueConfig.DAMAGE_RADIUS),
                        Integer.toString(LimitlessBlueConfig.DAMAGE_INTERVAL),
                        TechniqueSkillStatFormat.formatNumber(LimitlessBlueSkillGrowth.damage(player)),
                        TechniqueSkillStatFormat.formatNumber(LimitlessBlueSkillGrowth.pullRadius(player))
                });

        TechniqueSkillDescriptionRegistry.register(
                LimitlessTechniqueSkillSet.SKILL_RED,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(LimitlessRedSkillGrowth.explosionPower(player)),
                        TechniqueSkillStatFormat.formatNumber(LimitlessRedConfig.BLOCK_LAUNCH_HURT_DAMAGE_PER_DISTANCE)
                });

        TechniqueSkillDescriptionRegistry.register(
                LimitlessTechniqueSkillSet.SKILL_PURPLE,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(LimitlessPurpleConfig.ARROW_BLOCK_ERASE_RADIUS),
                        TechniqueSkillStatFormat.formatNumber(LimitlessPurpleConfig.ARROW_SPHERE_DAMAGE_PER_TICK)
                });

        TechniqueSkillDescriptionRegistry.register(
                LimitlessTechniqueSkillSet.SKILL_REMOTE_PURPLE,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatSecondsFromTicks(
                                LimitlessRemoteMurasakiConfig.HOLD_RELEASE_CHARGE_TICKS),
                        TechniqueSkillStatFormat.formatNumber(LimitlessRemoteMurasakiConfig.ARROW_BLOCK_ERASE_RADIUS),
                        TechniqueSkillStatFormat.formatNumber(LimitlessRemoteMurasakiConfig.ARROW_SPHERE_DAMAGE_PER_TICK),
                        Integer.toString(SkillCombatConfigRegistry.resolveInt(
                                SkillCombatConfigRegistry.SKILL_LIMITLESS_REMOTE_PURPLE,
                                "max_flight_chunks",
                                player))
                });
    }

    private static void registerShrineSkills()
    {
        TechniqueSkillDescriptionRegistry.register(
                ShrineTechniqueSkillSet.SKILL_KAI,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(ShrineKaiConfig.COLUMN_RADIUS),
                        TechniqueSkillStatFormat.formatNumber(ShrineSkillGrowth.kaiColumnDamage(player))
                });

        TechniqueSkillDescriptionRegistry.register(
                ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(ShrineKaiConfig.FLYOBJ_MAX_RANGE),
                        TechniqueSkillStatFormat.formatNumber(ShrineKaiConfig.COLUMN_RADIUS),
                        TechniqueSkillStatFormat.formatNumber(ShrineSkillGrowth.kaiFlyobjColumnDamage(player))
                });

        TechniqueSkillDescriptionRegistry.register(
                ShrineTechniqueSkillSet.SKILL_HACHI,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(2.0D),
                        Integer.toString(ShrineHachiConfig.DAMAGE_INTERVAL),
                        TechniqueSkillStatFormat.formatNumber(ShrineSkillGrowth.hachiDamage(player))
                });

        TechniqueSkillDescriptionRegistry.register(
                ShrineTechniqueSkillSet.SKILL_FUGA,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatSecondsFromTicks(ShrineFugaConfig.CHARGE_TICKS),
                        TechniqueSkillStatFormat.formatNumber(ShrineFugaConfig.BOOM_RADIUS),
                        TechniqueSkillStatFormat.formatSecondsFromTicks(ShrineFugaConfig.BOOM_LIFETIME_TICKS),
                        TechniqueSkillStatFormat.formatNumber(ShrineFugaConfig.BOOM_DAMAGE_PER_TICK)
                });
    }
}
