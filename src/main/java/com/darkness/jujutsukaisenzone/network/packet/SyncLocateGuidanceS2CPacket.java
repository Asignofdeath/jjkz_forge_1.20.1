package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.LocateGuidanceHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：结构引导目标坐标（或清除）。 */
public class SyncLocateGuidanceS2CPacket
{
    private final boolean active;
    private final String displayName;
    private final int x;
    private final int y;
    private final int z;

    public SyncLocateGuidanceS2CPacket(boolean active, String displayName, int x, int y, int z)
    {
        this.active = active;
        this.displayName = displayName == null ? "" : displayName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static SyncLocateGuidanceS2CPacket clear()
    {
        return new SyncLocateGuidanceS2CPacket(false, "", 0, 0, 0);
    }

    public static void encode(SyncLocateGuidanceS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.displayName);
        buffer.writeInt(packet.x);
        buffer.writeInt(packet.y);
        buffer.writeInt(packet.z);
    }

    public static SyncLocateGuidanceS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncLocateGuidanceS2CPacket(
                buffer.readBoolean(),
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt());
    }

    public static void handle(SyncLocateGuidanceS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.active)
            {
                LocateGuidanceHud.setTarget(packet.displayName, packet.x, packet.y, packet.z);
            }
            else
            {
                LocateGuidanceHud.clear();
            }
        });
        context.setPacketHandled(true);
    }
}
