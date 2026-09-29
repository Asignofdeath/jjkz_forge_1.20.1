package com.darkness.jujutsukaisenzone.player;

import net.minecraft.world.entity.player.Player;

/**
 * 式神模式冷却，存主人 persistentData。
 * <ul>
 *   <li>槽位公用桶（主动 / BOTH）：{@code jujutsukaisenzone_shikigami_cd_<modeId>_<shikigamiSlotId>}</li>
 *   <li>AUTO 招独立桶：{@code jujutsukaisenzone_shikigami_cd_ability_<modeId>_<abilityId>}</li>
 * </ul>
 */
public final class PlayerShikigamiCooldownData
{
    private static final String PREFIX = "jujutsukaisenzone_shikigami_cd_";
    private static final String ABILITY_PREFIX = "jujutsukaisenzone_shikigami_cd_ability_";

    private PlayerShikigamiCooldownData()
    {
    }

    public static String tagKey(String modeId, String shikigamiSlotId)
    {
        return PREFIX + modeId + "_" + shikigamiSlotId;
    }

    public static String abilityTagKey(String modeId, String abilityId)
    {
        return ABILITY_PREFIX + modeId + "_" + abilityId;
    }

    public static int getRemainingTicks(Player player, String modeId, String shikigamiSlotId, long now)
    {
        return remaining(player, tagKey(modeId, shikigamiSlotId), now);
    }

    public static int getAbilityRemainingTicks(Player player, String modeId, String abilityId, long now)
    {
        return remaining(player, abilityTagKey(modeId, abilityId), now);
    }

    public static boolean isReady(Player player, String modeId, String shikigamiSlotId, long now)
    {
        return getRemainingTicks(player, modeId, shikigamiSlotId, now) <= 0;
    }

    public static boolean isAbilityReady(Player player, String modeId, String abilityId, long now)
    {
        return getAbilityRemainingTicks(player, modeId, abilityId, now) <= 0;
    }

    public static void setCooldown(Player player, String modeId, String shikigamiSlotId, long expireTick)
    {
        putExpire(player, tagKey(modeId, shikigamiSlotId), expireTick);
    }

    public static void setAbilityCooldown(Player player, String modeId, String abilityId, long expireTick)
    {
        putExpire(player, abilityTagKey(modeId, abilityId), expireTick);
    }

    public static void clear(Player player, String modeId, String shikigamiSlotId)
    {
        if (player == null)
        {
            return;
        }
        player.getPersistentData().remove(tagKey(modeId, shikigamiSlotId));
    }

    public static void clearAbility(Player player, String modeId, String abilityId)
    {
        if (player == null)
        {
            return;
        }
        player.getPersistentData().remove(abilityTagKey(modeId, abilityId));
    }

    private static int remaining(Player player, String key, long now)
    {
        if (player == null)
        {
            return 0;
        }
        long expire = player.getPersistentData().getLong(key);
        return expire > now ? (int) (expire - now) : 0;
    }

    private static void putExpire(Player player, String key, long expireTick)
    {
        if (player == null)
        {
            return;
        }
        player.getPersistentData().putLong(key, expireTick);
    }
}
