package com.darkness.jujutsukaisenzone.entity.combat;

/**
 * 怪物韧性条的一项配置（数据包 {@code mob_combat_stats} 里的 {@code poise} 块）。
 * <p>
 * 语义边界：
 * <ul>
 *   <li>{@code poise} 是<b>韧性上限</b>，不是护甲、不吃穿甲、不吃体抗/术抗。</li>
 *   <li>{@code damageReduction} 是「韧性未破」时的伤害减免比例，走<b>独立乘区</b>
 *       （{@code damage × (1 - r)}）。<b>负数表示未指定</b>，按 {@link PoiseRules} 的档位派生。</li>
 *   <li>{@code specialized} 决定 {@code resistance} 是否生效：
 *       <b>只有它为 true 时该怪才对 Break 效力打折</b>，为 false 时 Break 全额生效。
 *       ⚠️ 它<b>不</b>控制任何特质相关的东西——咒力特质克制表已删除。</li>
 *   <li>{@code resistance} 是 <b>Break 效力</b>在该怪身上的打折系数（0~1，1.0 = 不打折）。
 *       存在的理由：不给高抗性怪打折的话，堆 Break 的玩家会把所有怪的条一起秒掉，
 *       韧性条本身（霸体 + 未破减伤的那段强势期）就失去意义。</li>
 * </ul>
 */
public record PoiseStats(
        double poise,
        double poiseRegen,
        double damageReduction,
        boolean specialized,
        double poiseResistance)
{
    /** 每秒回复点数的默认值。 */
    public static final double DEFAULT_REGEN = 4.0D;
    /** Break 效力在特攻怪身上的默认打折系数。 */
    public static final double DEFAULT_POISE_RESISTANCE = 0.35D;
    /** {@link #damageReduction} 未指定时的哨兵值。 */
    public static final double REDUCTION_DERIVED = -1.0D;

    /** 是否真的会给这只怪挂上韧性条。{@code poise <= 0} 表示显式关闭。 */
    public boolean enabled()
    {
        return this.poise > 0.0D;
    }

    /** 减免比例是否由档位派生（数据包没写）。 */
    public boolean reductionDerived()
    {
        return this.damageReduction < 0.0D;
    }

    /** 每秒回复点数，负数/NaN 视为默认。 */
    public double regenOrDefault()
    {
        return this.poiseRegen > 0.0D ? this.poiseRegen : DEFAULT_REGEN;
    }
}
