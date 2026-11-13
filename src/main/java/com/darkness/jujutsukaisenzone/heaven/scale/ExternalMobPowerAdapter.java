package com.darkness.jujutsukaisenzone.heaven.scale;

import com.darkness.jujutsukaisenzone.heaven.adapter.HeavenAdapter;

/**
 * 天道适配器：外模敌对/中立随在线均血缩放。
 */
public final class ExternalMobPowerAdapter implements HeavenAdapter
{
    public static final ExternalMobPowerAdapter INSTANCE = new ExternalMobPowerAdapter();

    private ExternalMobPowerAdapter()
    {
    }

    @Override
    public String id()
    {
        return ExternalMobPowerScale.ADAPTER_ID;
    }

    @Override
    public String description()
    {
        return "外模敌对/中立生物：生命×(在线均血/20)，出伤×√该系数";
    }
}
