package com.darkness.jujutsukaisenzone.player;

import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 长按蓄力、松手释放类技能的状态（如御厨子「灶」开）。
 */
public final class PlayerHoldReleaseChargeData
{
    public static final String TAG_ENTITY_UUID = "jujutsukaisenzone_hold_release_entity_uuid";
    public static final String TAG_CHARGE_START_TICK = "jujutsukaisenzone_hold_release_charge_start_tick";
    public static final String TAG_SLOT = "jujutsukaisenzone_hold_release_slot";
    public static final String TAG_SKILL_ID = "jujutsukaisenzone_hold_release_skill_id";

    private PlayerHoldReleaseChargeData()
    {
    }

    public static boolean isCharging(Player player)
    {
        return player.getPersistentData().hasUUID(TAG_ENTITY_UUID);
    }

    public static UUID getChargeEntityUuid(Player player)
    {
        return player.getPersistentData().getUUID(TAG_ENTITY_UUID);
    }

    public static long getChargeStartGameTick(Player player)
    {
        return player.getPersistentData().getLong(TAG_CHARGE_START_TICK);
    }

    public static int getChargingSlot(Player player)
    {
        return Math.max(0, player.getPersistentData().getInt(TAG_SLOT));
    }

    public static String getChargingSkillId(Player player)
    {
        return player.getPersistentData().getString(TAG_SKILL_ID);
    }

    public static void beginCharge(Player player, int slot, String skillId, UUID entityUuid, long startGameTick)
    {
        player.getPersistentData().putInt(TAG_SLOT, slot);
        player.getPersistentData().putString(TAG_SKILL_ID, skillId == null ? "" : skillId);
        player.getPersistentData().putUUID(TAG_ENTITY_UUID, entityUuid);
        player.getPersistentData().putLong(TAG_CHARGE_START_TICK, startGameTick);
    }

    public static void clear(Player player)
    {
        player.getPersistentData().remove(TAG_ENTITY_UUID);
        player.getPersistentData().remove(TAG_CHARGE_START_TICK);
        player.getPersistentData().remove(TAG_SLOT);
        player.getPersistentData().remove(TAG_SKILL_ID);
    }
}
