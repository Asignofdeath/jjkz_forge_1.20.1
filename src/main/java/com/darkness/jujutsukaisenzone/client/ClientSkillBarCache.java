package com.darkness.jujutsukaisenzone.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端技能栏配置缓存：术式 → 9栏位 skillId 映射。
 * <p>
 * 在服务端同步术式时一并更新；{@link com.darkness.jujutsukaisenzone.event.TechniqueSkillKeyEvents}
 * 据此判断某槽位是否支持最大输出等。
 */
public final class ClientSkillBarCache
{
    private static final Map<String, String[]> CACHE = new HashMap<>();

    private ClientSkillBarCache()
    {
    }

    /** 服务端同步配置到达时更新缓存。 */
    public static void put(String techniqueId, String[] skillIds)
    {
        CACHE.put(techniqueId, Arrays.copyOf(skillIds, 9));
    }

    /** 是否已有指定术式的快捷栏缓存。 */
    public static boolean has(String techniqueId)
    {
        return techniqueId != null && !techniqueId.isBlank() && CACHE.containsKey(techniqueId);
    }

    /** 客户端断线/退出世界时清空，避免沿用旧会话内存缓存。 */
    public static void clear()
    {
        CACHE.clear();
    }

    /** 获取指定术式、指定槽位的技能 ID；未配置时返回空串。 */
    public static String getSkillId(String techniqueId, int slot)
    {
        if (techniqueId == null || techniqueId.isBlank() || slot < 1 || slot > 9)
        {
            return "";
        }
        String[] ids = CACHE.get(techniqueId);
        if (ids == null) return "";
        return ids[slot - 1];
    }

    /** 获取指定术式的全部配置；无缓存时返回空数组。 */
    public static String[] getAll(String techniqueId)
    {
        String[] ids = CACHE.get(techniqueId);
        return ids != null ? Arrays.copyOf(ids, 9) : emptyConfig();
    }

    private static String[] emptyConfig()
    {
        String[] empty = new String[9];
        Arrays.fill(empty, "");
        return empty;
    }
}
