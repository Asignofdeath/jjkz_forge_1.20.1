package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.music.JjkzMusicDirector;
import com.darkness.jujutsukaisenzone.music.MusicPriority;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：停止全部或指定优先级的 BGM。 */
public class SyncMusicStopS2CPacket
{
    /** {@code null} 表示停全部。 */
    @Nullable
    private final MusicPriority priority;
    private final int fadeMs;

    private SyncMusicStopS2CPacket(@Nullable MusicPriority priority, int fadeMs)
    {
        this.priority = priority;
        this.fadeMs = Math.max(0, fadeMs);
    }

    public static SyncMusicStopS2CPacket all(int fadeMs)
    {
        return new SyncMusicStopS2CPacket(null, fadeMs);
    }

    public static SyncMusicStopS2CPacket of(MusicPriority priority, int fadeMs)
    {
        return new SyncMusicStopS2CPacket(priority, fadeMs);
    }

    public static void encode(SyncMusicStopS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.priority != null);
        if (packet.priority != null)
        {
            buffer.writeVarInt(packet.priority.ordinal());
        }
        buffer.writeVarInt(packet.fadeMs);
    }

    public static SyncMusicStopS2CPacket decode(FriendlyByteBuf buffer)
    {
        MusicPriority priority = null;
        if (buffer.readBoolean())
        {
            priority = MusicPriority.fromOrdinalSafe(buffer.readVarInt());
        }
        return new SyncMusicStopS2CPacket(priority, buffer.readVarInt());
    }

    public static void handle(SyncMusicStopS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.priority == null)
            {
                JjkzMusicDirector.stop(packet.fadeMs);
            }
            else
            {
                JjkzMusicDirector.stopIf(packet.priority, packet.fadeMs);
            }
        });
        context.setPacketHandled(true);
    }
}
