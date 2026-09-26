package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSeLoadoutData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：把 SE 装备到指定槽。
 */
public final class EquipSeC2SPacket
{
    private final int slot;
    private final String seId;

    public EquipSeC2SPacket(int slot, String seId)
    {
        this.slot = slot;
        this.seId = seId == null ? "" : seId;
    }

    public static void encode(EquipSeC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.slot);
        buffer.writeUtf(packet.seId);
    }

    public static EquipSeC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new EquipSeC2SPacket(buffer.readVarInt(), buffer.readUtf());
    }

    public static void handle(EquipSeC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null || packet.seId.isBlank())
            {
                return;
            }
            PlayerSeLoadoutData.SeEquipResult result =
                    PlayerSeLoadoutData.tryEquip(player, packet.slot, packet.seId);
            if (result != PlayerSeLoadoutData.SeEquipResult.SUCCESS)
            {
                player.sendSystemMessage(Component.translatable(
                        "message.jujutsukaisenzone.se.equip." + result.name().toLowerCase()));
            }
        });
        context.setPacketHandled(true);
    }
}
