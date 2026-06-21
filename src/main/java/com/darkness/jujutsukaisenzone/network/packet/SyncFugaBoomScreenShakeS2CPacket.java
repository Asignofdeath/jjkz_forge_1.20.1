package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.client.ScreenShakeController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 恒定振动 S2C（灶开爆炸等）。 */
public final class SyncFugaBoomScreenShakeS2CPacket
{
    private final int durationTicks;
    private final float maxDegrees;

    public SyncFugaBoomScreenShakeS2CPacket(int durationTicks, float maxDegrees)
    {
        this.durationTicks = durationTicks;
        this.maxDegrees = maxDegrees;
    }

    public static void encode(SyncFugaBoomScreenShakeS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.durationTicks);
        buffer.writeFloat(packet.maxDegrees);
    }

    public static SyncFugaBoomScreenShakeS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncFugaBoomScreenShakeS2CPacket(buffer.readInt(), buffer.readFloat());
    }

    public static void handle(SyncFugaBoomScreenShakeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ScreenShakeController.apply(
                ScreenShakeKind.CONSTANT, packet.durationTicks, packet.maxDegrees, false));
        context.setPacketHandled(true);
    }
}
