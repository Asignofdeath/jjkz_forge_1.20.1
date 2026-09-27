package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 体术暴击后触发的 SE 窄口（条件附加式）。
 * <p>
 * 调用方：{@code SeEffectEvents} 在确认体术暴击后查询已装载实现。
 */
public interface MartialCritSeMechanic extends SeMechanic
{
    /** 点燃秒数；0 表示本实现不点燃。 */
    int igniteSeconds();
}
