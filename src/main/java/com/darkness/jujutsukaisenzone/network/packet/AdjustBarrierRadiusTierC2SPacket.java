package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端调整结界半径档位。
 * <p>
 * 档位与半径的对应表只保存在 {@link BarrierTechniqueServerLogic} 一份，本包不再复制；
 * 调整后回读实际半径用于反馈，避免两处数值各自漂移。
 */
public class AdjustBarrierRadiusTierC2SPacket
{
    private final int delta;

    public AdjustBarrierRadiusTierC2SPacket(int delta)
    {
        this.delta = delta;
    }

    public static void encode(AdjustBarrierRadiusTierC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.delta);
    }

    public static AdjustBarrierRadiusTierC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new AdjustBarrierRadiusTierC2SPacket(buffer.readVarInt());
    }

    public static void handle(AdjustBarrierRadiusTierC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            int safeDelta = Integer.compare(packet.delta, 0);
            if (safeDelta == 0)
            {
                return;
            }
            int tier = BarrierTechniqueServerLogic.staticAdjustBarrierRadiusTier(player, safeDelta);
            int radius = BarrierTechniqueServerLogic.staticGetBarrierRadius(player);
            player.displayClientMessage(Component.literal("结界半径档位：" + (tier + 1) + "（半径 " + radius + "）"), true);
        });
        context.setPacketHandled(true);
    }
}
