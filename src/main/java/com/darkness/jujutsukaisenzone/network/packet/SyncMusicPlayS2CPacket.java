package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.music.JjkzMusicDirector;
import com.darkness.jujutsukaisenzone.music.MusicPriority;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：请求导演播放曲目。 */
public class SyncMusicPlayS2CPacket
{
    private final String trackId;
    private final MusicPriority priority;
    private final boolean loop;
    private final int fadeMs;
    private final float volume;
    /** 峰值播放时长；0 表示不限时。 */
    private final int durationMs;
    private final int fadeOutMs;

    public SyncMusicPlayS2CPacket(String trackId, MusicPriority priority, boolean loop, int fadeMs)
    {
        this(trackId, priority, loop, fadeMs, 1.0F, 0, 0);
    }

    public SyncMusicPlayS2CPacket(
            String trackId, MusicPriority priority, boolean loop, int fadeMs, float volume, int durationMs)
    {
        this(trackId, priority, loop, fadeMs, volume, durationMs, 0);
    }

    public SyncMusicPlayS2CPacket(
            String trackId,
            MusicPriority priority,
            boolean loop,
            int fadeMs,
            float volume,
            int durationMs,
            int fadeOutMs)
    {
        this.trackId = trackId == null ? "" : trackId;
        this.priority = priority == null ? MusicPriority.STORY : priority;
        this.loop = loop;
        this.fadeMs = Math.max(0, fadeMs);
        this.volume = volume;
        this.durationMs = Math.max(0, durationMs);
        this.fadeOutMs = Math.max(0, fadeOutMs);
    }

    public static void encode(SyncMusicPlayS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.trackId);
        buffer.writeVarInt(packet.priority.ordinal());
        buffer.writeBoolean(packet.loop);
        buffer.writeVarInt(packet.fadeMs);
        buffer.writeFloat(packet.volume);
        buffer.writeVarInt(packet.durationMs);
        buffer.writeVarInt(packet.fadeOutMs);
    }

    public static SyncMusicPlayS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncMusicPlayS2CPacket(
                buffer.readUtf(),
                MusicPriority.fromOrdinalSafe(buffer.readVarInt()),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readVarInt());
    }

    public static void handle(SyncMusicPlayS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> JjkzMusicDirector.play(
                packet.trackId,
                packet.priority,
                packet.loop,
                packet.fadeMs,
                packet.volume,
                packet.durationMs,
                packet.fadeOutMs));
        context.setPacketHandled(true);
    }
}
