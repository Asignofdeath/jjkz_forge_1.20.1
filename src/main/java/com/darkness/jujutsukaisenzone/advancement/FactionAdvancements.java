package com.darkness.jujutsukaisenzone.advancement;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class FactionAdvancements
{
    private static final ResourceLocation BECOME_SORCERER =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "become_sorcerer");
    private static final ResourceLocation BECOME_CURSE_USER =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "become_curse_user");
    private static final ResourceLocation BECOME_CURSE_SPIRIT =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "become_curse_spirit");

    private FactionAdvancements()
    {
    }

    /**
     * 在玩家获得对应阵营时授予进度（咒术师 / 诅咒师 / 咒灵）。
     */
    public static void grantForFaction(ServerPlayer player, String factionId)
    {
        ResourceLocation advancementId = switch (factionId)
        {
            case PlayerFactionData.FACTION_SORCERER -> BECOME_SORCERER;
            case PlayerFactionData.FACTION_CURSE_USER -> BECOME_CURSE_USER;
            case PlayerFactionData.FACTION_CURSE_SPIRIT -> BECOME_CURSE_SPIRIT;
            default -> null;
        };
        if (advancementId == null)
        {
            return;
        }
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null)
        {
            return;
        }
        var advancements = player.getAdvancements();
        for (String criterion : advancement.getCriteria().keySet())
        {
            advancements.award(advancement, criterion);
        }
    }
}
