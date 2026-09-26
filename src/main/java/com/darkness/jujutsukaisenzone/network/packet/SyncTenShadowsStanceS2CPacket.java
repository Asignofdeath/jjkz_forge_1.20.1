package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsStanceData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 十影式神姿态同步：true = 护卫。 */
public class SyncTenShadowsStanceS2CPacket
{
    private final boolean guard;

    public SyncTenShadowsStanceS2CPacket(boolean guard)
    {
        this.guard = guard;
    }

    public static void encode(SyncTenShadowsStanceS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.guard);
    }

    public static SyncTenShadowsStanceS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncTenShadowsStanceS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncTenShadowsStanceS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerTenShadowsStanceData.applyClientSnapshot(player, packet.guard);
        });
        context.setPacketHandled(true);
    }
}
