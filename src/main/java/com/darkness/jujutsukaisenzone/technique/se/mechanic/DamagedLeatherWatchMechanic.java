package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.CritPowerStackSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 损坏的皮质手表：暴击叠层抬高体术 / 术式强度。
 */
public final class DamagedLeatherWatchMechanic implements CritPowerStackSeMechanic
{
    public static final String SE_ID = "damaged_leather_watch";

    public static final double BONUS_PER_STACK = 0.05D;
    public static final int MAX_STACKS = 5;
    public static final int DURATION_TICKS = 20 * 5;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public double bonusPerStack()
    {
        return BONUS_PER_STACK;
    }

    @Override
    public int maxStacks()
    {
        return MAX_STACKS;
    }

    @Override
    public int durationTicks()
    {
        return DURATION_TICKS;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
