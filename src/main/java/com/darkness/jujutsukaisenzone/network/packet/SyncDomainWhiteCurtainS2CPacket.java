package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.DomainWhiteCurtainRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端播放领域白幕铺开/开洞演出。
 */
public class SyncDomainWhiteCurtainS2CPacket
{
    private final int durationTicks;

    public SyncDomainWhiteCurtainS2CPacket(int durationTicks)
    {
        this.durationTicks = durationTicks;
    }

    public static void encode(SyncDomainWhiteCurtainS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.durationTicks);
    }

    public static SyncDomainWhiteCurtainS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncDomainWhiteCurtainS2CPacket(buffer.readVarInt());
    }

    public static void handle(SyncDomainWhiteCurtainS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DomainWhiteCurtainRenderer.play(packet.durationTicks));
        context.setPacketHandled(true);
    }
}
