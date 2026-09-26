package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSeLoadoutData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SyncSeLoadoutS2CPacket
{
    private final List<String> slots;

    public SyncSeLoadoutS2CPacket(List<String> slots)
    {
        this.slots = List.copyOf(slots);
    }

    public static void encode(SyncSeLoadoutS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.slots.size());
        for (String id : packet.slots)
        {
            buffer.writeUtf(id == null ? "" : id);
        }
    }

    public static SyncSeLoadoutS2CPacket decode(FriendlyByteBuf buffer)
    {
        int count = buffer.readVarInt();
        List<String> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            slots.add(buffer.readUtf());
        }
        return new SyncSeLoadoutS2CPacket(slots);
    }

    public static void handle(SyncSeLoadoutS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerSeLoadoutData.applyClientSnapshot(player, packet.slots);
        });
        context.setPacketHandled(true);
    }
}
