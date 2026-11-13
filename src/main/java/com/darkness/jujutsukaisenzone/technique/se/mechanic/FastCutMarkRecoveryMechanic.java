package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.CutMarkRecoverySeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 干净的纱布卷：切痕自然消退更快。
 */
public final class FastCutMarkRecoveryMechanic implements CutMarkRecoverySeMechanic
{
    public static final String SE_ID = "fast_cut_mark_recovery";

    public static final int DECAY_INTERVAL_TICKS = 20;
    public static final int DECAY_AMOUNT = 2;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public int decayIntervalTicks()
    {
        return DECAY_INTERVAL_TICKS;
    }

    @Override
    public int decayAmount()
    {
        return DECAY_AMOUNT;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
