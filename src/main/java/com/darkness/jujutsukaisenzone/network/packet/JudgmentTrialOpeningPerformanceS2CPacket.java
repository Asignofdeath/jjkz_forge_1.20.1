package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.JudgmentTrialOpeningCmdCamClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 「开始审判」开庭演出：施法者客户端播放前方 CMDCam 镜头。
 */
public class JudgmentTrialOpeningPerformanceS2CPacket
{
    private final Vec3 playerPos;
    private final float playerYRot;

    public JudgmentTrialOpeningPerformanceS2CPacket(Vec3 playerPos, float playerYRot)
    {
        this.playerPos = playerPos;
        this.playerYRot = playerYRot;
    }

    public static void encode(JudgmentTrialOpeningPerformanceS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeDouble(packet.playerPos.x);
        buffer.writeDouble(packet.playerPos.y);
        buffer.writeDouble(packet.playerPos.z);
        buffer.writeFloat(packet.playerYRot);
    }

    public static JudgmentTrialOpeningPerformanceS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new JudgmentTrialOpeningPerformanceS2CPacket(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat());
    }

    public static void handle(JudgmentTrialOpeningPerformanceS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        Vec3 pos = packet.playerPos;
        float yRot = packet.playerYRot;
        context.enqueueWork(() -> Minecraft.getInstance().execute(() ->
                JudgmentTrialOpeningCmdCamClient.playOpeningCamera(pos.x, pos.y, pos.z, yRot)));
        context.setPacketHandled(true);
    }
}
