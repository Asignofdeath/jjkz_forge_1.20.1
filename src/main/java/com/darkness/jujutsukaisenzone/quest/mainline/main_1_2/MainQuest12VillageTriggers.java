package com.darkness.jujutsukaisenzone.quest.mainline.main_1_2;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestCodCaptionsS2CPacket;
import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDefs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/**
 * 主线 1.2 探索期：叙事村内常见建筑（民宅床、箱桶、钟楼/讲台、村中心）靠近或交互时播主角自述。
 */
public final class MainQuest12VillageTriggers
{
    private static final double PROXIMITY_H = 4.5D;
    private static final double PROXIMITY_CENTER = 6.0D;
    private static final int LINE_HOLD_TICKS = 70;
    /** 两句自述最短间隔，避免连刷盖住开场独白。 */
    private static final int COOLDOWN_TICKS = 100;
    /** 开场独白队列播完后再允许靠近触发（约 10s）。 */
    private static final int MIN_EXPLORE_TICKS = 200;
    private static final int CHECK_INTERVAL = 5;
    /** 选民宅锚点时，床须离村中心至少这么远（水平格）。 */
    private static final double MIN_HOUSE_DIST = 8.0D;

    private static final Map<UUID, TriggerState> STATES = new ConcurrentHashMap<>();

    private MainQuest12VillageTriggers()
    {
    }

    public static void begin(ServerPlayer player, BoundingBox box, Vec3 villageCenter)
    {
        if (!isActiveQuest(player))
        {
            return;
        }
        TriggerState state = new TriggerState();
        state.bounds = box;
        state.villageCenter = villageCenter;
        state.anchors = scanAnchors(player.serverLevel(), box, villageCenter);
        STATES.put(player.getUUID(), state);
    }

    public static void tick(ServerPlayer player, int exploreTicks)
    {
        TriggerState state = STATES.get(player.getUUID());
        if (state == null)
        {
            return;
        }
        state.exploreTicks = exploreTicks;
        if (!isActiveQuest(player) || exploreTicks < MIN_EXPLORE_TICKS)
        {
            return;
        }
        if (state.anchors == null)
        {
            return;
        }
        if (state.cooldownTicks > 0)
        {
            state.cooldownTicks--;
        }
        if (player.tickCount % CHECK_INTERVAL != 0 || state.cooldownTicks > 0)
        {
            return;
        }
        Anchors anchors = state.anchors;
        if (anchors.house != null && !state.isFired(Kind.HOUSE))
        {
            tryProximity(player, state, Kind.HOUSE, anchors.house, PROXIMITY_H);
        }
        if (anchors.landmark != null && anchors.landmarkKind != null && !state.isFired(anchors.landmarkKind))
        {
            tryProximity(player, state, anchors.landmarkKind, anchors.landmark, PROXIMITY_H);
        }
        if (anchors.center != null && !state.isFired(Kind.CENTER))
        {
            tryProximity(player, state, Kind.CENTER, anchors.center, PROXIMITY_CENTER);
        }
    }

    public static void onContainerOpen(ServerPlayer player, BlockPos pos)
    {
        if (!isActiveQuest(player) || !MainQuest12Anomaly.isExplorePhase(player))
        {
            return;
        }
        TriggerState state = STATES.get(player.getUUID());
        if (state == null || state.bounds == null || state.isFired(Kind.CONTAINER))
        {
            return;
        }
        if (state.exploreTicks < MIN_EXPLORE_TICKS)
        {
            return;
        }
        if (!state.bounds.isInside(pos))
        {
            return;
        }
        fire(player, state, Kind.CONTAINER);
    }

    public static void clear(ServerPlayer player)
    {
        STATES.remove(player.getUUID());
    }

    private static boolean isActiveQuest(ServerPlayer player)
    {
        return MainQuestDefs.MAIN_1_2_ID.equals(PlayerQuestData.getTrackedId(player))
                && !PlayerQuestData.isCompleted(player, MainQuestDefs.MAIN_1_2_ID);
    }

    private static void tryProximity(
            ServerPlayer player,
            TriggerState state,
            Kind kind,
            BlockPos anchor,
            double radius)
    {
        double dx = player.getX() - (anchor.getX() + 0.5D);
        double dy = player.getY() - (anchor.getY() + 0.5D);
        double dz = player.getZ() - (anchor.getZ() + 0.5D);
        if (dx * dx + dy * dy + dz * dz <= radius * radius)
        {
            fire(player, state, kind);
        }
    }

    private static void fire(ServerPlayer player, TriggerState state, Kind kind)
    {
        if (state.isFired(kind))
        {
            return;
        }
        state.setFired(kind);
        state.cooldownTicks = COOLDOWN_TICKS;
        NetworkSenders.toPlayer(
                player,
                SyncQuestCodCaptionsS2CPacket.playUniform(
                        new String[] {kind.captionKey},
                        0,
                        LINE_HOLD_TICKS));
    }

