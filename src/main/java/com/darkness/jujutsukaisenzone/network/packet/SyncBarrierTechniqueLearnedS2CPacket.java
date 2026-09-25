package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端→客户端：同步结界术是否已习得与当前等级（等级供玩家数据面板显示）。 */
public class SyncBarrierTechniqueLearnedS2CPacket
{
    private final boolean learned;
    private final int tier;

    public SyncBarrierTechniqueLearnedS2CPacket(boolean learned, int tier)
    {
        this.learned = learned;
        this.tier = tier;
    }

    public static void encode(SyncBarrierTechniqueLearnedS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.learned);
        buffer.writeInt(packet.tier);
    }

    public static SyncBarrierTechniqueLearnedS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncBarrierTechniqueLearnedS2CPacket(buffer.readBoolean(), buffer.readInt());
    }

    public static void handle(SyncBarrierTechniqueLearnedS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player != null)
            {
                PlayerBarrierTechniqueData.applyClientLearned(player, packet.learned, packet.tier);
            }
        });
        context.setPacketHandled(true);
    }
}
