package com.darkness.jujutsukaisenzone.heaven.threat;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 每维度天道压力表：按 threatId 独立累压，空闲后衰减。
 */
public final class HeavenBalanceData extends SavedData
{
    private static final String DATA_NAME = JuJutsuKaisenZone.MODID + "_heaven_balance";
    private static final String TAG_THREATS = "threats";
    private static final String TAG_PRESSURE = "pressure";
    private static final String TAG_LAST_ACTIVITY = "lastActivity";

    public static final int PRESSURE_MAX = 100;
    /** 对咒灵断肢成功一次加多少压力。 */
    public static final int PRESSURE_PER_SEVER = 4;
    /** 无活动多久后开始衰减（tick）。 */
    public static final int DECAY_IDLE_TICKS = 20 * 60 * 10;
    /** 开始衰减后每隔多久掉 1 点。 */
    public static final int DECAY_INTERVAL_TICKS = 20 * 60;

    private final Map<String, ThreatState> threats = new HashMap<>();

    public static HeavenBalanceData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(
                HeavenBalanceData::load,
                HeavenBalanceData::new,
                DATA_NAME);
    }

    private static HeavenBalanceData load(CompoundTag root)
    {
        HeavenBalanceData data = new HeavenBalanceData();
        CompoundTag threatsTag = root.getCompound(TAG_THREATS);
        for (String threatId : threatsTag.getAllKeys())
        {
            CompoundTag entry = threatsTag.getCompound(threatId);
            data.threats.put(threatId, new ThreatState(
                    entry.getInt(TAG_PRESSURE),
                    entry.getLong(TAG_LAST_ACTIVITY)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root)
    {
        CompoundTag threatsTag = new CompoundTag();
        for (Map.Entry<String, ThreatState> entry : threats.entrySet())
        {
            CompoundTag tag = new CompoundTag();
            tag.putInt(TAG_PRESSURE, entry.getValue().pressure);
            tag.putLong(TAG_LAST_ACTIVITY, entry.getValue().lastActivityGameTime);
            threatsTag.put(entry.getKey(), tag);
        }
        root.put(TAG_THREATS, threatsTag);
        return root;
    }

    public int getPressure(ServerLevel level, String threatId)
    {
        if (threatId == null || threatId.isBlank())
        {
            return 0;
        }
        ThreatState state = threats.get(threatId);
        if (state == null)
        {
            return 0;
        }
        int effective = effectivePressure(state, level.getGameTime());
        if (effective != state.pressure)
        {
            state.pressure = effective;
            if (effective <= 0)
            {
                threats.remove(threatId);
            }
            setDirty();
        }
        return effective;
    }

    public void addPressure(ServerLevel level, String threatId, int amount)
    {
        if (level == null || threatId == null || threatId.isBlank() || amount <= 0)
        {
            return;
        }
        long now = level.getGameTime();
        ThreatState state = threats.computeIfAbsent(threatId, ignored -> new ThreatState(0, now));
        state.pressure = effectivePressure(state, now);
        state.pressure = Mth.clamp(state.pressure + amount, 0, PRESSURE_MAX);
        state.lastActivityGameTime = now;
        if (state.pressure <= 0)
        {
            threats.remove(threatId);
        }
        setDirty();
    }

    private static int effectivePressure(ThreatState state, long now)
    {
        if (state.pressure <= 0)
        {
            return 0;
        }
        long idle = now - state.lastActivityGameTime;
        if (idle < DECAY_IDLE_TICKS)
        {
            return state.pressure;
        }
        long overdue = idle - DECAY_IDLE_TICKS;
        int steps = (int) (overdue / DECAY_INTERVAL_TICKS) + 1;
        return Math.max(0, state.pressure - steps);
    }

    private static final class ThreatState
    {
        private int pressure;
        private long lastActivityGameTime;

        private ThreatState(int pressure, long lastActivityGameTime)
        {
            this.pressure = pressure;
            this.lastActivityGameTime = lastActivityGameTime;
        }
    }
}
