package com.darkness.jujutsukaisenzone.technique.se;

/**
 * SE 触发 / 生效形态（装载元数据用；真正效果在 {@link SeMechanic}）。
 * <p>
 * ⚠️ 与 mechanic 实现的窄口接口是<b>同一件事的两种写法</b>，必须保持一致：
 * <ul>
 *   <li>{@link #ON_MARTIAL_CRIT} ⟺ mechanic 实现了 {@link MartialCritSeMechanic}（走事件链）。</li>
 *   <li>{@link #PASSIVE} ⟺ 由机制点主动查询 {@link SeMechanicRegistry}，无事件附加链。</li>
 * </ul>
 * 该一致性由 {@code SeMechanicRegistry#validatePairedWithEffects()} 在启动期强制校验：
 * 只改 {@code SeEffectDef} 上的 trigger 而不改 mechanic 实现，游戏会直接起不来。
 */
public enum SeTrigger
{
    /** 造成体术暴击后触发（黑闪不算）。 */
    ON_MARTIAL_CRIT,

    /** 常驻改规则：机制点主动问 {@link SeMechanicRegistry}，无事件附加链。 */
    PASSIVE
}
