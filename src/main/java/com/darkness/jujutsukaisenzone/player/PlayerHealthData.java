package com.darkness.jujutsukaisenzone.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家生命值持久化：镜像实体当前血量至 {@link JjkzPlayerDataStorage}，供存档与 {@code /jjkz data} 查询。
 */
public final class PlayerHealthData
{
    private static final String TAG_CURRENT = "jujutsukaisenzone_health_current";

    private PlayerHealthData()
    {
    }

    public static float getMax(Player player)
    {
        return player.getMaxHealth();
    }

    public static double getBonus(Player player)
    {
        return CursedGradeBonuses.getEffectiveHealthBonus(player);
    }

    public static float getCurrent(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return getServerData(serverPlayer).healthCurrent;
        }
        return player.getPersistentData().getFloat(TAG_CURRENT);
    }

    /** 重生时将当前生命设为上限，并写入 SavedData */
    public static void restoreToMax(ServerPlayer player)
    {
        float max = player.getMaxHealth();
        player.setHealth(max);
        JjkzPlayerDataStorage.PlayerDataEntry data = getServerData(player);
        data.healthInitialized = true;
        data.healthCurrent = max;
        markDirty(player);
        cacheForClient(player, max);
    }

    /**
     * 最大生命值变动后，按原有百分比重算当前生命值。
     * 例：20/100（20%）→ 上限变为 200 时，当前生命变为 40。
     */
    public static void scaleHealthAfterMaxChange(ServerPlayer player, float oldMax, float oldHealth)
    {
        float newMax = player.getMaxHealth();
        if (newMax <= 0.0F)
        {
            return;
        }

        float newHealth;
        if (Math.abs(newMax - oldMax) <= 0.001F)
        {
            newHealth = Math.min(oldHealth, newMax);
        }
        else
        {
            float ratio = oldMax > 0.001F ? oldHealth / oldMax : 1.0F;
            newHealth = Mth.clamp(newMax * ratio, 0.0F, newMax);
        }

        if (Math.abs(player.getHealth() - newHealth) > 0.001F)
        {
            player.setHealth(newHealth);
        }

        JjkzPlayerDataStorage.PlayerDataEntry data = getServerData(player);
        data.healthInitialized = true;
        data.healthCurrent = newHealth;
        markDirty(player);
        cacheForClient(player, newHealth);
    }

    /** 首次写入或实体血量变化时同步至 SavedData */
    public static void syncFromEntity(ServerPlayer player)
    {
        float entityHealth = player.getHealth();
        JjkzPlayerDataStorage.PlayerDataEntry data = getServerData(player);
        if (!data.healthInitialized)
        {
            data.healthInitialized = true;
            data.healthCurrent = entityHealth;
            markDirty(player);
            cacheForClient(player, entityHealth);
            return;
        }
        if (Math.abs(data.healthCurrent - entityHealth) > 0.001F)
        {
            data.healthCurrent = entityHealth;
            markDirty(player);
            cacheForClient(player, entityHealth);
        }
    }

    private static JjkzPlayerDataStorage.PlayerDataEntry getServerData(ServerPlayer player)
    {
        return JjkzPlayerDataStorage.get(player).getOrCreate(player);
    }

    private static void markDirty(ServerPlayer player)
    {
        JjkzPlayerDataStorage.get(player).setDirty();
    }

    private static void cacheForClient(Player player, float current)
    {
        player.getPersistentData().putFloat(TAG_CURRENT, current);
    }
}
