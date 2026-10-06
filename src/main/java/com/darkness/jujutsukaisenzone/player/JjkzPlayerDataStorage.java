package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 统一维护玩家核心成长数据，避免继续依赖实体克隆时的手工 NBT 拷贝。
 */
public final class JjkzPlayerDataStorage extends SavedData
{
    private static final String DATA_NAME = JuJutsuKaisenZone.MODID + "_player_data";
    private static final String TAG_PLAYERS = "players";

    private final Map<UUID, PlayerDataEntry> players = new HashMap<>();

    public static JjkzPlayerDataStorage get(ServerPlayer player)
    {
        return get(player.getServer());
    }

    public static JjkzPlayerDataStorage get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(JjkzPlayerDataStorage::load, JjkzPlayerDataStorage::new, DATA_NAME);
    }

    private static JjkzPlayerDataStorage load(CompoundTag rootTag)
    {
        JjkzPlayerDataStorage storage = new JjkzPlayerDataStorage();
        CompoundTag playersTag = rootTag.getCompound(TAG_PLAYERS);
        for (String uuidString : playersTag.getAllKeys())
        {
            storage.players.put(UUID.fromString(uuidString), PlayerDataEntry.load(playersTag.getCompound(uuidString)));
        }
        return storage;
    }

    private static final String LEGACY_TAG_FACTION = "jujutsukaisenzone_faction";
    private static final String LEGACY_TAG_CURSE_GRADE_XP = "jujutsukaisenzone_curse_grade_xp";

    public PlayerDataEntry getOrCreate(ServerPlayer player)
    {
        PlayerDataEntry entry = players.computeIfAbsent(player.getUUID(), uuid -> new PlayerDataEntry());
        importLegacyPersistentData(player, entry);
        return entry;
    }

    private void importLegacyPersistentData(ServerPlayer player, PlayerDataEntry entry)
    {
        CompoundTag tag = player.getPersistentData();
        boolean changed = false;

        if (entry.factionId == null || entry.factionId.isEmpty())
        {
            String legacyFaction = tag.getString(LEGACY_TAG_FACTION);
            if (!legacyFaction.isEmpty())
            {
                entry.factionId = legacyFaction;
                changed = true;
            }
        }

        if (entry.curseGradeExperience <= 0)
        {
            int legacyXp = tag.getInt(LEGACY_TAG_CURSE_GRADE_XP);
            if (legacyXp > 0)
            {
                entry.curseGradeExperience = legacyXp;
                changed = true;
            }
        }

        if (changed)
        {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag rootTag)
    {
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, PlayerDataEntry> entry : players.entrySet())
        {
            playersTag.put(entry.getKey().toString(), entry.getValue().save());
        }
        rootTag.put(TAG_PLAYERS, playersTag);
        return rootTag;
    }

    public static final class PlayerDataEntry
    {
        private static final String TAG_CURSED_ENERGY_INITIALIZED = "cursedEnergyInitialized";
        private static final String TAG_CURSED_ENERGY_BASE_MAX = "cursedEnergyBaseMax";
        private static final String TAG_CURSED_ENERGY_CURRENT = "cursedEnergyCurrent";
        private static final String TAG_CURSE_GRADE_EXPERIENCE = "curseGradeExperience";
        private static final String TAG_REVERSE_TECHNIQUE_LEARNED = "reverseTechniqueLearned";
        private static final String TAG_REVERSE_STUDYBOOK_BOOST_USES = "reverseStudybookBoostUses";
        private static final String TAG_FACTION_ID = "factionId";
        private static final String TAG_INNATE_TECHNIQUE = "innateTechnique";
        private static final String TAG_BARRIER_TECHNIQUE_LEVEL = "barrierTechniqueLevel";
        private static final String TAG_BARRIER_STUDYBOOK_READ_COUNT = "barrierStudybookReadCount";
        private static final String TAG_BARRIER_TECHNIQUE_PENDING_WAKE_UNLOCK = "barrierTechniquePendingWakeUnlock";
        private static final String TAG_DOMAIN_EXPANSION_LEARNED = "domainExpansionLearned";
        private static final String TAG_DOMAIN_STUDYBOOK_BOOST_USES = "domainStudybookBoostUses";
        private static final String TAG_INNATE_DOMAIN_SESSION_ACTIVE = "innateDomainSessionActive";
        private static final String TAG_HEALTH_INITIALIZED = "healthInitialized";
        private static final String TAG_HEALTH_CURRENT = "healthCurrent";
        private static final String TAG_MIMICRY_ACTIVE = "mimicryActive";
        private static final String TAG_SEANCE_TECHNIQUE_LEVEL = "seanceTechniqueLevel";
        private static final String TAG_SOUL_STUDYBOOK_READ_COUNT = "soulStudybookReadCount";
        private static final String TAG_BARRIER_TECHNIQUE_UPGRADE_CHANCE = "barrierTechniqueUpgradeChance";
        private static final String TAG_SEANCE_TECHNIQUE_UPGRADE_CHANCE = "seanceTechniqueUpgradeChance";
        private static final String TAG_STRENGTHEN_TECHNIQUE_LEVEL = "strengthenTechniqueLevel";
        private static final String TAG_STRENGTHEN_TECHNIQUE_UPGRADE_CHANCE = "strengthenTechniqueUpgradeChance";
        private static final String TAG_STRENGTHEN_ACTIVE = "strengthenActive";
        private static final String TAG_LAST_PROCESSED_GAME_DAY = "lastProcessedGameDay";
        private static final String TAG_TECHNIQUE_INTEL = "techniqueIntel";
        private static final String TAG_SP_POINTS = "spPoints";
        private static final String TAG_SP_SPENT_LIBERATION = "spSpentLiberation";
        private static final String TAG_SP_SPENT_REFRESH = "spSpentRefresh";
        private static final String TAG_SP_NODE_QUALITY = "spNodeQuality";
        private static final String TAG_SP_LIT_NODES = "spLitNodes";
        private static final String TAG_SP_OFFERS = "spOffers";
        private static final String TAG_SP_LAST_GRANTED_TIER = "spLastGrantedTier";
        /** 旧固定节点 id 列表，仅读入后丢弃（不做迁移）。 */
        private static final String TAG_SP_UNLOCKED_NODES_LEGACY = "spUnlockedNodes";
        private static final String TAG_CURSED_ENERGY_TRAIT = "cursedEnergyTrait";
        private static final String TAG_SE_EQUIPPED = "seEquipped";
        private static final String TAG_PHYSICAL_POWER_BONUS = "physicalPowerBonus";
        private static final String TAG_TECHNIQUE_POWER_BONUS = "techniquePowerBonus";
        private static final String TAG_BRAIN_DAMAGE = "brainDamage";
        private static final String TAG_DOMAIN_EXPANDS_TODAY = "domainExpandsToday";
        private static final String TAG_DOMAIN_EXPAND_COUNT_DAY = "domainExpandCountDay";
        private static final String TAG_DOMAIN_GAUGE = "domainGauge";
        private static final String TAG_MELTDOWN_REPAIRS_TODAY = "meltdownRepairsToday";
        private static final String TAG_MELTDOWN_REPAIR_COUNT_DAY = "meltdownRepairCountDay";
        private static final String TAG_MELTDOWN_REPAIR_LEARNED = "meltdownRepairLearned";
        private static final String TAG_MAIN_QUEST_BOOTSTRAPPED = "mainQuestBootstrapped";
        private static final String TAG_COMPLETED_MAIN_QUESTS = "completedMainQuests";
        private static final String TAG_TRACKED_MAIN_QUEST = "trackedMainQuest";
        private static final String TAG_MAIN_11_INTRO_DONE = "main11IntroDone";
        private static final String TAG_MAIN_11_TALK_DONE = "main11TalkDone";
        private static final String TAG_MAIN_11_KITTED = "main11Kitted";
        private static final String TAG_MAIN_11_CABIN = "main11Cabin";
        private static final String TAG_NARRATIVE_VILLAGE = "narrativeVillage";
        private static final String TAG_MAIN_12_AMBUSH_STARTED = "main12AmbushStarted";
        private static final String TAG_MAIN_12_AMBUSH_ORIGIN = "main12AmbushOrigin";
        private static final String TAG_MAIN_12_DECOR_PLACED = "main12DecorPlaced";
        private static final String TAG_MAIN_12_HEALTH_LOCKED = "main12HealthLocked";
        private static final String TAG_MAIN_13_PERFORMANCE_DONE = "main13PerformanceDone";
        private static final String TAG_INNATE_TALENT_ID = "innateTalentId";
        private static final String TAG_INNATE_TALENT_SOURCE = "innateTalentSource";
        private static final String TAG_TECHNIQUE_ATK_OBJ_MODE = "techniqueAtkObjMode";
        private static final String TAG_TECHNIQUE_ATK_OBJ_IDS = "techniqueAtkObjIds";

        boolean cursedEnergyInitialized;
        int cursedEnergyBaseMax;
        int cursedEnergyCurrent;
        int curseGradeExperience;
        boolean reverseTechniqueLearned;
        int reverseStudybookBoostUses;
        String factionId = "";
        String innateTechnique = PlayerTechniqueData.TECHNIQUE_NONE;
        String barrierTechniqueLevel = BarrierTechniqueLevel.UNLEARNED.name();
        int barrierStudybookReadCount;
        boolean barrierTechniquePendingWakeUnlock;
        boolean domainExpansionLearned;
        int domainStudybookBoostUses;
        boolean innateDomainSessionActive;
        boolean healthInitialized;
        float healthCurrent;
        boolean mimicryActive;
        String seanceTechniqueLevel = SeanceTechniqueLevel.UNLEARNED.name();
        int soulStudybookReadCount;
        int barrierTechniqueUpgradeChance;
        int seanceTechniqueUpgradeChance;
        String strengthenTechniqueLevel = StrengthenTechniqueLevel.UNLEARNED.name();
        int strengthenTechniqueUpgradeChance;
        boolean strengthenActive;
        long lastProcessedGameDay = -1L;
        final Map<String, Integer> techniqueIntel = new LinkedHashMap<>();
        int spPoints;
        int spSpentLiberation;
        /** SP 货架付费刷新永久消耗（不退）。 */
        int spSpentRefresh;
        /** 质量上限缓存（权威由咒术经验派生，见 {@code SpNodeShelfRules}）。 */
        int spNodeQuality = PlayerSpNodeData.DEFAULT_QUALITY;
        final List<SpNodeInstance> spLitNodes = new ArrayList<>();
        final List<SpNodeInstance> spOffers = new ArrayList<>();
        int spLastGrantedTier;
        String cursedEnergyTrait = CursedEnergyTrait.NORMAL.id();
        /** 已装载 SE：seId → 装载时咒物实例的特质 + 代价（卸下按此退回）。 */
        final Map<String, SeEquippedEntry> seEquipped = new LinkedHashMap<>();
        int physicalPowerBonus;
        int techniquePowerBonus;
        int brainDamage;
        int domainExpandsToday;
        long domainExpandCountDay = -1L;
        int domainGauge;
        int meltdownRepairsToday;
        long meltdownRepairCountDay = -1L;
        boolean meltdownRepairLearned;
        /** 是否已做过主线链头引导（1.1 自动解锁并追踪）。 */
        boolean mainQuestBootstrapped;
        final java.util.LinkedHashSet<String> completedMainQuests = new java.util.LinkedHashSet<>();
        String trackedMainQuest = "";
        boolean main11IntroDone;
        boolean main11TalkDone;
        boolean main11Kitted;
        @javax.annotation.Nullable BlockPos main11CabinPos;
        @javax.annotation.Nullable BlockPos narrativeVillagePos;
        boolean main12AmbushStarted;
        @javax.annotation.Nullable BlockPos main12AmbushOrigin;
        boolean main12DecorPlaced;
        boolean main12HealthLocked;
        /** 1.3「特级术师的实力」演出是否已播完（复活点续播用）。 */
        boolean main13PerformanceDone;
        public String innateTalentId = "";
        public String innateTalentSource = "";
        public String techniqueAtkObjMode = "";
        public final java.util.LinkedHashSet<UUID> techniqueAtkObjIds = new java.util.LinkedHashSet<>();

        private static PlayerDataEntry load(CompoundTag tag)
        {
            PlayerDataEntry entry = new PlayerDataEntry();
            entry.cursedEnergyInitialized = tag.getBoolean(TAG_CURSED_ENERGY_INITIALIZED);
            entry.cursedEnergyBaseMax = tag.getInt(TAG_CURSED_ENERGY_BASE_MAX);
            entry.cursedEnergyCurrent = tag.getInt(TAG_CURSED_ENERGY_CURRENT);
            entry.curseGradeExperience = tag.getInt(TAG_CURSE_GRADE_EXPERIENCE);
            entry.reverseTechniqueLearned = tag.getBoolean(TAG_REVERSE_TECHNIQUE_LEARNED);
            entry.reverseStudybookBoostUses = Math.max(0, tag.getInt(TAG_REVERSE_STUDYBOOK_BOOST_USES));
            entry.factionId = tag.getString(TAG_FACTION_ID);
            entry.innateTechnique = tag.getString(TAG_INNATE_TECHNIQUE);
            entry.barrierTechniqueLevel = tag.contains(TAG_BARRIER_TECHNIQUE_LEVEL) 
                    ? tag.getString(TAG_BARRIER_TECHNIQUE_LEVEL) 
                    : BarrierTechniqueLevel.UNLEARNED.name();
            entry.barrierStudybookReadCount = Math.max(0, tag.getInt(TAG_BARRIER_STUDYBOOK_READ_COUNT));
            entry.barrierTechniquePendingWakeUnlock = tag.getBoolean(TAG_BARRIER_TECHNIQUE_PENDING_WAKE_UNLOCK);
            entry.domainExpansionLearned = tag.getBoolean(TAG_DOMAIN_EXPANSION_LEARNED);
            entry.domainStudybookBoostUses = Math.max(0, tag.getInt(TAG_DOMAIN_STUDYBOOK_BOOST_USES));
            entry.innateDomainSessionActive = tag.getBoolean(TAG_INNATE_DOMAIN_SESSION_ACTIVE);
            entry.healthInitialized = tag.getBoolean(TAG_HEALTH_INITIALIZED);
            entry.healthCurrent = tag.getFloat(TAG_HEALTH_CURRENT);
            entry.mimicryActive = tag.getBoolean(TAG_MIMICRY_ACTIVE);
            entry.seanceTechniqueLevel = tag.contains(TAG_SEANCE_TECHNIQUE_LEVEL)
                    ? tag.getString(TAG_SEANCE_TECHNIQUE_LEVEL)
                    : SeanceTechniqueLevel.UNLEARNED.name();
            entry.soulStudybookReadCount = Math.max(0, tag.getInt(TAG_SOUL_STUDYBOOK_READ_COUNT));
            entry.barrierTechniqueUpgradeChance = clampUpgradeChance(tag.getInt(TAG_BARRIER_TECHNIQUE_UPGRADE_CHANCE));
            entry.seanceTechniqueUpgradeChance = clampUpgradeChance(tag.getInt(TAG_SEANCE_TECHNIQUE_UPGRADE_CHANCE));
            entry.strengthenTechniqueLevel = tag.contains(TAG_STRENGTHEN_TECHNIQUE_LEVEL)
                    ? tag.getString(TAG_STRENGTHEN_TECHNIQUE_LEVEL)
                    : StrengthenTechniqueLevel.UNLEARNED.name();
            entry.strengthenTechniqueUpgradeChance = clampUpgradeChance(tag.getInt(TAG_STRENGTHEN_TECHNIQUE_UPGRADE_CHANCE));
            entry.strengthenActive = tag.getBoolean(TAG_STRENGTHEN_ACTIVE);
            entry.lastProcessedGameDay = tag.contains(TAG_LAST_PROCESSED_GAME_DAY)
                    ? tag.getLong(TAG_LAST_PROCESSED_GAME_DAY)
                    : -1L;
            CompoundTag intelTag = tag.getCompound(TAG_TECHNIQUE_INTEL);
            for (String techniqueId : intelTag.getAllKeys())
            {
                entry.techniqueIntel.put(techniqueId, Math.max(0, intelTag.getInt(techniqueId)));
            }
            entry.spPoints = Math.max(0, tag.getInt(TAG_SP_POINTS));
            entry.spSpentLiberation = Math.max(0, tag.getInt(TAG_SP_SPENT_LIBERATION));
            entry.spSpentRefresh = Math.max(0, tag.getInt(TAG_SP_SPENT_REFRESH));
            entry.spNodeQuality = tag.contains(TAG_SP_NODE_QUALITY)
                    ? Math.max(1, tag.getInt(TAG_SP_NODE_QUALITY))
                    : PlayerSpNodeData.DEFAULT_QUALITY;
            entry.spLastGrantedTier = Math.max(0, tag.getInt(TAG_SP_LAST_GRANTED_TIER));
            loadSpNodeList(tag.getList(TAG_SP_LIT_NODES, Tag.TAG_COMPOUND), entry.spLitNodes);
            loadSpNodeList(tag.getList(TAG_SP_OFFERS, Tag.TAG_COMPOUND), entry.spOffers);
            // 旧档字符串节点表直接丢弃（货架改为实例制，不做迁移）。
            tag.getList(TAG_SP_UNLOCKED_NODES_LEGACY, Tag.TAG_STRING);
            entry.cursedEnergyTrait = tag.contains(TAG_CURSED_ENERGY_TRAIT)
                    ? CursedEnergyTrait.byId(tag.getString(TAG_CURSED_ENERGY_TRAIT)).id()
                    : CursedEnergyTrait.NORMAL.id();
            // 装载是「集合」语义：每项为 {id, trait, penaltyStat, penaltyAmount}；
            // 兼容旧档纯字符串列表 / 仅有 trait 的 compound（缺代价时 amount=0）。
            ListTag seCompoundList = tag.getList(TAG_SE_EQUIPPED, Tag.TAG_COMPOUND);
            if (!seCompoundList.isEmpty())
            {
                for (int i = 0; i < seCompoundList.size(); i++)
                {
                    CompoundTag seTag = seCompoundList.getCompound(i);
                    String seId = seTag.getString("id");
                    if (seId == null || seId.isBlank())
                    {
                        continue;
                    }
                    entry.seEquipped.put(seId, SeEquippedEntry.fromTag(seTag, entry.cursedEnergyTrait));
                }
            }
            else
            {
                ListTag legacy = tag.getList(TAG_SE_EQUIPPED, Tag.TAG_STRING);
                for (int i = 0; i < legacy.size(); i++)
                {
                    String seId = legacy.getString(i);
                    if (seId != null && !seId.isBlank())
                    {
                        entry.seEquipped.put(seId, new SeEquippedEntry(entry.cursedEnergyTrait, null));
                    }
                }
            }
            entry.physicalPowerBonus = Math.max(0, tag.getInt(TAG_PHYSICAL_POWER_BONUS));
            entry.techniquePowerBonus = Math.max(0, tag.getInt(TAG_TECHNIQUE_POWER_BONUS));
            entry.brainDamage = Math.max(0, tag.getInt(TAG_BRAIN_DAMAGE));
            entry.domainExpandsToday = Math.max(0, tag.getInt(TAG_DOMAIN_EXPANDS_TODAY));
            entry.domainExpandCountDay = tag.contains(TAG_DOMAIN_EXPAND_COUNT_DAY) ? tag.getLong(TAG_DOMAIN_EXPAND_COUNT_DAY) : -1L;
            entry.domainGauge = Math.max(0, tag.getInt(TAG_DOMAIN_GAUGE));
            entry.meltdownRepairsToday = Math.max(0, tag.getInt(TAG_MELTDOWN_REPAIRS_TODAY));
            entry.meltdownRepairCountDay = tag.contains(TAG_MELTDOWN_REPAIR_COUNT_DAY)
                    ? tag.getLong(TAG_MELTDOWN_REPAIR_COUNT_DAY)
                    : -1L;
            entry.meltdownRepairLearned = tag.getBoolean(TAG_MELTDOWN_REPAIR_LEARNED);
            entry.mainQuestBootstrapped = tag.getBoolean(TAG_MAIN_QUEST_BOOTSTRAPPED);
            entry.completedMainQuests.clear();
            if (tag.contains(TAG_COMPLETED_MAIN_QUESTS, Tag.TAG_LIST))
            {
                ListTag completed = tag.getList(TAG_COMPLETED_MAIN_QUESTS, Tag.TAG_STRING);
                for (int i = 0; i < completed.size(); i++)
                {
                    String questId = completed.getString(i);
                    if (questId != null && !questId.isBlank())
                    {
                        entry.completedMainQuests.add(questId);
                    }
                }
            }
            entry.trackedMainQuest = tag.getString(TAG_TRACKED_MAIN_QUEST);
            entry.main11IntroDone = tag.getBoolean(TAG_MAIN_11_INTRO_DONE);
            entry.main11TalkDone = tag.getBoolean(TAG_MAIN_11_TALK_DONE);
            entry.main11Kitted = tag.getBoolean(TAG_MAIN_11_KITTED);
            if (tag.contains(TAG_MAIN_11_CABIN, Tag.TAG_COMPOUND))
            {
                CompoundTag cabin = tag.getCompound(TAG_MAIN_11_CABIN);
                entry.main11CabinPos = new BlockPos(cabin.getInt("x"), cabin.getInt("y"), cabin.getInt("z"));
            }
            if (tag.contains(TAG_NARRATIVE_VILLAGE, Tag.TAG_COMPOUND))
            {
                CompoundTag village = tag.getCompound(TAG_NARRATIVE_VILLAGE);
                entry.narrativeVillagePos = new BlockPos(
                        village.getInt("x"), village.getInt("y"), village.getInt("z"));
            }
            entry.main12AmbushStarted = tag.getBoolean(TAG_MAIN_12_AMBUSH_STARTED);
            if (tag.contains(TAG_MAIN_12_AMBUSH_ORIGIN, Tag.TAG_COMPOUND))
            {
                CompoundTag ambush = tag.getCompound(TAG_MAIN_12_AMBUSH_ORIGIN);
                entry.main12AmbushOrigin = new BlockPos(ambush.getInt("x"), ambush.getInt("y"), ambush.getInt("z"));
            }
            entry.main12DecorPlaced = tag.getBoolean(TAG_MAIN_12_DECOR_PLACED);
            entry.main12HealthLocked = tag.getBoolean(TAG_MAIN_12_HEALTH_LOCKED);
            entry.main13PerformanceDone = tag.getBoolean(TAG_MAIN_13_PERFORMANCE_DONE);
            entry.innateTalentId = tag.getString(TAG_INNATE_TALENT_ID);
            entry.innateTalentSource = tag.getString(TAG_INNATE_TALENT_SOURCE);
            entry.techniqueAtkObjMode = tag.getString(TAG_TECHNIQUE_ATK_OBJ_MODE);
            entry.techniqueAtkObjIds.clear();
            if (tag.contains(TAG_TECHNIQUE_ATK_OBJ_IDS, Tag.TAG_LIST))
            {
                ListTag ids = tag.getList(TAG_TECHNIQUE_ATK_OBJ_IDS, Tag.TAG_STRING);
                for (int i = 0; i < ids.size(); i++)
                {
                    String raw = ids.getString(i);
                    if (raw == null || raw.isBlank())
                    {
                        continue;
                    }
                    try
                    {
                        entry.techniqueAtkObjIds.add(UUID.fromString(raw));
                    }
                    catch (IllegalArgumentException ignored)
                    {
                    }
                }
            }
            return entry;
        }

        private CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean(TAG_CURSED_ENERGY_INITIALIZED, cursedEnergyInitialized);
            tag.putInt(TAG_CURSED_ENERGY_BASE_MAX, cursedEnergyBaseMax);
            tag.putInt(TAG_CURSED_ENERGY_CURRENT, cursedEnergyCurrent);
            tag.putInt(TAG_CURSE_GRADE_EXPERIENCE, curseGradeExperience);
            tag.putBoolean(TAG_REVERSE_TECHNIQUE_LEARNED, reverseTechniqueLearned);
            tag.putInt(TAG_REVERSE_STUDYBOOK_BOOST_USES, Math.max(0, reverseStudybookBoostUses));
            tag.putString(TAG_FACTION_ID, factionId == null ? "" : factionId);
            tag.putString(TAG_INNATE_TECHNIQUE, innateTechnique == null ? PlayerTechniqueData.TECHNIQUE_NONE : innateTechnique);
            tag.putString(TAG_BARRIER_TECHNIQUE_LEVEL, barrierTechniqueLevel == null ? BarrierTechniqueLevel.UNLEARNED.name() : barrierTechniqueLevel);
            tag.putInt(TAG_BARRIER_STUDYBOOK_READ_COUNT, Math.max(0, barrierStudybookReadCount));
            tag.putBoolean(TAG_BARRIER_TECHNIQUE_PENDING_WAKE_UNLOCK, barrierTechniquePendingWakeUnlock);
            tag.putBoolean(TAG_DOMAIN_EXPANSION_LEARNED, domainExpansionLearned);
            tag.putInt(TAG_DOMAIN_STUDYBOOK_BOOST_USES, Math.max(0, domainStudybookBoostUses));
            tag.putBoolean(TAG_INNATE_DOMAIN_SESSION_ACTIVE, innateDomainSessionActive);
            tag.putBoolean(TAG_HEALTH_INITIALIZED, healthInitialized);
            tag.putFloat(TAG_HEALTH_CURRENT, healthCurrent);
            tag.putBoolean(TAG_MIMICRY_ACTIVE, mimicryActive);
            tag.putString(TAG_SEANCE_TECHNIQUE_LEVEL, seanceTechniqueLevel == null ? SeanceTechniqueLevel.UNLEARNED.name() : seanceTechniqueLevel);
            tag.putInt(TAG_SOUL_STUDYBOOK_READ_COUNT, Math.max(0, soulStudybookReadCount));
            tag.putInt(TAG_BARRIER_TECHNIQUE_UPGRADE_CHANCE, clampUpgradeChance(barrierTechniqueUpgradeChance));
            tag.putInt(TAG_SEANCE_TECHNIQUE_UPGRADE_CHANCE, clampUpgradeChance(seanceTechniqueUpgradeChance));
            tag.putString(TAG_STRENGTHEN_TECHNIQUE_LEVEL, strengthenTechniqueLevel == null
                    ? StrengthenTechniqueLevel.UNLEARNED.name()
                    : strengthenTechniqueLevel);
            tag.putInt(TAG_STRENGTHEN_TECHNIQUE_UPGRADE_CHANCE, clampUpgradeChance(strengthenTechniqueUpgradeChance));
            tag.putBoolean(TAG_STRENGTHEN_ACTIVE, strengthenActive);
            tag.putLong(TAG_LAST_PROCESSED_GAME_DAY, lastProcessedGameDay);
            CompoundTag intelTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : techniqueIntel.entrySet())
            {
                intelTag.putInt(entry.getKey(), Math.max(0, entry.getValue()));
            }
            tag.put(TAG_TECHNIQUE_INTEL, intelTag);
            tag.putInt(TAG_SP_POINTS, Math.max(0, spPoints));
            tag.putInt(TAG_SP_SPENT_LIBERATION, Math.max(0, spSpentLiberation));
            tag.putInt(TAG_SP_SPENT_REFRESH, Math.max(0, spSpentRefresh));
            tag.putInt(TAG_SP_NODE_QUALITY, Math.max(1, spNodeQuality));
            tag.putInt(TAG_SP_LAST_GRANTED_TIER, Math.max(0, spLastGrantedTier));
            tag.put(TAG_SP_LIT_NODES, saveSpNodeList(spLitNodes));
            tag.put(TAG_SP_OFFERS, saveSpNodeList(spOffers));
            tag.putString(TAG_CURSED_ENERGY_TRAIT,
                    cursedEnergyTrait == null ? CursedEnergyTrait.NORMAL.id() : cursedEnergyTrait);
            ListTag seList = new ListTag();
            for (Map.Entry<String, SeEquippedEntry> seEntry : seEquipped.entrySet())
            {
                String seId = seEntry.getKey();
                if (seId == null || seId.isBlank() || seEntry.getValue() == null)
                {
                    continue;
                }
                seList.add(seEntry.getValue().toTag(seId));
            }
            tag.put(TAG_SE_EQUIPPED, seList);
            tag.putInt(TAG_PHYSICAL_POWER_BONUS, Math.max(0, physicalPowerBonus));
            tag.putInt(TAG_TECHNIQUE_POWER_BONUS, Math.max(0, techniquePowerBonus));
            tag.putInt(TAG_BRAIN_DAMAGE, Math.max(0, brainDamage));
            tag.putInt(TAG_DOMAIN_EXPANDS_TODAY, Math.max(0, domainExpandsToday));
            tag.putLong(TAG_DOMAIN_EXPAND_COUNT_DAY, domainExpandCountDay);
            tag.putInt(TAG_DOMAIN_GAUGE, Math.max(0, domainGauge));
            tag.putInt(TAG_MELTDOWN_REPAIRS_TODAY, Math.max(0, meltdownRepairsToday));
            tag.putLong(TAG_MELTDOWN_REPAIR_COUNT_DAY, meltdownRepairCountDay);
            tag.putBoolean(TAG_MELTDOWN_REPAIR_LEARNED, meltdownRepairLearned);
            tag.putBoolean(TAG_MAIN_QUEST_BOOTSTRAPPED, mainQuestBootstrapped);
            ListTag completedQuests = new ListTag();
            for (String questId : completedMainQuests)
            {
                if (questId != null && !questId.isBlank())
                {
                    completedQuests.add(net.minecraft.nbt.StringTag.valueOf(questId));
                }
            }
            tag.put(TAG_COMPLETED_MAIN_QUESTS, completedQuests);
            tag.putString(TAG_TRACKED_MAIN_QUEST, trackedMainQuest == null ? "" : trackedMainQuest);
            tag.putBoolean(TAG_MAIN_11_INTRO_DONE, main11IntroDone);
            tag.putBoolean(TAG_MAIN_11_TALK_DONE, main11TalkDone);
            tag.putBoolean(TAG_MAIN_11_KITTED, main11Kitted);
            if (main11CabinPos != null)
            {
                CompoundTag cabin = new CompoundTag();
                cabin.putInt("x", main11CabinPos.getX());
                cabin.putInt("y", main11CabinPos.getY());
                cabin.putInt("z", main11CabinPos.getZ());
                tag.put(TAG_MAIN_11_CABIN, cabin);
            }
            if (narrativeVillagePos != null)
            {
                CompoundTag village = new CompoundTag();
                village.putInt("x", narrativeVillagePos.getX());
                village.putInt("y", narrativeVillagePos.getY());
                village.putInt("z", narrativeVillagePos.getZ());
                tag.put(TAG_NARRATIVE_VILLAGE, village);
            }
            tag.putBoolean(TAG_MAIN_12_AMBUSH_STARTED, main12AmbushStarted);
            if (main12AmbushOrigin != null)
            {
                CompoundTag ambush = new CompoundTag();
                ambush.putInt("x", main12AmbushOrigin.getX());
                ambush.putInt("y", main12AmbushOrigin.getY());
                ambush.putInt("z", main12AmbushOrigin.getZ());
                tag.put(TAG_MAIN_12_AMBUSH_ORIGIN, ambush);
            }
            tag.putBoolean(TAG_MAIN_12_DECOR_PLACED, main12DecorPlaced);
            tag.putBoolean(TAG_MAIN_12_HEALTH_LOCKED, main12HealthLocked);
            tag.putBoolean(TAG_MAIN_13_PERFORMANCE_DONE, main13PerformanceDone);
            tag.putString(TAG_INNATE_TALENT_ID, innateTalentId == null ? "" : innateTalentId);
            tag.putString(TAG_INNATE_TALENT_SOURCE, innateTalentSource == null ? "" : innateTalentSource);
            tag.putString(TAG_TECHNIQUE_ATK_OBJ_MODE, techniqueAtkObjMode == null ? "" : techniqueAtkObjMode);
            ListTag atkObjIds = new ListTag();
            for (UUID id : techniqueAtkObjIds)
            {
                if (id != null)
                {
                    atkObjIds.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
                }
            }
            tag.put(TAG_TECHNIQUE_ATK_OBJ_IDS, atkObjIds);
            return tag;
        }

        private static void loadSpNodeList(ListTag list, List<SpNodeInstance> target)
        {
            target.clear();
            for (int i = 0; i < list.size(); i++)
            {
                SpNodeInstance node = SpNodeInstance.load(list.getCompound(i));
                if (node != null)
                {
                    target.add(node);
                }
            }
        }

        private static ListTag saveSpNodeList(List<SpNodeInstance> nodes)
        {
            ListTag list = new ListTag();
            for (SpNodeInstance node : nodes)
            {
                if (node != null)
                {
                    list.add(node.save());
                }
            }
            return list;
        }

        private static int clampUpgradeChance(int value)
        {
            return Math.max(0, Math.min(8, value));
        }
    }
}
