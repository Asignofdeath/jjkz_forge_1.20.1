package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 术式熔断修复规则的 SE 窄口（机制修改式）。
 * <p>
 * 调用方：{@code PlayerBrainDamageData#tryRepairMeltdown}；只影响装载了对应 SE 的玩家。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface MeltdownRepairSeMechanic extends SeMechanic
{
    /**
     * 修复咒力消耗倍率。{@code 1.0} = 不变；{@code 0.6} = 花费六成。
     */
    double repairCostMultiplier();

    /**
     * 本次修复产生的脑损倍率（含高脑损附加之后再乘）。
     * {@code 1.0} = 不变；{@code 0.7} = 七成。
     */
    double repairBrainGainMultiplier();
}
