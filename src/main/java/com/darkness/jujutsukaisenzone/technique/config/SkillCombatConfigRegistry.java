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
 * 技能战斗数值：由 {@code config/jujutsukaisenzone-common.toml} 的 {@code [skillCombat]} 段加载。
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

    /** 从 Forge 配置文件重载（{@link Config} 加载/重载时调用）。 */
    public static void reloadFromForgeConfig()
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
                stat("damage", LimitlessBlueConfig.DAMAGE, 2.0F),
                stat("pull_radius", LimitlessBlueConfig.PULL_RADIUS, 1.0D));

        put(map, SKILL_LIMITLESS_RED,
                stat("explosion_power", LimitlessRedConfig.ARROW_EXPLOSION_POWER, 2.0F),
                stat("knockback_radius", LimitlessRedConfig.ARROW_KNOCKBACK_RADIUS, 0.0D));

        put(map, SKILL_LIMITLESS_REMOTE_PURPLE,
                stat("max_flight_chunks", LimitlessRemoteMurasakiConfig.MAX_FLIGHT_CHUNK_COUNT, 0.0D));

        put(map, SKILL_SHRINE_KAI,
                stat("damage", ShrineKaiConfig.COLUMN_DAMAGE, 2.0F));

        put(map, SKILL_SHRINE_KAI_FLYOBJ,
                stat("damage", ShrineKaiConfig.COLUMN_DAMAGE, 2.0F));

        put(map, SKILL_SHRINE_HACHI,
                stat("damage", ShrineHachiConfig.DAMAGE, 2.0F));

        put(map, SKILL_SHRINE_DOMAIN,
                stat("slash_damage", ShrineDomainSkillGrowth.BASELINE_SLASH_DAMAGE, 5.0F),
                stat("power_slash_damage", ShrineDomainSkillGrowth.BASELINE_POWER_SLASH_DAMAGE, 10.0F));

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
