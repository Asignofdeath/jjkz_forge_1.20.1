package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncCursedGradeS2CPacket
{
    private final int experience;

    public SyncCursedGradeS2CPacket(int experience)
    {
        this.experience = experience;
    }

    public static void encode(SyncCursedGradeS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.experience);
    }

    public static SyncCursedGradeS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncCursedGradeS2CPacket(buffer.readInt());
    }

    public static void handle(SyncCursedGradeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerCursedGradeData.applySyncedExperience(player, packet.experience);
        });
        context.setPacketHandled(true);
    }
}
