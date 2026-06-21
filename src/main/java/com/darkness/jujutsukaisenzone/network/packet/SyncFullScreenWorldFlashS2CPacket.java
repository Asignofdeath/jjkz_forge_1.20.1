package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.FullScreenWorldFlashRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 同步“世界层最上”的全屏白色闪光动画（与蓄力 {@link SyncWhiteOverlayAnimationS2CPacket} 独立）。
 */
public class SyncFullScreenWorldFlashS2CPacket
{
    private final int totalTicks;
    private final int fadeInTicks;
    private final int fadeOutTicks;
    private final float peakAlpha;

    public SyncFullScreenWorldFlashS2CPacket(int totalTicks, int fadeInTicks, int fadeOutTicks, float peakAlpha)
    {
        this.totalTicks = totalTicks;
        this.fadeInTicks = fadeInTicks;
        this.fadeOutTicks = fadeOutTicks;
        this.peakAlpha = peakAlpha;
    }

    public static void encode(SyncFullScreenWorldFlashS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.totalTicks);
        buffer.writeInt(packet.fadeInTicks);
        buffer.writeInt(packet.fadeOutTicks);
        buffer.writeFloat(packet.peakAlpha);
    }

    public static SyncFullScreenWorldFlashS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncFullScreenWorldFlashS2CPacket(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readFloat()
        );
    }

    public static void handle(SyncFullScreenWorldFlashS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                FullScreenWorldFlashRenderer.playFadeAnimation(
                        packet.totalTicks, packet.fadeInTicks, packet.fadeOutTicks, packet.peakAlpha));
        context.setPacketHandled(true);
    }
}
