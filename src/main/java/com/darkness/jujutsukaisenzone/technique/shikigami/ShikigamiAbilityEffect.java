package com.darkness.jujutsukaisenzone.technique.shikigami;

import net.minecraft.server.level.ServerPlayer;

/**
 * 式神招式效果。返回 {@code true} 表示释放成功（由 Runtime 写入公用 CD / 扣咒力已在调用前完成条件检查后、效果成功时落盘）。
 * <p>
 * 条件不满足时应返回 {@code false}，Runtime 不会上 CD、并退还已扣咒力。
 */
@FunctionalInterface
public interface ShikigamiAbilityEffect
{
    boolean execute(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source);
}
