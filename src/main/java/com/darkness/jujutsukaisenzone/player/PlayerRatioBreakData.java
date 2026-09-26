package com.darkness.jujutsukaisenzone.player;

import net.minecraft.world.entity.player.Player;

/**
 * 十划咒法「弱点击破」计数：挂在施术者身上，供一文字 / 瓦落等后续技能消费。
 */
public final class PlayerRatioBreakData
{
    public static final int MAX_BREAKS = 8;
    private static final String TAG_BREAK_COUNT = "jujutsukaisenzone_ratio_break_count";

    private PlayerRatioBreakData()
    {
    }

    public static int get(Player player)
    {
        return Math.max(0, player.getPersistentData().getInt(TAG_BREAK_COUNT));
    }

    public static void set(Player player, int value)
    {
        player.getPersistentData().putInt(TAG_BREAK_COUNT, Math.max(0, Math.min(MAX_BREAKS, value)));
    }

    public static int add(Player player, int delta)
    {
        int next = get(player) + delta;
        set(player, next);
        return get(player);
    }

    public static int consume(Player player, int amount)
    {
        int have = get(player);
        int used = Math.min(have, Math.max(0, amount));
        set(player, have - used);
        return used;
    }

    public static void clear(Player player)
    {
        player.getPersistentData().remove(TAG_BREAK_COUNT);
    }
}
