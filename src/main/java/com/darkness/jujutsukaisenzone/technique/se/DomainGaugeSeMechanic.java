package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 领域量表灌充规则的 SE 窄口（机制修改式）。
 * <p>
 * 调用方：{@code PlayerDomainGaugeData}；只影响装载了对应 SE 的玩家。
 */
public interface DomainGaugeSeMechanic extends SeMechanic
{
    /**
     * 体术命中灌充相对「术式命中基数」的比例。
     * {@code 0} = 不启用体术灌充；{@code 0.5} = 半额。
     */
    float martialChargeRatio();
}
