package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.ShrineDomainClientState;
import com.darkness.jujutsukaisenzone.client.ShrinePowerSlashChargeHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 同步施法者是否处于自己的伏魔神龛领域存续期。 */
public class SyncShrineDomainActiveS2CPacket
{
    private final boolean active;

    public SyncShrineDomainActiveS2CPacket(boolean active)
    {
        this.active = active;
    }

    public static void encode(SyncShrineDomainActiveS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
    }

    public static SyncShrineDomainActiveS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncShrineDomainActiveS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncShrineDomainActiveS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ShrineDomainClientState.setInOwnActiveShrineDomain(packet.active);
            if (!packet.active)
            {
                ShrinePowerSlashChargeHud.hide();
            }
        });
        context.setPacketHandled(true);
    }
}
