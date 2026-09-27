package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.LimitlessNearCasterExemptSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 最强的证明：移除无量空处「身旁幸免」——挨着施法者的人也会吃领域锁定。
 */
public final class StrongestProofMechanic implements LimitlessNearCasterExemptSeMechanic
{
    public static final String SE_ID = "strongest_proof";

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public boolean removesNearCasterExempt()
    {
        return true;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
