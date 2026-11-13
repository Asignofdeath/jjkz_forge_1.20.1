package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.CutMarkCapSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 沉甸甸的护腰：生命越高，切痕上限越高（可适度超过默认硬顶）。
 */
public final class VitalCutMarkCapMechanic implements CutMarkCapSeMechanic
{
    public static final String SE_ID = "vital_cut_mark_cap";

    public static final float EXTRA_CAP_PER_MAX_HEALTH = 0.15F;
    public static final int SOFT_CAP_MAX = 80;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public float extraCapPerMaxHealth()
    {
        return EXTRA_CAP_PER_MAX_HEALTH;
    }

    @Override
    public int softCapMax()
    {
        return SOFT_CAP_MAX;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
