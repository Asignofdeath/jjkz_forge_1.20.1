package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.MeltdownRepairSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 半融的冰袋：用反转修熔断时咒力更省、脑损更轻。
 */
public final class MeltdownSoftResetMechanic implements MeltdownRepairSeMechanic
{
    public static final String SE_ID = "meltdown_soft_reset";

    /** 修复咒力六成（约 −40%）。 */
    public static final double COST_MULTIPLIER = 0.6D;

    /** 修复脑损七成（约 −30%）。 */
    public static final double BRAIN_GAIN_MULTIPLIER = 0.7D;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public double repairCostMultiplier()
    {
        return COST_MULTIPLIER;
    }

    @Override
    public double repairBrainGainMultiplier()
    {
        return BRAIN_GAIN_MULTIPLIER;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
