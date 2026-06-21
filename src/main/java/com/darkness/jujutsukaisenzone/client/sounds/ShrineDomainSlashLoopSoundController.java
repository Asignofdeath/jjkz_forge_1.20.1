package com.darkness.jujutsukaisenzone.client.sounds;

import com.darkness.jujutsukaisenzone.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端：伏魔神龛斩击循环音（与施法者 UUID 绑定）。
 */
public final class ShrineDomainSlashLoopSoundController
{
    private static final Map<UUID, ShrineDomainSlashLoopSound> ACTIVE = new ConcurrentHashMap<>();

    private ShrineDomainSlashLoopSoundController()
    {
    }

    public static void start(UUID casterUuid)
    {
        if (ACTIVE.containsKey(casterUuid))
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
        {
            return;
        }

        SoundEvent soundEvent = ModSounds.SHRINE_SLASH.get();
        if (soundEvent == null)
        {
            return;
        }

        ShrineDomainSlashLoopSound instance = new ShrineDomainSlashLoopSound(soundEvent);
        ACTIVE.put(casterUuid, instance);
        minecraft.getSoundManager().play(instance);
    }

    public static void stop(UUID casterUuid)
    {
        ShrineDomainSlashLoopSound existing = ACTIVE.remove(casterUuid);
        if (existing != null)
        {
            existing.requestStop();
            Minecraft.getInstance().getSoundManager().stop(existing);
        }
    }
}
