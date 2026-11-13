package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 切痕上限加成 SE 窄口（机制修改式）。
 * <p>
 * 调用方：{@code DismembermentSystem#cutMarkCap}；只影响装载了对应 SE 的玩家（作为断肢目标时）。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface CutMarkCapSeMechanic extends SeMechanic
{
    /** 在基础上限之外，每点最大生命额外贡献的切痕上限。 */
    float extraCapPerMaxHealth();

    /** 允许略超全局硬顶后的软顶。 */
    int softCapMax();
}
