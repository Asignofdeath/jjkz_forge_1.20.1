package com.darkness.jujutsukaisenzone.quest.mainline.main_1_2;

import com.darkness.jujutsukaisenzone.entity.Grade3CurseSpiritAEntity;
import com.darkness.jujutsukaisenzone.locate.PlayerLocateGuidance;
import com.darkness.jujutsukaisenzone.locate.StructureLocateTarget;
import com.darkness.jujutsukaisenzone.locate.StructureLocator;
import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestCodCaptionsS2CPacket;
import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDefs;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestLogic;
import com.darkness.jujutsukaisenzone.quest.mainline.NarrativeVillage;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 主线 1.2：进村探索 → 探索超时伏击 → 残血锁血或跑够距离结束。
 * 探索中远离村庄中心过远则放弃任务（非包围盒判定，中心距离 + 宽限）。
 * 结束时不清理本波咒灵（留给 1.3）。
 */
public final class MainQuest12Anomaly
{
    /** 探索时长：120 秒。 */
    public static final int EXPLORE_TICKS = 2400;
    public static final int MAX_SPAWN = 30;
    /** {@code fall_down.ogg} 约 1.35s，之后接 squeeze_and_pop。 */
    private static final int FALL_DOWN_TICKS = 27;
    public static final double SPAWN_MIN_DIST = 12.0D;
    public static final double SPAWN_MAX_DIST = 18.0D;
    public static final double FLEE_DISTANCE = 40.0D;
    public static final float LOCK_HEALTH = 1.0F;
    /**
     * 放弃距离宽限：相对村庄水平半对角线再加这么多格，
     * 超出「中心 → 玩家」水平距离则放弃（不用包围盒出界）。
     */
    public static final double ABANDON_GRACE_BLOCKS = 48.0D;
    public static final double ABANDON_MIN_DISTANCE = 80.0D;

    public static final String OBJECTIVE_EXPLORE = "quest.jujutsukaisenzone.main_1_2.objective.explore";
    public static final String OBJECTIVE_FLEE = "quest.jujutsukaisenzone.main_1_2.objective.flee";

    private static final String[] MONOLOGUE_KEYS = {
            "quest.jujutsukaisenzone.main_1_2.monologue.1",
            "quest.jujutsukaisenzone.main_1_2.monologue.2",
            "quest.jujutsukaisenzone.main_1_2.monologue.3",
            "quest.jujutsukaisenzone.main_1_2.monologue.4",
            "quest.jujutsukaisenzone.main_1_2.monologue.5"
    };
    /**
     * 伏击编排：玩家句与三级咒灵甲战吼交错；
     * 短间隔只用于「不同说话人」同屏，单人连句用长间隔顶替。
     */
    private static final String[] AMBUSH_KEYS = {
            "quest.jujutsukaisenzone.main_1_2.ambush.1",
            "quest.jujutsukaisenzone.main_1_2.ambush.roar.1",
            "quest.jujutsukaisenzone.main_1_2.ambush.2",
            "quest.jujutsukaisenzone.main_1_2.ambush.roar.2"
    };
    private static final int LINE_TICKS = 70;
    private static final int GAP_TICKS = 10;
    /**
     * 末句（monologue.5「地上还有没收干净的东西……」）相对前句出现的间隔：10 秒。
     * 其余句维持 {@code LINE_TICKS + GAP_TICKS} 的默认节奏。
     */
    private static final int TAIL_LINE_DELAY_TICKS = 200;
    /** 多人抢话间隔（小于客户端同时出现阈值 12）。 */
    private static final int MULTI_SPEAKER_GAP_TICKS = 8;
    private static final int SPAWN_ATTEMPTS = 96;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private MainQuest12Anomaly()
    {
    }

