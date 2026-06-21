package com.darkness.jujutsukaisenzone.gameplay;

import com.darkness.jujutsukaisenzone.entity.WedgeEntity;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import com.darkness.jujutsukaisenzone.registry.ModBlocks;
import com.darkness.jujutsukaisenzone.technique.barrier.AbstractBarrierTechnique;
import com.darkness.jujutsukaisenzone.technique.barrier.ExpansionAnimationType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 天元之楔玩法结界：半径 128、帐方块、结界术式自下而上构建动画。
 */
public final class TengenWedgeBarrierLogic
{
    public static final int BARRIER_RADIUS = 128;
    public static final int MIN_WEDGES_REQUIRED = 3;
    private static final double TELEPORT_OFFSET = 2.5D;
    private static final BarrierEngine ENGINE = new BarrierEngine();

    private TengenWedgeBarrierLogic()
    {
    }

    public static boolean setup(ServerLevel level, BlockPos center, ServerPlayer owner)
    {
        return ENGINE.beginSetup(level, center, owner);
    }

    public static void tickLevel(ServerLevel level)
    {
        ENGINE.tickLevel(level);
    }

    public static boolean isPositionInside(ServerLevel level, BlockPos pos)
    {
        return ENGINE.isPositionInside(level, pos);
    }

    /** setup 玩家处于其天元之楔结界内且结界已构建完成时返回对应记录。 */
    @Nullable
    public static TengenWedgeBarrier findOwnedBarrierInside(ServerPlayer player)
    {
        return ENGINE.findOwnedBarrierInside(player);
    }

    /** setup 玩家关闭当前维度内其拥有的全部天元之楔结界。 */
    public static boolean close(ServerPlayer owner)
    {
        return ENGINE.closeOwnedBarriers(owner);
    }

    public static TengenWedgeStartResult start(ServerPlayer owner)
    {
        return ENGINE.startBarrier(owner);
    }

    /** 玩家处于进行中的天元之楔结界内时返回对应记录。 */
    @Nullable
    public static TengenWedgeBarrier findParticipantBarrierInside(ServerPlayer player)
    {
        return ENGINE.findParticipantBarrierInside(player);
    }

    public static List<String> collectTeammateNames(ServerPlayer player, TengenWedgeBarrier barrier)
    {
        return ENGINE.collectTeammateNames(player, barrier);
    }

    @Nullable
    public static WedgeEntity findWedgeByFactionInBarrier(
            ServerLevel level,
            TengenWedgeBarrier barrier,
            String factionId)
    {
        if (!WedgeEntity.isPlayableFaction(factionId))
        {
            return null;
        }
        double radiusSquared = (double) barrier.getRadius() * barrier.getRadius();
        BlockPos center = barrier.getCenter();
        double cx = center.getX() + 0.5D;
        double cy = center.getY() + 0.5D;
        double cz = center.getZ() + 0.5D;
        int radius = barrier.getRadius();
        AABB bounds = new AABB(
                cx - radius, cy - radius, cz - radius,
                cx + radius, cy + radius, cz + radius);
        for (WedgeEntity wedge : level.getEntitiesOfClass(WedgeEntity.class, bounds, WedgeEntity::isAlive))
        {
            if (factionId.equals(wedge.getFactionId())
                    && isInsideSphere(wedge.position(), cx, cy, cz, radiusSquared))
            {
                return wedge;
            }
        }
        return null;
    }

    public static boolean hasAllFactionWedgesInBarrier(ServerLevel level, TengenWedgeBarrier barrier)
    {
        return findWedgeByFactionInBarrier(level, barrier, PlayerFactionData.FACTION_SORCERER) != null
                && findWedgeByFactionInBarrier(level, barrier, PlayerFactionData.FACTION_CURSE_USER) != null
                && findWedgeByFactionInBarrier(level, barrier, PlayerFactionData.FACTION_CURSE_SPIRIT) != null;
    }

