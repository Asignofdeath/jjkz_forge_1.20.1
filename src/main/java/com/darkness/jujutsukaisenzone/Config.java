package com.darkness.jujutsukaisenzone;

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
    public static int killExpRadius;
    public static double killExpLastHitRatio;
    public static double killExpNearbyRatio;
    public static int killExpMaxFromMob;

    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
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
    }
}
