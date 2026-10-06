package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.QuestRedBlackoutOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：主线 1.3 开场「红屏→渐变黑屏」遮罩。
 * 纯黑开场旁白走 {@link SyncQuestBlackoutS2CPacket}。
 */
public class SyncQuestRedBlackoutS2CPacket
{
    private final boolean active;

    public SyncQuestRedBlackoutS2CPacket(boolean active)
    {
        this.active = active;
    }

    public static SyncQuestRedBlackoutS2CPacket show()
    {
        return new SyncQuestRedBlackoutS2CPacket(true);
    }

    public static SyncQuestRedBlackoutS2CPacket clear()
    {
        return new SyncQuestRedBlackoutS2CPacket(false);
    }

    public static void encode(SyncQuestRedBlackoutS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
    }

    public static SyncQuestRedBlackoutS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncQuestRedBlackoutS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncQuestRedBlackoutS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Minecraft.getInstance().setScreen(null);
            if (packet.active)
            {
                QuestRedBlackoutOverlay.show();
            }
            else
            {
                QuestRedBlackoutOverlay.hide();
            }
        });
        context.setPacketHandled(true);
    }
}
