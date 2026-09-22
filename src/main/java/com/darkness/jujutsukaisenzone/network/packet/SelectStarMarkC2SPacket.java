package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：战斗模式下用鼠标侧键 4/5（调节模式切至「星星标记」）切换当前标记。
 */
public class SelectStarMarkC2SPacket
{
    private final int delta;

    public SelectStarMarkC2SPacket(int delta)
    {
        this.delta = delta;
    }

    public static void encode(SelectStarMarkC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.delta);
    }

    public static SelectStarMarkC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new SelectStarMarkC2SPacket(buffer.readVarInt());
    }

    public static void handle(SelectStarMarkC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            int delta = Integer.compare(packet.delta, 0);
            if (delta == 0
                    || !EpicFightCompat.isBattleMode(player)
                    || !PlayerTechniqueData.isLoveRendezvous(player))
            {
                return;
            }
            StarMarkType selected = PlayerStarMarkData.cycleSelected(player, delta);
            player.displayClientMessage(Component.literal("当前标记：" + selected.displayName()), true);
        });
        context.setPacketHandled(true);
    }
}
