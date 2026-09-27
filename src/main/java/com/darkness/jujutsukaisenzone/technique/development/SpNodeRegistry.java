package com.darkness.jujutsukaisenzone.technique.development;

/**
 * 旧固定节点表已废弃；SP 节点改为模板族 {@link SpNodeFamily} + 玩家实例 {@link SpNodeInstance}。
 * 保留此类仅作启动钩子占位，避免调用方编译失败。
 */
public final class SpNodeRegistry
{
    private SpNodeRegistry()
    {
    }

    public static void bootstrap()
    {
        // 无静态节点表；roll 见 SpNodeRoller。
    }
}
