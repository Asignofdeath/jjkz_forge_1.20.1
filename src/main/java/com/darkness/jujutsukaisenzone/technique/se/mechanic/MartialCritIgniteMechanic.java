package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.MartialCritSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 旧烟管：体术暴击后点燃目标。
 */
public final class MartialCritIgniteMechanic implements MartialCritSeMechanic
{
    public static final String SE_ID = "martial_crit_ignite";
    private static final int IGNITE_SECONDS = 4;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public int igniteSeconds()
    {
        return IGNITE_SECONDS;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID,
                IGNITE_SECONDS);
    }
}
