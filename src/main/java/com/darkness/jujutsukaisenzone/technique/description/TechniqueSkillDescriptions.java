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
import com.darkness.jujutsukaisenzone.technique.SkillDamageResolver;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessBlueSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessPurpleSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessRedSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineSkillGrowth;
import com.darkness.jujutsukaisenzone.player.JudgmentTrialTargetData;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentWeaponLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TenShadowsTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skill.RatioSpotLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioIchimonjiLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioImpactLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioCollapseLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioAbsolutionLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioOvertimeLogic;
import com.darkness.jujutsukaisenzone.player.PlayerRatioOvertimeData;
import com.darkness.jujutsukaisenzone.player.PlayerRatioBreakData;
import com.darkness.jujutsukaisenzone.technique.skill.StarFlightSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkDetonation;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkRules;
import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.darkness.jujutsukaisenzone.technique.commonskill.CommonTechniqueSkillRegistry;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkills;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.StrengthenTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.player.PlayerStrengthenTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.StrengthenTechniqueBonuses;
import com.darkness.jujutsukaisenzone.player.StrengthenTechniqueLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;

/**
 * 术式技能描述入口：注册各技能数值解析，并组装完整描述视图。
 */
public final class TechniqueSkillDescriptions
{
    /** 描述变体后缀：未解锁「量刑」——裁决里根本没有【死刑】这一档。 */
    private static final String SUFFIX_NO_SENTENCING = "no_sentencing";
    /** 描述变体后缀：已解锁「量刑」但未解锁「从重论处」——有【死刑】，但重罪与 debuff 加成一律不计。 */
    private static final String SUFFIX_NO_AGGRAVATION = "no_aggravation";

    private TechniqueSkillDescriptions()
    {
    }

    public static void bootstrap()
    {
        registerLimitlessSkills();
        registerShrineSkills();
        registerJudgmentSkills();
        registerLoveRendezvousSkills();
        registerRatioSkills();
        registerTenShadowsSkills();
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
        Component description = buildDescription(player, techniqueId, skill);
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

    /**
     * 描述正文：优先用「按解锁状态切换的变体」（键与参数都由变体一次给全），没有变体时回退默认键
     * + 已注册的参数解析器。
     */
    private static Component buildDescription(Player player, String techniqueId, TechniqueSkill skill)
    {
        TechniqueSkillDescriptionVariant variant = resolveVariant(player, techniqueId, skill);
        if (variant != null)
        {
            return Component.translatable(variant.descriptionKey(), variant.descriptionArgs());
        }
        return Component.translatable(
                TechniqueSkillDescriptionKeys.description(skill.skillId()),
                resolveDescriptionArgs(player, techniqueId, skill));
    }

    private static TechniqueSkillDescriptionVariant resolveVariant(Player player, String techniqueId, TechniqueSkill skill)
    {
        TechniqueSkillDescriptionVariantResolver resolver =
                TechniqueSkillDescriptionRegistry.getVariant(skill.skillId());
        if (resolver == null)
        {
            return null;
        }
        TechniqueSkillDescriptionVariant variant = resolver.resolve(player, techniqueId, skill);
        if (variant == null || variant.descriptionKey() == null || variant.descriptionKey().isBlank())
        {
            return null;
        }
        return variant;
    }

    /**
     * 简洁页正文键：变体可以指定另一份文案，否则沿用默认的 {@code .brief}。
     * <p>
     * 简洁页同样要跟着解锁状态走——它是悬停框的默认视图，只改完整页的话，玩家看到的仍是
     * 那句「再受【死刑】或【没收】」。
     */
    public static String resolveBriefKey(Player player, String techniqueId, TechniqueSkill skill)
    {
        if (skill == null)
        {
            return null;
        }
        TechniqueSkillDescriptionVariant variant = resolveVariant(player, techniqueId, skill);
        if (variant != null && variant.briefKey() != null && !variant.briefKey().isBlank())
        {
            return variant.briefKey();
        }
        return TechniqueSkillDescriptionKeys.brief(skill.skillId());
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
                        SkillDamageResolver.ratioText(LimitlessBlueSkillGrowth.damage(player)),
                        TechniqueSkillStatFormat.formatNumber(LimitlessBlueSkillGrowth.pullRadius(player))
                });

