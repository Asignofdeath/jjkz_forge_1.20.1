package com.darkness.jujutsukaisenzone.network.api;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.AerialMultiJumpC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SelectStarMarkC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.StarMarkClearC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCombatPowerS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncInnateTalentS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCursedEnergyS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCursedEnergyTraitS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCursedGradeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFactionS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncModConfigS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncStarMarksS2CPacket;
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

/** Domain specific send API for playersync packets. */
public final class PlayerSyncNetwork
{
    private PlayerSyncNetwork()
    {
    }

    public static void sendCombatPowerSync(ServerPlayer player, int physicalPower, int techniquePower)
    {
        NetworkSenders.toPlayer(player,
                new SyncCombatPowerS2CPacket(physicalPower, techniquePower));
    }

    public static void sendCursedEnergySync(ServerPlayer player, int current, int max)
    {
        NetworkSenders.toPlayer(player, new SyncCursedEnergyS2CPacket(current, max));
    }

    public static void sendInnateTalentSync(ServerPlayer player, String talentId, String source)
    {
        NetworkSenders.toPlayer(player, new SyncInnateTalentS2CPacket(talentId, source));
    }

    public static void sendFactionSync(ServerPlayer player, String factionId)
    {
        NetworkSenders.toPlayer(player, new SyncFactionS2CPacket(factionId == null ? "" : factionId));
    }

    public static void sendCursedGradeSync(ServerPlayer player, int experience)
    {
        NetworkSenders.toPlayer(player, new SyncCursedGradeS2CPacket(experience));
    }

    public static void sendCursedEnergyTraitSync(ServerPlayer player, String traitId)
    {
        NetworkSenders.toPlayer(player,
                new SyncCursedEnergyTraitS2CPacket(traitId == null ? "" : traitId));
    }

    public static void sendAerialMultiJumpRequest()
    {
        NetworkSenders.toServer( new AerialMultiJumpC2SPacket());
    }

    public static void sendSelectStarMarkToServer(int delta)
    {
        NetworkSenders.toServer( new SelectStarMarkC2SPacket(delta));
    }

    public static void sendStarMarkClearToServer()
    {
        NetworkSenders.toServer( new StarMarkClearC2SPacket());
    }

    public static void sendStarMarksSync(ServerPlayer owner, List<StarMark> marks)
    {
        NetworkSenders.toDimension(owner,
                new SyncStarMarksS2CPacket(owner.getUUID(), marks));
    }

    public static void sendStarMarksSyncTo(ServerPlayer viewer, ServerPlayer owner, List<StarMark> marks)
    {
        NetworkSenders.toPlayer(viewer,
                new SyncStarMarksS2CPacket(owner.getUUID(), marks));
    }

    /** 把服务端当前生效的数值推给玩家（联机时客户端以服务端为唯一真相）。 */
    public static void sendModConfigSync(ServerPlayer player)
    {
        SyncModConfigS2CPacket.send(player);
    }
}
