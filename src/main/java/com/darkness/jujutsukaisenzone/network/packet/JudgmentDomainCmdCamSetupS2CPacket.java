package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.JudgmentDomainCmdCamClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 诛伏赐死结界构建完成瞬间：通知施法者客户端在领域中心编排 CMDCam 双镜头并回放。
 */
public class JudgmentDomainCmdCamSetupS2CPacket
{
    private final BlockPos domainCenter;

    public JudgmentDomainCmdCamSetupS2CPacket(BlockPos domainCenter)
    {
        this.domainCenter = domainCenter;
    }

    public static void encode(JudgmentDomainCmdCamSetupS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBlockPos(packet.domainCenter);
    }

    public static JudgmentDomainCmdCamSetupS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new JudgmentDomainCmdCamSetupS2CPacket(buffer.readBlockPos());
    }

    public static void handle(JudgmentDomainCmdCamSetupS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        BlockPos center = packet.domainCenter;
        context.enqueueWork(() -> JudgmentDomainCmdCamClient.scheduleCameraPerformance(center));
        context.setPacketHandled(true);
    }
}
