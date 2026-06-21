package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端触发领域展开（Alt + 结界术键）。
 * <p>
 * 根据玩家术式类型自动路由到对应的领域实现。
 */
public class DomainExpansionCastC2SPacket
{
    public DomainExpansionCastC2SPacket()
    {
    }

    public static void encode(DomainExpansionCastC2SPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static DomainExpansionCastC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new DomainExpansionCastC2SPacket();
    }

    public static void handle(DomainExpansionCastC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            DomainExpansionRouter.getDomainForPlayer(player).cast(player);
        });
        context.setPacketHandled(true);
    }
}
