package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class SyncSkillLevelsS2CPacket
{
    private final Map<String, Integer> levelTags;

    public SyncSkillLevelsS2CPacket(Map<String, Integer> levelTags)
    {
        this.levelTags = Map.copyOf(levelTags);
    }

    public static void encode(SyncSkillLevelsS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.levelTags.size());
        for (Map.Entry<String, Integer> entry : packet.levelTags.entrySet())
        {
            buffer.writeUtf(entry.getKey());
            buffer.writeVarInt(Math.max(1, entry.getValue()));
        }
    }

    public static SyncSkillLevelsS2CPacket decode(FriendlyByteBuf buffer)
    {
        int count = buffer.readVarInt();
        Map<String, Integer> values = new LinkedHashMap<>();
        for (int i = 0; i < count; i++)
        {
            values.put(buffer.readUtf(), Math.max(1, buffer.readVarInt()));
        }
        return new SyncSkillLevelsS2CPacket(values);
    }

    public static void handle(SyncSkillLevelsS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            SkillUnlockLogic.applyClientLevelTags(player, packet.levelTags);
        });
        context.setPacketHandled(true);
    }
}
