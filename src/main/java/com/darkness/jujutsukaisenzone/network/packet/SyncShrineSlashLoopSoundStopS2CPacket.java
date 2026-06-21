package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.sounds.ShrineDomainSlashLoopSoundController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncShrineSlashLoopSoundStopS2CPacket
{
    private final UUID casterUuid;

    public SyncShrineSlashLoopSoundStopS2CPacket(UUID casterUuid)
    {
        this.casterUuid = casterUuid;
    }

    public static void encode(SyncShrineSlashLoopSoundStopS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(packet.casterUuid);
    }

    public static SyncShrineSlashLoopSoundStopS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncShrineSlashLoopSoundStopS2CPacket(buffer.readUUID());
    }

    public static void handle(SyncShrineSlashLoopSoundStopS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ShrineDomainSlashLoopSoundController.stop(packet.casterUuid));
        context.setPacketHandled(true);
    }
}
