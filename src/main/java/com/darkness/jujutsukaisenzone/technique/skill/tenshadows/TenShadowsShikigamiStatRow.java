package com.darkness.jujutsukaisenzone.technique.skill.tenshadows;

/**
 * 单只式神的战斗面板一行（未调伏固定 / 已调伏基础共用结构）。
 * <p>
 * 攻击侧是体术/术式<strong>强度</strong>，不是原版 {@code ATTACK_DAMAGE}。
 */
public record TenShadowsShikigamiStatRow(
        double maxHealth,
        double armor,
        double physicalResistance,
        double techniqueResistance,
        double physicalPenetration,
        double techniquePenetration,
        int physicalPower,
        int techniquePower)
{
    public static final TenShadowsShikigamiStatRow EMPTY = new TenShadowsShikigamiStatRow(
            20.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0);
}