        TechniqueSkillDescriptionRegistry.register(
                LimitlessTechniqueSkillSet.SKILL_RED,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(LimitlessRedSkillGrowth.explosionPower(player)),
                        SkillDamageResolver.ratioText(LimitlessRedConfig.BLOCK_LAUNCH_HURT_DAMAGE_PER_DISTANCE)
                });

        TechniqueSkillDescriptionRegistry.register(
                LimitlessTechniqueSkillSet.SKILL_PURPLE,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(LimitlessPurpleConfig.ARROW_BLOCK_ERASE_RADIUS),
                        SkillDamageResolver.ratioText(LimitlessPurpleConfig.ARROW_SPHERE_DAMAGE_PER_TICK)
                });

        TechniqueSkillDescriptionRegistry.register(
                LimitlessTechniqueSkillSet.SKILL_REMOTE_PURPLE,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatSecondsFromTicks(
                                LimitlessRemoteMurasakiConfig.HOLD_RELEASE_CHARGE_TICKS),
                        TechniqueSkillStatFormat.formatNumber(LimitlessRemoteMurasakiConfig.ARROW_BLOCK_ERASE_RADIUS),
                        SkillDamageResolver.ratioText(LimitlessRemoteMurasakiConfig.ARROW_SPHERE_DAMAGE_PER_TICK),
                        Integer.toString(SkillCombatConfigRegistry.resolveInt(
                                SkillCombatConfigRegistry.SKILL_LIMITLESS_REMOTE_PURPLE,
                                "max_flight_chunks",
                                player))
                });

        /*
         * 机制型：「0.2秒的领域展开」——不占槽位；解锁后可用通用圆盘切换领域瞬发模式。
         * 描述无动态数值占位。
         */
        TechniqueSkillDescriptionRegistry.register(
                LimitlessTechniqueSkillSet.SKILL_INSTANT_DOMAIN,
                (player, techniqueId, skill) -> new Object[0]);
    }

    private static void registerShrineSkills()
    {
        /*
         * ⛔ 伤害文案一律走 *SkillGrowth（配置解析后的真值），不要读 *Config 上的常量：
         * 常量只是 Forge 配置项的默认值，玩家改了 skillCombat 之后文案会与实际伤害对不上。
         * 几何 / 时长一类不在配置里的参数才直接读常量。
         */
        TechniqueSkillDescriptionRegistry.register(
                ShrineTechniqueSkillSet.SKILL_KAI,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(ShrineKaiConfig.COLUMN_RADIUS),
                        SkillDamageResolver.ratioText(ShrineSkillGrowth.kaiColumnDamage(player))
                });

        TechniqueSkillDescriptionRegistry.register(
                ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(ShrineKaiConfig.FLYOBJ_MAX_RANGE),
                        TechniqueSkillStatFormat.formatNumber(ShrineKaiConfig.COLUMN_RADIUS),
                        SkillDamageResolver.ratioText(ShrineSkillGrowth.kaiFlyobjColumnDamage(player))
                });

        TechniqueSkillDescriptionRegistry.register(
                ShrineTechniqueSkillSet.SKILL_HACHI,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(2.0D),
                        Integer.toString(ShrineHachiConfig.DAMAGE_INTERVAL),
                        SkillDamageResolver.ratioText(ShrineSkillGrowth.hachiDamage(player))
                });

        TechniqueSkillDescriptionRegistry.register(
                ShrineTechniqueSkillSet.SKILL_FUGA,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatSecondsFromTicks(ShrineFugaConfig.CHARGE_TICKS),
                        TechniqueSkillStatFormat.formatNumber(ShrineFugaConfig.BOOM_RADIUS),
                        TechniqueSkillStatFormat.formatSecondsFromTicks(ShrineFugaConfig.BOOM_LIFETIME_TICKS),
                        SkillDamageResolver.ratioText(ShrineFugaConfig.BOOM_DAMAGE_PER_TICK)
                });
    }

    private static void registerJudgmentSkills()
    {
        /*
         * 三个主动技能的参数解析器先留成变量：下面的「按解锁状态换文案」解析器要复用它们
         * （变体只是取前 N 个参数），不另写一份，免得两边数值走偏。
         */
        TechniqueSkillDescriptionArgsResolver gavelArgs = (player, techniqueId, skill) -> new Object[] {
                SkillDamageResolver.ratioText(GavelProjectileEntity.BASE_DAMAGE),
                TechniqueSkillStatFormat.formatNumber(GavelProjectileEntity.IMPACT_EXPLOSION_POWER),
                TechniqueSkillStatFormat.formatNumber(
                        (JudgmentWeaponLogic.SINFUL_ONE_DAMAGE_MULTIPLIER - 1.0F) * 100.0F)
        };

        TechniqueSkillDescriptionArgsResolver trialArgs = (player, techniqueId, skill) -> new Object[] {
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
        };

        TechniqueSkillDescriptionArgsResolver accuseArgs = (player, techniqueId, skill) -> new Object[] {
                TechniqueSkillStatFormat.formatSecondsFromTicks(JudgmentAccuseSkillLogic.CHOICE_WINDOW_TICKS),
                TechniqueSkillStatFormat.formatNumber(JudgmentAccuseSkillLogic.EVIDENCE_CONFIRMED_CHANCE_PERCENT),
                TechniqueSkillStatFormat.formatNumber(JudgmentTrialTargetData.GUILTY_BONUS_PER_TRIAL_PERCENT),
                TechniqueSkillStatFormat.formatNumber(JudgmentTrialTargetData.MAX_GUILTY_BONUS_PERCENT),
                TechniqueSkillStatFormat.formatNumber(JudgmentTrialSkillLogic.DEATH_SENTENCE_CHANCE_PERCENT),
                TechniqueSkillStatFormat.formatNumber(
                        JudgmentTrialSkillLogic.DEATH_SENTENCE_BONUS_PER_DEBUFF_PERCENT),
                TechniqueSkillStatFormat.formatNumber(
                        JudgmentTrialSkillLogic.MAX_DEATH_SENTENCE_DEBUFF_BONUS_PERCENT)
        };

        TechniqueSkillDescriptionRegistry.register(JudgmentTechniqueSkillSet.SKILL_GAVEL, gavelArgs);
        TechniqueSkillDescriptionRegistry.register(JudgmentTechniqueSkillSet.SKILL_TRIAL, trialArgs);
        TechniqueSkillDescriptionRegistry.register(JudgmentTechniqueSkillSet.SKILL_ACCUSE, accuseArgs);

        registerJudgmentDescriptionVariants(gavelArgs, trialArgs, accuseArgs);

        /* 机制型技能：文案里没有数值，但同样注册——保持「每个技能都有注册项」这一条不出现例外。 */
        TechniqueSkillDescriptionRegistry.register(
                JudgmentTechniqueSkillSet.SKILL_GAVEL_FORMS,
                (player, techniqueId, skill) -> new Object[0]);

        TechniqueSkillDescriptionRegistry.register(
                JudgmentTechniqueSkillSet.SKILL_SENTENCING,
                (player, techniqueId, skill) -> new Object[] {
                        /* 顺序必须与 lang 里 %s 的出现次序一致：死刑基础概率 */
                        TechniqueSkillStatFormat.formatNumber(JudgmentTrialSkillLogic.DEATH_SENTENCE_CHANCE_PERCENT)
                });

        TechniqueSkillDescriptionRegistry.register(
                JudgmentTechniqueSkillSet.SKILL_AGGRAVATION,
                (player, techniqueId, skill) -> new Object[] {
                        /* 顺序必须与 lang 里 %s 的出现次序一致：每个 debuff 的加成 → 加成上限 */
                        TechniqueSkillStatFormat.formatNumber(
                                JudgmentTrialSkillLogic.DEATH_SENTENCE_BONUS_PER_DEBUFF_PERCENT),
                        TechniqueSkillStatFormat.formatNumber(
                                JudgmentTrialSkillLogic.MAX_DEATH_SENTENCE_DEBUFF_BONUS_PERCENT)
                });
    }

    /**
     * 审判术式三个主动技能的描述随「量刑」/「从重论处」的解锁状态换文案。
     * <p>
     * 门控只有一处（{@code JudgmentTrialSkillLogic#rollGuiltyVerdictType}），但受它影响的是
     * 三条路径（审判 / 指控 / 领域审理）外加【死刑】的下游产物（法槌的【处刑人之剑】）。
     * 若描述一律照满配形态写，玩家在解锁前读到的就是一段「现在根本不会发生」的规则；
     * 反过来若只在文末补一句「需解锁」，那一大段概率数字仍然摆在眼前，照样看不懂。
     * 所以这里直接换键：未解锁的内容从正文里消失，只留一句「解锁 X 后会怎样」。
     * <p>
     * <b>每个后缀都必须在 zh_cn 与 en_us 两侧各写一条 .desc_<suffix></b>（简洁页要换的再写
     * .brief_<suffix>），否则界面会直接把键名显示出来。
     */
    private static void registerJudgmentDescriptionVariants(
            TechniqueSkillDescriptionArgsResolver gavelArgs,
            TechniqueSkillDescriptionArgsResolver trialArgs,
            TechniqueSkillDescriptionArgsResolver accuseArgs)
    {
        /* 法槌：只有「有没有死刑」这一件事会变——【处刑人之剑】是死刑的下游产物，与「从重论处」无关。 */
        TechniqueSkillDescriptionRegistry.registerVariant(
                JudgmentTechniqueSkillSet.SKILL_GAVEL,
                (player, techniqueId, skill) ->
                {
                    if (isJudgmentMechanicUnlocked(player, JudgmentTechniqueSkillSet.SKILL_SENTENCING))
                    {
                        return null;
                    }
                    return TechniqueSkillDescriptionVariant.of(
                            TechniqueSkillDescriptionKeys.description(
                                    JudgmentTechniqueSkillSet.SKILL_GAVEL, SUFFIX_NO_SENTENCING),
                            TechniqueSkillDescriptionKeys.brief(
                                    JudgmentTechniqueSkillSet.SKILL_GAVEL, SUFFIX_NO_SENTENCING),
                            gavelArgs.resolve(player, techniqueId, skill));
                });

        /*
         * 审判：三档。lang 里 .desc_no_sentencing 只用到前 4 个 %s（到有罪叠层上限为止），
         * .desc_no_aggravation 用到前 6 个（再到「附加了没收的死刑」为止），debuff 两项加成被整段删掉。
         */
        TechniqueSkillDescriptionRegistry.registerVariant(
                JudgmentTechniqueSkillSet.SKILL_TRIAL,
                (player, techniqueId, skill) ->
                {
                    String suffix = sentencingVariantSuffix(player);
                    if (suffix.isEmpty())
                    {
                        return null;
                    }
                    /*
                     * 简洁页不换：审判的 .brief 只讲「传唤与宣判」，不含【死刑】/【没收】
                     * 这些会被门控的内容，未解锁时也照样成立，因此不注册 .brief_no_*。
                     */
                    return TechniqueSkillDescriptionVariant.of(
                            TechniqueSkillDescriptionKeys.description(
                                    JudgmentTechniqueSkillSet.SKILL_TRIAL, suffix),
                            null,
                            Arrays.copyOf(
                                    trialArgs.resolve(player, techniqueId, skill),
                                    SUFFIX_NO_SENTENCING.equals(suffix) ? 4 : 6));
                });

        /* 指控：同样三档；.desc_no_sentencing 用前 4 个 %s，.desc_no_aggravation 用前 5 个。 */
        TechniqueSkillDescriptionRegistry.registerVariant(
                JudgmentTechniqueSkillSet.SKILL_ACCUSE,
                (player, techniqueId, skill) ->
                {
                    String suffix = sentencingVariantSuffix(player);
                    if (suffix.isEmpty())
                    {
                        return null;
                    }
                    return TechniqueSkillDescriptionVariant.of(
                            TechniqueSkillDescriptionKeys.description(
                                    JudgmentTechniqueSkillSet.SKILL_ACCUSE, suffix),
                            null,
                            Arrays.copyOf(
                                    accuseArgs.resolve(player, techniqueId, skill),
                                    SUFFIX_NO_SENTENCING.equals(suffix) ? 4 : 5));
                });
    }

    /**
     * 「量刑」/「从重论处」的解锁状态后缀：""（都解锁）→ {@link #SUFFIX_NO_AGGRAVATION} →
     * {@link #SUFFIX_NO_SENTENCING}。顺序即学习顺序，与开发树的前置链一致。
     */
    private static String sentencingVariantSuffix(Player player)
    {
        if (!isJudgmentMechanicUnlocked(player, JudgmentTechniqueSkillSet.SKILL_SENTENCING))
        {
            return SUFFIX_NO_SENTENCING;
        }
        if (!isJudgmentMechanicUnlocked(player, JudgmentTechniqueSkillSet.SKILL_AGGRAVATION))
        {
            return SUFFIX_NO_AGGRAVATION;
        }
        return "";
    }

    private static boolean isJudgmentMechanicUnlocked(Player player, String skillId)
    {
        return SkillUnlockLogic.isSkillUnlockedById(
                player, PlayerTechniqueData.TECHNIQUE_JUDGMENT, skillId);
    }


    private static void registerRatioSkills()
    {
        TechniqueSkillDescriptionRegistry.register(
                RatioTechniqueSkillSet.SKILL_RATIO_MARK,
                (player, techniqueId, skill) -> new Object[] {
                        Integer.toString(RatioSpotLogic.SPOT_DURATION_TICKS / 20),
                        TechniqueSkillStatFormat.formatNumber(RatioSpotLogic.FORCED_CRIT_MULT),
                        Integer.toString(PlayerRatioBreakData.MAX_BREAKS)
                });
        TechniqueSkillDescriptionRegistry.register(
                RatioTechniqueSkillSet.SKILL_ICHIMONJI,
                (player, techniqueId, skill) -> new Object[] {
                        Integer.toString(RatioIchimonjiLogic.BASE_HITS_BLUNT),
                        Integer.toString(RatioIchimonjiLogic.BASE_HITS_SHARP),
                        Integer.toString(RatioIchimonjiLogic.MAX_BREAKS_CONSUMED),
                        SkillDamageResolver.martialRatioText(RatioIchimonjiLogic.DAMAGE_PER_HIT),
                        TechniqueSkillStatFormat.formatNumber(RatioIchimonjiLogic.RANGE)
                });
        TechniqueSkillDescriptionRegistry.register(
                RatioTechniqueSkillSet.SKILL_IMPACT,
                (player, techniqueId, skill) -> new Object[] {
                        TechniqueSkillStatFormat.formatNumber(RatioImpactLogic.RANGE),
                        SkillDamageResolver.martialRatioText(RatioImpactLogic.DAMAGE),
                        Integer.toString(RatioImpactLogic.STUN_TICKS / 20),
                        TechniqueSkillStatFormat.formatNumber(
                                RatioImpactLogic.FOLLOWUP_WINDOW_TICKS / 20.0D)
                });
        TechniqueSkillDescriptionRegistry.register(
                RatioTechniqueSkillSet.SKILL_COLLAPSE,
                (player, techniqueId, skill) -> new Object[] {
                        Integer.toString(RatioCollapseLogic.BASE_DEBRIS),
                        Integer.toString(RatioCollapseLogic.MAX_BREAKS_CONSUMED),
                        TechniqueSkillStatFormat.formatNumber(RatioCollapseLogic.BREAK_RADIUS),
                        SkillDamageResolver.ratioText(RatioCollapseLogic.HURT_DAMAGE)
                });
        TechniqueSkillDescriptionRegistry.register(
                RatioTechniqueSkillSet.SKILL_OVERTIME_VOW,
                (player, techniqueId, skill) -> new Object[] {
                        Integer.toString(Math.round((1.0F - RatioOvertimeLogic.WORK_DAMAGE_MULT) * 100.0F)),
                        Integer.toString(Math.round((RatioOvertimeLogic.OVERTIME_DAMAGE_MULT - 1.0F) * 100.0F))
                });
        TechniqueSkillDescriptionRegistry.register(
                RatioTechniqueSkillSet.SKILL_OVERTIME,
                (player, techniqueId, skill) -> new Object[] {});
        TechniqueSkillDescriptionRegistry.register(
                RatioTechniqueSkillSet.SKILL_ABSOLUTION,
                (player, techniqueId, skill) -> new Object[] {
                        SkillDamageResolver.martialRatioText(RatioAbsolutionLogic.DAMAGE),
                        TechniqueSkillStatFormat.formatNumber(RatioAbsolutionLogic.RANGE),
                        Integer.toString(PlayerRatioOvertimeData.EFFICIENCY_DURATION_TICKS / 20),
                        Integer.toString(5),
                        Integer.toString(2),
                        TechniqueSkillStatFormat.formatNumber(PlayerRatioOvertimeData.EFFICIENCY_BONUS_CAP)
                });
    }

    private static void registerTenShadowsSkills()
    {
        TechniqueSkillDescriptionRegistry.register(
                TenShadowsTechniqueSkillSet.SKILL_HAND_SHADOW,
                (player, techniqueId, skill) -> new Object[] {
                        Integer.toString(TenShadowsTechniqueSkillSet.HAND_SHADOW_INVIS_TICKS / 20)
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
                        SkillDamageResolver.ratioText(StarMarkDetonation.DETONATION_DAMAGE)
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
                        /* 顺序必须与 lang 里 %s 的出现次序一致：射程 → 冲撞伤害 → 引爆单体伤害 → 路径伤害
                           （「依次穿越」的加成数值已随折线一并搬到机制型技能「在星间穿梭」的描述里） */
                        TechniqueSkillStatFormat.formatNumber(StarFlightSkillLogic.CAST_RANGE),
                        SkillDamageResolver.martialRatioText(StarFlightSkillLogic.RAM_DAMAGE),
                        SkillDamageResolver.ratioText(StarMarkDetonation.DETONATION_DAMAGE),
                        SkillDamageResolver.martialRatioText(StarFlightSkillLogic.PATH_DAMAGE)
                });
        /*
         * 机制型技能「在星间穿梭」：与「引力弹弓」同一形态——不占槽位、没有咒力与冷却，
         * tooltip 只渲染名字与这段描述（见 {@code TechniqueSkillTooltipHelper} 对机制型技能的特判），
         * 所以这里的 %s 就是描述里要用到的全部数值。
         */
        TechniqueSkillDescriptionRegistry.register(
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_CHAIN,
                (player, techniqueId, skill) -> new Object[] {
                        /* 顺序必须与 lang 里 %s 的出现次序一致：每颗加成 → 加成上限 */
                        TechniqueSkillStatFormat.formatNumber(StarFlightSkillLogic.PASS_BONUS_PER_STAR * 100.0D),
                        TechniqueSkillStatFormat.formatNumber(StarFlightSkillLogic.PASS_BONUS_CAP * 100.0D)
                });
        /*
         * 机制型技能「引力弹弓」：不占槽位、没有咒力与冷却，tooltip 只渲染名字与这段描述
         * （见 {@code TechniqueSkillTooltipHelper} 对机制型技能的特判），所以这里的 %s 就是描述里
         * 要用到的全部数值。数值一律从 StarMarkRules 的常量取，调参时描述跟着走，不会对不上。
         */
        TechniqueSkillDescriptionRegistry.register(
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_SLING,
                (player, techniqueId, skill) -> new Object[] {
                        /* 顺序必须与 lang 里 %s 的出现次序一致：影响半径 → 加速量 → 速度上限 → 借力次数 */
                        TechniqueSkillStatFormat.formatNumber(StarMarkRules.SLING_RANGE),
                        TechniqueSkillStatFormat.formatNumber(StarMarkRules.SLING_BOOST),
                        TechniqueSkillStatFormat.formatNumber(StarMarkRules.MAX_BALL_SPEED),
                        Integer.toString(StarMarkRules.SLING_MAX_PER_BALL)
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
