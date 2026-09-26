package com.darkness.jujutsukaisenzone.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 咒物等级：常见 / 罕见 / 特级。
 */
public enum CursedObjectGrade
{
    COMMON("常见", ChatFormatting.WHITE),
    RARE("罕见", ChatFormatting.AQUA),
    SPECIAL("特级", ChatFormatting.GOLD);

    private final String displayName;
    private final ChatFormatting color;

    CursedObjectGrade(String displayName, ChatFormatting color)
    {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName()
    {
        return displayName;
    }

    public ChatFormatting color()
    {
        return color;
    }

    public Component tooltipLine()
    {
        return Component.literal("等级：" + displayName).withStyle(color);
    }
}
