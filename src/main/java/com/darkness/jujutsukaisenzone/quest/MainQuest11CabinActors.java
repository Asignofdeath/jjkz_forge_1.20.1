package com.darkness.jujutsukaisenzone.quest;

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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 主线 1.1 小屋 Geo 演员：第一组巡镜开始时生成。
 * 暂不主动回收；重跑开场时才清掉上一批。
 */
public final class MainQuest11CabinActors
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, List<UUID>> ACTORS_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, UUID>> ACTOR_BY_APPEARANCE = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> XIAOSHIZI_REST_YAW = new ConcurrentHashMap<>();
    /** 相对床面再下沉半格。 */
    private static final double SIT_Y_OFFSET = -0.5D;
    /** 相对床 FACING 再右转 90°。 */
    private static final float YAW_RIGHT_TURN = 90.0F;

    private MainQuest11CabinActors()
    {
    }

    /**
     * 在 {@code cam start} 之前调用：小石子坐床、KiRo 在工作台前做活。
     */
    public static void spawnForCamStart(
            ServerPlayer player, @Nullable BoundingBox cabinBounds, @Nullable BlockPos cabinInterior)
    {
        despawnFor(player);
        spawnXiaoshiziOnBed(player, cabinBounds, cabinInterior);
        spawnKiroAtWorkbench(player, cabinBounds, cabinInterior);
    }

    private static void spawnXiaoshiziOnBed(
            ServerPlayer player, @Nullable BoundingBox cabinBounds, @Nullable BlockPos cabinInterior)
    {
        ServerLevel level = player.serverLevel();
        SurvivalCabinPlacer.BedSeat seat = resolveBedSeat(level, cabinBounds, cabinInterior);
        if (seat == null)
        {
            LOGGER.warn("[Cutscene] Main 1.1: no bed foot found near cabin; skip xiaoshizi spawn.");
            return;
        }
        double x = seat.sitPos().x;
        double y = seat.sitPos().y + SIT_Y_OFFSET;
        double z = seat.sitPos().z;
        float yaw = seat.yaw() + YAW_RIGHT_TURN;
        spawnActor(
                player,
                CutsceneActorEntity.APPEARANCE_XIAOSHIZI,
                CutsceneActorEntity.CLIP_XIAOSHIZI_SITTING_ON_THE_BED,
                x, y, z, yaw);
    }

    private static void spawnKiroAtWorkbench(
            ServerPlayer player, @Nullable BoundingBox cabinBounds, @Nullable BlockPos cabinInterior)
    {
        ServerLevel level = player.serverLevel();
        SurvivalCabinPlacer.WorkbenchStand stand = resolveWorkbenchStand(level, cabinBounds, cabinInterior);
        if (stand == null)
        {
            LOGGER.warn("[Cutscene] Main 1.1: no workbench stand found near cabin; skip kiro spawn.");
            return;
        }
        spawnActor(
                player,
                CutsceneActorEntity.APPEARANCE_KIRO,
                CutsceneActorEntity.CLIP_KIRO_CRAFT_AT_THE_WORKBENCH,
                stand.standPos().x, stand.standPos().y, stand.standPos().z, stand.yaw());
    }

    private static void spawnActor(
            ServerPlayer player, String appearance, String clip, double x, double y, double z, float yaw)
    {
        ServerLevel level = player.serverLevel();
        CutsceneActorEntity actor = ModEntities.CUTSCENE_ACTOR.get().create(level);
        if (actor == null)
        {
            LOGGER.warn("[Cutscene] Main 1.1: failed to create cutscene actor {}.", appearance);
            return;
        }
        actor.prepare(appearance, clip);
        actor.moveTo(x, y, z, yaw, 0.0F);
        actor.yRotO = yaw;
        level.addFreshEntity(actor);
        UUID playerId = player.getUUID();
        ACTORS_BY_PLAYER.computeIfAbsent(playerId, id -> new ArrayList<>()).add(actor.getUUID());
        ACTOR_BY_APPEARANCE.computeIfAbsent(playerId, id -> new HashMap<>()).put(appearance, actor.getUUID());
        if (CutsceneActorEntity.APPEARANCE_XIAOSHIZI.equals(appearance))
        {
            XIAOSHIZI_REST_YAW.put(playerId, yaw);
        }
        LOGGER.info("[Cutscene] Main 1.1: spawned {} at {},{},{} yaw={}", appearance, x, y, z, yaw);
    }

    @Nullable
    public static CutsceneActorEntity actor(ServerPlayer player, String appearance)
    {
        Map<String, UUID> byAppearance = ACTOR_BY_APPEARANCE.get(player.getUUID());
        if (byAppearance == null)
        {
            return null;
        }
        UUID actorId = byAppearance.get(appearance);
        if (actorId == null)
        {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(actorId);
        return entity instanceof CutsceneActorEntity actor ? actor : null;
    }

    public static float xiaoshiziRestYaw(ServerPlayer player)
    {
        return XIAOSHIZI_REST_YAW.getOrDefault(player.getUUID(), 0.0F);
    }

    /** 镜头看向小石子上身（与生成坐标同一套偏移）。 */
    @Nullable
    public static Vec3 xiaoshiziLookPos(
            ServerLevel level, @Nullable BoundingBox cabinBounds, @Nullable BlockPos cabinInterior)
    {
        SurvivalCabinPlacer.BedSeat seat = resolveBedSeat(level, cabinBounds, cabinInterior);
        if (seat == null)
        {
            return null;
        }
        return new Vec3(seat.sitPos().x, seat.sitPos().y + SIT_Y_OFFSET + 1.0D, seat.sitPos().z);
    }

    /** 镜头看向 KiRo 上身。 */
    @Nullable
    public static Vec3 kiroLookPos(
            ServerLevel level, @Nullable BoundingBox cabinBounds, @Nullable BlockPos cabinInterior)
    {
        SurvivalCabinPlacer.WorkbenchStand stand = resolveWorkbenchStand(level, cabinBounds, cabinInterior);
        if (stand == null)
        {
            return null;
        }
        return stand.standPos().add(0.0D, 1.4D, 0.0D);
    }

    @Nullable
    private static SurvivalCabinPlacer.BedSeat resolveBedSeat(
            ServerLevel level, @Nullable BoundingBox cabinBounds, @Nullable BlockPos cabinInterior)
    {
        SurvivalCabinPlacer.BedSeat seat = null;
        if (cabinBounds != null)
        {
            seat = SurvivalCabinPlacer.findBedSeat(level, cabinBounds);
        }
        if (seat == null && cabinInterior != null)
        {
            seat = SurvivalCabinPlacer.findBedSeatNear(level, cabinInterior);
        }
        return seat;
    }

    @Nullable
    private static SurvivalCabinPlacer.WorkbenchStand resolveWorkbenchStand(
            ServerLevel level, @Nullable BoundingBox cabinBounds, @Nullable BlockPos cabinInterior)
    {
        SurvivalCabinPlacer.WorkbenchStand stand = null;
        if (cabinBounds != null)
        {
            stand = SurvivalCabinPlacer.findWorkbenchStand(level, cabinBounds, cabinInterior);
        }
        if (stand == null && cabinInterior != null)
        {
            stand = SurvivalCabinPlacer.findWorkbenchStandNear(level, cabinInterior);
        }
        return stand;
    }

    public static void despawnFor(ServerPlayer player)
    {
        List<UUID> actorIds = ACTORS_BY_PLAYER.remove(player.getUUID());
        ACTOR_BY_APPEARANCE.remove(player.getUUID());
        XIAOSHIZI_REST_YAW.remove(player.getUUID());
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
