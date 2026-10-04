package com.darkness.jujutsukaisenzone.client.music;

import com.darkness.jujutsukaisenzone.music.ModMusicTrack;
import com.darkness.jujutsukaisenzone.music.ModMusicTracks;
import com.darkness.jujutsukaisenzone.music.MusicPriority;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 客户端单通道 BGM 导演。成功开播时弹出 {@link MusicNowPlayingToast}。
 */
@OnlyIn(Dist.CLIENT)
public final class JjkzMusicDirector
{
    @Nullable
    private static MusicPriority currentPriority;
    @Nullable
    private static String currentTrackId;
    @Nullable
    private static JjkzMusicSoundInstance currentSound;
    /** 正在淡出、已让出通道的实例；新开播前硬停，避免叠音。 */
    @Nullable
    private static JjkzMusicSoundInstance releasingSound;

    private JjkzMusicDirector()
    {
    }

    /**
     * @return 是否实际开播（含同级替换）；被更高优先级挡住时为 false
     */
    public static boolean play(String trackId, MusicPriority priority, boolean loop, int fadeMs)
    {
        ModMusicTrack track = ModMusicTracks.byId(trackId);
        if (track == null || priority == null)
        {
            return false;
        }
        if (!priority.canOverride(currentPriority))
        {
            return false;
        }

        hardStop(releasingSound);
        releasingSound = null;
        stopInternal(0);
        boolean useLoop = loop || track.loop();
        JjkzMusicSoundInstance instance = new JjkzMusicSoundInstance(track.sound().get(), useLoop, fadeMs);
        Minecraft.getInstance().getSoundManager().play(instance);
        currentSound = instance;
        currentPriority = priority;
        currentTrackId = track.id();
        MusicNowPlayingToast.show(track);
        return true;
    }

    public static void stop(int fadeMs)
    {
        stopInternal(fadeMs);
    }

    public static void stopIf(MusicPriority priority, int fadeMs)
    {
        if (priority == null || currentPriority != priority)
        {
            return;
        }
        stop(fadeMs);
    }

    public static void stopAppreciationNow()
    {
        stopIf(MusicPriority.APPRECIATION, 0);
    }

    @Nullable
    public static MusicPriority currentPriority()
    {
        return currentPriority;
    }

    @Nullable
    public static String currentTrackId()
    {
        return currentTrackId;
    }

    public static boolean isPlayingAppreciation(String trackId)
    {
        return currentPriority == MusicPriority.APPRECIATION
                && trackId != null
                && trackId.equals(currentTrackId);
    }

    /** 鉴赏 UI：当前是否可点歌（无剧情/战斗占用）。 */
    public static boolean canStartAppreciation()
    {
        return currentPriority == null || currentPriority == MusicPriority.APPRECIATION;
    }

    public static void tick()
    {
        if (releasingSound != null && releasingSound.isStopped())
        {
            releasingSound = null;
        }
        if (currentSound != null && currentSound.isStopped())
        {
            currentSound = null;
            currentPriority = null;
            currentTrackId = null;
        }
    }

    private static void stopInternal(int fadeMs)
    {
        JjkzMusicSoundInstance sound = currentSound;
        currentSound = null;
        currentPriority = null;
        currentTrackId = null;
        if (sound == null)
        {
            return;
        }
        if (fadeMs <= 0)
        {
            sound.requestStopNow();
            return;
        }
        hardStop(releasingSound);
        sound.requestFadeOut(fadeMs);
        releasingSound = sound;
    }

    private static void hardStop(@Nullable JjkzMusicSoundInstance sound)
    {
        if (sound != null)
        {
            sound.requestStopNow();
        }
    }
}
