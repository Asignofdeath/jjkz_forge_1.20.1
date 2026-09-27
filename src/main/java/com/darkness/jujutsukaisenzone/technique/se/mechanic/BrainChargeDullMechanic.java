package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.DomainChargeCurveSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 填料脱落的耳塞：中度脑损对领域充能的拖累减半；重度（75+）与满脑损规则不变。
 */
public final class BrainChargeDullMechanic implements DomainChargeCurveSeMechanic
{
    public static final String SE_ID = "brain_charge_dull";

    /** 惩罚保留比例：0.5 = 惩罚减半（效率更接近 1）。 */
    public static final double MID_TIER_PENALTY_RETAIN = 0.5D;

    /** 从此档起不再软化（含）。 */
    public static final int FULL_PENALTY_FROM = 75;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public double adjustBrainTierEfficiency(int brainDamage, double defaultEfficiency)
    {
        if (brainDamage >= FULL_PENALTY_FROM)
        {
            return defaultEfficiency;
        }
        if (brainDamage < 25 || defaultEfficiency >= 1.0D)
        {
            return defaultEfficiency;
        }
        return 1.0D - (1.0D - defaultEfficiency) * MID_TIER_PENALTY_RETAIN;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
