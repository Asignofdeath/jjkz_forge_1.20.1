package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.QuestWakeBlinkOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：主线 1.3 醒来眨眼（约 4 秒，客户端自关）。
 */
public class SyncQuestWakeBlinkS2CPacket
{
    private final boolean play;

    public SyncQuestWakeBlinkS2CPacket(boolean play)
    {
        this.play = play;
    }

    public static SyncQuestWakeBlinkS2CPacket play()
    {
        return new SyncQuestWakeBlinkS2CPacket(true);
    }

    public static SyncQuestWakeBlinkS2CPacket clear()
    {
        return new SyncQuestWakeBlinkS2CPacket(false);
    }

    public static void encode(SyncQuestWakeBlinkS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.play);
    }

    public static SyncQuestWakeBlinkS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncQuestWakeBlinkS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncQuestWakeBlinkS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.play)
            {
                QuestWakeBlinkOverlay.play();
            }
            else
            {
                QuestWakeBlinkOverlay.hide();
            }
        });
        context.setPacketHandled(true);
    }
}
