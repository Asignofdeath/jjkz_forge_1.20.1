package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：追踪 / 取消追踪主线任务。 */
public class TrackQuestC2SPacket
{
    private final String questId;
    private final boolean track;

    public TrackQuestC2SPacket(String questId, boolean track)
    {
        this.questId = questId == null ? "" : questId;
        this.track = track;
    }

    public static void encode(TrackQuestC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.questId);
        buffer.writeBoolean(packet.track);
    }

    public static TrackQuestC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new TrackQuestC2SPacket(buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(TrackQuestC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            if (packet.track)
            {
                MainQuestLogic.track(player, packet.questId, true);
            }
            else
            {
                MainQuestLogic.untrack(player, true);
            }
        });
        context.setPacketHandled(true);
    }
}