    static Vec3 computeTeleportSpotNearWedge(TengenWedgeBarrier barrier, WedgeEntity wedge)
    {
        BlockPos center = barrier.getCenter();
        double dx = wedge.getX() - (center.getX() + 0.5D);
        double dz = wedge.getZ() - (center.getZ() + 0.5D);
        Vec3 outward;
        if (dx * dx + dz * dz < 0.01D)
        {
            outward = new Vec3(1.0D, 0.0D, 0.0D);
        }
        else
        {
            outward = new Vec3(dx, 0.0D, dz).normalize();
        }
        return wedge.position().add(outward.scale(TELEPORT_OFFSET));
    }

    public static void removeWedgesInside(ServerLevel level, TengenWedgeBarrier barrier)
    {
        double radiusSquared = (double) barrier.getRadius() * barrier.getRadius();
        BlockPos center = barrier.getCenter();
        double cx = center.getX() + 0.5D;
        double cy = center.getY() + 0.5D;
        double cz = center.getZ() + 0.5D;
        int radius = barrier.getRadius();
        AABB bounds = new AABB(
                cx - radius, cy - radius, cz - radius,
                cx + radius, cy + radius, cz + radius);
        for (WedgeEntity wedge : level.getEntitiesOfClass(WedgeEntity.class, bounds, WedgeEntity::isAlive))
        {
            if (isInsideSphere(wedge.position(), cx, cy, cz, radiusSquared))
            {
                wedge.discard();
            }
        }
    }

    public static int countWedgesInside(ServerLevel level, BlockPos center, int radius)
    {
        double radiusSquared = (double) radius * radius;
        double cx = center.getX() + 0.5D;
        double cy = center.getY() + 0.5D;
        double cz = center.getZ() + 0.5D;
        AABB bounds = new AABB(
                cx - radius, cy - radius, cz - radius,
                cx + radius, cy + radius, cz + radius);
        int count = 0;
        for (WedgeEntity wedge : level.getEntitiesOfClass(WedgeEntity.class, bounds, WedgeEntity::isAlive))
        {
            if (isInsideSphere(wedge.position(), cx, cy, cz, radiusSquared))
            {
                count++;
            }
        }
        return count;
    }

    static boolean isInsideSphere(Vec3 pos, double cx, double cy, double cz, double radiusSquared)
    {
        double dx = pos.x - cx;
        double dy = pos.y - cy;
        double dz = pos.z - cz;
        return dx * dx + dy * dy + dz * dz < radiusSquared;
    }

    public static final class TengenWedgeBarrier
    {
        private final UUID ownerUuid;
        private final ResourceKey<Level> dimension;
        private final BlockPos center;
        private final int radius;
        private final int totalShellBlocks;
        private int expansionProgress;
        private boolean expanding;
        private TengenWedgeBarrierState state = TengenWedgeBarrierState.PREPARING;
        private final Set<UUID> participants = new HashSet<>();

        private TengenWedgeBarrier(
                UUID ownerUuid,
                ResourceKey<Level> dimension,
                BlockPos center,
                int radius,
                int totalShellBlocks,
                int expansionProgress,
                boolean expanding)
        {
            this.ownerUuid = ownerUuid;
            this.dimension = dimension;
            this.center = center;
            this.radius = radius;
            this.totalShellBlocks = totalShellBlocks;
            this.expansionProgress = expansionProgress;
            this.expanding = expanding;
        }

        public UUID getOwnerUuid()
        {
            return ownerUuid;
        }

        public ResourceKey<Level> getDimension()
        {
            return dimension;
        }

        public BlockPos getCenter()
        {
            return center;
        }

        public int getRadius()
        {
            return radius;
        }

        public boolean isExpanding()
        {
            return expanding;
        }

        public int getCompletionPercent()
        {
            if (totalShellBlocks <= 0)
            {
                return 0;
            }
            return (int) Math.round(100.0D * expansionProgress / totalShellBlocks);
        }

