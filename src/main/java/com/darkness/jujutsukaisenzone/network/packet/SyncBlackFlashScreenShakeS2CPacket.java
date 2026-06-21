package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.client.ScreenShakeController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 衰减振动 S2C（黑闪等）。 */
public final class SyncBlackFlashScreenShakeS2CPacket
{
    private final int durationTicks;
    private final float maxDegrees;

    public SyncBlackFlashScreenShakeS2CPacket(int durationTicks, float maxDegrees)
    {
        this.durationTicks = durationTicks;
        this.maxDegrees = maxDegrees;
    }

    public static void encode(SyncBlackFlashScreenShakeS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.durationTicks);
        buffer.writeFloat(packet.maxDegrees);
    }

    public static SyncBlackFlashScreenShakeS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncBlackFlashScreenShakeS2CPacket(buffer.readInt(), buffer.readFloat());
    }

    public static void handle(SyncBlackFlashScreenShakeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ScreenShakeController.apply(
                ScreenShakeKind.DECAY, packet.durationTicks, packet.maxDegrees, false));
        context.setPacketHandled(true);
    }
}
