package com.darkness.jujutsukaisenzone.compat;

/**
 * ParCool 空战联动桥接层占位。
 * <p>
 * 仅在 {@link ParCoolPresence#isAerialIntegrationAvailable()} 为 true 时由
 * {@link ParCoolIntegrationRegistrar} 注册到事件总线。
 * 后续空战逻辑（跑酷状态查询、术式门控等）在此扩展；可安全 import ParCool API。
 */
public final class ParCoolAerialBridge
{
    public ParCoolAerialBridge()
    {
    }
}
