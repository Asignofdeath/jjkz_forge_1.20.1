package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerBrainDamageData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端请求：修复术式熔断（Shift+G；需已习得该通用技能）。 */
public class RepairMeltdownC2SPacket
{
    public RepairMeltdownC2SPacket()
    {
    }

    public static void encode(RepairMeltdownC2SPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static RepairMeltdownC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new RepairMeltdownC2SPacket();
    }

    public static void handle(RepairMeltdownC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            PlayerBrainDamageData.tryRepairMeltdown(player);
        });
        context.setPacketHandled(true);
    }
}
