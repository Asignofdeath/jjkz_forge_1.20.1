package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.sounds.LimitlessDomainAmbientSoundController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncLimitlessDomainAmbientSoundStartS2CPacket
{
    private final UUID casterUuid;
    private final double x;
    private final double y;
    private final double z;

    public SyncLimitlessDomainAmbientSoundStartS2CPacket(UUID casterUuid, double x, double y, double z)
    {
        this.casterUuid = casterUuid;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(SyncLimitlessDomainAmbientSoundStartS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(packet.casterUuid);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
    }

    public static SyncLimitlessDomainAmbientSoundStartS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncLimitlessDomainAmbientSoundStartS2CPacket(
                buffer.readUUID(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }

    public static void handle(SyncLimitlessDomainAmbientSoundStartS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                LimitlessDomainAmbientSoundController.start(packet.casterUuid, packet.x, packet.y, packet.z));
        context.setPacketHandled(true);
    }
}
