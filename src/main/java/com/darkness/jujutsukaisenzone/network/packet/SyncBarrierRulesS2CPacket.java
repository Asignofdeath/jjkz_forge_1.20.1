package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.BarrierRulesData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端→客户端：同步玩家结界规则
 */
public class SyncBarrierRulesS2CPacket
{
    private final boolean blockMobSpawn;
    private final boolean autoRepairBarrier;

    public SyncBarrierRulesS2CPacket(boolean blockMobSpawn, boolean autoRepairBarrier)
    {
        this.blockMobSpawn = blockMobSpawn;
        this.autoRepairBarrier = autoRepairBarrier;
    }

    public static void encode(SyncBarrierRulesS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.blockMobSpawn);
        buffer.writeBoolean(packet.autoRepairBarrier);
    }

    public static SyncBarrierRulesS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncBarrierRulesS2CPacket(buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(SyncBarrierRulesS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null)
            {
                UUID playerId = minecraft.player.getUUID();
                BarrierRulesData.setBlockMobSpawn(playerId, packet.blockMobSpawn);
                BarrierRulesData.setAutoRepairBarrier(playerId, packet.autoRepairBarrier);
            }
        });
        context.setPacketHandled(true);
    }

    public static void send(ServerPlayer player)
    {
        UUID playerId = player.getUUID();
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncBarrierRulesS2CPacket(
                        BarrierRulesData.isBlockMobSpawn(playerId),
                        BarrierRulesData.isAutoRepairBarrier(playerId)
                )
        );
    }
}
