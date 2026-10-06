package com.darkness.jujutsukaisenzone.quest.mainline.main_1_3;

import com.darkness.jujutsukaisenzone.entity.CutsceneActorEntity;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

/**
 * 主线 1.3 演出 Geo 演员：五条悟（戴/摘眼罩）。
 * <p>
 * 与 {@link com.darkness.jujutsukaisenzone.quest.mainline.main_1_1.MainQuest11CabinActors} 同构：
 * 每玩家一组，演完 / 重跑 / 退出时统一回收。
 * <p>
 * ⛔ <b>玩家替身已废弃</b>（2026-10-05）：{@link #spawnPlayerDouble} 与
 * {@link #ROLE_PLAYER_DOUBLE} / {@link #APPEARANCE_PLAYER_DOUBLE} 目前<b>无任何调用方</b>。
 * 整套替身系统不再启用，对应资源（{@code player_mimic.geo/animation.json}）不再被加载。
 */
public final class MainQuest13Actors
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** ⛔ 已废弃（无调用方）：玩家替身角色名。 */
    public static final String ROLE_PLAYER_DOUBLE = "player_double";
    public static final String ROLE_GOJO = "gojo";
    /** ⛔ 已废弃（无调用方）：玩家替身外观 id。 */
    public static final String APPEARANCE_PLAYER_DOUBLE = "player_mimic";

    private static final Map<UUID, List<UUID>> ACTORS_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, UUID>> ACTOR_BY_ROLE = new ConcurrentHashMap<>();

    private MainQuest13Actors()
    {
    }

    /**
     * ⛔ 已废弃（2026-10-05，无调用方）：生成玩家替身（倒地仰躺，一次性倒地后定格）。
     * 保留仅为回滚方便；替身系统已不再启用。
     *
     * @param skinTexture 完整资源路径；null = 用默认同名贴图
     */
    @Nullable
    public static CutsceneActorEntity spawnPlayerDouble(
            ServerPlayer player,
            double x, double y, double z,
            float yaw,
            @Nullable String skinTexture)
    {
        return spawnActor(
                player,
                ROLE_PLAYER_DOUBLE,
                APPEARANCE_PLAYER_DOUBLE,
                CutsceneActorEntity.CLIP_HUMANOID_FALL_DOWN_AND_BACK,
                x, y, z, yaw, skinTexture,
                false);
    }

    /** 生成五条悟演员（戴眼罩），播保护/站立姿势（一次性，定格在末帧保护姿势）。 */
    @Nullable
    public static CutsceneActorEntity spawnGojo(
            ServerPlayer player, double x, double y, double z, float yaw)
    {
        return spawnActor(
                player,
                ROLE_GOJO,
                CutsceneActorEntity.APPEARANCE_GOJO,
                CutsceneActorEntity.CLIP_GOJO_STAND_UP_AND_PROTECT,
                x, y, z, yaw, null,
                false);
    }

    @Nullable
    private static CutsceneActorEntity spawnActor(
            ServerPlayer player,
            String role,
            String appearance,
            String clip,
            double x, double y, double z,
            float yaw,
            @Nullable String skinTexture)
    {
        return spawnActor(player, role, appearance, clip, x, y, z, yaw, skinTexture, true);
    }

    @Nullable
    private static CutsceneActorEntity spawnActor(
            ServerPlayer player,
            String role,
            String appearance,
            String clip,
            double x, double y, double z,
            float yaw,
            @Nullable String skinTexture,
            boolean loop)
    {
        ServerLevel level = player.serverLevel();
        CutsceneActorEntity actor = ModEntities.CUTSCENE_ACTOR.get().create(level);
        if (actor == null)
        {
            LOGGER.warn("[Cutscene] Main 1.3: failed to create actor {}.", role);
            return null;
        }
        actor.prepare(appearance, clip);
        if (!loop)
        {
            // 一次性动画（如 stand_up_and_protect）：播完后定格末帧，不循环重放。
            actor.setClip(clip, false);
        }
        if (skinTexture != null && !skinTexture.isEmpty())
        {
            actor.setTextureOverride(skinTexture);
        }
        actor.moveTo(x, y, z, yaw, 0.0F);
        actor.snapLookYaw(yaw);
        level.addFreshEntity(actor);
        UUID playerId = player.getUUID();
        ACTORS_BY_PLAYER.computeIfAbsent(playerId, id -> new ArrayList<>()).add(actor.getUUID());
        ACTOR_BY_ROLE.computeIfAbsent(playerId, id -> new HashMap<>()).put(role, actor.getUUID());
        LOGGER.info("[Cutscene] Main 1.3: spawned {} at {},{},{} yaw={}", role, x, y, z, yaw);
        return actor;
    }

    @Nullable
    public static CutsceneActorEntity actor(ServerPlayer player, String role)
    {
        Map<String, UUID> byRole = ACTOR_BY_ROLE.get(player.getUUID());
        if (byRole == null)
        {
            return null;
        }
        UUID actorId = byRole.get(role);
        if (actorId == null)
        {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(actorId);
        return entity instanceof CutsceneActorEntity actor ? actor : null;
    }

    /** 回收该玩家本演出生成的全部演员。 */
    public static void despawnFor(ServerPlayer player)
    {
        List<UUID> actorIds = ACTORS_BY_PLAYER.remove(player.getUUID());
        ACTOR_BY_ROLE.remove(player.getUUID());
        if (actorIds == null)
        {
            return;
        }
        ServerLevel level = player.serverLevel();
        for (UUID actorId : actorIds)
        {
            Entity entity = level.getEntity(actorId);
            if (entity != null)
            {
                entity.discard();
            }
        }
    }
}