    public static boolean isRunning(ServerPlayer player)
    {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** 伏击残血锁血：会话内或存档标记（1.2 结束后仍生效，防场上咒灵秒杀）。 */
    public static boolean isHealthLocked(ServerPlayer player)
    {
        if (PlayerQuestData.isMain12HealthLocked(player))
        {
            return true;
        }
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.phase == Phase.AMBUSH && session.healthLocked;
    }

    @Nullable
    public static String objectiveKeyOverride(ServerPlayer player)
    {
        if (!MainQuestDefs.MAIN_1_2_ID.equals(PlayerQuestData.getTrackedId(player)))
        {
            return null;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null)
        {
            return PlayerQuestData.isMain12AmbushStarted(player) ? OBJECTIVE_FLEE : OBJECTIVE_EXPLORE;
        }
        return session.phase == Phase.AMBUSH ? OBJECTIVE_FLEE : OBJECTIVE_EXPLORE;
    }

    public static void startIfNeeded(ServerPlayer player)
    {
        if (!MainQuestDefs.MAIN_1_2_ID.equals(PlayerQuestData.getTrackedId(player)))
        {
            return;
        }
        if (PlayerQuestData.isCompleted(player, MainQuestDefs.MAIN_1_2_ID))
        {
            return;
        }
        if (SESSIONS.containsKey(player.getUUID()))
        {
            return;
        }
        Session session = new Session();
        if (PlayerQuestData.isMain12AmbushStarted(player))
        {
            session.phase = Phase.AMBUSH;
            BlockPos origin = PlayerQuestData.getMain12AmbushOrigin(player);
            session.ambushOrigin = origin != null
                    ? new Vec3(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D)
                    : player.position();
            session.villageBounds = resolveVillageBounds(player).orElse(null);
            if (PlayerQuestData.isMain12HealthLocked(player))
            {
                session.healthLocked = true;
                session.pendingComplete = true;
            }
        }
        else
        {
            session.phase = Phase.WAIT_ENTER;
            session.villageBounds = resolveVillageBounds(player).orElse(null);
            if (session.villageBounds != null && isInside(player, session.villageBounds))
            {
                beginExplore(player, session);
            }
        }
        SESSIONS.put(player.getUUID(), session);
        MainQuestLogic.syncTrackHud(player);
    }

    public static void abort(ServerPlayer player)
    {
        MainQuest12LoopTrap.clear(player);
        Session removed = SESSIONS.remove(player.getUUID());
        if (removed != null)
        {
            clearMonologue(player, removed, true);
        }
    }

    /** 任务标记完成后清理运行时会话（不丢场上咒灵；残血锁血标记保留给 1.3）。 */
    public static void onCompleted(ServerPlayer player)
    {
        MainQuest12LoopTrap.clear(player);
        Session removed = SESSIONS.remove(player.getUUID());
        if (removed != null)
        {
            clearMonologue(player, removed, true);
            if (removed.healthLocked)
            {
                PlayerQuestData.setMain12HealthLocked(player, true);
            }
        }
        PlayerQuestData.clearMain12Ambush(player);
        PlayerQuestData.setMain12DecorPlaced(player, false);
        playCompleteSounds(player);
    }

    private static void playCompleteSounds(ServerPlayer player)
    {
        player.playNotifySound(ModSounds.QUEST_FALL_DOWN.get(), SoundSource.MASTER, 1.0F, 1.0F);
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return;
        }
        UUID playerId = player.getUUID();
        server.tell(new TickTask(server.getTickCount() + FALL_DOWN_TICKS, () ->
        {
            ServerPlayer still = server.getPlayerList().getPlayer(playerId);
            if (still != null)
            {
                still.playNotifySound(ModSounds.QUEST_SQUEEZE_AND_POP.get(), SoundSource.MASTER, 1.0F, 1.0F);
            }
        }));
    }

