package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.ColoredWorldOverlayLayerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncColoredWorldOverlayAnimationS2CPacket
{
    private final int colorR;
    private final int colorG;
    private final int colorB;
    private final int totalTicks;
    private final int fadeInTicks;
    private final int fadeOutTicks;
    private final float peakAlpha;

    public SyncColoredWorldOverlayAnimationS2CPacket(
            int colorR,
            int colorG,
            int colorB,
            int totalTicks,
            int fadeInTicks,
            int fadeOutTicks,
            float peakAlpha)
    {
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;
        this.totalTicks = totalTicks;
        this.fadeInTicks = fadeInTicks;
        this.fadeOutTicks = fadeOutTicks;
        this.peakAlpha = peakAlpha;
    }

    public static void encode(SyncColoredWorldOverlayAnimationS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeByte(packet.colorR);
        buffer.writeByte(packet.colorG);
        buffer.writeByte(packet.colorB);
        buffer.writeInt(packet.totalTicks);
        buffer.writeInt(packet.fadeInTicks);
        buffer.writeInt(packet.fadeOutTicks);
        buffer.writeFloat(packet.peakAlpha);
    }

    public static SyncColoredWorldOverlayAnimationS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncColoredWorldOverlayAnimationS2CPacket(
                buffer.readUnsignedByte(),
                buffer.readUnsignedByte(),
                buffer.readUnsignedByte(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readFloat()
        );
    }

    public static void handle(SyncColoredWorldOverlayAnimationS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                ColoredWorldOverlayLayerRenderer.playFadeAnimation(
                        packet.colorR,
                        packet.colorG,
                        packet.colorB,
                        packet.totalTicks,
                        packet.fadeInTicks,
                        packet.fadeOutTicks,
                        packet.peakAlpha));
        context.setPacketHandled(true);
    }
}
