package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.LimitlessDomainGarbledTextRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 无量空处乱码：在指定世界坐标刷一批漂浮乱码文字（玩家与生物共用）。
 */
public final class SyncLimitlessDomainGarbledBurstS2CPacket
{
    private final double x;
    private final double y;
    private final double z;

    public SyncLimitlessDomainGarbledBurstS2CPacket(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(SyncLimitlessDomainGarbledBurstS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
    }

    public static SyncLimitlessDomainGarbledBurstS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncLimitlessDomainGarbledBurstS2CPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }

    public static void handle(SyncLimitlessDomainGarbledBurstS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> LimitlessDomainGarbledTextRenderer.spawnBurstAt(packet.x, packet.y, packet.z));
        context.setPacketHandled(true);
    }
}
