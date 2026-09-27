package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSeLoadoutData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：卸下指定 SE（不再有槽位参数，装载是集合语义）。
 */
public final class UnequipSeC2SPacket
{
    private final String seId;

    public UnequipSeC2SPacket(String seId)
    {
        this.seId = seId == null ? "" : seId;
    }

    public static void encode(UnequipSeC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.seId);
    }

    public static UnequipSeC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new UnequipSeC2SPacket(buffer.readUtf());
    }

    public static void handle(UnequipSeC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null || packet.seId.isBlank())
            {
                return;
            }
            PlayerSeLoadoutData.SeEquipResult result = PlayerSeLoadoutData.tryUnequip(player, packet.seId);
            if (result != PlayerSeLoadoutData.SeEquipResult.SUCCESS)
            {
                player.sendSystemMessage(Component.translatable(
                        "message.jujutsukaisenzone.se.unequip." + result.name().toLowerCase()));
            }
        });
        context.setPacketHandled(true);
    }
}
