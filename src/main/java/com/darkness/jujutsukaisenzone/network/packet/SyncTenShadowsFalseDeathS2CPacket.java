package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.ClientTenShadowsFalseDeathState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** 调伏仪式假死表现同步（离体相机 / 躺姿），不含 GameType。 */
public class SyncTenShadowsFalseDeathS2CPacket
{
    private final UUID entityId;
    private final boolean active;

    public SyncTenShadowsFalseDeathS2CPacket(UUID entityId, boolean active)
    {
        this.entityId = entityId;
        this.active = active;
    }

    public static void encode(SyncTenShadowsFalseDeathS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(packet.entityId);
        buffer.writeBoolean(packet.active);
    }

    public static SyncTenShadowsFalseDeathS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncTenShadowsFalseDeathS2CPacket(buffer.readUUID(), buffer.readBoolean());
    }

    public static void handle(SyncTenShadowsFalseDeathS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientTenShadowsFalseDeathState.apply(packet.entityId, packet.active));
        context.setPacketHandled(true);
    }
}
