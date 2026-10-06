package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.talent.InnateTalentData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncInnateTalentS2CPacket
{
    private final String talentId;
    private final String source;

    public SyncInnateTalentS2CPacket(String talentId, String source)
    {
        this.talentId = talentId == null ? "" : talentId;
        this.source = source == null ? "" : source;
    }

    public static void encode(SyncInnateTalentS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.talentId);
        buffer.writeUtf(packet.source);
    }

    public static SyncInnateTalentS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncInnateTalentS2CPacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(SyncInnateTalentS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            InnateTalentData.applyClient(player, packet.talentId, packet.source);
        });
        context.setPacketHandled(true);
    }
}
