package com.darkness.jujutsukaisenzone.player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家结界规则数据管理器。
 * 每个玩家可以独立配置结界的行为规则。
 */
public final class BarrierRulesData
{
    private static final ConcurrentHashMap<UUID, BarrierRules> PLAYER_RULES = new ConcurrentHashMap<>();

    private BarrierRulesData()
    {
    }

    /**
     * 获取玩家的结界规则
     */
    public static BarrierRules getRules(UUID playerId)
    {
        return PLAYER_RULES.computeIfAbsent(playerId, k -> new BarrierRules());
    }

    /**
     * 设置玩家的阻止生物生成规则
     */
    public static void setBlockMobSpawn(UUID playerId, boolean blockMobSpawn)
    {
        getRules(playerId).blockMobSpawn = blockMobSpawn;
    }

    /**
     * 获取玩家是否启用了阻止生物生成
     */
    public static boolean isBlockMobSpawn(UUID playerId)
    {
        return getRules(playerId).blockMobSpawn;
    }

    /**
     * 设置玩家的自动修补结界规则
     */
    public static void setAutoRepairBarrier(UUID playerId, boolean autoRepairBarrier)
    {
        getRules(playerId).autoRepairBarrier = autoRepairBarrier;
    }

    /**
     * 获取玩家是否启用了自动修补结界
     */
    public static boolean isAutoRepairBarrier(UUID playerId)
    {
        return getRules(playerId).autoRepairBarrier;
    }

    /**
     * 玩家结界规则数据
     */
    public static class BarrierRules
    {
        /** 是否阻止生物在结界内生成 */
        public boolean blockMobSpawn = false;
        /** 是否自动修补结界/领域（需习得反转术式，在结界内时消耗咒力修补） */
        public boolean autoRepairBarrier = false;
    }
}
