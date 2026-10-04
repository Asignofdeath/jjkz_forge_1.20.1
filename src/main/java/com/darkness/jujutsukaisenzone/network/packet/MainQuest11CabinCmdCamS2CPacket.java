package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.MainQuest11CabinChatHudSuppressor;
import com.darkness.jujutsukaisenzone.client.MainQuest11CabinCmdCamClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 主线 1.1：黑屏期间布三组镜头；揭幕时逐组 {@code cam start}。 */
public class MainQuest11CabinCmdCamS2CPacket
{
    private final Vec3 playerPos;
    private final float playerYRot;
    private final Vec3 lookCenter;
    private final Vec3 xiaoshiziLook;
    private final Vec3 kiroLook;
    private final boolean startPlayback;

    public MainQuest11CabinCmdCamS2CPacket(
            Vec3 playerPos,
            float playerYRot,
            Vec3 lookCenter,
            Vec3 xiaoshiziLook,
            Vec3 kiroLook,
            boolean startPlayback)
    {
        this.playerPos = playerPos;
        this.playerYRot = playerYRot;
        this.lookCenter = lookCenter;
        this.xiaoshiziLook = xiaoshiziLook;
        this.kiroLook = kiroLook;
        this.startPlayback = startPlayback;
    }

    public static MainQuest11CabinCmdCamS2CPacket setup(
            Vec3 playerPos, float playerYRot, Vec3 lookCenter, Vec3 xiaoshiziLook, Vec3 kiroLook)
    {
        return new MainQuest11CabinCmdCamS2CPacket(
                playerPos, playerYRot, lookCenter, xiaoshiziLook, kiroLook, false);
    }

    public static MainQuest11CabinCmdCamS2CPacket startPlayback()
    {
        return new MainQuest11CabinCmdCamS2CPacket(Vec3.ZERO, 0.0F, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, true);
    }

    public static void encode(MainQuest11CabinCmdCamS2CPacket packet, FriendlyByteBuf buffer)
    {
        writeVec(buffer, packet.playerPos);
        buffer.writeFloat(packet.playerYRot);
        writeVec(buffer, packet.lookCenter);
        writeVec(buffer, packet.xiaoshiziLook);
        writeVec(buffer, packet.kiroLook);
        buffer.writeBoolean(packet.startPlayback);
    }

    public static MainQuest11CabinCmdCamS2CPacket decode(FriendlyByteBuf buffer)
    {
        Vec3 playerPos = readVec(buffer);
        float yRot = buffer.readFloat();
        Vec3 lookCenter = readVec(buffer);
        Vec3 xiaoshiziLook = readVec(buffer);
        Vec3 kiroLook = readVec(buffer);
        boolean startPlayback = buffer.readBoolean();
        return new MainQuest11CabinCmdCamS2CPacket(
                playerPos, yRot, lookCenter, xiaoshiziLook, kiroLook, startPlayback);
    }

    private static void writeVec(FriendlyByteBuf buffer, Vec3 vec)
    {
        buffer.writeDouble(vec.x);
        buffer.writeDouble(vec.y);
        buffer.writeDouble(vec.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buffer)
    {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(MainQuest11CabinCmdCamS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        Vec3 playerPos = packet.playerPos;
        float yRot = packet.playerYRot;
        Vec3 lookCenter = packet.lookCenter;
        Vec3 xiaoshiziLook = packet.xiaoshiziLook;
        Vec3 kiroLook = packet.kiroLook;
        boolean startPlayback = packet.startPlayback;
        context.enqueueWork(() -> {
            MainQuest11CabinChatHudSuppressor.startHideForCabinCamera();
            if (startPlayback)
            {
                MainQuest11CabinCmdCamClient.startPlayback();
            }
            else
            {
                MainQuest11CabinCmdCamClient.setup(playerPos, yRot, lookCenter, xiaoshiziLook, kiroLook);
            }
        });
        context.setPacketHandled(true);
    }
}
