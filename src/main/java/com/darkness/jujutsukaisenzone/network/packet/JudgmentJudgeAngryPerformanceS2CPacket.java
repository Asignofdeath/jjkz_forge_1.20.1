package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.JudgmentJudgeAngryCmdCamClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 审判者愤怒演出：施法者客户端播放审判者正脸 CMDCam 镜头（2 秒）。 */
public final class JudgmentJudgeAngryPerformanceS2CPacket
{
    private final Vec3 judgePos;
    private final float judgeYRot;

    public JudgmentJudgeAngryPerformanceS2CPacket(Vec3 judgePos, float judgeYRot)
    {
        this.judgePos = judgePos;
        this.judgeYRot = judgeYRot;
    }

    public static void encode(JudgmentJudgeAngryPerformanceS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeDouble(packet.judgePos.x);
        buffer.writeDouble(packet.judgePos.y);
        buffer.writeDouble(packet.judgePos.z);
        buffer.writeFloat(packet.judgeYRot);
    }

    public static JudgmentJudgeAngryPerformanceS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new JudgmentJudgeAngryPerformanceS2CPacket(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat());
    }

    public static void handle(JudgmentJudgeAngryPerformanceS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        Vec3 pos = packet.judgePos;
        float yRot = packet.judgeYRot;
        context.enqueueWork(() -> Minecraft.getInstance().execute(() ->
                JudgmentJudgeAngryCmdCamClient.playJudgeFaceCamera(pos.x, pos.y, pos.z, yRot)));
        context.setPacketHandled(true);
    }
}
