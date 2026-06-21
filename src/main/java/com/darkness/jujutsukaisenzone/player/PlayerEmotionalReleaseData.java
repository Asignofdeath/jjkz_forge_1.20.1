package com.darkness.jujutsukaisenzone.player;

import net.minecraft.world.entity.player.Player;

/**
 * 「释放情绪」蓄力与冷却状态（服务端权威，存于玩家 PersistentData）。
 */
public final class PlayerEmotionalReleaseData
{
    private static final String TAG_CHARGE_START = "jujutsukaisenzone_emotional_release_charge_start";
    private static final String TAG_COOLDOWN_UNTIL = "jujutsukaisenzone_emotional_release_cooldown_until";
    private static final String TAG_MOUNTED_FX = "jujutsukaisenzone_emotional_release_mounted_fx";
    private static final String TAG_LAST_REGEN_TICK = "jujutsukaisenzone_emotional_release_last_regen_tick";

    private PlayerEmotionalReleaseData()
    {
    }

    public static boolean isCharging(Player player)
    {
        return getChargeStartTick(player) > 0L;
    }

    public static long getChargeStartTick(Player player)
    {
        return player.getPersistentData().getLong(TAG_CHARGE_START);
    }

    public static long getCooldownUntilTick(Player player)
    {
        return player.getPersistentData().getLong(TAG_COOLDOWN_UNTIL);
    }

    public static String getMountedFxId(Player player)
    {
        return player.getPersistentData().getString(TAG_MOUNTED_FX);
    }

    public static long getLastRegenTick(Player player)
    {
        return player.getPersistentData().getLong(TAG_LAST_REGEN_TICK);
    }

    public static void beginCharge(Player player, long gameTime, String fxId)
    {
        player.getPersistentData().putLong(TAG_CHARGE_START, gameTime);
        player.getPersistentData().putString(TAG_MOUNTED_FX, fxId == null ? "" : fxId);
        player.getPersistentData().putLong(TAG_LAST_REGEN_TICK, gameTime);
    }

    public static void setCooldownUntil(Player player, long gameTime)
    {
        player.getPersistentData().putLong(TAG_COOLDOWN_UNTIL, gameTime);
    }

    public static void setLastRegenTick(Player player, long gameTime)
    {
        player.getPersistentData().putLong(TAG_LAST_REGEN_TICK, gameTime);
    }

    public static void clearCharge(Player player)
    {
        player.getPersistentData().remove(TAG_CHARGE_START);
        player.getPersistentData().remove(TAG_MOUNTED_FX);
        player.getPersistentData().remove(TAG_LAST_REGEN_TICK);
    }

    public static void clearAll(Player player)
    {
        clearCharge(player);
        player.getPersistentData().remove(TAG_COOLDOWN_UNTIL);
    }

    public static boolean isOnCooldown(Player player, long gameTime)
    {
        long until = getCooldownUntilTick(player);
        return until > 0L && gameTime < until;
    }
}
