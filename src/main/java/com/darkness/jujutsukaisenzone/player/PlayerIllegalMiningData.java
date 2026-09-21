package com.darkness.jujutsukaisenzone.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * 累计挖掉的矿石方块数量，用于触发【罪恶者】与「非法采矿」罪行记录。
 */
public final class PlayerIllegalMiningData
{
    private static final String TAG_ROOT = "jujutsukaisenzone_illegal_mining";
    private static final String TAG_ORES_BROKEN = "oresBroken";

    /** 触发一次非法采矿罪所需挖掉的矿石方块数量（超过 10 颗，即第 11 颗起算一轮）。 */
    public static final int ORES_PER_CRIME = 11;
    public static final String CRIME_DESCRIPTION = "非法采矿";

    private PlayerIllegalMiningData()
    {
    }

    public static int getOresBroken(ServerPlayer player)
    {
        return Math.max(0, player.getPersistentData().getCompound(TAG_ROOT).getInt(TAG_ORES_BROKEN));
    }

    public static void setOresBroken(ServerPlayer player, int count)
    {
        CompoundTag root = player.getPersistentData().getCompound(TAG_ROOT);
        root.putInt(TAG_ORES_BROKEN, Math.max(0, count));
        player.getPersistentData().put(TAG_ROOT, root);
    }

    public static int incrementAndGet(ServerPlayer player)
    {
        int count = getOresBroken(player) + 1;
        setOresBroken(player, count);
        return count;
    }
}
