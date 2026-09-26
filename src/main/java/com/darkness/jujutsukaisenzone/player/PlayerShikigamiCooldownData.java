package com.darkness.jujutsukaisenzone.player;

import net.minecraft.world.entity.player.Player;

/**
 * 式神模式公用 CD：每「模式 + 式神槽」一桶，存主人 persistentData。
 * <p>
 * 键：{@code jujutsukaisenzone_shikigami_cd_<modeId>_<shikigamiSlotId>} → expireTick。
 */
public final class PlayerShikigamiCooldownData
{
    private static final String PREFIX = "jujutsukaisenzone_shikigami_cd_";

    private PlayerShikigamiCooldownData()
    {
    }

    public static String tagKey(String modeId, String shikigamiSlotId)
    {
        return PREFIX + modeId + "_" + shikigamiSlotId;
    }

    public static int getRemainingTicks(Player player, String modeId, String shikigamiSlotId, long now)
    {
        if (player == null)
        {
            return 0;
        }
        long expire = player.getPersistentData().getLong(tagKey(modeId, shikigamiSlotId));
        return expire > now ? (int) (expire - now) : 0;
    }

    public static boolean isReady(Player player, String modeId, String shikigamiSlotId, long now)
    {
        return getRemainingTicks(player, modeId, shikigamiSlotId, now) <= 0;
    }

    public static void setCooldown(Player player, String modeId, String shikigamiSlotId, long expireTick)
    {
        if (player == null)
        {
            return;
        }
        player.getPersistentData().putLong(tagKey(modeId, shikigamiSlotId), expireTick);
    }

    public static void clear(Player player, String modeId, String shikigamiSlotId)
    {
        if (player == null)
        {
            return;
        }
        player.getPersistentData().remove(tagKey(modeId, shikigamiSlotId));
    }
}
