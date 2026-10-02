package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.client.ScreenShakeController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 单次冲击屏幕晃动（固定方向甩出再收回）。
 */
public final class SyncSingleScreenShakeS2CPacket
{
    private final int durationTicks;
    private final float maxDegrees;

    public SyncSingleScreenShakeS2CPacket(int durationTicks, float maxDegrees)
    {
        this.durationTicks = durationTicks;
        this.maxDegrees = maxDegrees;
    }

    public static void encode(SyncSingleScreenShakeS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeFloat(packet.maxDegrees);
    }

    public static SyncSingleScreenShakeS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncSingleScreenShakeS2CPacket(buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(SyncSingleScreenShakeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ScreenShakeController.apply(
                ScreenShakeKind.SINGLE, packet.durationTicks, packet.maxDegrees, false));
        context.setPacketHandled(true);
    }
}
