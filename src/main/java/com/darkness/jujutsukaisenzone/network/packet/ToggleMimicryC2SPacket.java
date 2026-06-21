package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerMimicryData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端请求切换拟态开关（空包，服务端翻转权威状态）。
 */
public class ToggleMimicryC2SPacket
{
    public ToggleMimicryC2SPacket()
    {
    }

    public static void encode(ToggleMimicryC2SPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static ToggleMimicryC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new ToggleMimicryC2SPacket();
    }

    public static void handle(ToggleMimicryC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isAlive())
            {
                return;
            }
            PlayerMimicryData.toggle(player);
        });
        context.setPacketHandled(true);
    }
}
