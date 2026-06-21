package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.FugaBoomFlashClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 「灶」开爆炸：触发全屏闪白 + 世界层闪白串联动画。 */
public final class SyncFugaBoomFlashSequenceS2CPacket
{
    public SyncFugaBoomFlashSequenceS2CPacket()
    {
    }

    public static void encode(SyncFugaBoomFlashSequenceS2CPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static SyncFugaBoomFlashSequenceS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncFugaBoomFlashSequenceS2CPacket();
    }

    public static void handle(SyncFugaBoomFlashSequenceS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(FugaBoomFlashClientEffects::playSequence);
        context.setPacketHandled(true);
    }
}
