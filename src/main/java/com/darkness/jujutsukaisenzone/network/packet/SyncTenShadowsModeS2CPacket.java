package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsModeData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 十种影法术「影之式神」模式开关同步。 */
public class SyncTenShadowsModeS2CPacket
{
    private final boolean active;

    public SyncTenShadowsModeS2CPacket(boolean active)
    {
        this.active = active;
    }

    public static void encode(SyncTenShadowsModeS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
    }

    public static SyncTenShadowsModeS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncTenShadowsModeS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncTenShadowsModeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerTenShadowsModeData.applyClientSnapshot(player, packet.active);
        });
        context.setPacketHandled(true);
    }
}
