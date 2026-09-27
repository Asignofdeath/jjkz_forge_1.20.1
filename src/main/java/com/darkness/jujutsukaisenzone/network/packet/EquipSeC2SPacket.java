package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSeLoadoutData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：装载指定 SE，并从指定背包槽扣除已探查咒物。
 */
public final class EquipSeC2SPacket
{
    private final String seId;
    /** 背包槽；&lt; 0 表示服务端自动选第一件吻合件。 */
    private final int inventorySlot;

    public EquipSeC2SPacket(String seId, int inventorySlot)
    {
        this.seId = seId == null ? "" : seId;
        this.inventorySlot = inventorySlot;
    }

    public static void encode(EquipSeC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.seId);
        buffer.writeVarInt(packet.inventorySlot);
    }

    public static EquipSeC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new EquipSeC2SPacket(buffer.readUtf(), buffer.readVarInt());
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
                    PlayerSeLoadoutData.tryEquip(player, packet.seId, packet.inventorySlot);
            if (result != PlayerSeLoadoutData.SeEquipResult.SUCCESS)
            {
                player.sendSystemMessage(Component.translatable(
                        "message.jujutsukaisenzone.se.equip." + result.name().toLowerCase()));
            }
        });
        context.setPacketHandled(true);
    }
}
