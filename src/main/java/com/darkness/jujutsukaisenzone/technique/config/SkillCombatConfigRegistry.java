package com.darkness.jujutsukaisenzone.technique.config;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessBlueConfig;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRedConfig;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRemoteMurasakiConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineHachiConfig;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.entity.util.SpecialEffectChunkForce;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineDomainSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能战斗数值的<b>唯一权威来源</b>：真值来自 {@link Config} 里各项 {@code defineInRange}
 * 的默认值（本模组<b>没有配置文件</b>，见 {@link Config} 的类注释）；
 * 未经 {@link #reloadFromSpecDefaults()} 时退回 {@link #buildCodeDefaults()} 的代码默认值。
 * <p>
 * <b>权威规则（新增 skillCombat 项时照此办理）</b>：
 * <ol>
 *   <li>数值只在 {@code entity/data/*Config}（或 {@code *SkillGrowth}）里手写<b>一次</b>，
 *       同时标注 {@code 【配置项·默认值】}。</li>
 *   <li>{@code Config.java} 的 {@code defineInRange(...)} 默认值与 {@link #buildCodeDefaults()}
 *       都<b>引用</b>那个常量，不许再写一遍数字字面量 —— 写三遍的后果是改一处不生效也不报错。</li>
 *   <li>业务代码读取数值<b>一律</b>走 {@code *SkillGrowth}（或本类的 {@code resolve*}），
 *       <b>不许直接引用那个常量</b>：常量只是默认值，直接引用会同时丢掉配置修改与咒术等级成长。
 *       {@code tools/jjkz_static_check.py} 有一条守卫检查在盯这件事。</li>
 * </ol>
 * <p>
 * ⚠️ 按强度面板接管的项：{@code damage} 一类的 {@code perRank} 在
 * {@code Config.combatPowerScalingEnabled} 为 true 时会被乘 0（等级成长改由「术式强度」承担），
 * 所以配置文件里能看到该项、但它当下不生效。这是刻意的，不是 bug。
 * <p>
 * 伤害类 baseline / perRank 的单位是<b>技能倍率</b>（1.0 = 100% 对应通道强度），
 * 不是旧版绝对伤害点数。若沿用改造前的 toml 数值（如 8.0），请除以 20 再写入。
 */
public final class SkillCombatConfigRegistry
{
    public static final String SKILL_LIMITLESS_BLUE = LimitlessTechniqueSkillSet.SKILL_BLUE;
    public static final String SKILL_LIMITLESS_RED = LimitlessTechniqueSkillSet.SKILL_RED;
    public static final String SKILL_LIMITLESS_REMOTE_PURPLE = LimitlessTechniqueSkillSet.SKILL_REMOTE_PURPLE;
    public static final String SKILL_SHRINE_KAI = ShrineTechniqueSkillSet.SKILL_KAI;
    public static final String SKILL_SHRINE_KAI_FLYOBJ = ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ;
    public static final String SKILL_SHRINE_HACHI = ShrineTechniqueSkillSet.SKILL_HACHI;
    public static final String SKILL_SHRINE_DOMAIN = "shrine_domain";

    private static volatile Map<String, Map<String, SkillCombatStatDef>> entries = buildCodeDefaults();

    private SkillCombatConfigRegistry()
    {
    }

    /**
     * 用 {@link Config} 的运行期取值重建数值表（{@link Config} 类初始化时调用一次）。
     * <p>
     * ⛔ 方法名里刻意没有 "ForgeConfig"：本模组已不注册配置文件，{@code Config} 的取值就是
     * spec 的默认值，所以这里不是「重载配置」，只是一次性的初始化。
     */
    public static void reloadFromSpecDefaults()
    {
        int baselineGrade = Config.skillCombatBaselineGrade;
        // ⚠️ 伤害类的 perRank 已交给强度面板（SkillDamageResolver）：等级成长统一由「术式强度」
        // 承担，这里再叠加一次就会双计。只有关闭面板换算时才沿用旧的 perRank 曲线。
        double perRankFactor = Config.combatPowerScalingEnabled ? 0.0D : 1.0D;
        Map<String, Map<String, SkillCombatStatDef>> loaded = new HashMap<>();

        put(loaded, SKILL_LIMITLESS_BLUE,
                stat("damage", Config.limitlessBlueDamageBaseline, Config.limitlessBlueDamagePerRank * perRankFactor, baselineGrade),
                stat("pull_radius", Config.limitlessBluePullRadiusBaseline, Config.limitlessBluePullRadiusPerRank, baselineGrade));

        put(loaded, SKILL_LIMITLESS_RED,
                stat("explosion_power", Config.limitlessRedExplosionPowerBaseline, Config.limitlessRedExplosionPowerPerRank, baselineGrade),
                stat("knockback_radius", Config.limitlessRedKnockbackRadiusBaseline, Config.limitlessRedKnockbackRadiusPerRank, baselineGrade));

        put(loaded, SKILL_LIMITLESS_REMOTE_PURPLE,
                stat("max_flight_chunks", Config.limitlessRemoteMurasakiMaxFlightChunksBaseline,
                        Config.limitlessRemoteMurasakiMaxFlightChunksPerRank, baselineGrade));

        put(loaded, SKILL_SHRINE_KAI,
                stat("damage", Config.shrineKaiDamageBaseline, Config.shrineKaiDamagePerRank * perRankFactor, baselineGrade));

        put(loaded, SKILL_SHRINE_KAI_FLYOBJ,
                stat("damage", Config.shrineKaiFlyobjDamageBaseline, Config.shrineKaiFlyobjDamagePerRank * perRankFactor, baselineGrade));

        put(loaded, SKILL_SHRINE_HACHI,
                stat("damage", Config.shrineHachiDamageBaseline, Config.shrineHachiDamagePerRank * perRankFactor, baselineGrade));

        put(loaded, SKILL_SHRINE_DOMAIN,
                stat("slash_damage", Config.shrineDomainSlashDamageBaseline,
                        Config.shrineDomainSlashDamagePerRank * perRankFactor, baselineGrade),
                stat("power_slash_damage", Config.shrineDomainPowerSlashDamageBaseline,
                        Config.shrineDomainPowerSlashDamagePerRank * perRankFactor, baselineGrade));

        entries = Map.copyOf(loaded);
    }

    public static float resolveFloat(String entryId, String statKey, @Nullable LivingEntity owner)
    {
        return stat(entryId, statKey).resolveFloat(owner);
    }

    public static double resolveDouble(String entryId, String statKey, @Nullable LivingEntity owner)
    {
        return stat(entryId, statKey).resolveDouble(owner);
    }

    public static int resolveInt(String entryId, String statKey, @Nullable LivingEntity owner)
    {
        return stat(entryId, statKey).resolveInt(owner);
    }

    /** 超远程虚式茈：将区块数换算为方块距离。 */
    public static double resolveRemoteMurasakiMaxFlightDistance(@Nullable LivingEntity owner)
    {
        int chunks = resolveInt(SKILL_LIMITLESS_REMOTE_PURPLE, "max_flight_chunks", owner);
        return SpecialEffectChunkForce.blocksFromChunks(chunks);
    }

    private static SkillCombatStatDef stat(String entryId, String statKey)
    {
        Map<String, SkillCombatStatDef> stats = entries.get(entryId);
        if (stats == null)
        {
            throw new IllegalArgumentException("Unknown skill combat entry: " + entryId);
        }
        SkillCombatStatDef def = stats.get(statKey);
        if (def == null)
        {
            throw new IllegalArgumentException("Unknown stat '" + statKey + "' for entry: " + entryId);
        }
        return def;
    }

    private static Map<String, Map<String, SkillCombatStatDef>> buildCodeDefaults()
    {
        Map<String, Map<String, SkillCombatStatDef>> map = new HashMap<>();

        put(map, SKILL_LIMITLESS_BLUE,
                stat("damage", LimitlessBlueConfig.DAMAGE, LimitlessBlueConfig.DAMAGE_PER_RANK),
                stat("pull_radius", LimitlessBlueConfig.PULL_RADIUS, LimitlessBlueConfig.PULL_RADIUS_PER_RANK));

        put(map, SKILL_LIMITLESS_RED,
                stat("explosion_power", LimitlessRedConfig.ARROW_EXPLOSION_POWER,
                        LimitlessRedConfig.EXPLOSION_POWER_PER_RANK),
                stat("knockback_radius", LimitlessRedConfig.ARROW_KNOCKBACK_RADIUS,
                        LimitlessRedConfig.KNOCKBACK_RADIUS_PER_RANK));

        put(map, SKILL_LIMITLESS_REMOTE_PURPLE,
                stat("max_flight_chunks", LimitlessRemoteMurasakiConfig.MAX_FLIGHT_CHUNK_COUNT,
                        LimitlessRemoteMurasakiConfig.MAX_FLIGHT_CHUNK_COUNT_PER_RANK));

        put(map, SKILL_SHRINE_KAI,
                stat("damage", ShrineKaiConfig.COLUMN_DAMAGE, ShrineKaiConfig.COLUMN_DAMAGE_PER_RANK));

        put(map, SKILL_SHRINE_KAI_FLYOBJ,
                stat("damage", ShrineKaiConfig.COLUMN_DAMAGE, ShrineKaiConfig.COLUMN_DAMAGE_PER_RANK));

        put(map, SKILL_SHRINE_HACHI,
                stat("damage", ShrineHachiConfig.DAMAGE, ShrineHachiConfig.DAMAGE_PER_RANK));

        put(map, SKILL_SHRINE_DOMAIN,
                stat("slash_damage", ShrineDomainSkillGrowth.BASELINE_SLASH_DAMAGE,
                        ShrineDomainSkillGrowth.BASELINE_SLASH_DAMAGE_PER_RANK),
                stat("power_slash_damage", ShrineDomainSkillGrowth.BASELINE_POWER_SLASH_DAMAGE,
                        ShrineDomainSkillGrowth.BASELINE_POWER_SLASH_DAMAGE_PER_RANK));

        return map;
    }

    private static void put(Map<String, Map<String, SkillCombatStatDef>> map, String entryId, SkillCombatStatDef... stats)
    {
        Map<String, SkillCombatStatDef> entry = new HashMap<>();
        for (SkillCombatStatDef def : stats)
        {
            entry.put(def.key(), def);
        }
        map.put(entryId, entry);
    }

    private static SkillCombatStatDef stat(String key, double baseline, double perRank)
    {
        return new SkillCombatStatDef(key, baseline, perRank);
    }

    private static SkillCombatStatDef stat(String key, double baseline, double perRank, int baselineGrade)
    {
        return new SkillCombatStatDef(key, baseline, perRank, baselineGrade);
    }
}
