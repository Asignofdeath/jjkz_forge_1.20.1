package com.darkness.jujutsukaisenzone.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * 对白字幕染色：解析「角色名：」后按 {@link QuestDialogSpeakerPalette} 上色，正文白色。
 * 电影黑幕与 COD 字幕共用。
 */
public final class QuestDialogCaptionStyle
{
    public static final int BODY_RGB = 0xFFFFFF;

    private QuestDialogCaptionStyle()
    {
    }

    /**
     * 用完整句解析「角色名：」，再对 {@code visible} 前缀染色
     * （打字机未揭完时名字也不闪白）。
     */
    public static Component style(String full, String visible)
    {
        if (visible == null || visible.isEmpty())
        {
            return Component.empty();
        }
        if (full == null || full.isEmpty())
        {
            return Component.literal(visible).withStyle(bodyStyle());
        }
        int colonIndex = indexOfSpeakerColon(full);
        if (colonIndex < 0)
        {
            return Component.literal(visible).withStyle(bodyStyle());
        }
        String speaker = full.substring(0, colonIndex);
        QuestDialogStance stance = QuestDialogSpeakerPalette.stanceOf(speaker);
        int nameLen = colonIndex + 1;
        if (visible.length() <= nameLen)
        {
            return Component.literal(visible).withStyle(nameStyle(stance));
        }
        MutableComponent result = Component.literal(visible.substring(0, nameLen)).withStyle(nameStyle(stance));
        result.append(Component.literal(visible.substring(nameLen)).withStyle(bodyStyle()));
        return result;
    }

    /** 整句染色（无打字机）。 */
    public static Component styleFull(String plain)
    {
        return style(plain, plain == null ? "" : plain);
    }

    public static int indexOfSpeakerColon(String plain)
    {
        if (plain == null || plain.isEmpty())
        {
            return -1;
        }
        int fullWidth = plain.indexOf('：');
        int halfWidth = plain.indexOf(':');
        if (fullWidth < 0)
        {
            return halfWidth;
        }
        if (halfWidth < 0)
        {
            return fullWidth;
        }
        return Math.min(fullWidth, halfWidth);
    }

    public static Style nameStyle(QuestDialogStance stance)
    {
        return Style.EMPTY.withColor(TextColor.fromRgb(stance.rgb()));
    }

    public static Style bodyStyle()
    {
        return Style.EMPTY.withColor(TextColor.fromRgb(BODY_RGB));
    }
}