        public TengenWedgeBarrierState getState()
        {
            return state;
        }

        void setState(TengenWedgeBarrierState state)
        {
            this.state = state;
        }

        public Set<UUID> getParticipants()
        {
            return Collections.unmodifiableSet(participants);
        }

        boolean addParticipant(UUID playerId)
        {
            return participants.add(playerId);
        }

        void clearParticipants()
        {
            participants.clear();
        }

        boolean isParticipant(UUID playerId)
        {
            return participants.contains(playerId);
        }

        void syncExpansionState(int progress, boolean expanding)
        {
            this.expansionProgress = progress;
            this.expanding = expanding;
        }
    }

    private static final class BarrierEngine extends AbstractBarrierTechnique
    {
        private final ConcurrentMap<ResourceKey<Level>, CopyOnWriteArrayList<ManagedBarrier>> activeBarriers =
                new ConcurrentHashMap<>();

        boolean beginSetup(ServerLevel level, BlockPos center, ServerPlayer owner)
        {
            List<BlockPos> shellBlocks = buildHollowSpherePositions(center, BARRIER_RADIUS);
            if (shellBlocks.isEmpty())
            {
                return false;
            }

            shellBlocks = sortShellBottomUp(shellBlocks, center);
            Map<BlockPos, BlockState> removedBlocks = new HashMap<>();

            BarrierInstance instance = new BarrierInstance(
                    level.dimension(),
                    shellBlocks,
                    removedBlocks,
                    center,
                    BARRIER_RADIUS,
                    true,
                    10,
                    0,
                    true,
                    0,
                    0
            );

            TengenWedgeBarrier record = new TengenWedgeBarrier(
                    owner.getUUID(),
                    level.dimension(),
                    center,
                    BARRIER_RADIUS,
                    shellBlocks.size(),
                    0,
                    true);
            activeBarriers.computeIfAbsent(level.dimension(), key -> new CopyOnWriteArrayList<>())
                    .add(new ManagedBarrier(record, instance));

            owner.displayClientMessage(
                    Component.literal("天元之楔结界构建中（半径 " + BARRIER_RADIUS + "）").withStyle(ChatFormatting.AQUA),
                    true);
            return true;
        }

        void tickLevel(ServerLevel level)
        {
            CopyOnWriteArrayList<ManagedBarrier> barriers = activeBarriers.get(level.dimension());
            if (barriers == null || barriers.isEmpty())
            {
                return;
            }

            for (ManagedBarrier managed : barriers)
            {
                if (managed.instance.isExpanding())
                {
                    tickExpand(level, managed);
                }
            }
        }

        @Nullable
        TengenWedgeBarrier findOwnedBarrierInside(ServerPlayer player)
        {
            CopyOnWriteArrayList<ManagedBarrier> barriers = activeBarriers.get(player.serverLevel().dimension());
            if (barriers == null || barriers.isEmpty())
            {
                return null;
            }

            Vec3 pos = player.position();
            for (ManagedBarrier managed : barriers)
            {
                TengenWedgeBarrier record = managed.record;
                if (!record.getOwnerUuid().equals(player.getUUID()) || record.isExpanding())
                {
                    continue;
                }
                if (record.getState() != TengenWedgeBarrierState.PREPARING)
                {
                    continue;
                }
                if (isInsideBarrier(pos, record))
                {
                    return record;
                }
            }
            return null;
        }

