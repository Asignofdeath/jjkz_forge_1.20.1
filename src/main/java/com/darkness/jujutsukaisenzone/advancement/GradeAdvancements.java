package com.darkness.jujutsukaisenzone.advancement;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class GradeAdvancements
{
    private static final int[] THRESHOLDS = {
            PlayerCursedGradeData.XP_GRADE_4,
            PlayerCursedGradeData.XP_GRADE_3,
            PlayerCursedGradeData.XP_GRADE_2,
            PlayerCursedGradeData.XP_GRADE_1,
            PlayerCursedGradeData.XP_SPECIAL
    };

    private static final String[] ID_SUFFIXES = {"grade_4", "grade_3", "grade_2", "grade_1", "special"};

    private GradeAdvancements()
    {
    }

    /**
     * 按当前阵营与咒术经验，授予已达成的等级进度（咒术师 / 诅咒师 / 咒灵各一条链）。
     */
    public static void syncForPlayer(ServerPlayer player)
    {
        String prefix = factionAdvancementPrefix(PlayerFactionData.getFactionId(player));
        if (prefix == null)
        {
            return;
        }
        int xp = PlayerCursedGradeData.getExperience(player);
        for (int i = 0; i < THRESHOLDS.length; i++)
        {
            if (xp >= THRESHOLDS[i])
            {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                        JuJutsuKaisenZone.MODID,
                        prefix + "_" + ID_SUFFIXES[i]
                );
                grantAdvancement(player, id);
            }
        }
    }

    private static String factionAdvancementPrefix(String factionId)
    {
        return switch (factionId)
        {
            case PlayerFactionData.FACTION_SORCERER -> "sorcerer";
            case PlayerFactionData.FACTION_CURSE_USER -> "curse_user";
            case PlayerFactionData.FACTION_CURSE_SPIRIT -> "curse_spirit";
            default -> null;
        };
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation advancementId)
    {
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
