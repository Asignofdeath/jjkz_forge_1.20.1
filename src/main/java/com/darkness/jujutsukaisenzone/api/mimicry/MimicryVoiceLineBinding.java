package com.darkness.jujutsukaisenzone.api.mimicry;

import net.minecraft.sounds.SoundEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 某拟态角色在指定 {@link MimicryVoiceLineEvent} 下的台词配置。
 *
 * @param triggerChance 触发概率，{@code 0..1}；{@code 1.0} 表示必定尝试播放（仍须拟态开启且角色匹配）
 * @param sounds        候选台词音效，触发后均匀随机其一
 * @param volume        播放音量
 * @param pitch         播放音调
 */
public record MimicryVoiceLineBinding(
        float triggerChance,
        List<SoundEvent> sounds,
        float volume,
        float pitch)
{
    public MimicryVoiceLineBinding
    {
        Objects.requireNonNull(sounds, "sounds");
        if (triggerChance < 0.0F || triggerChance > 1.0F)
        {
            throw new IllegalArgumentException("triggerChance must be in [0, 1], was " + triggerChance);
        }
        if (volume < 0.0F)
        {
            throw new IllegalArgumentException("volume must be >= 0, was " + volume);
        }
        if (pitch <= 0.0F)
        {
            throw new IllegalArgumentException("pitch must be > 0, was " + pitch);
        }
        sounds = List.copyOf(sounds);
        if (sounds.isEmpty())
        {
            throw new IllegalArgumentException("sounds must not be empty");
        }
        for (SoundEvent sound : sounds)
        {
            Objects.requireNonNull(sound, "sound");
        }
    }

    public static MimicryVoiceLineBinding of(float triggerChance, SoundEvent... sounds)
    {
        return new MimicryVoiceLineBinding(triggerChance, Arrays.asList(sounds), 1.0F, 1.0F);
    }

    public static MimicryVoiceLineBinding of(float triggerChance, float volume, float pitch, SoundEvent... sounds)
    {
        return new MimicryVoiceLineBinding(triggerChance, Arrays.asList(sounds), volume, pitch);
    }
}
