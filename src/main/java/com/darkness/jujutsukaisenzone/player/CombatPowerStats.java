package com.darkness.jujutsukaisenzone.player;

import java.util.List;

/**
 * 战斗强度面板快照：体术强度 / 术式强度，以及各自的构成明细。
 * <p>
 * 强度是<b>整数面板值</b>，本身不是伤害；技能出伤为 {@code 强度 × 技能倍率}，见 {@code SkillDamageResolver}。
 */
public record CombatPowerStats(
        int physicalPower,
        int techniquePower,
        List<Contribution> physicalBreakdown,
        List<Contribution> techniqueBreakdown)
{
    public CombatPowerStats(int physicalPower, int techniquePower)
    {
        this(physicalPower, techniquePower, List.of(), List.of());
    }

    /** 负值代表「尚未收到服务端同步」，消费方应按未启用处理。 */
    public boolean resolved()
    {
        return physicalPower >= 0 || techniquePower >= 0;
    }

    public record Contribution(String label, int amount)
    {
    }
}
