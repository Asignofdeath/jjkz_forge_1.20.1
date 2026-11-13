package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 断肢对策：切痕自然恢复加快。
 * <p>
 * 调用方：{@code DismembermentSystem#tickDecay}，经 {@code HeavenBalance#findActive}（玩家装载或咒灵天道）。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface CutMarkRecoverySeMechanic extends SeMechanic
{
    /** 衰减间隔 tick（默认 40；越小越快）。 */
    int decayIntervalTicks();

    /** 每次衰减减去的层数（默认 1）。 */
    int decayAmount();
}
