package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 领域模式同步（常规 / 瞬发 / 开放式）。 */
public class SyncDomainModeS2CPacket
{
    private final PlayerDomainModeData.Mode mode;

    public SyncDomainModeS2CPacket(PlayerDomainModeData.Mode mode)
    {
        this.mode = mode == null ? PlayerDomainModeData.Mode.NORMAL : mode;
    }

    public static void encode(SyncDomainModeS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeEnum(packet.mode);
    }

    public static SyncDomainModeS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncDomainModeS2CPacket(buffer.readEnum(PlayerDomainModeData.Mode.class));
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
            PlayerDomainModeData.applyClientSnapshot(player, packet.mode);
        });
        context.setPacketHandled(true);
    }
}
