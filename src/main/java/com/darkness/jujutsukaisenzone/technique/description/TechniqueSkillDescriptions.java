package com.darkness.jujutsukaisenzone.technique.description;

import com.darkness.jujutsukaisenzone.entity.GavelProjectileEntity;
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
import com.darkness.jujutsukaisenzone.player.JudgmentTrialTargetData;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentWeaponLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skill.StarFlightSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkDetonation;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkRules;
import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.darkness.jujutsukaisenzone.technique.commonskill.CommonTechniqueSkillRegistry;
import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkills;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.StrengthenTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.player.PlayerStrengthenTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.StrengthenTechniqueBonuses;
import com.darkness.jujutsukaisenzone.player.StrengthenTechniqueLevel;
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
        registerJudgmentSkills();
        registerLoveRendezvousSkills();
        registerCommonSkills();
        registerAerialSkills();
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
        if (CommonTechniqueSkillRegistry.STRENGTHEN_TECHNIQUE_ID.equals(skillId))
        {
            return new TechniqueSkillDescriptionView(
                    skill.displayName(),
                    StrengthenTechniqueServerLogic.getMaintainCostPerSecond(player),
                    0,
                    description,
                    true);
        }
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

    private static void registerJudgmentSkills()
    {
        TechniqueSkillDescriptionRegistry.register(
                JudgmentTechniqueSkillSet.SKILL_GAVEL,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(GavelProjectileEntity.BASE_DAMAGE),
                        TechniqueSkillStatFormat.formatNumber(GavelProjectileEntity.IMPACT_EXPLOSION_POWER),
                        TechniqueSkillStatFormat.formatNumber(
                                (JudgmentWeaponLogic.SINFUL_ONE_DAMAGE_MULTIPLIER - 1.0F) * 100.0F)
                });

        TechniqueSkillDescriptionRegistry.register(
                JudgmentTechniqueSkillSet.SKILL_TRIAL,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatSecondsFromTicks(JudgmentTrialSkillLogic.LOCK_CHARGE_TICKS),
                        TechniqueSkillStatFormat.formatNumber(JudgmentTrialSkillLogic.BASE_GUILTY_CHANCE_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(JudgmentTrialTargetData.GUILTY_BONUS_PER_TRIAL_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(JudgmentTrialTargetData.MAX_GUILTY_BONUS_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(JudgmentTrialSkillLogic.DEATH_SENTENCE_CHANCE_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(
                                JudgmentTrialSkillLogic.DEATH_SENTENCE_WITH_CONFISCATION_CHANCE_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(
                                JudgmentTrialSkillLogic.DEATH_SENTENCE_BONUS_PER_DEBUFF_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(
                                JudgmentTrialSkillLogic.MAX_DEATH_SENTENCE_DEBUFF_BONUS_PERCENT)
                });

        TechniqueSkillDescriptionRegistry.register(
                JudgmentTechniqueSkillSet.SKILL_ACCUSE,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatSecondsFromTicks(JudgmentAccuseSkillLogic.CHOICE_WINDOW_TICKS),
                        TechniqueSkillStatFormat.formatNumber(JudgmentAccuseSkillLogic.EVIDENCE_CONFIRMED_CHANCE_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(JudgmentTrialTargetData.GUILTY_BONUS_PER_TRIAL_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(JudgmentTrialTargetData.MAX_GUILTY_BONUS_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(JudgmentTrialSkillLogic.DEATH_SENTENCE_CHANCE_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(
                                JudgmentTrialSkillLogic.DEATH_SENTENCE_BONUS_PER_DEBUFF_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(
                                JudgmentTrialSkillLogic.MAX_DEATH_SENTENCE_DEBUFF_BONUS_PERCENT)
                });
    }

    private static void registerLoveRendezvousSkills()
    {
        TechniqueSkillDescriptionRegistry.register(
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_MARK,
                (player, techniqueId, skill) -> new Object[] {
                        /* 顺序必须与 lang 里 %s 的出现次序一致：
                           持有上限 → 距离 → 等级 → 衰减 → 引爆半径 → 引爆单体伤害
                           （星种数量与其效果已搬到开发树的说明节点，不再注入） */
                        Integer.toString(PlayerStarMarkData.MAX_MARKS),
                        TechniqueSkillStatFormat.formatNumber(StarMarkRules.MARK_RANGE),
                        Integer.toString(StarMark.MAX_STACKS),
                        Integer.toString(StarMarkRules.STACK_DECAY_TICKS / 20),
                        TechniqueSkillStatFormat.formatNumber(StarMarkDetonation.RADIUS),
                        TechniqueSkillStatFormat.formatNumber(StarMarkDetonation.DETONATION_DAMAGE)
                });
        TechniqueSkillDescriptionRegistry.register(
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_PULL,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(StarMarkRules.PULL_RANGE),
                        Integer.toString(StarMarkRules.PULL_DURATION_TICKS / 20)
                });
        TechniqueSkillDescriptionRegistry.register(
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_GATE,
                (player, techniqueId, skill) -> new Object[] {
                        /*
                         * 星域半径已托管给「结界尺寸调节器」，具体档位只存在服务端，
                         * 而描述是客户端渲染的——所以只能报「默认 / 上限」区间，不报本人当前值。
                         * 星域本身没有时限，所以「持续秒数」填的是**跨序解锁的存活窗口**，不是域的寿命。
                         */
                        Integer.toString(BarrierTechniqueServerLogic.staticGetDefaultRadius()),
                        Integer.toString(BarrierTechniqueServerLogic.staticGetMaxRadius()),
                        Integer.toString(StarMarkRules.UNLOCK_DECAY_TICKS / 20)
                });
        TechniqueSkillDescriptionRegistry.register(
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_FLIGHT,
                (player, techniqueId, skill) -> new Object[] {
                        /* 顺序必须与 lang 里 %s 的出现次序一致：射程 → 每颗加成 → 加成上限 → 冲撞伤害 → 引爆单体伤害 → 路径伤害 */
                        TechniqueSkillStatFormat.formatNumber(StarFlightSkillLogic.CAST_RANGE),
                        TechniqueSkillStatFormat.formatNumber(StarFlightSkillLogic.PASS_BONUS_PER_STAR * 100.0D),
                        TechniqueSkillStatFormat.formatNumber(StarFlightSkillLogic.PASS_BONUS_CAP * 100.0D),
                        TechniqueSkillStatFormat.formatNumber(StarFlightSkillLogic.RAM_DAMAGE),
                        TechniqueSkillStatFormat.formatNumber(StarMarkDetonation.DETONATION_DAMAGE),
                        TechniqueSkillStatFormat.formatNumber(StarFlightSkillLogic.PATH_DAMAGE)
                });
    }

    private static void registerCommonSkills()
    {
        TechniqueSkillDescriptionRegistry.register(
                CommonTechniqueSkillRegistry.STRENGTHEN_TECHNIQUE_ID,
                (player, techniqueId, skill) -> {
                    StrengthenTechniqueLevel level = PlayerStrengthenTechniqueData.getLevel(player);
                    return new Object[] {
                            TechniqueSkillStatFormat.formatNumber(level.getActiveHealthBonusMultiplier() * 100.0D),
                            TechniqueSkillStatFormat.formatNumber(StrengthenTechniqueBonuses.ACTIVE_SPEED_BONUS * 100.0D),
                            TechniqueSkillStatFormat.formatNumber(StrengthenTechniqueBonuses.ACTIVE_ATTACK_BONUS * 100.0D),
                            TechniqueSkillStatFormat.formatNumber(StrengthenTechniqueBonuses.resolveJumpHeightBlocks(level)),
                            TechniqueSkillStatFormat.formatNumber(StrengthenTechniqueServerLogic.MAINTAIN_COST_PER_SECOND),
                            Integer.toString(StrengthenTechniqueServerLogic.getMaintainCostPerSecond(player))
                    };
                });
    }

    private static void registerAerialSkills()
    {
        TechniqueSkillDescriptionRegistry.register(
                AerialCombatSkills.SKILL_CURSE_LEVITATION,
                (player, techniqueId, skill) -> new Object[0]);
    }
}
