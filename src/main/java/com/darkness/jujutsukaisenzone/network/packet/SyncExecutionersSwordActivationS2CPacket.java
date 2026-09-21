package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.ExecutionersSwordActivationClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncExecutionersSwordActivationS2CPacket
{
    public SyncExecutionersSwordActivationS2CPacket()
    {
    }

    public static void encode(SyncExecutionersSwordActivationS2CPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static SyncExecutionersSwordActivationS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncExecutionersSwordActivationS2CPacket();
    }

    public static void handle(SyncExecutionersSwordActivationS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(ExecutionersSwordActivationClientEffects::play);
        context.setPacketHandled(true);
    }
}
