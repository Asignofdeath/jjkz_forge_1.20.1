package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.client.ScreenShakeController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 平滑晃动 S2C（ghost_face 等）。 */
public final class SyncGhostFaceWobbleS2CPacket
{
    private final int durationTicks;
    private final float maxAmplitude;

    public SyncGhostFaceWobbleS2CPacket(int durationTicks, float maxAmplitude)
    {
        this.durationTicks = durationTicks;
        this.maxAmplitude = maxAmplitude;
    }

    public static void encode(SyncGhostFaceWobbleS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.durationTicks);
        buffer.writeFloat(packet.maxAmplitude);
    }

    public static SyncGhostFaceWobbleS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncGhostFaceWobbleS2CPacket(buffer.readInt(), buffer.readFloat());
    }

    public static void handle(SyncGhostFaceWobbleS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ScreenShakeController.apply(
                ScreenShakeKind.SMOOTH_WOBBLE, packet.durationTicks, packet.maxAmplitude, false));
        context.setPacketHandled(true);
    }
}
