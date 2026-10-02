package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.DomainScanWaveRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端播放领域展开「扫描波」演出（球心 + 时长 + 峰值半径）。
 */
public class SyncDomainScanWaveS2CPacket
{
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final int totalTicks;
    private final float peakRadius;

    public SyncDomainScanWaveS2CPacket(double centerX, double centerY, double centerZ, int totalTicks, float peakRadius)
    {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.totalTicks = totalTicks;
        this.peakRadius = peakRadius;
    }

    public static void encode(SyncDomainScanWaveS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeDouble(packet.centerX);
        buffer.writeDouble(packet.centerY);
        buffer.writeDouble(packet.centerZ);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeFloat(packet.peakRadius);
    }

    public static SyncDomainScanWaveS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncDomainScanWaveS2CPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readFloat()
        );
    }

    public static void handle(SyncDomainScanWaveS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DomainScanWaveRenderer.play(
                packet.centerX, packet.centerY, packet.centerZ, packet.totalTicks, packet.peakRadius));
        context.setPacketHandled(true);
    }
}
