package com.darkness.jujutsukaisenzone.technique.description;

import java.util.Locale;

/**
 * 技能描述中数值与时间的格式化（与战斗 Config 常量一致展示）。
 */
public final class TechniqueSkillStatFormat
{
    private TechniqueSkillStatFormat()
    {
    }

    public static int ticksToSeconds(int ticks)
    {
        return (ticks + 19) / 20;
    }

    public static String formatSecondsFromTicks(int ticks)
    {
        return Integer.toString(ticksToSeconds(ticks));
    }

    public static String formatNumber(float value)
    {
        if (Math.abs(value - Math.round(value)) < 1.0E-4F)
        {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static String formatNumber(double value)
    {
        return formatNumber((float) value);
    }
}
