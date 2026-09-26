package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsContractData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 十种影法术已调伏式神位图同步。 */
public class SyncTenShadowsContractS2CPacket
{
    private final int tamedMask;

    public SyncTenShadowsContractS2CPacket(int tamedMask)
    {
        this.tamedMask = tamedMask;
    }

    public static void encode(SyncTenShadowsContractS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.tamedMask);
    }

    public static SyncTenShadowsContractS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncTenShadowsContractS2CPacket(buffer.readVarInt());
    }

    public static void handle(SyncTenShadowsContractS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerTenShadowsContractData.applyClientSnapshot(player, packet.tamedMask);
        });
        context.setPacketHandled(true);
    }
}
