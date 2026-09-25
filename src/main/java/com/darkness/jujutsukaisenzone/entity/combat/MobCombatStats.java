package com.darkness.jujutsukaisenzone.entity.combat;

/**
 * 生物战斗面板一行：体抗 / 术抗 / 穿甲 / 术穿（百分比点数，与 {@code ModAttributes} 同单位）。
 * 缺省字段按 0 处理；全部为 0 表示「表里登记了但不起额外修正」。
 */
public record MobCombatStats(
        double physicalResistance,
        double techniqueResistance,
        double physicalPenetration,
        double techniquePenetration)
{
    public static final MobCombatStats ZERO = new MobCombatStats(0, 0, 0, 0);

    public boolean isZero()
    {
        return physicalResistance == 0.0D
                && techniqueResistance == 0.0D
                && physicalPenetration == 0.0D
                && techniquePenetration == 0.0D;
    }
}
