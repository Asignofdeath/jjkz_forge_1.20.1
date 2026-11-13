package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.CutMarkBaseBonusSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 誰かの古びた人形：加痕时 baseMarks +2。
 */
public final class WornDollMechanic implements CutMarkBaseBonusSeMechanic
{
    public static final String SE_ID = "cut_mark_base_bonus";

    public static final float BONUS_BASE_MARKS = 2.0F;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public float bonusBaseMarks()
    {
        return BONUS_BASE_MARKS;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
