package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.sounds.LimitlessDomainAmbientSoundController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncLimitlessDomainAmbientSoundStopS2CPacket
{
    private final UUID casterUuid;

    public SyncLimitlessDomainAmbientSoundStopS2CPacket(UUID casterUuid)
    {
        this.casterUuid = casterUuid;
    }

    public static void encode(SyncLimitlessDomainAmbientSoundStopS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(packet.casterUuid);
    }

    public static SyncLimitlessDomainAmbientSoundStopS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncLimitlessDomainAmbientSoundStopS2CPacket(buffer.readUUID());
    }

    public static void handle(SyncLimitlessDomainAmbientSoundStopS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> LimitlessDomainAmbientSoundController.stop(packet.casterUuid));
        context.setPacketHandled(true);
    }
}
