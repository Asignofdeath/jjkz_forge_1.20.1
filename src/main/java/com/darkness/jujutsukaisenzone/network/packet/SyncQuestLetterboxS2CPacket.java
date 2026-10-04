package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.QuestCinematicLetterboxOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：任务<strong>对话</strong>（电影黑幕下条字幕）。不是聊天栏，也不是开场全黑。 */
public class SyncQuestLetterboxS2CPacket
{
    private final boolean active;
    private final String captionKey;

    public SyncQuestLetterboxS2CPacket(boolean active, String captionKey)
    {
        this.active = active;
        this.captionKey = captionKey == null ? "" : captionKey;
    }

    public static SyncQuestLetterboxS2CPacket clear()
    {
        return new SyncQuestLetterboxS2CPacket(false, "");
    }

    public static void encode(SyncQuestLetterboxS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.captionKey);
    }

    public static SyncQuestLetterboxS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncQuestLetterboxS2CPacket(buffer.readBoolean(), buffer.readUtf());
    }

    public static void handle(SyncQuestLetterboxS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Minecraft.getInstance().setScreen(null);
            if (packet.active)
            {
                QuestCinematicLetterboxOverlay.show(packet.captionKey);
            }
            else
            {
                QuestCinematicLetterboxOverlay.clearImmediate();
            }
        });
        context.setPacketHandled(true);
    }
}
