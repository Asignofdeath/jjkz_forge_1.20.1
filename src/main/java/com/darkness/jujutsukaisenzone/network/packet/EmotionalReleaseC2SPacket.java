package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.EmotionalReleaseServerLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class EmotionalReleaseC2SPacket
{
    public static final byte ACTION_BEGIN = 0;
    public static final byte ACTION_RELEASE = 1;

    private final byte action;

    public EmotionalReleaseC2SPacket(byte action)
    {
        this.action = action;
    }

    public static void encode(EmotionalReleaseC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeByte(packet.action);
    }

    public static EmotionalReleaseC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new EmotionalReleaseC2SPacket(buffer.readByte());
    }

    public static void handle(EmotionalReleaseC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            if (packet.action == ACTION_BEGIN)
            {
                EmotionalReleaseServerLogic.tryBegin(player);
            }
            else if (packet.action == ACTION_RELEASE)
            {
                EmotionalReleaseServerLogic.tryRelease(player);
            }
        });
        context.setPacketHandled(true);
    }
}
