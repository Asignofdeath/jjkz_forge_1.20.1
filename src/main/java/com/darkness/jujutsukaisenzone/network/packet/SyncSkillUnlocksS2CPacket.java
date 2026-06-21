package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步已解锁技能的 unlockTag 状态。
 */
public final class SyncSkillUnlocksS2CPacket
{
    private final Map<String, Boolean> unlockTags;

    public SyncSkillUnlocksS2CPacket(Map<String, Boolean> unlockTags)
    {
        this.unlockTags = Map.copyOf(unlockTags);
    }

    public static void encode(SyncSkillUnlocksS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.unlockTags.size());
        for (Map.Entry<String, Boolean> entry : packet.unlockTags.entrySet())
        {
            buffer.writeUtf(entry.getKey());
            buffer.writeBoolean(entry.getValue());
        }
    }

    public static SyncSkillUnlocksS2CPacket decode(FriendlyByteBuf buffer)
    {
        int count = buffer.readVarInt();
        Map<String, Boolean> unlockTags = new LinkedHashMap<>();
        for (int i = 0; i < count; i++)
        {
            unlockTags.put(buffer.readUtf(), buffer.readBoolean());
        }
        return new SyncSkillUnlocksS2CPacket(unlockTags);
    }

    public static void handle(SyncSkillUnlocksS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            SkillUnlockLogic.applyClientUnlockTags(player, packet.unlockTags);
        });
        context.setPacketHandled(true);
    }
}
