package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.photon.OwnerOnlyPhotonFxClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class OwnerOnlyPhotonRemoveS2CPacket
{
    private final int entityId;
    private final boolean force;
    @Nullable
    private final ResourceLocation fxId;

    public OwnerOnlyPhotonRemoveS2CPacket(int entityId, boolean force, @Nullable ResourceLocation fxId)
    {
        this.entityId = entityId;
        this.force = force;
        this.fxId = fxId;
    }

    public static void encode(OwnerOnlyPhotonRemoveS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.force);
        buffer.writeBoolean(packet.fxId != null);
        if (packet.fxId != null)
        {
            buffer.writeResourceLocation(packet.fxId);
        }
    }

    public static OwnerOnlyPhotonRemoveS2CPacket decode(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        boolean force = buffer.readBoolean();
        ResourceLocation fxId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        return new OwnerOnlyPhotonRemoveS2CPacket(entityId, force, fxId);
    }

    public static void handle(OwnerOnlyPhotonRemoveS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        if (!FMLEnvironment.dist.isClient())
        {
            return;
        }
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> OwnerOnlyPhotonFxClient.remove(packet.entityId, packet.force, packet.fxId));
        context.setPacketHandled(true);
    }
}
