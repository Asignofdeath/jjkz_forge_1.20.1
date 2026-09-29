package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.player.StarMark;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 客户端缓存：各施术者持有的星星标记，供喷漆贴花渲染。 */
@OnlyIn(Dist.CLIENT)
public final class StarMarkDecalClient
{
    private static final Map<UUID, List<StarMark>> BY_OWNER = new ConcurrentHashMap<>();

    private StarMarkDecalClient()
    {
    }

    public static void accept(UUID ownerId, List<StarMark> marks)
    {
        if (ownerId == null)
        {
            return;
        }
        if (marks == null || marks.isEmpty())
        {
            BY_OWNER.remove(ownerId);
            return;
        }
        BY_OWNER.put(ownerId, List.copyOf(marks));
    }

    public static Map<UUID, List<StarMark>> snapshot()
    {
        return Collections.unmodifiableMap(BY_OWNER);
    }

    public static void clear()
    {
        BY_OWNER.clear();
    }
}
