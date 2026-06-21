package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSkillBarConfig;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：保存指定术式的 9 栏位快捷栏配置。
 */
public class SyncSkillBarConfigC2SPacket
{
    private final String techniqueId;
    private final String[] skillIds; // 索引 0~8 对应槽位 1~9

    public SyncSkillBarConfigC2SPacket(String techniqueId, String[] skillIds)
    {
        this.techniqueId = techniqueId;
        this.skillIds = skillIds;
    }

    public static void encode(SyncSkillBarConfigC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.techniqueId);
        for (int i = 0; i < 9; i++)
        {
            buffer.writeUtf(packet.skillIds[i]);
        }
    }

    public static SyncSkillBarConfigC2SPacket decode(FriendlyByteBuf buffer)
    {
        String tid = buffer.readUtf();
        String[] ids = new String[9];
        for (int i = 0; i < 9; i++)
        {
            ids[i] = buffer.readUtf();
        }
        return new SyncSkillBarConfigC2SPacket(tid, ids);
    }

    public static void handle(SyncSkillBarConfigC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            String[] sanitized = new String[9];
            for (int i = 0; i < 9; i++)
            {
                sanitized[i] = SkillUnlockLogic.sanitizeSkillBarSlot(
                        player, packet.techniqueId, packet.skillIds[i]);
                PlayerSkillBarConfig.setSkillId(player, packet.techniqueId, i + 1, sanitized[i]);
            }
            com.darkness.jujutsukaisenzone.network.ModNetwork.sendSkillBarConfigSync(player, packet.techniqueId, sanitized);
        });
        context.setPacketHandled(true);
    }
}