        TengenWedgeStartResult startBarrier(ServerPlayer owner)
        {
            TengenWedgeBarrier barrier = findOwnedBarrierInside(owner);
            if (barrier == null)
            {
                return TengenWedgeStartResult.NOT_INSIDE;
            }
            if (barrier.isExpanding())
            {
                return TengenWedgeStartResult.STILL_EXPANDING;
            }
            if (barrier.getState() == TengenWedgeBarrierState.IN_PROGRESS)
            {
                return TengenWedgeStartResult.ALREADY_STARTED;
            }

            ServerLevel level = owner.serverLevel();
            int wedgeCount = TengenWedgeBarrierLogic.countWedgesInside(
                    level, barrier.getCenter(), barrier.getRadius());
            if (wedgeCount < MIN_WEDGES_REQUIRED)
            {
                return TengenWedgeStartResult.NOT_ENOUGH_WEDGES;
            }
            if (!TengenWedgeBarrierLogic.hasAllFactionWedgesInBarrier(level, barrier))
            {
                return TengenWedgeStartResult.MISSING_FACTION_WEDGE;
            }
            int spawnCount = TengenWedgeCurseSpiritSpawns.countForBarrier(barrier);
            if (spawnCount < TengenWedgeCurseSpiritSpawns.MIN_REQUIRED)
            {
                return TengenWedgeStartResult.NOT_ENOUGH_SPAWNS;
            }

            barrier.setState(TengenWedgeBarrierState.IN_PROGRESS);
            int teleported = 0;
            for (ServerPlayer player : level.players())
            {
                if (!isInsideBarrier(player.position(), barrier))
                {
                    continue;
                }
                String factionId = PlayerFactionData.getFactionId(player);
                if (!WedgeEntity.isPlayableFaction(factionId))
                {
                    continue;
                }
                WedgeEntity wedge = TengenWedgeBarrierLogic.findWedgeByFactionInBarrier(level, barrier, factionId);
                if (wedge == null)
                {
                    continue;
                }

                Vec3 spot = TengenWedgeBarrierLogic.computeTeleportSpotNearWedge(barrier, wedge);
                player.teleportTo(level, spot.x, spot.y, spot.z, Collections.emptySet(), player.getYRot(), player.getXRot());
                BlockPos respawnPos = BlockPos.containing(spot);
                player.setRespawnPosition(level.dimension(), respawnPos, player.getYRot(), true, true);
                barrier.addParticipant(player.getUUID());
                TengenWedgeScoreboard.hide(player);
                TengenWedgeScoreboard.updateTeammates(
                        player,
                        collectTeammateNames(player, barrier));
                TengenWedgeIntroMessage.sendToParticipant(player, factionId);
                teleported++;
            }

            owner.displayClientMessage(
                    Component.literal("天元之楔已开始（状态：进行中，传送 "
                            + teleported + " 名阵营玩家）").withStyle(ChatFormatting.GOLD),
                    true);
            return TengenWedgeStartResult.SUCCESS;
        }

        @Nullable
        TengenWedgeBarrier findParticipantBarrierInside(ServerPlayer player)
        {
            CopyOnWriteArrayList<ManagedBarrier> barriers = activeBarriers.get(player.serverLevel().dimension());
            if (barriers == null || barriers.isEmpty())
            {
                return null;
            }

            Vec3 pos = player.position();
            for (ManagedBarrier managed : barriers)
            {
                TengenWedgeBarrier record = managed.record;
                if (record.getState() != TengenWedgeBarrierState.IN_PROGRESS
                        || record.isExpanding()
                        || !record.isParticipant(player.getUUID()))
                {
                    continue;
                }
                if (isInsideBarrier(pos, record))
                {
                    return record;
                }
            }
            return null;
        }

        List<String> collectTeammateNames(ServerPlayer player, TengenWedgeBarrier barrier)
        {
            String factionId = PlayerFactionData.getFactionId(player);
            if (!WedgeEntity.isPlayableFaction(factionId))
            {
                return List.of();
            }

            List<String> names = new ArrayList<>();
            for (UUID participantId : barrier.getParticipants())
            {
                if (participantId.equals(player.getUUID()))
                {
                    continue;
                }
                ServerPlayer teammate = player.server.getPlayerList().getPlayer(participantId);
                if (teammate == null || !factionId.equals(PlayerFactionData.getFactionId(teammate)))
                {
                    continue;
                }
                names.add(teammate.getScoreboardName());
            }
            names.sort(String::compareToIgnoreCase);
            return names;
        }

