package com.darkness.jujutsukaisenzone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端假死表现状态：谁在假死（躺姿）、本地玩家是否开离体相机。
 */
public final class ClientTenShadowsFalseDeathState
{
    private static final Set<UUID> FALSE_DEAD = ConcurrentHashMap.newKeySet();
    /** 本地玩家进入假死相机的时刻（毫秒），用于蒙层淡入。 */
    private static long localActivateMs;

    private ClientTenShadowsFalseDeathState()
    {
    }

    public static void apply(UUID entityId, boolean active)
    {
        if (entityId == null)
        {
            return;
        }
        if (active)
        {
            FALSE_DEAD.add(entityId);
        }
        else
        {
            FALSE_DEAD.remove(entityId);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entityId.equals(mc.player.getUUID()))
        {
            if (active)
            {
                localActivateMs = System.currentTimeMillis();
                TenShadowsFalseDeathCamera.activateAt(mc.player);
            }
            else
            {
                localActivateMs = 0L;
                TenShadowsFalseDeathCamera.deactivate();
            }
        }
    }

    public static boolean isFalseDead(UUID entityId)
    {
        return entityId != null && FALSE_DEAD.contains(entityId);
    }

    public static boolean isFalseDead(Entity entity)
    {
        return entity != null && isFalseDead(entity.getUUID());
    }

    public static boolean isLocalCameraActive()
    {
        return TenShadowsFalseDeathCamera.isActive();
    }

    /** @return 0～1，本地假死蒙层淡入进度 */
    public static float localVeilFade(int fadeMs)
    {
        if (!isLocalCameraActive() || localActivateMs <= 0L || fadeMs <= 0)
        {
            return isLocalCameraActive() ? 1.0F : 0.0F;
        }
        float t = (System.currentTimeMillis() - localActivateMs) / (float) fadeMs;
        if (t >= 1.0F)
        {
            return 1.0F;
        }
        if (t <= 0.0F)
        {
            return 0.0F;
        }
        return t;
    }

    public static void clearAll()
    {
        FALSE_DEAD.clear();
        localActivateMs = 0L;
        TenShadowsFalseDeathCamera.deactivate();
    }
}
