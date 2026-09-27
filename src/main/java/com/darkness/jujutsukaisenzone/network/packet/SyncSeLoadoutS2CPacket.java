package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerSeLoadoutData;
import com.darkness.jujutsukaisenzone.player.SeEquippedEntry;
import com.darkness.jujutsukaisenzone.technique.se.PanelStat;
import com.darkness.jujutsukaisenzone.technique.se.SePenalty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class SyncSeLoadoutS2CPacket
{
    private final Map<String, SeEquippedEntry> entries;

    public SyncSeLoadoutS2CPacket(Map<String, SeEquippedEntry> entries)
    {
        this.entries = entries == null ? Map.of() : Map.copyOf(entries);
    }

    public static void encode(SyncSeLoadoutS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.entries.size());
        for (Map.Entry<String, SeEquippedEntry> e : packet.entries.entrySet())
        {
            SeEquippedEntry entry = e.getValue();
            buffer.writeUtf(e.getKey() == null ? "" : e.getKey());
            buffer.writeUtf(entry.traitId == null ? "" : entry.traitId);
            buffer.writeUtf(entry.penalty.stat().id());
            buffer.writeDouble(entry.penalty.amount());
        }
    }

    public static SyncSeLoadoutS2CPacket decode(FriendlyByteBuf buffer)
    {
        int count = buffer.readVarInt();
        Map<String, SeEquippedEntry> entries = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++)
        {
            String id = buffer.readUtf();
            String traitId = buffer.readUtf();
            String statId = buffer.readUtf();
            double amount = buffer.readDouble();
            PanelStat stat = PanelStat.find(statId);
            SePenalty penalty = stat == null
                    ? new SePenalty(PanelStat.PHYSICAL_POWER, 0.0D)
                    : new SePenalty(stat, amount);
            if (id != null && !id.isBlank())
            {
                entries.put(id, new SeEquippedEntry(traitId, penalty));
            }
        }
        return new SyncSeLoadoutS2CPacket(entries);
    }

    public static void handle(SyncSeLoadoutS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerSeLoadoutData.applyClientSnapshot(player, packet.entries);
        });
        context.setPacketHandled(true);
    }
}
