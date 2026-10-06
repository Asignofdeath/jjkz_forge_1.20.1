package com.darkness.jujutsukaisenzone.faction;

import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;

/**
 * 阵营 id 解析。玩家走存档；本模组自定义实体手动写入 persistent data；
 * 其它生物只作标签、不落盘。
 */
public final class JjkzFactions
{
    public static final String SORCERER = PlayerFactionData.FACTION_SORCERER;
    public static final String CURSE_USER = PlayerFactionData.FACTION_CURSE_USER;
    public static final String CURSE_SPIRIT = PlayerFactionData.FACTION_CURSE_SPIRIT;
    public static final String CIVILIAN = PlayerFactionData.FACTION_CIVILIAN;
    public static final String MYSTERIOUS = "mysterious";
    public static final String TAG = "jujutsukaisenzone_faction";

    private JjkzFactions()
    {
    }

    public static String resolve(LivingEntity entity)
    {
        if (entity instanceof Player player)
        {
            String id = PlayerFactionData.getFactionId(player);
            return id == null || id.isEmpty() ? CIVILIAN : id;
        }
        String assigned = assigned(entity);
        if (!assigned.isEmpty())
        {
            return assigned;
        }
        return isFriendlyMob(entity) ? CIVILIAN : MYSTERIOUS;
    }

    public static String assigned(LivingEntity entity)
    {
        return entity.getPersistentData().getString(TAG);
    }

    public static void assign(LivingEntity entity, String factionId)
    {
        if (entity == null || factionId == null || factionId.isEmpty())
        {
            return;
        }
        entity.getPersistentData().putString(TAG, factionId);
    }

    public static boolean isThree(String factionId)
    {
        return SORCERER.equals(factionId) || CURSE_USER.equals(factionId) || CURSE_SPIRIT.equals(factionId);
    }

    public static void copyToSave(LivingEntity entity, CompoundTag tag)
    {
        String assigned = assigned(entity);
        if (!assigned.isEmpty())
        {
            tag.putString(TAG, assigned);
        }
    }

    public static void restoreFromSave(LivingEntity entity, CompoundTag tag)
    {
        if (tag.contains(TAG))
        {
            entity.getPersistentData().putString(TAG, tag.getString(TAG));
        }
    }

    private static boolean isFriendlyMob(LivingEntity entity)
    {
        MobCategory category = entity.getType().getCategory();
        return category == MobCategory.CREATURE
                || category == MobCategory.AMBIENT
                || category == MobCategory.AXOLOTLS
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE;
    }
}
