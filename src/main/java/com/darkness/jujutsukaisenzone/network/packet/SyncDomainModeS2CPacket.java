package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 领域模式同步：true = 瞬发。 */
public class SyncDomainModeS2CPacket
{
    private final boolean instant;

    public SyncDomainModeS2CPacket(boolean instant)
    {
        this.instant = instant;
    }

    public static void encode(SyncDomainModeS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.instant);
    }

    public static SyncDomainModeS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncDomainModeS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncDomainModeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerDomainModeData.applyClientSnapshot(player, packet.instant);
        });
        context.setPacketHandled(true);
    }
}
