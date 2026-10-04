package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.advancement.FactionAdvancements;
import com.darkness.jujutsukaisenzone.advancement.GradeAdvancements;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

public final class PlayerFactionData
{
    public static final String FACTION_SORCERER = "sorcerer";
    public static final String FACTION_CURSE_USER = "curse_user";
    public static final String FACTION_CURSE_SPIRIT = "curse_spirit";
    public static final String FACTION_CIVILIAN = "civilian";

    private static final String TAG_FACTION = "jujutsukaisenzone_faction";

    private PlayerFactionData()
    {
    }

    public static boolean hasFaction(Player player)
    {
        String id = getFactionId(player);
        return !id.isEmpty();
    }

    /** 咒术师、诅咒师、咒灵可绑定生得术式；平民或未选阵营不可。 */
    public static boolean canBindInnateTechnique(Player player)
    {
        return switch (getFactionId(player))
        {
            case FACTION_SORCERER, FACTION_CURSE_USER, FACTION_CURSE_SPIRIT -> true;
            default -> false;
        };
    }

    public static String getFactionId(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).factionId;
        }
        return player.getPersistentData().getString(TAG_FACTION);
    }

    public static void setFaction(Player player, String factionId)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer);
            data.factionId = factionId == null ? "" : factionId;
            JjkzPlayerDataStorage.get(serverPlayer).setDirty();
            FactionAdvancements.grantForFaction(serverPlayer, factionId);
            ModNetwork.sendFactionSync(serverPlayer, factionId);
            GradeAdvancements.syncForPlayer(serverPlayer);
            return;
        }
        player.getPersistentData().putString(TAG_FACTION, factionId == null ? "" : factionId);
    }

    public static String normalizeFactionInput(String raw)
    {
        if (raw == null)
        {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("【") && s.endsWith("】") && s.length() > 2)
        {
            s = s.substring(1, s.length() - 1).trim();
        }
        return switch (s)
        {
            case "咒术师" -> FACTION_SORCERER;
            case "诅咒师" -> FACTION_CURSE_USER;
            case "咒灵" -> FACTION_CURSE_SPIRIT;
            case "平民" -> FACTION_CIVILIAN;
            default -> switch (s.toLowerCase(Locale.ROOT))
            {
                case "sorcerer" -> FACTION_SORCERER;
                case "curse_user", "curseuser" -> FACTION_CURSE_USER;
                case "curse_spirit", "cursespirit" -> FACTION_CURSE_SPIRIT;
                case "civilian" -> FACTION_CIVILIAN;
                default -> null;
            };
        };
    }

    public static String getDisplayName(String factionId)
    {
        return switch (factionId)
        {
            case FACTION_SORCERER -> "咒术师";
            case FACTION_CURSE_USER -> "诅咒师";
            case FACTION_CURSE_SPIRIT -> "咒灵";
            case FACTION_CIVILIAN -> "平民";
            default -> factionId;
        };
    }
}
