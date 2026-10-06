package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.MainQuest13CmdCamClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：主线 1.3 五条悟镜头编排（背影低角度 / 正脸特写）。
 */
public class MainQuest13CmdCamS2CPacket
{
    private final boolean faceCloseup;
    private final double aX;
    private final double aY;
    private final double aZ;
    private final double bX;
    private final double bZ;
    private final float yaw;

    public MainQuest13CmdCamS2CPacket(
            boolean faceCloseup,
            double aX, double aY, double aZ,
            double bX, double bZ,
            float yaw)
    {
        this.faceCloseup = faceCloseup;
        this.aX = aX;
        this.aY = aY;
        this.aZ = aZ;
        this.bX = bX;
        this.bZ = bZ;
        this.yaw = yaw;
    }

    /** 阶段 4：背影低角度。A = 玩家（相机锚点），B = 五条悟。 */
    public static MainQuest13CmdCamS2CPacket backView(double px, double py, double pz, double gojoX, double gojoZ)
    {
        return new MainQuest13CmdCamS2CPacket(false, px, py, pz, gojoX, gojoZ, 0.0F);
    }

    /** 阶段 5：正脸特写。A = 五条悟坐标(B 不用)，yaw = 五条悟朝向。 */
    public static MainQuest13CmdCamS2CPacket faceCloseup(double gojoX, double gojoFeetY, double gojoZ, float yaw)
    {
        return new MainQuest13CmdCamS2CPacket(true, gojoX, gojoFeetY, gojoZ, 0.0D, 0.0D, yaw);
    }

    public static void encode(MainQuest13CmdCamS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.faceCloseup);
        buffer.writeDouble(packet.aX);
        buffer.writeDouble(packet.aY);
        buffer.writeDouble(packet.aZ);
        buffer.writeDouble(packet.bX);
        buffer.writeDouble(packet.bZ);
        buffer.writeFloat(packet.yaw);
    }

    public static MainQuest13CmdCamS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new MainQuest13CmdCamS2CPacket(
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat());
    }

    public static void handle(MainQuest13CmdCamS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.faceCloseup)
            {
                MainQuest13CmdCamClient.setupFaceCloseup(packet.aX, packet.aY, packet.aZ, packet.yaw);
            }
            else
            {
                MainQuest13CmdCamClient.setupBackView(packet.aX, packet.aY, packet.aZ, packet.bX, packet.bZ);
            }
        });
        context.setPacketHandled(true);
    }
}
