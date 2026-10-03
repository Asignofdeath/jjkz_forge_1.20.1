package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.DomainScanWaveRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 客户端播放领域展开「扫描波」演出（施法者 UUID + 球心 + 时长 + 峰值半径）。
 * <p>
 * 带上施法者 UUID 是为了让客户端<strong>每帧跟踪施法者脚底</strong>当球心：前摇期间不锁操作、
 * 玩家可自由移动，只发一次快照球心的话，人一走波心就留在原地 —— 看起来「不是从脚下发出的」。
 * 这里的 {@code centerX/Y/Z} 只是<strong>兜底</strong>（施法者不在客户端实体列表时用）。
 */
public class SyncDomainScanWaveS2CPacket
{
    private final UUID casterUuid;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final int totalTicks;
    private final float peakRadius;

    public SyncDomainScanWaveS2CPacket(UUID casterUuid, double centerX, double centerY, double centerZ,
                                       int totalTicks, float peakRadius)
    {
        this.casterUuid = casterUuid;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.totalTicks = totalTicks;
        this.peakRadius = peakRadius;
    }

    public static void encode(SyncDomainScanWaveS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(packet.casterUuid);
        buffer.writeDouble(packet.centerX);
        buffer.writeDouble(packet.centerY);
        buffer.writeDouble(packet.centerZ);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeFloat(packet.peakRadius);
    }

    public static SyncDomainScanWaveS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncDomainScanWaveS2CPacket(
                buffer.readUUID(),
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
                packet.casterUuid, packet.centerX, packet.centerY, packet.centerZ,
                packet.totalTicks, packet.peakRadius));
        context.setPacketHandled(true);
    }
}
