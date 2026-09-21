package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.aerial.AerialMultiJumpServerLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：请求空战多段跳。 */
public final class AerialMultiJumpC2SPacket
{
    public static void encode(AerialMultiJumpC2SPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static AerialMultiJumpC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new AerialMultiJumpC2SPacket();
    }

    public static void handle(AerialMultiJumpC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player != null)
            {
                AerialMultiJumpServerLogic.tryJump(player);
            }
        });
        context.setPacketHandled(true);
    }
}
