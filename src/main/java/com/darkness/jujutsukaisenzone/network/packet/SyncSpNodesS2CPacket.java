package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSpNodeData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class SyncSpNodesS2CPacket
{
    private final Set<String> nodeIds;
    private final int quality;

    public SyncSpNodesS2CPacket(Set<String> nodeIds, int quality)
    {
        this.nodeIds = Set.copyOf(nodeIds);
        this.quality = Math.max(1, quality);
    }

    public static void encode(SyncSpNodesS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.nodeIds.size());
        for (String id : packet.nodeIds)
        {
            buffer.writeUtf(id);
        }
        buffer.writeVarInt(packet.quality);
    }

    public static SyncSpNodesS2CPacket decode(FriendlyByteBuf buffer)
    {
        int count = buffer.readVarInt();
        Set<String> ids = new LinkedHashSet<>();
        for (int i = 0; i < count; i++)
        {
            ids.add(buffer.readUtf());
        }
        return new SyncSpNodesS2CPacket(ids, buffer.readVarInt());
    }

    public static void handle(SyncSpNodesS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerSpNodeData.applyClientSnapshot(player, packet.nodeIds, packet.quality);
        });
        context.setPacketHandled(true);
    }
}
