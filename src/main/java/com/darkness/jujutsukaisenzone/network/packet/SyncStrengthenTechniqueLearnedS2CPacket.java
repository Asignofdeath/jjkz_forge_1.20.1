package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerStrengthenTechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端→客户端：同步强化术习得状态、等级 tier 与开关。 */
public class SyncStrengthenTechniqueLearnedS2CPacket
{
    private final boolean learned;
    private final int tier;
    private final boolean active;

    public SyncStrengthenTechniqueLearnedS2CPacket(boolean learned, int tier, boolean active)
    {
        this.learned = learned;
        this.tier = tier;
        this.active = active;
    }

    public static void encode(SyncStrengthenTechniqueLearnedS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.learned);
        buffer.writeVarInt(packet.tier);
        buffer.writeBoolean(packet.active);
    }

    public static SyncStrengthenTechniqueLearnedS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncStrengthenTechniqueLearnedS2CPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readBoolean());
    }

    public static void handle(SyncStrengthenTechniqueLearnedS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player != null)
            {
                PlayerStrengthenTechniqueData.applyClientState(player, packet.learned, packet.tier, packet.active);
            }
        });
        context.setPacketHandled(true);
    }
}
