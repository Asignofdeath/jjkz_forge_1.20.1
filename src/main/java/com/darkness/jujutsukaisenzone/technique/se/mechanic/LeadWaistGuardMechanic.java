package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.CutMarkCapBonusSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 灌铅的护腰：最大生命额外抬高切痕上限，可适度超过默认封顶。
 */
public final class LeadWaistGuardMechanic implements CutMarkCapBonusSeMechanic
{
    public static final String SE_ID = "cut_mark_vital_cap";

    /** 每点最大生命额外贡献的切痕上限。 */
    public static final float EXTRA_CAP_PER_MAX_HEALTH = 0.15F;

    /** 有本 SE 时的软顶（默认硬顶 64）。 */
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
