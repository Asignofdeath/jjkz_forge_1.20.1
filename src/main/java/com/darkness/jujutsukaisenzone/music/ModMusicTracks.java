package com.darkness.jujutsukaisenzone.music;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组曲库。剧情与鉴赏共用。
 * <p>
 * 添加曲目步骤：
 * <ol>
 *   <li>放入 {@code assets/.../sounds/music/&lt;file&gt;.ogg}</li>
 *   <li>在 {@code sounds.json} 增加 {@code music.&lt;id&gt;}，且 {@code "stream": true}</li>
 *   <li>在本类 static 块里 {@link #register} 一条</li>
 *   <li>补 lang：{@code music.jujutsukaisenzone.&lt;id&gt;.title/source}</li>
 * </ol>
 */
public final class ModMusicTracks
{
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, JuJutsuKaisenZone.MODID);

    private static final Map<String, ModMusicTrack> BY_ID = new LinkedHashMap<>();

    static
    {
        /* 曲目在有 ogg 后于此 register；无音频时勿注册，以免点播放报缺失。 */
    }

    private ModMusicTracks()
    {
    }

    public static void register(IEventBus bus)
    {
        SOUND_EVENTS.register(bus);
    }

    /**
     * @param id         逻辑 id
     * @param soundFile  相对 sounds/ 的路径（无扩展名），如 {@code music/village_calm}
     * @param titleKey   曲名 lang
     * @param sourceKey  来源 lang
     * @param loop       默认循环
     */
    public static ModMusicTrack register(
            String id,
            String soundFile,
            String titleKey,
            String sourceKey,
            boolean loop)
    {
        String soundEventId = "music." + id;
        RegistryObject<SoundEvent> sound = SOUND_EVENTS.register(
                soundEventId,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, soundEventId)));
        ModMusicTrack track = new ModMusicTrack(id, sound, titleKey, sourceKey, loop);
        if (BY_ID.put(id, track) != null)
        {
            throw new IllegalStateException("Duplicate music track id: " + id);
        }
        /* soundFile 仅作文档约定；实际映射在 sounds.json 的 name 字段。 */
        if (soundFile == null || soundFile.isBlank())
        {
            throw new IllegalArgumentException("soundFile required for track " + id);
        }
        return track;
    }

    @Nullable
    public static ModMusicTrack byId(@Nullable String id)
    {
        if (id == null || id.isEmpty())
        {
            return null;
        }
        return BY_ID.get(id);
    }

    public static Collection<ModMusicTrack> all()
    {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static boolean isEmpty()
    {
        return BY_ID.isEmpty();
    }
}
