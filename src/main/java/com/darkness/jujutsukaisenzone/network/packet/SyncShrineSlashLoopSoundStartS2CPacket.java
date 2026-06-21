package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.sounds.ShrineDomainSlashLoopSoundController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncShrineSlashLoopSoundStartS2CPacket
{
    private final UUID casterUuid;

    public SyncShrineSlashLoopSoundStartS2CPacket(UUID casterUuid)
    {
        this.casterUuid = casterUuid;
    }

    public static void encode(SyncShrineSlashLoopSoundStartS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(packet.casterUuid);
    }

    public static SyncShrineSlashLoopSoundStartS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncShrineSlashLoopSoundStartS2CPacket(buffer.readUUID());
    }

    public static void handle(SyncShrineSlashLoopSoundStartS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ShrineDomainSlashLoopSoundController.start(packet.casterUuid));
        context.setPacketHandled(true);
    }
}
