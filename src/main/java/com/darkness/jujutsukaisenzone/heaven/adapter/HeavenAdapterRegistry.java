package com.darkness.jujutsukaisenzone.heaven.adapter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 天道适配器目录。bootstrap 时登记；业务不经本表调效果。
 */
public final class HeavenAdapterRegistry
{
    private static final Map<String, HeavenAdapter> BY_ID = new LinkedHashMap<>();

    private HeavenAdapterRegistry()
    {
    }

    public static void bootstrap()
    {
        BY_ID.clear();
    }

    public static void register(HeavenAdapter adapter)
    {
        if (adapter == null || adapter.id() == null || adapter.id().isBlank())
        {
            throw new IllegalArgumentException("HeavenAdapter 缺少 id");
        }
        if (BY_ID.put(adapter.id(), adapter) != null)
        {
            throw new IllegalStateException("重复注册天道适配器：" + adapter.id());
        }
    }

    public static Optional<HeavenAdapter> find(String id)
    {
        if (id == null || id.isBlank())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Collection<HeavenAdapter> all()
    {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
