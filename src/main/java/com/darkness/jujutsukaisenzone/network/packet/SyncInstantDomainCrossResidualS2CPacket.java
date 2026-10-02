package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.InstantDomainCrossResidualRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 瞬发领域关闭后：客户端播放十字窗口残留（窗内为领域立方体贴图）。
 */
public class SyncInstantDomainCrossResidualS2CPacket
{
    private final double x;
    private final double y;
    private final double z;
    private final int durationTicks;
    private final float revolutions;
    private final ResourceLocation textureFolder;

    public SyncInstantDomainCrossResidualS2CPacket(
            double x,
            double y,
            double z,
            int durationTicks,
            float revolutions,
            ResourceLocation textureFolder)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.durationTicks = durationTicks;
        this.revolutions = revolutions;
        this.textureFolder = textureFolder;
    }

    public static void encode(SyncInstantDomainCrossResidualS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeFloat(packet.revolutions);
        buffer.writeResourceLocation(packet.textureFolder);
    }

    public static SyncInstantDomainCrossResidualS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncInstantDomainCrossResidualS2CPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readResourceLocation()
        );
    }

    public static void handle(SyncInstantDomainCrossResidualS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> InstantDomainCrossResidualRenderer.play(
                packet.x,
                packet.y,
                packet.z,
                packet.durationTicks,
                packet.revolutions,
                packet.textureFolder
        ));
        context.setPacketHandled(true);
    }
}
