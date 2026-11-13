package com.darkness.jujutsukaisenzone.heaven.threat;

import java.util.Objects;

/**
 * 天道对策目录项：指向已有玩家 SE（{@code seId}），由压力桶解锁后可对咒灵生效。
 * <p>
 * 效果数值不在此重复声明，运行时走 {@code SeMechanicRegistry.find(seId)}。
 */
public record HeavenCounterDef(
        String seId,
        String threatId,
        int unlockPressure,
        int weight)
{
    public HeavenCounterDef
    {
        if (seId == null || seId.isBlank())
        {
            throw new IllegalArgumentException("seId");
        }
        if (threatId == null || threatId.isBlank())
        {
            throw new IllegalArgumentException("threatId");
        }
        if (unlockPressure < 0)
        {
            throw new IllegalArgumentException("unlockPressure < 0：" + seId);
        }
        if (weight < 1)
        {
            throw new IllegalArgumentException("weight 必须 >= 1：" + seId);
        }
    }

    public static HeavenCounterDef of(String seId, String threatId, int unlockPressure)
    {
        return new HeavenCounterDef(seId, threatId, unlockPressure, 1);
    }

    public static HeavenCounterDef of(String seId, String threatId, int unlockPressure, int weight)
    {
        return new HeavenCounterDef(seId, threatId, unlockPressure, weight);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof HeavenCounterDef that))
        {
            return false;
        }
        return Objects.equals(seId, that.seId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(seId);
    }
}
