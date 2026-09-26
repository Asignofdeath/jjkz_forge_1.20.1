package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSpNodeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：点亮一个 SP 节点。
 */
public final class UnlockSpNodeC2SPacket
{
    private final String nodeId;

    public UnlockSpNodeC2SPacket(String nodeId)
    {
        this.nodeId = nodeId == null ? "" : nodeId;
    }

    public static void encode(UnlockSpNodeC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.nodeId);
    }

    public static UnlockSpNodeC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new UnlockSpNodeC2SPacket(buffer.readUtf());
    }

    public static void handle(UnlockSpNodeC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null || packet.nodeId.isBlank())
            {
                return;
            }
            PlayerSpNodeData.SpNodeUnlockResult result = PlayerSpNodeData.tryUnlock(player, packet.nodeId);
            if (result != PlayerSpNodeData.SpNodeUnlockResult.SUCCESS)
            {
                player.sendSystemMessage(Component.translatable(
                        "message.jujutsukaisenzone.sp_node.unlock." + result.name().toLowerCase()));
            }
        });
        context.setPacketHandled(true);
    }
}
