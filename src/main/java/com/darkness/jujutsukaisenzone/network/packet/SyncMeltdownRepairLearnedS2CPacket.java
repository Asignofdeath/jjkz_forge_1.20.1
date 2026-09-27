package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerMeltdownRepairData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端→客户端：同步是否已习得修复术式熔断。 */
public class SyncMeltdownRepairLearnedS2CPacket
{
    private final boolean learned;

    public SyncMeltdownRepairLearnedS2CPacket(boolean learned)
    {
        this.learned = learned;
    }

    public static void encode(SyncMeltdownRepairLearnedS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.learned);
    }

    public static SyncMeltdownRepairLearnedS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncMeltdownRepairLearnedS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncMeltdownRepairLearnedS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player != null)
            {
                PlayerMeltdownRepairData.applyClientLearned(player, packet.learned);
            }
        });
        context.setPacketHandled(true);
    }
}
