package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 领域展开习得状态与「生得领域」演出进度。
 */
public final class PlayerDomainExpansionData
{
    private static final String CLIENT_TAG_LEARNED = "jujutsukaisenzone_domain_expansion_learned";

    private PlayerDomainExpansionData()
    {
    }

    public static boolean hasLearned(ServerPlayer player)
    {
        return getData(player).domainExpansionLearned;
    }

    public static boolean hasLearned(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return hasLearned(serverPlayer);
        }
        return player.getPersistentData().getBoolean(CLIENT_TAG_LEARNED);
    }

    public static void setLearned(ServerPlayer player, boolean learned)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        data.domainExpansionLearned = learned;
        markDirty(player);
        ModNetwork.sendDomainExpansionLearnedSync(player, learned);
    }

    public static void applyClientLearned(Player player, boolean learned)
    {
        player.getPersistentData().putBoolean(CLIENT_TAG_LEARNED, learned);
    }

    public static int getStudybookBoostUses(ServerPlayer player)
    {
        return Math.max(0, getData(player).domainStudybookBoostUses);
    }

    public static void setStudybookBoostUses(ServerPlayer player, int uses)
    {
        getData(player).domainStudybookBoostUses = Math.max(0, uses);
        markDirty(player);
    }

    public static boolean isInnateDomainSessionActive(ServerPlayer player)
    {
        return getData(player).innateDomainSessionActive;
    }

    public static void setInnateDomainSessionActive(ServerPlayer player, boolean active)
    {
        getData(player).innateDomainSessionActive = active;
        markDirty(player);
    }

    private static JjkzPlayerDataStorage.PlayerDataEntry getData(ServerPlayer player)
    {
        PlayerCursedEnergyData.initializeIfAbsent(player);
        return JjkzPlayerDataStorage.get(player).getOrCreate(player);
    }

    private static void markDirty(ServerPlayer player)
    {
        JjkzPlayerDataStorage.get(player).setDirty();
    }
}
