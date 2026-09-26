package com.darkness.jujutsukaisenzone.technique.development;

/**
 * 主动技能升级结果。
 */
public enum SkillUpgradeResult
{
    SUCCESS,
    ALREADY_MAX,
    NOT_UNLOCKED,
    UNKNOWN_SKILL,
    NOT_UPGRADEABLE,
    INSUFFICIENT_VANILLA_XP,
    MISSING_CURSED_OBJECT,
    WRONG_TECHNIQUE;

    public net.minecraft.network.chat.Component toMessage()
    {
        return net.minecraft.network.chat.Component.translatable(
                "message.jujutsukaisenzone.skill_upgrade." + name().toLowerCase());
    }
}
