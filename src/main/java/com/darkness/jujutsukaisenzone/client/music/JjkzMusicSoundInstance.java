package com.darkness.jujutsukaisenzone.client.music;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * 本地非立体 BGM：{@code relative + NONE}，走 {@link SoundSource#MUSIC}。
 */
public final class JjkzMusicSoundInstance extends AbstractTickableSoundInstance
{
    private float peakVolume = 1.0F;
    private int fadeInRemaining;
    private int fadeInTotal;
    private int fadeOutRemaining;
    private int fadeOutTotal;
    private boolean fadeOutRequested;

    public JjkzMusicSoundInstance(SoundEvent soundEvent, boolean loop, int fadeInMs)
    {
        super(soundEvent, SoundSource.MUSIC, RandomSource.create());
        this.looping = loop;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.pitch = 1.0F;
        this.peakVolume = 1.0F;
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
