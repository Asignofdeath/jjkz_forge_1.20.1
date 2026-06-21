package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerShrineBlackFlashMilestoneData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncShrineBlackFlashMilestoneS2CPacket
{
    private final int count;

    public SyncShrineBlackFlashMilestoneS2CPacket(int count)
    {
        this.count = count;
    }

    public static void encode(SyncShrineBlackFlashMilestoneS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.count);
    }

    public static SyncShrineBlackFlashMilestoneS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncShrineBlackFlashMilestoneS2CPacket(buffer.readVarInt());
    }

    public static void handle(SyncShrineBlackFlashMilestoneS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerShrineBlackFlashMilestoneData.applyClientCount(player, packet.count);
        });
        context.setPacketHandled(true);
    }
}
