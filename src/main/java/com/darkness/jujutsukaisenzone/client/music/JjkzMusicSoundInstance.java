package com.darkness.jujutsukaisenzone.client.music;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * 本地非立体 BGM：{@code relative + NONE}。
 * 走 {@link SoundSource#MASTER}，与任务简报语音同一通道——CMDCam 运镜时原版会停 {@code MUSIC}。
 */
public final class JjkzMusicSoundInstance extends AbstractTickableSoundInstance
{
    private float peakVolume = 1.0F;
    private int fadeInRemaining;
    private int fadeInTotal;
    private int fadeOutRemaining;
    private int fadeOutTotal;
    private boolean fadeOutRequested;
    /** 峰值播完后自动淡出；0 表示不限时。 */
    private int durationRemaining;
    private final int autoFadeOutMs;

    public JjkzMusicSoundInstance(SoundEvent soundEvent, boolean loop, int fadeInMs)
    {
        this(soundEvent, loop, fadeInMs, 1.0F, 0, 0);
    }

    public JjkzMusicSoundInstance(
            SoundEvent soundEvent, boolean loop, int fadeInMs, float volume, int durationMs)
    {
        this(soundEvent, loop, fadeInMs, volume, durationMs, 0);
    }

    public JjkzMusicSoundInstance(
            SoundEvent soundEvent,
            boolean loop,
            int fadeInMs,
            float volume,
            int durationMs,
            int fadeOutMs)
    {
        super(soundEvent, SoundSource.MASTER, RandomSource.create());
        this.looping = loop;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.pitch = 1.0F;
        this.peakVolume = Mth.clamp(volume, 0.0F, 1.0F);
        this.durationRemaining = durationMs <= 0 ? 0 : msToTicks(durationMs);
        int resolvedFadeOut = fadeOutMs > 0 ? fadeOutMs : (fadeInMs > 0 ? fadeInMs : 400);
        this.autoFadeOutMs = resolvedFadeOut;
        int fadeInTicks = msToTicks(fadeInMs);
        if (fadeInTicks <= 0)
        {
            this.volume = this.peakVolume;
            this.fadeInRemaining = 0;
            this.fadeInTotal = 0;
        }
        else
        {
            this.volume = 0.0F;
            this.fadeInTotal = fadeInTicks;
            this.fadeInRemaining = fadeInTicks;
        }
    }

    /** 淡入从 0 起；不覆盖的话原版会直接 skip 整条音。 */
    @Override
    public boolean canStartSilent()
    {
        return true;
    }

    public void requestFadeOut(int fadeOutMs)
    {
        if (this.isStopped() || this.fadeOutRequested)
        {
            return;
        }
        int ticks = msToTicks(fadeOutMs);
        if (ticks <= 0)
        {
            this.stop();
            return;
        }
        this.fadeOutRequested = true;
        this.fadeOutTotal = ticks;
        this.fadeOutRemaining = ticks;
        this.fadeInRemaining = 0;
    }

    public void requestStopNow()
    {
        if (!this.isStopped())
        {
            this.stop();
        }
    }

    @Override
    public void tick()
    {
        if (this.fadeOutRequested)
        {
            this.fadeOutRemaining--;
            float t = this.fadeOutTotal <= 0
                    ? 0.0F
                    : this.fadeOutRemaining / (float) this.fadeOutTotal;
            this.volume = this.peakVolume * Mth.clamp(t, 0.0F, 1.0F);
            if (this.fadeOutRemaining <= 0)
            {
                this.stop();
            }
            return;
        }
        if (this.fadeInRemaining > 0)
        {
            this.fadeInRemaining--;
            float t = 1.0F - (this.fadeInRemaining / (float) this.fadeInTotal);
            this.volume = this.peakVolume * Mth.clamp(t, 0.0F, 1.0F);
        }
        if (this.durationRemaining > 0)
        {
            this.durationRemaining--;
            if (this.durationRemaining <= 0)
            {
                this.requestFadeOut(this.autoFadeOutMs);
            }
        }
    }

    private static int msToTicks(int ms)
    {
        if (ms <= 0)
        {
            return 0;
        }
        return Math.max(1, (ms + 25) / 50);
    }
}
