package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.MimicryClientState;
import com.darkness.jujutsukaisenzone.player.PlayerMimicryData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端向客户端同步某玩家的拟态状态（含 UUID，供所有客户端更新外观缓存）。
 */
public class SyncMimicryS2CPacket
{
    private final UUID playerUuid;
    private final boolean active;
    private final String characterId;

    public SyncMimicryS2CPacket(UUID playerUuid, boolean active, String characterId)
    {
        this.playerUuid = playerUuid;
        this.active = active;
        this.characterId = characterId == null ? "" : characterId;
    }

    public static void encode(SyncMimicryS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(packet.playerUuid);
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.characterId);
    }

    public static SyncMimicryS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncMimicryS2CPacket(buffer.readUUID(), buffer.readBoolean(), buffer.readUtf());
    }

    public static void handle(SyncMimicryS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            MimicryClientState.apply(packet.playerUuid, packet.active, packet.characterId);
            Player localPlayer = Minecraft.getInstance().player;
            if (localPlayer != null && localPlayer.getUUID().equals(packet.playerUuid))
            {
                PlayerMimicryData.applyClientActive(localPlayer, packet.active);
            }
        });
        context.setPacketHandled(true);
    }
}
