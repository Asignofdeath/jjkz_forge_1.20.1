package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 断肢进攻：加痕时额外抬高 {@code baseMarks}。
 * <p>
 * 调用方：{@code DismembermentSystem#applyCutMarks}，查攻击方装载。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface CutMarkBaseBonusSeMechanic extends SeMechanic
{
    /** 叠进 {@code baseMarks} 的额外量（≥0）。 */
    float bonusBaseMarks();
}
