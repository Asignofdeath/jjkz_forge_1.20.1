package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.registry.ModSounds;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.RegistryObject;

/**
 * 剧情台词语音：出字幕时播对应 ogg，换句/关黑幕立刻停上一句。
 * 时长由服务端按句表对齐，本类不负责切句。
 */
public final class QuestDialogVoice
{
    private static final Map<String, RegistryObject<SoundEvent>> BY_CAPTION = new HashMap<>();
    @Nullable
    private static SoundEvent playing;

    static
    {
        BY_CAPTION.put("quest.jujutsukaisenzone.main_1_1.talk.1", ModSounds.QUEST_MAIN_1_1_TALK_1);
        BY_CAPTION.put("quest.jujutsukaisenzone.main_1_1.talk.2", ModSounds.QUEST_MAIN_1_1_TALK_2);
        BY_CAPTION.put("quest.jujutsukaisenzone.main_1_1.talk.3", ModSounds.QUEST_MAIN_1_1_TALK_3);
        BY_CAPTION.put("quest.jujutsukaisenzone.main_1_1.talk.4", ModSounds.QUEST_MAIN_1_1_TALK_4);
        BY_CAPTION.put("quest.jujutsukaisenzone.main_1_1.talk.5", ModSounds.QUEST_MAIN_1_1_TALK_5);
        BY_CAPTION.put("quest.jujutsukaisenzone.main_1_1.talk.7", ModSounds.QUEST_MAIN_1_1_TALK_7);
        BY_CAPTION.put("quest.jujutsukaisenzone.main_1_1.talk.8", ModSounds.QUEST_MAIN_1_1_TALK_8);
        BY_CAPTION.put("quest.jujutsukaisenzone.main_1_1.talk.10", ModSounds.QUEST_MAIN_1_1_TALK_10);
        BY_CAPTION.put("quest.jujutsukaisenzone.main_1_1.talk.11", ModSounds.QUEST_MAIN_1_1_TALK_11);
    }

    private QuestDialogVoice()
    {
    }

    public static void playCaption(String captionKey)
    {
        if (captionKey == null || captionKey.isEmpty())
        {
            stop();
            return;
        }
        RegistryObject<SoundEvent> holder = BY_CAPTION.get(captionKey);
        if (holder == null)
        {
            stop();
            return;
        }
        SoundEvent next = holder.get();
        stop();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(new SimpleSoundInstance(
                next.getLocation(),
                SoundSource.VOICE,
                1.0F,
                1.0F,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true));
        playing = next;
    }

    public static void stop()
    {
        if (playing == null)
        {
            return;
        }
        Minecraft.getInstance().getSoundManager().stop(playing.getLocation(), SoundSource.VOICE);
        playing = null;
    }
}
