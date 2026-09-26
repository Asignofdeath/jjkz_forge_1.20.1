package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSeLoadoutData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：卸下指定 SE 槽。
 */
public final class UnequipSeC2SPacket
{
    private final int slot;

    public UnequipSeC2SPacket(int slot)
    {
        this.slot = slot;
    }

    public static void encode(UnequipSeC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.slot);
    }

    public static UnequipSeC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new UnequipSeC2SPacket(buffer.readVarInt());
    }

    public static void handle(UnequipSeC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            PlayerSeLoadoutData.SeEquipResult result = PlayerSeLoadoutData.tryUnequip(player, packet.slot);
            if (result != PlayerSeLoadoutData.SeEquipResult.SUCCESS)
            {
                player.sendSystemMessage(Component.translatable(
                        "message.jujutsukaisenzone.se.unequip." + result.name().toLowerCase()));
            }
        });
        context.setPacketHandled(true);
    }
}
