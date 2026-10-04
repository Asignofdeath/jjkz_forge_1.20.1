package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.CgLookClampController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：COD 式 CG 把第一人称朝向夹在基准方向附近。 */
public class SyncCgLookClampS2CPacket
{
    private final boolean active;
    private final float baseYaw;
    private final float basePitch;
    private final float yawHalf;
    private final float pitchHalf;

    public SyncCgLookClampS2CPacket(
            boolean active, float baseYaw, float basePitch, float yawHalf, float pitchHalf)
    {
        this.active = active;
        this.baseYaw = baseYaw;
        this.basePitch = basePitch;
        this.yawHalf = yawHalf;
        this.pitchHalf = pitchHalf;
    }

    public static SyncCgLookClampS2CPacket clear()
    {
        return new SyncCgLookClampS2CPacket(false, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    public static void encode(SyncCgLookClampS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
        buffer.writeFloat(packet.baseYaw);
        buffer.writeFloat(packet.basePitch);
        buffer.writeFloat(packet.yawHalf);
        buffer.writeFloat(packet.pitchHalf);
    }

    public static SyncCgLookClampS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncCgLookClampS2CPacket(
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat());
    }

    public static void handle(SyncCgLookClampS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.active)
            {
                CgLookClampController.set(
                        true, packet.baseYaw, packet.basePitch, packet.yawHalf, packet.pitchHalf);
            }
            else
            {
                CgLookClampController.clear();
            }
        });
        context.setPacketHandled(true);
    }
}
