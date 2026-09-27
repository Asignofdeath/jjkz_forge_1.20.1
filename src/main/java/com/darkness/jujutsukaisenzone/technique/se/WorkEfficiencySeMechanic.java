package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 「工作效率」触发窄口：装载后，单次消耗足够咒力即可进入与无可谅解相同的效率窗口。
 * <p>
 * 调用方：{@code PlayerCursedEnergyData#tryConsume}；状态本体仍在 {@code PlayerRatioOvertimeData}。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface WorkEfficiencySeMechanic extends SeMechanic
{
    /**
     * 单次 {@code tryConsume} 成功消耗达到该值时触发 / 刷新工作效率。
     */
    int triggerConsumeThreshold();
}
