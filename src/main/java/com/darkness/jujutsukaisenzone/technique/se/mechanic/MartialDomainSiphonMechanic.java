package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.DomainGaugeSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 裂纹算盘：体术命中也灌领域量表（半额，与术式共用命中冷却）。
 */
public final class MartialDomainSiphonMechanic implements DomainGaugeSeMechanic
{
    public static final String SE_ID = "martial_domain_siphon";
    /** 相对术式命中灌充基数的比例。 */
    public static final float MARTIAL_CHARGE_RATIO = 0.5F;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public float martialChargeRatio()
    {
        return MARTIAL_CHARGE_RATIO;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
