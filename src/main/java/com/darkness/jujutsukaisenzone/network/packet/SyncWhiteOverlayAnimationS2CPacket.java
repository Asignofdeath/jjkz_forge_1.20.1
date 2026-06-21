package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.WhiteOverlayLayerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 同步一次性白色滤镜动画参数到客户端。
 * <p>
 * 业务意图：将技能触发时机与客户端视觉表现解耦，由服务器统一下发动画参数，客户端按本地帧平滑播放。
 */
public class SyncWhiteOverlayAnimationS2CPacket
{
    private final int totalTicks;
    private final int fadeInTicks;
    private final int fadeOutTicks;
    private final float peakAlpha;

    public SyncWhiteOverlayAnimationS2CPacket(int totalTicks, int fadeInTicks, int fadeOutTicks, float peakAlpha)
    {
        this.totalTicks = totalTicks;
        this.fadeInTicks = fadeInTicks;
        this.fadeOutTicks = fadeOutTicks;
        this.peakAlpha = peakAlpha;
    }

    public static void encode(SyncWhiteOverlayAnimationS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.totalTicks);
        buffer.writeInt(packet.fadeInTicks);
        buffer.writeInt(packet.fadeOutTicks);
        buffer.writeFloat(packet.peakAlpha);
    }

    public static SyncWhiteOverlayAnimationS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncWhiteOverlayAnimationS2CPacket(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readFloat()
        );
    }

    public static void handle(SyncWhiteOverlayAnimationS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                WhiteOverlayLayerRenderer.playFadeAnimation(packet.totalTicks, packet.fadeInTicks, packet.fadeOutTicks, packet.peakAlpha));
        context.setPacketHandled(true);
    }
}
