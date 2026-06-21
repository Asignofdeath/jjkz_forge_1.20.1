package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.LimitlessDomainChatHudSuppressor;
import com.darkness.jujutsukaisenzone.client.LimitlessDomainCmdCamClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 无量空处结界构建完成瞬间：通知施法者客户端按<strong>玩家实体坐标</strong>执行 CMDCam 镜头 A 的初始化命令。
 */
public class LimitlessDomainCmdCamSetupAS2CPacket
{
    private final Vec3 playerPos;

    public LimitlessDomainCmdCamSetupAS2CPacket(Vec3 playerPos)
    {
        this.playerPos = playerPos;
    }

    public static void encode(LimitlessDomainCmdCamSetupAS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeDouble(packet.playerPos.x);
        buffer.writeDouble(packet.playerPos.y);
        buffer.writeDouble(packet.playerPos.z);
    }

    public static LimitlessDomainCmdCamSetupAS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new LimitlessDomainCmdCamSetupAS2CPacket(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(LimitlessDomainCmdCamSetupAS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        Vec3 pos = packet.playerPos;
        context.enqueueWork(() -> {
            LimitlessDomainChatHudSuppressor.startHideForLimitlessDomainComplete();
            LimitlessDomainCmdCamClient.applyCameraPathASetup(pos.x, pos.y, pos.z);
        });
        context.setPacketHandled(true);
    }
}
