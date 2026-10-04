package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.QuestTrackHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：当前追踪的主线任务（或清除）。 */
public class SyncQuestTrackS2CPacket
{
    private final boolean active;
    private final String questId;
    private final String title;
    private final String objective;

    public SyncQuestTrackS2CPacket(boolean active, String questId, String title, String objective)
    {
        this.active = active;
        this.questId = questId == null ? "" : questId;
        this.title = title == null ? "" : title;
        this.objective = objective == null ? "" : objective;
    }

    public static SyncQuestTrackS2CPacket clear()
    {
        return new SyncQuestTrackS2CPacket(false, "", "", "");
    }

    public static void encode(SyncQuestTrackS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.questId);
        buffer.writeUtf(packet.title);
        buffer.writeUtf(packet.objective);
    }

    public static SyncQuestTrackS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncQuestTrackS2CPacket(
                buffer.readBoolean(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf());
    }

    public static void handle(SyncQuestTrackS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.active)
            {
                QuestTrackHud.setTracked(packet.questId, packet.title, packet.objective);
            }
            else
            {
                QuestTrackHud.clear();
            }
        });
        context.setPacketHandled(true);
    }
}
