package com.darkness.jujutsukaisenzone.heaven.threat;

import com.darkness.jujutsukaisenzone.heaven.adapter.HeavenAdapter;

/**
 * 天道适配器：维度压力解锁对策池，咒灵按等级掷骰获得资格。
 */
public final class CurseCounterSeAdapter implements HeavenAdapter
{
    public static final String ADAPTER_ID = "curse_counter_se";
    public static final CurseCounterSeAdapter INSTANCE = new CurseCounterSeAdapter();

    private CurseCounterSeAdapter()
    {
    }

    @Override
    public String id()
    {
        return ADAPTER_ID;
    }

    @Override
    public String description()
    {
        return "咒灵对策 SE：维度压力开池 + 等级掷骰资格，效果复用玩家 SE mechanic";
    }
}
