package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端→客户端：同步是否已习得结界术。 */
public class SyncBarrierTechniqueLearnedS2CPacket
{
    private final boolean learned;

    public SyncBarrierTechniqueLearnedS2CPacket(boolean learned)
    {
        this.learned = learned;
    }

    public static void encode(SyncBarrierTechniqueLearnedS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.learned);
    }

    public static SyncBarrierTechniqueLearnedS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncBarrierTechniqueLearnedS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncBarrierTechniqueLearnedS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player != null)
            {
                PlayerBarrierTechniqueData.applyClientLearned(player, packet.learned);
            }
        });
        context.setPacketHandled(true);
    }
}
