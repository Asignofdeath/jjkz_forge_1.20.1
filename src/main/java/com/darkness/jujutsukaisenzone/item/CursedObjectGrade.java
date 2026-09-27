package com.darkness.jujutsukaisenzone.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 咒物等级：常见 / 罕见 / 特级。
 */
public enum CursedObjectGrade
{
    COMMON("常见", ChatFormatting.WHITE, 0),
    RARE("罕见", ChatFormatting.AQUA, 0xFF55FFFF),
    SPECIAL("特级", ChatFormatting.GOLD, 0xFFFFAA00);

    private final String displayName;
    private final ChatFormatting color;
    /** ARGB；0 表示不画背包边框（常见档避免噪音）。 */
    private final int borderColorArgb;

    CursedObjectGrade(String displayName, ChatFormatting color, int borderColorArgb)
    {
        this.displayName = displayName;
        this.color = color;
        this.borderColorArgb = borderColorArgb;
    }

    public String displayName()
    {
        return displayName;
    }

    public ChatFormatting color()
    {
        return color;
    }

    public int borderColorArgb()
    {
        return borderColorArgb;
    }

    public Component tooltipLine()
    {
        return Component.literal("等级：" + displayName).withStyle(color);
    }
}
