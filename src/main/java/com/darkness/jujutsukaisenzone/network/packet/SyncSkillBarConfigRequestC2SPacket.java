package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSkillBarConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求同步指定术式的快捷栏配置。
 * 服务端收到后通过 {@code SyncSkillBarConfigS2CPacket} 回传。
 */
public class SyncSkillBarConfigRequestC2SPacket
{
    private final String techniqueId;

    public SyncSkillBarConfigRequestC2SPacket(String techniqueId)
    {
        this.techniqueId = techniqueId;
    }

    public static void encode(SyncSkillBarConfigRequestC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.techniqueId);
    }

    public static SyncSkillBarConfigRequestC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncSkillBarConfigRequestC2SPacket(buffer.readUtf());
    }

    public static void handle(SyncSkillBarConfigRequestC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            PlayerSkillBarConfig.sanitizeSlots(player, packet.techniqueId);
            Map<Integer, String> config = PlayerSkillBarConfig.getAllSlots(player, packet.techniqueId);
            String[] skillIds = new String[9];
            for (int i = 0; i < 9; i++)
            {
                skillIds[i] = config.getOrDefault(i + 1, "");
            }
            com.darkness.jujutsukaisenzone.network.ModNetwork.sendSkillBarConfigSync(player, packet.techniqueId, skillIds);
        });
        context.setPacketHandled(true);
    }
}
