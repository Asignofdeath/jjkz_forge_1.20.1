package com.darkness.jujutsukaisenzone.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * 累计挖掉的原木数量，用于触发【罪恶者】与「盗伐林木」罪行记录。
 */
public final class PlayerIllegalLoggingData
{
    private static final String TAG_ROOT = "jujutsukaisenzone_illegal_logging";
    private static final String TAG_LOGS_BROKEN = "logsBroken";

    /** 触发一次盗伐林木罪所需挖掉的原木数量。 */
    public static final int LOGS_PER_CRIME = 50;
    public static final String CRIME_DESCRIPTION = "盗伐林木";

    private PlayerIllegalLoggingData()
    {
    }

    public static int getLogsBroken(ServerPlayer player)
    {
        return Math.max(0, player.getPersistentData().getCompound(TAG_ROOT).getInt(TAG_LOGS_BROKEN));
    }

    public static void setLogsBroken(ServerPlayer player, int count)
    {
        CompoundTag root = player.getPersistentData().getCompound(TAG_ROOT);
        root.putInt(TAG_LOGS_BROKEN, Math.max(0, count));
        player.getPersistentData().put(TAG_ROOT, root);
    }

    public static int incrementAndGet(ServerPlayer player)
    {
        int count = getLogsBroken(player) + 1;
        setLogsBroken(player, count);
        return count;
    }
}
