package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
        private static final String TAG_PHYSICAL_POWER_BONUS = "physicalPowerBonus";
        private static final String TAG_TECHNIQUE_POWER_BONUS = "techniquePowerBonus";

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
        int physicalPowerBonus;
        int techniquePowerBonus;

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
            entry.physicalPowerBonus = Math.max(0, tag.getInt(TAG_PHYSICAL_POWER_BONUS));
            entry.techniquePowerBonus = Math.max(0, tag.getInt(TAG_TECHNIQUE_POWER_BONUS));
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
            tag.putInt(TAG_PHYSICAL_POWER_BONUS, Math.max(0, physicalPowerBonus));
            tag.putInt(TAG_TECHNIQUE_POWER_BONUS, Math.max(0, techniquePowerBonus));
            return tag;
        }

        private static int clampUpgradeChance(int value)
        {
            return Math.max(0, Math.min(8, value));
        }
    }
}
