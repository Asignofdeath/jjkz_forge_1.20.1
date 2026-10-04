package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：主线完成集合 + 当前追踪。 */
public class SyncQuestProgressS2CPacket
{
    private final String trackedId;
    private final List<String> completedIds;

    public SyncQuestProgressS2CPacket(String trackedId, List<String> completedIds)
    {
        this.trackedId = trackedId == null ? "" : trackedId;
        this.completedIds = completedIds == null ? List.of() : List.copyOf(completedIds);
    }

    public static void encode(SyncQuestProgressS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.trackedId);
        buffer.writeVarInt(packet.completedIds.size());
        for (String id : packet.completedIds)
        {
            buffer.writeUtf(id);
        }
    }

    public static SyncQuestProgressS2CPacket decode(FriendlyByteBuf buffer)
    {
        String tracked = buffer.readUtf();
        int count = buffer.readVarInt();
        List<String> completed = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            completed.add(buffer.readUtf());
        }
        return new SyncQuestProgressS2CPacket(tracked, completed);
    }

    public static void handle(SyncQuestProgressS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player != null)
            {
                PlayerQuestData.applyClientSnapshot(
                        player,
                        packet.trackedId.isBlank() ? null : packet.trackedId,
                        packet.completedIds);
            }
        });
        context.setPacketHandled(true);
    }
}
