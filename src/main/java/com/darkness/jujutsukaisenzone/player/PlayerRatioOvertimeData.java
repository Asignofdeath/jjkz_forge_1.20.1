package com.darkness.jujutsukaisenzone.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 十划「加班」作息：违约标记、工作效率窗口。
 */
public final class PlayerRatioOvertimeData
{
    private static final String TAG_VOW_BROKEN_DAY = "jujutsukaisenzone_ratio_vow_broken_day";
    private static final String TAG_EFFICIENCY_UNTIL = "jujutsukaisenzone_ratio_efficiency_until";
    private static final String TAG_EFFICIENCY_BONUS = "jujutsukaisenzone_ratio_efficiency_bonus";
    private static final String TAG_EFFICIENCY_LAST_CE = "jujutsukaisenzone_ratio_efficiency_last_ce";

    /** 工作效率体术攻击力加成上限（属性点数）。 */
    public static final double EFFICIENCY_BONUS_CAP = 20.0D;
    public static final int EFFICIENCY_DURATION_TICKS = 20 * 10;

    private PlayerRatioOvertimeData()
    {
    }

    /** 游戏日编号（每 24000 tick 一天）。 */
    public static long dayIndex(Level level)
    {
        return Math.floorDiv(level.getDayTime(), 24000L);
    }

    public static boolean isVowBroken(Player player)
    {
        if (!player.getPersistentData().contains(TAG_VOW_BROKEN_DAY))
        {
            return false;
        }
        long brokenDay = player.getPersistentData().getLong(TAG_VOW_BROKEN_DAY);
        return brokenDay == dayIndex(player.level());
    }

    /** 声明今日违约；持续到下一个游戏日。 */
    public static void breakVow(Player player)
    {
        player.getPersistentData().putLong(TAG_VOW_BROKEN_DAY, dayIndex(player.level()));
        clearEfficiency(player);
    }

    public static void startEfficiency(Player player)
    {
        long until = player.level().getGameTime() + EFFICIENCY_DURATION_TICKS;
        player.getPersistentData().putLong(TAG_EFFICIENCY_UNTIL, until);
        player.getPersistentData().putDouble(TAG_EFFICIENCY_BONUS, 0.0D);
        player.getPersistentData().putInt(TAG_EFFICIENCY_LAST_CE, PlayerCursedEnergyData.getCurrent(player));
    }

    public static boolean isEfficiencyActive(Player player)
    {
        long until = player.getPersistentData().getLong(TAG_EFFICIENCY_UNTIL);
        return until > player.level().getGameTime();
    }

    public static double getEfficiencyBonus(Player player)
    {
        if (!isEfficiencyActive(player))
        {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(EFFICIENCY_BONUS_CAP,
                player.getPersistentData().getDouble(TAG_EFFICIENCY_BONUS)));
    }

    public static void addEfficiencyBonus(Player player, double delta)
    {
        if (!isEfficiencyActive(player) || delta <= 0.0D)
        {
            return;
        }
        double next = Math.min(EFFICIENCY_BONUS_CAP, getEfficiencyBonus(player) + delta);
        player.getPersistentData().putDouble(TAG_EFFICIENCY_BONUS, next);
    }

    public static int getAndUpdateLastCe(Player player)
    {
        int last = player.getPersistentData().getInt(TAG_EFFICIENCY_LAST_CE);
        int now = PlayerCursedEnergyData.getCurrent(player);
        player.getPersistentData().putInt(TAG_EFFICIENCY_LAST_CE, now);
        return last;
    }

    public static void clearEfficiency(Player player)
    {
        player.getPersistentData().remove(TAG_EFFICIENCY_UNTIL);
        player.getPersistentData().remove(TAG_EFFICIENCY_BONUS);
        player.getPersistentData().remove(TAG_EFFICIENCY_LAST_CE);
    }
}
