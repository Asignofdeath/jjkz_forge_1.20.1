package com.darkness.jujutsukaisenzone.network.api;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.EquipSeC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.RefreshSpOffersC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ResetSpNodesC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSeLoadoutS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillBarConfigC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillBarConfigS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillLevelsS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillUnlocksS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSpNodesS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSpS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueSkillUseC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.UnequipSeC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.UnlockSkillC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.UnlockSkillS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.UnlockSpNodeC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.UpgradeSkillC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.UpgradeSkillS2CPacket;
import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyPhotonFxParams;
import com.darkness.jujutsukaisenzone.player.PlayerMimicryData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

/** Domain specific send API for skill packets. */
public final class SkillNetwork
{
    private SkillNetwork()
    {
    }

    public static void sendSpSync(ServerPlayer player, int available, int total, int liberation, int allocated)
    {
        NetworkSenders.toPlayer(player,
                new SyncSpS2CPacket(available, total, liberation, allocated));
    }

    public static void sendSpNodesSync(
            ServerPlayer player,
            java.util.List<SpNodeInstance> lit,
            java.util.List<SpNodeInstance> offers,
            int quality,
            int allowance,
            int tier,
            int xp)
    {
        NetworkSenders.toPlayer(player,
                new SyncSpNodesS2CPacket(lit, offers, quality, allowance, tier, xp));
    }

    public static void sendSeLoadoutSync(ServerPlayer player, java.util.Map<String, com.darkness.jujutsukaisenzone.player.SeEquippedEntry> entries)
    {
        NetworkSenders.toPlayer(player, new SyncSeLoadoutS2CPacket(entries));
    }

    public static void sendSkillLevelsSync(ServerPlayer player, java.util.Map<String, Integer> levelTags)
    {
        NetworkSenders.toPlayer(player, new SyncSkillLevelsS2CPacket(levelTags));
    }

    public static void sendUpgradeSkillRequest(String techniqueId, String skillId)
    {
        NetworkSenders.toServer( new UpgradeSkillC2SPacket(techniqueId, skillId));
    }

    public static void sendUpgradeSkillResult(
            ServerPlayer player,
            com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeResult result,
            String techniqueId,
            String skillId,
            String levelTag,
            int level)
    {
        NetworkSenders.toPlayer(player,
                new UpgradeSkillS2CPacket(result, techniqueId, skillId, levelTag, level));
    }

    public static void sendSkillUnlocksSync(ServerPlayer player, java.util.Map<String, Boolean> unlockTags)
    {
        NetworkSenders.toPlayer(player, new SyncSkillUnlocksS2CPacket(unlockTags));
    }

    public static void sendUnlockSkillRequest(String techniqueId, String nodeId)
    {
        NetworkSenders.toServer( new UnlockSkillC2SPacket(techniqueId, nodeId));
    }

    public static void sendUnlockSpNodeRequest(String nodeId)
    {
        NetworkSenders.toServer( new UnlockSpNodeC2SPacket(nodeId));
    }

    public static void sendResetSpNodesRequest()
    {
        NetworkSenders.toServer( new ResetSpNodesC2SPacket());
    }

    public static void sendRefreshSpOffersRequest()
    {
        NetworkSenders.toServer( new RefreshSpOffersC2SPacket());
    }

    public static void sendEquipSeRequest(String seId)
    {
        sendEquipSeRequest(seId, -1);
    }

    public static void sendEquipSeRequest(String seId, int inventorySlot)
    {
        NetworkSenders.toServer( new EquipSeC2SPacket(seId, inventorySlot));
    }

    public static void sendUnequipSeRequest(String seId)
    {
        NetworkSenders.toServer( new UnequipSeC2SPacket(seId));
    }

    public static void sendUnlockSkillResult(
            ServerPlayer player,
            com.darkness.jujutsukaisenzone.technique.development.SkillUnlockResult result,
            String techniqueId,
            String nodeId,
            String unlockTag)
    {
        NetworkSenders.toPlayer(player,
                new UnlockSkillS2CPacket(result, techniqueId, nodeId, unlockTag));
    }

    public static void sendTechniqueSkillUseToServer(int slot)
    {
        NetworkSenders.toServer( new TechniqueSkillUseC2SPacket(slot));
    }

    public static void sendSkillBarConfigToServer(String techniqueId, String[] skillIds)
    {
        NetworkSenders.toServer( new SyncSkillBarConfigC2SPacket(techniqueId, skillIds));
    }

    public static void sendSkillBarConfigSync(ServerPlayer player, String techniqueId, String[] skillIds)
    {
        NetworkSenders.toPlayer(player, new SyncSkillBarConfigS2CPacket(techniqueId, skillIds));
    }
}
