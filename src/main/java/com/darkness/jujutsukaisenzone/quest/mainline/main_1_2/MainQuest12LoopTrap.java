package com.darkness.jujutsukaisenzone.quest.mainline.main_1_2;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestCodCaptionsS2CPacket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 主线 1.2 探索期「鬼打墙」：足迹折返，固定编排触发 1～2 次，附带一句自述。
 */
public final class MainQuest12LoopTrap
{
    private static final int SAMPLE_INTERVAL = 10;
    private static final int TRAIL_CAPACITY = 12;
    /** 一场探索最多触发次数。 */
    private static final int MAX_TRIGGERS = 2;
    /** 第 1 次固定触发刻（探索 30 秒）。 */
    private static final int FIRST_TRIGGER_TICK = 600;
    /** 第 2 次固定触发刻（探索 75 秒）。 */
    private static final int SECOND_TRIGGER_TICK = 1500;
    /** 折返到约 4～8 秒前的足迹（取样间隔 10 tick）。 */
    private static final int REWIND_MIN = 4;
    private static final int REWIND_MAX = 8;
    private static final int LINE_HOLD_TICKS = 70;

    private static final String[] LOOP_KEYS = {
            "quest.jujutsukaisenzone.main_1_2.loop.1",
            "quest.jujutsukaisenzone.main_1_2.loop.2"
    };

    private static final Map<UUID, TrapState> STATES = new ConcurrentHashMap<>();

    private MainQuest12LoopTrap()
    {
    }

    public static void clear(ServerPlayer player)
    {
        STATES.remove(player.getUUID());
    }

    public static void clear(UUID playerId)
    {
        STATES.remove(playerId);
    }

    /**
     * 仅探索阶段调用。
     *
     * @param villageCenter   村庄水平中心（可空：空则不做折返安全校验）
     * @param abandonDistance 放弃距离；折返点须仍在此内
     */
    public static void tick(
            ServerPlayer player,
            int exploreTicks,
            @Nullable Vec3 villageCenter,
            double abandonDistance)
    {
        TrapState state = STATES.computeIfAbsent(player.getUUID(), id -> new TrapState());
        sampleTrail(player, state);
        planTriggers(player, state);

        if (state.triggers >= state.plannedTriggers)
        {
            return;
        }
        if (exploreTicks < state.plannedTriggerTicks[state.triggers])
        {
            return;
        }
        if (state.trailCount < REWIND_MIN + 1)
        {
            return;
        }
        if (!tryRewind(player, state, villageCenter, abandonDistance))
        {
            return;
        }
        state.triggers++;
        playLoopLine(player);
    }

    /**
     * 每场探索开排一次：随机 1 或 2 次，落点固定在第 30 / 75 秒。
     * <p>
     * 不再掷概率——旧实现里「相对上次取样（10 tick）至少走 6 格」的闸门在 0.5 秒内
     * 根本走不满，导致整段探索几乎永不触发。
     */
    private static void planTriggers(ServerPlayer player, TrapState state)
    {
        if (state.planned)
        {
            return;
        }
        state.planned = true;
        state.plannedTriggers = player.getRandom().nextBoolean() ? 1 : 2;
        state.plannedTriggerTicks[0] = FIRST_TRIGGER_TICK;
        state.plannedTriggerTicks[1] = state.plannedTriggers >= 2 ? SECOND_TRIGGER_TICK : Integer.MAX_VALUE;
    }

    private static void sampleTrail(ServerPlayer player, TrapState state)
    {
        if (player.tickCount % SAMPLE_INTERVAL != 0)
        {
            return;
        }
        Vec3 pos = player.position();
        state.trailX[state.trailWrite] = pos.x;
        state.trailY[state.trailWrite] = pos.y;
        state.trailZ[state.trailWrite] = pos.z;
        state.trailWrite = (state.trailWrite + 1) % TRAIL_CAPACITY;
        if (state.trailCount < TRAIL_CAPACITY)
        {
            state.trailCount++;
        }
    }

    private static boolean tryRewind(
            ServerPlayer player,
            TrapState state,
            @Nullable Vec3 villageCenter,
            double abandonDistance)
    {
        int maxRewind = Math.min(REWIND_MAX, state.trailCount - 1);
        int minRewind = Math.min(REWIND_MIN, maxRewind);
        if (maxRewind < 1)
        {
            return false;
        }
        int rewind = Mth.randomBetweenInclusive(player.getRandom(), minRewind, maxRewind);
        // trailWrite 指向下一写入位；最新样本在 write-1
        int index = Math.floorMod(state.trailWrite - 1 - rewind, TRAIL_CAPACITY);
        double x = state.trailX[index];
        double y = state.trailY[index];
        double z = state.trailZ[index];
        if (villageCenter != null)
        {
            double dx = x - villageCenter.x;
            double dz = z - villageCenter.z;
            if (dx * dx + dz * dz > abandonDistance * abandonDistance)
            {
                return false;
            }
        }
        ServerLevel level = player.serverLevel();
        int ix = Mth.floor(x);
        int iz = Mth.floor(z);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
        double destY = Math.max(y, surfaceY);
        float yaw = player.getYRot() + (player.getRandom().nextBoolean() ? 90.0F : 180.0F);
        player.teleportTo(x, destY, z);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.connection.resetPosition();
        return true;
    }

    private static void playLoopLine(ServerPlayer player)
    {
        String key = LOOP_KEYS[player.getRandom().nextInt(LOOP_KEYS.length)];
        NetworkSenders.toPlayer(player, SyncQuestCodCaptionsS2CPacket.playUniform(
                new String[] {key},
                0,
                LINE_HOLD_TICKS));
    }

    private static final class TrapState
    {
        final double[] trailX = new double[TRAIL_CAPACITY];
        final double[] trailY = new double[TRAIL_CAPACITY];
        final double[] trailZ = new double[TRAIL_CAPACITY];
        int trailWrite;
        int trailCount;
        int triggers;
        boolean planned;
        int plannedTriggers;
        final int[] plannedTriggerTicks = new int[MAX_TRIGGERS];
    }
}
