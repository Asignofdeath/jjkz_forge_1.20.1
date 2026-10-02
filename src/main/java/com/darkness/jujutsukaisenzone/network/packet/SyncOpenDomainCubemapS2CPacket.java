package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.OpenDomainCubemapRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 开放式领域范围立方体贴图：开/关与球心参数。
 */
public class SyncOpenDomainCubemapS2CPacket
{
    private final boolean active;
    private final double x;
    private final double y;
    private final double z;
    private final int radius;
    private final ResourceLocation textureFolder;

    public SyncOpenDomainCubemapS2CPacket(
            boolean active,
            double x,
            double y,
            double z,
            int radius,
            ResourceLocation textureFolder)
    {
        this.active = active;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.textureFolder = textureFolder;
    }

    public static void encode(SyncOpenDomainCubemapS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.radius);
        buffer.writeResourceLocation(packet.textureFolder);
    }

    public static SyncOpenDomainCubemapS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncOpenDomainCubemapS2CPacket(
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readResourceLocation()
        );
    }

    public static void handle(SyncOpenDomainCubemapS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.active)
            {
                    OpenDomainCubemapRenderer.activate(
                            packet.x, packet.y, packet.z, packet.radius, packet.textureFolder);
            }
            else
            {
                OpenDomainCubemapRenderer.deactivate();
            }
        });
        context.setPacketHandled(true);
    }
}