    private static Anchors scanAnchors(ServerLevel level, BoundingBox box, Vec3 villageCenter)
    {
        forceChunks(level, box);
        List<BlockPos> beds = new ArrayList<>();
        List<BlockPos> bells = new ArrayList<>();
        List<BlockPos> lecterns = new ArrayList<>();
        int minCx = box.minX() >> 4;
        int maxCx = box.maxX() >> 4;
        int minCz = box.minZ() >> 4;
        int maxCz = box.maxZ() >> 4;
        for (int cx = minCx; cx <= maxCx; cx++)
        {
            for (int cz = minCz; cz <= maxCz; cz++)
            {
                LevelChunk chunk = level.getChunk(cx, cz);
                collectBlocksInChunk(level, chunk, box, beds, bells, lecterns);
            }
        }
        Anchors anchors = new Anchors();
        anchors.house = pickHouseBed(beds, villageCenter);
        pickLandmark(anchors, bells, lecterns, villageCenter);
        anchors.center = centerSurface(level, villageCenter);
        return anchors;
    }

    private static void collectBlocksInChunk(
            ServerLevel level,
            LevelChunk chunk,
            BoundingBox box,
            List<BlockPos> beds,
            List<BlockPos> bells,
            List<BlockPos> lecterns)
    {
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        int minY = Math.max(box.minY(), level.getMinBuildHeight());
        int maxY = Math.min(box.maxY(), level.getMaxBuildHeight() - 1);
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                int wx = baseX + x;
                int wz = baseZ + z;
                if (wx < box.minX() || wx > box.maxX() || wz < box.minZ() || wz > box.maxZ())
                {
                    continue;
                }
                for (int y = minY; y <= maxY; y++)
                {
                    BlockState state = chunk.getBlockState(new BlockPos(wx, y, wz));
                    if (state.getBlock() instanceof BedBlock)
                    {
                        beds.add(new BlockPos(wx, y, wz));
                    }
                    else if (state.getBlock() instanceof BellBlock)
                    {
                        bells.add(new BlockPos(wx, y, wz));
                    }
                    else if (state.getBlock() instanceof LecternBlock)
                    {
                        lecterns.add(new BlockPos(wx, y, wz));
                    }
                }
            }
        }
    }

    @Nullable
    private static BlockPos pickHouseBed(List<BlockPos> beds, Vec3 villageCenter)
    {
        if (beds.isEmpty())
        {
            return null;
        }
        return beds.stream()
                .filter(pos -> horizontalDist(pos, villageCenter) >= MIN_HOUSE_DIST)
                .max(Comparator.comparingDouble(pos -> horizontalDist(pos, villageCenter)))
                .orElse(beds.get(0));
    }

    private static void pickLandmark(
            Anchors anchors,
            List<BlockPos> bells,
            List<BlockPos> lecterns,
            Vec3 villageCenter)
    {
        if (!bells.isEmpty())
        {
            anchors.landmark = bells.stream()
                    .min(Comparator.comparingDouble(pos -> horizontalDist(pos, villageCenter)))
                    .orElse(null);
            anchors.landmarkKind = Kind.BELL;
            return;
        }
        if (!lecterns.isEmpty())
        {
            anchors.landmark = lecterns.stream()
                    .min(Comparator.comparingDouble(pos -> horizontalDist(pos, villageCenter)))
                    .orElse(null);
            anchors.landmarkKind = Kind.LECTERN;
        }
    }

    private static BlockPos centerSurface(ServerLevel level, Vec3 villageCenter)
    {
        int x = (int) Math.floor(villageCenter.x);
        int z = (int) Math.floor(villageCenter.z);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private static double horizontalDist(BlockPos pos, Vec3 center)
    {
        double dx = pos.getX() + 0.5D - center.x;
        double dz = pos.getZ() + 0.5D - center.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void forceChunks(ServerLevel level, BoundingBox box)
    {
        int minCx = box.minX() >> 4;
        int maxCx = box.maxX() >> 4;
        int minCz = box.minZ() >> 4;
        int maxCz = box.maxZ() >> 4;
        for (int cx = minCx; cx <= maxCx; cx++)
        {
            for (int cz = minCz; cz <= maxCz; cz++)
            {
                level.getChunk(cx, cz, ChunkStatus.FULL, true);
            }
        }
    }

    private enum Kind
    {
        HOUSE("quest.jujutsukaisenzone.main_1_2.trigger.house"),
        CONTAINER("quest.jujutsukaisenzone.main_1_2.trigger.container"),
        BELL("quest.jujutsukaisenzone.main_1_2.trigger.bell"),
        LECTERN("quest.jujutsukaisenzone.main_1_2.trigger.lectern"),
        CENTER("quest.jujutsukaisenzone.main_1_2.trigger.center");

        private final String captionKey;

        Kind(String captionKey)
        {
            this.captionKey = captionKey;
        }
    }

    private static final class Anchors
    {
        @Nullable BlockPos house;
        @Nullable BlockPos landmark;
        @Nullable Kind landmarkKind;
        @Nullable BlockPos center;
    }

    private static final class TriggerState
    {
        @Nullable BoundingBox bounds;
        @Nullable Vec3 villageCenter;
        @Nullable Anchors anchors;
        int exploreTicks;
        int firedMask;
        int cooldownTicks;

        boolean isFired(Kind kind)
        {
            return (firedMask & (1 << kind.ordinal())) != 0;
        }

        void setFired(Kind kind)
        {
            firedMask |= 1 << kind.ordinal();
        }
    }
}
