package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.renderer.ShrineCastFilterRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncShrineCastFilterS2CPacket
{
    private final boolean enabled;

    public SyncShrineCastFilterS2CPacket(boolean enabled)
    {
        this.enabled = enabled;
    }

    public static void encode(SyncShrineCastFilterS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.enabled);
    }

    public static SyncShrineCastFilterS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncShrineCastFilterS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncShrineCastFilterS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.enabled) {
                ShrineCastFilterRenderer.enable();
            } else {
                ShrineCastFilterRenderer.disable();
            }
        });
        context.setPacketHandled(true);
    }
}
