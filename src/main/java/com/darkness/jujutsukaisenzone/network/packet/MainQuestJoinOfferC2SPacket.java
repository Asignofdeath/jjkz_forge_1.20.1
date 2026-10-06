package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDefs;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：第一次进世界主线询问的答复。 */
public class MainQuestJoinOfferC2SPacket
{
    private final boolean accept;

    public MainQuestJoinOfferC2SPacket(boolean accept)
    {
        this.accept = accept;
    }

    public static void encode(MainQuestJoinOfferC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.accept);
    }

    public static MainQuestJoinOfferC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new MainQuestJoinOfferC2SPacket(buffer.readBoolean());
    }

    public static void handle(MainQuestJoinOfferC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null || PlayerQuestData.isBootstrapped(player))
            {
                return;
            }
            PlayerQuestData.setBootstrapped(player, true);
            if (packet.accept)
            {
                MainQuestLogic.track(player, MainQuestDefs.MAIN_1_1_ID, true);
            }
        });
        context.setPacketHandled(true);
    }
}
