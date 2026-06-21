package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.EmotionalReleaseClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端同步释放情绪蓄力/冷却状态至客户端。 */
public final class SyncEmotionalReleaseStateS2CPacket
{
    private final boolean charging;
    private final long chargeStartGameTime;
    private final long cooldownUntilGameTime;

    public SyncEmotionalReleaseStateS2CPacket(boolean charging, long chargeStartGameTime, long cooldownUntilGameTime)
    {
        this.charging = charging;
        this.chargeStartGameTime = chargeStartGameTime;
        this.cooldownUntilGameTime = cooldownUntilGameTime;
    }

    public static void encode(SyncEmotionalReleaseStateS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.charging);
        buffer.writeVarLong(packet.chargeStartGameTime);
        buffer.writeVarLong(packet.cooldownUntilGameTime);
    }

    public static SyncEmotionalReleaseStateS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncEmotionalReleaseStateS2CPacket(
                buffer.readBoolean(),
                buffer.readVarLong(),
                buffer.readVarLong()
        );
    }

    public static void handle(SyncEmotionalReleaseStateS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> EmotionalReleaseClientState.applySync(
                packet.charging,
                packet.chargeStartGameTime,
                packet.cooldownUntilGameTime
        ));
        context.setPacketHandled(true);
    }
}
