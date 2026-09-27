package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSpData;
import com.darkness.jujutsukaisenzone.player.SpNodeShelfLogic;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeShelfRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：付费刷新 SP 在售货架。
 */
public final class RefreshSpOffersC2SPacket
{
    public RefreshSpOffersC2SPacket()
    {
    }

    public static void encode(RefreshSpOffersC2SPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static RefreshSpOffersC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new RefreshSpOffersC2SPacket();
    }

    public static void handle(RefreshSpOffersC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            SpNodeShelfLogic.RefreshResult result = SpNodeShelfLogic.tryPaidRefresh(player);
            if (result == SpNodeShelfLogic.RefreshResult.SUCCESS)
            {
                player.sendSystemMessage(Component.translatable(
                        "message.jujutsukaisenzone.sp_node.refresh.done",
                        PlayerSpData.getAvailable(player)));
                return;
            }
            player.sendSystemMessage(Component.translatable(
                    "message.jujutsukaisenzone.sp_node.refresh." + result.name().toLowerCase(),
                    SpNodeShelfRules.refreshCost(
                            com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData.getExperience(player))));
        });
        context.setPacketHandled(true);
    }
}
