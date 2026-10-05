package com.darkness.jujutsukaisenzone.music;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.SyncMusicPlayS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMusicStopS2CPacket;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;

/** 服务端入口：向指定玩家发 S2C，由客户端导演本地播放。 */
public final class ModMusic
{
    public static final int DEFAULT_FADE_MS = 400;

    private ModMusic()
    {
    }

    public static void play(ServerPlayer player, String trackId, MusicPriority priority)
    {
        play(player, trackId, priority, false, DEFAULT_FADE_MS);
    }

    public static void play(ServerPlayer player, String trackId, MusicPriority priority, boolean loop)
    {
        play(player, trackId, priority, loop, DEFAULT_FADE_MS);
    }

    public static void play(
            ServerPlayer player,
            String trackId,
            MusicPriority priority,
            boolean loop,
            int fadeMs)
    {
        play(player, trackId, priority, loop, fadeMs, 1.0F, 0);
    }

    public static void play(
            ServerPlayer player,
            String trackId,
            MusicPriority priority,
            boolean loop,
            int fadeMs,
            float volume,
            int durationMs)
    {
        play(player, trackId, priority, loop, fadeMs, volume, durationMs, 0);
    }

    public static void play(
            ServerPlayer player,
            String trackId,
            MusicPriority priority,
            boolean loop,
            int fadeMs,
            float volume,
            int durationMs,
            int fadeOutMs)
    {
        if (player == null || trackId == null || trackId.isEmpty() || priority == null)
        {
            return;
        }
        NetworkSenders.toPlayer(
                player,
                new SyncMusicPlayS2CPacket(trackId, priority, loop, fadeMs, volume, durationMs, fadeOutMs));
    }

    public static void stop(ServerPlayer player)
    {
        stop(player, DEFAULT_FADE_MS);
    }

    public static void stop(ServerPlayer player, int fadeMs)
    {
        if (player == null)
        {
            return;
        }
        NetworkSenders.toPlayer(player, SyncMusicStopS2CPacket.all(fadeMs));
    }

    public static void stopIf(ServerPlayer player, MusicPriority priority)
    {
        stopIf(player, priority, DEFAULT_FADE_MS);
    }

    public static void stopIf(ServerPlayer player, @Nullable MusicPriority priority, int fadeMs)
    {
        if (player == null)
        {
            return;
        }
        if (priority == null)
        {
            stop(player, fadeMs);
            return;
        }
        NetworkSenders.toPlayer(player, SyncMusicStopS2CPacket.of(priority, fadeMs));
    }
}
