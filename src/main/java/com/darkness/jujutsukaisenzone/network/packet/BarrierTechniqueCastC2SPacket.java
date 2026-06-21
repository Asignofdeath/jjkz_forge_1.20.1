package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端触发结界术（独立按键 B）。
 */
public class BarrierTechniqueCastC2SPacket
{
    public BarrierTechniqueCastC2SPacket()
    {
    }

    public static void encode(BarrierTechniqueCastC2SPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static BarrierTechniqueCastC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new BarrierTechniqueCastC2SPacket();
    }

    public static void handle(BarrierTechniqueCastC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            BarrierTechniqueServerLogic.staticCast(player);
        });
        context.setPacketHandled(true);
    }
}
