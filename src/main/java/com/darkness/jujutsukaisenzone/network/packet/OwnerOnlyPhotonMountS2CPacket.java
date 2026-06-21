package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.photon.OwnerOnlyPhotonFxClient;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyPhotonFxParams;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class OwnerOnlyPhotonMountS2CPacket
{
    private final int entityId;
    private final ResourceLocation fxId;
    private final OwnerOnlyPhotonFxParams params;

    public OwnerOnlyPhotonMountS2CPacket(int entityId, ResourceLocation fxId, OwnerOnlyPhotonFxParams params)
    {
        this.entityId = entityId;
        this.fxId = fxId;
        this.params = params;
    }

    public static void encode(OwnerOnlyPhotonMountS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.entityId);
        buffer.writeResourceLocation(packet.fxId);
        packet.params.encode(buffer);
    }

    public static OwnerOnlyPhotonMountS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new OwnerOnlyPhotonMountS2CPacket(
                buffer.readVarInt(),
                buffer.readResourceLocation(),
                OwnerOnlyPhotonFxParams.decode(buffer));
    }

    public static void handle(OwnerOnlyPhotonMountS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        if (!FMLEnvironment.dist.isClient())
        {
            return;
        }
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> OwnerOnlyPhotonFxClient.mount(packet.entityId, packet.fxId, packet.params));
        context.setPacketHandled(true);
    }
}