        boolean closeOwnedBarriers(ServerPlayer owner)
        {
            ServerLevel level = owner.serverLevel();
            CopyOnWriteArrayList<ManagedBarrier> barriers = activeBarriers.get(level.dimension());
            if (barriers == null || barriers.isEmpty())
            {
                return false;
            }

            List<ManagedBarrier> owned = new ArrayList<>();
            for (ManagedBarrier managed : barriers)
            {
                if (managed.record.getOwnerUuid().equals(owner.getUUID()))
                {
                    owned.add(managed);
                }
            }
            if (owned.isEmpty())
            {
                return false;
            }

            for (ManagedBarrier managed : owned)
            {
                for (UUID participantId : managed.record.getParticipants())
                {
                    ServerPlayer participant = level.getServer().getPlayerList().getPlayer(participantId);
                    if (participant != null)
                    {
                        TengenWedgeScoreboard.hide(participant);
                    }
                }
                teardownBarrier(level, owner, managed);
            }
            barriers.removeAll(owned);
            if (barriers.isEmpty())
            {
                activeBarriers.remove(level.dimension());
            }

            TengenWedgeScoreboard.hide(owner);
            owner.displayClientMessage(
                    Component.literal("天元之楔结界已关闭").withStyle(ChatFormatting.YELLOW),
                    true);
            return true;
        }

        private void teardownBarrier(ServerLevel level, ServerPlayer owner, ManagedBarrier managed)
        {
            TengenWedgeBarrier record = managed.record;
            record.clearParticipants();
            TengenWedgeBarrierLogic.removeWedgesInside(level, record);
            TengenWedgeCurseSpiritSpawns.removeForBarrier(record);
            clearBarrierInstance(owner, managed.instance);
        }

        boolean isPositionInside(ServerLevel level, BlockPos pos)
        {
            CopyOnWriteArrayList<ManagedBarrier> barriers = activeBarriers.get(level.dimension());
            if (barriers == null || barriers.isEmpty())
            {
                return false;
            }

            Vec3 check = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            for (ManagedBarrier managed : barriers)
            {
                if (managed.record.isExpanding())
                {
                    continue;
                }
                if (isInsideBarrier(check, managed.record))
                {
                    return true;
                }
            }
            return false;
        }

        private static boolean isInsideBarrier(Vec3 pos, TengenWedgeBarrier record)
        {
            double cx = record.getCenter().getX() + 0.5D;
            double cy = record.getCenter().getY() + 0.5D;
            double cz = record.getCenter().getZ() + 0.5D;
            double radiusSquared = (double) record.getRadius() * record.getRadius();
            return TengenWedgeBarrierLogic.isInsideSphere(pos, cx, cy, cz, radiusSquared);
        }

        private void tickExpand(ServerLevel level, ManagedBarrier managed)
        {
            BarrierInstance instance = managed.instance;
            List<BlockPos> allBlocks = instance.getShellBlocks();
            int currentProgress = instance.getExpansionProgress();

            if (currentProgress >= allBlocks.size())
            {
                finishBarrier(level, managed);
                return;
            }

            int currentTopY = currentProgress > 0 ? allBlocks.get(currentProgress - 1).getY() : allBlocks.get(0).getY() - 1;
            int targetY = currentTopY + 1;
            BlockState shroud = getBarrierBlockState();

            int placedThisTick = 0;
            int newProgress = currentProgress;

            while (newProgress < allBlocks.size() && placedThisTick < BLOCKS_PER_BATCH)
            {
                BlockPos pos = allBlocks.get(newProgress);
                if (pos.getY() > targetY)
                {
                    break;
                }

                if (!level.getBlockState(pos).isAir())
                {
                    rememberRemovedBlockForPos(level, pos, instance.getRemovedBlocks());
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                }
                level.setBlock(pos, shroud, BLOCK_UPDATE_FLAGS);

                placedThisTick++;
                newProgress++;
            }

            instance.setExpansionProgress(newProgress);
            managed.syncFromInstance();

            if (newProgress >= allBlocks.size())
            {
                finishBarrier(level, managed);
            }
        }

