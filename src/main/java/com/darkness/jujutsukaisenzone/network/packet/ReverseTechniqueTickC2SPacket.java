package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.ReverseTechniqueServerLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReverseTechniqueTickC2SPacket
{
    public ReverseTechniqueTickC2SPacket()
    {
    }

    public static void encode(ReverseTechniqueTickC2SPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static ReverseTechniqueTickC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new ReverseTechniqueTickC2SPacket();
    }

    public static void handle(ReverseTechniqueTickC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            ReverseTechniqueServerLogic.applyTick(player);
        });
        context.setPacketHandled(true);
    }
}
