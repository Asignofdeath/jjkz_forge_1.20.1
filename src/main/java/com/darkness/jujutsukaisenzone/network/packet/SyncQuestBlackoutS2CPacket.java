package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.QuestBlackoutOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：开场全黑 + 旁白。CG 对话走 {@link SyncQuestLetterboxS2CPacket}；普通对白走 {@link SyncQuestCodCaptionsS2CPacket}。 */
public class SyncQuestBlackoutS2CPacket
{
    private final boolean active;
    private final String captionKey;

    public SyncQuestBlackoutS2CPacket(boolean active, String captionKey)
    {
        this.active = active;
        this.captionKey = captionKey == null ? "" : captionKey;
    }

    public static SyncQuestBlackoutS2CPacket clear()
    {
        return new SyncQuestBlackoutS2CPacket(false, "");
    }

    public static void encode(SyncQuestBlackoutS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.captionKey);
    }

    public static SyncQuestBlackoutS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncQuestBlackoutS2CPacket(buffer.readBoolean(), buffer.readUtf());
    }

    public static void handle(SyncQuestBlackoutS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Minecraft.getInstance().setScreen(null);
            if (packet.active)
            {
                QuestBlackoutOverlay.show(packet.captionKey);
            }
            else
            {
                QuestBlackoutOverlay.hide();
            }
        });
        context.setPacketHandled(true);
    }
}
