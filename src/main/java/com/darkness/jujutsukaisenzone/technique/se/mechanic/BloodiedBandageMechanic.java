package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.CutMarkInflictBonusSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 血のにじんだ絆創膏：护甲结算后的加痕 ×1.2。
 */
public final class BloodiedBandageMechanic implements CutMarkInflictBonusSeMechanic
{
    public static final String SE_ID = "cut_mark_inflict_bonus";

    public static final float INFLICT_MULTIPLIER = 1.20F;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public float inflictMultiplier()
    {
        return INFLICT_MULTIPLIER;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
