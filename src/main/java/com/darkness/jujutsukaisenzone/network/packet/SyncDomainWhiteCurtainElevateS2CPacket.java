package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.DomainWhiteCurtainRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 结界/天空盒完成后：将进行中的领域白幕提升到 {@code AFTER_LEVEL}。
 */
public class SyncDomainWhiteCurtainElevateS2CPacket
{
    public SyncDomainWhiteCurtainElevateS2CPacket()
    {
    }

    public static void encode(SyncDomainWhiteCurtainElevateS2CPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static SyncDomainWhiteCurtainElevateS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncDomainWhiteCurtainElevateS2CPacket();
    }

    public static void handle(SyncDomainWhiteCurtainElevateS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(DomainWhiteCurtainRenderer::elevateAboveWorld);
        context.setPacketHandled(true);
    }
}
