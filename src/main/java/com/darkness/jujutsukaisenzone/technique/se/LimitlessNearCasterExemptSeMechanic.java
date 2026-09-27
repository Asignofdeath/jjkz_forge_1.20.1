package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 无量空处「身旁豁免」的 SE 窄口（机制修改式）。
 * <p>
 * 调用方：{@code LimitlessDomainExpansion}；只影响装载了对应 SE 的施法者。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface LimitlessNearCasterExemptSeMechanic extends SeMechanic
{
    /**
     * {@code true} = 不建身旁白名单、身旁单位不再豁免领域锁定。
     */
    boolean removesNearCasterExempt();
}
