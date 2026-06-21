package com.darkness.jujutsukaisenzone.advancement;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class HeavenlyRestrictionAdvancements
{
    private static final ResourceLocation HEAVENLY_RESTRICTION = ResourceLocation.fromNamespaceAndPath(
            JuJutsuKaisenZone.MODID,
            "heavenly_restriction"
    );

    private HeavenlyRestrictionAdvancements()
    {
    }

    public static void grant(ServerPlayer player)
    {
        Advancement advancement = player.server.getAdvancements().getAdvancement(HEAVENLY_RESTRICTION);
        if (advancement == null)
        {
            return;
        }
        var progress = player.getAdvancements();
        for (String criterion : advancement.getCriteria().keySet())
        {
            progress.award(advancement, criterion);
        }
    }
}
