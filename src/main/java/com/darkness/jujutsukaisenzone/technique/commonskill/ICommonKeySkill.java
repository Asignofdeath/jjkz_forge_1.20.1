package com.darkness.jujutsukaisenzone.technique.commonskill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;

/**
 * 独立按键绑定的通用技能接口：不依赖生得术式，也不占用术式 1~9 槽位。
 * <p>
 * 业务意图：把「反转术式」这类全局技能从主类硬编码里抽离，统一走注册表分发，便于后续扩展更多独立按键技能。
 */
public interface ICommonKeySkill
{
    /** 技能唯一标识，仅用于日志与调试定位。 */
    String skillId();

    /** 返回该技能对应的按键映射。 */
    KeyMapping keyMapping();

    /**
     * 客户端当前帧是否允许该技能触发。
     * <p>
     * 重要假设：这里只做本地前置校验，最终判定仍应在服务端逻辑中完成。
     */
    boolean canTriggerClient(LocalPlayer player);

    /**
     * 是否在「按键按下瞬间」触发。
     * <p>
     * 默认 false，表示走按住每 tick 触发；适用于持续型技能（如反转术式）。
     */
    default boolean triggerOnPress()
    {
        return false;
    }

    /**
     * 按键按下瞬间回调（只触发一次）。
     * <p>
     * 业务意图：用于一次性技能，避免按住时重复触发。
     */
    default void onKeyPressClientTick(Minecraft minecraft, LocalPlayer player)
    {
    }

    /**
     * 按键松开瞬间回调（只触发一次）。
     */
    default void onKeyReleaseClientTick(Minecraft minecraft, LocalPlayer player)
    {
    }

    /**
     * 按键按住期间每 tick 回调。
     * <p>
     * 副作用：通常会发送 C2S 包触发服务端执行。
     */
    void onKeyHoldClientTick(Minecraft minecraft, LocalPlayer player);
}
