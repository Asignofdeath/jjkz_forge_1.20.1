package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.CinematicLetterboxOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 同步「电影上下黑条」演出（与闪白 / 白幕链路独立，只做屏幕上下两条黑带）。
 */
public class SyncCinematicLetterboxS2CPacket
{
    private final int totalTicks;
    private final int fadeInTicks;
    private final int fadeOutTicks;
    private final float barFraction;

    public SyncCinematicLetterboxS2CPacket(int totalTicks, int fadeInTicks, int fadeOutTicks, float barFraction)
    {
        this.totalTicks = totalTicks;
        this.fadeInTicks = fadeInTicks;
        this.fadeOutTicks = fadeOutTicks;
        this.barFraction = barFraction;
    }

    public static void encode(SyncCinematicLetterboxS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.totalTicks);
        buffer.writeInt(packet.fadeInTicks);
        buffer.writeInt(packet.fadeOutTicks);
        buffer.writeFloat(packet.barFraction);
    }

    public static SyncCinematicLetterboxS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncCinematicLetterboxS2CPacket(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readFloat()
        );
    }

    public static void handle(SyncCinematicLetterboxS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                CinematicLetterboxOverlay.play(
                        packet.totalTicks, packet.fadeInTicks, packet.fadeOutTicks, packet.barFraction));
        context.setPacketHandled(true);
    }
}
