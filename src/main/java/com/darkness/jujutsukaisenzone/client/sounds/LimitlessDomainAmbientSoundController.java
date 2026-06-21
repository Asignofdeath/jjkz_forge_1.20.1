package com.darkness.jujutsukaisenzone.client.sounds;

import com.darkness.jujutsukaisenzone.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端：无量空处球心环境循环音（与施法者 UUID 绑定，领域关闭包触发停止）。
 */
public final class LimitlessDomainAmbientSoundController
{
    private static final Map<UUID, LimitlessDomainCenterLoopSound> ACTIVE = new ConcurrentHashMap<>();

    private LimitlessDomainAmbientSoundController()
    {
    }

    public static void start(UUID casterUuid, double x, double y, double z)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
        {
            return;
        }
        SoundEvent soundEvent = ModSounds.LIMITLESS_DOMAIN_CENTER_AMBIENT.get();
        if (soundEvent == null)
        {
            return;
        }
        stop(casterUuid);
        LimitlessDomainCenterLoopSound instance = new LimitlessDomainCenterLoopSound(soundEvent, x, y, z);
        ACTIVE.put(casterUuid, instance);
        minecraft.getSoundManager().play(instance);
    }

    public static void stop(UUID casterUuid)
    {
        LimitlessDomainCenterLoopSound existing = ACTIVE.remove(casterUuid);
        if (existing != null)
        {
            existing.requestStop();
        }
    }
}
