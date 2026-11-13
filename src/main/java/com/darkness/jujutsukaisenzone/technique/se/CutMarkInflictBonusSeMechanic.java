package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 断肢进攻：护甲/体抗结算后的加痕量再乘系数。
 * <p>
 * 调用方：{@code DismembermentSystem#applyCutMarks}，查攻击方装载。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface CutMarkInflictBonusSeMechanic extends SeMechanic
{
    /** 乘在 floor 后的加痕上（例如 1.2 = +20%）。 */
    float inflictMultiplier();
}
