package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.client.ScreenShakeController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 持续振动 S2C（伏魔神龛领域攻击期间）。 */
public final class SyncShrineDomainScreenShakeS2CPacket
{
    private final boolean active;
    private final float maxDegrees;

    public SyncShrineDomainScreenShakeS2CPacket(boolean active, float maxDegrees)
    {
        this.active = active;
        this.maxDegrees = maxDegrees;
    }

    public static void encode(SyncShrineDomainScreenShakeS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
        buffer.writeFloat(packet.maxDegrees);
    }

    public static SyncShrineDomainScreenShakeS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncShrineDomainScreenShakeS2CPacket(buffer.readBoolean(), buffer.readFloat());
    }

    public static void handle(SyncShrineDomainScreenShakeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ScreenShakeController.apply(
                ScreenShakeKind.SUSTAINED, 0, packet.maxDegrees, packet.active));
        context.setPacketHandled(true);
    }
}
