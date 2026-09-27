package com.darkness.jujutsukaisenzone.technique.se;

/**
 * SE 的代价：削弱<b>一条</b>面板 / 资源属性（落点见 {@link PanelStat}）。
 * <p>
 * ⛔ 不得作用于咒力特质等身份项。
 * <p>
 * {@code amount} 用正数表示「削弱强度」：多数落点转为减法；
 * {@link PanelStat#DOMAIN_GAUGE_MAX} 转为「满条值 +amount」。
 */
public record SePenalty(PanelStat stat, double amount)
{
    public SePenalty
    {
        if (stat == null)
        {
            throw new IllegalArgumentException("stat（SE 的代价必须写明削弱哪条属性）");
        }
        amount = Math.max(0.0D, amount);
    }

    public int powerRowPenalty()
    {
        return (int) Math.ceil(amount);
    }

    /** Attribute / 资源层的修饰数值（负数 = 削弱）。 */
    public double attributeDelta()
    {
        return -amount;
    }

    public int resourcePoints()
    {
        return (int) Math.ceil(amount);
    }

    public int domainGaugeMaxBonus()
    {
        return resourcePoints();
    }

    public int brainDamageMaxPenalty()
    {
        return resourcePoints();
    }
}
