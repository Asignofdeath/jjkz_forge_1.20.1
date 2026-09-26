package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSpData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncSpS2CPacket
{
    private final int available;
    private final int total;
    private final int liberation;
    private final int allocated;

    public SyncSpS2CPacket(int available, int total, int liberation, int allocated)
    {
        this.available = Math.max(0, available);
        this.total = Math.max(0, total);
        this.liberation = Math.max(0, liberation);
        this.allocated = Math.max(0, allocated);
    }

    /** 兼容旧构造：只同步可用余额。 */
    public SyncSpS2CPacket(int available)
    {
        this(available, available, 0, 0);
    }

    public static void encode(SyncSpS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.available);
        buffer.writeVarInt(packet.total);
        buffer.writeVarInt(packet.liberation);
        buffer.writeVarInt(packet.allocated);
    }

    public static SyncSpS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncSpS2CPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt());
    }

    public static void handle(SyncSpS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerSpData.applyClientSnapshot(
                    player, packet.available, packet.total, packet.liberation, packet.allocated);
        });
        context.setPacketHandled(true);
    }
}