    public static void tick(ServerPlayer player)
    {
        if (!MainQuestDefs.MAIN_1_2_ID.equals(PlayerQuestData.getTrackedId(player)))
        {
            return;
        }
        if (PlayerQuestData.isCompleted(player, MainQuestDefs.MAIN_1_2_ID))
        {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null)
        {
            startIfNeeded(player);
            session = SESSIONS.get(player.getUUID());
            if (session == null)
            {
                return;
            }
        }

        if (session.villageBounds == null && player.tickCount % 40 == 0)
        {
            session.villageBounds = resolveVillageBounds(player).orElse(null);
        }

        switch (session.phase)
        {
            case WAIT_ENTER -> tickWaitEnter(player, session);
            case EXPLORE -> tickExplore(player, session);
            case AMBUSH -> tickAmbush(player, session);
        }
    }

    /**
     * 锁血受击：伤害钳到不会低于 {@link #LOCK_HEALTH}。
     * 伏击中首次触及残血时落锁，并标记结束 1.2；锁血标记在任务完成后仍保留。
     *
     * @return 钳制后的伤害
     */
    public static float onLockedOrAmbushHurt(ServerPlayer player, float amount)
    {
        boolean locked = isHealthLocked(player);
        Session session = SESSIONS.get(player.getUUID());
        boolean ambush = session != null && session.phase == Phase.AMBUSH;
        if (!locked && !ambush)
        {
            return amount;
        }

        float health = player.getHealth();
        if (locked || health <= LOCK_HEALTH)
        {
            activateHealthLock(player, session);
            if (health <= LOCK_HEALTH)
            {
                return 0.0F;
            }
            return Math.min(amount, Math.max(0.0F, health - LOCK_HEALTH));
        }

        float projected = health - amount;
        if (projected <= LOCK_HEALTH)
        {
            activateHealthLock(player, session);
            return Math.max(0.0F, health - LOCK_HEALTH);
        }
        return amount;
    }

    /** 锁血期间阻止死亡。 */
    public static boolean preventDeath(ServerPlayer player)
    {
        if (!isHealthLocked(player))
        {
            return false;
        }
        if (player.getHealth() < LOCK_HEALTH)
        {
            player.setHealth(LOCK_HEALTH);
        }
        return true;
    }

    /** 每 tick 把血钳回锁血线。 */
    public static void tickHealthLock(ServerPlayer player)
    {
        if (!isHealthLocked(player))
        {
            return;
        }
        if (player.getHealth() < LOCK_HEALTH)
        {
            player.setHealth(LOCK_HEALTH);
        }
    }

    private static void activateHealthLock(ServerPlayer player, @Nullable Session session)
    {
        PlayerQuestData.setMain12HealthLocked(player, true);
        if (session != null && session.phase == Phase.AMBUSH)
        {
            session.healthLocked = true;
            session.pendingComplete = true;
        }
    }

    private static void tickWaitEnter(ServerPlayer player, Session session)
    {
        if (session.villageBounds == null)
        {
            return;
        }
        if (isInside(player, session.villageBounds))
        {
            beginExplore(player, session);
            MainQuestLogic.syncTrackHud(player);
        }
    }

    private static void tickExplore(ServerPlayer player, Session session)
    {
        session.exploreTicks++;
        if (session.villageCenter != null
                && horizontalDistance(player, session.villageCenter) > session.abandonDistance)
        {
            abandonExplore(player, session);
            return;
        }
        MainQuest12LoopTrap.tick(
                player,
                session.exploreTicks,
                session.villageCenter,
                session.abandonDistance);
        if (session.exploreTicks >= EXPLORE_TICKS)
        {
            beginAmbush(player, session);
        }
    }

    /** 探索中走太远：放弃 1.2（未完成，可再追踪），不触发伏击。 */
    private static void abandonExplore(ServerPlayer player, Session session)
    {
        MainQuest12LoopTrap.clear(player);
        clearMonologue(player, session, true);
        SESSIONS.remove(player.getUUID());
        MainQuestLogic.untrack(player, false);
        player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.quest.main_1_2.abandoned"));
    }

