package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerBrainDamageData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainGaugeData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 领域相关资源同步：脑损 / 今日展开次数 / 领域量表 / 今日熔断修复次数。 */
public class SyncBrainDamageS2CPacket
{
    private final int brainDamage;
    private final int expandsToday;
    private final int domainGauge;
    private final int meltdownRepairsToday;

    public SyncBrainDamageS2CPacket(int brainDamage, int expandsToday, int domainGauge, int meltdownRepairsToday)
    {
        this.brainDamage = brainDamage;
        this.expandsToday = expandsToday;
        this.domainGauge = domainGauge;
        this.meltdownRepairsToday = meltdownRepairsToday;
    }

    public static void encode(SyncBrainDamageS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.brainDamage);
        buffer.writeVarInt(packet.expandsToday);
        buffer.writeVarInt(packet.domainGauge);
        buffer.writeVarInt(packet.meltdownRepairsToday);
    }

    public static SyncBrainDamageS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncBrainDamageS2CPacket(
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SyncBrainDamageS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerBrainDamageData.applyClientSnapshot(
                    player, packet.brainDamage, packet.expandsToday, packet.meltdownRepairsToday);
            PlayerDomainGaugeData.applyClientSnapshot(player, packet.domainGauge);
        });
        context.setPacketHandled(true);
    }
}
