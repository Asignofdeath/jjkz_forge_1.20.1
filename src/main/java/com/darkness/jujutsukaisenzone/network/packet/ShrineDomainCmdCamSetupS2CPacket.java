package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.ShrineDomainCmdCamClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 伏魔神龛领域构建完成瞬间：通知施法者客户端按玩家实体坐标与朝向执行 CMDCam 镜头路径编排（仅 save，不播放）。
 */
public class ShrineDomainCmdCamSetupS2CPacket
{
    private final Vec3 playerPos;
    private final float playerYRot;

    public ShrineDomainCmdCamSetupS2CPacket(Vec3 playerPos, float playerYRot)
    {
        this.playerPos = playerPos;
        this.playerYRot = playerYRot;
    }

    public static void encode(ShrineDomainCmdCamSetupS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeDouble(packet.playerPos.x);
        buffer.writeDouble(packet.playerPos.y);
        buffer.writeDouble(packet.playerPos.z);
        buffer.writeFloat(packet.playerYRot);
    }

    public static ShrineDomainCmdCamSetupS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new ShrineDomainCmdCamSetupS2CPacket(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat());
    }

    public static void handle(ShrineDomainCmdCamSetupS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        Vec3 pos = packet.playerPos;
        float yRot = packet.playerYRot;
        context.enqueueWork(() -> {
            Minecraft.getInstance().execute(() ->
                    ShrineDomainCmdCamClient.applyCameraPathSetup(pos.x, pos.y, pos.z, yRot));
        });
        context.setPacketHandled(true);
    }
}
