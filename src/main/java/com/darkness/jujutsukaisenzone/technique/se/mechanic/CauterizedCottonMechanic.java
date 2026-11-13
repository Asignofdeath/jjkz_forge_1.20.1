package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.DismembermentBleedImmuneSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 烧焦的止血棉：断肢后不流血。
 */
public final class CauterizedCottonMechanic implements DismembermentBleedImmuneSeMechanic
{
    public static final String SE_ID = "dismemberment_bleed_immune";

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
