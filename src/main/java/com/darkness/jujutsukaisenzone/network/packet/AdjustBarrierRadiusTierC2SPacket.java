package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端调整结界半径档位（档位索引：0→8, 1→16, 2→32, 3→64）。
 */
public class AdjustBarrierRadiusTierC2SPacket
{
    /** 可用半径档位，index 即 tier。 */
    private static final int[] RADIUS_VALUES = {8, 16, 32, 64};
    private static final int MIN_TIER = 0;
    private static final int MAX_TIER = RADIUS_VALUES.length - 1;

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
            int radius = RADIUS_VALUES[tier];
            player.displayClientMessage(Component.literal("结界半径档位：" + (tier + 1) + "（半径 " + radius + "）"), true);
        });
        context.setPacketHandled(true);
    }
}