    private static void tickAmbush(ServerPlayer player, Session session)
    {
        tickHealthLock(player);
        if (session.pendingComplete)
        {
            activateHealthLock(player, session);
            MainQuestLogic.complete(player, MainQuestDefs.MAIN_1_2_ID, true);
            return;
        }
        if (player.getHealth() <= LOCK_HEALTH)
        {
            activateHealthLock(player, session);
            player.setHealth(LOCK_HEALTH);
            MainQuestLogic.complete(player, MainQuestDefs.MAIN_1_2_ID, true);
            return;
        }

        if (session.ambushOrigin != null)
        {
            double dx = player.getX() - session.ambushOrigin.x;
            double dz = player.getZ() - session.ambushOrigin.z;
            if (dx * dx + dz * dz >= FLEE_DISTANCE * FLEE_DISTANCE)
            {
                MainQuestLogic.complete(player, MainQuestDefs.MAIN_1_2_ID, true);
            }
        }
    }

    private static void beginExplore(ServerPlayer player, Session session)
    {
        session.phase = Phase.EXPLORE;
        session.exploreTicks = 0;
        bindVillageCenter(session);
        if (!PlayerQuestData.isMain12DecorPlaced(player) && session.villageBounds != null)
        {
            ServerLevel level = player.serverLevel();
            MainQuest12VillageDecor.place(level, session.villageBounds);
            MainQuest12VillageLoot.place(level, session.villageBounds);
            PlayerQuestData.setMain12DecorPlaced(player, true);
        }
        playMonologueQueue(player, session);
    }

    private static void bindVillageCenter(Session session)
    {
        if (session.villageBounds == null)
        {
            session.villageCenter = null;
            session.abandonDistance = ABANDON_MIN_DISTANCE;
            return;
        }
        BoundingBox box = session.villageBounds;
        double cx = (box.minX() + box.maxX() + 1) * 0.5D;
        double cz = (box.minZ() + box.maxZ() + 1) * 0.5D;
        session.villageCenter = new Vec3(cx, 0.0D, cz);
        double halfX = (box.maxX() - box.minX() + 1) * 0.5D;
        double halfZ = (box.maxZ() - box.minZ() + 1) * 0.5D;
        double halfDiag = Math.sqrt(halfX * halfX + halfZ * halfZ);
        session.abandonDistance = Math.max(ABANDON_MIN_DISTANCE, halfDiag + ABANDON_GRACE_BLOCKS);
    }

