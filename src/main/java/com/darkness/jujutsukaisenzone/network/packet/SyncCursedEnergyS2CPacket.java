package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncCursedEnergyS2CPacket
{
    private final int current;
    private final int max;

    public SyncCursedEnergyS2CPacket(int current, int max)
    {
        this.current = current;
        this.max = max;
    }

    public static void encode(SyncCursedEnergyS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.current);
        buffer.writeInt(packet.max);
    }

    public static SyncCursedEnergyS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncCursedEnergyS2CPacket(buffer.readInt(), buffer.readInt());
    }

    public static void handle(SyncCursedEnergyS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerCursedEnergyData.setMaxAndCurrent(player, packet.max, packet.current);
        });
        context.setPacketHandled(true);
    }
}
