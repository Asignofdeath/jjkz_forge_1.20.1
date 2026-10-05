package com.darkness.jujutsukaisenzone.quest.mainline.main_1_1;

import com.darkness.jujutsukaisenzone.locate.PlayerLocateGuidance;
import com.darkness.jujutsukaisenzone.locate.StructureLocateTarget;
import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDefs;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * 主线 1.1 前往村庄时：按导航剩余距离悄悄推进世界时间，抵达时已是夜晚。
 */
public final class MainQuest11VillageNightApproach
{
    /** 目标时刻：午夜，强化空村异样感。 */
    private static final long TARGET_TOD = 18000L;
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 23000L;
    /** 每 tick 最多拨快的时间（悄然；接近终点时会加大）。 */
    private static final long MAX_STEP = 18L;
    private static final long NEAR_ARRIVE_STEP = 120L;

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private MainQuest11VillageNightApproach()
    {
    }

    public static void tick(ServerPlayer player)
    {
        if (!MainQuestDefs.MAIN_1_1_ID.equals(PlayerQuestData.getTrackedId(player)))
        {
            clear(player);
            return;
        }
        if (PlayerQuestData.isCompleted(player, MainQuestDefs.MAIN_1_1_ID))
        {
            clear(player);
            return;
        }
        if (MainQuest11Intro.isRunning(player) || !PlayerQuestData.isMain11TalkDone(player))
        {
            return;
        }
        if (player.level().dimension() != Level.OVERWORLD)
        {
            return;
        }
        PlayerLocateGuidance.Session session = PlayerLocateGuidance.get(player);
        if (session == null || !StructureLocateTarget.NEAREST_VILLAGE.id().equals(session.targetId()))
        {
            clear(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        double remaining = horizontalDistance(player, session.targetPos());
        double arrive = session.arriveDistance();
        State state = STATES.computeIfAbsent(player.getUUID(), id -> createState(level, remaining, arrive));

        double span = Math.max(8.0D, state.initialDist - arrive);
        double progress = 1.0D - Mth.clamp((remaining - arrive) / span, 0.0D, 1.0D);
        long desired = state.startDayTime
                + Math.round((state.endDayTime - state.startDayTime) * progress);
        long current = level.getDayTime();
        if (desired > current)
        {
            long maxStep = progress >= 0.88D ? NEAR_ARRIVE_STEP : MAX_STEP;
            level.setDayTime(current + Math.min(maxStep, desired - current));
        }

        if (remaining <= arrive * 1.25D)
        {
            ensureNight(level);
        }
    }

    /** 抵达村庄时再兜底一次，保证入夜。 */
    public static void onArrived(ServerPlayer player)
    {
        if (player.level().dimension() == Level.OVERWORLD)
        {
            ensureNight(player.serverLevel());
        }
        clear(player);
    }

    public static void clear(ServerPlayer player)
    {
        STATES.remove(player.getUUID());
    }

    private static State createState(ServerLevel level, double remaining, double arrive)
    {
        long start = level.getDayTime();
        long tod = Math.floorMod(start, 24000L);
        long ticksToTarget = ticksForwardToTarget(tod);
        double initial = Math.max(remaining, arrive + 8.0D);
        return new State(initial, start, start + ticksToTarget);
    }

    /**
     * 从当前一日内时刻算到目标夜晚还需前进多少 tick（只向前拨）。
     * 已在夜间且已过午夜则不再拨；傍晚未到午夜则拨到午夜。
     */
    private static long ticksForwardToTarget(long tod)
    {
        if (tod >= NIGHT_START && tod < NIGHT_END)
        {
            if (tod < TARGET_TOD)
            {
                return TARGET_TOD - tod;
            }
            return 0L;
        }
        return Math.floorMod(TARGET_TOD - tod, 24000L);
    }

    private static void ensureNight(ServerLevel level)
    {
        long current = level.getDayTime();
        long tod = Math.floorMod(current, 24000L);
        if (tod >= NIGHT_START && tod < NIGHT_END)
        {
            if (tod < TARGET_TOD)
            {
                level.setDayTime(current + (TARGET_TOD - tod));
            }
            return;
        }
        level.setDayTime(current + Math.floorMod(TARGET_TOD - tod, 24000L));
    }

    private static double horizontalDistance(ServerPlayer player, BlockPos target)
    {
        double dx = player.getX() - (target.getX() + 0.5D);
        double dz = player.getZ() - (target.getZ() + 0.5D);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private record State(double initialDist, long startDayTime, long endDayTime)
    {
    }
}
