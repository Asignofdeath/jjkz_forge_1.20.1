package com.darkness.jujutsukaisenzone.technique.se;

import net.minecraft.network.chat.Component;

/**
 * SE 效果处理器：与 {@link SeEffectDef}（装载元数据）成对。
 * <p>
 * 机制修改式效果<b>不得</b>改写全局常量 / Config；只在带玩家参数的解析点
 * 通过 {@link SeMechanicRegistry#findEquipped} / {@link SeMechanicRegistry#allEquipped} 查询。
 * 未装备时走原路径，卸下后无需清理全局状态。
 */
public interface SeMechanic
{
    String seId();

    /** UI 效果行（数值写在实现或 lang 里，不读 {@link SeEffectDef} 上的烟管时代字段）。 */
    Component effectDescription();
}