    private static double horizontalDistance(ServerPlayer player, Vec3 center)
    {
        double dx = player.getX() - center.x;
        double dz = player.getZ() - center.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void beginAmbush(ServerPlayer player, Session session)
    {
        MainQuest12LoopTrap.clear(player);
        clearMonologue(player, session, true);
        session.phase = Phase.AMBUSH;
        session.ambushOrigin = player.position();
        session.healthLocked = false;
        PlayerQuestData.setMain12AmbushStarted(player, true);
        PlayerQuestData.setMain12AmbushOrigin(player, BlockPos.containing(session.ambushOrigin));
        spawnGrade3Around(player);
        playAmbushQueue(player);
        MainQuestLogic.syncTrackHud(player);
    }

    private static void playMonologueQueue(ServerPlayer player, Session session)
    {
        session.monologueFinished = true;
        int gap = LINE_TICKS + GAP_TICKS;
        int tail = MONOLOGUE_KEYS.length - 1;
        int[] delays = new int[MONOLOGUE_KEYS.length];
        int[] holds = new int[MONOLOGUE_KEYS.length];
        for (int i = 0; i < MONOLOGUE_KEYS.length; i++)
        {
            // 末句（「地上还有没收干净的东西……」）与前句相隔 10 秒，其余维持默认节奏。
            delays[i] = i == 0 ? 0 : (i == tail ? TAIL_LINE_DELAY_TICKS : gap);
            holds[i] = gap;
        }
        NetworkSenders.toPlayer(player, SyncQuestCodCaptionsS2CPacket.play(MONOLOGUE_KEYS, delays, holds));
    }

    private static void playAmbushQueue(ServerPlayer player)
    {
        int beat = LINE_TICKS + GAP_TICKS;
        // t=0 你 / t=8 甲战吼（同屏）→ t=80 你 / t=88 甲战吼（同屏）
        int[] delays = {0, MULTI_SPEAKER_GAP_TICKS, beat - MULTI_SPEAKER_GAP_TICKS, MULTI_SPEAKER_GAP_TICKS};
        int[] holds = {beat, beat - MULTI_SPEAKER_GAP_TICKS, beat, LINE_TICKS};
        NetworkSenders.toPlayer(player, SyncQuestCodCaptionsS2CPacket.play(AMBUSH_KEYS, delays, holds));
    }

    private static void clearMonologue(ServerPlayer player, Session session, boolean immediate)
    {
        session.monologueFinished = true;
        NetworkSenders.toPlayer(player, immediate
                ? SyncQuestCodCaptionsS2CPacket.clearImmediate()
                : SyncQuestCodCaptionsS2CPacket.fadeAll());
    }

    private static void spawnGrade3Around(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        int spawned = 0;
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS && spawned < MAX_SPAWN; attempt++)
        {
            double angle = level.random.nextDouble() * (Math.PI * 2.0D);
            double dist = Mth.lerp(level.random.nextDouble(), SPAWN_MIN_DIST, SPAWN_MAX_DIST);
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            Grade3CurseSpiritAEntity entity = ModEntities.GRADE_3_CURSE_SPIRIT_A.get().create(level);
            if (entity == null)
            {
                continue;
            }
            entity.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(entity) || !entity.checkSpawnObstruction(level))
            {
                entity.discard();
                continue;
            }
            DifficultyInstance difficulty = level.getCurrentDifficultyAt(pos);
            entity.finalizeSpawn(level, difficulty, MobSpawnType.EVENT, null, null);
            entity.setPersistenceRequired();
            entity.setTarget(player);
            if (level.addFreshEntity(entity))
            {
                spawned++;
            }
        }
    }

    private static Optional<BoundingBox> resolveVillageBounds(ServerPlayer player)
    {
        Optional<BoundingBox> pinned = NarrativeVillage.resolveBounds(player);
        if (pinned.isPresent())
        {
            return pinned;
        }
        // 异常：1.1 未留下锁定点时，一次性 locate 并锁定，避免后续漂移
        ServerLevel level = player.serverLevel();
        Optional<BlockPos> found = StructureLocator.findNearest(
                level,
                player.blockPosition(),
                StructureLocateTarget.NEAREST_VILLAGE,
                StructureLocator.DEFAULT_RADIUS_CHUNKS);
        if (found.isEmpty())
        {
            return Optional.empty();
        }
        NarrativeVillage.pin(player, found.get());
        return NarrativeVillage.resolveBounds(player);
    }

    private static boolean isInside(ServerPlayer player, BoundingBox box)
    {
        AABB aabb = new AABB(
                box.minX(),
                box.minY(),
                box.minZ(),
                box.maxX() + 1.0D,
                box.maxY() + 1.0D,
                box.maxZ() + 1.0D);
        return aabb.contains(player.getX(), player.getY(), player.getZ());
    }

    private enum Phase
    {
        WAIT_ENTER,
        EXPLORE,
        AMBUSH
    }

    private static final class Session
    {
        Phase phase = Phase.WAIT_ENTER;
        @Nullable BoundingBox villageBounds;
        /** 村庄水平中心（Y 忽略）。 */
        @Nullable Vec3 villageCenter;
        /** 超出该水平距离则放弃探索。 */
        double abandonDistance = ABANDON_MIN_DISTANCE;
        int exploreTicks;
        boolean monologueFinished;
        @Nullable Vec3 ambushOrigin;
        boolean healthLocked;
        boolean pendingComplete;
    }
}
