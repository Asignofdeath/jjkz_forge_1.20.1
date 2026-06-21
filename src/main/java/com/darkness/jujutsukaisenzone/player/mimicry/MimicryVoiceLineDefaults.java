package com.darkness.jujutsukaisenzone.player.mimicry;

import com.darkness.jujutsukaisenzone.api.mimicry.MimicryVoiceLineEvents;
import com.darkness.jujutsukaisenzone.api.mimicry.MimicryVoiceLines;
import com.darkness.jujutsukaisenzone.registry.ModSounds;

/**
 * 本模组内置拟态台词注册；在 mod 初始化阶段调用一次 {@link #register()}.
 */
public final class MimicryVoiceLineDefaults
{
    private MimicryVoiceLineDefaults()
    {
    }

    public static void register()
    {
        MimicryVoiceLines.register(
                MimicryVoiceLineEvents.SHRINE_KAI_SLASH,
                MimicryCharacterRegistry.CHARACTER_SUKUNA,
                0.30F,
                ModSounds.SUKUNA_KAI.get(),
                ModSounds.SUKUNA_HMPH.get());
        MimicryVoiceLines.register(
                MimicryVoiceLineEvents.SHRINE_FUGA_LAUNCH,
                MimicryCharacterRegistry.CHARACTER_SUKUNA,
                0.40F,
                ModSounds.SUKUNA_FUGA.get());
        MimicryVoiceLines.register(
                MimicryVoiceLineEvents.SHRINE_FUGA_BOOM_END,
                MimicryCharacterRegistry.CHARACTER_SUKUNA,
                0.30F,
                ModSounds.SUKUNA_ZONGAI_TANOSHIMETA.get());
    }
}
