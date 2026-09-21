package com.darkness.jujutsukaisenzone.effect;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 判定是否属于「非法穿越边境」：从主世界进入下界或末地。
 */
public final class IllegalBorderCrossingRules
{
    public static final String TO_NETHER_DESCRIPTION = "进入下界";
    public static final String TO_END_DESCRIPTION = "进入末地";

    private IllegalBorderCrossingRules()
    {
    }

    public static String getCrimeDescription(ResourceKey<Level> from, ResourceKey<Level> to)
    {
        if (from != Level.OVERWORLD)
        {
            return null;
        }
        if (to == Level.NETHER)
        {
            return TO_NETHER_DESCRIPTION;
        }
        if (to == Level.END)
        {
            return TO_END_DESCRIPTION;
        }
        return null;
    }
}
