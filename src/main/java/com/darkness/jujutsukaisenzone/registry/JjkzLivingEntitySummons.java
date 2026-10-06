package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.entity.GojoSatoruEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@code /jjkz living_entity <id>} 可召唤的模组生物。id 与外观名对齐，不等于 Forge 实体注册名。
 */
public final class JjkzLivingEntitySummons
{
    public static final String GOJOSATORU_TEACHER = "gojosatoru_teacher";

    @FunctionalInterface
    public interface Spawner
    {
        @Nullable
        Entity spawn(ServerLevel level, Vec3 pos, float yaw);
    }

    private static final Map<String, Spawner> SPAWNERS = new LinkedHashMap<>();

    static
    {
        register(GOJOSATORU_TEACHER, GojoSatoruEntity::spawnAt);
    }

    private JjkzLivingEntitySummons()
    {
    }

    public static Set<String> ids()
    {
        return Collections.unmodifiableSet(SPAWNERS.keySet());
    }

    @Nullable
    public static Entity spawn(String id, ServerLevel level, Vec3 pos, float yaw)
    {
        Spawner spawner = SPAWNERS.get(id);
        return spawner == null ? null : spawner.spawn(level, pos, yaw);
    }

    private static void register(String id, Spawner spawner)
    {
        SPAWNERS.put(id, spawner);
    }
}
