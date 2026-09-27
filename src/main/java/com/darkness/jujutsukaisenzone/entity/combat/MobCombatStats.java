package com.darkness.jujutsukaisenzone.entity.combat;

import org.jetbrains.annotations.Nullable;

/**
 * 生物战斗面板一行：体抗 / 术抗 / 穿甲 / 术穿（百分比点数，与 {@code ModAttributes} 同单位）。
 * 缺省字段按 0 处理；全部为 0 表示「表里登记了但不起额外修正」。
 * <p>
 * {@link #poise} 为 {@code null} 表示<b>没写 poise 块</b>——此时按
 * {@link PoiseRules#qualifiesByToughness(double)} 的默认门槛派生。
 * 写了 poise 块但 {@code value} 为 0，则表示<b>显式关闭</b>该怪的韧性条，两者语义不同。
 */
public record MobCombatStats(
        double physicalResistance,
        double techniqueResistance,
        double physicalPenetration,
        double techniquePenetration,
        @Nullable PoiseStats poise)
{
    /** 保留旧的四参数构造：不声明 poise 块的条目仍走派生规则。 */
    public MobCombatStats(double physicalResistance, double techniqueResistance,
                          double physicalPenetration, double techniquePenetration)
    {
        this(physicalResistance, techniqueResistance, physicalPenetration, techniquePenetration, null);
    }

    public static final MobCombatStats ZERO = new MobCombatStats(0, 0, 0, 0);

    public boolean isZero()
    {
        return physicalResistance == 0.0D
                && techniqueResistance == 0.0D
                && physicalPenetration == 0.0D
                && techniquePenetration == 0.0D;
    }

    /** 体抗 + 术抗：韧性档位与默认门槛都用这个量，改口径时只改这里。 */
    public double toughness()
    {
        return this.physicalResistance + this.techniqueResistance;
    }

    /** 是否显式声明了 poise 块（用于区分「没写」和「写了 0」）。 */
    public boolean hasPoiseBlock()
    {
        return this.poise != null;
    }
}