        private void finishBarrier(ServerLevel level, ManagedBarrier managed)
        {
            managed.instance.setExpanding(false);
            managed.instance.setExpansionProgress(managed.instance.getShellBlocks().size());
            managed.syncFromInstance();

            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(managed.record.getOwnerUuid());
            if (owner != null)
            {
                owner.displayClientMessage(
                        Component.literal("天元之楔结界构建完成").withStyle(ChatFormatting.GREEN),
                        true);
            }
        }

        private static final class ManagedBarrier
        {
            final TengenWedgeBarrier record;
            final BarrierInstance instance;

            ManagedBarrier(TengenWedgeBarrier record, BarrierInstance instance)
            {
                this.record = record;
                this.instance = instance;
            }

            void syncFromInstance()
            {
                record.syncExpansionState(instance.getExpansionProgress(), instance.isExpanding());
            }
        }

        private List<BlockPos> sortShellBottomUp(List<BlockPos> shellBlocks, BlockPos center)
        {
            List<BlockPos> sorted = new ArrayList<>(shellBlocks);
            sorted.sort((a, b) ->
            {
                int yCompare = Integer.compare(a.getY(), b.getY());
                if (yCompare != 0)
                {
                    return yCompare;
                }
                int distA = (a.getX() - center.getX()) * (a.getX() - center.getX())
                        + (a.getZ() - center.getZ()) * (a.getZ() - center.getZ());
                int distB = (b.getX() - center.getX()) * (b.getX() - center.getX())
                        + (b.getZ() - center.getZ()) * (b.getZ() - center.getZ());
                return Integer.compare(distA, distB);
            });
            return sorted;
        }

        @Override
        protected int[] getRadiusValues()
        {
            return new int[]{BARRIER_RADIUS};
        }

        @Override
        protected int getDefaultRadiusTier()
        {
            return 0;
        }

        @Override
        protected int getMaxActiveBarrierCount()
        {
            return Integer.MAX_VALUE;
        }

        @Override
        protected double getMaintainCostBasePerSecond()
        {
            return 0.0D;
        }

        @Override
        protected double getMaintainCostPerRadiusPerSecond()
        {
            return 0.0D;
        }

        @Override
        protected ExpansionAnimationType getExpansionAnimationType()
        {
            return ExpansionAnimationType.BOTTOM_UP;
        }

        @Override
        public boolean canCast(ServerPlayer player)
        {
            return false;
        }

        @Override
        protected int calculateExpansionCost(int tier)
        {
            return 0;
        }

        @Override
        protected BlockState getBarrierBlockState()
        {
            return ModBlocks.SHROUD_BLOCK.get().defaultBlockState();
        }

        @Override
        protected double getMaintainCostReduction(ServerPlayer player)
        {
            return 0.0D;
        }

        @Override
        public String getTechniqueType()
        {
            return "tengen_wedge";
        }

        @Override
        public void cast(ServerPlayer player)
        {
        }

        @Override
        public void tickMaintain(ServerPlayer player)
        {
        }

        @Override
        public boolean isPositionInBarrier(ServerLevel level, BlockPos pos)
        {
            return isPositionInside(level, pos);
        }

        @Override
        public boolean isPositionBlockingMobSpawn(ServerLevel level, BlockPos pos)
        {
            return isPositionInBarrier(level, pos);
        }

        @Override
        public void updateBarrierRules(ServerPlayer player)
        {
        }

        @Override
        public void repairBarrierGaps(ServerPlayer player)
        {
        }

        @Override
        public int adjustBarrierRadiusTier(ServerPlayer player, int delta)
        {
            return 0;
        }
    }
}
