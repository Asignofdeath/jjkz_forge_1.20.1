package com.darkness.jujutsukaisenzone;

import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;
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

    // ======== 技能战斗数值（玩家可在 config/jujutsukaisenzone-common.toml 修改） ========

    private static final ForgeConfigSpec.IntValue SKILL_COMBAT_BASELINE_GRADE = BUILDER
            .comment("技能数值成长基准咒术等级（1=四级，2=三级，依此类推）")
            .defineInRange("skillCombat.baselineGrade", 1, 1, 10);

    private static final ForgeConfigSpec.DoubleValue LIMITLESS_BLUE_DAMAGE_BASELINE = BUILDER
            .comment("苍：伤害基准值")
            .defineInRange("skillCombat.limitlessBlue.damageBaseline", 8.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_BLUE_DAMAGE_PER_RANK = BUILDER
            .comment("苍：伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.limitlessBlue.damagePerRank", 2.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_BLUE_PULL_RADIUS_BASELINE = BUILDER
            .comment("苍：吸附半径基准值（格）")
            .defineInRange("skillCombat.limitlessBlue.pullRadiusBaseline", 20.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_BLUE_PULL_RADIUS_PER_RANK = BUILDER
            .comment("苍：吸附半径每升一级咒术等级的增量")
            .defineInRange("skillCombat.limitlessBlue.pullRadiusPerRank", 1.0D, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue LIMITLESS_RED_EXPLOSION_POWER_BASELINE = BUILDER
            .comment("赫：爆炸威力基准值")
            .defineInRange("skillCombat.limitlessRed.explosionPowerBaseline", 14.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_RED_EXPLOSION_POWER_PER_RANK = BUILDER
            .comment("赫：爆炸威力每升一级咒术等级的增量")
            .defineInRange("skillCombat.limitlessRed.explosionPowerPerRank", 2.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_RED_KNOCKBACK_RADIUS_BASELINE = BUILDER
            .comment("赫：击飞范围基准值（格）")
            .defineInRange("skillCombat.limitlessRed.knockbackRadiusBaseline", 18.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_RED_KNOCKBACK_RADIUS_PER_RANK = BUILDER
            .comment("赫：击飞范围每升一级咒术等级的增量")
            .defineInRange("skillCombat.limitlessRed.knockbackRadiusPerRank", 0.0D, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue LIMITLESS_REMOTE_MURASAKI_MAX_FLIGHT_CHUNKS_BASELINE = BUILDER
            .comment("超远程虚式茈：最大飞行距离基准值（区块数）")
            .defineInRange("skillCombat.limitlessRemoteMurasaki.maxFlightChunksBaseline", 64.0D, 1.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue LIMITLESS_REMOTE_MURASAKI_MAX_FLIGHT_CHUNKS_PER_RANK = BUILDER
            .comment("超远程虚式茈：最大飞行距离每升一级咒术等级的增量（区块数）")
            .defineInRange("skillCombat.limitlessRemoteMurasaki.maxFlightChunksPerRank", 0.0D, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue SHRINE_KAI_DAMAGE_BASELINE = BUILDER
            .comment("解挥砍：伤害基准值")
            .defineInRange("skillCombat.shrineKai.damageBaseline", 10.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_KAI_DAMAGE_PER_RANK = BUILDER
            .comment("解挥砍：伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.shrineKai.damagePerRank", 2.0D, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue SHRINE_KAI_FLYOBJ_DAMAGE_BASELINE = BUILDER
            .comment("解切割：伤害基准值")
            .defineInRange("skillCombat.shrineKaiFlyobj.damageBaseline", 10.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_KAI_FLYOBJ_DAMAGE_PER_RANK = BUILDER
            .comment("解切割：伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.shrineKaiFlyobj.damagePerRank", 2.0D, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue SHRINE_HACHI_DAMAGE_BASELINE = BUILDER
            .comment("捌：伤害基准值")
            .defineInRange("skillCombat.shrineHachi.damageBaseline", 5.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_HACHI_DAMAGE_PER_RANK = BUILDER
            .comment("捌：伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.shrineHachi.damagePerRank", 2.0D, 0.0D, 10000.0D);

    private static final ForgeConfigSpec.DoubleValue SHRINE_DOMAIN_SLASH_DAMAGE_BASELINE = BUILDER
            .comment("伏魔神龛：斩击伤害基准值")
            .defineInRange("skillCombat.shrineDomain.slashDamageBaseline", 3.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_DOMAIN_SLASH_DAMAGE_PER_RANK = BUILDER
            .comment("伏魔神龛：斩击伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.shrineDomain.slashDamagePerRank", 5.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_DOMAIN_POWER_SLASH_DAMAGE_BASELINE = BUILDER
            .comment("伏魔神龛：强力斩击伤害基准值")
            .defineInRange("skillCombat.shrineDomain.powerSlashDamageBaseline", 6.0D, 0.0D, 10000.0D);
    private static final ForgeConfigSpec.DoubleValue SHRINE_DOMAIN_POWER_SLASH_DAMAGE_PER_RANK = BUILDER
            .comment("伏魔神龛：强力斩击伤害每升一级咒术等级的增量")
            .defineInRange("skillCombat.shrineDomain.powerSlashDamagePerRank", 10.0D, 0.0D, 10000.0D);

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

        SkillCombatConfigRegistry.reloadFromForgeConfig();
    }
}
