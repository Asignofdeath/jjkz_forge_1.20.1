package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.RemoteMurasakiPurpleFlashClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPurpleOverlayAnimationS2CPacket
{
    public SyncPurpleOverlayAnimationS2CPacket()
    {
    }

    public static void encode(SyncPurpleOverlayAnimationS2CPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static SyncPurpleOverlayAnimationS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncPurpleOverlayAnimationS2CPacket();
    }

    public static void handle(SyncPurpleOverlayAnimationS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(RemoteMurasakiPurpleFlashClientEffects::playSequence);
        context.setPacketHandled(true);
    }
}
