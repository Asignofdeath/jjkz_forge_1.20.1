package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 断肢对策：生命抬高每肢切痕上限（可适度超过默认绝对封顶）。
 * <p>
 * 调用方：{@code DismembermentSystem#cutMarkCap}，经 {@code HeavenBalance#findActive}（玩家装载或咒灵天道）。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface CutMarkCapBonusSeMechanic extends SeMechanic
{
    /** 额外上限 = ceil(maxHealth × 本系数)。 */
    float extraCapPerMaxHealth();

    /** 有本 SE 时允许的切痕上限软顶（可高于默认 {@code CUT_MARK_CAP_MAX}）。 */
    int softCapMax();
}
