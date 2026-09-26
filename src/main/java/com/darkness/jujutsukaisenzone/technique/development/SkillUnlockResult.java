package com.darkness.jujutsukaisenzone.technique.development;

import net.minecraft.network.chat.Component;

/**
 * 服务端尝试解锁技能节点的结果。
 */
public enum SkillUnlockResult
{
    SUCCESS,
    ALREADY_UNLOCKED,
    NOT_SKILL_NODE,
    UNKNOWN_NODE,
    MISSING_PREREQUISITES,
    CONDITIONS_NOT_MET,
    INSUFFICIENT_INTEL,
    INSUFFICIENT_VANILLA_XP,
    INSUFFICIENT_SP,
    ACTIVE_NOT_MAX_LEVEL,
    WRONG_TECHNIQUE;

    public Component toMessage()
    {
        return Component.translatable("message.jujutsukaisenzone.skill_unlock." + name().toLowerCase());
    }
}
