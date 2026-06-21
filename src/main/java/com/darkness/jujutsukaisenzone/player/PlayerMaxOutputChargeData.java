package com.darkness.jujutsukaisenzone.player;

import net.minecraft.world.entity.player.Player;

/**
 * 「最大输出」蓄力状态：服务端在按键按下时记录开始刻，松手后校验是否蓄满。
 */
public final class PlayerMaxOutputChargeData
{
    public static final String TAG_CHARGE_SLOT = "jujutsukaisenzone_max_output_charge_slot";
    public static final String TAG_CHARGE_START_TICK = "jujutsukaisenzone_max_output_charge_start_tick";
    public static final String TAG_CHARGE_TECHNIQUE_ID = "jujutsukaisenzone_max_output_charge_technique_id";

    private PlayerMaxOutputChargeData()
    {
    }

    public static int getChargingSlot(Player player)
    {
        return Math.max(0, player.getPersistentData().getInt(TAG_CHARGE_SLOT));
    }

    public static long getChargeStartGameTick(Player player)
    {
        return player.getPersistentData().getLong(TAG_CHARGE_START_TICK);
    }

    public static boolean isCharging(Player player)
    {
        return getChargingSlot(player) > 0;
    }

    public static void beginCharge(Player player, int slot, long startGameTick, String techniqueId)
    {
        player.getPersistentData().putInt(TAG_CHARGE_SLOT, slot);
        player.getPersistentData().putLong(TAG_CHARGE_START_TICK, startGameTick);
        player.getPersistentData().putString(TAG_CHARGE_TECHNIQUE_ID, techniqueId == null ? "" : techniqueId);
    }

    public static String getChargeTechniqueId(Player player)
    {
        return player.getPersistentData().getString(TAG_CHARGE_TECHNIQUE_ID);
    }

    public static void clear(Player player)
    {
        player.getPersistentData().remove(TAG_CHARGE_SLOT);
        player.getPersistentData().remove(TAG_CHARGE_START_TICK);
        player.getPersistentData().remove(TAG_CHARGE_TECHNIQUE_ID);
    }
}
