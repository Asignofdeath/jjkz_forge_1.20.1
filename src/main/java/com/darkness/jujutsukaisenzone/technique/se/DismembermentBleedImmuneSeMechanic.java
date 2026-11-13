package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 断肢对策：断肢后不触发流血。
 * <p>
 * 调用方：{@code DismembermentSystem#trySever} → mcg Compat，经 {@code HeavenBalance#findActive}。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface DismembermentBleedImmuneSeMechanic extends SeMechanic
{
}
