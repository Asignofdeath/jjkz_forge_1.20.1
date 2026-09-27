package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyTraitData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步玩家咒力特质 id。
 */
public final class SyncCursedEnergyTraitS2CPacket
{
    private final String traitId;

    public SyncCursedEnergyTraitS2CPacket(String traitId)
    {
        this.traitId = traitId == null ? "" : traitId;
    }

    public static void encode(SyncCursedEnergyTraitS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.traitId);
    }

    public static SyncCursedEnergyTraitS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncCursedEnergyTraitS2CPacket(buffer.readUtf());
    }

    public static void handle(SyncCursedEnergyTraitS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerCursedEnergyTraitData.applyClientSnapshot(player, packet.traitId);
        });
        context.setPacketHandled(true);
    }
}
