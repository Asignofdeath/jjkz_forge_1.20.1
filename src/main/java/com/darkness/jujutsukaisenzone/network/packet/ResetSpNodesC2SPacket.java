package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSpData;
import com.darkness.jujutsukaisenzone.player.PlayerSpNodeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：重置全部 SP 节点（占用 SP 退回可用池）。
 */
public final class ResetSpNodesC2SPacket
{
    public ResetSpNodesC2SPacket()
    {
    }

    public static void encode(ResetSpNodesC2SPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static ResetSpNodesC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new ResetSpNodesC2SPacket();
    }

    public static void handle(ResetSpNodesC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            if (PlayerSpNodeData.unlockedCount(player) <= 0)
            {
                player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.sp_node.reset.empty"));
                return;
            }
            PlayerSpNodeData.resetAll(player);
            player.sendSystemMessage(Component.translatable(
                    "message.jujutsukaisenzone.sp_node.reset.done",
                    PlayerSpData.getAvailable(player)));
        });
        context.setPacketHandled(true);
    }
}
