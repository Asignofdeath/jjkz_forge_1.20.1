package com.darkness.jujutsukaisenzone.music;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * 曲目元数据。剧情 / 战斗 / 鉴赏共用同一 {@link #id}。
 *
 * @param id        逻辑 id（网络包与指令用）
 * @param sound     已注册的 SoundEvent
 * @param titleKey  曲名 lang key
 * @param sourceKey 来源 / 授权说明 lang key
 * @param loop      默认是否循环（调用方可覆盖）
 */
public record ModMusicTrack(
        String id,
        RegistryObject<SoundEvent> sound,
        String titleKey,
        String sourceKey,
        boolean loop)
{
    public Component title()
    {
        return Component.translatable(titleKey);
    }

    public Component source()
    {
        return Component.translatable(sourceKey);
    }
}
