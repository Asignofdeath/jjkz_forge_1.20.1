package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 领域量表「脑损 → 充能效率」曲线的 SE 窄口（机制修改式）。
 * <p>
 * 调用方：{@code PlayerDomainGaugeData#brainDamageEfficiency}；
 * 满脑损冻结仍由外层 {@code chargeEfficiency} 处理，本窄口改不了那道硬闸。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface DomainChargeCurveSeMechanic extends SeMechanic
{
    /**
     * 改写脑损档位效率。
     *
     * @param brainDamage        当前脑损
     * @param defaultEfficiency  无 SE 时该档位的配置效率（0～1）
     * @return 最终脑损侧系数
     */
    double adjustBrainTierEfficiency(int brainDamage, double defaultEfficiency);
}
