package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncFactionS2CPacket
{
    private final String factionId;

    public SyncFactionS2CPacket(String factionId)
    {
        this.factionId = factionId;
    }

    public static void encode(SyncFactionS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.factionId);
    }

    public static SyncFactionS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncFactionS2CPacket(buffer.readUtf());
    }

    public static void handle(SyncFactionS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerFactionData.setFaction(player, packet.factionId);
        });
        context.setPacketHandled(true);
    }
}
