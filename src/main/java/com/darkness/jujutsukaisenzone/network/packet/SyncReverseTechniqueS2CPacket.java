package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncReverseTechniqueS2CPacket
{
    private final boolean learned;

    public SyncReverseTechniqueS2CPacket(boolean learned)
    {
        this.learned = learned;
    }

    public static void encode(SyncReverseTechniqueS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.learned);
    }

    public static SyncReverseTechniqueS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncReverseTechniqueS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncReverseTechniqueS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerReverseTechniqueData.applyClientLearned(player, packet.learned);
        });
        context.setPacketHandled(true);
    }
}
