package com.darkness.jujutsukaisenzone.client.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * 球心处循环环境音效（{@link SoundSource#AMBIENT}），由 {@link LimitlessDomainAmbientSoundController} 在领域关闭时停止。
 */
public final class LimitlessDomainCenterLoopSound extends AbstractTickableSoundInstance
{
    public LimitlessDomainCenterLoopSound(SoundEvent soundEvent, double x, double y, double z)
    {
        super(soundEvent, SoundSource.AMBIENT, RandomSource.create());
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = 0.2F;
        this.pitch = 1.0F;
        this.looping = true;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.relative = false;
    }

    @Override
    public void tick()
    {
        // 循环由引擎处理；停止由 requestStop() → AbstractTickableSoundInstance#stop
    }

    public void requestStop()
    {
        if (!this.isStopped())
        {
            this.stop();
        }
    }
}
