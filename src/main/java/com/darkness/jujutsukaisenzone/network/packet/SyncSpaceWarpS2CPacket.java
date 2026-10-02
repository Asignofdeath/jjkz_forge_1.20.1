package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.SpaceWarpRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 同步客户端屏幕空间径向扭曲开关与强度。
 */
public class SyncSpaceWarpS2CPacket
{
    private final boolean enabled;
    private final float intensity;

    public SyncSpaceWarpS2CPacket(boolean enabled, float intensity)
    {
        this.enabled = enabled;
        this.intensity = intensity;
    }

    public static void encode(SyncSpaceWarpS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.enabled);
        buffer.writeFloat(packet.intensity);
    }

    public static SyncSpaceWarpS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncSpaceWarpS2CPacket(buffer.readBoolean(), buffer.readFloat());
    }

    public static void handle(SyncSpaceWarpS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> SpaceWarpRenderer.setState(packet.enabled, packet.intensity));
        context.setPacketHandled(true);
    }
}
