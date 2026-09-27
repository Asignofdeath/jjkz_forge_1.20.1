package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSpNodeData;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SyncSpNodesS2CPacket
{
    private final List<SpNodeInstance> lit;
    private final List<SpNodeInstance> offers;
    private final int quality;
    private final int allowance;
    private final int tier;
    private final int xp;

    public SyncSpNodesS2CPacket(
            List<SpNodeInstance> lit,
            List<SpNodeInstance> offers,
            int quality,
            int allowance,
            int tier,
            int xp)
    {
        this.lit = List.copyOf(lit == null ? List.of() : lit);
        this.offers = List.copyOf(offers == null ? List.of() : offers);
        this.quality = Math.max(1, quality);
        this.allowance = Math.max(0, allowance);
        this.tier = Math.max(0, tier);
        this.xp = Math.max(0, xp);
    }

    public static void encode(SyncSpNodesS2CPacket packet, FriendlyByteBuf buffer)
    {
        writeList(buffer, packet.lit);
        writeList(buffer, packet.offers);
        buffer.writeVarInt(packet.quality);
        buffer.writeVarInt(packet.allowance);
        buffer.writeVarInt(packet.tier);
        buffer.writeVarInt(packet.xp);
    }

    public static SyncSpNodesS2CPacket decode(FriendlyByteBuf buffer)
    {
        List<SpNodeInstance> lit = readList(buffer);
        List<SpNodeInstance> offers = readList(buffer);
        return new SyncSpNodesS2CPacket(
                lit,
                offers,
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt());
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
            PlayerSpNodeData.applyClientSnapshot(
                    player,
                    packet.lit,
                    packet.offers,
                    packet.quality,
                    packet.allowance,
                    packet.tier,
                    packet.xp);
        });
        context.setPacketHandled(true);
    }

    private static void writeList(FriendlyByteBuf buffer, List<SpNodeInstance> nodes)
    {
        buffer.writeVarInt(nodes.size());
        for (SpNodeInstance node : nodes)
        {
            node.write(buffer);
        }
    }

    private static List<SpNodeInstance> readList(FriendlyByteBuf buffer)
    {
        int count = buffer.readVarInt();
        List<SpNodeInstance> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            SpNodeInstance node = SpNodeInstance.read(buffer);
            if (node != null)
            {
                result.add(node);
            }
        }
        return result;
    }
}
