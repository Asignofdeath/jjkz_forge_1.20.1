package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.QuestMissionBriefingOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：短暂 COD 风格任务简报（标题 / 组织 / 地点 key；时间与角色名由客户端填）。 */
public class SyncQuestMissionBriefingS2CPacket
{
    private final boolean active;
    private final String titleKey;
    private final String orgKey;
    private final String locationKey;

    public SyncQuestMissionBriefingS2CPacket(boolean active, String titleKey, String orgKey, String locationKey)
    {
        this.active = active;
        this.titleKey = titleKey == null ? "" : titleKey;
        this.orgKey = orgKey == null ? "" : orgKey;
        this.locationKey = locationKey == null ? "" : locationKey;
    }

    public static SyncQuestMissionBriefingS2CPacket show(String titleKey, String orgKey, String locationKey)
    {
        return new SyncQuestMissionBriefingS2CPacket(true, titleKey, orgKey, locationKey);
    }

    public static SyncQuestMissionBriefingS2CPacket clear()
    {
        return new SyncQuestMissionBriefingS2CPacket(false, "", "", "");
    }

    public static void encode(SyncQuestMissionBriefingS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.titleKey);
        buffer.writeUtf(packet.orgKey);
        buffer.writeUtf(packet.locationKey);
    }

    public static SyncQuestMissionBriefingS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncQuestMissionBriefingS2CPacket(
                buffer.readBoolean(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf());
    }

    public static void handle(SyncQuestMissionBriefingS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.active)
            {
                QuestMissionBriefingOverlay.show(packet.titleKey, packet.orgKey, packet.locationKey);
            }
            else
            {
                QuestMissionBriefingOverlay.clear();
            }
        });
        context.setPacketHandled(true);
    }
}
