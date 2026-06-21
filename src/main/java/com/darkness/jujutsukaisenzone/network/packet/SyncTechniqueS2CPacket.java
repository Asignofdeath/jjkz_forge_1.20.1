package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.ClientSkillBarSyncHandler;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncTechniqueS2CPacket
{
    private final String techniqueId;

    public SyncTechniqueS2CPacket(String techniqueId)
    {
        this.techniqueId = techniqueId;
    }

    public static void encode(SyncTechniqueS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.techniqueId);
    }

    public static SyncTechniqueS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncTechniqueS2CPacket(buffer.readUtf());
    }

    public static void handle(SyncTechniqueS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player != null)
            {
                PlayerTechniqueData.applyClientTechnique(player, packet.techniqueId);
                ClientSkillBarSyncHandler.onTechniqueSynced(packet.techniqueId);
            }
        });
        context.setPacketHandled(true);
    }
}
