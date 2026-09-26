package com.darkness.jujutsukaisenzone.technique.skill.tenshadows;

import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** 十影式神属性表运行时缓存。 */
public final class TenShadowsShikigamiStatsRegistry
{
    private static volatile Map<TenShadowsShikigami, TenShadowsShikigamiStatRow> UNTAMED = Map.of();
    private static volatile Map<TenShadowsShikigami, TenShadowsShikigamiStatRow> TAMED_BASE = Map.of();
    private static volatile TenShadowsOwnerScaling OWNER_SCALING = TenShadowsOwnerScaling.DEFAULT;

    private TenShadowsShikigamiStatsRegistry()
    {
    }

    public static void replace(
            Map<TenShadowsShikigami, TenShadowsShikigamiStatRow> untamed,
            Map<TenShadowsShikigami, TenShadowsShikigamiStatRow> tamedBase,
            TenShadowsOwnerScaling ownerScaling)
    {
        UNTAMED = Collections.unmodifiableMap(new EnumMap<>(untamed));
        TAMED_BASE = Collections.unmodifiableMap(new EnumMap<>(tamedBase));
        OWNER_SCALING = ownerScaling == null ? TenShadowsOwnerScaling.DEFAULT : ownerScaling;
    }

    @Nullable
    public static TenShadowsShikigamiStatRow untamed(TenShadowsShikigami shikigami)
    {
        return shikigami == null ? null : UNTAMED.get(shikigami);
    }

    @Nullable
    public static TenShadowsShikigamiStatRow tamedBase(TenShadowsShikigami shikigami)
    {
        return shikigami == null ? null : TAMED_BASE.get(shikigami);
    }

    public static TenShadowsOwnerScaling ownerScaling()
    {
        return OWNER_SCALING;
    }
}
