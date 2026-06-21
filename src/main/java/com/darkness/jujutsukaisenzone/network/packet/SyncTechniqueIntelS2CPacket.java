package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class SyncTechniqueIntelS2CPacket
{
    private final Map<String, Integer> values;

    public SyncTechniqueIntelS2CPacket(Map<String, Integer> values)
    {
        this.values = Map.copyOf(values);
    }

    public static void encode(SyncTechniqueIntelS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.values.size());
        for (Map.Entry<String, Integer> entry : packet.values.entrySet())
        {
            buffer.writeUtf(entry.getKey());
            buffer.writeVarInt(Math.max(0, entry.getValue()));
        }
    }

    public static SyncTechniqueIntelS2CPacket decode(FriendlyByteBuf buffer)
    {
        int count = buffer.readVarInt();
        Map<String, Integer> values = new LinkedHashMap<>();
        for (int i = 0; i < count; i++)
        {
            values.put(buffer.readUtf(), Math.max(0, buffer.readVarInt()));
        }
        return new SyncTechniqueIntelS2CPacket(values);
    }

    public static void handle(SyncTechniqueIntelS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerTechniqueIntelData.applyClientSnapshot(player, packet.values);
        });
        context.setPacketHandled(true);
    }
}
