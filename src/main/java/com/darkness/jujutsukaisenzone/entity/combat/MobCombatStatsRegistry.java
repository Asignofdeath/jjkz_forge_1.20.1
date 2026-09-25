package com.darkness.jujutsukaisenzone.entity.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生物战斗属性表（数据包 {@code data/<namespace>/mob_combat_stats/*.json}）的运行时缓存。
 * 键为实体类型 id（如 {@code minecraft:zombie}）。
 */
public final class MobCombatStatsRegistry
{
    private static volatile Map<ResourceLocation, MobCombatStats> TABLE = Map.of();

    private MobCombatStatsRegistry()
    {
    }

    public static void replaceAll(Map<ResourceLocation, MobCombatStats> next)
    {
        TABLE = Collections.unmodifiableMap(new ConcurrentHashMap<>(next));
    }

    public static Map<ResourceLocation, MobCombatStats> snapshot()
    {
        return TABLE;
    }

    @Nullable
    public static MobCombatStats get(ResourceLocation entityTypeId)
    {
        return TABLE.get(entityTypeId);
    }

    @Nullable
    public static MobCombatStats get(EntityType<?> type)
    {
        ResourceLocation id = EntityType.getKey(type);
        return id == null ? null : TABLE.get(id);
    }

    public static int size()
    {
        return TABLE.size();
    }
}
