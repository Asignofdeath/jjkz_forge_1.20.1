package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerDomainExpansionData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端→客户端：同步是否已习得领域展开。 */
public class SyncDomainExpansionLearnedS2CPacket
{
    private final boolean learned;

    public SyncDomainExpansionLearnedS2CPacket(boolean learned)
    {
        this.learned = learned;
    }

    public static void encode(SyncDomainExpansionLearnedS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.learned);
    }

    public static SyncDomainExpansionLearnedS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncDomainExpansionLearnedS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncDomainExpansionLearnedS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player != null)
            {
                PlayerDomainExpansionData.applyClientLearned(player, packet.learned);
                if (packet.learned)
                {
                    com.darkness.jujutsukaisenzone.client.gui.technique.TechniqueDevelopmentCanvasWidget.refreshIfOpen();
                }
            }
        });
        context.setPacketHandled(true);
    }
}
