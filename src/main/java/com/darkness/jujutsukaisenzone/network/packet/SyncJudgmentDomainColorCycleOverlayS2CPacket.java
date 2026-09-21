package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.JudgmentDomainColorCycleOverlayRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 诛伏赐死：结界完成第 4 秒触发红黄青循环闪现世界层滤镜。 */
public final class SyncJudgmentDomainColorCycleOverlayS2CPacket
{
    public SyncJudgmentDomainColorCycleOverlayS2CPacket()
    {
    }

    public static void encode(SyncJudgmentDomainColorCycleOverlayS2CPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static SyncJudgmentDomainColorCycleOverlayS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncJudgmentDomainColorCycleOverlayS2CPacket();
    }

    public static void handle(SyncJudgmentDomainColorCycleOverlayS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(JudgmentDomainColorCycleOverlayRenderer::playCycleFlash);
        context.setPacketHandled(true);
    }
}
