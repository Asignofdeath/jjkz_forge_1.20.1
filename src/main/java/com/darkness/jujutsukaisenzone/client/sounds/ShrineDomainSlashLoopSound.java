package com.darkness.jujutsukaisenzone.client.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 伏魔神龛领域斩击循环音（玩家本地播放），由 {@link ShrineDomainSlashLoopSoundController} 停止。
 */
public final class ShrineDomainSlashLoopSound extends AbstractTickableSoundInstance
{
    public ShrineDomainSlashLoopSound(SoundEvent soundEvent)
    {
        super(soundEvent, SoundSource.PLAYERS, RandomSource.create());
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.looping = true;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick()
    {
    }

    public void requestStop()
    {
        if (!this.isStopped())
        {
            this.stop();
        }
    }
}
