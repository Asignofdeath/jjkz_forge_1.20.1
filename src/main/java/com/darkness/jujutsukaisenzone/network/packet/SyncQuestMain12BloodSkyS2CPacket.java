package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.MainQuest12BloodSkyOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：主线 1.2 血色天空（与 ghost_ambient 铺垫同步）。 */
public class SyncQuestMain12BloodSkyS2CPacket
{
    private final boolean active;

    public SyncQuestMain12BloodSkyS2CPacket(boolean active)
    {
        this.active = active;
    }

    public static SyncQuestMain12BloodSkyS2CPacket show()
    {
        return new SyncQuestMain12BloodSkyS2CPacket(true);
    }

    public static SyncQuestMain12BloodSkyS2CPacket clear()
    {
        return new SyncQuestMain12BloodSkyS2CPacket(false);
    }

    public static void encode(SyncQuestMain12BloodSkyS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
    }

    public static SyncQuestMain12BloodSkyS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncQuestMain12BloodSkyS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncQuestMain12BloodSkyS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.active)
            {
                MainQuest12BloodSkyOverlay.show();
            }
            else
            {
                MainQuest12BloodSkyOverlay.hide();
            }
        });
        context.setPacketHandled(true);
    }
}
