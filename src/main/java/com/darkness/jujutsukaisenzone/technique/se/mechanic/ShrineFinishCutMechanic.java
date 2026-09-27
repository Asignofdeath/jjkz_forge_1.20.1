package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.ShrineFinishCutSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 缺口的俎板：解命中留下开刀印，可对印记目标远程收刀（捌）。
 */
public final class ShrineFinishCutMechanic implements ShrineFinishCutSeMechanic
{
    public static final String SE_ID = "shrine_finish_the_cut";
    public static final int MARK_DURATION_TICKS = 100;
    public static final double REMOTE_CAST_RANGE = 24.0D;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public int markDurationTicks()
    {
        return MARK_DURATION_TICKS;
    }

    @Override
    public double remoteCastRange()
    {
        return REMOTE_CAST_RANGE;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
