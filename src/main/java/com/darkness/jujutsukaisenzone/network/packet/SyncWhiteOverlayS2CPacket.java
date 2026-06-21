package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.WhiteOverlayLayerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 同步客户端白色滤镜渲染配置。
 * <p>
 * 业务意图：通过服务器命令直接控制指定玩家客户端的中间层白色覆盖效果。
 */
public class SyncWhiteOverlayS2CPacket
{
    private final boolean enabled;
    private final float alpha;

    public SyncWhiteOverlayS2CPacket(boolean enabled, float alpha)
    {
        this.enabled = enabled;
        this.alpha = alpha;
    }

    public static void encode(SyncWhiteOverlayS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.enabled);
        buffer.writeFloat(packet.alpha);
    }

    public static SyncWhiteOverlayS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncWhiteOverlayS2CPacket(buffer.readBoolean(), buffer.readFloat());
    }

    public static void handle(SyncWhiteOverlayS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> WhiteOverlayLayerRenderer.setState(packet.enabled, packet.alpha));
        context.setPacketHandled(true);
    }
}
