package com.darkness.jujutsukaisenzone;

import com.darkness.jujutsukaisenzone.entity.data.LimitlessBlueConfig;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRedConfig;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRemoteMurasakiConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineHachiConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineDomainSkillGrowth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // ======== 黑闪 (Black Flash) 配置 ========

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_BASE_CHANCE = BUILDER
            .comment("黑闪基础触发概率 (0.0 ~ 1.0)")
            .defineInRange("blackFlashBaseChance", 0.01D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_COMBO_BONUS_PER_HIT = BUILDER
            .comment("黑闪每连击数概率加成")
            .defineInRange("blackFlashComboBonusPerHit", 0.003D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_KILL_BONUS = BUILDER
            .comment("黑闪近期击杀概率加成")
            .defineInRange("blackFlashKillBonus", 0.025D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_LOW_HP_THRESHOLD = BUILDER
            .comment("黑闪残血判定阈值 (HP 比例，0.0 ~ 1.0)")
            .defineInRange("blackFlashLowHpThreshold", 0.30D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_LOW_HP_BONUS = BUILDER
            .comment("黑闪残血概率加成")
            .defineInRange("blackFlashLowHpBonus", 0.015D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_DODGE_BONUS = BUILDER
            .comment("黑闪闪避概率加成")
            .defineInRange("blackFlashDodgeBonus", 0.03D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_TARGET_LOW_HP_THRESHOLD = BUILDER
            .comment("黑闪攻击目标残血判定阈值 (HP 比例，0.0 ~ 1.0)")
            .defineInRange("blackFlashTargetLowHpThreshold", 0.10D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_TARGET_LOW_HP_BONUS = BUILDER
            .comment("黑闪攻击目标残血概率加成")
            .defineInRange("blackFlashTargetLowHpBonus", 0.004D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_DAMAGE_MULTIPLIER = BUILDER
            .comment("黑闪触发时的伤害指数（2.5 = 伤害的 2.5 次方）")
            .defineInRange("blackFlashDamageMultiplier", 2.5D, 1.0D, 100.0D);

    private static final ForgeConfigSpec.IntValue BLACK_FLASH_COMBO_WINDOW_TICKS = BUILDER
            .comment("黑闪连击窗口（tick），超过此时间未命中则连击中断")
            .defineInRange("blackFlashComboWindowTicks", 30, 1, 1200);

    private static final ForgeConfigSpec.IntValue BLACK_FLASH_KILL_WINDOW_TICKS = BUILDER
            .comment("黑闪击杀窗口（tick），击杀后在此时间内提供加成")
            .defineInRange("blackFlashKillWindowTicks", 200, 1, 1200);

    private static final ForgeConfigSpec.IntValue BLACK_FLASH_DODGE_WINDOW_TICKS = BUILDER
            .comment("黑闪闪避窗口（tick），闪避后在此时间内提供加成")
            .defineInRange("blackFlashDodgeWindowTicks", 60, 1, 1200);

    // ======== 黑闪触发冷却 ========

    private static final ForgeConfigSpec.IntValue BLACK_FLASH_TRIGGER_COOLDOWN_TICKS = BUILDER
            .comment("黑闪触发后的冷却窗口（tick），在此期间无法再次触发黑闪")
            .defineInRange("blackFlashTriggerCooldownTicks", 40, 0, 600);

    // ======== 黑闪 ZONE 状态配置 ========

    private static final ForgeConfigSpec.IntValue BLACK_FLASH_ZONE_MIN_COMBO = BUILDER
            .comment("进入 ZONE 状态所需的最低连击数")
            .defineInRange("blackFlashZoneMinCombo", 3, 1, 20);

    private static final ForgeConfigSpec.IntValue BLACK_FLASH_ZONE_DURATION_TICKS = BUILDER
            .comment("ZONE 状态持续时间（tick）")
            .defineInRange("blackFlashZoneDurationTicks", 600, 1, 1200);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_ZONE_BONUS_I = BUILDER
            .comment("ZONE I 级黑闪概率加成")
            .defineInRange("blackFlashZoneBonusI", 0.015D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_ZONE_BONUS_II = BUILDER
            .comment("ZONE II 级黑闪概率加成")
            .defineInRange("blackFlashZoneBonusII", 0.03D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_ZONE_BONUS_III = BUILDER
            .comment("ZONE III 级黑闪概率加成")
            .defineInRange("blackFlashZoneBonusIII", 0.045D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_ZONE_ATTACK_BONUS = BUILDER
            .comment("ZONE 状态攻击伤害加成（不随等级变化，0.2 = 20%）")
            .defineInRange("blackFlashZoneAttackBonus", 0.20D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue BLACK_FLASH_ZONE_DEFENSE_BONUS = BUILDER
            .comment("ZONE 状态伤害减免（不随等级变化，0.15 = 15%）")
            .defineInRange("blackFlashZoneDefenseBonus", 0.15D, 0.0D, 1.0D);

    // ======== 击杀获取咒术经验配置 ========

    private static final ForgeConfigSpec.IntValue KILL_EXP_RADIUS = BUILDER
            .comment("击杀经验分配半径（格），同阵营玩家在此范围内可分得经验")
            .defineInRange("killExpRadius", 32, 1, 128);

    private static final ForgeConfigSpec.DoubleValue KILL_EXP_LAST_HIT_RATIO = BUILDER
            .comment("最后一击者获得的经验比例（0.6 = 60%）")
            .defineInRange("killExpLastHitRatio", 0.6D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue KILL_EXP_NEARBY_RATIO = BUILDER
            .comment("周围同阵营玩家分配的经验比例（0.4 = 40%）")
            .defineInRange("killExpNearbyRatio", 0.4D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.IntValue KILL_EXP_MAX_FROM_MOB = BUILDER
            .comment("击杀原版生物获得的经验上限")
            .defineInRange("killExpMaxFromMob", 20, 1, 100);

    // ======== 战斗强度面板（体术强度 / 术式强度） ========

    private static final ForgeConfigSpec.BooleanValue COMBAT_POWER_SCALING_ENABLED = BUILDER
            .comment("是否启用「体术强度 / 术式强度」面板换算伤害（关闭则回到旧的固定点数结算）")
            .define("combatPower.scalingEnabled", true);

    private static final ForgeConfigSpec.DoubleValue COMBAT_POWER_BASE = BUILDER
            .comment("战斗强度：无咒术等级时的基础值")
            .defineInRange("combatPower.base", 10.0D, 0.0D, 100000.0D);

    private static final ForgeConfigSpec.DoubleValue COMBAT_POWER_PER_RANK = BUILDER
            .comment("战斗强度：每升一级咒术等级的增量（四级 = base + perRank）")
            .defineInRange("combatPower.perRank", 10.0D, 0.0D, 100000.0D);

    private static final ForgeConfigSpec.DoubleValue COMBAT_POWER_TECHNIQUE_BASELINE = BUILDER
            .comment("术式参考强度：无面板/关闭强度缩放时，技能倍率×本值=绝对伤；"
                    + "近战以外的部分公式（如蟾蜍加成）也用它当参考。"
                    + "默认 20 约等于旧「四级」强度锚点。技能出伤本身是 强度×倍率，不再除以本值。")
            .defineInRange("combatPower.techniqueBaseline", 20.0D, 0.001D, 100000.0D);

    private static final ForgeConfigSpec.DoubleValue COMBAT_POWER_PHYSICAL_BASELINE = BUILDER
            .comment("体术参考强度：① 技能体术倍率在无面板时 ×本值；"
                    + "② EF/原版近战绝对伤缩放分母（绝对伤×体术强度/本值）。"
                    + "与 techniqueBaseline 取同一值时，参考强度下的手感与改造前对齐。")
            .defineInRange("combatPower.physicalBaseline", 20.0D, 0.001D, 100000.0D);

    private static final ForgeConfigSpec.DoubleValue COMBAT_POWER_HEAVENLY_MULTIPLIER = BUILDER
            .comment("天与咒缚：体术强度放大倍数（术式强度恒为 0）")
            .defineInRange("combatPower.heavenlyPhysicalMultiplier", 3.5D, 1.0D, 1000.0D);

    private static final ForgeConfigSpec.DoubleValue COMBAT_POWER_REVERSE_TECHNIQUE_BONUS = BUILDER
            .comment("习得反转术式后的术式强度加成")
            .defineInRange("combatPower.reverseTechniqueBonus", 10.0D, 0.0D, 100000.0D);

    private static final ForgeConfigSpec.DoubleValue COMBAT_POWER_INTEL_PER_LEVEL = BUILDER
            .comment("每点术式情报提供的术式强度（线性段；过软顶后见 intelSoftScale）")
            .defineInRange("combatPower.intelPerLevel", 0.5D, 0.0D, 100000.0D);

    private static final ForgeConfigSpec.IntValue COMBAT_POWER_INTEL_SOFT_CAP = BUILDER
            .comment("术式情报软顶：加情报前的术式强度达到本值后，继续用情报堆强度改为对数衰减。0=关闭软顶")
            .defineInRange("combatPower.intelSoftCap", 2000, 0, 1000000);

    private static final ForgeConfigSpec.DoubleValue COMBAT_POWER_INTEL_SOFT_SCALE = BUILDER
            .comment("术式情报软顶曲率 S：过顶后 excessPower = rate*S*ln(1+excessIntel/S)；S越大越钝")
            .defineInRange("combatPower.intelSoftScale", 200.0D, 0.001D, 1000000.0D);

    // ======== SE 术式侵入（统一质量池） ========

    private static final ForgeConfigSpec.IntValue SE_QUALITY_ALLOWANCE = BUILDER
            .comment("质量宽限：占用超出上限后还允许多占多少。宽限内的超出不吃超载罚，超过宽限则拒绝装载")
            .defineInRange("se.qualityAllowance", 2, 0, 64);

    private static final ForgeConfigSpec.IntValue SE_OVERLOAD_POWER_PENALTY_PER_POINT = BUILDER
            .comment("质量超载罚：每超出上限 1 点质量，体术强度与术式强度各扣多少点")
            .defineInRange("se.overloadPowerPenaltyPerPoint", 2, 0, 1000);

    // ======== 防御面板（护甲 / 体术抗性 / 术式抗性） ========

    private static final ForgeConfigSpec.BooleanValue COMBAT_DEFENSE_ENABLED = BUILDER
            .comment("是否启用防御面板（护甲补算 + 体术 / 术式抗性）。关闭则回到「术式无视护甲、无抗性」的旧行为")
            .define("combatDefense.enabled", true);

    private static final ForgeConfigSpec.DoubleValue COMBAT_DEFENSE_ARMOR_FACTOR = BUILDER
            .comment("每点护甲提供的术式减伤（百分比点数）。")
            .defineInRange("combatDefense.armorFactorPerPoint", 2.0D, 0.0D, 100.0D);

    private static final ForgeConfigSpec.DoubleValue COMBAT_DEFENSE_ARMOR_CAP = BUILDER
            .comment("护甲层对术式减伤的封顶（百分比）。")
            .defineInRange("combatDefense.armorMaxReduction", 60.0D, 0.0D, 100.0D);

    private static final ForgeConfigSpec.DoubleValue COMBAT_DEFENSE_PHYSICAL_RESIST_CAP = BUILDER
            .comment("体术抗性的生效封顶（百分比）。")
            .defineInRange("combatDefense.physicalResistanceCap", 70.0D, 0.0D, 100.0D);

    private static final ForgeConfigSpec.DoubleValue COMBAT_DEFENSE_TECHNIQUE_RESIST_CAP = BUILDER
            .comment("术式抗性的生效封顶（百分比）。")
            .defineInRange("combatDefense.techniqueResistanceCap", 70.0D, 0.0D, 100.0D);

    private static final ForgeConfigSpec.DoubleValue COMBAT_DEFENSE_TOTAL_CAP = BUILDER
            .comment("护甲 + 抗性合起来的减伤封顶（百分比）。")
            .defineInRange("combatDefense.defenseReductionCap", 85.0D, 0.0D, 100.0D);

    private static final ForgeConfigSpec.BooleanValue COMBAT_DEFENSE_PENETRATION_ENABLED = BUILDER
            .comment("Enable physical/technique penetration. Off => pen attributes read as 0.")
            .define("combatDefense.penetrationEnabled", true);

    private static final ForgeConfigSpec.BooleanValue COMBAT_DEFENSE_PENETRATION_CAN_OVERPEN = BUILDER
            .comment("If false, penetration floors positive mitigation at 0 (no overpen into vulnerability).")
            .define("combatDefense.penetrationCanOverpen", false);

    // ======== 伤害浮动 ========

    private static final ForgeConfigSpec.BooleanValue DAMAGE_VARIANCE_ENABLED = BUILDER
            .comment("是否启用伤害浮动乘区（每次命中在 ±spread 内均匀随机，破除固定出伤）")
            .define("damageVariance.enabled", true);

    private static final ForgeConfigSpec.DoubleValue DAMAGE_VARIANCE_SPREAD = BUILDER
            .comment("伤害浮动幅度：0.10 = 每次命中在 90%~110% 之间随机")
            .defineInRange("damageVariance.spread", 0.10D, 0.0D, 1.0D);

    // ======== 领域脑损（第1期） ========
    private static final ForgeConfigSpec.BooleanValue DOMAIN_BRAIN_ENABLED = BUILDER
            .comment("Enable domain brain-damage soft limit (free daily expansions, then escalating damage).")
            .define("domainBrain.enabled", true);

    private static final ForgeConfigSpec.IntValue DOMAIN_BRAIN_MAX = BUILDER
            .comment("Max brain damage points.")
            .defineInRange("domainBrain.max", 100, 1, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_BRAIN_FREE_DAILY = BUILDER
            .comment("Daily free domain expansions that do not add brain damage.")
            .defineInRange("domainBrain.freeDailyExpansions", 3, 0, 100);

    private static final ForgeConfigSpec.IntValue DOMAIN_BRAIN_RECOVER_PER_DAY = BUILDER
            .comment("Brain damage recovered per Minecraft day passed.")
            .defineInRange("domainBrain.recoverPerDay", 20, 0, 1000);

    private static final ForgeConfigSpec.DoubleValue DOMAIN_BRAIN_FULL_ATTEMPT_DAMAGE = BUILDER
            .comment("Magic damage dealt when forcing domain at full brain damage.")
            .defineInRange("domainBrain.fullAttemptDamage", 6.0D, 0.0D, 1000.0D);

    private static final ForgeConfigSpec.IntValue DOMAIN_BRAIN_OVERAGE_1 = BUILDER
            .comment("Brain damage added on first expansion beyond free quota.")
            .defineInRange("domainBrain.overageGain1", 15, 0, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_BRAIN_OVERAGE_2 = BUILDER
            .comment("Brain damage added on second expansion beyond free quota.")
            .defineInRange("domainBrain.overageGain2", 25, 0, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_BRAIN_OVERAGE_3 = BUILDER
            .comment("Brain damage added on third expansion beyond free quota.")
            .defineInRange("domainBrain.overageGain3", 40, 0, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_BRAIN_OVERAGE_REST = BUILDER
            .comment("Brain damage added on further expansions beyond free quota.")
            .defineInRange("domainBrain.overageGainRest", 50, 0, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_BRAIN_FOOD_HEAL_MINOR = BUILDER
            .comment("Brain damage healed by soothing candy.")
            .defineInRange("domainBrain.foodHealMinor", 15, 1, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_BRAIN_FOOD_HEAL_MAJOR = BUILDER
            .comment("Brain damage healed by calming pill.")
            .defineInRange("domainBrain.foodHealMajor", 35, 1, 1000);

    // ======== 领域量表（第2期） ========
    private static final ForgeConfigSpec.BooleanValue DOMAIN_GAUGE_ENABLED = BUILDER
            .comment("Require a full domain gauge to expand. Charge via combat; brain damage reduces efficiency.")
            .define("domainGauge.enabled", true);

    private static final ForgeConfigSpec.IntValue DOMAIN_GAUGE_MAX = BUILDER
            .comment("Domain gauge maximum.")
            .defineInRange("domainGauge.max", 100, 1, 1000);

    private static final ForgeConfigSpec.DoubleValue DOMAIN_GAUGE_TECH_HIT_GAIN = BUILDER
            .comment("Base gauge gained per technique hit (before efficiency).")
            .defineInRange("domainGauge.techniqueHitGain", 3.0D, 0.0D, 1000.0D);

    private static final ForgeConfigSpec.IntValue DOMAIN_GAUGE_TECH_HIT_CD = BUILDER
            .comment("Cooldown ticks between technique-hit gauge charges.")
            .defineInRange("domainGauge.techniqueHitCooldownTicks", 10, 0, 200);

    private static final ForgeConfigSpec.DoubleValue DOMAIN_GAUGE_BLACK_FLASH_GAIN = BUILDER
            .comment("Base gauge gained on Black Flash.")
            .defineInRange("domainGauge.blackFlashGain", 20.0D, 0.0D, 1000.0D);

    private static final ForgeConfigSpec.DoubleValue DOMAIN_GAUGE_EFF_25 = BUILDER
            .comment("Charge efficiency when brain damage >= 25.")
            .defineInRange("domainGauge.efficiencyAt25", 0.85D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue DOMAIN_GAUGE_EFF_50 = BUILDER
            .comment("Charge efficiency when brain damage >= 50.")
            .defineInRange("domainGauge.efficiencyAt50", 0.65D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue DOMAIN_GAUGE_EFF_75 = BUILDER
            .comment("Charge efficiency when brain damage >= 75.")
            .defineInRange("domainGauge.efficiencyAt75", 0.40D, 0.0D, 1.0D);

    // ======== 修复术式熔断（第3期） ========
    private static final ForgeConfigSpec.BooleanValue DOMAIN_MELTDOWN_REPAIR_ENABLED = BUILDER
            .comment("Allow Shift+G (requires unlocked meltdown-repair common skill) to clear Technique Meltdown at the cost of cursed energy + brain damage.")
            .define("domainMeltdownRepair.enabled", true);

    private static final ForgeConfigSpec.IntValue DOMAIN_MELTDOWN_REPAIR_COST = BUILDER
            .comment("Cursed energy cost to repair meltdown.")
            .defineInRange("domainMeltdownRepair.cost", 300, 0, 100000);

    private static final ForgeConfigSpec.IntValue DOMAIN_MELTDOWN_REPAIR_GAIN_1 = BUILDER
            .comment("Brain damage from the 1st meltdown repair today.")
            .defineInRange("domainMeltdownRepair.gain1", 20, 0, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_MELTDOWN_REPAIR_GAIN_2 = BUILDER
            .comment("Brain damage from the 2nd meltdown repair today.")
            .defineInRange("domainMeltdownRepair.gain2", 30, 0, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_MELTDOWN_REPAIR_GAIN_3 = BUILDER
            .comment("Brain damage from the 3rd meltdown repair today.")
            .defineInRange("domainMeltdownRepair.gain3", 45, 0, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_MELTDOWN_REPAIR_GAIN_REST = BUILDER
            .comment("Brain damage from the 4th+ meltdown repair today.")
            .defineInRange("domainMeltdownRepair.gainRest", 55, 0, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_MELTDOWN_REPAIR_HIGH_BRAIN_THRESHOLD = BUILDER
            .comment("When current brain damage is at least this, add extra repair brain cost.")
            .defineInRange("domainMeltdownRepair.highBrainThreshold", 75, 0, 1000);

    private static final ForgeConfigSpec.IntValue DOMAIN_MELTDOWN_REPAIR_HIGH_BRAIN_EXTRA = BUILDER
            .comment("Extra brain damage when repairing while already at/above highBrainThreshold.")
            .defineInRange("domainMeltdownRepair.highBrainExtra", 10, 0, 1000);

    // ======== 技能战斗数值（玩家可在 config/jujutsukaisenzone-common.toml 修改） ========

    private static final ForgeConfigSpec.IntValue SKILL_COMBAT_BASELINE_GRADE = BUILDER
            .comment("技能数值成长基准咒术等级（1=四级，2=三级，依此类推）")
            .defineInRange("skillCombat.baselineGrade", 1, 1, 10);

    /*
     * ⛔ 这里的 default 一律引用 *Config / *SkillGrowth 上的常量，不要写数字字面量。
     * 同一批数值原本在「常量类」「本文件 default」「SkillCombatConfigRegistry#buildCodeDefaults」
     * 三处各写一遍，改一处不会报错也不会生效——现在只有常量类那一处手写，其余全是引用。
     * 新增 skillCombat 项时同样先加常量、再回来引用。
     */

    private static final ForgeConfigSpec.DoubleValue LIMITLESS_BLUE_DAMAGE_BASELINE = BUILDER
            .comment("苍：技能伤害倍率（1.0=100% 术式强度）")
            .defineInRange("skillCombat.limitlessBlue.damageBaseline",
                    LimitlessBlueConfig.DAMAGE, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_BLUE_DAMAGE_PER_RANK = BUILDER
            .comment("苍：伤害倍率每升一级咒术等级的增量（强度面板开启时通常为 0）")
            .defineInRange("skillCombat.limitlessBlue.damagePerRank",
                    LimitlessBlueConfig.DAMAGE_PER_RANK, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_BLUE_PULL_RADIUS_BASELINE = BUILDER
            .comment("苍：吸附半径基准值（格）")
            .defineInRange("skillCombat.limitlessBlue.pullRadiusBaseline",
                    LimitlessBlueConfig.PULL_RADIUS, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_BLUE_PULL_RADIUS_PER_RANK = BUILDER
            .comment("苍：吸附半径每升一级咒术等级的增量")
            .defineInRange("skillCombat.limitlessBlue.pullRadiusPerRank",
                    LimitlessBlueConfig.PULL_RADIUS_PER_RANK, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue LIMITLESS_RED_EXPLOSION_POWER_BASELINE = BUILDER
            .comment("赫：爆炸威力基准值")
            .defineInRange("skillCombat.limitlessRed.explosionPowerBaseline",
                    LimitlessRedConfig.ARROW_EXPLOSION_POWER, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_RED_EXPLOSION_POWER_PER_RANK = BUILDER
            .comment("赫：爆炸威力每升一级咒术等级的增量")
            .defineInRange("skillCombat.limitlessRed.explosionPowerPerRank",
                    LimitlessRedConfig.EXPLOSION_POWER_PER_RANK, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_RED_KNOCKBACK_RADIUS_BASELINE = BUILDER
            .comment("赫：击飞范围基准值（格）")
            .defineInRange("skillCombat.limitlessRed.knockbackRadiusBaseline",
                    LimitlessRedConfig.ARROW_KNOCKBACK_RADIUS, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_RED_KNOCKBACK_RADIUS_PER_RANK = BUILDER
            .comment("赫：击飞范围每升一级咒术等级的增量")
            .defineInRange("skillCombat.limitlessRed.knockbackRadiusPerRank",
                    LimitlessRedConfig.KNOCKBACK_RADIUS_PER_RANK, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue LIMITLESS_REMOTE_MURASAKI_MAX_FLIGHT_CHUNKS_BASELINE = BUILDER
            .comment("超远程虚式茈：最大飞行距离基准值（区块数）")
            .defineInRange("skillCombat.limitlessRemoteMurasaki.maxFlightChunksBaseline",
                    LimitlessRemoteMurasakiConfig.MAX_FLIGHT_CHUNK_COUNT, 1.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_REMOTE_MURASAKI_MAX_FLIGHT_CHUNKS_PER_RANK = BUILDER
            .comment("超远程虚式茈：最大飞行距离每升一级咒术等级的增量（区块数）")
            .defineInRange("skillCombat.limitlessRemoteMurasaki.maxFlightChunksPerRank",
                    LimitlessRemoteMurasakiConfig.MAX_FLIGHT_CHUNK_COUNT_PER_RANK, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue SHRINE_KAI_DAMAGE_BASELINE = BUILDER
            .comment("解挥砍：伤害基准值")
            .defineInRange("skillCombat.shrineKai.damageBaseline",
                    ShrineKaiConfig.COLUMN_DAMAGE, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_KAI_DAMAGE_PER_RANK = BUILDER
            .comment("解挥砍：伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.shrineKai.damagePerRank",
                    ShrineKaiConfig.COLUMN_DAMAGE_PER_RANK, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue SHRINE_KAI_FLYOBJ_DAMAGE_BASELINE = BUILDER
            .comment("解切割：伤害基准值")
            .defineInRange("skillCombat.shrineKaiFlyobj.damageBaseline",
                    ShrineKaiConfig.COLUMN_DAMAGE, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_KAI_FLYOBJ_DAMAGE_PER_RANK = BUILDER
            .comment("解切割：伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.shrineKaiFlyobj.damagePerRank",
                    ShrineKaiConfig.COLUMN_DAMAGE_PER_RANK, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue SHRINE_HACHI_DAMAGE_BASELINE = BUILDER
            .comment("捌：伤害基准值")
            .defineInRange("skillCombat.shrineHachi.damageBaseline",
                    ShrineHachiConfig.DAMAGE, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_HACHI_DAMAGE_PER_RANK = BUILDER
            .comment("捌：伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.shrineHachi.damagePerRank",
                    ShrineHachiConfig.DAMAGE_PER_RANK, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue SHRINE_DOMAIN_SLASH_DAMAGE_BASELINE = BUILDER
            .comment("伏魔神龛：斩击伤害基准值")
            .defineInRange("skillCombat.shrineDomain.slashDamageBaseline",
                    ShrineDomainSkillGrowth.BASELINE_SLASH_DAMAGE, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_DOMAIN_SLASH_DAMAGE_PER_RANK = BUILDER
            .comment("伏魔神龛：斩击伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.shrineDomain.slashDamagePerRank",
                    ShrineDomainSkillGrowth.BASELINE_SLASH_DAMAGE_PER_RANK, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_DOMAIN_POWER_SLASH_DAMAGE_BASELINE = BUILDER
            .comment("伏魔神龛：强力斩击伤害基准值")
            .defineInRange("skillCombat.shrineDomain.powerSlashDamageBaseline",
                    ShrineDomainSkillGrowth.BASELINE_POWER_SLASH_DAMAGE, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_DOMAIN_POWER_SLASH_DAMAGE_PER_RANK = BUILDER
            .comment("伏魔神龛：强力斩击伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.shrineDomain.powerSlashDamagePerRank",
                    ShrineDomainSkillGrowth.BASELINE_POWER_SLASH_DAMAGE_PER_RANK, 0.0D, 10000.0D);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    // ======== 黑闪运行时配置（从 SPEC 加载） ========
    public static double blackFlashBaseChance;
    public static double blackFlashComboBonusPerHit;
    public static double blackFlashKillBonus;
    public static double blackFlashLowHpThreshold;
    public static double blackFlashLowHpBonus;
    public static double blackFlashDodgeBonus;
    public static double blackFlashTargetLowHpThreshold;
    public static double blackFlashTargetLowHpBonus;
    public static double blackFlashDamageMultiplier;
    public static int blackFlashComboWindowTicks;
    public static int blackFlashKillWindowTicks;
    public static int blackFlashDodgeWindowTicks;

    // ======== 黑闪触发冷却运行时配置 ========
    public static int blackFlashTriggerCooldownTicks;

    // ======== ZONE 状态运行时配置 ========
    public static int blackFlashZoneMinCombo;
    public static int blackFlashZoneDurationTicks;
    public static double blackFlashZoneBonusI;
    public static double blackFlashZoneBonusII;
    public static double blackFlashZoneBonusIII;
    public static double blackFlashZoneAttackBonus;
    public static double blackFlashZoneDefenseBonus;

    // ======== 击杀获取咒术经验运行时配置 ========
    public static int killExpRadius = 32;
    public static double killExpLastHitRatio = 0.6D;
    public static double killExpNearbyRatio = 0.4D;
    public static int killExpMaxFromMob = 20;

    // ======== 技能战斗数值（从 SPEC 加载） ========
    public static int skillCombatBaselineGrade;
    public static double limitlessBlueDamageBaseline;
    public static double limitlessBlueDamagePerRank;
    public static double limitlessBluePullRadiusBaseline;
    public static double limitlessBluePullRadiusPerRank;
    public static double limitlessRedExplosionPowerBaseline;
    public static double limitlessRedExplosionPowerPerRank;
    public static double limitlessRedKnockbackRadiusBaseline;
    public static double limitlessRedKnockbackRadiusPerRank;
    public static double limitlessRemoteMurasakiMaxFlightChunksBaseline;
    public static double limitlessRemoteMurasakiMaxFlightChunksPerRank;
    public static double shrineKaiDamageBaseline;
    public static double shrineKaiDamagePerRank;
    public static double shrineKaiFlyobjDamageBaseline;
    public static double shrineKaiFlyobjDamagePerRank;
    public static double shrineHachiDamageBaseline;
    public static double shrineHachiDamagePerRank;
    public static double shrineDomainSlashDamageBaseline;
    public static double shrineDomainSlashDamagePerRank;
    public static double shrineDomainPowerSlashDamageBaseline;
    public static double shrineDomainPowerSlashDamagePerRank;

    // ======== 战斗强度面板 ========

    public static boolean combatPowerScalingEnabled;
    public static double combatPowerBase;
    public static double combatPowerPerRank;
    public static double combatPowerTechniqueBaseline;
    public static double combatPowerPhysicalBaseline;
    public static double combatPowerHeavenlyPhysicalMultiplier;
    public static double combatPowerReverseTechniqueBonus;
    public static double combatPowerIntelPerLevel;
    /** 0 = 关闭软顶。 */
    public static int combatPowerIntelSoftCap;
    public static double combatPowerIntelSoftScale;

    // ======== SE 术式侵入（统一质量池） ========

    /** 质量宽限：占用超出上限后还允许多占多少（超出部分才吃超载罚）。 */
    public static int seQualityAllowance;
    /** 质量超载罚：每超上限 1 点质量，体术 / 术式强度各扣多少点。 */
    public static int seOverloadPowerPenaltyPerPoint;

    // ======== 防御面板 ========

    public static boolean combatDefenseEnabled;
    public static double combatDefenseArmorFactorPerPoint;
    public static double combatDefenseArmorMaxReduction;
    public static double combatDefensePhysicalResistanceCap;
    public static double combatDefenseTechniqueResistanceCap;
    public static double combatDefenseDefenseReductionCap;
    public static boolean combatDefensePenetrationEnabled;
    public static boolean combatDefensePenetrationCanOverpen;

    // ======== 伤害浮动 ========

    /** 是否启用伤害浮动乘区。 */
    public static boolean damageVarianceEnabled;
    /** 伤害浮动幅度（0.10 = ±10%，每次命中在 [1-spread, 1+spread] 内均匀随机）。 */
    public static double damageVarianceSpread;

    // domain brain
    public static boolean domainBrainEnabled;
    public static int domainBrainMax;
    public static int domainBrainFreeDailyExpansions;
    public static int domainBrainRecoverPerDay;
    public static double domainBrainFullAttemptDamage;
    public static int domainBrainOverageGain1;
    public static int domainBrainOverageGain2;
    public static int domainBrainOverageGain3;
    public static int domainBrainOverageGainRest;
    public static int domainBrainFoodHealMinor;
    public static int domainBrainFoodHealMajor;

    public static boolean domainGaugeEnabled;
    public static int domainGaugeMax;
    public static double domainGaugeTechniqueHitGain;
    public static int domainGaugeTechniqueHitCooldownTicks;
    public static double domainGaugeBlackFlashGain;
    public static double domainGaugeEfficiency25;
    public static double domainGaugeEfficiency50;
    public static double domainGaugeEfficiency75;

    public static boolean domainMeltdownRepairEnabled;
    public static int domainMeltdownRepairCost;
    public static int domainMeltdownRepairGain1;
    public static int domainMeltdownRepairGain2;
    public static int domainMeltdownRepairGain3;
    public static int domainMeltdownRepairGainRest;
    public static int domainMeltdownRepairHighBrainThreshold;
    public static int domainMeltdownRepairHighBrainExtra;

    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        if (event.getConfig().getSpec() != SPEC)
        {
            return;
        }

        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemName)))
                .collect(Collectors.toSet());

        // 黑闪配置加载
        blackFlashBaseChance = BLACK_FLASH_BASE_CHANCE.get();
        blackFlashComboBonusPerHit = BLACK_FLASH_COMBO_BONUS_PER_HIT.get();
        blackFlashKillBonus = BLACK_FLASH_KILL_BONUS.get();
        blackFlashLowHpThreshold = BLACK_FLASH_LOW_HP_THRESHOLD.get();
        blackFlashLowHpBonus = BLACK_FLASH_LOW_HP_BONUS.get();
        blackFlashDodgeBonus = BLACK_FLASH_DODGE_BONUS.get();
        blackFlashTargetLowHpThreshold = BLACK_FLASH_TARGET_LOW_HP_THRESHOLD.get();
        blackFlashTargetLowHpBonus = BLACK_FLASH_TARGET_LOW_HP_BONUS.get();
        blackFlashDamageMultiplier = BLACK_FLASH_DAMAGE_MULTIPLIER.get();
        blackFlashComboWindowTicks = BLACK_FLASH_COMBO_WINDOW_TICKS.get();
        blackFlashKillWindowTicks = BLACK_FLASH_KILL_WINDOW_TICKS.get();
        blackFlashDodgeWindowTicks = BLACK_FLASH_DODGE_WINDOW_TICKS.get();

        // 触发冷却配置加载
        blackFlashTriggerCooldownTicks = BLACK_FLASH_TRIGGER_COOLDOWN_TICKS.get();

        // ZONE 状态配置加载
        blackFlashZoneMinCombo = BLACK_FLASH_ZONE_MIN_COMBO.get();
        blackFlashZoneDurationTicks = BLACK_FLASH_ZONE_DURATION_TICKS.get();
        blackFlashZoneBonusI = BLACK_FLASH_ZONE_BONUS_I.get();
        blackFlashZoneBonusII = BLACK_FLASH_ZONE_BONUS_II.get();
        blackFlashZoneBonusIII = BLACK_FLASH_ZONE_BONUS_III.get();
        blackFlashZoneAttackBonus = BLACK_FLASH_ZONE_ATTACK_BONUS.get();
        blackFlashZoneDefenseBonus = BLACK_FLASH_ZONE_DEFENSE_BONUS.get();

        // 击杀获取咒术经验配置加载
        killExpRadius = KILL_EXP_RADIUS.get();
        killExpLastHitRatio = KILL_EXP_LAST_HIT_RATIO.get();
        killExpNearbyRatio = KILL_EXP_NEARBY_RATIO.get();
        killExpMaxFromMob = KILL_EXP_MAX_FROM_MOB.get();

        skillCombatBaselineGrade = SKILL_COMBAT_BASELINE_GRADE.get();
        limitlessBlueDamageBaseline = LIMITLESS_BLUE_DAMAGE_BASELINE.get();
        limitlessBlueDamagePerRank = LIMITLESS_BLUE_DAMAGE_PER_RANK.get();
        limitlessBluePullRadiusBaseline = LIMITLESS_BLUE_PULL_RADIUS_BASELINE.get();
        limitlessBluePullRadiusPerRank = LIMITLESS_BLUE_PULL_RADIUS_PER_RANK.get();
        limitlessRedExplosionPowerBaseline = LIMITLESS_RED_EXPLOSION_POWER_BASELINE.get();
        limitlessRedExplosionPowerPerRank = LIMITLESS_RED_EXPLOSION_POWER_PER_RANK.get();
        limitlessRedKnockbackRadiusBaseline = LIMITLESS_RED_KNOCKBACK_RADIUS_BASELINE.get();
        limitlessRedKnockbackRadiusPerRank = LIMITLESS_RED_KNOCKBACK_RADIUS_PER_RANK.get();
        limitlessRemoteMurasakiMaxFlightChunksBaseline = LIMITLESS_REMOTE_MURASAKI_MAX_FLIGHT_CHUNKS_BASELINE.get();
        limitlessRemoteMurasakiMaxFlightChunksPerRank = LIMITLESS_REMOTE_MURASAKI_MAX_FLIGHT_CHUNKS_PER_RANK.get();
        shrineKaiDamageBaseline = SHRINE_KAI_DAMAGE_BASELINE.get();
        shrineKaiDamagePerRank = SHRINE_KAI_DAMAGE_PER_RANK.get();
        shrineKaiFlyobjDamageBaseline = SHRINE_KAI_FLYOBJ_DAMAGE_BASELINE.get();
        shrineKaiFlyobjDamagePerRank = SHRINE_KAI_FLYOBJ_DAMAGE_PER_RANK.get();
        shrineHachiDamageBaseline = SHRINE_HACHI_DAMAGE_BASELINE.get();
        shrineHachiDamagePerRank = SHRINE_HACHI_DAMAGE_PER_RANK.get();
        shrineDomainSlashDamageBaseline = SHRINE_DOMAIN_SLASH_DAMAGE_BASELINE.get();
        shrineDomainSlashDamagePerRank = SHRINE_DOMAIN_SLASH_DAMAGE_PER_RANK.get();
        shrineDomainPowerSlashDamageBaseline = SHRINE_DOMAIN_POWER_SLASH_DAMAGE_BASELINE.get();
        shrineDomainPowerSlashDamagePerRank = SHRINE_DOMAIN_POWER_SLASH_DAMAGE_PER_RANK.get();

        // 战斗强度面板配置加载
        combatPowerScalingEnabled = COMBAT_POWER_SCALING_ENABLED.get();
        combatPowerBase = COMBAT_POWER_BASE.get();
        combatPowerPerRank = COMBAT_POWER_PER_RANK.get();
        combatPowerTechniqueBaseline = COMBAT_POWER_TECHNIQUE_BASELINE.get();
        combatPowerPhysicalBaseline = COMBAT_POWER_PHYSICAL_BASELINE.get();
        combatPowerHeavenlyPhysicalMultiplier = COMBAT_POWER_HEAVENLY_MULTIPLIER.get();
        combatPowerReverseTechniqueBonus = COMBAT_POWER_REVERSE_TECHNIQUE_BONUS.get();
        combatPowerIntelPerLevel = COMBAT_POWER_INTEL_PER_LEVEL.get();
        combatPowerIntelSoftCap = COMBAT_POWER_INTEL_SOFT_CAP.get();
        combatPowerIntelSoftScale = COMBAT_POWER_INTEL_SOFT_SCALE.get();

        // SE 术式侵入：统一质量池
        seQualityAllowance = SE_QUALITY_ALLOWANCE.get();
        seOverloadPowerPenaltyPerPoint = SE_OVERLOAD_POWER_PENALTY_PER_POINT.get();

        // 防御面板配置加载
        combatDefenseEnabled = COMBAT_DEFENSE_ENABLED.get();
        combatDefenseArmorFactorPerPoint = COMBAT_DEFENSE_ARMOR_FACTOR.get();
        combatDefenseArmorMaxReduction = COMBAT_DEFENSE_ARMOR_CAP.get();
        combatDefensePhysicalResistanceCap = COMBAT_DEFENSE_PHYSICAL_RESIST_CAP.get();
        combatDefenseTechniqueResistanceCap = COMBAT_DEFENSE_TECHNIQUE_RESIST_CAP.get();
        combatDefenseDefenseReductionCap = COMBAT_DEFENSE_TOTAL_CAP.get();
        combatDefensePenetrationEnabled = COMBAT_DEFENSE_PENETRATION_ENABLED.get();
        combatDefensePenetrationCanOverpen = COMBAT_DEFENSE_PENETRATION_CAN_OVERPEN.get();

        // 伤害浮动配置加载
        damageVarianceEnabled = DAMAGE_VARIANCE_ENABLED.get();
        damageVarianceSpread = DAMAGE_VARIANCE_SPREAD.get();

        domainBrainEnabled = DOMAIN_BRAIN_ENABLED.get();
        domainBrainMax = DOMAIN_BRAIN_MAX.get();
        domainBrainFreeDailyExpansions = DOMAIN_BRAIN_FREE_DAILY.get();
        domainBrainRecoverPerDay = DOMAIN_BRAIN_RECOVER_PER_DAY.get();
        domainBrainFullAttemptDamage = DOMAIN_BRAIN_FULL_ATTEMPT_DAMAGE.get();
        domainBrainOverageGain1 = DOMAIN_BRAIN_OVERAGE_1.get();
        domainBrainOverageGain2 = DOMAIN_BRAIN_OVERAGE_2.get();
        domainBrainOverageGain3 = DOMAIN_BRAIN_OVERAGE_3.get();
        domainBrainOverageGainRest = DOMAIN_BRAIN_OVERAGE_REST.get();
        domainBrainFoodHealMinor = DOMAIN_BRAIN_FOOD_HEAL_MINOR.get();
        domainBrainFoodHealMajor = DOMAIN_BRAIN_FOOD_HEAL_MAJOR.get();
        domainGaugeEnabled = DOMAIN_GAUGE_ENABLED.get();
        domainGaugeMax = DOMAIN_GAUGE_MAX.get();
        domainGaugeTechniqueHitGain = DOMAIN_GAUGE_TECH_HIT_GAIN.get();
        domainGaugeTechniqueHitCooldownTicks = DOMAIN_GAUGE_TECH_HIT_CD.get();
        domainGaugeBlackFlashGain = DOMAIN_GAUGE_BLACK_FLASH_GAIN.get();
        domainGaugeEfficiency25 = DOMAIN_GAUGE_EFF_25.get();
        domainGaugeEfficiency50 = DOMAIN_GAUGE_EFF_50.get();
        domainGaugeEfficiency75 = DOMAIN_GAUGE_EFF_75.get();
        domainMeltdownRepairEnabled = DOMAIN_MELTDOWN_REPAIR_ENABLED.get();
        domainMeltdownRepairCost = DOMAIN_MELTDOWN_REPAIR_COST.get();
        domainMeltdownRepairGain1 = DOMAIN_MELTDOWN_REPAIR_GAIN_1.get();
        domainMeltdownRepairGain2 = DOMAIN_MELTDOWN_REPAIR_GAIN_2.get();
        domainMeltdownRepairGain3 = DOMAIN_MELTDOWN_REPAIR_GAIN_3.get();
        domainMeltdownRepairGainRest = DOMAIN_MELTDOWN_REPAIR_GAIN_REST.get();
        domainMeltdownRepairHighBrainThreshold = DOMAIN_MELTDOWN_REPAIR_HIGH_BRAIN_THRESHOLD.get();
        domainMeltdownRepairHighBrainExtra = DOMAIN_MELTDOWN_REPAIR_HIGH_BRAIN_EXTRA.get();

        SkillCombatConfigRegistry.reloadFromForgeConfig();
    }
}
